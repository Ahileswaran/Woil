package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ClientWorkerSelectionActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvName, tvSkill, tvRating, tvDistance, tvEta;
    private MaterialButton btnRequestMatch;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String workerUid;
    private String workerName;
    private String workerSkill;
    private double workerRating;
    private double workerDistanceKm;
    private long workerEtaMinutes;
    private String clientAddress;
    private String selectedCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_job); // temporary reuse if needed

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        workerUid = getIntent().getStringExtra("workerUid");
        workerName = getIntent().getStringExtra("workerName");
        workerSkill = getIntent().getStringExtra("workerSkill");
        workerRating = getIntent().getDoubleExtra("workerRating", 0.0);
        workerDistanceKm = getIntent().getDoubleExtra("workerDistanceKm", 0.0);
        workerEtaMinutes = getIntent().getLongExtra("workerEtaMinutes", 0L);
        clientAddress = getIntent().getStringExtra("clientAddress");
        selectedCategory = getIntent().getStringExtra("selectedCategory");

        // Minimal placeholder flow
        Toast.makeText(this, "Selected worker: " + workerName, Toast.LENGTH_SHORT).show();

        // For now immediately create a pending job request record if needed later
        createPendingMatchRequest();
        finish();
    }

    private void createPendingMatchRequest() {
        if (mAuth.getCurrentUser() == null) return;

        Map<String, Object> request = new HashMap<>();
        request.put("clientUid", mAuth.getCurrentUser().getUid());
        request.put("workerUid", workerUid);
        request.put("category", selectedCategory);
        request.put("clientAddress", clientAddress);
        request.put("status", "PENDING");
        request.put("createdAt", FieldValue.serverTimestamp());

        db.collection("provider_responses")
                .add(request)
                .addOnSuccessListener(doc -> Toast.makeText(this, "Match request sent", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}