package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.RadioGroup;
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

public class SignUpActivity extends AppCompatActivity {

    private EditText etPhone;
    private RadioGroup rgRole;
    private MaterialButton btnSignUp;
    private TextView tvAlready;

    private FirebaseAuth mAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etPhone = findViewById(R.id.etPhone);
        rgRole = findViewById(R.id.rgRole);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvAlready = findViewById(R.id.tvAlready);

        mAuth = FirebaseAuth.getInstance();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                signInWithPhoneAuthCredential(credential, getRoleFromUi());
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                btnSignUp.setEnabled(true);
                String msg = e != null ? e.getMessage() : "Verification failed";
                Toast.makeText(SignUpActivity.this, "Verification failed: " + msg, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                btnSignUp.setEnabled(true);
                mResendToken = token;

                String fullPhone = normalizeSriLankanPhone(etPhone.getText().toString().trim());
                String role = getRoleFromUi();

                Intent intent = new Intent(SignUpActivity.this, VerifyOtpActivity.class);
                intent.putExtra("phone", fullPhone);
                intent.putExtra("role", role);
                intent.putExtra("verificationId", verificationId);
                startActivity(intent);
            }
        };

        btnSignUp.setOnClickListener(v -> {
            String phoneInput = etPhone.getText().toString().trim();
            if (!validatePhone(phoneInput)) return;

            String fullPhone = normalizeSriLankanPhone(phoneInput);
            btnSignUp.setEnabled(false);
            startPhoneNumberVerification(fullPhone);
        });

        tvAlready.setOnClickListener(v -> {
            Toast.makeText(this, "Open Login screen (not implemented)", Toast.LENGTH_SHORT).show();
        });
    }

    private String getRoleFromUi() {
        int checkedId = rgRole.getCheckedRadioButtonId();
        if (checkedId == R.id.rbClient) return "client";
        return "worker";
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
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        Toast.makeText(this, "Requesting OTP for " + phoneNumber, Toast.LENGTH_SHORT).show();
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential, String role) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    btnSignUp.setEnabled(true);
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        String phone = task.getResult().getUser().getPhoneNumber();

                        createUserInFirestoreIfNotExists(uid, phone, role);

                        Intent intent = new Intent(SignUpActivity.this, ProfileSetupActivity.class);
                        intent.putExtra("role", role);
                        startActivity(intent);
                        finish();
                    } else {
                        String err = task.getException() != null
                                ? task.getException().getMessage()
                                : "Authentication failed";
                        if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(SignUpActivity.this, "Invalid code.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(SignUpActivity.this, "Sign-in failed: " + err, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void createUserInFirestoreIfNotExists(String uid, String phone, String role) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            FirebaseDebugLogger.read("auth_user_read", "users/" + uid, documentSnapshot.exists() ? 1 : 0);
            if (!documentSnapshot.exists()) {
                Map<String, Object> user = new HashMap<>();
                user.put("phone", phone);
                user.put("role", role);
                user.put("nicVerified", false);
                user.put("consentNicProcessing", false);
                user.put("authProvider", "phone");
                user.put("createdAt", FieldValue.serverTimestamp());
                user.put("lastSeenAt", FieldValue.serverTimestamp());

                db.collection("users").document(uid).set(user)
                        .addOnSuccessListener(unused -> FirebaseDebugLogger.success("auth_user_create", "users", uid))
                        .addOnFailureListener(e -> {
                            FirebaseDebugLogger.failure("auth_user_create", "users/" + uid, e);
                            Toast.makeText(SignUpActivity.this,
                                "Firestore error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            } else {
                db.collection("users").document(uid)
                        .update("lastSeenAt", FieldValue.serverTimestamp(), "role", role, "authProvider", "phone")
                        .addOnSuccessListener(unused -> FirebaseDebugLogger.success("auth_user_update", "users", uid))
                        .addOnFailureListener(e -> FirebaseDebugLogger.failure("auth_user_update", "users/" + uid, e));
            }
        }).addOnFailureListener(e -> {
            FirebaseDebugLogger.failure("auth_user_read", "users/" + uid, e);
            Toast.makeText(SignUpActivity.this,
                "Firestore read error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
