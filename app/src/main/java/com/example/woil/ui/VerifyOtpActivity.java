package com.example.woil.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.woil.R;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class VerifyOtpActivity extends AppCompatActivity {

    private EditText etOtp;
    private MaterialButton btnVerify;
    private TextView tvResend;
    private String phone;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verify_otp);

        etOtp = findViewById(R.id.etOtp);
        btnVerify = findViewById(R.id.btnVerifyOtp);
        tvResend = findViewById(R.id.tvResend);

        Intent i = getIntent();
        phone = i.getStringExtra("phone");
        role = i.getStringExtra("role");

        btnVerify.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (!validateOtp(otp)) return;

            // TODO: Replace with real server verification
            verifyOtp(otp);
        });

        tvResend.setOnClickListener(v -> {
            // TODO: call resend OTP endpoint
            resendOtp();
        });
    }

    private boolean validateOtp(String otp) {
        if (TextUtils.isEmpty(otp)) {
            etOtp.setError("Enter OTP");
            return false;
        }
        if (otp.length() != 6) {
            etOtp.setError("OTP must be 6 digits");
            return false;
        }
        return true;
    }

    private void verifyOtp(String otp) {
        // Temporary dummy check: accept "123456" for testing. Replace with your server check.
        if ("123456".equals(otp)) {
            Toast.makeText(this, "Phone verified! role=" + role, Toast.LENGTH_SHORT).show();
            // TODO: continue to registration / main screen
            finish();
        } else {
            Toast.makeText(this, "Invalid OTP (for demo use 123456)", Toast.LENGTH_SHORT).show();
        }
    }

    private void resendOtp() {
        // TODO: call resend API
        Toast.makeText(this, "Resending OTP to " + phone, Toast.LENGTH_SHORT).show();
    }
}
