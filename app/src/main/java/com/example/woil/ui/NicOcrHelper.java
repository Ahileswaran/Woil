package com.example.woil.ui;

import android.content.Context;
import android.graphics.Bitmap;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NicOcrHelper {

    private final Context context;

    public NicOcrHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public void ensureTrainedData() throws Exception {
        File tessDir = new File(context.getFilesDir(), "tesseract/tessdata");
        if (!tessDir.exists() && !tessDir.mkdirs()) {
            throw new Exception("Failed to create tessdata directory");
        }

        File trainedData = new File(tessDir, "eng.traineddata");
        if (!trainedData.exists()) {
            try (InputStream in = context.getAssets().open("tessdata/eng.traineddata");
                 FileOutputStream out = new FileOutputStream(trainedData)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
        }
    }



    public String extractNicCandidate(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            return null;
        }

        // Normalize common OCR mistakes and separators
        String normalized = ocrText.toUpperCase()
                .replace('O', '0')
                .replace('I', '1')
                .replace('L', '1')
                .replace('S', '5')
                .replace('B', '8');

        // Replace anything that is not digit or letter with spaces
        normalized = normalized.replaceAll("[^A-Z0-9]", " ");

        // Split into tokens
        String[] tokens = normalized.split("\\s+");

        String bestOld = null;
        String bestNew = null;

        for (String token : tokens) {
            if (token == null) continue;
            token = token.trim();
            if (token.isEmpty()) continue;

            // Only consider token lengths that could realistically be NIC-like
            if (token.length() < 9 || token.length() > 15) {
                continue;
            }

            // Old NIC valid pattern:
            // exactly 9 digits + 1 ending letter V or X
            if (token.matches("^\\d{9}[VX]$")) {
                bestOld = token;
                continue;
            }

            // New NIC valid pattern:
            // exactly 12 digits
            if (token.matches("^\\d{12}$")) {
                bestNew = token;
                continue;
            }

            // Try cleaning token further:
            // remove leading/trailing junk while preserving middle structure
            String cleaned = token.replaceAll("^[^0-9]+", "").replaceAll("[^0-9VX]+$", "");

            if (cleaned.matches("^\\d{9}[VX]$")) {
                bestOld = cleaned;
                continue;
            }

            if (cleaned.matches("^\\d{12}$")) {
                bestNew = cleaned;
            }
        }

        // Prefer new NIC if found, otherwise old NIC
        if (bestNew != null) return bestNew;
        if (bestOld != null) return bestOld;

        // Fallback: scan the full OCR text as one compact string
        String compact = normalized.replaceAll("\\s+", "");

        java.util.regex.Matcher oldMatcher =
                java.util.regex.Pattern.compile("\\d{9}[VX]").matcher(compact);
        if (oldMatcher.find()) {
            String found = oldMatcher.group();
            if (found.matches("^\\d{9}[VX]$")) {
                return found;
            }
        }

        java.util.regex.Matcher newMatcher =
                java.util.regex.Pattern.compile("\\d{12}").matcher(compact);
        if (newMatcher.find()) {
            String found = newMatcher.group();
            if (found.matches("^\\d{12}$")) {
                return found;
            }
        }

        return null;
    }
}