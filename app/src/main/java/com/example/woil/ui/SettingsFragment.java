package com.example.woil.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.woil.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_DARK = "pref_dark_mode";

    public SettingsFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Find views from the updated XML
        LinearLayout systemExpandable = view.findViewById(R.id.system_expandable);
        RelativeLayout systemMainRow = view.findViewById(R.id.system_main_row);
        ImageView chevron = view.findViewById(R.id.ic_system_chevron);
        SwitchMaterial switchTheme = view.findViewById(R.id.switch_theme);

        final SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean(KEY_DARK, false);

        // Initialize switch from stored preference
        switchTheme.setChecked(isDark);

        // Apply initial theme
        applyTheme(isDark);

        // Toggle expandable on main row click
        systemMainRow.setOnClickListener(v -> {
            if (systemExpandable.getVisibility() == View.GONE) {
                systemExpandable.setVisibility(View.VISIBLE);
                chevron.setRotation(90f); // optional visual
            } else {
                systemExpandable.setVisibility(View.GONE);
                chevron.setRotation(0f);
            }
        });

        // Theme switch listener
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            prefs.edit().putBoolean(KEY_DARK, isChecked).apply();
            // Apply immediately
            applyTheme(isChecked);
        });

        return view;
    }

    private void applyTheme(boolean dark) {
        if (dark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        // Note: AppCompatDelegate changes may recreate activities, which is expected to apply the theme.
    }
}
