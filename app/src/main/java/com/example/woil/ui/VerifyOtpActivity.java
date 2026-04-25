package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Receives verificationId from SignUpActivity, verifies the real Firebase phone OTP,
 * signs in with the phone credential, and then opens ProfileSetupActivity.
 */
public class VerifyOtpActivity extends AppCompatActivity {

    private EditText etOtp;
    private MaterialButton btnVerify;
    private TextView tvResend;

    private String phone;
    private String role;
    private String verificationId;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verify_otp);

        etOtp = findViewById(R.id.etOtp);
        btnVerify = findViewById(R.id.btnVerifyOtp);
        tvResend = findViewById(R.id.tvResend);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        phone = intent.getStringExtra("phone");
        role = intent.getStringExtra("role");
        verificationId = intent.getStringExtra("verificationId");

        btnVerify.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (!validateOtp(otp)) return;

            if (TextUtils.isEmpty(verificationId)) {
                Toast.makeText(this, "Missing verification data. Please request OTP again.", Toast.LENGTH_LONG).show();
                return;
            }

            btnVerify.setEnabled(false);
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
            signInWithPhoneAuthCredential(credential);
        });

        tvResend.setOnClickListener(v -> {
            Intent signupIntent = new Intent(VerifyOtpActivity.this, SignUpActivity.class);
            signupIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(signupIntent);
            finish();
        });
    }

    private boolean validateOtp(String otp) {
        if (TextUtils.isEmpty(otp)) {
            etOtp.setError("Enter OTP");
            return false;
        }
        if (!otp.matches("\\d{6}")) {
            etOtp.setError("OTP must be 6 digits");
            return false;
        }
        return true;
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    btnVerify.setEnabled(true);
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        String phoneNumber = mAuth.getCurrentUser().getPhoneNumber();
                        if (TextUtils.isEmpty(phoneNumber)) {
                            phoneNumber = phone;
                        }

                        createUserInFirestoreIfNotExists(uid, phoneNumber, role);

                        Intent intent = new Intent(VerifyOtpActivity.this, ProfileSetupActivity.class);
                        intent.putExtra("role", role);
                        startActivity(intent);
                        finish();
                    } else {
                        String err = task.getException() != null
                                ? task.getException().getMessage()
                                : "Verification failed";
                        Toast.makeText(VerifyOtpActivity.this,
                                "OTP verification failed: " + err, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createUserInFirestoreIfNotExists(String uid, String phone, String role) {
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
                            Toast.makeText(this,
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
            Toast.makeText(this,
                "Firestore read error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
