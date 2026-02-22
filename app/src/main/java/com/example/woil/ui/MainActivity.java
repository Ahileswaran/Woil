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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.content.ContextCompat;
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

        // Inflate layout (your XML is unchanged)
        setContentView(R.layout.activity_main);

        // Allow content to lay out behind system bars (we will handle insets manually)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Controller for system bar appearance
        insetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        // Make status & navigation backgrounds transparent so fragment header can be drawn behind them
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        // Ensure system bars are visible (we are not using immersive)
        try {
            insetsController.show(WindowInsetsCompat.Type.systemBars());
        } catch (Throwable t) {
            Log.w(TAG, "Could not call insetsController.show(...): " + t);
        }

        // Default icon appearance; fragments can override via HeaderConfig (see below)
        try {
            insetsController.setAppearanceLightStatusBars(true);
            insetsController.setAppearanceLightNavigationBars(true);
        } catch (Throwable t) {
            Log.w(TAG, "setAppearanceLight... not supported: " + t);
        }

        // Setup inset handling.
        applyWindowInsetsPadding();

        // --- existing initialization (bottom nav and fragment handling) ---
        bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState != null) {
            currentTag = savedInstanceState.getString("currentTag", "home");
            currentMenuItemId = savedInstanceState.getInt("currentMenuItemId", R.id.navigation_home);
            if (bottomNav != null) bottomNav.setSelectedItemId(currentMenuItemId);
        } else {
            // initial fragment
            openFragment(new HomeFragment(), false, "home");
            if (bottomNav != null) bottomNav.setSelectedItemId(R.id.navigation_home);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.nav_bar_black));
        }
        WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightNavigationBars(false);

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

        // When back stack changes we should re-apply insets to the new top fragment
        getSupportFragmentManager().addOnBackStackChangedListener(this::applyInsetsToCurrentFragment);

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
     * Optional fragment interface:
     * Fragments that want to control header icon color can implement this and return whether they
     * want "light status bar icons" (true => dark icons on a light header).
     */
    public interface HeaderConfig {
        boolean useLightStatusBarIcons();
    }

    /**
     * Apply window insets to activity root and to the current fragment if necessary.
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

            try {
                Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                topInset = sys.top;
                bottomInset = sys.bottom;
            } catch (Throwable t) {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
            }

            // save to re-use when fragment changes
            lastStatusBarInset = topInset;
            lastNavBarInset = bottomInset;

            // Optional: extra spacing (8dp)
            int extra = dpToPx(8);

            // If the current fragment has R.id.orange_panel we expand & translate it, otherwise
            // push main_content down by the status bar + extra.
            boolean currentHasOrange = fragmentHasView(R.id.orange_panel);

            if (currentHasOrange) {
                mainContent.setPadding(
                        mainContent.getPaddingLeft(),
                        extra,
                        mainContent.getPaddingRight(),
                        bottomInset + extra
                );
                applyInsetsToCurrentFragment(); // adjust orange panel
            } else {
                mainContent.setPadding(
                        mainContent.getPaddingLeft(),
                        topInset + extra,
                        mainContent.getPaddingRight(),
                        bottomInset + extra
                );
                clearOrangePanelAdjustments();
            }

            // bottom nav padding & color sync
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

    /**
     * Apply the saved insets to the current fragment's orange_panel (if present).
     */
    private void applyInsetsToCurrentFragment() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (current == null) return;
        View fragView = current.getView();
        if (fragView == null) return;

        final View orange = fragView.findViewById(R.id.orange_panel);
        if (orange == null) {
            return;
        }

        // base height in DP must match what's in your fragment xml (120dp)
        final int baseOrangeDp = 120;
        final int baseOrangePx = dpToPx(baseOrangeDp);

        // set the orange_panel height = base + statusBarInset so it covers the status area
        ViewGroup.LayoutParams lp = orange.getLayoutParams();
        if (lp != null) {
            int desired = baseOrangePx + lastStatusBarInset;
            if (lp.height != desired) {
                lp.height = desired;
                orange.setLayoutParams(lp);
            }
        }

        // move it up so the top of the orange_panel sits behind the status bar
        orange.setTranslationY(-lastStatusBarInset);

        // If the fragment implements HeaderConfig, use it to set icon appearance.
        boolean requestDarkIcons = true; // default (dark icons)
        if (current instanceof HeaderConfig) {
            try {
                requestDarkIcons = ((HeaderConfig) current).useLightStatusBarIcons();
            } catch (Throwable ignored) {}
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

    private void applyBottomNavInsets(int navBarInset) {
        if (bottomNav == null) return;

        // Add bottom padding so BottomNavigationView content isn't under the system nav
        bottomNav.setPadding(
                bottomNav.getPaddingLeft(),
                bottomNav.getPaddingTop(),
                bottomNav.getPaddingRight(),
                navBarInset
        );

        // Try to set navigation bar color to match bottomNav background:
        try {
            int bgColor = Color.TRANSPARENT;

            Drawable bg = bottomNav.getBackground();
            if (bg instanceof ColorDrawable) {
                bgColor = ((ColorDrawable) bg).getColor();
            } else if (bg != null) {
                // attempt to convert other drawables to a color (best-effort).
                Bitmap bmp = Bitmap.createBitmap(
                        Math.max(1, bg.getIntrinsicWidth()),
                        Math.max(1, bg.getIntrinsicHeight()),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp);
                bg.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                bg.draw(canvas);
                bgColor = bmp.getPixel(0, 0);
            } else {
                // fallback to a resource color if you have one
                try {
                    bgColor = ContextCompat.getColor(this, R.color.nav_background);
                } catch (Throwable ignored) {}
            }

            getWindow().setNavigationBarColor(bgColor);

            // set nav icon appearance depending on whether bg is light/dark
            boolean useDarkNavIcons = isColorLight(bgColor);
            try {
                insetsController.setAppearanceLightNavigationBars(useDarkNavIcons);
            } catch (Throwable t) { /* ignore */ }
        } catch (Throwable t) {
            Log.w(TAG, "applyBottomNavInsets: failed to set nav color: " + t);
        }
    }

    /**
     * Whether the color is "light" — simple luminance threshold.
     */
    private boolean isColorLight(@ColorInt int color) {
        // convert to RGB components
        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = color & 0xff;
        // linear luminance approximation (rec. 709)
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
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
        } else {
            clearBackStack();
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
        }

        ft.commit();

        // force immediate execution so we can reliably access fragment.getView()
        try {
            getSupportFragmentManager().executePendingTransactions();
        } catch (Throwable ignored) {}

        // re-apply insets to the fragment we just created (if it has orange_panel)
        applyInsetsToCurrentFragment();

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

        // re-apply insets to the new to p fragment
        applyInsetsToCurrentFragment();
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
