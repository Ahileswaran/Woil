package com.example.woil.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NicMlKitHelper {

    private final TextRecognizer recognizer;

    public NicMlKitHelper(Context context) {
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    public Task<Text> runOcr(@NonNull Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        return recognizer.process(image);
    }

    public void close() {
        recognizer.close();
    }

    public String extractNicCandidate(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            return null;
        }

        String normalized = normalizeOcrText(ocrText);

        List<String> candidates = new ArrayList<>();

        // Split tokens first
        String[] tokens = normalized.split("\\s+");
        for (String token : tokens) {
            if (token == null) continue;
            token = token.trim();
            if (token.isEmpty()) continue;

            if (token.length() >= 9 && token.length() <= 15) {
                candidates.add(token);
            }
        }

        // Also scan compact text for embedded candidates
        String compact = normalized.replaceAll("\\s+", "");

        Matcher oldMatcher = Pattern.compile("\\d{9}[VX]").matcher(compact);
        while (oldMatcher.find()) {
            candidates.add(oldMatcher.group());
        }

        Matcher newMatcher = Pattern.compile("\\d{12}").matcher(compact);
        while (newMatcher.find()) {
            candidates.add(newMatcher.group());
        }

        // Prefer exact valid new NIC first
        for (String candidate : candidates) {
            String cleaned = cleanCandidate(candidate);
            if (cleaned.matches("^\\d{12}$")) {
                return cleaned;
            }
        }

        // Then exact valid old NIC
        for (String candidate : candidates) {
            String cleaned = cleanCandidate(candidate);
            if (cleaned.matches("^\\d{9}[VX]$")) {
                return cleaned;
            }
        }

        return null;
    }

    private String normalizeOcrText(String input) {
        String text = input.toUpperCase(Locale.US);

        // Common OCR corrections
        text = text
                .replace('O', '0')
                .replace('Q', '0')
                .replace('D', '0')
                .replace('I', '1')
                .replace('L', '1')
                .replace('|', '1')
                .replace('S', '5')
                .replace('B', '8');

        // Keep only letters, digits, and separators
        text = text.replaceAll("[^A-Z0-9\\s]", " ");

        // Collapse spaces
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    private String cleanCandidate(String token) {
        if (TextUtils.isEmpty(token)) return "";

        String cleaned = token.toUpperCase(Locale.US).trim();

        // Remove leading non-digits
        cleaned = cleaned.replaceAll("^[^0-9]+", "");

        // Allow only ending V/X for old NIC
        cleaned = cleaned.replaceAll("[^0-9VX]", "");

        // Reject if there is any letter before the last char
        if (cleaned.matches(".*[VX].*[0-9].*")) {
            return "";
        }

        if (cleaned.length() > 1) {
            String prefix = cleaned.substring(0, cleaned.length() - 1);
            String last = cleaned.substring(cleaned.length() - 1);

            if ((last.equals("V") || last.equals("X")) && prefix.matches("^\\d{9}$")) {
                return prefix + last;
            }
        }

        if (cleaned.matches("^\\d{12}$")) {
            return cleaned;
        }

        return cleaned;
    }
}