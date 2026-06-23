package com.example.woil.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.woil.R;
import com.example.woil.adapters.SkillVideoAdapter;
import com.example.woil.models.SkillVideo;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ClientApplicationDetailActivity extends AppCompatActivity {
    public static final String EXTRA_MATCH_ID="matchId", EXTRA_JOB_ID="jobId", EXTRA_WORKER_UID="workerUid", EXTRA_WORKER_NAME="workerName",
            EXTRA_WORKER_ROLE="workerRole", EXTRA_WORKER_PHOTO="workerPhoto", EXTRA_WORKER_LOCATION="workerLocation", EXTRA_WORKER_RATING="workerRating",
            EXTRA_COMPLETED_JOBS="completedJobs", EXTRA_STATUS="status", EXTRA_DISTANCE_KM="distanceKm", EXTRA_ETA_MIN="etaMin", EXTRA_CATEGORY="category",
            EXTRA_CLIENT_UID="clientUid", EXTRA_JOB_TITLE="jobTitle";

    private FirebaseFirestore db;
    private String matchId, jobId, workerUid, workerName, workerRole, workerPhoto, workerLocation, status, category, clientUid, jobTitle;
    private double workerRating, distanceKm;
    private long completedJobs, etaMin;

    private RecyclerView recyclerSkillVideos;
    private TextView tvShowcaseTitle;
    private final ArrayList<SkillVideo> skillVideoList = new ArrayList<>();
    private SkillVideoAdapter videoAdapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_application_detail);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        db = FirebaseFirestore.getInstance();

        Intent i = getIntent();
        matchId=i.getStringExtra(EXTRA_MATCH_ID); jobId=i.getStringExtra(EXTRA_JOB_ID); workerUid=i.getStringExtra(EXTRA_WORKER_UID);
        workerName=i.getStringExtra(EXTRA_WORKER_NAME); workerRole=i.getStringExtra(EXTRA_WORKER_ROLE); workerPhoto=i.getStringExtra(EXTRA_WORKER_PHOTO);
        workerLocation=i.getStringExtra(EXTRA_WORKER_LOCATION); status=i.getStringExtra(EXTRA_STATUS); category=i.getStringExtra(EXTRA_CATEGORY);
        clientUid=i.getStringExtra(EXTRA_CLIENT_UID); jobTitle=i.getStringExtra(EXTRA_JOB_TITLE);
        workerRating=i.getDoubleExtra(EXTRA_WORKER_RATING, 0.0); distanceKm=i.getDoubleExtra(EXTRA_DISTANCE_KM, 0.0);
        completedJobs=i.getLongExtra(EXTRA_COMPLETED_JOBS, 0L); etaMin=i.getLongExtra(EXTRA_ETA_MIN, 0L);

        ((TextView)findViewById(R.id.tv_title)).setText(first(workerName, "Worker application"));
        ((TextView)findViewById(R.id.tv_status)).setText("Status: " + first(status, "PENDING"));
        ((TextView)findViewById(R.id.tv_role)).setText(first(workerRole, "Worker"));
        ((TextView)findViewById(R.id.tv_job_title)).setText(first(jobTitle, "Selected job"));
        ((TextView)findViewById(R.id.tv_category)).setText("Category: " + first(category, "Not set"));
        ((TextView)findViewById(R.id.tv_rating)).setText(String.format(Locale.getDefault(), "Rating %.1f", workerRating));
        ((TextView)findViewById(R.id.tv_jobs)).setText(String.format(Locale.getDefault(), "Completed jobs: %d", completedJobs));
        ((TextView)findViewById(R.id.tv_distance)).setText(String.format(Locale.getDefault(), "Distance: %.1f km", distanceKm));
        ((TextView)findViewById(R.id.tv_eta)).setText(String.format(Locale.getDefault(), "Estimated arrival: %d min", etaMin));
        ((TextView)findViewById(R.id.tv_location)).setText(first(workerLocation, "Location not available"));
        ImageView image = findViewById(R.id.iv_worker);
        if (!TextUtils.isEmpty(workerPhoto)) Glide.with(this).load(workerPhoto).placeholder(R.drawable.ic_person).error(R.drawable.ic_person).circleCrop().into(image);
        else image.setImageResource(R.drawable.ic_person);

        recyclerSkillVideos = findViewById(R.id.recycler_skill_videos);
        tvShowcaseTitle = findViewById(R.id.tv_showcase_title);
        setupShowcaseVideos();

        findViewById(R.id.btn_back_arrow_settings).setOnClickListener(v -> finish());
        MaterialButton btnAccept=findViewById(R.id.btn_accept_detail), btnReject=findViewById(R.id.btn_reject_detail), btnMessage=findViewById(R.id.btn_message_worker);
        boolean pending = "PENDING".equalsIgnoreCase(status) || "VIEWED".equalsIgnoreCase(status);
        btnAccept.setEnabled(pending); btnReject.setEnabled(pending);
        btnAccept.setOnClickListener(v -> updateStatus("ACCEPTED"));
        btnReject.setOnClickListener(v -> updateStatus("REJECTED"));
        btnMessage.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatHostActivity.class);
            intent.putExtra(ChatFragment.ARG_CONTACT_UID, workerUid);
            intent.putExtra(ChatFragment.ARG_CONTACT_NAME, workerName);
            intent.putExtra(ChatFragment.ARG_CONTACT_ROLE, workerRole);
            intent.putExtra(ChatFragment.ARG_CONTACT_PHOTO, workerPhoto);
            intent.putExtra(ChatHostActivity.EXTRA_JOB_ID, jobId);
            startActivity(intent);
        });
    }

    private void updateStatus(String newStatus) {
        if (TextUtils.isEmpty(matchId)) { Toast.makeText(this, "Missing application id", Toast.LENGTH_SHORT).show(); return; }
        Map<String,Object> updates=new HashMap<>(); updates.put("status", newStatus); updates.put("updatedAt", FieldValue.serverTimestamp());
        if ("ACCEPTED".equals(newStatus)) updates.put("acceptedAt", FieldValue.serverTimestamp());
        db.collection("matches").document(matchId).set(updates, SetOptions.merge())
                .continueWithTask(task -> db.collection("matching_requests").document(matchId).set(updates, SetOptions.merge()))
                .addOnSuccessListener(unused -> {
                    if ("ACCEPTED".equals(newStatus) && !TextUtils.isEmpty(jobId)) {
                        Map<String,Object> jobUpdate=new HashMap<>(); jobUpdate.put("assignedUid", workerUid); jobUpdate.put("status", "MATCHED"); jobUpdate.put("updatedAt", FieldValue.serverTimestamp());
                        db.collection("jobs").document(jobId).set(jobUpdate, SetOptions.merge());
                    }
                    status = newStatus; ((TextView)findViewById(R.id.tv_status)).setText("Status: " + newStatus);
                    Toast.makeText(this, "Application " + newStatus.toLowerCase(Locale.ROOT), Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
    private void setupShowcaseVideos() {
        videoAdapter = new SkillVideoAdapter(this, skillVideoList, new SkillVideoAdapter.OnVideoActionListener() {
            @Override
            public void onPreview(SkillVideo video) {
                playVideo(video);
            }

            @Override
            public void onEdit(SkillVideo video, int position) {}

            @Override
            public void onDelete(SkillVideo video, int position) {}
        }, true);

        recyclerSkillVideos.setLayoutManager(new LinearLayoutManager(this));
        recyclerSkillVideos.setAdapter(videoAdapter);

        if (!TextUtils.isEmpty(workerUid)) {
            db.collection("profile_showcase_skill_videos")
                    .whereEqualTo("uid", workerUid)
                    .get()
                    .addOnSuccessListener(snap -> {
                        skillVideoList.clear();
                        if (snap != null && !snap.isEmpty()) {
                            for (DocumentSnapshot doc : snap.getDocuments()) {
                                String title = doc.getString("title");
                                String category = doc.getString("category");
                                String description = doc.getString("description");
                                String status = doc.getString("status");
                                String videoUrl = doc.getString("videoUrl");
                                if (TextUtils.isEmpty(videoUrl)) videoUrl = doc.getString("videoUri");
                                skillVideoList.add(new SkillVideo(
                                        doc.getId(),
                                        TextUtils.isEmpty(title) ? "Skill video" : title,
                                        TextUtils.isEmpty(category) ? "Other" : category,
                                        TextUtils.isEmpty(description) ? "" : description,
                                        TextUtils.isEmpty(status) ? "PENDING" : status,
                                        videoUrl
                                ));
                            }
                        }
                        if (!skillVideoList.isEmpty()) {
                            tvShowcaseTitle.setVisibility(View.VISIBLE);
                            recyclerSkillVideos.setVisibility(View.VISIBLE);
                        } else {
                            tvShowcaseTitle.setVisibility(View.GONE);
                            recyclerSkillVideos.setVisibility(View.GONE);
                        }
                        videoAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        tvShowcaseTitle.setVisibility(View.GONE);
                        recyclerSkillVideos.setVisibility(View.GONE);
                    });
        }
    }

    private void playVideo(SkillVideo video) {
        if (video == null || video.getVideoUri() == null) {
            Toast.makeText(this, "No video URL available for preview", Toast.LENGTH_SHORT).show();
            return;
        }
        java.util.ArrayList<String> videoUrls = new java.util.ArrayList<>();
        int startIndex = 0;
        for (int i = 0; i < skillVideoList.size(); i++) {
            SkillVideo v = skillVideoList.get(i);
            if (v.getVideoUri() != null) {
                videoUrls.add(v.getVideoUri().toString());
                if (v.getId().equals(video.getId())) {
                    startIndex = videoUrls.size() - 1;
                }
            }
        }
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putStringArrayListExtra("video_urls", videoUrls);
        intent.putExtra("start_index", startIndex);
        startActivity(intent);
    }

    private static String first(String a, String b){ return TextUtils.isEmpty(a)?b:a; }
}