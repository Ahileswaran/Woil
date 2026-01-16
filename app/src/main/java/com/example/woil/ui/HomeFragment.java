package com.example.woil.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.CategoryAdapter;
import com.example.woil.CategoryModel;
import com.example.woil.R;
import com.example.woil.TimelineAdapter;
import com.example.woil.TimelineModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        RecyclerView rvTimeline = view.findViewById(R.id.rvTimeline);

        List<CategoryModel> categoryList = new ArrayList<>();
        categoryList.add(new CategoryModel("Plumbing", R.drawable.ic_plumber));
        categoryList.add(new CategoryModel("Electrician", R.drawable.ic_electrician));
        categoryList.add(new CategoryModel("Cleaning", R.drawable.ic_cleaning));

        rvCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        CategoryAdapter catAdapter = new CategoryAdapter(categoryList, getContext());
        rvCategories.setAdapter(catAdapter);

        List<TimelineModel> timelineList = new ArrayList<>();
        timelineList.add(new TimelineModel("Fix sink leak", "Pending", "Today 2PM"));
        timelineList.add(new TimelineModel("Install ceiling fan", "Completed", "Yesterday"));
        timelineList.add(new TimelineModel("Paint living room", "Ongoing", "Tomorrow"));

        rvTimeline.setLayoutManager(new LinearLayoutManager(getContext()));
        TimelineAdapter tAdapter = new TimelineAdapter(timelineList);
        rvTimeline.setAdapter(tAdapter);

        // ---- Gesture detector for right-edge -> left swipe ----
        final View rootView = view; // capture root view for width checks

        final GestureDetector gestureDetector = new GestureDetector(requireContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    private static final int SWIPE_THRESHOLD = 100;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        if (e1 == null || e2 == null) return false;
                        float diffX = e2.getX() - e1.getX();
                        float diffY = e2.getY() - e1.getY();

                        // only consider mostly-horizontal flings
                        if (Math.abs(diffX) > Math.abs(diffY)) {
                            // ensure fling started near the right edge to avoid interfering with normal list scrolls
                            int width = rootView.getWidth();
                            // if width isn't ready, still allow normal check (guard)
                            boolean startedNearRight = (width == 0) || (e1.getX() > width * 0.6f);

                            if (startedNearRight && diffX < -SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                                openSettingsFragmentWithAnimation();
                                return true;
                            }
                        }
                        return false;
                    }
                });

        // Attach listener to root view. Return FALSE so child views (RecyclerView) still receive touch events.
        rootView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // allow children to continue receiving events (important for RecyclerView)
        });

        return view;
    }

    // ---- helper method (class-level) ----
    private void openSettingsFragmentWithAnimation() {
        // Create the settings fragment
        SettingsFragment settingsFragment = new SettingsFragment();

        // Option A: use MainActivity navigation helper so bottom nav & backstack behavior is consistent
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).navigateToFragment(settingsFragment, true, "settings");
            return;
        }

        // Fallback: if Activity isn't MainActivity, do direct fragment transaction.
        // NOTE: Use your nav host id (nav_host_fragment) so it replaces the correct container.
        FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();

        // Optional: animations (ensure these anim resources exist; otherwise remove this block)
        try {
            ft.setCustomAnimations(
                    R.anim.enter_from_right,
                    R.anim.exit_to_left,
                    R.anim.enter_from_left,
                    R.anim.exit_to_right
            );
        } catch (Resources.NotFoundException ignored) {
            // If animations missing, continue without animations.
        }

        ft.replace(R.id.nav_host_fragment, settingsFragment);
        ft.addToBackStack("settings");
        ft.commit();
    }
}
