
package com.example.woil.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EmergencyContactsActivity extends AppCompatActivity {

    private EditText etName, etPhone, etRelation;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        ImageButton btnBack = findViewById(R.id.btn_back_arrow_settings);
        MaterialButton btnSave = findViewById(R.id.btn_save_contact);
        etName = findViewById(R.id.et_contact_name);
        etPhone = findViewById(R.id.et_contact_phone);
        etRelation = findViewById(R.id.et_contact_relation);

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveContact());

        loadContact();
    }

    private void loadContact() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (TextUtils.isEmpty(uid)) return;

        db.collection("ccc_contacts").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    etName.setText(doc.getString("name"));
                    etPhone.setText(doc.getString("phone"));
                    etRelation.setText(doc.getString("relation"));
                });
    }

    private void saveContact() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (TextUtils.isEmpty(uid)) {
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String relation = etRelation.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Name and phone are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("name", name);
        data.put("phone", phone);
        data.put("relation", relation);
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("ccc_contacts").document(uid).set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> Toast.makeText(this, "Emergency contact saved.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
