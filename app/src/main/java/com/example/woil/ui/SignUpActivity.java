package com.example.woil.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.woil.FirestoreHelper;
import com.example.woil.R;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class SignUpActivity extends AppCompatActivity {

    private EditText etUsername, etPhone, etEmail, etNic;
    private RadioGroup rgRole, rgOtpMethod;
    private Button btnSignUp;
    private TextView tvLoginLink;

    private FirebaseAuth mAuth;
    private FirestoreHelper firestoreHelper;

    private String verificationId; // set in onCodeSent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        mAuth = FirebaseAuth.getInstance();
        firestoreHelper = new FirestoreHelper();

        etUsername = findViewById(R.id.etUsername);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etNic = findViewById(R.id.etNic);
        rgRole = findViewById(R.id.rgRole);
        rgOtpMethod = findViewById(R.id.rgOtpMethod);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        btnSignUp.setOnClickListener(v -> validateAndSignUp());

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void validateAndSignUp() {
        String username = etUsername.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String nic = etNic.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone number is required");
            return;
        }
        if (rgRole.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rgOtpMethod.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select OTP method", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = ((RadioButton) findViewById(rgRole.getCheckedRadioButtonId())).getText().toString();
        String otpMethod = ((RadioButton) findViewById(rgOtpMethod.getCheckedRadioButtonId())).getText().toString().toLowerCase();

        if (otpMethod.equals("phone")) {
            startPhoneVerification(phone, username, role, email, nic);
        } else {
            // Email OTP not implemented because SRS uses Firebase Auth (email verification requires password or link flows).
            Toast.makeText(this, "Email verification flow not implemented. Use phone OTP.", Toast.LENGTH_LONG).show();
        }
    }

    private void startPhoneVerification(String phone, String username, String role, String email, String nic) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                // Auto-retrieved or instant verification
                                signInWithCredential(credential, username, role, email, nic, phone);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                Toast.makeText(SignUpActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onCodeSent(@NonNull String verId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                super.onCodeSent(verId, token);
                                verificationId = verId;
                                // Show a dialog to let user enter the received OTP code
                                promptForOtp(username, role, email, nic, phone);
                            }
                        })
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void promptForOtp(String username, String role, String email, String nic, String phone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter OTP");

        LayoutInflater inflater = this.getLayoutInflater();
        final EditText input = new EditText(this);
        input.setHint("OTP code");
        builder.setView(input);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(SignUpActivity.this, "Enter OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            signInWithCredential(credential, username, role, email, nic, phone);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void signInWithCredential(PhoneAuthCredential credential, String username, String role, String email, String nic, String phone) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Authenticated (FirebaseAuth holds credentials). Now create users/{uid} doc per SRS (no password)
                String uid = mAuth.getCurrentUser().getUid();
                firestoreHelper.createUserDoc(uid, phone, role, email, username, true);
                // Pass NIC to profile setup via intent extras if needed
                Intent i = new Intent(SignUpActivity.this, ProfileSetupActivity.class);
                i.putExtra("nic", nic);
                startActivity(i);
                finish();
            } else {
                Toast.makeText(SignUpActivity.this, "Sign in failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
