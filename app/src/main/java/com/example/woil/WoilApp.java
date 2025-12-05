package com.example.woil;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class WoilApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // initialize Firebase (safe to call even if already auto-initialized)
        FirebaseApp.initializeApp(this);
    }
}
