package com.example.woil.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.woil.R;

public class HomeLowFragment extends Fragment {

    public HomeLowFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_low_profile_dashboard, container, false);

        bindActions(view);
        setupSwipeToSettings(view);

        return view;
    }

    private void bindActions(@NonNull View view) {
        ImageButton panicButton = view.findViewById(R.id.btn_panic);
        Button callAlert1 = view.findViewById(R.id.btn_call_alert1);
        Button callAlert2 = view.findViewById(R.id.btn_call_alert2);
        Button callAlert3 = view.findViewById(R.id.btn_call_alert3);
        Button calculateButton = view.findViewById(R.id.btn_calculate);

        ImageButton btnPhone = view.findViewById(R.id.btn_phone);
        ImageButton btnMessage = view.findViewById(R.id.btn_message);
        ImageButton btnVideo = view.findViewById(R.id.btn_video);

        if (panicButton != null) {
            panicButton.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Panic button pressed", Toast.LENGTH_SHORT).show()
            );
        }

        if (callAlert1 != null) {
            callAlert1.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Calling client for alert 1", Toast.LENGTH_SHORT).show()
            );
        }

        if (callAlert2 != null) {
            callAlert2.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Calling client for alert 2", Toast.LENGTH_SHORT).show()
            );
        }

        if (callAlert3 != null) {
            callAlert3.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Calling client for alert 3", Toast.LENGTH_SHORT).show()
            );
        }

        if (calculateButton != null) {
            calculateButton.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity())
                            .navigateToFragment(new WageFragment(), true, "wage");
                }
            });
        }

        if (btnPhone != null) {
            btnPhone.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Phone action", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnMessage != null) {
            btnMessage.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity())
                            .navigateToFragment(new ChatFragment(), true, "chat");
                }
            });
        }

        if (btnVideo != null) {
            btnVideo.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Video action", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void setupSwipeToSettings(@NonNull View rootView) {
        final View swipeTarget = rootView.findViewById(R.id.scroll_wage) != null
                ? rootView.findViewById(R.id.scroll_wage)
                : rootView;

        final GestureDetector gestureDetector = new GestureDetector(
                requireContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    private static final int SWIPE_THRESHOLD = 120;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 120;

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
                            int width = swipeTarget.getWidth();
                            boolean startedNearRight = (width == 0) || (e1.getX() > width * 0.65f);

                            if (startedNearRight
                                    && diffX < -SWIPE_THRESHOLD
                                    && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                                openSettingsFragmentWithAnimation();
                                return true;
                            }
                        }
                        return false;
                    }
                }
        );

        swipeTarget.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void openSettingsFragmentWithAnimation() {
        SettingsFragment settingsFragment = new SettingsFragment();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).navigateToFragment(settingsFragment, true, "settings");
            return;
        }

        FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();

        try {
            ft.setCustomAnimations(
                    R.anim.enter_from_right,
                    R.anim.exit_to_left,
                    R.anim.enter_from_left,
                    R.anim.exit_to_right
            );
        } catch (Resources.NotFoundException ignored) { }

        ft.replace(R.id.nav_host_fragment, settingsFragment);
        ft.addToBackStack("settings");
        ft.commit();
    }
}