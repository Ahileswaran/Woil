package com.example.woil.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;
import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ClientActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "woil_prefs";
    private static final String KEY_ACTIVE_ROLE = "active_role";

    private CircleImageView ivProfile;
    private TextView tvUsername, tvSubtitle, tvRatingValue, tvJobsValue, tvMemberSince;
    private TextView tvFullName, tvPhone, tvLocation;
    private Button btnEditProfile;
    private ImageButton btnBack;
    private TextView tvPending;

    private EditText etName;
    private EditText etLocation;
    private Button btnChooseImage;

    private boolean isEditMode = false;
    private Uri selectedImageUri;

    private ImageView ad1Image, ad2Image;
    private TextView ad1Date, ad1Title, ad2Date, ad2Title;
    private ProgressBar wpTask1Progress, wpTask2Progress;
    private TextView wpTask1Title, wpTask1Date, wpTask2Title, wpTask2Date;

    private TextView btnClientToggle, btnWorkerToggle;
    private View togglePill;
    private String currentRole = "client";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;
    private ListenerRegistration profileListener;
    private ListenerRegistration workProgressListener;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK &&
                        result.getData() != null &&
                        result.getData().getData() != null) {

                    selectedImageUri = result.getData().getData();

                    if (ivProfile != null) {
                        ivProfile.setImageURI(selectedImageUri);
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ivProfile = findViewById(R.id.profile_image_main);
        tvUsername = findViewById(R.id.username);
        tvSubtitle = findViewById(R.id.subtitle);
        tvRatingValue = findViewById(R.id.rating_value);
        tvJobsValue = findViewById(R.id.jobs_value);
        tvMemberSince = findViewById(R.id.member_since_value);
        tvFullName = findViewById(R.id.tv_name);
        tvPhone = findViewById(R.id.tv_phone);
        tvLocation = findViewById(R.id.tv_location);

        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnBack = findViewById(R.id.btn_back);

        //tvPending = findViewById(R.id.tv_pending); // may be null in some layouts

        etName = findViewById(R.id.et_name);
        etLocation = findViewById(R.id.et_location);
        btnChooseImage = findViewById(R.id.btn_choose_image);

        ad1Image = findViewById(R.id.ad1_image);
        ad1Date = findViewById(R.id.ad1_date);
        ad1Title = findViewById(R.id.ad1_title);
        ad2Image = findViewById(R.id.ad2_image);
        ad2Date = findViewById(R.id.ad2_date);
        ad2Title = findViewById(R.id.ad2_title);

        wpTask1Progress = findViewById(R.id.wp_task1_progress);
        wpTask2Progress = findViewById(R.id.wp_task2_progress);
        wpTask1Title = findViewById(R.id.wp_task1_title);
        wpTask1Date = findViewById(R.id.wp_task1_date);
        wpTask2Title = findViewById(R.id.wp_task2_title);
        wpTask2Date = findViewById(R.id.wp_task2_date);

        btnClientToggle = findViewById(R.id.btn_client);
        btnWorkerToggle = findViewById(R.id.btn_worker);
        togglePill = findViewById(R.id.toggle_pill);

        setPlaceholders();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                if (!isEditMode) {
                    enterEditMode();
                } else {
                    saveEditableProfileFields();
                }
            });
        }

        if (btnChooseImage != null) {
            btnChooseImage.setOnClickListener(v -> openImagePicker());
        }

        MaterialButton btnAddJob = findViewById(R.id.btn_add_job_floating);
        if (btnAddJob != null) {
            btnAddJob.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, PostJobActivity.class));
                } catch (Exception e) {
                    Toast.makeText(this, "Can't open Post Job screen", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnClientToggle != null) {
            btnClientToggle.setOnClickListener(v -> applyToggleState("client", true));
        }

        if (btnWorkerToggle != null) {
            btnWorkerToggle.setOnClickListener(v -> {
                if (!"worker".equalsIgnoreCase(currentRole)) {
                    applyToggleState("worker", true);
                    btnWorkerToggle.postDelayed(this::switchRoleToWorker, 220);
                }
            });
        }

        View toggleGroup = findViewById(R.id.toggle_group);
        if (toggleGroup != null) {
            toggleGroup.post(() -> applyToggleState("client", false));
        }

        attachListeners();
        setupWorkProgress();
    }

    private void setPlaceholders() {
        if (tvUsername != null) tvUsername.setText("—");
        if (tvSubtitle != null) tvSubtitle.setText("Client · —");
        if (tvRatingValue != null) tvRatingValue.setText("★ —");
        if (tvJobsValue != null) tvJobsValue.setText("0");
        if (tvMemberSince != null) tvMemberSince.setText("—");

        if (tvFullName != null) tvFullName.setText("—");
        if (tvPhone != null) tvPhone.setText("");
        if (tvLocation != null) tvLocation.setText("—");

        if (etName != null) etName.setVisibility(View.GONE);
        if (etLocation != null) etLocation.setVisibility(View.GONE);
        if (btnChooseImage != null) btnChooseImage.setVisibility(View.GONE);

        if (ivProfile != null) ivProfile.setImageResource(R.drawable.photo_placeholder);

        if (tvPending != null) tvPending.setText("⏱ Pending");

        if (ad1Date != null) ad1Date.setText("");
        if (ad1Title != null) ad1Title.setText("");
        if (ad2Date != null) ad2Date.setText("");
        if (ad2Title != null) ad2Title.setText("");

        currentRole = "client";
    }

    private void applyToggleState(String role, boolean animate) {
        currentRole = role == null ? "client" : role.toLowerCase();

        if (togglePill == null || btnClientToggle == null || btnWorkerToggle == null) return;

        btnClientToggle.post(() -> {
            float targetX = "worker".equalsIgnoreCase(currentRole)
                    ? btnWorkerToggle.getLeft()
                    : btnClientToggle.getLeft();

            if (animate) {
                togglePill.animate()
                        .translationX(targetX)
                        .setDuration(220)
                        .start();
            } else {
                togglePill.setTranslationX(targetX);
            }

            if ("worker".equalsIgnoreCase(currentRole)) {
                btnClientToggle.setTextColor(Color.WHITE);
                btnWorkerToggle.setTextColor(Color.parseColor("#222222"));
            } else {
                btnClientToggle.setTextColor(Color.parseColor("#222222"));
                btnWorkerToggle.setTextColor(Color.WHITE);
            }
        });
    }

    private void attachListeners() {
        String uid = FirebaseDebugLogger.requireUid(this, mAuth, "client_profile_listen");
        if (uid == null) return;

        userListener = db.collection("users").document(uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { FirebaseDebugLogger.failure("client_user_listen", "users/" + uid, e); return; }
                    if (snap == null || !snap.exists()) return;
                    FirebaseDebugLogger.read("client_user_listen", "users/" + uid, 1);
                    populateFromUserSnapshot(snap);
                });

        profileListener = db.collection("profiles").document(uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { FirebaseDebugLogger.failure("client_profile_listen", "profiles/" + uid, e); return; }
                    if (snap == null || !snap.exists()) return;
                    FirebaseDebugLogger.read("client_profile_listen", "profiles/" + uid, 1);
                    populateFromProfileSnapshot(snap);
                });
    }

    private void populateFromUserSnapshot(DocumentSnapshot snap) {
        applyToggleState("client", false);
        String phoneFromDoc = snap.getString("phone");

        if (mAuth.getCurrentUser() != null) {
            String phone = mAuth.getCurrentUser().getPhoneNumber();
            if (tvPhone != null) {
                tvPhone.setText(!TextUtils.isEmpty(phone) ? phone : safe(phoneFromDoc));
            }
        } else {
            if (tvPhone != null) tvPhone.setText(safe(phoneFromDoc));
        }
    }

    private void populateFromProfileSnapshot(DocumentSnapshot snap) {
        String first = snap.getString("firstName");
        String last = snap.getString("lastName");
        String displayName = snap.getString("displayName");
        String locationText = snap.getString("locationText");
        if (TextUtils.isEmpty(locationText)) locationText = snap.getString("address");

        String fullName;

        if (!TextUtils.isEmpty(displayName)) {
            fullName = displayName.trim().replaceAll("\\s+", " ");
        } else {
            fullName = (safe(first).trim() + " " + safe(last).trim()).trim().replaceAll("\\s+", " ");
        }

        if (TextUtils.isEmpty(fullName)) fullName = "—";

        if (!isEditMode && tvFullName != null) tvFullName.setText(fullName);

        String handle = "@" + fullName.trim().replaceAll("\\s+", " ");
        if ("@".equals(handle)) {
            handle = "@" + (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "user");
        }
        if (tvUsername != null) tvUsername.setText(handle);

        String subtitle = "Client" + (!TextUtils.isEmpty(locationText) ? " · " + locationText : " · —");
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);

        if (!isEditMode && tvLocation != null) {
            tvLocation.setText(!TextUtils.isEmpty(locationText) ? locationText : "—");
        }

        Object ratingObj = snap.get("rating");
        if (ratingObj != null && tvRatingValue != null) {
            try {
                double rating = Double.parseDouble(ratingObj.toString());
                tvRatingValue.setText("★ " + new DecimalFormat("#0.0").format(rating));
            } catch (Exception ignored) {
            }
        }

        Object jobsObj = snap.get("jobsPosted");
        if (jobsObj == null) jobsObj = snap.get("jobs");
        if (jobsObj != null && tvJobsValue != null) {
            tvJobsValue.setText(String.valueOf(jobsObj));
        }

        Object memberSinceObj = snap.get("memberSince");
        if (memberSinceObj instanceof Timestamp && tvMemberSince != null) {
            Date d = ((Timestamp) memberSinceObj).toDate();
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            tvMemberSince.setText(String.valueOf(c.get(Calendar.YEAR)));
        } else {
            Object msMillis = snap.get("memberSinceMillis");
            if (msMillis instanceof Number && tvMemberSince != null) {
                long millis = ((Number) msMillis).longValue();
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(millis);
                tvMemberSince.setText(String.valueOf(c.get(Calendar.YEAR)));
            } else if (snap.getString("memberSinceYear") != null && tvMemberSince != null) {
                tvMemberSince.setText(snap.getString("memberSinceYear"));
            } else if (snap.getTimestamp("createdAt") != null && tvMemberSince != null) {
                Calendar c = Calendar.getInstance();
                c.setTime(snap.getTimestamp("createdAt").toDate());
                tvMemberSince.setText(String.valueOf(c.get(Calendar.YEAR)));
            }
        }

        String photoUrl = snap.getString("photoUrl");
        if (TextUtils.isEmpty(photoUrl)) photoUrl = snap.getString("photo");
        if (TextUtils.isEmpty(photoUrl)) photoUrl = snap.getString("avatar");
        if (TextUtils.isEmpty(photoUrl)) photoUrl = snap.getString("nicFrontUri");

        if (!isEditMode) {
            if (!TextUtils.isEmpty(photoUrl)) {
                try {
                    Glide.with(this)
                            .load(photoUrl)
                            .placeholder(R.drawable.photo_placeholder)
                            .error(R.drawable.photo_placeholder)
                            .into(ivProfile);
                } catch (Exception ex) {
                    try {
                        ivProfile.setImageURI(Uri.parse(photoUrl));
                    } catch (Exception ignored) {
                        ivProfile.setImageResource(R.drawable.photo_placeholder);
                    }
                }
            } else if (ivProfile != null) {
                ivProfile.setImageResource(R.drawable.photo_placeholder);
            }
        }
    }

    private void enterEditMode() {
        isEditMode = true;

        if (btnEditProfile != null) btnEditProfile.setText("Save");
        if (btnChooseImage != null) btnChooseImage.setVisibility(View.VISIBLE);

        if (tvFullName != null && etName != null) {
            etName.setText(tvFullName.getText().toString());
            tvFullName.setVisibility(View.GONE);
            etName.setVisibility(View.VISIBLE);
        }

        if (tvLocation != null && etLocation != null) {
            etLocation.setText(tvLocation.getText().toString());
            tvLocation.setVisibility(View.GONE);
            etLocation.setVisibility(View.VISIBLE);
        }
    }

    private void exitEditMode() {
        isEditMode = false;

        if (btnEditProfile != null) btnEditProfile.setText("Edit profile");
        if (btnChooseImage != null) btnChooseImage.setVisibility(View.GONE);

        if (tvFullName != null) tvFullName.setVisibility(View.VISIBLE);
        if (etName != null) etName.setVisibility(View.GONE);

        if (tvLocation != null) tvLocation.setVisibility(View.VISIBLE);
        if (etLocation != null) etLocation.setVisibility(View.GONE);
    }

    private void saveEditableProfileFields() {
        String uid = FirebaseDebugLogger.requireUid(this, mAuth, "client_profile_edit_update");
        if (uid == null) return;

        String updatedName = etName != null ? etName.getText().toString().trim() : "";
        String updatedLocation = etLocation != null ? etLocation.getText().toString().trim() : "";

        if (TextUtils.isEmpty(updatedName)) {
            if (etName != null) etName.setError("Enter name");
            return;
        }

        String firstName = updatedName;
        String lastName = "";

        String[] parts = updatedName.split("\\s+", 2);
        if (parts.length > 0) firstName = parts[0];
        if (parts.length > 1) lastName = parts[1];

        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", updatedName);
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("locationText", updatedLocation);
        updates.put("address", updatedLocation);
        updates.put("role", "client");
        updates.put("isWorker", false);

        if (selectedImageUri != null) {
            StorageReference storageRef = FirebaseStorage.getInstance("gs://woil-f8f1c.firebasestorage.app")
                    .getReference()
                    .child("profile_images/" + uid + ".jpg");

            storageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            updates.put("photoUrl", uri.toString());
                            saveClientProfileUpdatesToFirestore(uid, updates, updatedName, updatedLocation);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // Proceed with updating other fields even if image upload fails
                        saveClientProfileUpdatesToFirestore(uid, updates, updatedName, updatedLocation);
                    });
        } else {
            saveClientProfileUpdatesToFirestore(uid, updates, updatedName, updatedLocation);
        }
    }

    private void saveClientProfileUpdatesToFirestore(String uid, Map<String, Object> updates, String updatedName, String updatedLocation) {
        db.collection("profiles").document(uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    FirebaseDebugLogger.success("client_profile_edit_update", "profiles", uid);
                    if (tvFullName != null) tvFullName.setText(updatedName);
                    if (tvLocation != null) tvLocation.setText(updatedLocation);

                    exitEditMode();
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    FirebaseDebugLogger.failure("client_profile_edit_update", "profiles/" + uid, e);
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void openImagePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(Intent.createChooser(intent, "Select profile image"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No image picker found", Toast.LENGTH_SHORT).show();
        }
    }

    private void switchRoleToWorker() {
        String uid = FirebaseDebugLogger.requireUid(this, mAuth, "client_switch_worker_update");
        if (uid == null) return;

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("role", "worker");

        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("role", "worker");
        profileUpdates.put("isWorker", true);

        db.collection("users").document(uid)
                .set(userUpdates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    FirebaseDebugLogger.success("client_switch_worker_user_update", "users", uid);
                    db.collection("profiles").document(uid)
                                .set(profileUpdates, SetOptions.merge())
                                .addOnSuccessListener(unused2 -> {
                                    saveActiveRole("worker");
                                    Toast.makeText(this, "Switched to Worker", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    FirebaseDebugLogger.failure("client_switch_worker_profile_update", "profiles/" + uid, e);
                                    Toast.makeText(this,
                                            "Role switch failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                })
                .addOnFailureListener(e -> {
                    FirebaseDebugLogger.failure("client_switch_worker_user_update", "users/" + uid, e);
                    Toast.makeText(this,
                            "Role switch failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveActiveRole(String role) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_ACTIVE_ROLE, role).apply();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.applyLocale(newBase));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
        if (workProgressListener != null) {
            workProgressListener.remove();
            workProgressListener = null;
        }
    }

    private void setupWorkProgress() {
        String uid = FirebaseDebugLogger.requireUid(this, mAuth, "client_work_progress_listen");
        if (uid == null) return;

        workProgressListener = db.collection("matches")
                .whereEqualTo("clientUid", uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) {
                        return;
                    }

                    List<DocumentSnapshot> activeOrCompleted = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String s = doc.getString("status");
                        if ("COMPLETED".equalsIgnoreCase(s) || "IN_PROGRESS".equalsIgnoreCase(s) || "STARTED".equalsIgnoreCase(s) || "ACCEPTED".equalsIgnoreCase(s) || "TRAVELING".equalsIgnoreCase(s) || "ARRIVED".equalsIgnoreCase(s)) {
                            activeOrCompleted.add(doc);
                        }
                    }

                    Collections.sort(activeOrCompleted, (a, b) -> {
                        Date da = a.getDate("updatedAt") != null ? a.getDate("updatedAt") : new Date(0);
                        Date dbVal = b.getDate("updatedAt") != null ? b.getDate("updatedAt") : new Date(0);
                        return dbVal.compareTo(da); // descending
                    });

                    View cardWorkProgress = findViewById(R.id.card_work_progress);
                    if (cardWorkProgress == null) return;

                    if (activeOrCompleted.isEmpty()) {
                        cardWorkProgress.setVisibility(View.GONE);
                        return;
                    }

                    cardWorkProgress.setVisibility(View.VISIBLE);

                    if (activeOrCompleted.size() > 0) {
                        DocumentSnapshot doc = activeOrCompleted.get(0);
                        wpTask1Title.setVisibility(View.VISIBLE);
                        wpTask1Date.setVisibility(View.VISIBLE);
                        wpTask1Progress.setVisibility(View.VISIBLE);

                        String category = doc.getString("category");
                        wpTask1Title.setText(TextUtils.isEmpty(category) ? "General Job" : category);

                        Date dateVal = doc.getDate("createdAt");
                        if (dateVal != null) {
                            wpTask1Date.setText(new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(dateVal));
                        } else {
                            wpTask1Date.setText("Just now");
                        }

                        String status = doc.getString("status");
                        int progress = 0;
                        if ("COMPLETED".equalsIgnoreCase(status)) {
                            progress = 100;
                        } else if ("IN_PROGRESS".equalsIgnoreCase(status) || "STARTED".equalsIgnoreCase(status)) {
                            progress = 60;
                        } else {
                            progress = 20;
                        }
                        wpTask1Progress.setProgress(progress);
                    } else {
                        wpTask1Title.setVisibility(View.GONE);
                        wpTask1Date.setVisibility(View.GONE);
                        wpTask1Progress.setVisibility(View.GONE);
                    }

                    if (activeOrCompleted.size() > 1) {
                        DocumentSnapshot doc = activeOrCompleted.get(1);
                        wpTask2Title.setVisibility(View.VISIBLE);
                        wpTask2Date.setVisibility(View.VISIBLE);
                        wpTask2Progress.setVisibility(View.VISIBLE);

                        String category = doc.getString("category");
                        wpTask2Title.setText(TextUtils.isEmpty(category) ? "General Job" : category);

                        Date dateVal = doc.getDate("createdAt");
                        if (dateVal != null) {
                            wpTask2Date.setText(new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(dateVal));
                        } else {
                            wpTask2Date.setText("Just now");
                        }

                        String status = doc.getString("status");
                        int progress = 0;
                        if ("COMPLETED".equalsIgnoreCase(status)) {
                            progress = 100;
                        } else if ("IN_PROGRESS".equalsIgnoreCase(status) || "STARTED".equalsIgnoreCase(status)) {
                            progress = 60;
                        } else {
                            progress = 20;
                        }
                        wpTask2Progress.setProgress(progress);
                    } else {
                        wpTask2Title.setVisibility(View.GONE);
                        wpTask2Date.setVisibility(View.GONE);
                        wpTask2Progress.setVisibility(View.GONE);
                    }
                });
    }
}