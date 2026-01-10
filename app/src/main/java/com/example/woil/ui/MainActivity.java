package com.example.woil.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.woil.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    // Keep track of the "current" fragment tag / menu id
    private String currentTag = "home";
    private int currentMenuItemId = R.id.navigation_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);

        // Restore selected tab if activity recreated
        if (savedInstanceState != null) {
            currentTag = savedInstanceState.getString("currentTag", "home");
            currentMenuItemId = savedInstanceState.getInt("currentMenuItemId", R.id.navigation_home);
            bottomNav.setSelectedItemId(currentMenuItemId);
        } else {
            // initial fragment
            openFragment(new HomeFragment(), false, "home");
            bottomNav.setSelectedItemId(R.id.navigation_home);
        }

        // Modern listener for material bottom navigation
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                // avoid re-creating the same tab fragment
                if (id == currentMenuItemId) {
                    return true;
                }

                if (id == R.id.navigation_home) {
                    openFragment(new HomeFragment(), false, "home");
                    return true;
                } else if (id == R.id.navigation_wallet) {
                    // deeper screen (hide bottom nav) -> add to back stack
                    openFragment(new WageFragment(), true, "wage");
                    return true;
                } else if (id == R.id.nav_messages) {
                    openFragment(new ChatFragment(), false, "chat");
                    return true;
                } else if (id == R.id.navigation_profile) {
                    openFragment(new ProfileFragment(), false, "profile");
                    return true;
                } else if (id == R.id.nav_guard) {
                    // example: treat as a regular tab
                    openFragment(new HomeFragment(), false, "guard"); // replace with real fragment
                    return true;
                }

                return false;
            }
        });

        // Back handling using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    // Pop backstack immediately and update bottom nav visibility
                    fm.popBackStackImmediate();
                    updateBottomNavVisibilityAfterPop();
                } else {
                    // No fragment in back stack -> behave like default (exit)
                    finish();
                }
            }
        });
    }

    /**
     * Replace content with fragment. If addToBackStack is true, the fragment will be added
     * to the back stack (useful for deeper screens where you want "back" behavior).
     *
     * @param fragment Fragment instance to display
     * @param addToBackStack if true add to back stack (and hide bottom nav)
     * @param tag unique tag for fragment
     */
    private void openFragment(Fragment fragment, boolean addToBackStack, @NonNull String tag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.nav_host_fragment, fragment, tag);

        if (addToBackStack) {
            ft.addToBackStack(tag);
            bottomNav.setVisibility(View.GONE);
        } else {
            // when switching tabs, clear any existing back stack so back acts predictably
            clearBackStack();
            bottomNav.setVisibility(View.VISIBLE);
        }

        ft.commit();

        currentTag = tag;
        // update menu item id mapping (for saved state / re-selection)
        if ("home".equals(tag)) currentMenuItemId = R.id.navigation_home;
        else if ("wage".equals(tag)) currentMenuItemId = R.id.navigation_wallet;
        else if ("chat".equals(tag)) currentMenuItemId = R.id.nav_messages;
        else if ("profile".equals(tag)) currentMenuItemId = R.id.navigation_profile;
        else currentMenuItemId = bottomNav.getSelectedItemId();
    }

    private void updateBottomNavVisibilityAfterPop() {
        // After popping the back stack, check top fragment and show bottomNav if appropriate
        Fragment top = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (top instanceof WageFragment) {
            bottomNav.setVisibility(View.GONE);
            // update menu selection to none or wage depending on desired UX
            bottomNav.setSelectedItemId(R.id.navigation_wallet);
        } else {
            bottomNav.setVisibility(View.VISIBLE);
            // set selected item to match the shown fragment's tag (if you track tags)
            // For simplicity, attempt to map by class
            if (top instanceof HomeFragment) bottomNav.setSelectedItemId(R.id.navigation_home);
            else if (top instanceof ChatFragment) bottomNav.setSelectedItemId(R.id.nav_messages);
            else if (top instanceof ProfileFragment) bottomNav.setSelectedItemId(R.id.navigation_profile);
        }
    }

    private void clearBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentTag", currentTag);
        outState.putInt("currentMenuItemId", currentMenuItemId);
    }
}
