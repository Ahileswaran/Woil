package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ClientJobApplicationsActivity extends AppCompatActivity implements JobApplicationAdapter.Listener {
    private ImageButton btnBack; private TextView tvJobSummary, tvEmpty; private RecyclerView rvApplications; private ProgressBar progressBar;
    private FirebaseFirestore db; private FirebaseAuth auth; private String jobId, currentUid; private ListenerRegistration matchesListener;
    private final List<JobApplicationModel> items = new ArrayList<>(); private JobApplicationAdapter adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_job_applications);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT); getWindow().setNavigationBarColor(Color.TRANSPARENT);
        btnBack=findViewById(R.id.btn_back_arrow_settings); tvJobSummary=findViewById(R.id.tv_job_summary); rvApplications=findViewById(R.id.rv_applications);
        tvEmpty=findViewById(R.id.tv_empty); progressBar=findViewById(R.id.progress_applications);
        db=FirebaseFirestore.getInstance(); auth=FirebaseAuth.getInstance(); currentUid=auth.getCurrentUser()!=null?auth.getCurrentUser().getUid():null;
        jobId=getIntent().getStringExtra("jobId");
        btnBack.setOnClickListener(v -> finish());
        rvApplications.setLayoutManager(new LinearLayoutManager(this)); adapter=new JobApplicationAdapter(items, this); rvApplications.setAdapter(adapter);
        tvJobSummary.setText(TextUtils.isEmpty(jobId) ? "Pending worker applications for your posted jobs" : "Applications for selected job");
    }
    @Override protected void onStart(){ super.onStart(); loadApplications(); }
    @Override protected void onStop(){ if(matchesListener!=null) matchesListener.remove(); matchesListener=null; super.onStop(); }

    private void loadApplications() {
        if (TextUtils.isEmpty(currentUid)) { showEmpty("Please sign in again."); return; }
        progressBar.setVisibility(View.VISIBLE);
        Query query = db.collection("matches").whereEqualTo("clientUid", currentUid).orderBy("createdAt", Query.Direction.DESCENDING);
        if (!TextUtils.isEmpty(jobId)) query = db.collection("matches").whereEqualTo("clientUid", currentUid).whereEqualTo("jobId", jobId);
        if (matchesListener!=null) matchesListener.remove();
        matchesListener = query.addSnapshotListener((snap,e)->{
            progressBar.setVisibility(View.GONE); items.clear();
            if (e != null) { showEmpty("Unable to load applications"); Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show(); return; }
            if (snap == null || snap.isEmpty()) { showEmpty("No applications yet"); return; }
            final int[] remaining = {snap.size()};
            for (DocumentSnapshot doc : snap.getDocuments()) {
                markViewedIfNeeded(doc);
                String docJobId = doc.getString("jobId");
                if (TextUtils.isEmpty(docJobId)) { remaining[0]--; continue; }
                String workerUid = doc.getString("workerUid");
                if (TextUtils.isEmpty(workerUid)) { remaining[0]--; continue; }
                db.collection("profiles").document(workerUid).get().addOnSuccessListener(profile -> {
                    items.add(buildModel(doc, profile)); if (--remaining[0] <= 0) finishPublish();
                }).addOnFailureListener(err -> { items.add(buildModel(doc, null)); if (--remaining[0] <= 0) finishPublish(); });
            }
        });
    }
    private void finishPublish(){ adapter.notifyDataSetChanged(); tvEmpty.setVisibility(items.isEmpty()?View.VISIBLE:View.GONE); rvApplications.setVisibility(items.isEmpty()?View.GONE:View.VISIBLE); }
    private void markViewedIfNeeded(DocumentSnapshot doc) {
        String status = doc.getString("status");
        if ("PENDING".equalsIgnoreCase(status)) {
            HashMap<String,Object> update=new HashMap<>(); update.put("status","VIEWED"); update.put("viewedAt", FieldValue.serverTimestamp());
            doc.getReference().set(update, SetOptions.merge()); db.collection("matching_requests").document(doc.getId()).set(update, SetOptions.merge());
        }
    }
    private JobApplicationModel buildModel(DocumentSnapshot match, DocumentSnapshot profile) {
        Timestamp createdAt = match.getTimestamp("createdAt");
        String timeText = createdAt!=null ? new SimpleDateFormat("MMM dd • hh:mm a", Locale.getDefault()).format(createdAt.toDate()) : "recently";
        String workerName = match.getString("workerName"), role = "Worker", photo = null, location = match.getString("workerLocationText");
        double rating = asDouble(match.get("workerRating")); long completedJobs = 0L;
        if (profile != null && profile.exists()) {
            workerName = first(workerName, profile.getString("displayName"), join(profile.getString("firstName"), profile.getString("lastName")), "Worker");
            role = first(profile.getString("role"), role); photo = first(profile.getString("photoUrl"), profile.getString("photo"), profile.getString("avatar"), null);
            if (TextUtils.isEmpty(location)) location = profile.getString("locationText");
            if (rating <= 0) rating = asDouble(profile.get("rating"));
            Object jobsVal = profile.get("completedJobs"); if (jobsVal instanceof Number) completedJobs = ((Number)jobsVal).longValue();
        } else workerName = first(workerName, "Worker");
        return new JobApplicationModel(match.getId(), match.getString("jobId"), match.getString("workerUid"), workerName, photo, role, location, rating, completedJobs,
                first(match.getString("status"), "PENDING"), timeText, first(match.getString("category"), match.getString("workerSkill"), "Any job"), asDouble(match.get("distanceKm")), asLong(match.get("etaMinutes")),
                match.getString("clientUid"), first(match.getString("jobTitle"), "Job application"));
    }
    private void showEmpty(String message){ items.clear(); adapter.notifyDataSetChanged(); tvEmpty.setVisibility(View.VISIBLE); tvEmpty.setText(message); rvApplications.setVisibility(View.GONE); }
    @Override public void onApplicationClick(JobApplicationModel item) {
        Intent intent = new Intent(this, ClientApplicationDetailActivity.class);
        intent.putExtra(ClientApplicationDetailActivity.EXTRA_MATCH_ID, item.getMatchId());
        intent.putExtra(ClientApplicationDetailActivity.EXTRA_JOB_ID, item.getJobId());
        intent.putExtra(ClientApplicationDetailActivity.EXTRA_WORKER_UID, item.getWorkerUid());
        intent.putExtra(ClientApplicationDetailActivity.EXTRA_WORKER_NAME, item.getWorkerName());
        intent.putExtra(ClientApplicationDetailActivity.EXTRA_WORKER_ROLE, item.getWorkerRole());
        intent.putExtra(ClientApplicationDetailActivity.EXTRA_WORKER_PHOTO, item.getWorkerPhotoUrl());
        intent.putExtra(ClientApplicationDetailActivity.EXTRA_WORKER_LOCATION, item.getWorkerLocationText());
        intent.putExtra(ClientApplicationDetailActivity.EXTRA_WORKER_RATING, item.getWorkerRating());
        intent.putExtra(ClientApplicationDetailActivity.EXTRA_COMPLETED_JOBS, item.getCompletedJobs());
        intent.putExtra(ClientApplicationDetailActivity.EXTRA_STATUS, item.getStatus());
        intent.putExtra(ClientApplicationDetailActivity.EXTRA_DISTANCE_KM, item.getDistanceKm());
        intent.putExtra(ClientApplicationDetailActivity.EXTRA_ETA_MIN, item.getEtaMinutes());
        intent.putExtra(ClientApplicationDetailActivity.EXTRA_CATEGORY, item.getCategory());
        intent.putExtra(ClientApplicationDetailActivity.EXTRA_CLIENT_UID, item.getClientUid());
        intent.putExtra(ClientApplicationDetailActivity.EXTRA_JOB_TITLE, item.getJobTitle());
        startActivity(intent);
    }
    @Override public void onAcceptClick(JobApplicationModel item){ updateStatus(item, "ACCEPTED"); }
    @Override public void onRejectClick(JobApplicationModel item){ updateStatus(item, "REJECTED"); }
    private void updateStatus(JobApplicationModel item, String status) {
        HashMap<String,Object> updates=new HashMap<>(); updates.put("status", status); updates.put("updatedAt", FieldValue.serverTimestamp()); if ("ACCEPTED".equals(status)) updates.put("acceptedAt", FieldValue.serverTimestamp());
        db.collection("matches").document(item.getMatchId()).set(updates, SetOptions.merge())
                .continueWithTask(task -> db.collection("matching_requests").document(item.getMatchId()).set(updates, SetOptions.merge()))
                .addOnSuccessListener(unused -> { if ("ACCEPTED".equals(status) && !TextUtils.isEmpty(item.getJobId())) {
                        HashMap<String,Object> job=new HashMap<>(); job.put("assignedUid", item.getWorkerUid()); job.put("status", "MATCHED"); job.put("updatedAt", FieldValue.serverTimestamp());
                        db.collection("jobs").document(item.getJobId()).set(job, SetOptions.merge());
                    } Toast.makeText(this, "Application " + status.toLowerCase(Locale.ROOT), Toast.LENGTH_SHORT).show();})
                .addOnFailureListener(e -> Toast.makeText(this, "Status update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
    private static String first(String... vals){ if(vals==null) return ""; for(String v:vals) if(!TextUtils.isEmpty(v)) return v; return ""; }
    private static String join(String a,String b){ String v=(first(a)+" "+first(b)).trim(); return TextUtils.isEmpty(v)?"":v; }
    private static double asDouble(Object v){ return v instanceof Number ? ((Number)v).doubleValue() : 0.0; }
    private static long asLong(Object v){ return v instanceof Number ? ((Number)v).longValue() : 0L; }
}