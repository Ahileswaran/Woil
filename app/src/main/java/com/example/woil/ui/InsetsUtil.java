package com.example.woil.ui;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class InsetsUtil {

    /**
     * Applies status bar inset to the orangePanel and adds top padding to headerRoot.
     * Call from Fragment.onViewCreated(view, ...) after view inflation.
     */
    public static void applyStatusBarInsetToHeader(@NonNull final View headerRoot,
                                                   @NonNull final View orangePanel,
                                                   final int baseHeightPx) {

        ViewCompat.setOnApplyWindowInsetsListener(headerRoot, (v, insets) -> {
            // Include statusBars + displayCutout for hole-punch devices
            int types = WindowInsetsCompat.Type.statusBars()
                                            | WindowInsetsCompat.Type.displayCutout();

            int statusBarTop = insets.getInsets(types).top;

            // set orangePanel height = baseHeightPx + statusBarTop
            ViewGroup.LayoutParams lp = orangePanel.getLayoutParams();
            if (lp != null) {
                lp.height = baseHeightPx + statusBarTop;
                orangePanel.setLayoutParams(lp);
            }

            // translate orange upwards so it sits *behind* status bar
            orangePanel.setTranslationY(-statusBarTop);

            // apply top padding so header content (card) isn't under the status icons
            v.setPadding(v.getPaddingLeft(), statusBarTop, v.getPaddingRight(), v.getPaddingBottom());

            return WindowInsetsCompat.CONSUMED;
        });

// request apply insets
        ViewCompat.requestApplyInsets(headerRoot);
    }
}