package com.example.woil.ui;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.woil.R;

public class MediumUiActivity extends AppCompatActivity {

    public static final String FEATURE_KEY = "MediumUiActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check UI-level permission
        if (!UiLevelManager.isFeatureAllowed(this, FEATURE_KEY)) {
            new AlertDialog.Builder(this)
                    .setTitle("Unavailable in current UI mode")
                    .setMessage("This screen requires the High Level UI. Switch to High UI now?")
                    .setPositiveButton("Switch to High", (dialog, which) -> {
                        UiLevelManager.setUiLevel(this, UiLevelManager.UI_HIGH);
                        // Recreate so UI-level changes can apply if needed
                        recreate();
                    })
                    .setNegativeButton("Close", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
            return; // stop further initialization until user chooses
        }

        setContentView(R.layout.activity_dashboard_medium);
        // TODO: initialize your medium-specific UI here (RecyclerViews, buttons, etc.)
    }
}