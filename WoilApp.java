package com.example.woil;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class WoilApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);
    }
}