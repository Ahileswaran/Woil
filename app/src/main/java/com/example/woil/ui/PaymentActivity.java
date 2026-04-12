package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.EditText;
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

public class PaymentActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvPaymentAmount;
    private EditText etTip;
    private MaterialButton btnPayNow;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String jobId;
    private String workerUid;
    private double amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        btnBack = findViewById(R.id.btn_back_arrow_settings);
        tvPaymentAmount = findViewById(R.id.tv_payment_amount);
        etTip = findViewById(R.id.et_tip);
        btnPayNow = findViewById(R.id.btn_pay_now);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        jobId = getIntent().getStringExtra("jobId");
        workerUid = getIntent().getStringExtra("workerUid");
        amount = getIntent().getDoubleExtra("amount", 0.0);

        tvPaymentAmount.setText("Amount: Rs. " + Math.round(amount));

        btnBack.setOnClickListener(v -> finish());
        btnPayNow.setOnClickListener(v -> savePayment());
    }

    private void savePayment() {
        Map<String, Object> payment = new HashMap<>();
        payment.put("jobId", jobId);
        payment.put("clientUid", mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null);
        payment.put("workerUid", workerUid);
        payment.put("amount", amount);
        payment.put("tip", etTip.getText().toString().trim());
        payment.put("status", "PAID");
        payment.put("createdAt", FieldValue.serverTimestamp());

        db.collection("payments")
                .add(payment)
                .addOnSuccessListener(doc -> {
                    startActivity(new Intent(this, PaymentSuccessActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Payment failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}