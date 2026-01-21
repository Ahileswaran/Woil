package com.example.woil.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.example.woil.R;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class SignUpActivity extends AppCompatActivity {

    private EditText etPhone;
    private RadioGroup rgRole;
    private MaterialButton btnSignUp;
    private TextView tvAlready;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etPhone = findViewById(R.id.etPhone);
        rgRole = findViewById(R.id.rgRole);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvAlready = findViewById(R.id.tvAlready);

        btnSignUp.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            if (!validatePhone(phone)) return;

            int checkedId = rgRole.getCheckedRadioButtonId();
            String role = "worker";
            if (checkedId == R.id.rbClient) role = "client";
            else if (checkedId == R.id.rbWorker) role = "worker";

            // TODO: call your backend to request OTP here (sendSms/sendOtp).
            sendOtp("+94" + phone);

            // navigate to verify screen
            Intent i = new Intent(SignUpActivity.this, VerifyOtpActivity.class);
            i.putExtra("phone", "+94" + phone);
            i.putExtra("role", role);
            startActivity(i);
        });

        tvAlready.setOnClickListener(v -> {
            // TODO: navigate to login screen if you have one
            Toast.makeText(this, "Open Login screen (not implemented)", Toast.LENGTH_SHORT).show();
        });
    }

    private boolean validatePhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Enter phone number");
            return false;
        }
        // basic length check (adjust for your format)
        if (phone.length() < 7) {
            etPhone.setError("Enter valid phone number");
            return false;
        }
        return true;
    }

    private void sendOtp(String fullPhone) {
        // Place your OTP send logic here (call SMS gateway / Firebase / server)
        Toast.makeText(this, "Requesting OTP for " + fullPhone, Toast.LENGTH_SHORT).show();
    }
}
