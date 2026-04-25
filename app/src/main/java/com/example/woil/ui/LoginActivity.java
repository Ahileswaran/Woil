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

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etPhone;
    private MaterialButton btnLogin;
    private TextView tvSignUpLink;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etPhone = findViewById(R.id.etPhone);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUpLink = findViewById(R.id.tvSignUpLink);

        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Log.d(TAG, "Auto verification completed");
                debugLog("login_auto_verify", "auth/phone", "success", null);
                signInWithCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                btnLogin.setEnabled(true);
                String msg = e != null ? e.getMessage() : "Verification failed";
                Log.e(TAG, "Phone verification failed", e);
                debugLog("login_send_otp", "auth/phone", "failed", msg);
                Toast.makeText(LoginActivity.this, "Verification failed: " + msg, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                btnLogin.setEnabled(true);
                resendToken = token;
                String phone = normalizeSriLankanPhone(etPhone.getText().toString().trim());
                debugLog("login_code_sent", "auth/phone", "success", phone);

                Intent intent = new Intent(LoginActivity.this, LoginVerifyOtpActivity.class);
                intent.putExtra("phone", phone);
                intent.putExtra("verificationId", verificationId);
                startActivity(intent);
            }
        };

        btnLogin.setOnClickListener(v -> {
            String phoneInput = etPhone.getText().toString().trim();
            if (!validatePhone(phoneInput)) return;

            String fullPhone = normalizeSriLankanPhone(phoneInput);
            btnLogin.setEnabled(false);
            startPhoneNumberVerification(fullPhone);
        });

        tvSignUpLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });
    }

    private boolean validatePhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Enter phone number");
            return false;
        }

        String digits = phone.replace(" ", "").replace("-", "");
        if (digits.startsWith("+94")) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("94")) {
            digits = "0" + digits.substring(2);
        }

        if (!digits.matches("0\\d{9}")) {
            etPhone.setError("Enter a valid Sri Lankan phone number");
            return false;
        }
        return true;
    }

    private String normalizeSriLankanPhone(String phone) {
        String digits = phone.replace(" ", "").replace("-", "");
        if (digits.startsWith("+94")) return digits;
        if (digits.startsWith("94")) return "+" + digits;
        if (digits.startsWith("0")) return "+94" + digits.substring(1);
        return "+94" + digits;
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
        debugLog("login_send_otp", "auth/phone", "started", phoneNumber);
        Toast.makeText(this, "Requesting OTP for " + phoneNumber, Toast.LENGTH_SHORT).show();
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            btnLogin.setEnabled(true);
            if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                String uid = task.getResult().getUser().getUid();
                String phone = task.getResult().getUser().getPhoneNumber();
                debugLog("login_auto_sign_in", "users/" + uid, "success", phone);
                loadUserAndRoute(uid, phone);
            } else {
                String err = task.getException() != null ? task.getException().getMessage() : "Login failed";
                debugLog("login_auto_sign_in", "auth/phone", "failed", err);
                Toast.makeText(this, "Login failed: " + err, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadUserAndRoute(String uid, String phone) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    debugLog("login_user_read", "users/" + uid, "success", "exists=" + userDoc.exists());
                    if (!userDoc.exists()) {
                        createMinimalUserThenProfileSetup(uid, phone);
                        return;
                    }

                    String roleFromDb = userDoc.getString("role");
                    final String role = TextUtils.isEmpty(roleFromDb) ? "worker" : roleFromDb;
                    saveActiveRole(role);

                    db.collection("profiles").document(uid).get()
                            .addOnSuccessListener(profileDoc -> {
                                debugLog("login_profile_read", "profiles/" + uid, "success", "exists=" + profileDoc.exists());

                                if (profileDoc.exists()) {
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                } else {
                                    Intent intent = new Intent(LoginActivity.this, ProfileSetupActivity.class);
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

    private void createMinimalUserThenProfileSetup(String uid, String phone) {
        Map<String, Object> user = new HashMap<>();
        user.put("phone", phone);
        user.put("role", "worker");
        user.put("authProvider", "phone");
        user.put("nicVerified", false);
        user.put("consentNicProcessing", false);
        user.put("createdAt", FieldValue.serverTimestamp());
        user.put("lastSeenAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(unused -> {
                    saveActiveRole("worker");
                    debugLog("login_user_create_missing", "users/" + uid, "success", phone);
                    Intent intent = new Intent(LoginActivity.this, ProfileSetupActivity.class);
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
        log.put("activity", "LoginActivity");
        log.put("action", action);
        log.put("path", path);
        log.put("status", status);
        log.put("message", message);
        log.put("createdAt", FieldValue.serverTimestamp());
        db.collection("firebase_debug_logs").add(log);
    }
}
