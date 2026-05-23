package com.example.woil.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etAddress, etNic, etDob;
    private TextView tvSelectLocation, tvSkillsLabel, tvLocationWarning;
    private ImageButton btnUploadNic;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale;
    private Spinner spinnerSkills;
    private MaterialButton btnSubmit;

    private String role = "worker";

    private Double selectedLatitude = null;
    private Double selectedLongitude = null;
    private String selectedMapAddress = "";
    private String selectedArea = "";
    private String selectedProvince = "";
    private String selectedMapSnapshotPath = "";

    private String nicFrontUriString = null;
    private String nicBackUriString = null;
    private String nicParsedDob = null;
    private String nicParsedGender = null;
    private boolean nicMatch = false;
    private boolean nicDobMatch = false;
    private boolean nicGenderMatch = false;
    private String nicVerificationStatus = "NOT_PROVIDED";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<Intent> nicVerificationLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                Intent data = result.getData();

                                nicFrontUriString = data.getStringExtra("nicFrontUri");
                                nicBackUriString = data.getStringExtra("nicBackUri");

                                String detectedNic = data.getStringExtra("nicNumber");
                                nicParsedDob = data.getStringExtra("nicParsedDob");
                                nicParsedGender = data.getStringExtra("nicParsedGender");
                                nicMatch = data.getBooleanExtra("nicMatch", false);
                                nicDobMatch = data.getBooleanExtra("nicDobMatch", false);
                                nicGenderMatch = data.getBooleanExtra("nicGenderMatch", false);
                                nicVerificationStatus = data.getStringExtra("nicVerificationStatus");

                                if (!TextUtils.isEmpty(detectedNic)) {
                                    etNic.setText(detectedNic);
                                }

                                Toast.makeText(
                                        ProfileSetupActivity.this,
                                        "NIC processed: " + (nicVerificationStatus == null ? "UNKNOWN" : nicVerificationStatus),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                    });

    private final ActivityResultLauncher<Intent> mapPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    selectedLatitude = data.getDoubleExtra("lat", 0.0);
                    selectedLongitude = data.getDoubleExtra("lng", 0.0);
                    selectedMapAddress = data.getStringExtra("address");
                    selectedMapSnapshotPath = data.getStringExtra("snapshot_path");

                    if (TextUtils.isEmpty(selectedMapAddress)) {
                        selectedMapAddress = String.format(
                                Locale.US,
                                "%.6f, %.6f",
                                selectedLatitude,
                                selectedLongitude
                        );
                    }

                    selectedArea = extractArea(selectedMapAddress);
                    selectedProvince = extractProvince(selectedMapAddress);

                    etAddress.setText(selectedMapAddress);
                    etAddress.setError(null);
                    refreshLocationRequiredUi(false);

                    FirebaseDebugLogger.success(
                            "profile_location_selected",
                            "lat=" + selectedLatitude + ", lng=" + selectedLongitude,
                            selectedMapAddress
                    );

                    Toast.makeText(
                            ProfileSetupActivity.this,
                            "Location selected for matching",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etAddress = findViewById(R.id.etAddress);
        tvSelectLocation = findViewById(R.id.tvSelectLocation);
        tvLocationWarning = findViewById(R.id.tvLocationWarning);
        etNic = findViewById(R.id.etNic);
        btnUploadNic = findViewById(R.id.btnUploadNic);
        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        etDob = findViewById(R.id.etDob);
        spinnerSkills = findViewById(R.id.spinnerSkills);
        tvSkillsLabel = findViewById(R.id.tvSkillsLabel);
        btnSubmit = findViewById(R.id.btnSubmitProfile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (getIntent() != null && getIntent().hasExtra("role")) {
            String incomingRole = getIntent().getStringExtra("role");
            if (!TextUtils.isEmpty(incomingRole)) {
                role = incomingRole;
            }
        }

        if (!"worker".equalsIgnoreCase(role)) {
            spinnerSkills.setVisibility(android.view.View.GONE);
            tvSkillsLabel.setVisibility(android.view.View.GONE);
        } else {
            spinnerSkills.setVisibility(android.view.View.VISIBLE);
            tvSkillsLabel.setVisibility(android.view.View.VISIBLE);
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Cleaning", "Cooking", "Driving", "Construction", "Electrical", "Other"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSkills.setAdapter(adapter);

        btnUploadNic.setOnClickListener(v -> openNicVerification());

        tvSelectLocation.setOnClickListener(v -> openMapPicker());
        etAddress.setOnClickListener(v -> openMapPicker());
        etAddress.setFocusable(false);
        etAddress.setCursorVisible(false);
        refreshLocationRequiredUi(false);

        etDob.setOnClickListener(v -> showDatePicker());

        btnSubmit.setOnClickListener(v -> submitProfile());
    }

    private void showLocationRequiredWarning() {
        etAddress.setError("Map location is required");
        etAddress.requestFocus();
        refreshLocationRequiredUi(true);
        Toast.makeText(this, "Please select and confirm your location on the map", Toast.LENGTH_LONG).show();
    }

    private void refreshLocationRequiredUi(boolean showWarning) {
        if (tvLocationWarning != null) {
            tvLocationWarning.setVisibility(showWarning ? View.VISIBLE : View.GONE);
        }

        if (tvSelectLocation != null) {
            if (selectedLatitude != null && selectedLongitude != null) {
                tvSelectLocation.setText("Location selected ✓ Tap to change");
                tvSelectLocation.setTextColor(Color.parseColor("#16A34A"));
            } else {
                tvSelectLocation.setText("Select location on the map *");
                tvSelectLocation.setTextColor(showWarning ? Color.parseColor("#DC2626") : Color.parseColor("#F59E0B"));
            }
        }
    }

    private void openNicVerification() {
        String dob = etDob.getText() != null ? etDob.getText().toString().trim() : "";
        int checkedId = rgGender.getCheckedRadioButtonId();

        String gender = "";
        if (checkedId == R.id.rbMale) {
            gender = "M";
        } else if (checkedId == R.id.rbFemale) {
            gender = "F";
        }

        if (TextUtils.isEmpty(dob)) {
            Toast.makeText(this, "Please select DOB first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(gender)) {
            Toast.makeText(this, "Please select gender first", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(ProfileSetupActivity.this, NicVerificationActivity.class);
        intent.putExtra(NicVerificationActivity.EXTRA_ENTERED_DOB, dob);
        intent.putExtra(NicVerificationActivity.EXTRA_ENTERED_GENDER, gender);
        nicVerificationLauncher.launch(intent);
    }

    private void openMapPicker() {
        Intent intent = new Intent(ProfileSetupActivity.this, MapPickerActivity.class);
        String currentAddress = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(currentAddress)) {
            intent.putExtra("address", currentAddress);
        }
        mapPickerLauncher.launch(intent);
    }

    private String extractProvince(String address) {
        if (TextUtils.isEmpty(address)) return "";
        String[] parts = address.split(",");
        for (String p : parts) {
            String t = p.trim();
            if (t.toLowerCase(Locale.US).contains("province")) return t;
        }
        return parts.length >= 2 ? parts[parts.length - 2].trim() : "";
    }

    private String extractArea(String address) {
        if (TextUtils.isEmpty(address)) return "";
        String[] parts = address.split(",");
        for (String p : parts) {
            String t = p.trim();
            String low = t.toLowerCase(Locale.US);
            if (TextUtils.isEmpty(t)) continue;
            if (low.contains("province") || low.equals("sri lanka")) continue;
            if (low.contains("road") || low.contains("street") || low.contains("lane")) continue;
            return t;
        }
        return parts.length > 0 ? parts[0].trim() : "";
    }

    private void showDatePicker() {
        final Calendar today = Calendar.getInstance();

        int year = 2000;
        int month = Calendar.JANUARY;
        int day = 1;

        String currentDob = etDob.getText() != null ? etDob.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(currentDob) && currentDob.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                String[] parts = currentDob.split("-");
                year = Integer.parseInt(parts[0]);
                month = Integer.parseInt(parts[1]) - 1;
                day = Integer.parseInt(parts[2]);
            } catch (Exception ignored) {
            }
        }

        DatePickerDialog dpd = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                    String chosen = String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            selectedYear,
                            selectedMonth + 1,
                            selectedDayOfMonth
                    );
                    etDob.setText(chosen);
                    etDob.setError(null);
                },
                year,
                month,
                day
        );

        dpd.getDatePicker().setMaxDate(today.getTimeInMillis());

        try {
            dpd.getDatePicker().setCalendarViewShown(false);
            dpd.getDatePicker().setSpinnersShown(true);
        } catch (Exception ignored) {
        }

        dpd.show();
    }

    private void submitProfile() {
        final String first = etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
        final String last = etLastName.getText() != null ? etLastName.getText().toString().trim() : "";
        final String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        final String nic = etNic.getText() != null ? etNic.getText().toString().trim() : "";
        final String dob = etDob.getText() != null ? etDob.getText().toString().trim() : "";

        final int selectedGenderId = rgGender.getCheckedRadioButtonId();
        final String gender;
        if (selectedGenderId == R.id.rbMale) {
            gender = "M";
        } else if (selectedGenderId == R.id.rbFemale) {
            gender = "F";
        } else {
            gender = null;
        }

        final String skill = spinnerSkills.getSelectedItem() != null
                ? spinnerSkills.getSelectedItem().toString().trim()
                : "";

        if (TextUtils.isEmpty(first)) {
            etFirstName.setError("Enter first name");
            etFirstName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(last)) {
            etLastName.setError("Enter last name");
            etLastName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(address) || selectedLatitude == null || selectedLongitude == null) {
            showLocationRequiredWarning();
            return;
        }
        if (TextUtils.isEmpty(dob)) {
            etDob.setError("Enter date of birth");
            etDob.requestFocus();
            return;
        }
        if (gender == null) {
            Toast.makeText(this, "Select gender", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("worker".equalsIgnoreCase(role) && TextUtils.isEmpty(skill)) {
            Toast.makeText(this, "Please choose at least one skill", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please verify your phone number before completing profile.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(ProfileSetupActivity.this, SignUpActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        writeProfileToFirestore(first, last, address, nic, dob, gender, skill);
    }

    private void writeProfileToFirestore(String first, String last, String address, String nic,
                                         String dob, String gender, String skill) {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "No authenticated user available", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("lat", selectedLatitude);
        locationMap.put("lng", selectedLongitude);

        String finalArea = !TextUtils.isEmpty(selectedArea) ? selectedArea : extractArea(address);
        String finalProvince = !TextUtils.isEmpty(selectedProvince) ? selectedProvince : extractProvince(address);

        Map<String, Object> userDoc = new HashMap<>();
        userDoc.put("uid", uid);
        userDoc.put("role", role);
        userDoc.put("nicVerified", false);
        userDoc.put("location", locationMap);
        userDoc.put("locationText", address);
        userDoc.put("address", address);
        userDoc.put("area", finalArea);
        userDoc.put("province", finalProvince);
        userDoc.put("lastSeenAt", FieldValue.serverTimestamp());

        Map<String, Object> nicProcessingConsent = new HashMap<>();
        nicProcessingConsent.put("given", !TextUtils.isEmpty(nic));
        nicProcessingConsent.put("version", "v1.0");
        nicProcessingConsent.put("at", FieldValue.serverTimestamp());

        Map<String, Object> consent = new HashMap<>();
        consent.put("nicProcessing", nicProcessingConsent);

        Map<String, Object> profileDoc = new HashMap<>();
        profileDoc.put("uid", uid);
        profileDoc.put("firstName", first);
        profileDoc.put("lastName", last);
        profileDoc.put("displayName", (first + " " + last).trim());
        profileDoc.put("location", locationMap);
        profileDoc.put("lat", selectedLatitude);
        profileDoc.put("lng", selectedLongitude);
        profileDoc.put("locationText", address);
        profileDoc.put("address", address);
        profileDoc.put("area", finalArea);
        profileDoc.put("province", finalProvince);
        profileDoc.put("mapSnapshotPath", selectedMapSnapshotPath);
        profileDoc.put("role", role);
        profileDoc.put("isWorker", "worker".equalsIgnoreCase(role));

        profileDoc.put("dob", dob);
        profileDoc.put("gender", gender);

        profileDoc.put("nicNumber", nic);
        profileDoc.put("nicFrontUri", nicFrontUriString);
        profileDoc.put("nicBackUri", nicBackUriString);
        profileDoc.put("nicParsedDob", nicParsedDob);
        profileDoc.put("nicParsedGender", nicParsedGender);
        profileDoc.put("nicMatch", nicMatch);
        profileDoc.put("nicDobMatch", nicDobMatch);
        profileDoc.put("nicGenderMatch", nicGenderMatch);
        profileDoc.put("nicVerificationStatus",
                TextUtils.isEmpty(nic) ? "NOT_PROVIDED" : nicVerificationStatus);
        profileDoc.put("nicVerified", false);
        profileDoc.put("nicParsedAt", FieldValue.serverTimestamp());

        profileDoc.put("consent", consent);
        profileDoc.put("profileCompleted", true);
        profileDoc.put("createdAt", FieldValue.serverTimestamp());
        profileDoc.put("memberSince", FieldValue.serverTimestamp());

        if ("worker".equalsIgnoreCase(role)) {
            profileDoc.put("skills", Collections.singletonList(skill.toLowerCase(Locale.US)));
            profileDoc.put("skill", skill);
        }

        db.collection("users").document(uid)
                .set(userDoc, SetOptions.merge())
                .continueWithTask(task -> db.collection("profiles").document(uid)
                        .set(profileDoc, SetOptions.merge()))
                .continueWithTask(task -> maybeEnqueueNicVerification(uid, first, last, nic, dob, gender, profileDoc))
                .addOnSuccessListener(unused -> {
                    FirebaseDebugLogger.success(
                            "profile_setup_write",
                            "users/" + uid + ", profiles/" + uid,
                            "location=" + selectedLatitude + "," + selectedLongitude + ", area=" + finalArea + ", province=" + finalProvince
                    );
                    Toast.makeText(ProfileSetupActivity.this,
                            "Profile saved successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(ProfileSetupActivity.this, MainActivity.class);
                    intent.putExtra("openProfile", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    FirebaseDebugLogger.failure("profile_setup_write", "users/" + uid + ", profiles/" + uid, e);
                    Toast.makeText(ProfileSetupActivity.this,
                            "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

private com.google.android.gms.tasks.Task<Void> maybeEnqueueNicVerification(String uid,
                                                                            String first,
                                                                            String last,
                                                                            String nic,
                                                                            String dob,
                                                                            String gender,
                                                                            Map<String, Object> profileDoc) {
    if (TextUtils.isEmpty(nic)) {
        return com.google.android.gms.tasks.Tasks.forResult(null);
    }

    final String queueStatus;
    if (nicMatch) {
        queueStatus = "AUTO_MATCHED_PENDING_ADMIN";
    } else if (nicDobMatch || nicGenderMatch) {
        queueStatus = "PENDING_MANUAL_REVIEW";
    } else {
        queueStatus = "MISMATCH";
    }

    Map<String, Object> queueDoc = new HashMap<>();
    queueDoc.put("uid", uid);
    queueDoc.put("displayName", (first + " " + last).trim());
    queueDoc.put("nicNumberParsed", nic);
    queueDoc.put("nicFrontUri", nicFrontUriString);
    queueDoc.put("nicBackUri", nicBackUriString);
    queueDoc.put("nicParsedDob", nicParsedDob);
    queueDoc.put("nicParsedGender", nicParsedGender);
    queueDoc.put("enteredDob", dob);
    queueDoc.put("enteredGender", gender);
    queueDoc.put("nicMatch", nicMatch);
    queueDoc.put("nicDobMatch", nicDobMatch);
    queueDoc.put("nicGenderMatch", nicGenderMatch);
    queueDoc.put("status", queueStatus);
    queueDoc.put("reviewReason", null);
    queueDoc.put("submittedAt", FieldValue.serverTimestamp());
    queueDoc.put("updatedAt", FieldValue.serverTimestamp());
    queueDoc.put("source", "mobile_profile_setup");
    queueDoc.put("profileLocationText", selectedMapAddress);

    return db.collection("nic_verification_queue")
            .document(uid)
            .set(queueDoc, SetOptions.merge())
            .continueWithTask(task -> db.collection("profiles").document(uid)
                    .set(new HashMap<String, Object>() {{
                        put("nicVerificationStatus", queueStatus);
                        put("nicQueueId", uid);
                        put("nicSubmittedAt", FieldValue.serverTimestamp());
                    }}, SetOptions.merge()));
}

}
