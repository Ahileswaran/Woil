package com.example.woil.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.woil.JobModel;
import com.example.woil.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class JobDetailActivity extends AppCompatActivity {

    private static final String TAG = "JobDetail";

    private TextView tvTitle, tvCategory, tvLocation, tvTime, tvWage, tvDescription;
    private ImageButton btnBack;
    private FirebaseFirestore db;
    private SimpleDateFormat fmt = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);

        tvTitle = findViewById(R.id.tv_job_title);
        tvCategory = findViewById(R.id.tv_category);
        tvLocation = findViewById(R.id.tv_location);
        tvTime = findViewById(R.id.tv_time);
        tvWage = findViewById(R.id.tv_wage);
        tvDescription = findViewById(R.id.tv_description);
        btnBack = findViewById(R.id.btn_back);

        db = FirebaseFirestore.getInstance();

        String jobId = getIntent().getStringExtra("jobId");
        Log.d(TAG, "onCreate: received jobId=" + jobId);

        // Print Firebase project in use (helps verify google-services.json)
        try {
            String projectId = FirebaseApp.getInstance().getOptions().getProjectId();
            Log.d(TAG, "Firebase projectId: " + projectId);
        } catch (Exception e) {
            Log.w(TAG, "Could not read Firebase projectId: " + e.getMessage(), e);
        }

        if (jobId == null || jobId.trim().isEmpty()) {
            Toast.makeText(this, "Missing job id", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> finish());

        loadJob(jobId);
    }

    private void loadJob(String jobId) {
        Log.d(TAG, "Loading job document: jobs/" + jobId);
        db.collection("jobs").document(jobId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "Document fetch success: exists=" + documentSnapshot.exists());
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Show the raw data in the log (useful to inspect unexpected shapes)
                    Map<String, Object> raw = documentSnapshot.getData();
                    Log.d(TAG, "Document data: " + (raw != null ? raw.toString() : "null"));

                    // Preferred path: map to JobModel
                    JobModel job = documentSnapshot.toObject(JobModel.class);
                    if (job != null) {
                        // ensure id field set if needed
                        job.id = documentSnapshot.getId();
                        displayJob(job);
                        return;
                    }

                    // Fallback: manual extraction (handles schema mismatch / missing fields)
                    Log.w(TAG, "toObject returned null — falling back to manual parsing");
                    JobModel j = new JobModel();
                    j.id = documentSnapshot.getId();
                    try {
                        Object o;
                        o = documentSnapshot.get("clientUid");
                        j.clientUid = o != null ? o.toString() : null;

                        o = documentSnapshot.get("title");
                        j.title = o != null ? o.toString() : null;

                        o = documentSnapshot.get("category");
                        j.category = o != null ? o.toString() : null;

                        o = documentSnapshot.get("locationText");
                        j.locationText = o != null ? o.toString() : null;

                        // location map
                        Object loc = documentSnapshot.get("location");
                        if (loc instanceof Map) {
                            Map<?,?> lm = (Map<?,?>) loc;
                            Object lat = lm.get("lat");
                            Object lng = lm.get("lng");
                            if (lat instanceof Number) j.lat = ((Number) lat).doubleValue();
                            if (lng instanceof Number) j.lng = ((Number) lng).doubleValue();
                        }

                        // timestamps: startAt/endAt may be com.google.firebase.Timestamp
                        Object startAt = documentSnapshot.get("startAt");
                        if (startAt instanceof com.google.firebase.Timestamp) {
                            j.startAt = (com.google.firebase.Timestamp) startAt;
                        }
                        Object endAt = documentSnapshot.get("endAt");
                        if (endAt instanceof com.google.firebase.Timestamp) {
                            j.endAt = (com.google.firebase.Timestamp) endAt;
                        }

                        Object wageObj = documentSnapshot.get("wageSuggested");
                        if (wageObj instanceof Number) j.wageSuggested = ((Number) wageObj).doubleValue();

                        Object wageText = documentSnapshot.get("wageSuggestedText");
                        if (wageText != null) j.wageSuggestedText = wageText.toString();

                        Object desc = documentSnapshot.get("description");
                        j.description = desc != null ? desc.toString() : null;
                    } catch (Exception e) {
                        Log.e(TAG, "Manual parsing failed", e);
                    }

                    displayJob(j);
                })
                .addOnFailureListener(e -> {
                    // Log exact exception message and show user-friendly toast
                    Log.e(TAG, "loadJob failed: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to load job: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void displayJob(JobModel job) {
        runOnUiThread(() -> {
            tvTitle.setText(job.title != null ? job.title : "—");
            tvCategory.setText(job.category != null ? job.category : "");
            tvLocation.setText(job.locationText != null ? job.locationText : "");
            tvWage.setText(job.wageSuggestedText != null ? job.wageSuggestedText : (job.wageSuggested != null ? "Rs. " + job.wageSuggested : ""));
            String time = "";
            if (job.startAt != null && job.endAt != null) {
                time = fmt.format(job.startAt.toDate()) + " - " + fmt.format(job.endAt.toDate());
            } else if (job.startAt != null) {
                time = fmt.format(job.startAt.toDate());
            }
            tvTime.setText(time);
            tvDescription.setText(job.description != null ? job.description : "");
        });
    }
}