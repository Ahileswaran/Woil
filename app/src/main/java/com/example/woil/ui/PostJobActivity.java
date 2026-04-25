package com.example.woil.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PostJobActivity extends AppCompatActivity {

    private static final String TAG = "POST_JOB";
    private static final int REQ_PICK_LOCATION = 1001;

    private MaterialButton btnPostJob;
    private ImageButton btnBack;
    private EditText etDescription;
    private EditText etTitle;
    private Spinner spinnerCategory;
    private EditText etWageSuggested;
    private TextView tvSetLocation;
    private TextView tvLocationName;
    private ImageView imgMap;
    private TextView tvDate;
    private TextView tvTimeRange;
    private TextView tvWageRangeBadge;
    private Switch switchAllowOffers;

    private String selectedAddress = "";
    private Double selectedLat = null;
    private Double selectedLng = null;
    private String selectedSnapshotPath = "";

    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal = Calendar.getInstance();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_job);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        btnPostJob = findViewById(R.id.btn_post_job);
        btnBack = findViewById(R.id.btn_back);
        etDescription = findViewById(R.id.et_description);
        etTitle = findViewById(R.id.et_title);
        spinnerCategory = findViewById(R.id.spinner_category);
        etWageSuggested = findViewById(R.id.et_wage_suggested);
        tvSetLocation = findViewById(R.id.tv_set_location);
        tvLocationName = findViewById(R.id.tv_location_name);
        imgMap = findViewById(R.id.img_map);
        tvDate = findViewById(R.id.tv_date);
        tvTimeRange = findViewById(R.id.tv_time_range);
        tvWageRangeBadge = findViewById(R.id.tv_wage_range_badge);
        switchAllowOffers = findViewById(R.id.switch_allow_offers);

        applyBottomInsetToPostButton();

        String[] categories = new String[] {
                "Cleaning",
                "Electrician",
                "Caregiver",
                "Appliance Repair",
                "Masonary",
                "Laundry",
                "Painting",
                "Plumbing",
                "Gardening",
                "Carpentry"
        };

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categories
        );
        spinnerCategory.setAdapter(catAdapter);

        setDefaultStartEndTimes();
        refreshDateTimeText();

        btnBack.setOnClickListener(v -> finish());

        android.view.View.OnClickListener openMapPicker = v -> {
            Intent i = new Intent(PostJobActivity.this, MapPickerActivity.class);
            startActivityForResult(i, REQ_PICK_LOCATION);
        };

        tvSetLocation.setOnClickListener(openMapPicker);
        imgMap.setOnClickListener(openMapPicker);
        if (tvLocationName != null) {
            tvLocationName.setOnClickListener(openMapPicker);
        }

        tvDate.setOnClickListener(v -> {
            int y = startCal.get(Calendar.YEAR);
            int m = startCal.get(Calendar.MONTH);
            int d = startCal.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dp = new DatePickerDialog(PostJobActivity.this, (view, year, month, dayOfMonth) -> {
                startCal.set(Calendar.YEAR, year);
                startCal.set(Calendar.MONTH, month);
                startCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                endCal.set(Calendar.YEAR, year);
                endCal.set(Calendar.MONTH, month);
                endCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                refreshDateTimeText();
            }, y, m, d);
            dp.show();
        });

        tvTimeRange.setOnClickListener(v -> pickStartTime());

        btnPostJob.setOnClickListener(v -> {
            String description = etDescription != null ? etDescription.getText().toString().trim() : "";
            if (TextUtils.isEmpty(description)) {
                Toast.makeText(this, "Please enter a job description", Toast.LENGTH_SHORT).show();
                return;
            }

            String title = etTitle != null ? etTitle.getText().toString().trim() : "";
            if (TextUtils.isEmpty(title)) {
                Toast.makeText(this, "Please enter a job title", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedLat == null || selectedLng == null || TextUtils.isEmpty(selectedAddress)) {
                Toast.makeText(this, "Please set a location", Toast.LENGTH_SHORT).show();
                return;
            }

            ensureAuthenticatedThenPost();
        });
    }

    private void applyBottomInsetToPostButton() {
        if (btnPostJob == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(btnPostJob, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.bottomMargin = systemBars.bottom + dpToPx(16);
            v.setLayoutParams(lp);

            return insets;
        });

        ViewCompat.requestApplyInsets(btnPostJob);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void setDefaultStartEndTimes() {
        Calendar now = Calendar.getInstance();

        int minute = now.get(Calendar.MINUTE);
        if (minute == 0) {
            now.set(Calendar.MINUTE, 0);
        } else if (minute <= 30) {
            now.set(Calendar.MINUTE, 30);
        } else {
            now.add(Calendar.HOUR_OF_DAY, 1);
            now.set(Calendar.MINUTE, 0);
        }

        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        startCal.setTimeInMillis(now.getTimeInMillis());
        endCal.setTimeInMillis(startCal.getTimeInMillis());
        endCal.add(Calendar.HOUR_OF_DAY, 2);
    }

    private void refreshDateTimeText() {
        tvDate.setText("  " + dateFormatter.format(startCal.getTime()));
        tvTimeRange.setText("  " + timeFormatter.format(startCal.getTime()) + " - " + timeFormatter.format(endCal.getTime()));
    }

    private void pickStartTime() {
        int hour = startCal.get(Calendar.HOUR_OF_DAY);
        int minute = startCal.get(Calendar.MINUTE);
        boolean is24 = false;

        TimePickerDialog tpd = new TimePickerDialog(PostJobActivity.this, (view, hourOfDay, minute1) -> {
            startCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
            startCal.set(Calendar.MINUTE, minute1);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);

            if (!endCal.after(startCal)) {
                endCal.setTimeInMillis(startCal.getTimeInMillis());
                endCal.add(Calendar.HOUR_OF_DAY, 1);
            }

            pickEndTime();
            refreshDateTimeText();
        }, hour, minute, is24);

        tpd.show();
    }

    private void pickEndTime() {
        int hour = endCal.get(Calendar.HOUR_OF_DAY);
        int minute = endCal.get(Calendar.MINUTE);
        boolean is24 = false;

        TimePickerDialog tpd = new TimePickerDialog(PostJobActivity.this, (view, hourOfDay, minute1) -> {
            endCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
            endCal.set(Calendar.MINUTE, minute1);
            endCal.set(Calendar.SECOND, 0);
            endCal.set(Calendar.MILLISECOND, 0);

            if (!endCal.after(startCal)) {
                endCal.setTimeInMillis(startCal.getTimeInMillis());
                endCal.add(Calendar.HOUR_OF_DAY, 1);
                Toast.makeText(PostJobActivity.this, "End time adjusted to be after start time", Toast.LENGTH_SHORT).show();
            }

            refreshDateTimeText();
        }, hour, minute, is24);

        tpd.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PICK_LOCATION && resultCode == Activity.RESULT_OK && data != null) {
            selectedAddress = data.getStringExtra("address");
            selectedLat = data.getDoubleExtra("lat", 0.0);
            selectedLng = data.getDoubleExtra("lng", 0.0);
            selectedSnapshotPath = data.getStringExtra("snapshot_path");

            Log.d(TAG, "selectedAddress = " + selectedAddress);
            Log.d(TAG, "selectedLat = " + selectedLat);
            Log.d(TAG, "selectedLng = " + selectedLng);
            Log.d(TAG, "selectedSnapshotPath = " + selectedSnapshotPath);

            if (TextUtils.isEmpty(selectedAddress)) {
                selectedAddress = selectedLat + ", " + selectedLng;
            }

            updateSelectedLocationPreview();
        }
    }

    private void updateSelectedLocationPreview() {
        if (tvSetLocation != null) {
            tvSetLocation.setText("Change location");
        }

        if (tvLocationName != null) {
            tvLocationName.setText(!TextUtils.isEmpty(selectedAddress)
                    ? selectedAddress
                    : "Selected location");
        }

        if (!TextUtils.isEmpty(selectedSnapshotPath)) {
            File file = new File(selectedSnapshotPath);
            Log.d(TAG, "snapshot exists = " + file.exists());

            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bitmap != null) {
                    imgMap.setImageBitmap(bitmap);
                    return;
                }
            }
        }

        imgMap.setImageResource(R.drawable.ic_map_placeholder);
    }

    private void ensureAuthenticatedThenPost() {
        String uid = FirebaseDebugLogger.requireUid(this, mAuth, "post_job_create");
        if (uid == null) return;
        postJobToFirestore();
    }

    private void postJobToFirestore() {
        String uid = FirebaseDebugLogger.requireUid(this, mAuth, "post_job_write");
        if (uid == null) return;

        Map<String, Object> job = new HashMap<>();
        job.put("clientUid", uid);

        String title = etTitle != null ? etTitle.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(title)) {
            job.put("title", title);
        }

        String category = spinnerCategory != null && spinnerCategory.getSelectedItem() != null
                ? spinnerCategory.getSelectedItem().toString()
                : "";
        if (!TextUtils.isEmpty(category)) {
            job.put("category", category);
        }

        job.put("locationText", selectedAddress);

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("lat", selectedLat);
        locationMap.put("lng", selectedLng);
        job.put("location", locationMap);

        job.put("startAt", new Timestamp(startCal.getTime()));
        job.put("endAt", new Timestamp(endCal.getTime()));

        if (etWageSuggested != null) {
            String wageS = etWageSuggested.getText().toString().trim();
            if (!TextUtils.isEmpty(wageS)) {
                try {
                    double wage = Double.parseDouble(wageS);
                    job.put("wageSuggested", wage);
                    job.put("wageSuggestedText", "Rs. " + wageS);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        job.put("description", etDescription.getText().toString().trim());
        job.put("assignedUid", null);
        job.put("status", "OPEN");
        job.put("allowOffers", switchAllowOffers != null && switchAllowOffers.isChecked());
        job.put("createdAt", FieldValue.serverTimestamp());

        db.collection("jobs")
                .add(job)
                .addOnSuccessListener(docRef -> {
                    FirebaseDebugLogger.success("post_job_write", "jobs", docRef.getId());
                    Toast.makeText(PostJobActivity.this, "Job posted (id: " + docRef.getId() + ")", Toast.LENGTH_SHORT).show();
                    Intent result = new Intent();
                    result.putExtra("jobId", docRef.getId());
                    setResult(Activity.RESULT_OK, result);
                    finish();
                })
                .addOnFailureListener(e -> {
                    FirebaseDebugLogger.failure("post_job_write", "jobs", e);
                    Toast.makeText(PostJobActivity.this, "Failed to post job: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}