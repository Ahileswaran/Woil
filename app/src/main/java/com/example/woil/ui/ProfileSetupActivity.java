package com.example.woil.ui;

import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
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
import com.example.woil.R;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {

    private static final int REQ_IMAGE = 100;

    private EditText etFirstName, etLastName, etAddress, etNic, etDob;
    private TextView tvSelectLocation, tvSkipNic, tvSkillsLabel;
    private ImageButton btnUploadNic;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale;
    private Spinner spinnerSkills;
    private MaterialButton btnSubmit;

    private String nicImageUriString = null;
    private String role = "worker"; // default

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // modern activity result launcher for picking images
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                Uri selected = result.getData().getData();
                                if (selected != null) {
                                    nicImageUriString = selected.toString();
                                    Toast.makeText(ProfileSetupActivity.this, "NIC photo selected", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        // UI refs
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

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // read role from intent (if passed)
        if (getIntent() != null && getIntent().hasExtra("role")) {
            role = getIntent().getStringExtra("role");
        }

        // Show/hide skills for workers only
        if (!"worker".equalsIgnoreCase(role)) {
            spinnerSkills.setVisibility(View.GONE);
            tvSkillsLabel.setVisibility(View.GONE);
        } else {
            spinnerSkills.setVisibility(View.VISIBLE);
            tvSkillsLabel.setVisibility(View.VISIBLE);
        }

        // populate skills spinner
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"Cleaning", "Cooking", "Driving", "Construction", "Electrical", "Other"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSkills.setAdapter(adapter);

        // upload NIC image (just pick and store URI string for tests)
        btnUploadNic.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                pickImageLauncher.launch(Intent.createChooser(i, "Select NIC photo"));
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, "No app found to pick image", Toast.LENGTH_SHORT).show();
            }
        });

        tvSkipNic.setOnClickListener(v -> {
            nicImageUriString = null;
            Toast.makeText(this, "NIC skipped", Toast.LENGTH_SHORT).show();
        });

        // select location (open maps search)
        tvSelectLocation.setOnClickListener(v -> {
            String q = etAddress.getText().toString().trim();
            if (TextUtils.isEmpty(q)) q = "my location";
            android.net.Uri gmmIntentUri = android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(q));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            try {
                startActivity(mapIntent);
            } catch (ActivityNotFoundException e) {
                Intent alt = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=" + android.net.Uri.encode(q)));
                startActivity(alt);
            }
        });

        // date picker for DOB
        etDob.setOnClickListener(v -> showDatePicker());

        btnSubmit.setOnClickListener(v -> submitProfile());
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String chosen = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
            etDob.setText(chosen);
        }, y, m, d);

        dpd.show();
    }

    private void submitProfile() {
        String first = etFirstName.getText().toString().trim();
        String last = etLastName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String nic = etNic.getText().toString().trim();
        String dob = etDob.getText().toString().trim();

        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        String gender = null;
        if (selectedGenderId == R.id.rbMale) gender = "male";
        else if (selectedGenderId == R.id.rbFemale) gender = "female";

        String skill = spinnerSkills.getSelectedItem() != null ? spinnerSkills.getSelectedItem().toString() : "";

        // Basic validation
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
        if ("worker".equalsIgnoreCase(role) && (skill == null || skill.isEmpty())) {
            Toast.makeText(this, "Please choose at least one skill", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure we have an authenticated user (uid). For testing with bypassed OTP we expect anonymous or phone user.
        if (mAuth.getCurrentUser() == null) {
            // fallback: sign in anonymously to get a uid for test flow
            mAuth.signInAnonymously().addOnCompleteListener(task -> {
                if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                    writeProfileToFirestore(first, last, address, nic, dob, null, skill);
                } else {
                    String err = task.getException() != null ? task.getException().getMessage() : "Anonymous signin failed";
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

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", first);
        updates.put("lastName", last);
        updates.put("address", address);
        updates.put("nic", nic);
        updates.put("dob", dob);
        updates.put("gender", gender);
        updates.put("role", role);
        if ("worker".equalsIgnoreCase(role)) updates.put("skill", skill);
        updates.put("nicImageUri", nicImageUriString); // stores the selected URI string for testing
        updates.put("profileCompleted", true);
        updates.put("lastSeenAt", FieldValue.serverTimestamp());

        // Merge so we don't overwrite existing fields such as phone/nicVerified that were set earlier
        db.collection("users").document(uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileSetupActivity.this, "Profile saved successfully", Toast.LENGTH_SHORT).show();

                    // Start MainActivity and ask it to open ProfileFragment
                    Intent intent = new Intent(ProfileSetupActivity.this, MainActivity.class);
                    intent.putExtra("openProfile", true);
                    // clear back stack so user cannot go back to setup
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })

                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileSetupActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
