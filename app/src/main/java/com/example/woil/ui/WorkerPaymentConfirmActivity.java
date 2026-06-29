package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Locale;

/**
 * WorkerPaymentConfirmActivity — shown to the worker as an auto-popup when
 * the client confirms a PENDING_CASH payment addressed to this worker.
 *
 * This activity is launched automatically from MainActivity / HomeFragment
 * via a Firestore real-time listener whenever a new PENDING_CASH payment
 * arrives for the current logged-in worker.
 *
 * The worker taps "I Received the Cash" → status changes to CONFIRMED_CASH.
 */
public class WorkerPaymentConfirmActivity extends AppCompatActivity {

    private static final String TAG = "WorkerPaymentConfirm";

    // ── Extras passed when launching ──────────────────────────────────────────
    public static final String EXTRA_PAYMENT_ID    = "paymentId";
    public static final String EXTRA_MATCH_ID      = "matchId";
    public static final String EXTRA_AMOUNT        = "amount";
    public static final String EXTRA_TIP           = "tip";
    public static final String EXTRA_TOTAL_AMOUNT  = "totalAmount";
    public static final String EXTRA_CLIENT_NAME   = "clientName";
    public static final String EXTRA_JOB_TITLE     = "jobTitle";

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView       tvAmount, tvFrom, tvJob;
    private MaterialButton btnConfirm, btnNotYet;

    // ── Data ──────────────────────────────────────────────────────────────────
    private String paymentId, matchId;
    private double totalAmount;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseFirestore    db;
    private FirebaseAuth         mAuth;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_payment_confirm);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Read extras
        paymentId   = getIntent().getStringExtra(EXTRA_PAYMENT_ID);
        matchId     = getIntent().getStringExtra(EXTRA_MATCH_ID);
        totalAmount = getIntent().getDoubleExtra(EXTRA_TOTAL_AMOUNT, 0.0);
        double tip  = getIntent().getDoubleExtra(EXTRA_TIP, 0.0);

        String clientName = getIntent().getStringExtra(EXTRA_CLIENT_NAME);
        String jobTitle   = getIntent().getStringExtra(EXTRA_JOB_TITLE);

        // Bind views
        tvAmount  = findViewById(R.id.tv_confirm_amount);
        tvFrom    = findViewById(R.id.tv_confirm_from);
        tvJob     = findViewById(R.id.tv_confirm_job);
        btnConfirm = findViewById(R.id.btn_confirm_received);
        btnNotYet  = findViewById(R.id.btn_not_yet);

        ImageButton btnBack = findViewById(R.id.btn_back_confirm);
        btnBack.setOnClickListener(v -> finish());

        // Populate
        tvAmount.setText(formatRs(totalAmount));
        tvFrom.setText("From: " + (clientName != null ? clientName : "Client"));
        tvJob.setText(jobTitle != null ? jobTitle : "Job");

        // Button actions
        btnConfirm.setOnClickListener(v -> confirmCashReceived());
        btnNotYet.setOnClickListener(v -> finish());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void confirmCashReceived() {
        if (paymentId == null || paymentId.isEmpty()) {
            Toast.makeText(this, "Error: payment ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirm.setEnabled(false);
        btnConfirm.setText("Confirming…");

        db.collection("payments").document(paymentId)
                .update(
                        "status",            "CONFIRMED_CASH",
                        "workerConfirmedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {
                    // Also update the match document
                    if (matchId != null && !matchId.isEmpty()) {
                        db.collection("matches").document(matchId)
                                .update(
                                        "paymentStatus", "CONFIRMED_CASH",
                                        "updatedAt",     FieldValue.serverTimestamp()
                                );
                    }
                    Toast.makeText(this,
                            "✅ Payment confirmed! Rs. " + Math.round(totalAmount),
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("✅  Yes, I Received the Cash");
                    Toast.makeText(this,
                            "Confirmation failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private String formatRs(double val) {
        return String.format(Locale.getDefault(), "Rs. %,d", Math.round(val));
    }
}
