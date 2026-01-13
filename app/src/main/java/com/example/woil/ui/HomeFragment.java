package com.example.woil.ui;

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

    // adjust if SettingsFragment is in different package
    // import is unnecessary if in same package; else add import com.example.woil.ui.SettingsFragment;

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

        // ---- Gesture detector for left swipe ----
        final GestureDetector gestureDetector = new GestureDetector(requireContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    private static final int SWIPE_THRESHOLD = 100;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        if (e1 == null || e2 == null) return false;
                        float diffX = e2.getX() - e1.getX();
                        float diffY = e2.getY() - e1.getY();

                        // horizontal fling
                        if (Math.abs(diffX) > Math.abs(diffY)) {
                            if (diffX < -SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                                // left swipe detected
                                openSettingsFragmentWithAnimation();
                                return true;
                            }
                        }
                        return false;
                    }
                });

        // Attach listener to root view. Return FALSE so child views (RecyclerView) still receive touch events.
        view.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // important: allow children to continue to receive events
        });

        return view;
    }

    // helper method (declared at class level, not inside onCreateView)
    private void openSettingsFragmentWithAnimation() {
        SettingsFragment settingsFragment = new SettingsFragment();

        FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();

        // Optional: provide these animation resources in res/anim/
        // If you don't have them, remove setCustomAnimations(...) or create the anim files.
        ft.setCustomAnimations(
                R.anim.enter_from_right, // enter
                R.anim.exit_to_left,     // exit
                R.anim.enter_from_left,  // popEnter
                R.anim.exit_to_right     // popExit
        );

        // NOTE: Replace R.id.fragment_container with the actual container ID in your Activity layout
        ft.replace(R.id.fragment_container, settingsFragment);
        ft.addToBackStack(null);
        ft.commit();
    }
}
