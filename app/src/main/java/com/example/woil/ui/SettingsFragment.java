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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.woil.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private static final String APP_PREFS = "app_prefs";
    private static final String KEY_DARK = "pref_dark_mode";
    private static final String KEY_VOICE = "pref_voice_guidance";
    private static final String KEY_SIMPLE = "pref_simplified_layout";
    private static final String KEY_TEXT_PROGRESS = "pref_text_size_progress";

    private SwitchMaterial switchUiHigh;
    private SwitchMaterial switchUiMedium;
    private SwitchMaterial switchUiLow;
    private boolean suppressUiSwitchListener = false;

    public SettingsFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        RelativeLayout systemMainRow = view.findViewById(R.id.system_main_row);
        LinearLayout systemExpandable = view.findViewById(R.id.system_expandable);
        ImageView systemChevron = view.findViewById(R.id.ic_system_chevron);

        RelativeLayout systemUiMainRow = view.findViewById(R.id.system_ui_main_row);
        LinearLayout systemUiExpandable = view.findViewById(R.id.system_ui_expandable);
        ImageView systemUiChevron = view.findViewById(R.id.ic_system_ui_chevron);

        RelativeLayout accessibilityMainRow = view.findViewById(R.id.accessibility_main_row);
        LinearLayout accessibilityExpandable = view.findViewById(R.id.accessibility_expandable);
        ImageView accessibilityChevron = view.findViewById(R.id.ic_accessibility_chevron);

        SwitchMaterial switchTheme = view.findViewById(R.id.switch_theme);
        SwitchMaterial switchVoice = view.findViewById(R.id.switch_voice);
        SwitchMaterial switchSimple = view.findViewById(R.id.switch_simple);
        SeekBar seekTextSize = view.findViewById(R.id.seek_text_size);
        TextView tvTextSizeValue = view.findViewById(R.id.tv_text_size_value);

        switchUiHigh = view.findViewById(R.id.switch_ui_high);
        switchUiMedium = view.findViewById(R.id.switch_ui_medium);
        switchUiLow = view.findViewById(R.id.switch_ui_low);

        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> goBackToSelectedHome());
        }

        final SharedPreferences appPrefs =
                requireContext().getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);

        boolean isDark = appPrefs.getBoolean(KEY_DARK, false);
        boolean isVoice = appPrefs.getBoolean(KEY_VOICE, false);
        boolean isSimple = appPrefs.getBoolean(KEY_SIMPLE, false);
        int textProgress = appPrefs.getInt(KEY_TEXT_PROGRESS, 13);

        if (switchTheme != null) {
            switchTheme.setChecked(isDark);
            applyTheme(isDark);
            switchTheme.setOnCheckedChangeListener((buttonView, checked) -> {
                appPrefs.edit().putBoolean(KEY_DARK, checked).apply();
                applyTheme(checked);
            });
        }

        if (switchVoice != null) {
            switchVoice.setChecked(isVoice);
            switchVoice.setOnCheckedChangeListener((buttonView, checked) ->
                    appPrefs.edit().putBoolean(KEY_VOICE, checked).apply()
            );
        }

        if (switchSimple != null) {
            switchSimple.setChecked(isSimple);
            switchSimple.setOnCheckedChangeListener((buttonView, checked) -> {
                appPrefs.edit().putBoolean(KEY_SIMPLE, checked).apply();
                Toast.makeText(requireContext(),
                        checked ? "Simplified layout enabled" : "Simplified layout disabled",
                        Toast.LENGTH_SHORT).show();
            });
        }

        if (seekTextSize != null && tvTextSizeValue != null) {
            seekTextSize.setMax(30);
            seekTextSize.setProgress(textProgress);
            updateTextSizeLabel(textProgress, tvTextSizeValue);

            seekTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    updateTextSizeLabel(progress, tvTextSizeValue);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int value = seekBar.getProgress();
                    appPrefs.edit().putInt(KEY_TEXT_PROGRESS, value).apply();
                    Toast.makeText(requireContext(),
                            "Text size updated",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        bindExpandable(systemMainRow, systemExpandable, systemChevron);
        bindExpandable(systemUiMainRow, systemUiExpandable, systemUiChevron);
        bindExpandable(accessibilityMainRow, accessibilityExpandable, accessibilityChevron);

        setupUiLevelSwitches();
        setupSwipeBack(view);

        return view;
    }

    private void bindExpandable(@Nullable View mainRow,
                                @Nullable View expandable,
                                @Nullable ImageView chevron) {
        if (mainRow == null || expandable == null || chevron == null) return;

        mainRow.setOnClickListener(v -> {
            boolean expand = expandable.getVisibility() == View.GONE;
            expandable.setVisibility(expand ? View.VISIBLE : View.GONE);
            chevron.animate().rotation(expand ? 90f : 0f).setDuration(180).start();
        });
    }

    private void setupUiLevelSwitches() {
        if (switchUiHigh == null || switchUiMedium == null || switchUiLow == null) return;

        String currentLevel = UiModeManager.getUiLevel(requireContext());
        applyUiSwitchState(currentLevel);

        switchUiHigh.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressUiSwitchListener || !isChecked) return;
            onUiLevelSelected(UiModeManager.HIGH);
        });

        switchUiMedium.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressUiSwitchListener || !isChecked) return;
            onUiLevelSelected(UiModeManager.MEDIUM);
        });

        switchUiLow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressUiSwitchListener || !isChecked) return;
            onUiLevelSelected(UiModeManager.LOW);
        });
    }

    private void onUiLevelSelected(@NonNull String clickedLevel) {
        String previousLevel = UiModeManager.getUiLevel(requireContext());

        if (clickedLevel.equals(previousLevel)) {
            return;
        }

        boolean isRestrictiveChange =
                UiModeManager.HIGH.equals(previousLevel)
                        && (UiModeManager.MEDIUM.equals(clickedLevel)
                        || UiModeManager.LOW.equals(clickedLevel));

        if (isRestrictiveChange) {
            showConfirmRestrictDialog(
                    clickedLevel,
                    () -> applyUiLevelChange(clickedLevel),
                    () -> applyUiSwitchState(previousLevel)
            );
        } else {
            applyUiLevelChange(clickedLevel);
        }
    }

    private void applyUiLevelChange(@NonNull String level) {
        UiModeManager.setUiLevel(requireContext(), level);
        applyUiSwitchState(level);

        Toast.makeText(requireContext(),
                labelForLevel(level) + " selected",
                Toast.LENGTH_SHORT).show();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).reloadHomeForSelectedUi();
        }
    }

    private void applyUiSwitchState(@NonNull String level) {
        suppressUiSwitchListener = true;

        if (switchUiHigh != null) {
            switchUiHigh.setChecked(UiModeManager.HIGH.equals(level));
        }
        if (switchUiMedium != null) {
            switchUiMedium.setChecked(UiModeManager.MEDIUM.equals(level));
        }
        if (switchUiLow != null) {
            switchUiLow.setChecked(UiModeManager.LOW.equals(level));
        }

        suppressUiSwitchListener = false;
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

    private void updateTextSizeLabel(int progress, @NonNull TextView tvTextSizeValue) {
        float scale = 0.85f + (progress / 30f) * 0.45f;
        tvTextSizeValue.setText(String.format(Locale.getDefault(), "%.2fx", scale));
    }

    private void applyTheme(boolean dark) {
        if (dark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}