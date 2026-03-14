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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

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
        if (jobId == null) {
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
                        Toast.makeText(this, "Bad job data", Toast.LENGTH_SHORT).show();
                        return;
                    }

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
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadJob failed", e);
                    Toast.makeText(this, "Failed to load job: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}