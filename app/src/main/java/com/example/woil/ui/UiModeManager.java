package com.example.woil.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class UiModeManager {

    public static final String PREFS = "woil_prefs";
    public static final String KEY_UI_LEVEL = "ui_level";

    public static final String HIGH = "HIGH";
    public static final String MEDIUM = "MEDIUM";
    public static final String LOW = "LOW";

    private UiModeManager() {
        // no instance
    }

    @NonNull
    public static String getUiLevel(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_UI_LEVEL, HIGH);
    }

    public static void setUiLevel(@NonNull Context context, @NonNull String level) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_UI_LEVEL, level)
                .apply();
    }

    public static boolean isHigh(@NonNull Context context) {
        return HIGH.equals(getUiLevel(context));
    }

    public static boolean isMedium(@NonNull Context context) {
        return MEDIUM.equals(getUiLevel(context));
    }

    public static boolean isLow(@NonNull Context context) {
        return LOW.equals(getUiLevel(context));
    }
}