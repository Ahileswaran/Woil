package com.example.woil.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.woil.JobModel;
import com.example.woil.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class JobDetailActivity extends AppCompatActivity {

    private static final String TAG = "JobDetail";

    private TextView tvTitle;
    private TextView tvCategoryChip;
    private TextView tvLocation;
    private TextView tvTime;
    private TextView tvWage;
    private TextView tvDescription;
    private TextView tvPostedTime;
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private final SimpleDateFormat fmt =
            new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);

        tvTitle = findViewById(R.id.tv_job_title);
        tvCategoryChip = findViewById(R.id.tv_chip_category);
        tvLocation = findViewById(R.id.tv_location);
        tvTime = findViewById(R.id.tv_time);
        tvWage = findViewById(R.id.tv_wage);
        tvDescription = findViewById(R.id.tv_description);
        tvPostedTime = findViewById(R.id.tv_posted_time);
        btnBack = findViewById(R.id.btn_back);

        db = FirebaseFirestore.getInstance();

        String jobId = getIntent().getStringExtra("jobId");
        Log.d(TAG, "onCreate: received jobId=" + jobId);

        try {
            String projectId = FirebaseApp.getInstance().getOptions().getProjectId();
            Log.d(TAG, "Firebase projectId: " + projectId);
        } catch (Exception e) {
            Log.w(TAG, "Could not read Firebase projectId", e);
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
                    if (startAt instanceof Timestamp) job.startAt = (Timestamp) startAt;

                    Object createdAt = documentSnapshot.get("createdAt");
                    if (createdAt instanceof Timestamp) job.createdAt = (Timestamp) createdAt;

                    displayJob(job);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadJob failed", e);
                    Toast.makeText(this, "Failed to load job", Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void displayJob(JobModel job) {
        tvTitle.setText(safe(job.title, "Untitled Job"));
        tvCategoryChip.setText(safe(job.category, "General"));
        tvLocation.setText(safe(job.locationText, "Location not available"));
        tvDescription.setText(safe(job.description, "No description available"));

        if (job.wageSuggestedText != null && !job.wageSuggestedText.trim().isEmpty()) {
            tvWage.setText(job.wageSuggestedText);
        } else if (job.wageSuggested != null) {
            tvWage.setText("Rs. " + Math.round(job.wageSuggested));
        } else {
            tvWage.setText("Wage not specified");
        }

        if (job.startAt != null) {
            tvTime.setText(fmt.format(job.startAt.toDate()));
        } else {
            tvTime.setText("Time not specified");
        }

        if (job.createdAt != null) {
            tvPostedTime.setText("Posted " + fmt.format(job.createdAt.toDate()));
        } else {
            tvPostedTime.setText("Recently posted");
        }
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}