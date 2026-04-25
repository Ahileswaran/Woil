package com.example.woil.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class ClientWorkerSelectionActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String workerUid;
    private String workerName;
    private String workerSkill;
    private double workerRating;
    private double workerDistanceKm;
    private long workerEtaMinutes;
    private String workerLocationText;
    private String workerArea;
    private String workerProvince;
    private String clientAddress;
    private String clientArea;
    private String clientProvince;
    private double clientLat;
    private double clientLng;
    private String selectedCategory;
    private String matchLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_job); // temporary reuse

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
        workerLocationText = getIntent().getStringExtra("workerLocationText");
        workerArea = getIntent().getStringExtra("workerArea");
        workerProvince = getIntent().getStringExtra("workerProvince");
        clientAddress = getIntent().getStringExtra("clientAddress");
        clientArea = getIntent().getStringExtra("clientArea");
        clientProvince = getIntent().getStringExtra("clientProvince");
        clientLat = getIntent().getDoubleExtra("clientLat", 0.0);
        clientLng = getIntent().getDoubleExtra("clientLng", 0.0);
        selectedCategory = getIntent().getStringExtra("selectedCategory");
        matchLevel = getIntent().getStringExtra("matchLevel");

        Toast.makeText(this, "Selected worker: " + workerName, Toast.LENGTH_SHORT).show();
        createPendingMatchRequest();
    }

    private void createPendingMatchRequest() {
        String clientUid = FirebaseDebugLogger.requireUid(this, mAuth, "matching_request_create");
        if (clientUid == null) return;

        DocumentReference matchRef = db.collection("matches").document();
        DocumentReference requestRef = db.collection("matching_requests").document(matchRef.getId());

        Map<String, Object> request = new HashMap<>();
        request.put("matchId", matchRef.getId());
        request.put("clientUid", clientUid);
        request.put("workerUid", workerUid);
        request.put("workerName", workerName);
        request.put("workerSkill", workerSkill);
        request.put("workerRating", workerRating);
        request.put("category", selectedCategory);
        request.put("clientAddress", clientAddress);
        request.put("clientArea", clientArea);
        request.put("clientProvince", clientProvince);
        request.put("workerLocationText", workerLocationText);
        request.put("workerArea", workerArea);
        request.put("workerProvince", workerProvince);
        request.put("distanceKm", workerDistanceKm);
        request.put("etaMinutes", workerEtaMinutes);
        request.put("matchLevel", matchLevel);
        request.put("status", "PENDING");
        request.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> clientLocation = new HashMap<>();
        clientLocation.put("lat", clientLat);
        clientLocation.put("lng", clientLng);
        request.put("clientLocation", clientLocation);

        WriteBatch batch = db.batch();
        batch.set(matchRef, request);
        batch.set(requestRef, request);

        batch.commit()
                .addOnSuccessListener(unused -> {
                    FirebaseDebugLogger.success("matching_request_create", "matches+matching_requests", matchRef.getId());
                    Toast.makeText(this, "Match request sent", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    FirebaseDebugLogger.failure("matching_request_create", "matches+matching_requests", e);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
