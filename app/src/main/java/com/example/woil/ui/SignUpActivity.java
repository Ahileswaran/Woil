package com.example.woil.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.woil.R;
import com.example.woil.FirestoreHelper;
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
    private String verificationId;

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
            // Show error for role
            ((RadioButton) rgRole.getChildAt(0)).setError("Role is required");
            return;
        }
        if (rgOtpMethod.getCheckedRadioButtonId() == -1) {
            // Show error for OTP method
            ((RadioButton) rgOtpMethod.getChildAt(0)).setError("OTP method is required");
            return;
        }

        String role = ((RadioButton) findViewById(rgRole.getCheckedRadioButtonId())).getText().toString();
        String otpMethod = ((RadioButton) findViewById(rgOtpMethod.getCheckedRadioButtonId())).getText().toString().toLowerCase();

        // Send OTP
        if (otpMethod.equals("phone")) {
            startPhoneVerification(phone);
        } else if (otpMethod.equals("email")) {
            // For email, use email verification (Firebase doesn't have built-in email OTP, so send verification link)
            mAuth.createUserWithEmailAndPassword(email, "temp_password") // Temp, replace with proper flow
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            mAuth.getCurrentUser().sendEmailVerification();
                            // Prompt user to check email and verify
                        }
                    });
        }

        // After OTP verification (in callback), save user
        // firestoreHelper.createUserDoc(mAuth.getCurrentUser().getUid(), phone, role, ...);
        // Then start ProfileSetupActivity
    }

    private void startPhoneVerification(String phone) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        signInWithCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {

                    }

                    public void onVerificationFailed(Exception e) {
                        etPhone.setError("Verification failed: " + e.getMessage());
                    }

                    @Override
                    public void onCodeSent(String verId, PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = verId;
                        // Prompt user for OTP (add a dialog or new screen for OTP input)
                        // Then call verifyOtp(otp)
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyOtp(String otp) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        String phone = etPhone.getText().toString().trim();
                        String role = ((RadioButton) findViewById(rgRole.getCheckedRadioButtonId())).getText().toString();
                        firestoreHelper.createUserDoc(uid, phone, role, null, null);
                        startActivity(new Intent(SignUpActivity.this, ProfileSetupActivity.class));
                        finish();
                    } else {
                        // Error handling
                    }
                });
    }
}