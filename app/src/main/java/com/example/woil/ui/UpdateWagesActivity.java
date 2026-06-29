package com.example.woil.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.woil.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UpdateWagesActivity extends AppCompatActivity {

    private EditText etCleaning, etGardening, etPlumbing, etHousekeeping, etLaundry, etCaregiving, etOther;
    private Button btnSave;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_wages);

        db = FirebaseFirestore.getInstance();

        etCleaning = findViewById(R.id.etWageCleaning);
        etGardening = findViewById(R.id.etWageGardening);
        etPlumbing = findViewById(R.id.etWagePlumbing);
        etHousekeeping = findViewById(R.id.etWageHousekeeping);
        etLaundry = findViewById(R.id.etWageLaundry);
        etCaregiving = findViewById(R.id.etWageCaregiving);
        etOther = findViewById(R.id.etWageOther);
        btnSave = findViewById(R.id.btnSaveWages);

        loadCurrentWages();

        btnSave.setOnClickListener(v -> saveWages());
    }

    private void loadCurrentWages() {
        db.collection("system_config").document("market_wages").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double cleaning = documentSnapshot.getDouble("cleaning");
                        Double gardening = documentSnapshot.getDouble("gardening");
                        Double plumbing = documentSnapshot.getDouble("plumbing");
                        Double housekeeping = documentSnapshot.getDouble("housekeeping");
                        Double laundry = documentSnapshot.getDouble("laundry");
                        Double caregiving = documentSnapshot.getDouble("caregiving");
                        Double other = documentSnapshot.getDouble("other");

                        if (cleaning != null) etCleaning.setText(String.valueOf(cleaning));
                        if (gardening != null) etGardening.setText(String.valueOf(gardening));
                        if (plumbing != null) etPlumbing.setText(String.valueOf(plumbing));
                        if (housekeeping != null) etHousekeeping.setText(String.valueOf(housekeeping));
                        if (laundry != null) etLaundry.setText(String.valueOf(laundry));
                        if (caregiving != null) etCaregiving.setText(String.valueOf(caregiving));
                        if (other != null) etOther.setText(String.valueOf(other));
                    } else {
                        // Defaults
                        etCleaning.setText("600");
                        etGardening.setText("700");
                        etPlumbing.setText("900");
                        etHousekeeping.setText("650");
                        etLaundry.setText("550");
                        etCaregiving.setText("800");
                        etOther.setText("600");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load wages", Toast.LENGTH_SHORT).show());
    }

    private void saveWages() {
        Map<String, Object> wages = new HashMap<>();
        wages.put("cleaning", parseDouble(etCleaning.getText().toString(), 600.0));
        wages.put("gardening", parseDouble(etGardening.getText().toString(), 700.0));
        wages.put("plumbing", parseDouble(etPlumbing.getText().toString(), 900.0));
        wages.put("housekeeping", parseDouble(etHousekeeping.getText().toString(), 650.0));
        wages.put("laundry", parseDouble(etLaundry.getText().toString(), 550.0));
        wages.put("caregiving", parseDouble(etCaregiving.getText().toString(), 800.0));
        wages.put("other", parseDouble(etOther.getText().toString(), 600.0));

        db.collection("system_config").document("market_wages")
                .set(wages)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Wages updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update wages", Toast.LENGTH_SHORT).show());
    }

    private double parseDouble(String text, double defaultValue) {
        try {
            if (text == null || text.trim().isEmpty()) return defaultValue;
            return Double.parseDouble(text.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
