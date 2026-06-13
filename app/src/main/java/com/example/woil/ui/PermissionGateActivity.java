package com.example.woil.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.woil.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class PermissionGateActivity extends AppCompatActivity {

    private static final int RUNTIME_PERMISSIONS_REQ_CODE = 3001;
    private static final int OVERLAY_PERMISSION_REQ_CODE = 3002;

    private TextView statusCameraMic;
    private TextView statusStorage;
    private TextView statusNotifications;
    private TextView statusOverlay;

    private AppCompatButton btnGrantStandard;
    private AppCompatButton btnGrantOverlay;
    private AppCompatButton btnProceed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If all permissions are already granted, bypass the gate and go directly next
        if (areAllPermissionsGranted()) {
            goNext();
            return;
        }

        setContentView(R.layout.activity_permission_gate);

        bindViews();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusUi();
    }

    private void bindViews() {
        statusCameraMic = findViewById(R.id.status_camera_mic);
        statusStorage = findViewById(R.id.status_storage);
        statusNotifications = findViewById(R.id.status_notifications);
        statusOverlay = findViewById(R.id.status_overlay);

        btnGrantStandard = findViewById(R.id.btn_grant_standard);
        btnGrantOverlay = findViewById(R.id.btn_grant_overlay);
        btnProceed = findViewById(R.id.btn_proceed);
    }

    private void setupClickListeners() {
        btnGrantStandard.setOnClickListener(v -> requestRuntimePermissions());
        btnGrantOverlay.setOnClickListener(v -> requestOverlayPermission());
        btnProceed.setOnClickListener(v -> {
            if (areAllPermissionsGranted()) {
                goNext();
            } else {
                Toast.makeText(this, "Please grant all permissions to proceed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean areAllPermissionsGranted() {
        return isCameraMicGranted() && isStorageGranted() && isNotificationsGranted() && isOverlayGranted();
    }

    private boolean isCameraMicGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isStorageGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean isNotificationsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Auto-granted on older versions
    }

    private boolean isOverlayGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true; // Auto-granted on older versions
    }

    private void updateStatusUi() {
        boolean camMic = isCameraMicGranted();
        boolean storage = isStorageGranted();
        boolean notif = isNotificationsGranted();
        boolean overlay = isOverlayGranted();

        setStatusText(statusCameraMic, camMic);
        setStatusText(statusStorage, storage);
        setStatusText(statusNotifications, notif);
        setStatusText(statusOverlay, overlay);

        if (camMic && storage && notif) {
            btnGrantStandard.setEnabled(false);
            btnGrantStandard.setAlpha(0.5f);
        } else {
            btnGrantStandard.setEnabled(true);
            btnGrantStandard.setAlpha(1.0f);
        }

        if (overlay) {
            btnGrantOverlay.setEnabled(false);
            btnGrantOverlay.setAlpha(0.5f);
        } else {
            btnGrantOverlay.setEnabled(true);
            btnGrantOverlay.setAlpha(1.0f);
        }

        if (camMic && storage && notif && overlay) {
            btnProceed.setEnabled(true);
            btnProceed.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            btnProceed.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            // Auto-proceed once all are set
            goNext();
        } else {
            btnProceed.setEnabled(false);
            btnProceed.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_gray));
            btnProceed.setTextColor(ContextCompat.getColor(this, R.color.gray));
        }
    }

    private void setStatusText(TextView tv, boolean granted) {
        if (granted) {
            tv.setText("Granted");
            tv.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
        } else {
            tv.setText("Required");
            tv.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
        }
    }

    private void requestRuntimePermissions() {
        List<String> listPermissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            listPermissions.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            listPermissions.add(Manifest.permission.RECORD_AUDIO);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                listPermissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                listPermissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                listPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                listPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!listPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissions.toArray(new String[0]),
                    RUNTIME_PERMISSIONS_REQ_CODE);
        }
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
        } else {
            Toast.makeText(this, "Overlay permission not required on this device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RUNTIME_PERMISSIONS_REQ_CODE) {
            updateStatusUi();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            updateStatusUi();
        }
    }

    private void goNext() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, SignUpActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }
}
