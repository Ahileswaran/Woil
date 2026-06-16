package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * PaymentActivity — Hybrid payment screen supporting:
 *   1. Ready Cash  → status: PENDING_CASH (worker confirms receipt separately)
 *   2. Card / Stripe → PaymentSheet → status: PAID_CARD
 *
 * Launched from FeedbackRatingActivity after feedback is submitted.
 * Extras expected:
 *   "jobId"     (String)
 *   "matchId"   (String)
 *   "workerUid" (String)
 *   "workerName"(String, optional display)
 *   "amount"    (double)
 */
public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";

    // ── Intent extras ─────────────────────────────────────────────────────────
    public static final String EXTRA_JOB_ID     = "jobId";
    public static final String EXTRA_MATCH_ID   = "matchId";
    public static final String EXTRA_WORKER_UID = "workerUid";
    public static final String EXTRA_WORKER_NAME= "workerName";
    public static final String EXTRA_AMOUNT     = "amount";

    // ── Backend endpoint (replace with your deployed URL for production) ──────
    // For local testing use your machine's LAN IP, e.g. "http://192.168.1.x:3000"
    private static final String BACKEND_BASE_URL = "http://10.0.2.2:3000";

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView  tvAmount, tvWorkerLabel, tvTotalWithTip;
    private EditText  etTip;
    private View      cardCash, cardStripe;
    private ImageView icCashCheck, icCardCheck;
    private MaterialButton btnPayNow;
    private ProgressBar progressStripe;

    // ── State ──────────────────────────────────────────────────────────────────
    private String  jobId, matchId, workerUid, workerName;
    private double  baseAmount = 0.0;
    private boolean isCashSelected = true;  // Cash is the default

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseAuth      mAuth;

    // ── Stripe ────────────────────────────────────────────────────────────────
    private PaymentSheet paymentSheet;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Read extras
        jobId      = getIntent().getStringExtra(EXTRA_JOB_ID);
        matchId    = getIntent().getStringExtra(EXTRA_MATCH_ID);
        workerUid  = getIntent().getStringExtra(EXTRA_WORKER_UID);
        workerName = getIntent().getStringExtra(EXTRA_WORKER_NAME);
        baseAmount = getIntent().getDoubleExtra(EXTRA_AMOUNT, 0.0);

        // Bind views
        tvAmount       = findViewById(R.id.tv_payment_amount);
        tvWorkerLabel  = findViewById(R.id.tv_worker_name_label);
        tvTotalWithTip = findViewById(R.id.tv_total_with_tip);
        etTip          = findViewById(R.id.et_tip);
        cardCash       = findViewById(R.id.card_method_cash);
        cardStripe     = findViewById(R.id.card_method_stripe);
        icCashCheck    = findViewById(R.id.ic_cash_check);
        icCardCheck    = findViewById(R.id.ic_card_check);
        btnPayNow      = findViewById(R.id.btn_pay_now);
        progressStripe = findViewById(R.id.progress_stripe);

        ImageButton btnBack = findViewById(R.id.btn_back_arrow_settings);
        btnBack.setOnClickListener(v -> finish());

        // Initial UI state
        tvAmount.setText(formatRs(baseAmount));
        tvWorkerLabel.setText(TextUtils.isEmpty(workerName) ? "Worker" : "To: " + workerName);
        updateTotalLabel();

        // Stripe PaymentSheet init
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

        // Method selector listeners
        cardCash.setOnClickListener(v -> selectCash());
        cardStripe.setOnClickListener(v -> selectCard());

        // Live tip → update total label
        etTip.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateTotalLabel(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Pay button
        btnPayNow.setOnClickListener(v -> onConfirmClicked());
    }

    // ── Method selection ──────────────────────────────────────────────────────

    private void selectCash() {
        isCashSelected = true;
        cardCash.setBackground(getDrawable(R.drawable.bg_payment_method_selected));
        cardStripe.setBackground(getDrawable(R.drawable.bg_payment_method_unselected));
        icCashCheck.setVisibility(View.VISIBLE);
        icCardCheck.setVisibility(View.INVISIBLE);
    }

    private void selectCard() {
        isCashSelected = false;
        cardStripe.setBackground(getDrawable(R.drawable.bg_payment_method_selected));
        cardCash.setBackground(getDrawable(R.drawable.bg_payment_method_unselected));
        icCardCheck.setVisibility(View.VISIBLE);
        icCashCheck.setVisibility(View.INVISIBLE);
    }

    // ── Tip helper ────────────────────────────────────────────────────────────

    private double getTipAmount() {
        try {
            String t = etTip.getText().toString().trim();
            return TextUtils.isEmpty(t) ? 0.0 : Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void updateTotalLabel() {
        double total = baseAmount + getTipAmount();
        tvTotalWithTip.setText("Total: " + formatRs(total));
    }

    private String formatRs(double val) {
        return String.format(Locale.getDefault(), "Rs. %,d", Math.round(val));
    }

    // ── Payment confirm ───────────────────────────────────────────────────────

    private void onConfirmClicked() {
        double tip   = getTipAmount();
        double total = baseAmount + tip;

        if (isCashSelected) {
            saveCashPayment(tip, total);
        } else {
            startStripePayment(total, tip);
        }
    }

    // ── CASH FLOW ─────────────────────────────────────────────────────────────

    private void saveCashPayment(double tip, double total) {
        String clientUid = FirebaseDebugLogger.requireUid(this, mAuth, "payment_create");
        if (clientUid == null) return;

        btnPayNow.setEnabled(false);

        Map<String, Object> payment = new HashMap<>();
        payment.put("jobId",         jobId);
        payment.put("matchId",       matchId);
        payment.put("clientUid",     clientUid);
        payment.put("workerUid",     workerUid);
        payment.put("workerName",    workerName != null ? workerName : "");
        payment.put("amount",        baseAmount);
        payment.put("tip",           tip);
        payment.put("totalAmount",   total);
        payment.put("paymentMethod", "CASH");
        payment.put("status",        "PENDING_CASH");
        payment.put("createdAt",     FieldValue.serverTimestamp());

        db.collection("payments")
                .add(payment)
                .addOnSuccessListener(docRef -> {
                    FirebaseDebugLogger.success("payment_create", "payments", docRef.getId());

                    // Update match document with payment reference
                    if (!TextUtils.isEmpty(matchId)) {
                        Map<String, Object> matchUpdate = new HashMap<>();
                        matchUpdate.put("paymentStatus", "PENDING_CASH");
                        matchUpdate.put("paymentId",     docRef.getId());
                        matchUpdate.put("updatedAt",     FieldValue.serverTimestamp());
                        db.collection("matches").document(matchId)
                                .update(matchUpdate)
                                .addOnFailureListener(e ->
                                        Log.w(TAG, "Failed to update match payment status", e));
                    }

                    navigateToSuccess(baseAmount, tip, total, "CASH", docRef.getId());
                })
                .addOnFailureListener(e -> {
                    FirebaseDebugLogger.failure("payment_create", "payments", e);
                    btnPayNow.setEnabled(true);
                    Toast.makeText(this, "Payment failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ── STRIPE FLOW ───────────────────────────────────────────────────────────

    private void startStripePayment(double total, double tip) {
        btnPayNow.setEnabled(false);
        progressStripe.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                JSONObject body = new JSONObject();
                body.put("amount",   total);
                body.put("currency", "lkr");
                body.put("jobId",    jobId != null ? jobId : "");
                body.put("matchId",  matchId != null ? matchId : "");

                RequestBody reqBody = RequestBody.create(
                        body.toString(),
                        MediaType.get("application/json; charset=utf-8"));

                Request request = new Request.Builder()
                        .url(BACKEND_BASE_URL + "/api/payment/create-intent")
                        .post(reqBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseStr = response.body() != null
                            ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        throw new RuntimeException("Server error: " + response.code());
                    }
                    JSONObject json = new JSONObject(responseStr);
                    String clientSecret = json.getString("clientSecret");

                    runOnUiThread(() -> {
                        progressStripe.setVisibility(View.GONE);
                        presentStripeSheet(clientSecret);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Stripe intent creation failed", e);
                runOnUiThread(() -> {
                    progressStripe.setVisibility(View.GONE);
                    btnPayNow.setEnabled(true);
                    Toast.makeText(this,
                            "Could not start card payment: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void presentStripeSheet(String clientSecret) {
        PaymentSheet.Configuration config = new PaymentSheet.Configuration.Builder("WOIL")
                .build();
        paymentSheet.presentWithPaymentIntent(clientSecret, config);
    }

    private void onPaymentSheetResult(PaymentSheetResult result) {
        if (result instanceof PaymentSheetResult.Completed) {
            double tip   = getTipAmount();
            double total = baseAmount + tip;
            saveStripePaymentRecord(tip, total);
        } else if (result instanceof PaymentSheetResult.Failed) {
            btnPayNow.setEnabled(true);
            Toast.makeText(this,
                    "Card payment failed: " +
                            ((PaymentSheetResult.Failed) result).getError().getMessage(),
                    Toast.LENGTH_LONG).show();
        } else {
            // Cancelled
            btnPayNow.setEnabled(true);
        }
    }

    private void saveStripePaymentRecord(double tip, double total) {
        String clientUid = FirebaseDebugLogger.requireUid(this, mAuth, "payment_card");
        if (clientUid == null) return;

        Map<String, Object> payment = new HashMap<>();
        payment.put("jobId",         jobId);
        payment.put("matchId",       matchId);
        payment.put("clientUid",     clientUid);
        payment.put("workerUid",     workerUid);
        payment.put("workerName",    workerName != null ? workerName : "");
        payment.put("amount",        baseAmount);
        payment.put("tip",           tip);
        payment.put("totalAmount",   total);
        payment.put("paymentMethod", "CARD");
        payment.put("status",        "PAID_CARD");
        payment.put("createdAt",     FieldValue.serverTimestamp());

        db.collection("payments").add(payment)
                .addOnSuccessListener(docRef -> {
                    if (!TextUtils.isEmpty(matchId)) {
                        Map<String, Object> matchUpdate = new HashMap<>();
                        matchUpdate.put("paymentStatus", "PAID_CARD");
                        matchUpdate.put("paymentId",     docRef.getId());
                        matchUpdate.put("updatedAt",     FieldValue.serverTimestamp());
                        db.collection("matches").document(matchId).update(matchUpdate);
                    }
                    navigateToSuccess(baseAmount, tip, total, "CARD", docRef.getId());
                })
                .addOnFailureListener(e -> {
                    btnPayNow.setEnabled(true);
                    Toast.makeText(this, "Record failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateToSuccess(double amount, double tip, double total,
                                   String method, String paymentId) {
        Intent intent = new Intent(this, PaymentSuccessActivity.class);
        intent.putExtra("amount",      amount);
        intent.putExtra("tip",         tip);
        intent.putExtra("totalAmount", total);
        intent.putExtra("method",      method);
        intent.putExtra("workerName",  workerName);
        intent.putExtra("paymentId",   paymentId);
        startActivity(intent);
        finish();
    }
}