package com.example.woil.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
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

public class FeedbackActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RatingBar ratingBar;
    private EditText etFeedbackComment;
    private MaterialButton btnSubmitFeedback;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String jobId;
    private String toUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        btnBack = findViewById(R.id.btn_back_arrow_settings);
        ratingBar = findViewById(R.id.rating_bar);
        etFeedbackComment = findViewById(R.id.et_feedback_comment);
        btnSubmitFeedback = findViewById(R.id.btn_submit_feedback);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        jobId = getIntent().getStringExtra("jobId");
        toUid = getIntent().getStringExtra("toUid");

        btnBack.setOnClickListener(v -> finish());
        btnSubmitFeedback.setOnClickListener(v -> submitFeedback());
    }

    private void submitFeedback() {
        String fromUid = FirebaseDebugLogger.requireUid(this, mAuth, "feedback_create");
        if (fromUid == null) return;

        Map<String, Object> feedback = new HashMap<>();
        feedback.put("jobId", jobId);
        feedback.put("fromUid", fromUid);
        feedback.put("toUid", toUid);
        feedback.put("rating", ratingBar.getRating());
        feedback.put("comment", etFeedbackComment.getText().toString().trim());
        feedback.put("createdAt", FieldValue.serverTimestamp());

        db.collection("feedback")
                .add(feedback)
                .addOnSuccessListener(doc -> {
                    FirebaseDebugLogger.success("feedback_create", "feedback", doc.getId());
                    Toast.makeText(this, "Feedback submitted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    FirebaseDebugLogger.failure("feedback_create", "feedback", e);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}