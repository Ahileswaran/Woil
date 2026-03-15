package com.example.woil.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.JobAdapter;
import com.example.woil.JobModel;
import com.example.woil.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    /**
     * Option B: Query without server-side ordering (no composite index required),
     * then sort the results client-side by createdAt (newest first).
     *
     * Note: This is fine for small result sets. For production or large datasets,
     * create the composite index and use server-side ordering instead.
     */
    private void fetchJobsForCategory(String category) {
        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        tvResultsInfo.setText("Loading...");

        // Query: filter by category only (no orderBy to avoid index requirement)
        Query q = db.collection("jobs")
                .whereEqualTo("category", category);

        q.get()
                .addOnSuccessListener((OnSuccessListener<QuerySnapshot>) queryDocumentSnapshots -> {
                    progress.setVisibility(View.GONE);
                    items.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        // Try mapping to model first
                        JobModel job = doc.toObject(JobModel.class);

                        if (job == null) {
                            // Fallback manual parsing if mapping fails
                            job = new JobModel();
                            job.id = doc.getId();
                            try {
                                Object o;
                                o = doc.get("clientUid"); if (o != null) job.clientUid = o.toString();
                                o = doc.get("title"); if (o != null) job.title = o.toString();
                                o = doc.get("category"); if (o != null) job.category = o.toString();
                                o = doc.get("locationText"); if (o != null) job.locationText = o.toString();
                                // location map
                                Object loc = doc.get("location");
                                if (loc instanceof java.util.Map) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<String, Object> lm = (java.util.Map<String, Object>) loc;
                                    Object lat = lm.get("lat"); if (lat instanceof Number) job.lat = ((Number) lat).doubleValue();
                                    Object lng = lm.get("lng"); if (lng instanceof Number) job.lng = ((Number) lng).doubleValue();
                                }
                                // timestamps
                                Object startAt = doc.get("startAt");
                                if (startAt instanceof Timestamp) job.startAt = (Timestamp) startAt;
                                Object endAt = doc.get("endAt");
                                if (endAt instanceof Timestamp) job.endAt = (Timestamp) endAt;
                                Object createdAt = doc.get("createdAt");
                                if (createdAt instanceof Timestamp) job.createdAt = (Timestamp) createdAt;
                                Object wage = doc.get("wageSuggested"); if (wage instanceof Number) job.wageSuggested = ((Number) wage).doubleValue();
                                Object wageText = doc.get("wageSuggestedText"); if (wageText != null) job.wageSuggestedText = wageText.toString();
                                Object desc = doc.get("description"); if (desc != null) job.description = desc.toString();
                            } catch (Exception ex) {
                                Log.w(TAG, "Manual parsing of doc failed: " + doc.getId(), ex);
                            }
                        } else {
                            // ensure id set and createdAt is captured if toObject didn't set it
                            job.id = doc.getId();
                            try {
                                Object createdAt = doc.get("createdAt");
                                if (createdAt instanceof Timestamp && job.createdAt == null) {
                                    job.createdAt = (Timestamp) createdAt;
                                }
                                // ensure lat/lng populated if missing
                                Object loc = doc.get("location");
                                if (loc instanceof java.util.Map) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<String, Object> lm = (java.util.Map<String, Object>) loc;
                                    Object lat = lm.get("lat"); if (lat instanceof Number && job.lat == null) job.lat = ((Number) lat).doubleValue();
                                    Object lng = lm.get("lng"); if (lng instanceof Number && job.lng == null) job.lng = ((Number) lng).doubleValue();
                                }
                            } catch (Exception ignore) {}
                        }

                        items.add(job);
                    }

                    // Client-side sort by createdAt (newest first). Null createdAt -> last.
                    Collections.sort(items, new Comparator<JobModel>() {
                        @Override
                        public int compare(JobModel a, JobModel b) {
                            Timestamp ta = a != null ? a.createdAt : null;
                            Timestamp tb = b != null ? b.createdAt : null;
                            if (ta == null && tb == null) return 0;
                            if (ta == null) return 1; // a after b
                            if (tb == null) return -1; // a before b
                            // both non-null: compare dates descending
                            return tb.toDate().compareTo(ta.toDate());
                        }
                    });

                    adapter.notifyDataSetChanged();
                    tvResultsInfo.setText("Found " + items.size() + " jobs");
                    tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    tvResultsInfo.setText("Failed to load jobs");
                    tvEmpty.setVisibility(View.VISIBLE);
                    Log.e(TAG, "fetchJobsForCategory failed", e);
                    Toast.makeText(JobsByCategoryActivity.this, "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}