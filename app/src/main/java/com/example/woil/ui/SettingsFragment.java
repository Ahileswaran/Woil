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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_DARK = "pref_dark_mode";
    private static final String KEY_UI_LEVEL = "pref_ui_level"; // "high" / "medium" / "low"

    private MaterialButtonToggleGroup toggleGroup;
    private int previousCheckedId = View.NO_ID;
    private boolean suppressToggleListener = false;

    public SettingsFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // ---------- Existing (original) theme card views ----------
        LinearLayout systemExpandable = view.findViewById(R.id.system_expandable);
        RelativeLayout systemMainRow = view.findViewById(R.id.system_main_row);
        ImageView chevron = view.findViewById(R.id.ic_system_chevron);
        SwitchMaterial switchTheme = view.findViewById(R.id.switch_theme);

        // ---------- New System UI Selection card views ----------
        LinearLayout systemUiExpandable = view.findViewById(R.id.system_ui_expandable);
        RelativeLayout systemUiMainRow = view.findViewById(R.id.system_ui_main_row);
        ImageView systemUiChevron = view.findViewById(R.id.ic_system_ui_chevron);

        // ---------- Toggle group (buttons) ----------
        toggleGroup = view.findViewById(R.id.system_ui_toggle_group);
        MaterialButton btnHigh = view.findViewById(R.id.btn_ui_high);
        MaterialButton btnMedium = view.findViewById(R.id.btn_ui_medium);
        MaterialButton btnLow = view.findViewById(R.id.btn_ui_low);

        final SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean(KEY_DARK, false);

        // Theme switch init + listener
        if (switchTheme != null) {
            switchTheme.setChecked(isDark);
            applyTheme(isDark);
            switchTheme.setOnCheckedChangeListener((buttonView, checked) -> {
                prefs.edit().putBoolean(KEY_DARK, checked).apply();
                applyTheme(checked);
            });
        }

        // Toggle original theme expandable
        if (systemMainRow != null && systemExpandable != null && chevron != null) {
            systemMainRow.setOnClickListener(v -> {
                if (systemExpandable.getVisibility() == View.GONE) {
                    systemExpandable.setVisibility(View.VISIBLE);
                    chevron.setRotation(90f);
                } else {
                    systemExpandable.setVisibility(View.GONE);
                    chevron.setRotation(0f);
                }
            });
        }

        // Toggle new System UI Selection expandable
        if (systemUiMainRow != null && systemUiExpandable != null && systemUiChevron != null) {
            systemUiMainRow.setOnClickListener(v -> {
                if (systemUiExpandable.getVisibility() == View.GONE) {
                    systemUiExpandable.setVisibility(View.VISIBLE);
                    systemUiChevron.setRotation(90f);
                } else {
                    systemUiExpandable.setVisibility(View.GONE);
                    systemUiChevron.setRotation(0f);
                }
            });
        }

        // Setup UI level toggle group (persistence + confirmation dialog)
        String uiLevel = prefs.getString(KEY_UI_LEVEL, UiLevelManager.UI_HIGH); // default HIGH
        int initialCheckedId = idForLevel(uiLevel);
        previousCheckedId = initialCheckedId;

        if (toggleGroup != null) {
            suppressToggleListener = true;
            toggleGroup.check(initialCheckedId);
            suppressToggleListener = false;

            toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (suppressToggleListener) return;
                if (!isChecked) return; // only react on button-checked

                final String clickedLevel = levelForId(checkedId);
                final String previousLevel = levelForId(previousCheckedId);

                // Confirm when switching away from HIGH to MEDIUM/LOW (restrictive)
                if (UiLevelManager.UI_HIGH.equals(previousLevel) &&
                        (UiLevelManager.UI_MEDIUM.equals(clickedLevel) || UiLevelManager.UI_LOW.equals(clickedLevel))) {

                    showConfirmRestrictDialog(clickedLevel, () -> {
                        // user confirmed
                        saveUiLevel(clickedLevel, prefs);
                        previousCheckedId = checkedId;
                        UiLevelManager.setUiLevel(requireContext(), clickedLevel);
                    }, () -> {
                        // user cancelled -> revert toggle selection
                        suppressToggleListener = true;
                        toggleGroup.check(previousCheckedId);
                        suppressToggleListener = false;
                    });

                } else {
                    // Normal change (e.g., MEDIUM->HIGH or MEDIUM<->LOW)
                    saveUiLevel(clickedLevel, prefs);
                    previousCheckedId = checkedId;
                    UiLevelManager.setUiLevel(requireContext(), clickedLevel);
                }
            });
        }

        return view;
    }

    private void saveUiLevel(String level, SharedPreferences prefs) {
        prefs.edit().putString(KEY_UI_LEVEL, level).apply();
    }

    private void showConfirmRestrictDialog(String targetLevel, Runnable onConfirm, Runnable onCancel) {
        String message = "Switching to " + labelForLevel(targetLevel) +
                " will restrict some app features (compact UI). Do you want to proceed?";
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm UI change")
                .setMessage(message)
                .setPositiveButton("Proceed", (d, which) -> onConfirm.run())
                .setNegativeButton("Cancel", (d, which) -> onCancel.run())
                .setCancelable(true)
                .show();
    }

    private int idForLevel(String level) {
        if (UiLevelManager.UI_HIGH.equals(level)) return R.id.btn_ui_high;
        if (UiLevelManager.UI_MEDIUM.equals(level)) return R.id.btn_ui_medium;
        if (UiLevelManager.UI_LOW.equals(level)) return R.id.btn_ui_low;
        return R.id.btn_ui_high;
    }

    private String levelForId(int id) {
        if (id == R.id.btn_ui_high) return UiLevelManager.UI_HIGH;
        if (id == R.id.btn_ui_medium) return UiLevelManager.UI_MEDIUM;
        if (id == R.id.btn_ui_low) return UiLevelManager.UI_LOW;
        return UiLevelManager.UI_HIGH;
    }

    private String labelForLevel(String level) {
        switch (level) {
            case UiLevelManager.UI_MEDIUM: return "Medium Level UI";
            case UiLevelManager.UI_LOW: return "Low Level UI";
            default: return "High Level UI (Default)";
        }
    }

    private void applyTheme(boolean dark) {
        if (dark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        // activities may be recreated by AppCompatDelegate to apply theme
    }
}