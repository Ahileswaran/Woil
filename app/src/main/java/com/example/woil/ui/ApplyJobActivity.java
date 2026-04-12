package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ApplyJobActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvApplyJobTitle;
    private TextView tvApplyWage;
    private TextView tvApplyTime;
    private TextView tvApplyLocation;
    private MaterialButton btnConfirmApply;
    private MaterialButton btnCancelApply;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String jobId;
    private String clientUid;
    private String title;
    private String wageText;
    private Double wageSuggested;
    private String timeText;
    private String locationText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_job);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnBack = findViewById(R.id.btn_back_arrow_settings);
        tvApplyJobTitle = findViewById(R.id.tv_apply_job_title);
        tvApplyWage = findViewById(R.id.tv_apply_wage);
        tvApplyTime = findViewById(R.id.tv_apply_time);
        tvApplyLocation = findViewById(R.id.tv_apply_location);
        btnConfirmApply = findViewById(R.id.btn_confirm_apply);
        btnCancelApply = findViewById(R.id.btn_cancel_apply);

        Intent intent = getIntent();
        jobId = intent.getStringExtra("jobId");
        clientUid = intent.getStringExtra("clientUid");
        title = intent.getStringExtra("title");
        wageText = intent.getStringExtra("wageText");
        timeText = intent.getStringExtra("timeText");
        locationText = intent.getStringExtra("locationText");

        if (intent.hasExtra("wageSuggested")) {
            wageSuggested = intent.getDoubleExtra("wageSuggested", 0.0);
        }

        tvApplyJobTitle.setText(safe(title, "Untitled Job"));
        tvApplyWage.setText("Wage: " + safe(wageText, "Not specified"));
        tvApplyTime.setText("Time: " + safe(timeText, "Not specified"));
        tvApplyLocation.setText("Location: " + safe(locationText, "Not specified"));

        btnBack.setOnClickListener(v -> finish());
        btnCancelApply.setOnClickListener(v -> finish());

        btnConfirmApply.setOnClickListener(v -> submitApplication());
    }

    private void submitApplication() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_LONG).show();
            return;
        }

        String workerUid = mAuth.getCurrentUser().getUid();

        if (TextUtils.isEmpty(jobId) || TextUtils.isEmpty(clientUid)) {
            Toast.makeText(this, "Missing job info", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> match = new HashMap<>();
        match.put("jobId", jobId);
        match.put("workerUid", workerUid);
        match.put("clientUid", clientUid);
        match.put("status", "PENDING");
        match.put("acceptedAt", null);
        match.put("wageAgreed", wageSuggested != null && wageSuggested > 0 ? wageSuggested : null);
        match.put("createdAt", FieldValue.serverTimestamp());

        btnConfirmApply.setEnabled(false);

        db.collection("matches")
                .add(match)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Application sent", Toast.LENGTH_SHORT).show();
                    Intent result = new Intent();
                    result.putExtra("matchId", docRef.getId());
                    setResult(RESULT_OK, result);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirmApply.setEnabled(true);
                    Toast.makeText(this, "Failed to apply: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String safe(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }
}