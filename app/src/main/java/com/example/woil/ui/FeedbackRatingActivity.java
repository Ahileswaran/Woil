package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;
import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class FeedbackRatingActivity extends AppCompatActivity {

    private CircleImageView profileImageWorker;
    private TextView tvWorkerName;
    private TextView tvWorkerSkill;
    private RatingBar ratingBar;
    private EditText etFeedback;
    private MaterialButton btnAddTip;
    private View layoutTipInput;
    private EditText etTipAmount;
    private MaterialButton btnSubmitFeedback;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String matchId;
    private String workerUid;
    private String jobId;          // for wage fallback
    private double currentWageAgreed = 0.0;
    private double feedbackTip = 0.0;  // tip entered in this screen (separate from PaymentActivity tip)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback_rating);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        matchId = getIntent().getStringExtra("matchId");
        workerUid = getIntent().getStringExtra("workerUid");

        profileImageWorker = findViewById(R.id.profile_image_worker);
        tvWorkerName = findViewById(R.id.tv_worker_name);
        tvWorkerSkill = findViewById(R.id.tv_worker_skill);
        ratingBar = findViewById(R.id.rating_bar);
        etFeedback = findViewById(R.id.et_feedback);
        btnAddTip = findViewById(R.id.btn_add_tip);
        layoutTipInput = findViewById(R.id.layout_tip_input);
        etTipAmount = findViewById(R.id.et_tip_amount);
        btnSubmitFeedback = findViewById(R.id.btn_submit_feedback);

        btnAddTip.setOnClickListener(v -> {
            layoutTipInput.setVisibility(View.VISIBLE);
            btnAddTip.setVisibility(View.GONE);
        });

        btnSubmitFeedback.setOnClickListener(v -> submitReview());

        loadWorkerAndMatchDetails();
    }

    private void loadWorkerAndMatchDetails() {
        if (TextUtils.isEmpty(matchId)) {
            Toast.makeText(this, "Error: missing match ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("matches").document(matchId).get()
                .addOnSuccessListener(matchDoc -> {
                    if (!matchDoc.exists()) {
                        Toast.makeText(this, "Match not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Always grab these fields first
                    if (TextUtils.isEmpty(workerUid)) {
                        workerUid = matchDoc.getString("workerUid");
                    }
                    jobId = matchDoc.getString("jobId");   // stored by ApplyJobActivity

                    // --- Step 1: try wageAgreed on the match doc ---
                    Double wage = matchDoc.getDouble("wageAgreed");
                    if (wage != null && wage > 0) {
                        currentWageAgreed = wage;
                        // Wage known — go straight to worker profile
                        proceedToWorkerProfile(matchDoc);
                    } else if (!TextUtils.isEmpty(jobId)) {
                        // --- Step 2: always fetch from jobs collection (authoritative source) ---
                        // jobs.wageSuggested is the field set by PostJobActivity
                        fetchWageFromJobDoc(jobId, matchDoc);
                    } else {
                        // No jobId available — proceed without wage (shows 0)
                        proceedToWorkerProfile(matchDoc);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load match: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Fetches wageSuggested from the jobs collection — this is the authoritative
     * value set by PostJobActivity (stored as jobs/{jobId}.wageSuggested).
     */
    private void fetchWageFromJobDoc(String jId,
            com.google.firebase.firestore.DocumentSnapshot matchDoc) {
        db.collection("jobs").document(jId).get()
                .addOnSuccessListener(jobDoc -> {
                    if (jobDoc.exists()) {
                        // Primary: wageSuggested (numeric — e.g. 2000.0)
                        Double jobWage = jobDoc.getDouble("wageSuggested");
                        if (jobWage != null && jobWage > 0) {
                            currentWageAgreed = jobWage;
                        }
                        // Fallback inside jobs doc: try wageSuggestedText → parse
                        if (currentWageAgreed <= 0) {
                            String wageText = jobDoc.getString("wageSuggestedText");
                            if (!TextUtils.isEmpty(wageText)) {
                                try {
                                    // Strip "Rs. " prefix and commas, e.g. "Rs. 2,000" → 2000
                                    String cleaned = wageText.replaceAll("[^0-9.]", "");
                                    if (!cleaned.isEmpty()) {
                                        currentWageAgreed = Double.parseDouble(cleaned);
                                    }
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                    proceedToWorkerProfile(matchDoc);
                })
                .addOnFailureListener(e -> {
                    // Jobs fetch failed — continue without wage
                    proceedToWorkerProfile(matchDoc);
                });
    }

    /** Loads worker profile after wage is resolved */
    private void proceedToWorkerProfile(
            com.google.firebase.firestore.DocumentSnapshot matchDoc) {
        if (!TextUtils.isEmpty(workerUid)) {
            fetchWorkerProfile(workerUid);
        } else {
            String workerName = matchDoc.getString("workerName");
            String category   = matchDoc.getString("category");
            tvWorkerName.setText(TextUtils.isEmpty(workerName) ? "Worker" : workerName);
            tvWorkerSkill.setText(TextUtils.isEmpty(category) ? "Cleaning" : category);
        }
    }

    private void fetchWorkerProfile(String uid) {
        db.collection("profiles").document(uid).get()
                .addOnSuccessListener(profileDoc -> {
                    if (profileDoc.exists()) {
                        String first = profileDoc.getString("firstName");
                        String last = profileDoc.getString("lastName");
                        String displayName = profileDoc.getString("displayName");
                        String skill = profileDoc.getString("workerSkill");
                        if (TextUtils.isEmpty(skill)) skill = profileDoc.getString("category");

                        String name = ((safe(first) + " " + safe(last)).trim());
                        if (TextUtils.isEmpty(name)) {
                            name = !TextUtils.isEmpty(displayName) ? displayName : "Worker";
                        }

                        tvWorkerName.setText(name);
                        tvWorkerSkill.setText(TextUtils.isEmpty(skill) ? "Cleaning" : skill);

                        String photoUrl = profileDoc.getString("photoUrl");
                        if (TextUtils.isEmpty(photoUrl)) photoUrl = profileDoc.getString("photo");
                        if (TextUtils.isEmpty(photoUrl)) photoUrl = profileDoc.getString("avatar");

                        if (!TextUtils.isEmpty(photoUrl)) {
                            try {
                                Glide.with(this)
                                        .load(photoUrl)
                                        .placeholder(R.drawable.photo_placeholder)
                                        .error(R.drawable.photo_placeholder)
                                        .into(profileImageWorker);
                            } catch (Exception ignored) {}
                        }
                    }
                });
    }

    private void submitReview() {
        String clientUid = FirebaseDebugLogger.requireUid(this, mAuth, "feedback_create");
        if (clientUid == null) return;
        if (TextUtils.isEmpty(workerUid)) {
            Toast.makeText(this, "Worker ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        final float rating = ratingBar.getRating();
        final String comment = etFeedback.getText().toString().trim();
        
        String tipText = etTipAmount.getText().toString().trim();
        double tipVal = 0.0;
        if (!TextUtils.isEmpty(tipText)) {
            try {
                tipVal = Double.parseDouble(tipText);
            } catch (Exception e) {
                etTipAmount.setError("Invalid tip amount");
                return;
            }
        }
        final double finalTip = tipVal;

        btnSubmitFeedback.setEnabled(false);

        // Update Worker's rating in their profile
        db.collection("profiles").document(workerUid).get()
                .addOnSuccessListener(profileDoc -> {
                    double currentAvgRating = 5.0;
                    long completedJobsCount = 0;

                    if (profileDoc.exists()) {
                        Double ratingVal = profileDoc.getDouble("rating");
                        if (ratingVal != null) {
                            currentAvgRating = ratingVal;
                        }
                        
                        Object jobsVal = profileDoc.get("completedJobs");
                        if (jobsVal instanceof Number) {
                            completedJobsCount = ((Number) jobsVal).longValue();
                        } else {
                            Object altJobs = profileDoc.get("jobsCompleted");
                            if (altJobs instanceof Number) completedJobsCount = ((Number) altJobs).longValue();
                        }
                    }

                    long newJobsCount = completedJobsCount + 1;
                    double newAvgRating = ((currentAvgRating * completedJobsCount) + rating) / newJobsCount;

                    // Update profiles collection
                    Map<String, Object> profileUpdates = new HashMap<>();
                    profileUpdates.put("rating", newAvgRating);
                    profileUpdates.put("completedJobs", newJobsCount);

                    db.collection("profiles").document(workerUid).set(profileUpdates, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                saveFeedbackAndMatchDetails(clientUid, rating, comment, finalTip);
                            })
                            .addOnFailureListener(e -> {
                                btnSubmitFeedback.setEnabled(true);
                                Toast.makeText(FeedbackRatingActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnSubmitFeedback.setEnabled(true);
                    Toast.makeText(FeedbackRatingActivity.this, "Failed to fetch profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveFeedbackAndMatchDetails(String clientUid, float rating, String comment, double tip) {
        // Save review to feedback collection
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("matchId", matchId);
        feedback.put("fromUid", clientUid);
        feedback.put("toUid", workerUid);
        feedback.put("rating", rating);
        feedback.put("comment", comment);
        feedback.put("createdAt", FieldValue.serverTimestamp());

        db.collection("feedback").add(feedback)
                .addOnSuccessListener(docRef -> {
                    // Update match record
                    Map<String, Object> matchUpdates = new HashMap<>();
                    matchUpdates.put("ratingGiven", rating);
                    matchUpdates.put("feedbackGiven", comment);
                    matchUpdates.put("feedbackDocId", docRef.getId());
                    if (tip > 0) {
                        matchUpdates.put("tipAmount", tip);
                        matchUpdates.put("wageAgreed", currentWageAgreed + tip);
                    }

                    db.collection("matches").document(matchId).set(matchUpdates, SetOptions.merge())
                            .continueWithTask(task -> db.collection("matching_requests").document(matchId).set(matchUpdates, SetOptions.merge()))
                            .addOnSuccessListener(unused -> {
                                    Toast.makeText(FeedbackRatingActivity.this, "Feedback submitted!", Toast.LENGTH_SHORT).show();

                                    // ── FIX Bug 1: launch PaymentActivity with correct base wage ──
                                    // currentWageAgreed = base wage (before tip)
                                    // tip in PaymentActivity's own field lets client add/adjust on payment screen
                                    Intent payIntent = new Intent(FeedbackRatingActivity.this, PaymentActivity.class);
                                    payIntent.putExtra(PaymentActivity.EXTRA_JOB_ID,     TextUtils.isEmpty(jobId) ? "" : jobId);
                                    payIntent.putExtra(PaymentActivity.EXTRA_MATCH_ID,   matchId);
                                    payIntent.putExtra(PaymentActivity.EXTRA_WORKER_UID, workerUid);
                                    payIntent.putExtra(PaymentActivity.EXTRA_WORKER_NAME,
                                            tvWorkerName.getText().toString());
                                    // Pass BASE wage — tip in PaymentActivity is entered fresh
                                    double baseWage = currentWageAgreed > 0 ? currentWageAgreed : 0.0;
                                    payIntent.putExtra(PaymentActivity.EXTRA_AMOUNT, baseWage);
                                    startActivity(payIntent);
                                    finish();
                            })
                            .addOnFailureListener(e -> {
                                btnSubmitFeedback.setEnabled(true);
                                Toast.makeText(FeedbackRatingActivity.this, "Feedback saved, but failed to update match details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnSubmitFeedback.setEnabled(true);
                    Toast.makeText(FeedbackRatingActivity.this, "Failed to submit feedback: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String safe(String val) {
        return val == null ? "" : val;
    }
}
