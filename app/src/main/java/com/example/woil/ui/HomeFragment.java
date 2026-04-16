package com.example.woil.ui;

import android.content.Intent;
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

    public HomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        RecyclerView rvTimeline = view.findViewById(R.id.rvTimeline);

        // categories
        List<CategoryModel> categoryList = new ArrayList<>();
        categoryList.add(new CategoryModel("Cleaning", R.drawable.cleaning));
        categoryList.add(new CategoryModel("Electrician", R.drawable.electic));
        categoryList.add(new CategoryModel("Caregiver", R.drawable.caregiver));
        categoryList.add(new CategoryModel("Appliance Repair", R.drawable.repair));
        categoryList.add(new CategoryModel("Masonry", R.drawable.masanory));
        categoryList.add(new CategoryModel("laundry", R.drawable.landury));
        categoryList.add(new CategoryModel("Painting", R.drawable.painting));
        categoryList.add(new CategoryModel("Plumbing", R.drawable.plumbing));
        categoryList.add(new CategoryModel("Gardening", R.drawable.gardining));
        categoryList.add(new CategoryModel("Carpentry", R.drawable.carpentary));

        rvCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // --- IMPORTANT: pass the click listener required by the new CategoryAdapter constructor ---
        CategoryAdapter catAdapter = new CategoryAdapter(categoryList, getContext(), new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClicked(CategoryModel category) {
                // start JobsByCategoryActivity and pass the category title
                Intent i = new Intent(requireContext(), JobsByCategoryActivity.class);
                i.putExtra("category", category.title);
                startActivity(i);
            }
        });
        rvCategories.setAdapter(catAdapter);

        // timeline
        List<TimelineModel> timelineList = new ArrayList<>();
        timelineList.add(new TimelineModel("Fix sink leak", "Pending", "Today 2PM"));
        timelineList.add(new TimelineModel("Install ceiling fan", "Completed", "Yesterday"));
        timelineList.add(new TimelineModel("Paint living room", "Ongoing", "Tomorrow"));

        rvTimeline.setLayoutManager(new LinearLayoutManager(getContext()));
        TimelineAdapter tAdapter = new TimelineAdapter(timelineList);
        rvTimeline.setAdapter(tAdapter);

        // ---- Gesture detector for right-edge -> left swipe ----
        final View rootView = view; // fragment root

        final GestureDetector gestureDetector = new GestureDetector(requireContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    private static final int SWIPE_THRESHOLD = 100;       // pixels
                    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

                    @Override
                    public boolean onDown(MotionEvent e) {
                        // Important: return true so subsequent gestures (fling) are detected.
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        if (e1 == null || e2 == null) return false;

                        float diffX = e2.getX() - e1.getX();
                        float diffY = e2.getY() - e1.getY();

                        // Make sure it's mostly horizontal
                        if (Math.abs(diffX) > Math.abs(diffY)) {
                            int width = rootView.getWidth();
                            // Consider fling started near right edge (60% or more to the right)
                            boolean startedNearRight = (width == 0) || (e1.getX() > width * 0.6f);

                            if (startedNearRight && diffX < -SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                                // Detected right-edge -> left fling
                                openSettingsFragmentWithAnimation();
                                return true;
                            }
                        }
                        return false;
                    }
                });

        // listener to root view. return true from onTouch only if the gestureDetector handled it.
        rootView.setOnTouchListener((v, event) -> {
            boolean handled = gestureDetector.onTouchEvent(event);
            // if gesture detector handled it (e.g. recognized a fling), consume it
            return handled;
            // returning false allows children (RecyclerView) to keep receiving touch events
        });

        return view;
    }

    // helper method to open settings
    private void openSettingsFragmentWithAnimation() {
        SettingsFragment settingsFragment = new SettingsFragment();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).navigateToFragment(settingsFragment, true, "settings");
            return;
        }

        FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();

        // Optional animations (guarded)
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