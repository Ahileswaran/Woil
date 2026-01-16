package com.example.woil.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.woil.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNav;

    // Keep track of the "current" fragment tag / menu id
    private String currentTag = "home";
    private int currentMenuItemId = R.id.navigation_home;

    // Insets controller for system bars control
    private WindowInsetsControllerCompat insetsController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate layout
        setContentView(R.layout.activity_main);

        // Allow content to lay out behind system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Create controller for system bars
        insetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        // Make status & navigation bar backgrounds transparent
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        // Show system bars (we don't want immersive/hide)
        try {
            insetsController.show(WindowInsetsCompat.Type.systemBars());
        } catch (Throwable t) {
            Log.w(TAG, "Could not call insetsController.show(...): " + t);
        }

        // Use dark icons (true) or light icons (false) depending on your header color
        try {
            insetsController.setAppearanceLightStatusBars(true);
            insetsController.setAppearanceLightNavigationBars(true);
        } catch (Throwable t) {
            // ignore if not available on this platform/compat version
            Log.w(TAG, "setAppearanceLight... not supported: " + t);
        }

        // Apply safe padding (robust method)
        applyWindowInsetsPadding();

        // --- existing initialization ---
        bottomNav = findViewById(R.id.bottom_navigation);

        // Restore selected tab if activity recreated
        if (savedInstanceState != null) {
            currentTag = savedInstanceState.getString("currentTag", "home");
            currentMenuItemId = savedInstanceState.getInt("currentMenuItemId", R.id.navigation_home);
            if (bottomNav != null) bottomNav.setSelectedItemId(currentMenuItemId);
        } else {
            // initial fragment
            openFragment(new HomeFragment(), false, "home");
            if (bottomNav != null) bottomNav.setSelectedItemId(R.id.navigation_home);
        }

        // Modern listener for material bottom navigation
        if (bottomNav != null) {
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
                        openFragment(new WageFragment(), true, "wage");
                        return true;
                    } else if (id == R.id.nav_messages) {
                        openFragment(new ChatFragment(), false, "chat");
                        return true;
                    } else if (id == R.id.navigation_profile) {
                        openFragment(new ProfileFragment(), false, "profile");
                        return true;
                    } else if (id == R.id.nav_guard) {
                        openFragment(new HomeFragment(), false, "guard");
                        return true;
                    }

                    return false;
                }
            });
        } else {
            Log.w(TAG, "bottom_navigation view not found in layout (id: R.id.bottom_navigation)");
        }

        // Back handling using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStackImmediate();
                    updateBottomNavVisibilityAfterPop();
                } else {
                    finish();
                }
            }
        });
    }

    /**
     * Robust inset application: uses legacy getters which exist across compat versions.
     * This avoids runtime class/method differences that were causing crashes.
     */
    private void applyWindowInsetsPadding() {
        final View root = findViewById(R.id.root_layout);
        final View mainContent = findViewById(R.id.main_content);

        if (root == null || mainContent == null) {
            Log.w(TAG, "root_layout or main_content not found — skipping insets application");
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int topInset;
            int bottomInset;

            // Prefer modern API if available — but don't crash if it's not
            try {
                // some support versions will accept the Type constant here
                topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            } catch (Throwable t) {
                // Fallback to legacy values (always present)
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
            }

            // Optional: extra spacing (8dp)
            int extra = dpToPx(8);

            mainContent.setPadding(
                    mainContent.getPaddingLeft(),
                    topInset + extra,
                    mainContent.getPaddingRight(),
                    bottomInset + extra
            );

            // Return the insets so children can react as well
            return insets;
        });

        // Request insets right away
        root.requestApplyInsets();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Replace content with fragment. If addToBackStack is true, the fragment will be added
     * to the back stack (useful for deeper screens where you want "back" behavior).
     */
    private void openFragment(Fragment fragment, boolean addToBackStack, @NonNull String tag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.nav_host_fragment, fragment, tag);

        if (addToBackStack) {
            ft.addToBackStack(tag);
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
        } else {
            clearBackStack();
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
        }

        ft.commit();

        currentTag = tag;
        if ("home".equals(tag)) currentMenuItemId = R.id.navigation_home;
        else if ("wage".equals(tag)) currentMenuItemId = R.id.navigation_wallet;
        else if ("chat".equals(tag)) currentMenuItemId = R.id.nav_messages;
        else if ("profile".equals(tag)) currentMenuItemId = R.id.navigation_profile;
        else if (bottomNav != null) currentMenuItemId = bottomNav.getSelectedItemId();
    }

    private void updateBottomNavVisibilityAfterPop() {
        Fragment top = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (top instanceof WageFragment) {
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
                bottomNav.setSelectedItemId(R.id.navigation_wallet);
            }
        } else {
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
                if (top instanceof HomeFragment) bottomNav.setSelectedItemId(R.id.navigation_home);
                else if (top instanceof ChatFragment) bottomNav.setSelectedItemId(R.id.nav_messages);
                else if (top instanceof ProfileFragment) bottomNav.setSelectedItemId(R.id.navigation_profile);
            }
        }
    }

    private void clearBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    // Public helper for fragments to request navigation via MainActivity's openFragment logic
    public void navigateToFragment(Fragment fragment, boolean addToBackStack, @NonNull String tag) {
        openFragment(fragment, addToBackStack, tag);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentTag", currentTag);
        outState.putInt("currentMenuItemId", currentMenuItemId);
    }
}
