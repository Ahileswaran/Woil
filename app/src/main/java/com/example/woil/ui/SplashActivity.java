package com.example.woil.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.woil.R;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::goNext, 800);
    }

    private void goNext() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Always go to signup if not logged in
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // Logged in → go straight to main (TEST)
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }
}
