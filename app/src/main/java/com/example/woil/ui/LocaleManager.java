package com.example.woil.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.Locale;

public class LocaleManager {

    private static final String APP_PREFS = "app_prefs";
    private static final String KEY_LANGUAGE = "pref_app_language";

    public static final String LANG_EN = "en";
    public static final String LANG_TA = "ta";
    public static final String LANG_SI = "si";

    private LocaleManager() { }

    @NonNull
    public static String getSavedLanguage(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANG_EN);
    }

    public static void saveLanguage(@NonNull Context context, @NonNull String language) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    @NonNull
    public static Context applyLocale(@NonNull Context context) {
        String language = getSavedLanguage(context);
        return updateResources(context, language);
    }

    @NonNull
    public static Context updateResources(@NonNull Context context, @NonNull String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createConfigurationContext(config);
        } else {
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            return context;
        }
    }
}