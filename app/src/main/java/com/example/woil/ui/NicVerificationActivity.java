package com.example.woil.ui;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.text.Text;

import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class NicVerificationActivity extends AppCompatActivity {

    public static final String EXTRA_ENTERED_DOB = "entered_dob";
    public static final String EXTRA_ENTERED_GENDER = "entered_gender";

    private static final String TAG = "NicVerification";

    private PreviewView previewView;
    private ImageView ivFront, ivBack;
    private TextView tvStep, tvParsedDob, tvParsedGender, tvOcrStatus;
    private TextInputEditText etDetectedNic;
    private MaterialButton btnCapture, btnConfirm;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    private Uri frontUri, backUri;
    private boolean capturingFront = true;

    private String detectedNic;
    private String parsedDob;
    private String parsedGender;

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nic_verification);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);


        previewView = findViewById(R.id.previewView);
        ivFront = findViewById(R.id.ivFront);
        ivBack = findViewById(R.id.ivBack);
        tvStep = findViewById(R.id.tvStep);
        tvParsedDob = findViewById(R.id.tvParsedDob);
        tvParsedGender = findViewById(R.id.tvParsedGender);
        tvOcrStatus = findViewById(R.id.tvOcrStatus);
        etDetectedNic = findViewById(R.id.etDetectedNic);
        btnCapture = findViewById(R.id.btnCapture);
        btnConfirm = findViewById(R.id.btnConfirm);

        cameraExecutor = Executors.newSingleThreadExecutor();

        btnCapture.setOnClickListener(v -> captureSide());
        btnConfirm.setOnClickListener(v -> returnResult());
        btnConfirm.setEnabled(false);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, selector, preview, imageCapture);

            } catch (Exception e) {
                Toast.makeText(this, "Camera start failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureSide() {
        if (imageCapture == null) return;

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,
                capturingFront
                        ? "nic_front_" + System.currentTimeMillis()
                        : "nic_back_" + System.currentTimeMillis());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                ).build();

        imageCapture.takePicture(
                options,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = outputFileResults.getSavedUri();
                        if (savedUri == null) {
                            Toast.makeText(NicVerificationActivity.this, "Image URI is null", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (capturingFront) {
                            frontUri = savedUri;
                            ivFront.setImageURI(frontUri);
                            capturingFront = false;
                            tvStep.setText("Step 2 of 2 - Capture NIC back side");
                            Toast.makeText(NicVerificationActivity.this, "Front side saved", Toast.LENGTH_SHORT).show();
                        } else {
                            backUri = savedUri;
                            ivBack.setImageURI(backUri);
                            tvStep.setText("Processing OCR...");
                            Toast.makeText(NicVerificationActivity.this, "Back side saved", Toast.LENGTH_SHORT).show();
                            runOcrOnCapturedImages();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(
                                NicVerificationActivity.this,
                                "Capture failed: " + exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void runOcrOnCapturedImages() {
        tvOcrStatus.setText("OCR status: processing...");
        btnConfirm.setEnabled(false);

        NicMlKitHelper helper = new NicMlKitHelper(this);

        try {
            Bitmap frontBitmap = frontUri != null ? uriToBitmap(frontUri) : null;
            Bitmap backBitmap = backUri != null ? uriToBitmap(backUri) : null;

            final StringBuilder merged = new StringBuilder();

            if (frontBitmap != null) {
                helper.runOcr(frontBitmap)
                        .addOnSuccessListener(frontText -> {
                            if (frontText != null && frontText.getText() != null) {
                                merged.append(frontText.getText()).append("\n");
                            }

                            if (backBitmap != null) {
                                helper.runOcr(backBitmap)
                                        .addOnSuccessListener(backText -> {
                                            if (backText != null && backText.getText() != null) {
                                                merged.append(backText.getText()).append("\n");
                                            }
                                            handleMlKitOcrResult(helper, merged.toString());
                                        })
                                        .addOnFailureListener(e -> {
                                            helper.close();
                                            tvOcrStatus.setText("OCR status: failed");
                                            Toast.makeText(
                                                    NicVerificationActivity.this,
                                                    "Back OCR failed: " + e.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        });
                            } else {
                                handleMlKitOcrResult(helper, merged.toString());
                            }
                        })
                        .addOnFailureListener(e -> {
                            helper.close();
                            tvOcrStatus.setText("OCR status: failed");
                            Toast.makeText(
                                    NicVerificationActivity.this,
                                    "Front OCR failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        });
            } else if (backBitmap != null) {
                helper.runOcr(backBitmap)
                        .addOnSuccessListener(backText -> {
                            if (backText != null && backText.getText() != null) {
                                merged.append(backText.getText()).append("\n");
                            }
                            handleMlKitOcrResult(helper, merged.toString());
                        })
                        .addOnFailureListener(e -> {
                            helper.close();
                            tvOcrStatus.setText("OCR status: failed");
                            Toast.makeText(
                                    NicVerificationActivity.this,
                                    "Back OCR failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        });
            } else {
                helper.close();
                tvOcrStatus.setText("OCR status: failed - no image");
            }
        } catch (Exception e) {
            helper.close();
            tvOcrStatus.setText("OCR status: failed");
            Toast.makeText(
                    NicVerificationActivity.this,
                    "OCR error: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void handleMlKitOcrResult(NicMlKitHelper helper, String rawOcr) {
        try {
            Log.d(TAG, "OCR raw text: " + rawOcr);

            if (rawOcr == null || rawOcr.trim().isEmpty()) {
                helper.close();
                etDetectedNic.setText("");
                tvParsedDob.setText("DOB: -");
                tvParsedGender.setText("Gender: -");
                tvOcrStatus.setText("OCR status: failed - no text detected");
                Toast.makeText(
                        this,
                        "No readable text found. Retake clearer NIC photos.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            String nicCandidate = helper.extractNicCandidate(rawOcr);
            Log.d(TAG, "OCR NIC candidate: " + nicCandidate);

            if (nicCandidate == null) {
                helper.close();
                etDetectedNic.setText("");
                tvParsedDob.setText("DOB: -");
                tvParsedGender.setText("Gender: -");
                tvOcrStatus.setText("OCR status: failed - NIC not found");
                Toast.makeText(
                        this,
                        "NIC number not detected. Retake clearer photos.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            NicParser.NicParseResult parseResult = NicParser.parse(nicCandidate);

            if (parseResult.valid) {
                detectedNic = parseResult.normalizedNic;
                parsedDob = parseResult.dobIso;
                parsedGender = parseResult.gender;

                etDetectedNic.setText(detectedNic);
                tvParsedDob.setText("DOB: " + parsedDob);
                tvParsedGender.setText("Gender: " + ("M".equals(parsedGender) ? "Male" : "Female"));
                tvOcrStatus.setText("OCR status: success");
                btnConfirm.setEnabled(true);
            } else {
                etDetectedNic.setText(nicCandidate);
                tvParsedDob.setText("DOB: -");
                tvParsedGender.setText("Gender: -");
                tvOcrStatus.setText("OCR status: failed - " + parseResult.error);
                Toast.makeText(
                        this,
                        "Detected text is not a valid NIC. Retake clearer photos.",
                        Toast.LENGTH_LONG
                ).show();
            }
        } finally {
            helper.close();
        }
    }

    private Bitmap uriToBitmap(Uri uri) throws Exception {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap == null) {
                throw new Exception("Failed to decode bitmap from URI");
            }
            return bitmap;
        }
    }

    private void returnResult() {
        String enteredDob = getIntent().getStringExtra(EXTRA_ENTERED_DOB);
        String enteredGender = getIntent().getStringExtra(EXTRA_ENTERED_GENDER);

        boolean dobMatch = parsedDob != null && parsedDob.equals(enteredDob);
        boolean genderMatch = parsedGender != null && parsedGender.equalsIgnoreCase(enteredGender);

        String status;
        if (dobMatch && genderMatch) {
            status = "AUTO_MATCHED_PENDING_ADMIN";
        } else {
            status = "MISMATCH";
        }

        Intent data = new Intent();
        data.putExtra("nicFrontUri", frontUri != null ? frontUri.toString() : null);
        data.putExtra("nicBackUri", backUri != null ? backUri.toString() : null);
        data.putExtra("nicNumber", detectedNic);
        data.putExtra("nicParsedDob", parsedDob);
        data.putExtra("nicParsedGender", parsedGender);
        data.putExtra("nicMatch", dobMatch && genderMatch);
        data.putExtra("nicDobMatch", dobMatch);
        data.putExtra("nicGenderMatch", genderMatch);
        data.putExtra("nicVerificationStatus", status);

        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}