package com.example.woil.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import android.text.TextUtils;
import java.util.Locale;

public class ClientHomeFragment extends Fragment {
    private RecyclerView rvClientPostedJobs;
    private RecyclerView rvClientMatching;
    private MaterialButton btnAddJobClientHome, btnViewApplications;
    private TextView tvMatchingSummary;
    private ListenerRegistration activeMatchListener;

    public ClientHomeFragment() {}
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.client_fragment_home, container, false);
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        rvClientPostedJobs = view.findViewById(R.id.rvClientPostedJobs);
        rvClientMatching = view.findViewById(R.id.rvClientMatching);
        btnAddJobClientHome = view.findViewById(R.id.btn_add_job_client_home);
        btnViewApplications = view.findViewById(R.id.btn_view_applications);
        tvMatchingSummary = view.findViewById(R.id.tvMatchingSummary);
        rvClientPostedJobs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvClientMatching.setLayoutManager(new LinearLayoutManager(requireContext()));
        btnAddJobClientHome.setOnClickListener(v -> {
            try { startActivity(new Intent(requireContext(), PostJobActivity.class)); }
            catch (Exception e) { Toast.makeText(requireContext(), "Can't open Post Job screen", Toast.LENGTH_SHORT).show(); }
        });
        btnViewApplications.setOnClickListener(v -> startActivity(new Intent(requireContext(), ClientJobApplicationsActivity.class)));
        String uid = FirebaseAuth.getInstance().getCurrentUser()!=null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("matches").whereEqualTo("clientUid", uid).get().addOnSuccessListener(snap -> {
                int total = snap.size(); int pending = 0;
                for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                    String s = d.getString("status");
                    if (s == null || "PENDING".equalsIgnoreCase(s) || "VIEWED".equalsIgnoreCase(s)) pending++;
                }
                tvMatchingSummary.setText(pending + " pending applications • " + total + " total");
            });
            setupActiveMatchListener(view, uid);
        }
    }

    private void setupActiveMatchListener(View view, String clientUid) {
        android.util.Log.d("ClientHomeFrag", "setupActiveMatchListener called for clientUid: " + clientUid);
        activeMatchListener = FirebaseFirestore.getInstance().collection("matches")
                .whereEqualTo("clientUid", clientUid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        android.util.Log.e("ClientHomeFrag", "Active match listener error: ", e);
                        return;
                    }
                    if (snap == null) {
                        android.util.Log.d("ClientHomeFrag", "Snapshot is null");
                        return;
                    }

                    android.util.Log.d("ClientHomeFrag", "Snapshot size: " + snap.size());
                    DocumentSnapshot activeMatch = null;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String s = d.getString("status");
                        android.util.Log.d("ClientHomeFrag", "Match doc ID: " + d.getId() + ", status: " + s);
                        if ("ACCEPTED".equalsIgnoreCase(s) || "TRAVELING".equalsIgnoreCase(s) || "IN_PROGRESS".equalsIgnoreCase(s) || "STARTED".equalsIgnoreCase(s) || "ARRIVED".equalsIgnoreCase(s)) {
                            activeMatch = d;
                            break;
                        }
                    }

                    View cardArrival = view.findViewById(R.id.card_active_arrival);
                    if (cardArrival != null) {
                        if (activeMatch != null) {
                            android.util.Log.d("ClientHomeFrag", "Showing active match card for doc: " + activeMatch.getId());
                            cardArrival.setVisibility(View.VISIBLE);
                            TextView tvTitle = cardArrival.findViewById(R.id.tv_arrival_title);
                            TextView tvDetails = cardArrival.findViewById(R.id.tv_arrival_details);
                            com.google.android.material.button.MaterialButton btnTrack = cardArrival.findViewById(R.id.btn_track_map);

                            String status = activeMatch.getString("status");
                            if ("IN_PROGRESS".equalsIgnoreCase(status) || "STARTED".equalsIgnoreCase(status)) {
                                tvTitle.setText("Work In Progress");
                            } else {
                                tvTitle.setText("Worker Arriving");
                            }

                            String workerName = activeMatch.getString("workerName");
                            if (TextUtils.isEmpty(workerName)) workerName = "Worker";
                            Long eta = activeMatch.getLong("etaMinutes");
                            Double dist = activeMatch.getDouble("distanceKm");
                            String etaText = eta != null ? eta + " min" : "calculating...";
                            String distText = dist != null ? String.format(Locale.getDefault(), "%.1f km", dist) : "calculating...";
                            tvDetails.setText(workerName + " • ETA: " + etaText + " • Distance: " + distText);

                            String finalMatchId = activeMatch.getId();
                            btnTrack.setOnClickListener(v -> {
                                Context ctx = view.getContext();
                                if (ctx != null) {
                                    Intent intent = new Intent(ctx, AssignedJobMapActivity.class);
                                    intent.putExtra("matchId", finalMatchId);
                                    intent.putExtra("role", "client");
                                    ctx.startActivity(intent);
                                }
                            });
                        } else {
                            android.util.Log.d("ClientHomeFrag", "No active match found, hiding card");
                            cardArrival.setVisibility(View.GONE);
                        }
                    } else {
                        android.util.Log.d("ClientHomeFrag", "card_active_arrival view not found");
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (activeMatchListener != null) {
            activeMatchListener.remove();
            activeMatchListener = null;
        }
    }
}