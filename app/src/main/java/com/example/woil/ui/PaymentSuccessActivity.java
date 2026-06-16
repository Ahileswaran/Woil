package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

/**
 * PaymentSuccessActivity — shown to the client after payment is confirmed.
 * Displays a receipt with amount, tip, total, method and status.
 *
 * Extras received from PaymentActivity:
 *   "amount"      (double)
 *   "tip"         (double)
 *   "totalAmount" (double)
 *   "method"      (String: "CASH" or "CARD")
 *   "workerName"  (String)
 *   "paymentId"   (String)
 */
public class PaymentSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_success);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        // Read extras
        double amount      = getIntent().getDoubleExtra("amount",      0.0);
        double tip         = getIntent().getDoubleExtra("tip",         0.0);
        double totalAmount = getIntent().getDoubleExtra("totalAmount", 0.0);
        String method      = getIntent().getStringExtra("method");
        String workerName  = getIntent().getStringExtra("workerName");

        // Bind views
        TextView tvAmount = findViewById(R.id.tv_receipt_amount);
        TextView tvTip    = findViewById(R.id.tv_receipt_tip);
        TextView tvTotal  = findViewById(R.id.tv_receipt_total);
        TextView tvMethod = findViewById(R.id.tv_receipt_method);
        TextView tvStatus = findViewById(R.id.tv_receipt_status);
        TextView tvSubtitle = findViewById(R.id.tv_success_subtitle);
        MaterialButton btnDone = findViewById(R.id.btn_done);

        // Populate receipt
        tvAmount.setText(formatRs(amount));
        tvTip.setText(tip > 0 ? formatRs(tip) : "No tip");
        tvTotal.setText(formatRs(totalAmount));

        boolean isCash = "CASH".equalsIgnoreCase(method);
        tvMethod.setText(isCash ? "💵 Ready Cash" : "💳 Card (Stripe)");

        if (isCash) {
            tvStatus.setText("⏳ Pending worker confirmation");
            tvStatus.setTextColor(Color.parseColor("#E67E00"));
            tvSubtitle.setText("Waiting for worker to confirm cash receipt");
        } else {
            tvStatus.setText("✅ Paid");
            tvStatus.setTextColor(Color.parseColor("#1E8A3B"));
            tvSubtitle.setText("Card payment processed successfully");
        }

        // Done → go back to home (clear stack)
        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private String formatRs(double val) {
        return String.format(Locale.getDefault(), "Rs. %,d", Math.round(val));
    }
}