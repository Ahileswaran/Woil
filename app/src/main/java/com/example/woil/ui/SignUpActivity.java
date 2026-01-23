package com.example.woil.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Starts phone verification with Firebase and navigates to VerifyOtpActivity when code is sent.
 */
public class SignUpActivity extends AppCompatActivity {

    private EditText etPhone;
    private RadioGroup rgRole;
    private MaterialButton btnSignUp;
    private TextView tvAlready;

    private FirebaseAuth mAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private PhoneAuthProvider.ForceResendingToken mResendToken; // kept in case you implement resend later

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etPhone = findViewById(R.id.etPhone);
        rgRole = findViewById(R.id.rgRole);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvAlready = findViewById(R.id.tvAlready);

        mAuth = FirebaseAuth.getInstance();

        // callbacks for phone verification
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // Auto verification or instant verification (rare) - sign in directly
                signInWithPhoneAuthCredential(credential, getRoleFromUi());
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                String msg = e != null ? e.getMessage() : "Verification failed";
                Toast.makeText(SignUpActivity.this, "Verification failed: " + msg, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                // Save token if you want to support resending later
                mResendToken = token;

                // Pass verificationId to OTP screen
                String phone = etPhone.getText().toString().trim();
                String role = getRoleFromUi();

                Intent i = new Intent(SignUpActivity.this, VerifyOtpActivity.class);
                i.putExtra("phone", "+94" + phone);
                i.putExtra("role", role);
                i.putExtra("verificationId", verificationId);
                startActivity(i);
            }
        };

        // Sign up button click
        btnSignUp.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            if (!validatePhone(phone)) return;

            startPhoneNumberVerification("+94" + phone);
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
        // basic length check - adjust to your expected local formats
        if (phone.length() < 7) {
            etPhone.setError("Enter valid phone number");
            return false;
        }
        return true;
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
                    if (task.isSuccessful()) {
                        // Signed in successfully
                        String uid = task.getResult().getUser().getUid();
                        String phone = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getPhoneNumber() : null;

                        createUserInFirestoreIfNotExists(uid, phone, role);

                        // Navigate to profile setup
                        Intent i = new Intent(SignUpActivity.this, ProfileSetupActivity.class);
                        i.putExtra("role", role);
                        startActivity(i);
                        finish();
                    } else {
                        String err = (task.getException() != null) ? task.getException().getMessage() : "Authentication failed";
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
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
                        .addOnFailureListener(e -> Toast.makeText(SignUpActivity.this, "Firestore error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).addOnFailureListener(e -> Toast.makeText(SignUpActivity.this, "Firestore read error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
