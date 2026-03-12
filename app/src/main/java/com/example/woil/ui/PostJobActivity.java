package com.example.woil.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Date;

public class PostJobActivity extends AppCompatActivity {

    private static final int REQ_PICK_LOCATION = 1001;

    private MaterialButton btnPostJob;
    private ImageButton btnBack;
    private EditText etDescription;
    private EditText etTitle;
    private Spinner spinnerCategory;
    private EditText etWageSuggested;
    private TextView tvSetLocation;
    private ImageView imgMap;
    private TextView tvDate;
    private TextView tvTimeRange;
    private TextView tvWageRangeBadge;
    private Switch switchAllowOffers;

    // selected location
    private String selectedAddress = "";
    private Double selectedLat = null;
    private Double selectedLng = null;

    // date/time state
    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal = Calendar.getInstance();

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // formatters
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_job);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // find views
        btnPostJob = findViewById(R.id.btn_post_job);
        btnBack = findViewById(R.id.btn_back);
        etDescription = findViewById(R.id.et_description);
        etTitle = findViewById(R.id.et_title);
        spinnerCategory = findViewById(R.id.spinner_category);
        etWageSuggested = findViewById(R.id.et_wage_suggested);
        tvSetLocation = findViewById(R.id.tv_set_location);
        imgMap = findViewById(R.id.img_map);
        tvDate = findViewById(R.id.tv_date);
        tvTimeRange = findViewById(R.id.tv_time_range);
        tvWageRangeBadge = findViewById(R.id.tv_wage_range_badge);
        switchAllowOffers = findViewById(R.id.switch_allow_offers);

        // categories list
        String[] categories = new String[] {
                "Cleaning",
                "Electrician",
                "Caregiver",
                "Appliance Repair",
                "Masonary", // keep as provided; change to "Masonry" if desired
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

        // Initialize date/time defaults (start = now rounded to next half-hour, end = start + 2 hours)
        setDefaultStartEndTimes();

        // update UI
        refreshDateTimeText();

        btnBack.setOnClickListener(v -> finish());

        android.view.View.OnClickListener openMapPicker = v -> {
            // MapPickerActivity should return "address", "lat", "lng" extras
            Intent i = new Intent(PostJobActivity.this, MapPickerActivity.class);
            startActivityForResult(i, REQ_PICK_LOCATION);
        };

        tvSetLocation.setOnClickListener(openMapPicker);
        imgMap.setOnClickListener(openMapPicker);

        // Date picker
        tvDate.setOnClickListener(v -> {
            int y = startCal.get(Calendar.YEAR);
            int m = startCal.get(Calendar.MONTH);
            int d = startCal.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dp = new DatePickerDialog(PostJobActivity.this, (view, year, month, dayOfMonth) -> {
                startCal.set(Calendar.YEAR, year);
                startCal.set(Calendar.MONTH, month);
                startCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                // also move endCal to same day (keep times)
                endCal.set(Calendar.YEAR, year);
                endCal.set(Calendar.MONTH, month);
                endCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                refreshDateTimeText();
            }, y, m, d);
            dp.show();
        });

        // Time range picker (start then end)
        tvTimeRange.setOnClickListener(v -> pickStartTime());
        // also allow clicking wage badge to show typed wage immediately (optional)
        // tvWageRangeBadge.setOnClickListener(...)

        btnPostJob.setOnClickListener(v -> {
            String description = etDescription != null ? etDescription.getText().toString().trim() : "";
            if (TextUtils.isEmpty(description)) {
                Toast.makeText(this, "Please enter a job description", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(etTitle != null ? etTitle.getText().toString().trim() : "")) {
                Toast.makeText(this, "Please enter a job title", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedLat == null || selectedLng == null || TextUtils.isEmpty(selectedAddress)) {
                Toast.makeText(this, "Please set a location", Toast.LENGTH_SHORT).show();
                return;
            }

            postJobToFirestore();
        });
    }

    private void setDefaultStartEndTimes() {
        Calendar now = Calendar.getInstance();

        // round minutes to nearest 30 minutes (next slot)
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
        // default end = start + 2 hours
        endCal.setTimeInMillis(startCal.getTimeInMillis());
        endCal.add(Calendar.HOUR_OF_DAY, 2);
    }

    private void refreshDateTimeText() {
        // date shown uses startCal's date
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

            // ensure end is same day; if end < start, move end to start + 1 hour
            if (!endCal.after(startCal)) {
                endCal.setTimeInMillis(startCal.getTimeInMillis());
                endCal.add(Calendar.HOUR_OF_DAY, 1);
            }

            // after picking start, open end time picker
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

            // if end <= start, adjust end = start + 1 hour
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

            tvSetLocation.setText(selectedAddress != null ? selectedAddress : "Selected location");
            // Optionally set an image in imgMap using the Static Maps API (not included here).
        }
    }

    private void postJobToFirestore() {
        String uid = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : null;

        Map<String, Object> job = new HashMap<>();
        // required/important fields per schema:
        job.put("clientUid", uid);

        // title
        String title = etTitle != null ? etTitle.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(title)) job.put("title", title);

        // category from spinner
        String category = spinnerCategory != null && spinnerCategory.getSelectedItem() != null
                ? spinnerCategory.getSelectedItem().toString()
                : "";
        if (!TextUtils.isEmpty(category)) job.put("category", category);

        job.put("locationText", selectedAddress);

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("lat", selectedLat);
        locationMap.put("lng", selectedLng);
        job.put("location", locationMap);

        // startAt / endAt as Firebase Timestamps
        if (startCal != null) {
            Date sDate = startCal.getTime();
            job.put("startAt", new Timestamp(sDate));
        }
        if (endCal != null) {
            Date eDate = endCal.getTime();
            job.put("endAt", new Timestamp(eDate));
        }

        // wageSuggested numeric
        if (etWageSuggested != null) {
            String wageS = etWageSuggested.getText().toString().trim();
            if (!TextUtils.isEmpty(wageS)) {
                try {
                    double wage = Double.parseDouble(wageS);
                    job.put("wageSuggested", wage);
                    job.put("wageSuggestedText", "Rs. " + wageS);
                } catch (NumberFormatException ignored) { }
            }
        }

        job.put("description", etDescription.getText().toString().trim());
        job.put("assignedUid", null);
        job.put("status", "OPEN");
        job.put("createdAt", Timestamp.now());
        // allow offers flag
        job.put("allowOffers", (switchAllowOffers != null) && switchAllowOffers.isChecked());

        // Write to Firestore jobs collection
        db.collection("jobs")
                .add(job)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(PostJobActivity.this, "Job posted (id: " + docRef.getId() + ")", Toast.LENGTH_SHORT).show();
                    Intent result = new Intent();
                    result.putExtra("jobId", docRef.getId());
                    setResult(Activity.RESULT_OK, result);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PostJobActivity.this, "Failed to post job: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}