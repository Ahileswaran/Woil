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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.woil.R;

public class HomeMediumFragment extends Fragment {

    public HomeMediumFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_dashboard_medium, container, false);

        bindActions(view);
        setupExpandableSections(view);
        setupSwipeToSettings(view);

        return view;
    }

    private void bindActions(@NonNull View view) {
        ImageButton panicButton = view.findViewById(R.id.btn_panic_medium);

        View profileCard = view.findViewById(R.id.card_profile);
        View jobAlertsCard = view.findViewById(R.id.card_job_alerts);
        View wageCard = view.findViewById(R.id.card_wage_calc);
        View guardCard = view.findViewById(R.id.card_woil_guard);

        Button btnCallAlerts = view.findViewById(R.id.btn_call_alerts);
        Button btnCalcMedium = view.findViewById(R.id.btn_calc_medium);

        ImageButton btnPhone = view.findViewById(R.id.btn_phone);
        ImageButton btnMessage = view.findViewById(R.id.btn_message);
        ImageButton btnVideo = view.findViewById(R.id.btn_video);

        if (panicButton != null) {
            panicButton.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Panic button pressed", Toast.LENGTH_SHORT).show()
            );
        }

        if (profileCard != null) {
            profileCard.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity())
                            .navigateToFragment(new ProfileFragment(), true, "profile");
                }
            });
        }

        if (jobAlertsCard != null) {
            jobAlertsCard.setOnClickListener(v -> toggleSection(
                    view,
                    R.id.content_job_alerts,
                    R.id.chev_job_alerts
            ));
        }

        if (wageCard != null) {
            wageCard.setOnClickListener(v -> toggleSection(
                    view,
                    R.id.content_wage_calc,
                    R.id.chev_wage_calc
            ));
        }

        if (guardCard != null) {
            guardCard.setOnClickListener(v -> toggleSection(
                    view,
                    R.id.content_woil_guard,
                    R.id.chev_woil_guard
            ));
        }

        if (btnCallAlerts != null) {
            btnCallAlerts.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Calling client from alerts", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnCalcMedium != null) {
            btnCalcMedium.setOnClickListener(v -> {
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

    private void setupExpandableSections(@NonNull View view) {
        bindExpandable(
                view,
                R.id.header_user_profile,
                R.id.content_user_profile,
                R.id.chev_user_profile
        );

        bindExpandable(
                view,
                R.id.header_job_alerts,
                R.id.content_job_alerts,
                R.id.chev_job_alerts
        );

        bindExpandable(
                view,
                R.id.header_wage_calc,
                R.id.content_wage_calc,
                R.id.chev_wage_calc
        );

        bindExpandable(
                view,
                R.id.header_woil_guard,
                R.id.content_woil_guard,
                R.id.chev_woil_guard
        );
    }

    private void bindExpandable(@NonNull View root,
                                int headerId,
                                int contentId,
                                int chevronId) {

        View header = root.findViewById(headerId);
        View content = root.findViewById(contentId);
        ImageView chevron = root.findViewById(chevronId);

        if (header == null || content == null || chevron == null) return;

        header.setOnClickListener(v -> {
            boolean expand = content.getVisibility() == View.GONE;
            content.setVisibility(expand ? View.VISIBLE : View.GONE);
            chevron.animate().rotation(expand ? 180f : 0f).setDuration(180).start();
        });
    }

    private void toggleSection(@NonNull View root, int contentId, int chevronId) {
        View content = root.findViewById(contentId);
        ImageView chevron = root.findViewById(chevronId);

        if (content == null || chevron == null) return;

        boolean expand = content.getVisibility() == View.GONE;
        content.setVisibility(expand ? View.VISIBLE : View.GONE);
        chevron.animate().rotation(expand ? 180f : 0f).setDuration(180).start();
    }

    private void setupSwipeToSettings(@NonNull View rootView) {
        final View swipeTarget = rootView.findViewById(R.id.scroll_main) != null
                ? rootView.findViewById(R.id.scroll_main)
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