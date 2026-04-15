package com.example.woil.ui;

import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etAddress, etNic, etDob;
    private TextView tvSelectLocation, tvSkipNic, tvSkillsLabel;
    private ImageButton btnUploadNic;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale;
    private Spinner spinnerSkills;
    private MaterialButton btnSubmit;

    private String role = "worker";

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
        etNic = findViewById(R.id.etNic);
        btnUploadNic = findViewById(R.id.btnUploadNic);
        tvSkipNic = findViewById(R.id.tvSkipNic);
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
            spinnerSkills.setVisibility(View.GONE);
            tvSkillsLabel.setVisibility(View.GONE);
        } else {
            spinnerSkills.setVisibility(View.VISIBLE);
            tvSkillsLabel.setVisibility(View.VISIBLE);
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Cleaning", "Cooking", "Driving", "Construction", "Electrical", "Other"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSkills.setAdapter(adapter);

        btnUploadNic.setOnClickListener(v -> openNicVerification());

        tvSkipNic.setOnClickListener(v -> {
            nicFrontUriString = null;
            nicBackUriString = null;
            nicParsedDob = null;
            nicParsedGender = null;
            nicMatch = false;
            nicDobMatch = false;
            nicGenderMatch = false;
            nicVerificationStatus = "NOT_PROVIDED";
            etNic.setText("");
            Toast.makeText(this, "NIC skipped", Toast.LENGTH_SHORT).show();
        });

        tvSelectLocation.setOnClickListener(v -> {
            String q = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
            if (TextUtils.isEmpty(q)) q = "my location";

            android.net.Uri gmmIntentUri =
                    android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(q));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            try {
                startActivity(mapIntent);
            } catch (ActivityNotFoundException e) {
                Intent alt = new Intent(
                        Intent.ACTION_VIEW,
                        android.net.Uri.parse(
                                "https://www.google.com/maps/search/?api=1&query=" +
                                        android.net.Uri.encode(q)
                        )
                );
                startActivity(alt);
            }
        });

        etDob.setOnClickListener(v -> showDatePicker());

        btnSubmit.setOnClickListener(v -> submitProfile());
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

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String chosen = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            etDob.setText(chosen);
        }, y, m, d);

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
        if (TextUtils.isEmpty(address)) {
            etAddress.setError("Enter address");
            etAddress.requestFocus();
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
            mAuth.signInAnonymously().addOnCompleteListener(task -> {
                if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                    writeProfileToFirestore(first, last, address, nic, dob, gender, skill);
                } else {
                    String err = task.getException() != null
                            ? task.getException().getMessage()
                            : "Anonymous signin failed";
                    Toast.makeText(ProfileSetupActivity.this, "Auth error: " + err, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            writeProfileToFirestore(first, last, address, nic, dob, gender, skill);
        }
    }

    private void writeProfileToFirestore(String first, String last, String address, String nic,
                                         String dob, String gender, String skill) {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "No authenticated user available", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> userDoc = new HashMap<>();
        userDoc.put("uid", uid);
        userDoc.put("role", role);
        userDoc.put("nicVerified", false);
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
        profileDoc.put("locationText", address);
        profileDoc.put("address", address);
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
                .addOnSuccessListener(unused -> {
                    Toast.makeText(ProfileSetupActivity.this,
                            "Profile saved successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(ProfileSetupActivity.this, MainActivity.class);
                    intent.putExtra("openProfile", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileSetupActivity.this,
                            "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}