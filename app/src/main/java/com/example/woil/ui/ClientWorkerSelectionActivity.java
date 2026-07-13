package com.example.woil.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;

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

    private EditText etHumanHours;
    private EditText etTotalWage;
    private Map<String, Double> currentMarketRates = new HashMap<>();

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
        etHumanHours = findViewById(R.id.et_human_hours);
        etTotalWage = findViewById(R.id.et_total_wage);
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

        View layoutWageCalculation = findViewById(R.id.layout_wage_calculation);
        if (layoutWageCalculation != null) {
            layoutWageCalculation.setVisibility(View.VISIBLE);
        }

        if (etHumanHours != null) {
            etHumanHours.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    recalculateWage();
                }
            });
        }
        recalculateWage();

        // Fetch dynamic market rates from Firestore so clients get live suggested wages
        db.collection("system_config").document("market_wages").get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && snap.exists()) {
                        currentMarketRates.put("cleaning", snap.getDouble("cleaning"));
                        currentMarketRates.put("gardening", snap.getDouble("gardening"));
                        currentMarketRates.put("plumbing", snap.getDouble("plumbing"));
                        currentMarketRates.put("housekeeping", snap.getDouble("housekeeping"));
                        currentMarketRates.put("laundry", snap.getDouble("laundry"));
                        currentMarketRates.put("caregiving", snap.getDouble("caregiving"));
                        currentMarketRates.put("other", snap.getDouble("other"));
                        recalculateWage(); // Recalculate once live rates are fetched
                    }
                });

        btnConfirmApply.setText("Confirm Match");
        btnConfirmApply.setOnClickListener(v -> createPendingMatchRequest());
        btnCancelApply.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());

        setupShowcaseVideos();
    }

    private void recalculateWage() {
        if (etHumanHours == null || etTotalWage == null) return;
        
        double hours = 1.0;
        try {
            String hoursStr = etHumanHours.getText().toString().trim();
            if (!hoursStr.isEmpty()) {
                hours = Double.parseDouble(hoursStr);
            }
        } catch (NumberFormatException e) {
            hours = 0.0;
        }

        WageCalculator.Input input = new WageCalculator.Input();
        input.category = selectedCategory;
        input.distanceKm = workerDistanceKm;
        input.humanHours = hours;
        input.marketRates = currentMarketRates;

        WageCalculator.Result result = WageCalculator.calculate(input);
        etTotalWage.setText(String.format(Locale.getDefault(), "%.2f", result.totalAmount));
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

        double finalWage = 0.0;
        if (etTotalWage != null) {
            try {
                String wageStr = etTotalWage.getText().toString().trim();
                if (!wageStr.isEmpty()) {
                    finalWage = Double.parseDouble(wageStr);
                }
            } catch (NumberFormatException ignored) {}
        }
        request.put("wageAgreed", finalWage);

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
                    Intent intent = new Intent(this, ClientActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirmApply.setEnabled(true);
                    FirebaseDebugLogger.failure("matching_request_create", "matches+matching_requests", e);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
