package com.example.woil.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
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

    // store latest insets so we can re-apply after fragment changes
    private int lastStatusBarInset = 0;
    private int lastNavBarInset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Allow content to lay out behind system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Controller for system bar appearance
        insetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        // Transparent bars so fragment header can draw behind them
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        try {
            insetsController.show(WindowInsetsCompat.Type.systemBars());
        } catch (Throwable t) {
            Log.w(TAG, "Could not call insetsController.show(...): " + t);
        }

        try {
            insetsController.setAppearanceLightStatusBars(true);
            insetsController.setAppearanceLightNavigationBars(true);
        } catch (Throwable t) {
            Log.w(TAG, "setAppearanceLight... not supported: " + t);
        }

        applyWindowInsetsPadding();

        bottomNav = findViewById(R.id.bottom_navigation);

        // Auto-wire btn_back from fragments
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewCreated(@NonNull FragmentManager fm,
                                                      @NonNull Fragment f,
                                                      @NonNull View v,
                                                      @Nullable Bundle savedInstanceState) {
                        super.onFragmentViewCreated(fm, f, v, savedInstanceState);
                        View btnBack = v.findViewById(R.id.btn_back);
                        if (btnBack != null) {
                            btnBack.setOnClickListener(view -> onFragmentArrowBackToHome());
                        }
                    }
                }, true
        );

        if (savedInstanceState != null) {
            currentTag = savedInstanceState.getString("currentTag", "home");
            currentMenuItemId = savedInstanceState.getInt("currentMenuItemId", R.id.navigation_home);

            if (bottomNav != null) {
                bottomNav.setSelectedItemId(currentMenuItemId);
            }

            boolean bottomVisible = savedInstanceState.getBoolean(
                    "bottomNavVisible",
                    "home".equals(currentTag)
            );
            setBottomNavVisibility(bottomVisible);
        } else {
            // IMPORTANT: open selected home UI, not hardcoded HomeFragment
            openFragment(getSelectedHomeFragment(), false, "home");
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.navigation_home);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.nav_bar_black));
        }

        WindowInsetsControllerCompat wic =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightNavigationBars(false);

        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    int id = item.getItemId();

                    // avoid re-creating same tab
                    if (id == currentMenuItemId) {
                        return true;
                    }

                    if (id == R.id.navigation_home) {
                        openFragment(getSelectedHomeFragment(), false, "home");
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
                        openFragment(new WoilGuardFragment(), false, "guard");
                        return true;
                    }

                    return false;
                }
            });
        } else {
            Log.w(TAG, "bottom_navigation view not found in layout");
        }

        getSupportFragmentManager().addOnBackStackChangedListener(this::applyInsetsToCurrentFragment);

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
     * Return the currently selected home fragment based on saved UI level.
     * Requires:
     * - UiModeManager.java
     * - UiNavigator.java
     * - HomeHighFragment.java / HomeMediumFragment.java / HomeLowFragment.java
     */
    private Fragment getSelectedHomeFragment() {
        return UiNavigator.getSelectedHomeFragment(this);
    }

    /**
     * Use this after user changes HIGH / MEDIUM / LOW in Settings.
     * It clears old screens and opens the selected Home UI only.
     */
    public void reloadHomeForSelectedUi() {
        clearBackStack();

        Fragment selectedHome = getSelectedHomeFragment();
        openFragment(selectedHome, false, "home");

        if (bottomNav != null) {
            bottomNav.post(() -> {
                setBottomNavVisibility(selectedHome instanceof HomeHighFragment);
                try {
                    bottomNav.setSelectedItemId(R.id.navigation_home);
                } catch (Throwable ignored) { }
                applyBottomNavInsets(lastNavBarInset);
            });
        }
    }

    public interface HeaderConfig {
        boolean useLightStatusBarIcons();
    }

    private void applyWindowInsetsPadding() {
        final View root = findViewById(R.id.root_layout);
        final View mainContent = findViewById(R.id.main_content);

        if (root == null || mainContent == null) {
            Log.w(TAG, "root_layout or main_content not found — skipping insets");
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int topInset;
            int bottomInset;

            try {
                Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                topInset = sys.top;
                bottomInset = sys.bottom;
            } catch (Throwable t) {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
            }

            lastStatusBarInset = topInset;
            lastNavBarInset = bottomInset;

            int extra = dpToPx(8);

            boolean currentHasOrange = fragmentHasView(R.id.orange_panel);

            if (currentHasOrange) {
                mainContent.setPadding(
                        mainContent.getPaddingLeft(),
                        extra,
                        mainContent.getPaddingRight(),
                        bottomInset + extra
                );
                applyInsetsToCurrentFragment();
            } else {
                mainContent.setPadding(
                        mainContent.getPaddingLeft(),
                        topInset + extra,
                        mainContent.getPaddingRight(),
                        bottomInset + extra
                );
                clearOrangePanelAdjustments();
            }

            applyBottomNavInsets(bottomInset);

            return insets;
        });

        root.requestApplyInsets();
    }

    private boolean fragmentHasView(int viewId) {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (current == null) return false;
        View fragView = current.getView();
        return fragView != null && fragView.findViewById(viewId) != null;
    }

    private void applyInsetsToCurrentFragment() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (current == null) return;

        View fragView = current.getView();
        if (fragView == null) return;

        final View orange = fragView.findViewById(R.id.orange_panel);
        if (orange == null) {
            return;
        }

        final int baseOrangeDp = 120;
        final int baseOrangePx = dpToPx(baseOrangeDp);

        ViewGroup.LayoutParams lp = orange.getLayoutParams();
        if (lp != null) {
            int desired = baseOrangePx + lastStatusBarInset;
            if (lp.height != desired) {
                lp.height = desired;
                orange.setLayoutParams(lp);
            }
        }

        orange.setTranslationY(-lastStatusBarInset);

        boolean requestDarkIcons = true;
        if (current instanceof HeaderConfig) {
            try {
                requestDarkIcons = ((HeaderConfig) current).useLightStatusBarIcons();
            } catch (Throwable ignored) { }
        }

        try {
            insetsController.setAppearanceLightStatusBars(requestDarkIcons);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to set appearanceLightStatusBars: " + t);
        }
    }

    private void clearOrangePanelAdjustments() {
        Fragment prev = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (prev == null) return;

        View v = prev.getView();
        if (v == null) return;

        View orange = v.findViewById(R.id.orange_panel);
        if (orange != null) {
            orange.setTranslationY(0);
            ViewGroup.LayoutParams lp = orange.getLayoutParams();
            if (lp != null) {
                int base = dpToPx(120);
                if (lp.height != base) {
                    lp.height = base;
                    orange.setLayoutParams(lp);
                }
            }
        }
    }

    public void setNavigationBarAppearance(@ColorInt int navBarColor, boolean useLightNavIcons) {
        try {
            getWindow().setNavigationBarColor(navBarColor);
            if (insetsController != null) {
                insetsController.setAppearanceLightNavigationBars(useLightNavIcons);
            }
        } catch (Throwable t) {
            Log.w(TAG, "setNavigationBarAppearance failed: " + t);
        }
    }

    private void applyBottomNavInsets(int navBarInset) {
        if (bottomNav == null) return;

        final int defaultNavHeight = dpToPx(56);
        final int effectiveInset = (navBarInset > 0) ? navBarInset : defaultNavHeight;

        int extraTop = dpToPx(4);
        bottomNav.setPadding(
                bottomNav.getPaddingLeft(),
                extraTop,
                bottomNav.getPaddingRight(),
                effectiveInset
        );

        try {
            if (bottomNav.getBackground() == null) {
                bottomNav.setBackgroundColor(ContextCompat.getColor(this, R.color.nav_bar_black));
            }
        } catch (Throwable ignored) { }

        bottomNav.post(() -> {
            try {
                int bgColor = Color.TRANSPARENT;
                Drawable bg = bottomNav.getBackground();

                if (bg instanceof ColorDrawable) {
                    bgColor = ((ColorDrawable) bg).getColor();
                } else if (bg != null) {
                    int w = Math.max(1, bg.getIntrinsicWidth());
                    int h = Math.max(1, bg.getIntrinsicHeight());
                    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bmp);
                    bg.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    bg.draw(canvas);
                    bgColor = bmp.getPixel(0, 0);
                } else {
                    bgColor = ContextCompat.getColor(this, R.color.nav_bar_black);
                }

                if (bgColor == Color.TRANSPARENT) {
                    bgColor = ContextCompat.getColor(this, R.color.nav_bar_black);
                }

                getWindow().setNavigationBarColor(bgColor);

                boolean useDarkNavIcons = isColorLight(bgColor);
                try {
                    insetsController.setAppearanceLightNavigationBars(useDarkNavIcons);
                } catch (Throwable ignored) { }

            } catch (Throwable t) {
                Log.w(TAG, "applyBottomNavInsets failed: " + t);
            }
        });
    }

    private boolean isColorLight(@ColorInt int color) {
        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = color & 0xff;
        double luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
        return luminance > 0.5;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void openFragment(Fragment fragment, boolean addToBackStack, @NonNull String tag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.nav_host_fragment, fragment, tag);

        if (addToBackStack) {
            ft.addToBackStack(tag);
        } else {
            clearBackStack();
        }

        ft.commit();

        try {
            getSupportFragmentManager().executePendingTransactions();
        } catch (Throwable ignored) { }

        applyInsetsToCurrentFragment();

        // Show bottom nav only for HIGH UI home
        boolean shouldShowNav = fragment instanceof HomeHighFragment;
        setBottomNavVisibility(shouldShowNav);

        currentTag = tag;
        if ("home".equals(tag)) currentMenuItemId = R.id.navigation_home;
        else if ("wage".equals(tag)) currentMenuItemId = R.id.navigation_wallet;
        else if ("chat".equals(tag)) currentMenuItemId = R.id.nav_messages;
        else if ("profile".equals(tag)) currentMenuItemId = R.id.navigation_profile;
        else if ("guard".equals(tag)) currentMenuItemId = R.id.nav_guard;
        else if (bottomNav != null) currentMenuItemId = bottomNav.getSelectedItemId();
    }

    private void setBottomNavVisibility(boolean visible) {
        if (bottomNav == null) return;

        final View mainContent = findViewById(R.id.main_content);
        int newVis = visible ? View.VISIBLE : View.GONE;
        if (bottomNav.getVisibility() == newVis) return;

        bottomNav.setVisibility(newVis);

        if (visible) {
            bottomNav.bringToFront();
            ViewCompat.setElevation(bottomNav, dpToPx(8));

            final int defaultNavHeight = dpToPx(56);
            final int effectiveInset = (lastNavBarInset > 0) ? lastNavBarInset : defaultNavHeight;
            final int safety = dpToPx(6);

            if (mainContent != null) {
                mainContent.setPadding(
                        mainContent.getPaddingLeft(),
                        mainContent.getPaddingTop(),
                        mainContent.getPaddingRight(),
                        effectiveInset + safety
                );
            }

            bottomNav.post(() -> {
                try {
                    bottomNav.setSelectedItemId(R.id.navigation_home);
                } catch (Throwable ignored) { }

                try {
                    bottomNav.setBackgroundColor(ContextCompat.getColor(this, R.color.nav_bar_black));
                } catch (Throwable ignored) { }

                applyBottomNavInsets(effectiveInset);
                bottomNav.requestLayout();
                bottomNav.invalidate();
            });
        } else {
            if (mainContent != null) {
                mainContent.setPadding(
                        mainContent.getPaddingLeft(),
                        mainContent.getPaddingTop(),
                        mainContent.getPaddingRight(),
                        0
                );
            }
            applyBottomNavInsets(0);
        }
    }

    public void navigateHomeAndSyncNav() {
        Fragment selectedHome = getSelectedHomeFragment();
        openFragment(selectedHome, false, "home");

        if (bottomNav == null) return;

        bottomNav.post(() -> {
            setBottomNavVisibility(selectedHome instanceof HomeHighFragment);
            try {
                bottomNav.setSelectedItemId(R.id.navigation_home);
            } catch (Throwable ignored) { }
            applyBottomNavInsets(lastNavBarInset);
        });
    }

    public void onFragmentArrowBackToHome() {
        navigateHomeAndSyncNav();
    }

    private void updateBottomNavVisibilityAfterPop() {
        Fragment top = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (top instanceof HomeHighFragment) {
            setBottomNavVisibility(true);
            if (bottomNav != null) bottomNav.setSelectedItemId(R.id.navigation_home);
        } else {
            setBottomNavVisibility(false);

            if (top instanceof WageFragment && bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.navigation_wallet);
            } else if (top instanceof ChatFragment && bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_messages);
            } else if (top instanceof ProfileFragment && bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.navigation_profile);
            } else if (top instanceof WoilGuardFragment && bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_guard);
            }
        }

        applyInsetsToCurrentFragment();
    }

    private void clearBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    public void navigateToFragment(Fragment fragment, boolean addToBackStack, @NonNull String tag) {
        openFragment(fragment, addToBackStack, tag);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentTag", currentTag);
        outState.putInt("currentMenuItemId", currentMenuItemId);
        outState.putBoolean(
                "bottomNavVisible",
                bottomNav != null && bottomNav.getVisibility() == View.VISIBLE
        );
    }
}