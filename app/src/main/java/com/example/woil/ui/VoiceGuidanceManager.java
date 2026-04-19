package com.example.woil.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public class VoiceGuidanceManager {

    private static final String APP_PREFS = "app_prefs";
    private static final String KEY_VOICE = "pref_voice_guidance";

    private final Context appContext;
    private TextToSpeech textToSpeech;
    private boolean isReady = false;

    public VoiceGuidanceManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void init() {
        if (textToSpeech != null) return;

        textToSpeech = new TextToSpeech(appContext, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.getDefault());
                isReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
                textToSpeech.setSpeechRate(0.95f);
                textToSpeech.setPitch(1.0f);
            } else {
                isReady = false;
            }
        });
    }

    public boolean isVoiceGuidedEnabled() {
        SharedPreferences prefs = appContext.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_VOICE, false);
    }

    public void speak(@Nullable String text) {
        if (!isVoiceGuidedEnabled()) return;
        if (TextUtils.isEmpty(text)) return;

        if (textToSpeech == null) {
            init();
        }
        if (!isReady || textToSpeech == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "woil_voice_guidance");
        } else {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public void speakAdd(@Nullable String text) {
        if (!isVoiceGuidedEnabled()) return;
        if (TextUtils.isEmpty(text)) return;

        if (textToSpeech == null) {
            init();
        }
        if (!isReady || textToSpeech == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, "woil_voice_guidance_add");
        } else {
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null);
        }
    }

    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        isReady = false;
    }
}