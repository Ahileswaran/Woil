package com.example.woil.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.JobAdapter;
import com.example.woil.JobModel;
import com.example.woil.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class JobsByCategoryActivity extends AppCompatActivity {

    private static final String TAG = "JobsByCat";

    private TextView tvCategoryTitle;
    private TextView tvResultsInfo;
    private RecyclerView rvJobs;
    private ProgressBar progress;
    private TextView tvEmpty;
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private JobAdapter adapter;
    private List<JobModel> items = new ArrayList<>();

    private String category;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jobs_by_category);

        tvCategoryTitle = findViewById(R.id.tv_category_title);
        tvResultsInfo = findViewById(R.id.tv_results_info);
        rvJobs = findViewById(R.id.rv_jobs);
        progress = findViewById(R.id.progress);
        tvEmpty = findViewById(R.id.tv_empty);
        btnBack = findViewById(R.id.btn_back);

        db = FirebaseFirestore.getInstance();

        category = getIntent().getStringExtra("category");
        if (category == null) category = "Uncategorized";

        tvCategoryTitle.setText(category);

        rvJobs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new JobAdapter(items, this, job -> {
            // open detail activity
            Intent i = new Intent(JobsByCategoryActivity.this, JobDetailActivity.class);
            i.putExtra("jobId", job.id);
            startActivity(i);
        });
        rvJobs.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        fetchJobsForCategory(category);
    }

    private void fetchJobsForCategory(String category) {
        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        tvResultsInfo.setText("Loading...");

        // Query: filter by category; order by createdAt descending
        Query q = db.collection("jobs")
                .whereEqualTo("category", category)
                .orderBy("createdAt", Query.Direction.DESCENDING);

        q.get()
                .addOnSuccessListener((OnSuccessListener<QuerySnapshot>) queryDocumentSnapshots -> {
                    progress.setVisibility(View.GONE);
                    items.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        JobModel job = doc.toObject(JobModel.class);
                        if (job == null) continue;
                        job.id = doc.getId();

                        // if location stored as map, try to extract
                        try {
                            Object loc = doc.get("location");
                            if (loc instanceof com.google.firebase.firestore.DocumentReference) {
                                // unlikely — skip
                            } else if (doc.get("location") instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> lm = (java.util.Map<String, Object>) doc.get("location");
                                Object lat = lm.get("lat");
                                Object lng = lm.get("lng");
                                if (lat instanceof Number) job.lat = ((Number) lat).doubleValue();
                                if (lng instanceof Number) job.lng = ((Number) lng).doubleValue();
                            }
                        } catch (Exception ignore) {}

                        items.add(job);
                    }
                    adapter.notifyDataSetChanged();
                    tvResultsInfo.setText("Found " + items.size() + " jobs");
                    tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    tvResultsInfo.setText("Failed to load jobs");
                    Log.e(TAG, "fetchJobsForCategory failed", e);
                });
    }
}