package com.example.woil.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.woil.R;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword, etPhone;
    private Button btnLogin;
    private TextView tvForgetPassword, tvSignUpLink;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgetPassword = findViewById(R.id.tvForgetPassword);
        tvSignUpLink = findViewById(R.id.tvSignUpLink);

        btnLogin.setOnClickListener(v -> loginUser());

        tvForgetPassword.setOnClickListener(v -> {
            // Implement forget password
            String email = etUsername.getText().toString().trim();
            if (!TextUtils.isEmpty(email)) {
                mAuth.sendPasswordResetEmail(email);
            }
        });

        tvSignUpLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone is required");
            return;
        }

        // Use email/password or phone-based login; here assuming email as username
        mAuth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        etPassword.setError("Invalid credentials");
                    }
                });
    }
}