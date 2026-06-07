package com.example.woil.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.R;
import com.example.woil.adapters.SkillVideoAdapter;
import com.example.woil.models.SkillVideo;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ClientWorkerSelectionActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String workerUid;
    private String workerName;
    private String workerSkill;
    private double workerRating;
    private double workerDistanceKm;
    private long workerEtaMinutes;
    private String workerLocationText;
    private String workerArea;
    private String workerProvince;
    private String clientAddress;
    private String clientArea;
    private String clientProvince;
    private double clientLat;
    private double clientLng;
    private String selectedCategory;
    private String matchLevel;

    private RecyclerView recyclerSkillVideos;
    private TextView tvShowcaseTitle;
    private final ArrayList<SkillVideo> skillVideoList = new ArrayList<>();
    private SkillVideoAdapter videoAdapter;
    private MaterialButton btnConfirmApply;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_job);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        workerUid = getIntent().getStringExtra("workerUid");
        workerName = getIntent().getStringExtra("workerName");
        workerSkill = getIntent().getStringExtra("workerSkill");
        workerRating = getIntent().getDoubleExtra("workerRating", 0.0);
        workerDistanceKm = getIntent().getDoubleExtra("workerDistanceKm", 0.0);
        workerEtaMinutes = getIntent().getLongExtra("workerEtaMinutes", 0L);
        workerLocationText = getIntent().getStringExtra("workerLocationText");
        workerArea = getIntent().getStringExtra("workerArea");
        workerProvince = getIntent().getStringExtra("workerProvince");
        clientAddress = getIntent().getStringExtra("clientAddress");
        clientArea = getIntent().getStringExtra("clientArea");
        clientProvince = getIntent().getStringExtra("clientProvince");
        clientLat = getIntent().getDoubleExtra("clientLat", 0.0);
        clientLng = getIntent().getDoubleExtra("clientLng", 0.0);
        selectedCategory = getIntent().getStringExtra("selectedCategory");
        matchLevel = getIntent().getStringExtra("matchLevel");

        ImageButton btnBack = findViewById(R.id.btn_back_arrow_settings);
        TextView settingsTitle = findViewById(R.id.settings_title);
        TextView tvConfirmTitle = findViewById(R.id.tv_confirm_title);
        TextView tvApplyJobTitle = findViewById(R.id.tv_apply_job_title);
        TextView tvApplyWage = findViewById(R.id.tv_apply_wage);
        TextView tvApplyTime = findViewById(R.id.tv_apply_time);
        TextView tvApplyLocation = findViewById(R.id.tv_apply_location);
        TextView tvApplyNote = findViewById(R.id.tv_apply_note);
        btnConfirmApply = findViewById(R.id.btn_confirm_apply);
        MaterialButton btnCancelApply = findViewById(R.id.btn_cancel_apply);
        recyclerSkillVideos = findViewById(R.id.recycler_skill_videos);
        tvShowcaseTitle = findViewById(R.id.tv_showcase_title);

        settingsTitle.setText("Select Worker");
        if (tvConfirmTitle != null) {
            tvConfirmTitle.setText("Confirm Worker Match");
        }
        tvApplyJobTitle.setText("Worker: " + (TextUtils.isEmpty(workerName) ? "Worker" : workerName));
        tvApplyWage.setText("Skill: " + (TextUtils.isEmpty(workerSkill) ? "General" : workerSkill) + " (★ " + String.format(Locale.getDefault(), "%.1f", workerRating) + ")");
        tvApplyTime.setText("Distance: " + String.format(Locale.getDefault(), "%.1f km", workerDistanceKm) + " • ETA: " + workerEtaMinutes + " min");
        tvApplyLocation.setText("Location: " + (TextUtils.isEmpty(workerLocationText) ? "Not specified" : workerLocationText));
        tvApplyNote.setText("Review the worker's showcase videos and skills below before confirming the matching request.");

        btnConfirmApply.setText("Confirm Match");
        btnConfirmApply.setOnClickListener(v -> createPendingMatchRequest());
        btnCancelApply.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());

        setupShowcaseVideos();
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
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_video_preview);
        VideoView dialogVideoView = dialog.findViewById(R.id.dialog_video_view);
        Uri videoUri = video != null ? video.getVideoUri() : null;
        if (videoUri != null) {
            dialogVideoView.setVideoURI(videoUri);
            dialogVideoView.start();
        } else {
            Toast.makeText(this, "No video URL available for preview", Toast.LENGTH_SHORT).show();
        }
        dialog.show();
    }

    private void createPendingMatchRequest() {
        String clientUid = FirebaseDebugLogger.requireUid(this, mAuth, "matching_request_create");
        if (clientUid == null) return;

        btnConfirmApply.setEnabled(false);

        DocumentReference matchRef = db.collection("matches").document();
        DocumentReference requestRef = db.collection("matching_requests").document(matchRef.getId());

        Map<String, Object> request = new HashMap<>();
        request.put("matchId", matchRef.getId());
        request.put("clientUid", clientUid);
        request.put("workerUid", workerUid);
        request.put("workerName", workerName);
        request.put("workerSkill", workerSkill);
        request.put("workerRating", workerRating);
        request.put("category", selectedCategory);
        request.put("clientAddress", clientAddress);
        request.put("clientArea", clientArea);
        request.put("clientProvince", clientProvince);
        request.put("workerLocationText", workerLocationText);
        request.put("workerArea", workerArea);
        request.put("workerProvince", workerProvince);
        request.put("distanceKm", workerDistanceKm);
        request.put("etaMinutes", workerEtaMinutes);
        request.put("matchLevel", matchLevel);
        request.put("status", "PENDING");
        request.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> clientLocation = new HashMap<>();
        clientLocation.put("lat", clientLat);
        clientLocation.put("lng", clientLng);
        request.put("clientLocation", clientLocation);

        WriteBatch batch = db.batch();
        batch.set(matchRef, request);
        batch.set(requestRef, request);

        batch.commit()
                .addOnSuccessListener(unused -> {
                    FirebaseDebugLogger.success("matching_request_create", "matches+matching_requests", matchRef.getId());
                    Toast.makeText(this, "Match request sent", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirmApply.setEnabled(true);
                    FirebaseDebugLogger.failure("matching_request_create", "matches+matching_requests", e);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
