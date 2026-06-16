package com.example.woil;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.stripe.android.PaymentConfiguration;

public class WoilApp extends Application {
    // ── Stripe keys ─────────────────────────────────────────────────────────
    // Replace with your TEST publishable key from https://dashboard.stripe.com/test/apikeys
    // For production, swap to the live key and ensure STRIPE_SECRET_KEY in backend/.env is also live.
    private static final String STRIPE_PUBLISHABLE_KEY = "pk_test_51Tik1aRyZUFynxTmmv6HfQFB9HZLMV0i1b8i38eTHAg1pM5asBo0DWkP142OLTy3OqX7RD4dimb6HA5WvV4em2me00h8L2vlnc";

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);

        // Stripe initialisation — required before any PaymentSheet usage
        PaymentConfiguration.init(getApplicationContext(), STRIPE_PUBLISHABLE_KEY);
    }
}