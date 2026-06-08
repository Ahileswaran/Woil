package com.example.woil.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;

public class WorkerJobOffersActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration offersListener;

    private TextView tvEmptyOffers;
    private View cardOfferDetail;
    private View layoutOfferActions;

    private TextView tvClientName;
    private TextView tvJobCategory;
    private TextView tvJobDistance;
    private TextView tvJobEta;
    private TextView tvJobLocation;

    private MaterialButton btnMessage;
    private MaterialButton btnAccept;
    private MaterialButton btnReject;

    private String currentMatchId;
    private String clientUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_job_offers);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ImageButton btnBack = findViewById(R.id.btn_back_arrow_settings);
        btnBack.setOnClickListener(v -> finish());

        tvEmptyOffers = findViewById(R.id.tv_empty_offers);
        cardOfferDetail = findViewById(R.id.card_offer_detail);
        layoutOfferActions = findViewById(R.id.layout_offer_actions);

        tvClientName = findViewById(R.id.tv_client_name);
        tvJobCategory = findViewById(R.id.tv_job_category);
        tvJobDistance = findViewById(R.id.tv_job_distance);
        tvJobEta = findViewById(R.id.tv_job_eta);
        tvJobLocation = findViewById(R.id.tv_job_location);

        btnMessage = findViewById(R.id.btn_message_client);
        btnAccept = findViewById(R.id.btn_accept_offer);
        btnReject = findViewById(R.id.btn_reject_offer);

        listenToJobOffers();
    }

    private void listenToJobOffers() {
        String workerUid = FirebaseDebugLogger.requireUid(this, mAuth, "worker_offers_listen");
        if (workerUid == null) return;

        offersListener = db.collection("matches")
                .whereEqualTo("workerUid", workerUid)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        FirebaseDebugLogger.failure("worker_offers_listen", "matches", e);
                        return;
                    }

                    if (snap == null || snap.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    currentMatchId = doc.getId();
                    clientUid = doc.getString("clientUid");

                    showOfferDetails(doc);
                });
    }

    private void showEmptyState() {
        tvEmptyOffers.setVisibility(View.VISIBLE);
        cardOfferDetail.setVisibility(View.GONE);
        layoutOfferActions.setVisibility(View.GONE);
        currentMatchId = null;
        clientUid = null;
    }

    private void showOfferDetails(DocumentSnapshot doc) {
        tvEmptyOffers.setVisibility(View.GONE);
        cardOfferDetail.setVisibility(View.VISIBLE);
        layoutOfferActions.setVisibility(View.VISIBLE);

        String category = doc.getString("category");
        Double distance = doc.getDouble("distanceKm");
        Long eta = doc.getLong("etaMinutes");
        String address = doc.getString("clientAddress");

        tvJobCategory.setText("Category: " + (TextUtils.isEmpty(category) ? "Cleaning" : category));
        tvJobDistance.setText("Distance: " + (distance != null ? String.format(Locale.getDefault(), "%.1f km", distance) : "calculating..."));
        tvJobEta.setText("Estimated ETA: " + (eta != null ? eta + " min" : "calculating..."));
        tvJobLocation.setText("Location: " + (TextUtils.isEmpty(address) ? "Not specified" : address));

        tvClientName.setText("Client: Loading...");
        if (!TextUtils.isEmpty(clientUid)) {
            db.collection("profiles").document(clientUid).get()
                    .addOnSuccessListener(pDoc -> {
                        if (pDoc.exists()) {
                            String name = pDoc.getString("displayName");
                            if (TextUtils.isEmpty(name)) {
                                String first = pDoc.getString("firstName");
                                String last = pDoc.getString("lastName");
                                name = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
                            }
                            tvClientName.setText("Client: " + (TextUtils.isEmpty(name) ? "Client" : name));
                        } else {
                            tvClientName.setText("Client: Client");
                        }
                    })
                    .addOnFailureListener(ex -> tvClientName.setText("Client: Client"));
        }

        btnMessage.setOnClickListener(v -> {
            if (TextUtils.isEmpty(clientUid)) return;
            Intent intent = new Intent(this, ChatHostActivity.class);
            intent.putExtra(ChatFragment.ARG_CONTACT_UID, clientUid);
            intent.putExtra(ChatFragment.ARG_CONTACT_NAME, "Client");
            intent.putExtra(ChatFragment.ARG_CONTACT_ROLE, "client");
            startActivity(intent);
        });

        btnReject.setOnClickListener(v -> {
            if (TextUtils.isEmpty(currentMatchId)) return;
            new AlertDialog.Builder(this)
                    .setTitle("Reject Offer")
                    .setMessage("Are you sure you want to reject this job offer?")
                    .setPositiveButton("Reject", (dialog, which) -> {
                        db.collection("matches").document(currentMatchId).update("status", "REJECTED");
                        db.collection("matching_requests").document(currentMatchId).update("status", "REJECTED");
                        Toast.makeText(this, "Job offer rejected", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnAccept.setOnClickListener(v -> {
            if (TextUtils.isEmpty(currentMatchId)) return;
            new AlertDialog.Builder(this)
                    .setTitle("Accept Offer")
                    .setMessage("Are you sure you want to accept this job offer? You will start traveling to client location.")
                    .setPositiveButton("Accept", (dialog, which) -> {
                        db.collection("matches").document(currentMatchId).update("status", "ACCEPTED");
                        db.collection("matching_requests").document(currentMatchId).update("status", "ACCEPTED");
                        
                        Toast.makeText(this, "Job offer accepted!", Toast.LENGTH_SHORT).show();
                        
                        Intent intent = new Intent(this, AssignedJobMapActivity.class);
                        intent.putExtra("matchId", currentMatchId);
                        intent.putExtra("role", "worker");
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (offersListener != null) {
            offersListener.remove();
            offersListener = null;
        }
    }
}
