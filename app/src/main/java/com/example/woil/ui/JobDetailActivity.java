package com.example.woil.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.JobModel;
import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class JobDetailActivity extends AppCompatActivity {

    private static final String TAG = "JobDetail";
    private static final int REQ_APPLY_JOB = 2001;

    private TextView tvTitle;
    private TextView tvCategoryChip;
    private TextView tvLocation;
    private TextView tvTime;
    private TextView tvWage;
    private TextView tvDescription;
    private TextView tvPostedTime;
    private TextView tvLocationPreviewText;
    private TextView tvDuration;

    private ImageView ivJobImage;
    private ImageView imgLocationPreview;

    private ImageButton btnBack;
    private MaterialButton btnApply;
    private MaterialButton btnApplyNowBottom;

    private FirebaseFirestore db;

    private String currentJobId;
    private String currentClientUid;
    private String currentTitle;
    private String currentWageText;
    private Double currentWageSuggested;
    private String currentTimeText;
    private String currentLocationText;
    private String currentCategory;
    private Double currentLat;
    private Double currentLng;

    private final SimpleDateFormat fmt =
            new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        tvTitle = findViewById(R.id.tv_job_title);
        tvCategoryChip = findViewById(R.id.tv_chip_category);
        tvLocation = findViewById(R.id.tv_location);
        tvTime = findViewById(R.id.tv_time);
        tvWage = findViewById(R.id.tv_wage);
        tvDescription = findViewById(R.id.tv_description);
        tvPostedTime = findViewById(R.id.tv_posted_time);
        tvLocationPreviewText = findViewById(R.id.tv_location_preview_text);
        tvDuration = findViewById(R.id.tv_duration);

        ivJobImage = findViewById(R.id.iv_job_image);
        imgLocationPreview = findViewById(R.id.img_location_preview);

        btnBack = findViewById(R.id.btn_back);
        btnApply = findViewById(R.id.btn_apply);
        btnApplyNowBottom = findViewById(R.id.btn_apply_now_bottom);

        db = FirebaseFirestore.getInstance();

        currentJobId = getIntent().getStringExtra("jobId");
        Log.d(TAG, "onCreate: received jobId=" + currentJobId);

        try {
            String projectId = FirebaseApp.getInstance().getOptions().getProjectId();
            Log.d(TAG, "Firebase projectId: " + projectId);
        } catch (Exception e) {
            Log.w(TAG, "Could not read Firebase projectId", e);
        }

        if (currentJobId == null || currentJobId.trim().isEmpty()) {
            Toast.makeText(this, "Missing job id", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> finish());
        btnApply.setOnClickListener(v -> openApplyScreen());
        btnApplyNowBottom.setOnClickListener(v -> openApplyScreen());

        loadJob(currentJobId);
    }

    private void loadJob(String jobId) {
        db.collection("jobs").document(jobId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    JobModel job = documentSnapshot.toObject(JobModel.class);
                    if (job == null) {
                        job = new JobModel();
                    }

                    job.id = documentSnapshot.getId();

                    Object title = documentSnapshot.get("title");
                    if (title != null) job.title = title.toString();

                    Object category = documentSnapshot.get("category");
                    if (category != null) job.category = category.toString();

                    Object locationText = documentSnapshot.get("locationText");
                    if (locationText != null) job.locationText = locationText.toString();

                    Object description = documentSnapshot.get("description");
                    if (description != null) job.description = description.toString();

                    Object wageText = documentSnapshot.get("wageSuggestedText");
                    if (wageText != null) {
                        job.wageSuggestedText = wageText.toString();
                    } else {
                        Object wage = documentSnapshot.get("wageSuggested");
                        if (wage instanceof Number) {
                            job.wageSuggested = ((Number) wage).doubleValue();
                        }
                    }

                    Object startAt = documentSnapshot.get("startAt");
                    if (startAt instanceof Timestamp) {
                        job.startAt = (Timestamp) startAt;
                    }

                    Object endAt = documentSnapshot.get("endAt");
                    if (endAt instanceof Timestamp) {
                        job.endAt = (Timestamp) endAt;
                    }

                    Object createdAt = documentSnapshot.get("createdAt");
                    if (createdAt instanceof Timestamp) {
                        job.createdAt = (Timestamp) createdAt;
                    }

                    Object clientUid = documentSnapshot.get("clientUid");
                    if (clientUid != null) {
                        currentClientUid = clientUid.toString();
                    }

                    Object locationObj = documentSnapshot.get("location");
                    Double lat = null;
                    Double lng = null;

                    if (locationObj instanceof Map) {
                        Map<?, ?> locationMap = (Map<?, ?>) locationObj;

                        Object latObj = locationMap.get("lat");
                        Object lngObj = locationMap.get("lng");

                        if (latObj instanceof Number) {
                            lat = ((Number) latObj).doubleValue();
                        }
                        if (lngObj instanceof Number) {
                            lng = ((Number) lngObj).doubleValue();
                        }
                    }

                    displayJob(job, lat, lng);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadJob failed", e);
                    Toast.makeText(this, "Failed to load job", Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void displayJob(JobModel job, Double lat, Double lng) {
        currentLat = lat;
        currentLng = lng;
        currentTitle = safe(job.title, "Untitled Job");
        currentLocationText = safe(job.locationText, "Location not available");

        tvTitle.setText(currentTitle);
        tvCategoryChip.setText(safe(job.category, "General"));
        setJobCategoryImage(job.category);

        tvLocation.setText(currentLocationText);
        tvDescription.setText(safe(job.description, "No description available"));
        tvLocationPreviewText.setText(currentLocationText);

        if (job.wageSuggestedText != null && !job.wageSuggestedText.trim().isEmpty()) {
            currentWageText = job.wageSuggestedText;
            tvWage.setText(job.wageSuggestedText);
        } else if (job.wageSuggested != null) {
            currentWageSuggested = job.wageSuggested;
            currentWageText = "Rs. " + Math.round(job.wageSuggested);
            tvWage.setText(currentWageText);
        } else {
            currentWageText = "Wage not specified";
            tvWage.setText(currentWageText);
        }

        if (job.startAt != null) {
            if (job.endAt != null) {
                currentTimeText =
                        fmt.format(job.startAt.toDate()) + " - " +
                                new SimpleDateFormat("hh:mm a", Locale.getDefault())
                                        .format(job.endAt.toDate());
                tvTime.setText(currentTimeText);
            } else {
                currentTimeText = fmt.format(job.startAt.toDate());
                tvTime.setText(currentTimeText);
            }
        } else {
            currentTimeText = "Time not specified";
            tvTime.setText(currentTimeText);
        }

        if (job.createdAt != null) {
            tvPostedTime.setText("Posted " + fmt.format(job.createdAt.toDate()));
        } else {
            tvPostedTime.setText("Recently posted");
        }

        if (job.startAt != null && job.endAt != null) {
            long diffMillis = job.endAt.toDate().getTime() - job.startAt.toDate().getTime();
            long totalMinutes = Math.max(0, diffMillis / (60 * 1000));
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;

            String durationText;
            if (hours > 0 && minutes > 0) {
                durationText = "Duration: " + hours + " hr " + minutes + " min";
            } else if (hours > 0) {
                durationText = "Duration: " + hours + " hour" + (hours > 1 ? "s" : "");
            } else {
                durationText = "Duration: " + minutes + " min";
            }
            tvDuration.setText(durationText);
        } else {
            tvDuration.setText("Duration: Not specified");
        }

        if (lat != null && lng != null) {
            Log.d(TAG, "Using stored lat/lng: " + lat + ", " + lng);
            loadStaticLocationPreview(lat, lng);
        } else {
            String address = tvLocationPreviewText.getText().toString().trim();
            Log.d(TAG, "Lat/lng missing. Trying geocode from address: " + address);
            geocodeAddressAndLoadPreview(address);
        }
    }

    private void setJobCategoryImage(String category) {
        if (ivJobImage == null) return;

        String value = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        int imageRes;

        switch (value) {
            case "cleaning":
                imageRes = R.drawable.cleaning;
                break;

            case "electrician":
                imageRes = R.drawable.electic;
                break;

            case "caregiver":
                imageRes = R.drawable.caregiver;
                break;

            case "appliance repair":
                imageRes = R.drawable.repair;
                break;

            case "masonry":
                imageRes = R.drawable.masanory;
                break;

            case "laundry":
                imageRes = R.drawable.landury;
                break;

            case "painting":
                imageRes = R.drawable.painting;
                break;

            case "plumbing":
                imageRes = R.drawable.plumbing;
                break;

            case "gardening":
                imageRes = R.drawable.gardining;
                break;

            case "carpentry":
                imageRes = R.drawable.carpentary;
                break;

            default:
                imageRes = R.drawable.cleaning;
                break;
        }

        ivJobImage.setImageResource(imageRes);
    }

    private void openApplyScreen() {
        if (TextUtils.isEmpty(currentJobId) || TextUtils.isEmpty(currentClientUid)) {
            Toast.makeText(this, "Job information not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(JobDetailActivity.this, ApplyJobActivity.class);
        intent.putExtra("jobId", currentJobId);
        intent.putExtra("clientUid", currentClientUid);
        intent.putExtra("title", currentTitle);
        intent.putExtra("wageText", currentWageText);
        intent.putExtra("timeText", currentTimeText);
        intent.putExtra("locationText", currentLocationText);
        intent.putExtra("category", currentCategory);

        if (currentLat != null) intent.putExtra("jobLat", currentLat);
        if (currentLng != null) intent.putExtra("jobLng", currentLng);

        if (currentWageSuggested != null) {
            intent.putExtra("wageSuggested", currentWageSuggested);
        }

        startActivityForResult(intent, REQ_APPLY_JOB);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_APPLY_JOB && resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Application submitted", Toast.LENGTH_SHORT).show();
        }
    }

    private void geocodeAddressAndLoadPreview(String address) {
        if (TextUtils.isEmpty(address) || "No location available".equalsIgnoreCase(address)) {
            imgLocationPreview.setImageResource(R.drawable.ic_map_placeholder);
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocationName(address, 1);

                if (results != null && !results.isEmpty()) {
                    double lat = results.get(0).getLatitude();
                    double lng = results.get(0).getLongitude();

                    Log.d(TAG, "Geocoded address to: " + lat + ", " + lng);
                    loadStaticLocationPreview(lat, lng);
                } else {
                    Log.e(TAG, "Geocoder returned no results for address: " + address);
                    runOnUiThread(() ->
                            imgLocationPreview.setImageResource(R.drawable.ic_map_placeholder)
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to geocode address", e);
                runOnUiThread(() ->
                        imgLocationPreview.setImageResource(R.drawable.ic_map_placeholder)
                );
            }
        });
    }

    private void loadStaticLocationPreview(double lat, double lng) {
        Executors.newSingleThreadExecutor().execute(() -> {
            HttpURLConnection connection = null;
            InputStream inputStream = null;

            try {
                String apiKey = getString(R.string.static_maps_api_key);
                Log.d(TAG, "maps_api_key empty? " + TextUtils.isEmpty(apiKey));
                Log.d(TAG, "Loading static map for: " + lat + ", " + lng);

                if (TextUtils.isEmpty(apiKey)) {
                    throw new IllegalStateException("maps_api_key is empty");
                }

                String mapUrl =
                        "https://maps.googleapis.com/maps/api/staticmap" +
                                "?center=" + lat + "," + lng +
                                "&zoom=15" +
                                "&size=800x400" +
                                "&scale=2" +
                                "&markers=color:red%7C" + lat + "," + lng +
                                "&key=" + apiKey;

                Log.d(TAG, "Map URL = " + mapUrl);

                URL url = new URL(mapUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setDoInput(true);
                connection.connect();

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Static map responseCode = " + responseCode);

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new RuntimeException("HTTP error code: " + responseCode);
                }

                inputStream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                runOnUiThread(() -> {
                    if (bitmap != null) {
                        imgLocationPreview.setImageBitmap(bitmap);
                    } else {
                        Log.e(TAG, "BitmapFactory.decodeStream returned null");
                        imgLocationPreview.setImageResource(R.drawable.ic_map_placeholder);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to load location preview", e);
                runOnUiThread(() ->
                        imgLocationPreview.setImageResource(R.drawable.ic_map_placeholder)
                );
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                } catch (Exception ignored) {
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private String safe(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }
}