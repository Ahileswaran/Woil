package com.example.woil.ui;

import android.content.Context;
import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

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

    public String runOcr(Bitmap bitmap) throws Exception {
        ensureTrainedData();

        TessBaseAPI api = new TessBaseAPI();
        api.init(new File(context.getFilesDir(), "tesseract").getAbsolutePath(), "eng");
        api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
        api.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789VvXx");
        api.setImage(bitmap);

        String text = api.getUTF8Text();
        api.end();
        return text == null ? "" : text;
    }

    public String extractNicCandidate(String ocrText) {
        if (ocrText == null) return null;

        String compact = ocrText.replaceAll("\\s+", "");

        Pattern oldNic = Pattern.compile("\\b\\d{9}[VvXx]\\b");
        Pattern newNic = Pattern.compile("\\b\\d{12}\\b");

        Matcher mOld = oldNic.matcher(compact);
        if (mOld.find()) return mOld.group();

        Matcher mNew = newNic.matcher(compact);
        if (mNew.find()) return mNew.group();

        return null;
    }
}