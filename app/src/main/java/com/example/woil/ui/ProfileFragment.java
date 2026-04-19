package com.example.woil.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import android.animation.ValueAnimator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.woil.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "woil_prefs";
    private static final String KEY_ACTIVE_ROLE = "active_role";

    private CircleImageView ivProfile;
    private TextView tvUsername, tvSubtitle, tvRatingValue, tvJobsValue, tvMemberSince;
    private TextView tvFullName, tvPhone, tvLocation;
    private Button btnEditProfile;
    private ImageButton btnBack;
    private TextView tvPending;
    private View togglePill;
    private TextView btnClient;
    private TextView btnWorker;
    private String currentRole = "worker";

    private TextView tvVerificationSubtitle;
    private TextView tvIdParsedDob;
    private TextView tvIdParsedGender;
    private TextView tvIdMatch;
    private TextView tvIdStatus;

    private EditText etName;
    private EditText etLocation;
    private Button btnChooseImage;

    private boolean isEditMode = false;
    private Uri selectedImageUri;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ListenerRegistration userListener;
    private ListenerRegistration profileListener;

    private VoiceGuidanceManager voiceGuidanceManager;

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

    public ProfileFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ivProfile = view.findViewById(R.id.profile_image_main);
        tvUsername = view.findViewById(R.id.username);
        tvSubtitle = view.findViewById(R.id.subtitle);
        tvRatingValue = view.findViewById(R.id.rating_value);
        tvJobsValue = view.findViewById(R.id.jobs_value);
        tvMemberSince = view.findViewById(R.id.member_since_value);
        tvFullName = view.findViewById(R.id.tv_name);
        tvPhone = view.findViewById(R.id.tv_phone);
        tvLocation = view.findViewById(R.id.tv_location);

        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnBack = view.findViewById(R.id.btn_back);
        tvPending = view.findViewById(R.id.tv_pending);
        togglePill = view.findViewById(R.id.toggle_pill);
        btnClient = view.findViewById(R.id.btn_client);
        btnWorker = view.findViewById(R.id.btn_worker);

        tvVerificationSubtitle = view.findViewById(R.id.tv_verification_subtitle);
        tvIdParsedDob = view.findViewById(R.id.tv_id_parsed_dob);
        tvIdParsedGender = view.findViewById(R.id.tv_id_parsed_gender);
        tvIdMatch = view.findViewById(R.id.tv_id_match);
        tvIdStatus = view.findViewById(R.id.tv_id_status);

        etName = view.findViewById(R.id.et_name);
        etLocation = view.findViewById(R.id.et_location);
        btnChooseImage = view.findViewById(R.id.btn_choose_image);

        voiceGuidanceManager = new VoiceGuidanceManager(requireContext());
        voiceGuidanceManager.init();

        setPlaceholders();

        TextView txtViewSkillDetails = view.findViewById(R.id.txt_view_skill_details);
        if (txtViewSkillDetails != null) {
            txtViewSkillDetails.setOnClickListener(v -> {
                if (voiceGuidanceManager != null) {
                    voiceGuidanceManager.speak("Opening skill showcase");
                }
                Intent intent = new Intent(requireContext(), SkillShowcaseActivity.class);
                startActivity(intent);
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (voiceGuidanceManager != null) {
                    voiceGuidanceManager.stop();
                }

                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).onFragmentArrowBackToHome();
                } else {
                    requireActivity().onBackPressed();
                }
            });
        }
        if (btnClient != null) {
            btnClient.setOnClickListener(v -> {
                if (!"client".equalsIgnoreCase(currentRole)) {
                    if (voiceGuidanceManager != null) {
                        voiceGuidanceManager.speak("Switching to client profile");
                    }
                    applyToggleState("client", true);
                    btnClient.postDelayed(() -> switchRole("client"), 220);
                }
            });
        }

        if (btnWorker != null) {
            btnWorker.setOnClickListener(v -> {
                if (!"worker".equalsIgnoreCase(currentRole)) {
                    if (voiceGuidanceManager != null) {
                        voiceGuidanceManager.speak("Switching to worker profile");
                    }
                    applyToggleState("worker", true);
                    btnWorker.postDelayed(() -> switchRole("worker"), 220);
                }
            });
        }

        View toggleGroup = view.findViewById(R.id.toggle_group);
        if (toggleGroup != null) {
            toggleGroup.post(() -> applyToggleState(currentRole, false));
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                if (voiceGuidanceManager != null) {
                    voiceGuidanceManager.speak(isEditMode ? "Saving profile" : "Edit profile");
                }

                if (!isEditMode) {
                    enterEditMode();
                } else {
                    saveEditableProfileFields();
                }
            });
        }

        if (btnChooseImage != null) {
            btnChooseImage.setOnClickListener(v -> {
                if (voiceGuidanceManager != null) {
                    voiceGuidanceManager.speak("Choose profile image");
                }
                openImagePicker();
            });
        }

        setupVoiceHints();
        announceProfileScreen();
        attachListeners();
    }

    private void announceProfileScreen() {
        View root = getView();
        if (root == null) return;

        root.postDelayed(() -> {
            if (isAdded() && voiceGuidanceManager != null) {
                voiceGuidanceManager.speak("Profile screen opened");
            }
        }, 500);
    }

    private void setupVoiceHints() {
        if (etName != null) {
            etName.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && voiceGuidanceManager != null) {
                    voiceGuidanceManager.speak("Name field");
                }
            });
        }

        if (etLocation != null) {
            etLocation.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && voiceGuidanceManager != null) {
                    voiceGuidanceManager.speak("Address field");
                }
            });
        }
    }

    private void applyToggleState(String role, boolean animate) {
        currentRole = role == null ? "worker" : role.toLowerCase();

        if (togglePill == null || btnClient == null || btnWorker == null) return;

        btnClient.post(() -> {
            float targetX = "worker".equalsIgnoreCase(currentRole)
                    ? btnWorker.getLeft()
                    : btnClient.getLeft();

            if (animate) {
                togglePill.animate()
                        .translationX(targetX)
                        .setDuration(220)
                        .start();
            } else {
                togglePill.setTranslationX(targetX);
            }

            if ("worker".equalsIgnoreCase(currentRole)) {
                btnClient.setTextColor(Color.WHITE);
                btnWorker.setTextColor(Color.parseColor("#222222"));
            } else {
                btnClient.setTextColor(Color.parseColor("#222222"));
                btnWorker.setTextColor(Color.WHITE);
            }
        });
    }

    private void switchRole(String role) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("role", role);

        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("role", role);
        profileUpdates.put("isWorker", "worker".equalsIgnoreCase(role));

        db.collection("users").document(uid)
                .set(userUpdates, SetOptions.merge())
                .addOnSuccessListener(unused ->
                        db.collection("profiles").document(uid)
                                .set(profileUpdates, SetOptions.merge())
                                .addOnSuccessListener(unused2 -> {
                                    saveActiveRole(role);
                                    Toast.makeText(requireContext(),
                                            "Switched to " + capitalize(role),
                                            Toast.LENGTH_SHORT).show();

                                    if ("client".equalsIgnoreCase(role)) {
                                        try {
                                            startActivity(new Intent(requireContext(), ClientActivity.class));
                                        } catch (Exception e) {
                                            Toast.makeText(requireContext(),
                                                    "Client profile not available",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        if (requireActivity() instanceof MainActivity) {
                                            ((MainActivity) requireActivity()).reloadHomeForRole("worker");
                                        } else {
                                            requireActivity().onBackPressed();
                                        }
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(requireContext(),
                                                "Role switch failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show()))
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Role switch failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void saveActiveRole(String role) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        prefs.edit().putString(KEY_ACTIVE_ROLE, role).apply();
    }

    private void setPlaceholders() {
        if (tvUsername != null) tvUsername.setText("—");
        if (tvSubtitle != null) tvSubtitle.setText("Worker · —");
        if (tvRatingValue != null) tvRatingValue.setText("★ —");
        if (tvJobsValue != null) tvJobsValue.setText("0");
        if (tvMemberSince != null) tvMemberSince.setText("—");

        if (tvFullName != null) tvFullName.setText("—");
        if (tvPhone != null) tvPhone.setText("");
        if (tvLocation != null) tvLocation.setText("—");

        if (etName != null) etName.setVisibility(View.GONE);
        if (etLocation != null) etLocation.setVisibility(View.GONE);
        if (btnChooseImage != null) btnChooseImage.setVisibility(View.GONE);

        if (tvVerificationSubtitle != null) tvVerificationSubtitle.setText("Awaiting admin review");
        if (tvIdParsedDob != null) tvIdParsedDob.setText("ID Parsed DOB: -");
        if (tvIdParsedGender != null) tvIdParsedGender.setText("Gender: -");
        if (tvIdMatch != null) tvIdMatch.setText("ID data match: -");
        if (tvIdStatus != null) tvIdStatus.setText("Status: -");

        if (ivProfile != null) ivProfile.setImageResource(R.drawable.photo_placeholder);

        if (tvPending != null) {
            tvPending.setText("⏱ Pending");
            tvPending.setTextColor(Color.parseColor("#6B4B00"));
        }

        currentRole = "worker";
    }

    private void attachListeners() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        userListener = db.collection("users").document(uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists()) return;
                    populateFromUserSnapshot(snap);
                });

        profileListener = db.collection("profiles").document(uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists()) return;
                    populateFromProfileSnapshot(snap);
                });
    }

    private void populateFromUserSnapshot(DocumentSnapshot snap) {
        String role = snap.getString("role");
        if (!TextUtils.isEmpty(role)) {
            applyToggleState(role, false);
        }
        String phoneFromDoc = snap.getString("phone");
        Boolean userNicVerified = snap.getBoolean("nicVerified");

        if (mAuth.getCurrentUser() != null) {
            String phone = mAuth.getCurrentUser().getPhoneNumber();
            if (tvPhone != null) {
                tvPhone.setText(!TextUtils.isEmpty(phone) ? phone : safe(phoneFromDoc));
            }
        } else {
            if (tvPhone != null) tvPhone.setText(safe(phoneFromDoc));
        }

        if (Boolean.TRUE.equals(userNicVerified) && tvPending != null) {
            tvPending.setText("✔ Verified");
            tvPending.setTextColor(Color.parseColor("#2E7D32"));
        }

        if (tvSubtitle != null && !TextUtils.isEmpty(role) && tvSubtitle.getText() != null) {
            String current = tvSubtitle.getText().toString();
            if (current.contains(" · ")) {
                String suffix = current.substring(current.indexOf(" · "));
                tvSubtitle.setText(capitalize(role) + suffix);
            } else {
                tvSubtitle.setText(capitalize(role));
            }
        }
    }

    private void populateFromProfileSnapshot(DocumentSnapshot snap) {
        String first = snap.getString("firstName");
        String last = snap.getString("lastName");
        String displayName = snap.getString("displayName");
        String locationText = snap.getString("locationText");
        if (TextUtils.isEmpty(locationText)) locationText = snap.getString("address");

        String fullName = !TextUtils.isEmpty(displayName)
                ? displayName
                : ((safe(first) + " " + safe(last)).trim());

        if (TextUtils.isEmpty(fullName)) fullName = "—";

        if (!isEditMode && tvFullName != null) tvFullName.setText(fullName);

        String handle = "@" + fullName.replaceAll("\\s+", "");
        if ("@".equals(handle)) {
            handle = "@" + (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "user");
        }
        if (tvUsername != null) tvUsername.setText(handle);

        String role = snap.getString("role");
        String subtitle = (TextUtils.isEmpty(role) ? "Worker" : capitalize(role))
                + (!TextUtils.isEmpty(locationText) ? " · " + locationText : " · —");
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

        Object jobsObj;
        if ("client".equalsIgnoreCase(role)) {
            jobsObj = snap.get("jobsPosted");
            if (jobsObj == null) jobsObj = snap.get("jobs");
        } else {
            jobsObj = snap.get("completedJobs");
            if (jobsObj == null) jobsObj = snap.get("jobsCompleted");
            if (jobsObj == null) jobsObj = snap.get("jobs");
        }
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

        String nicParsedDob = snap.getString("nicParsedDob");
        String nicParsedGender = snap.getString("nicParsedGender");
        Boolean nicMatch = snap.getBoolean("nicMatch");
        String nicVerificationStatus = snap.getString("nicVerificationStatus");
        Boolean nicVerified = snap.getBoolean("nicVerified");

        if (tvIdParsedDob != null) {
            tvIdParsedDob.setText("ID Parsed DOB: " + (!TextUtils.isEmpty(nicParsedDob) ? nicParsedDob : "-"));
        }

        if (tvIdParsedGender != null) {
            String genderLabel;
            if ("M".equalsIgnoreCase(nicParsedGender)) genderLabel = "Male";
            else if ("F".equalsIgnoreCase(nicParsedGender)) genderLabel = "Female";
            else genderLabel = "-";
            tvIdParsedGender.setText("Gender: " + genderLabel);
        }

        if (tvIdMatch != null) {
            tvIdMatch.setText("ID data match: " +
                    (nicMatch != null ? (nicMatch ? "Yes" : "No") : "-"));
        }

        if (tvIdStatus != null) {
            tvIdStatus.setText("Status: " +
                    (!TextUtils.isEmpty(nicVerificationStatus) ? nicVerificationStatus : "-"));
        }

        if (Boolean.TRUE.equals(nicVerified)) {
            if (tvPending != null) {
                tvPending.setText("✔ Verified");
                tvPending.setTextColor(Color.parseColor("#2E7D32"));
            }
            if (tvVerificationSubtitle != null) {
                tvVerificationSubtitle.setText("NIC manually approved");
            }
        } else if ("AUTO_MATCHED_PENDING_ADMIN".equals(nicVerificationStatus)) {
            if (tvPending != null) {
                tvPending.setText("⏱ Pending");
                tvPending.setTextColor(Color.parseColor("#6B4B00"));
            }
            if (tvVerificationSubtitle != null) {
                tvVerificationSubtitle.setText("Auto-matched. Awaiting admin review");
            }
        } else if ("MISMATCH".equals(nicVerificationStatus)) {
            if (tvPending != null) {
                tvPending.setText("⚠ Mismatch");
                tvPending.setTextColor(Color.parseColor("#C62828"));
            }
            if (tvVerificationSubtitle != null) {
                tvVerificationSubtitle.setText("Entered details do not match NIC data");
            }
        } else if ("NOT_PROVIDED".equals(nicVerificationStatus) || TextUtils.isEmpty(nicVerificationStatus)) {
            if (tvPending != null) {
                tvPending.setText("— Not provided");
                tvPending.setTextColor(Color.parseColor("#666666"));
            }
            if (tvVerificationSubtitle != null) {
                tvVerificationSubtitle.setText("NIC not submitted");
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
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

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

        if (selectedImageUri != null) {
            updates.put("photoUrl", selectedImageUri.toString());
        }

        db.collection("profiles").document(uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (tvFullName != null) tvFullName.setText(updatedName);
                    if (tvLocation != null) tvLocation.setText(updatedLocation);

                    exitEditMode();
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }



    private void openImagePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(Intent.createChooser(intent, "Select profile image"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "No image picker found", Toast.LENGTH_SHORT).show();
        }
    }

    private String capitalize(String s) {
        if (TextUtils.isEmpty(s)) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }

        if (voiceGuidanceManager != null) {
            voiceGuidanceManager.shutdown();
            voiceGuidanceManager = null;
        }
    }
}