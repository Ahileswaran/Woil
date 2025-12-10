package com.example.woil.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.woil.FirestoreHelper;
import com.example.woil.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {

    private EditText etDisplayName, etFirstName, etLastName, etLocationText, etNicImageUrl;
    private RadioGroup rgProficiency;
    private Button btnSaveProfile;

    private FirebaseAuth mAuth;
    private FirestoreHelper firestoreHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        mAuth = FirebaseAuth.getInstance();
        firestoreHelper = new FirestoreHelper();

        etDisplayName = findViewById(R.id.etDisplayName);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etLocationText = findViewById(R.id.etLocationText);
        etNicImageUrl = findViewById(R.id.etNicImageUrl);
        rgProficiency = findViewById(R.id.rgProficiency);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        // Prefill NIC if passed from signup
        String nic = getIntent().getStringExtra("nic");
        if (nic != null) {
            // optionally prefill an input or keep for parsing; do NOT auto-save sensitive data without consent
        }

        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String displayName = etDisplayName.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String locationText = etLocationText.getText().toString().trim();
        String nicImageUrl = etNicImageUrl.getText().toString().trim();

        if (TextUtils.isEmpty(displayName) || TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName)) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int checkedId = rgProficiency.getCheckedRadioButtonId();
        if (checkedId == -1) {
            Toast.makeText(this, "Please select proficiency level", Toast.LENGTH_SHORT).show();
            return;
        }
        String proficiency = ((RadioButton) findViewById(checkedId)).getText().toString();

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("uid", uid);
        profileData.put("displayName", displayName);
        profileData.put("firstName", firstName);
        profileData.put("lastName", lastName);
        profileData.put("locationText", locationText);
        profileData.put("nicImageUrl", nicImageUrl);
        profileData.put("nicVerified", false);
        profileData.put("proficiencyLevel", proficiency);

        Map<String, Object> uiSettings = new HashMap<>();
        uiSettings.put("voiceGuided", true);
        uiSettings.put("simplifiedLayout", true);
        uiSettings.put("language", "ta");
        uiSettings.put("confirmWithPhysicalButtons", true);
        uiSettings.put("preferredFontScale", 1.3);

        profileData.put("uiSettings", uiSettings);

        // minimal wearable metadata
        Map<String, Object> wearable = new HashMap<>();
        wearable.put("deviceId", null);
        profileData.put("wearable", wearable);

        profileData.put("createdAt", FieldValue.serverTimestamp());

        // Save profile using Task-based API
        firestoreHelper.saveProfile(uid, profileData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(ProfileSetupActivity.this, "Profile saved", Toast.LENGTH_SHORT).show();
                        // Optionally navigate to main screen
                        // startActivity(new Intent(ProfileSetupActivity.this, MainActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ProfileSetupActivity.this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
