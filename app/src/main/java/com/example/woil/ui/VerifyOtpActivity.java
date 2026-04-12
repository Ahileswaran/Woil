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
 * Receives verificationId from SignUpActivity. Verifies OTP and signs in.
 * Also supports 'bypass' mode for testing: any OTP accepted -> signInAnonymously().
 */
public class VerifyOtpActivity extends AppCompatActivity {

    private EditText etOtp;
    private MaterialButton btnVerify;
    private TextView tvResend;

    private String phone;
    private String role;
    private String verificationId;
    private boolean bypass = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verify_otp);

        etOtp = findViewById(R.id.etOtp);
        btnVerify = findViewById(R.id.btnVerifyOtp);
        tvResend = findViewById(R.id.tvResend);

        // Allow content to lay out behind system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);


        // Transparent bars so fragment header can draw behind them
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Intent i = getIntent();
        phone = i.getStringExtra("phone");
        role = i.getStringExtra("role");
        verificationId = i.getStringExtra("verificationId");
        bypass = i.getBooleanExtra("bypass", false);

        btnVerify.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (!validateOtp(otp)) return;

            if (bypass) {
                // TEST MODE: accept any OTP by signing in anonymously
                mAuth.signInAnonymously()
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                String uid = mAuth.getCurrentUser().getUid();
                                createUserInFirestoreIfNotExists(uid, phone, role);

                                Intent intent = new Intent(VerifyOtpActivity.this, ProfileSetupActivity.class);
                                intent.putExtra("role", role);
                                startActivity(intent);
                                finish();
                            } else {
                                String err = (task.getException() != null) ? task.getException().getMessage() : "Anonymous sign-in failed";
                                Toast.makeText(VerifyOtpActivity.this, "Anonymous sign-in failed: " + err, Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                // Real OTP verification path
                if (verificationId == null) {
                    Toast.makeText(this, "Missing verification data. Please request OTP again.", Toast.LENGTH_LONG).show();
                    return;
                }
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                String uid = mAuth.getCurrentUser().getUid();
                                String phoneNumber = mAuth.getCurrentUser().getPhoneNumber();
                                createUserInFirestoreIfNotExists(uid, phoneNumber, role);

                                Intent intent = new Intent(VerifyOtpActivity.this, ProfileSetupActivity.class);
                                intent.putExtra("role", role);
                                startActivity(intent);
                                finish();
                            } else {
                                String err = (task.getException() != null) ? task.getException().getMessage() : "Verification failed";
                                Toast.makeText(VerifyOtpActivity.this, "OTP verification failed: " + err, Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        tvResend.setOnClickListener(v -> {
            Toast.makeText(this, "Resend not implemented here. Go back and request a new OTP.", Toast.LENGTH_LONG).show();
        });
    }

    private boolean validateOtp(String otp) {
        if (TextUtils.isEmpty(otp)) {
            etOtp.setError("Enter OTP");
            return false;
        }
        // accept any length in bypass mode so quick testing is easier
        if (!bypass && otp.length() != 6) {
            etOtp.setError("OTP must be 6 digits");
            return false;
        }
        return true;
    }

    private void createUserInFirestoreIfNotExists(String uid, String phone, String role) {
        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Map<String, Object> user = new HashMap<>();
                user.put("phone", phone);
                user.put("role", role);
                user.put("nicVerified", false);
                user.put("consentNicProcessing", false);
                user.put("createdAt", FieldValue.serverTimestamp());
                user.put("lastSeenAt", FieldValue.serverTimestamp());

                db.collection("users").document(uid).set(user)
                        .addOnSuccessListener(aVoid -> {
                            // created successfully
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } else {
                // Optionally update lastSeenAt if doc exists
                db.collection("users").document(uid).update("lastSeenAt", FieldValue.serverTimestamp());
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Firestore read error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
