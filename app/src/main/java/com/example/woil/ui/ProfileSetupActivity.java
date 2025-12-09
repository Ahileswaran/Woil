package com.example.woil.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

import com.example.woil.FirestoreHelper;
import com.example.woil.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {
    private EditText etDisplayName, etFirstName, etLastName, etLocationText, etNicImageUrl; // For simplicity, text for URL; use picker for real
    private Button btnSaveProfile;
    private FirebaseAuth mAuth;
    private FirestoreHelper firestoreHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        mAuth = FirebaseAuth.getInstance();
        firestoreHelper = new FirestoreHelper();

        etDisplayName = findViewById(R.id.etDisplayName);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etLocationText = findViewById(R.id.etLocationText);
        etNicImageUrl = findViewById(R.id.etNicImageUrl); // Placeholder; implement upload
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String uid = mAuth.getCurrentUser().getUid();
        String displayName = etDisplayName.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String locationText = etLocationText.getText().toString().trim();
        String nicImageUrl = etNicImageUrl.getText().toString().trim(); // In real, upload to Storage and get URL

        if (TextUtils.isEmpty(displayName) || TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName)) {
            // Error handling
            return;
        }

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("displayName", displayName);
        profileData.put("firstName", firstName);
        profileData.put("lastName", lastName);
        // Location: Use GeoPoint if you have lat/long; here using text
        profileData.put("locationText", locationText);
        profileData.put("nicImageUrl", nicImageUrl);
        profileData.put("nicVerified", false);
        // Add other fields as needed, e.g., proficiencyLevel, uiSettings, wearable

    }
}