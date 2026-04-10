package com.example.woil.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.woil.R;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private static final String APP_PREFS = "app_prefs";
    private static final String KEY_DARK = "pref_dark_mode";

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

        LinearLayout systemExpandable = view.findViewById(R.id.system_expandable);
        RelativeLayout systemMainRow = view.findViewById(R.id.system_main_row);
        ImageView chevron = view.findViewById(R.id.ic_system_chevron);
        SwitchMaterial switchTheme = view.findViewById(R.id.switch_theme);

        LinearLayout systemUiExpandable = view.findViewById(R.id.system_ui_expandable);
        RelativeLayout systemUiMainRow = view.findViewById(R.id.system_ui_main_row);
        ImageView systemUiChevron = view.findViewById(R.id.ic_system_ui_chevron);
        toggleGroup = view.findViewById(R.id.system_ui_toggle_group);

        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> goBackToSelectedHome());
        }

        final SharedPreferences appPrefs =
                requireContext().getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);

        boolean isDark = appPrefs.getBoolean(KEY_DARK, false);

        if (switchTheme != null) {
            switchTheme.setChecked(isDark);
            applyTheme(isDark);

            switchTheme.setOnCheckedChangeListener((buttonView, checked) -> {
                appPrefs.edit().putBoolean(KEY_DARK, checked).apply();
                applyTheme(checked);
            });
        }

        if (systemMainRow != null && systemExpandable != null && chevron != null) {
            systemMainRow.setOnClickListener(v -> {
                boolean expand = systemExpandable.getVisibility() == View.GONE;
                systemExpandable.setVisibility(expand ? View.VISIBLE : View.GONE);
                chevron.setRotation(expand ? 90f : 0f);
            });
        }

        if (systemUiMainRow != null && systemUiExpandable != null && systemUiChevron != null) {
            systemUiMainRow.setOnClickListener(v -> {
                boolean expand = systemUiExpandable.getVisibility() == View.GONE;
                systemUiExpandable.setVisibility(expand ? View.VISIBLE : View.GONE);
                systemUiChevron.setRotation(expand ? 90f : 0f);
            });
        }

        setupUiLevelToggleGroup();
        setupSwipeBack(view);

        return view;
    }

    private void setupUiLevelToggleGroup() {
        if (toggleGroup == null) return;

        String currentLevel = UiModeManager.getUiLevel(requireContext());
        int initialCheckedId = idForLevel(currentLevel);
        previousCheckedId = initialCheckedId;

        suppressToggleListener = true;
        toggleGroup.check(initialCheckedId);
        suppressToggleListener = false;

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (suppressToggleListener) return;
            if (!isChecked) return;

            final String clickedLevel = levelForId(checkedId);
            final String previousLevel = levelForId(previousCheckedId);

            if (clickedLevel.equals(previousLevel)) {
                return;
            }

            boolean isRestrictiveChange =
                    UiModeManager.HIGH.equals(previousLevel)
                            && (UiModeManager.MEDIUM.equals(clickedLevel)
                            || UiModeManager.LOW.equals(clickedLevel));

            if (isRestrictiveChange) {
                showConfirmRestrictDialog(clickedLevel,
                        () -> applyUiLevelChange(clickedLevel, checkedId),
                        this::revertUiSelection);
            } else {
                applyUiLevelChange(clickedLevel, checkedId);
            }
        });
    }

    private void applyUiLevelChange(@NonNull String level, int checkedId) {
        UiModeManager.setUiLevel(requireContext(), level);
        previousCheckedId = checkedId;

        Toast.makeText(requireContext(),
                labelForLevel(level) + " selected",
                Toast.LENGTH_SHORT).show();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).reloadHomeForSelectedUi();
        }
    }

    private void revertUiSelection() {
        if (toggleGroup == null) return;

        suppressToggleListener = true;
        toggleGroup.check(previousCheckedId);
        suppressToggleListener = false;
    }

    private void setupSwipeBack(@NonNull View rootView) {
        final GestureDetector gestureDetector = new GestureDetector(
                requireContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    private static final int SWIPE_THRESHOLD = 100;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {
                        if (e1 == null || e2 == null) return false;

                        float diffX = e2.getX() - e1.getX();
                        float diffY = e2.getY() - e1.getY();

                        if (Math.abs(diffX) > Math.abs(diffY)) {
                            int width = rootView.getWidth();
                            boolean startedNearLeft = (width == 0) || (e1.getX() < width * 0.4f);

                            if (startedNearLeft
                                    && diffX > SWIPE_THRESHOLD
                                    && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                                goBackToSelectedHome();
                                return true;
                            }
                        }
                        return false;
                    }
                }
        );

        rootView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void goBackToSelectedHome() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).navigateHomeAndSyncNav();
            return;
        }

        FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();

        try {
            ft.setCustomAnimations(
                    R.anim.enter_from_left,
                    R.anim.exit_to_right,
                    R.anim.enter_from_right,
                    R.anim.exit_to_left
            );
        } catch (Resources.NotFoundException ignored) { }

        ft.replace(R.id.nav_host_fragment, UiNavigator.getSelectedHomeFragment(requireContext()));
        ft.commit();
    }

    private void showConfirmRestrictDialog(@NonNull String targetLevel,
                                           @NonNull Runnable onConfirm,
                                           @NonNull Runnable onCancel) {
        String message = "Switching to " + labelForLevel(targetLevel)
                + " will use a simpler layout. Do you want to continue?";

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm UI change")
                .setMessage(message)
                .setPositiveButton("Proceed", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Cancel", (dialog, which) -> onCancel.run())
                .setOnCancelListener(dialog -> onCancel.run())
                .show();
    }

    private int idForLevel(@Nullable String level) {
        if (UiModeManager.MEDIUM.equals(level)) return R.id.btn_ui_medium;
        if (UiModeManager.LOW.equals(level)) return R.id.btn_ui_low;
        return R.id.btn_ui_high;
    }

    @NonNull
    private String levelForId(int id) {
        if (id == R.id.btn_ui_medium) return UiModeManager.MEDIUM;
        if (id == R.id.btn_ui_low) return UiModeManager.LOW;
        return UiModeManager.HIGH;
    }

    @NonNull
    private String labelForLevel(@NonNull String level) {
        switch (level) {
            case UiModeManager.MEDIUM:
                return "Medium Level UI";
            case UiModeManager.LOW:
                return "Low Level UI";
            default:
                return "High Level UI";
        }
    }

    private void applyTheme(boolean dark) {
        if (dark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}