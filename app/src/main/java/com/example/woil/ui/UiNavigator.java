package com.example.woil.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public final class UiNavigator {

    private UiNavigator() {
        // no instance
    }

    @NonNull
    public static Fragment getSelectedHomeFragment(@NonNull Context context) {
        String level = UiModeManager.getUiLevel(context);

        switch (level) {
            case UiModeManager.MEDIUM:
                return new HomeMediumFragment();

            case UiModeManager.LOW:
                return new HomeLowFragment();

            case UiModeManager.HIGH:
            default:
                return new HomeHighFragment();
        }
    }
}