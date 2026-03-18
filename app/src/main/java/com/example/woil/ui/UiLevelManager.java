package com.example.woil.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class UiLevelManager {

    public static final String UI_HIGH = "high";
    public static final String UI_MEDIUM = "medium";
    public static final String UI_LOW = "low";

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_UI_LEVEL = "pref_ui_level";

    // Allow-lists use Activity simple class-names (or feature keys you choose)
    private static final Set<String> ALLOWED_MEDIUM = new HashSet<>(Arrays.asList(
            "MediumUiActivity",    // allow this activity in MEDIUM
            "LowUiActivity"        // note: medium may include low-permitted screens if you want; keep or remove as needed
    ));

    private static final Set<String> ALLOWED_LOW = new HashSet<>(Arrays.asList(
            "LowUiActivity"       // only LowUiActivity allowed in LOW
    ));

    // Save UI level
    public static void setUiLevel(@NonNull Context ctx, @NonNull String level) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_UI_LEVEL, level).apply();
    }

    public static String getUiLevel(@NonNull Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_UI_LEVEL, UI_HIGH);
    }

    /**
     * Check whether a particular activity or feature is allowed under the current UI level.
     * Use the activity's simple class name (e.g. `MediumUiActivity`) or any key you agree on.
     */
    public static boolean isFeatureAllowed(@NonNull Context ctx, @NonNull String featureKey) {
        String level = getUiLevel(ctx);
        switch (level) {
            case UI_LOW:
                return ALLOWED_LOW.contains(featureKey);
            case UI_MEDIUM:
                // medium allows medium + low
                return ALLOWED_MEDIUM.contains(featureKey) || ALLOWED_LOW.contains(featureKey);
            case UI_HIGH:
            default:
                return true; // full access
        }
    }
}