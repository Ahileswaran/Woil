package com.example.woil.ui;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.woil.R;

public class LowUiActivity extends AppCompatActivity {

    public static final String FEATURE_KEY = "LowUiActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!UiLevelManager.isFeatureAllowed(this, FEATURE_KEY)) {
            new AlertDialog.Builder(this)
                    .setTitle("Unavailable in current UI mode")
                    .setMessage("This screen is unavailable in the current UI mode. Switch to High UI to proceed?")
                    .setPositiveButton("Switch to High", (dialog, which) -> {
                        UiLevelManager.setUiLevel(this, UiLevelManager.UI_HIGH);
                        recreate();
                    })
                    .setNegativeButton("Close", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }

        setContentView(R.layout.activity_low_profile_dashboard);
        // TODO: initialize low-density UI controls
    }
}