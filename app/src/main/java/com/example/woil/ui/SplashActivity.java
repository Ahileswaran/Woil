package com.example.woil.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.woil.R;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    private VideoView splashVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        splashVideo = findViewById(R.id.splashVideo);

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.woil);
        splashVideo.setVideoURI(videoUri);

        splashVideo.setOnCompletionListener(mp -> goNext());

        splashVideo.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            splashVideo.start();
        });
    }

    private void goNext() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, SignUpActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }
}