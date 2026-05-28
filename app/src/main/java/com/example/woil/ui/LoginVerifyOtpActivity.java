package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginVerifyOtpActivity extends AppCompatActivity {

    private static final String TAG = "LoginVerifyOtpActivity";

    private EditText etOtp;
    private MaterialButton btnVerifyOtp;
    private TextView tvResend, tvChangeNumber, tvOtpSubtitle;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String phone;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_verify_otp);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etOtp = findViewById(R.id.etOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        tvResend = findViewById(R.id.tvResend);
        tvChangeNumber = findViewById(R.id.tvChangeNumber);
        tvOtpSubtitle = findViewById(R.id.tvOtpSubtitle);

        Intent intent = getIntent();
        phone = intent.getStringExtra("phone");
        verificationId = intent.getStringExtra("verificationId");

        if (!TextUtils.isEmpty(phone)) {
            tvOtpSubtitle.setText("Enter the OTP sent to " + phone);
        }

        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                debugLog("login_otp_auto_verify", "auth/phone", "success", phone);
                signInWithCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                String msg = e != null ? e.getMessage() : "Verification failed";
                debugLog("login_otp_resend", "auth/phone", "failed", msg);
                Toast.makeText(LoginVerifyOtpActivity.this, "Verification failed: " + msg, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(String newVerificationId, PhoneAuthProvider.ForceResendingToken token) {
                verificationId = newVerificationId;
                resendToken = token;
                debugLog("login_otp_resend", "auth/phone", "success", phone);
                Toast.makeText(LoginVerifyOtpActivity.this, "OTP resent to " + phone, Toast.LENGTH_SHORT).show();
            }
        };

        btnVerifyOtp.setOnClickListener(v -> verifyOtp());

        tvResend.setOnClickListener(v -> resendOtp());

        tvChangeNumber.setOnClickListener(v -> {
            startActivity(new Intent(LoginVerifyOtpActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void verifyOtp() {
        String otp = etOtp.getText().toString().trim();
        if (TextUtils.isEmpty(otp)) {
            etOtp.setError("Enter OTP");
            return;
        }
        if (otp.length() != 6) {
            etOtp.setError("OTP must be 6 digits");
            return;
        }
        if (TextUtils.isEmpty(verificationId)) {
            Toast.makeText(this, "Missing verification data. Request OTP again.", Toast.LENGTH_LONG).show();
            return;
        }

        btnVerifyOtp.setEnabled(false);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        signInWithCredential(credential);
    }

    private void resendOtp() {
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Missing phone number. Go back and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks);

        if (resendToken != null) {
            builder.setForceResendingToken(resendToken);
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build());
        debugLog("login_otp_resend", "auth/phone", "started", phone);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            btnVerifyOtp.setEnabled(true);
            if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                String uid = task.getResult().getUser().getUid();
                String authPhone = task.getResult().getUser().getPhoneNumber();
                debugLog("login_otp_sign_in", "users/" + uid, "success", authPhone);
                loadUserAndRoute(uid, authPhone);
            } else {
                String err = task.getException() != null ? task.getException().getMessage() : "OTP verification failed";
                debugLog("login_otp_sign_in", "auth/phone", "failed", err);
                Toast.makeText(this, "OTP verification failed: " + err, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadUserAndRoute(String uid, String authPhone) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    debugLog("login_user_read", "users/" + uid, "success", "exists=" + userDoc.exists());
                    if (!userDoc.exists()) {
                        createMinimalUserThenProfileSetup(uid, authPhone);
                        return;
                    }

                    String roleFromDb = userDoc.getString("role");
                    final String role = TextUtils.isEmpty(roleFromDb) ? "worker" : roleFromDb;
                    saveActiveRole(role);

                    db.collection("users").document(uid)
                            .update("lastSeenAt", FieldValue.serverTimestamp(), "authProvider", "phone")
                            .addOnSuccessListener(unused ->
                                    debugLog("login_user_update", "users/" + uid, "success", "lastSeenAt"))
                            .addOnFailureListener(e ->
                                    debugLog("login_user_update", "users/" + uid, "failed", e.getMessage()));

                    db.collection("profiles").document(uid).get()
                            .addOnSuccessListener(profileDoc -> {
                                debugLog("login_profile_read", "profiles/" + uid, "success", "exists=" + profileDoc.exists());

                                if (profileDoc.exists()) {
                                    Intent intent = new Intent(LoginVerifyOtpActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                } else {
                                    Intent intent = new Intent(LoginVerifyOtpActivity.this, ProfileSetupActivity.class);
                                    intent.putExtra("role", role);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                }

                                finish();
                            })
                            .addOnFailureListener(e -> {
                                debugLog("login_profile_read", "profiles/" + uid, "failed", e.getMessage());
                                Toast.makeText(this, "Profile read failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    debugLog("login_user_read", "users/" + uid, "failed", e.getMessage());
                    Toast.makeText(this, "User read failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void createMinimalUserThenProfileSetup(String uid, String authPhone) {
        Map<String, Object> user = new HashMap<>();
        user.put("phone", authPhone);
        user.put("role", "worker");
        user.put("authProvider", "phone");
        user.put("nicVerified", false);
        user.put("consentNicProcessing", false);
        user.put("createdAt", FieldValue.serverTimestamp());
        user.put("lastSeenAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(unused -> {
                    saveActiveRole("worker");
                    debugLog("login_user_create_missing", "users/" + uid, "success", authPhone);
                    Intent intent = new Intent(LoginVerifyOtpActivity.this, ProfileSetupActivity.class);
                    intent.putExtra("role", "worker");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    debugLog("login_user_create_missing", "users/" + uid, "failed", e.getMessage());
                    Toast.makeText(this, "User create failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveActiveRole(String role) {
        getSharedPreferences("woil_prefs", MODE_PRIVATE)
                .edit()
                .putString("active_role", role)
                .apply();
    }

    private void debugLog(String action, String path, String status, String message) {
        Log.d(TAG, action + " | " + path + " | " + status + " | " + message);
        if (mAuth.getCurrentUser() == null) return;

        Map<String, Object> log = new HashMap<>();
        log.put("uid", mAuth.getCurrentUser().getUid());
        log.put("activity", "LoginVerifyOtpActivity");
        log.put("action", action);
        log.put("path", path);
        log.put("status", status);
        log.put("message", message);
        log.put("createdAt", FieldValue.serverTimestamp());
        db.collection("firebase_debug_logs").add(log);
    }
}
