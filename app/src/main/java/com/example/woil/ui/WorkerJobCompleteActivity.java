package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Arrays;
import java.util.Locale;

/**
 * WorkerJobCompleteActivity — shown immediately after the worker presses
 * "End Work" (AssignedJobMapActivity closes and navigates here).
 *
 * Purpose:
 *   1. Shows the worker a "Job Complete — Waiting for client payment" screen.
 *   2. Listens to the Firestore `payments` collection for a PENDING_CASH
 *      document addressed to this worker + this matchId.
 *   3. When found → auto-launches WorkerPaymentConfirmActivity.
 *   4. Also handles PAID_CARD (client paid by card) → shows success directly.
 *
 * Extras expected from AssignedJobMapActivity:
 *   "matchId"    (String)
 *   "workerUid"  (String)
 *   "jobTitle"   (String, optional)
 *   "wageAmount" (double, optional — shown as expected amount)
 */
public class WorkerJobCompleteActivity extends AppCompatActivity {

    public static final String EXTRA_MATCH_ID   = "matchId";
    public static final String EXTRA_WORKER_UID = "workerUid";
    public static final String EXTRA_JOB_TITLE  = "jobTitle";
    public static final String EXTRA_WAGE_AMOUNT = "wageAmount";

    private String matchId;
    private String workerUid;
    private boolean paymentPopupShown = false;

    private FirebaseFirestore db;
    private ListenerRegistration paymentListener;

    private TextView tvStatus, tvJobTitle, tvExpectedAmount;
    private View progressRing;
    private MaterialButton btnGoHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_job_complete);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        db = FirebaseFirestore.getInstance();

        matchId   = getIntent().getStringExtra(EXTRA_MATCH_ID);
        workerUid = getIntent().getStringExtra(EXTRA_WORKER_UID);
        String jobTitle  = getIntent().getStringExtra(EXTRA_JOB_TITLE);
        double wageAmount = getIntent().getDoubleExtra(EXTRA_WAGE_AMOUNT, 0.0);

        // Fallback workerUid from Firebase Auth
        if (TextUtils.isEmpty(workerUid) && FirebaseAuth.getInstance().getCurrentUser() != null) {
            workerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        tvStatus         = findViewById(R.id.tv_complete_status);
        tvJobTitle       = findViewById(R.id.tv_complete_job_title);
        tvExpectedAmount = findViewById(R.id.tv_complete_expected_amount);
        progressRing     = findViewById(R.id.progress_waiting);
        btnGoHome        = findViewById(R.id.btn_go_home);

        tvJobTitle.setText(TextUtils.isEmpty(jobTitle) ? "Job" : jobTitle);
        if (wageAmount > 0) {
            tvExpectedAmount.setText("Expected: " + formatRs(wageAmount));
            tvExpectedAmount.setVisibility(View.VISIBLE);
        }

        btnGoHome.setOnClickListener(v -> goHome());

        listenForPayment();
    }

    // ── Firestore listener for payment ────────────────────────────────────────

    private void listenForPayment() {
        if (TextUtils.isEmpty(matchId) && TextUtils.isEmpty(workerUid)) return;

        // Query by matchId if available, otherwise by workerUid
        com.google.firebase.firestore.Query query;
        if (!TextUtils.isEmpty(matchId)) {
            query = db.collection("payments").whereEqualTo("matchId", matchId);
        } else {
            query = db.collection("payments")
                    .whereEqualTo("workerUid", workerUid)
                    .whereIn("status", java.util.Arrays.asList("PENDING_CASH", "PAID_CARD"));
        }

        paymentListener = query.addSnapshotListener((snap, e) -> {
            if (e != null || snap == null || snap.isEmpty()) return;

            com.google.firebase.firestore.DocumentSnapshot doc = snap.getDocuments().get(0);
            String status = doc.getString("status");

            if ("PAID_CARD".equalsIgnoreCase(status)) {
                // Client paid by card — show direct success
                showPaymentDone(doc);
            } else if ("PENDING_CASH".equalsIgnoreCase(status) && !paymentPopupShown) {
                // Client confirmed cash — worker must acknowledge receipt
                paymentPopupShown = true;
                launchCashConfirmPopup(doc);
            } else if ("CONFIRMED_CASH".equalsIgnoreCase(status)) {
                // Already confirmed by worker — show success
                showPaymentDone(doc);
            }
        });
    }

    private void launchCashConfirmPopup(com.google.firebase.firestore.DocumentSnapshot doc) {
        double total     = doc.getDouble("totalAmount") != null ? doc.getDouble("totalAmount") : 0.0;
        double tip       = doc.getDouble("tip")         != null ? doc.getDouble("tip")         : 0.0;
        String pmatchId  = doc.getString("matchId");
        String clientName= doc.getString("clientName");
        String jobTitle  = doc.getString("jobTitle");

        Intent intent = new Intent(this, WorkerPaymentConfirmActivity.class);
        intent.putExtra(WorkerPaymentConfirmActivity.EXTRA_PAYMENT_ID,   doc.getId());
        intent.putExtra(WorkerPaymentConfirmActivity.EXTRA_MATCH_ID,     pmatchId);
        intent.putExtra(WorkerPaymentConfirmActivity.EXTRA_TOTAL_AMOUNT, total);
        intent.putExtra(WorkerPaymentConfirmActivity.EXTRA_TIP,          tip);
        intent.putExtra(WorkerPaymentConfirmActivity.EXTRA_CLIENT_NAME,  clientName);
        intent.putExtra(WorkerPaymentConfirmActivity.EXTRA_JOB_TITLE,    jobTitle);
        startActivity(intent);
    }

    private void showPaymentDone(com.google.firebase.firestore.DocumentSnapshot doc) {
        double total  = doc.getDouble("totalAmount") != null ? doc.getDouble("totalAmount") : 0.0;
        String method = doc.getString("paymentMethod");

        tvStatus.setText("✅  Payment Received!");
        tvStatus.setTextColor(Color.parseColor("#1E8A3B"));
        if (progressRing != null) progressRing.setVisibility(View.GONE);
        tvExpectedAmount.setText(formatRs(total) + " • " +
                ("CARD".equalsIgnoreCase(method) ? "💳 Card" : "💵 Cash"));
        tvExpectedAmount.setVisibility(View.VISIBLE);
        btnGoHome.setText("Go to Home");
    }

    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private String formatRs(double val) {
        return String.format(Locale.getDefault(), "Rs. %,d", Math.round(val));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (paymentListener != null) {
            paymentListener.remove();
            paymentListener = null;
        }
    }

    // Prevent back press from navigating back to the map
    @Override
    public void onBackPressed() {
        goHome();
    }
}
