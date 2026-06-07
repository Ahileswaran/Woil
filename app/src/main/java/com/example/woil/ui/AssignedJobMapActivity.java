package com.example.woil.ui;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AssignedJobMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView tvEta, tvDistance;
    private FirebaseFirestore db;
    private String matchId;
    private String role; // "worker" or "client"
    private ListenerRegistration matchListener;
    private GoogleMap mMap;
    private LatLng clientLatLng;
    private LatLng workerLatLng;
    private String workerName;

    private TextView tvWorkTimer;
    private MaterialButton btnStartWork;
    private MaterialButton btnEndWork;
    private TextView tvStatusDisplay;
    private MaterialButton btnArrived;
    private MaterialButton btnCancelJob;

    private long startTimeMillis = 0;
    private boolean isTimerRunning = false;
    private final Handler timerHandler = new Handler();
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (startTimeMillis > 0 && isTimerRunning) {
                long seconds = (System.currentTimeMillis() - startTimeMillis) / 1000;
                long minutes = seconds / 60;
                long hours = minutes / 60;
                seconds = seconds % 60;
                minutes = minutes % 60;

                String timeStr;
                if (hours > 0) {
                    timeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
                } else {
                    timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                }
                tvWorkTimer.setText("Work Time: " + timeStr);
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assigned_job_map);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        db = FirebaseFirestore.getInstance();
        matchId = getIntent().getStringExtra("matchId");
        role = getIntent().getStringExtra("role");

        tvEta = findViewById(R.id.tv_eta);
        tvDistance = findViewById(R.id.tv_distance);

        tvWorkTimer = findViewById(R.id.tv_work_timer);
        btnStartWork = findViewById(R.id.btn_start_work);
        btnEndWork = findViewById(R.id.btn_end_work);
        tvStatusDisplay = findViewById(R.id.tv_status_display);
        btnArrived = findViewById(R.id.btn_arrived);
        btnCancelJob = findViewById(R.id.btn_cancel_job);

        btnArrived.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Worker Arrival")
                    .setMessage("Confirm that you have arrived at the client's location?")
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", "ARRIVED");
                        updates.put("arrivedAt", System.currentTimeMillis());
                        db.collection("matches").document(matchId).set(updates, SetOptions.merge());
                        db.collection("matching_requests").document(matchId).set(updates, SetOptions.merge());
                        Toast.makeText(this, "Arrival confirmed", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnCancelJob.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Cancel Job")
                    .setMessage("Are you sure you want to cancel this job route?")
                    .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", "CANCELLED");
                        updates.put("cancelledAt", System.currentTimeMillis());
                        db.collection("matches").document(matchId).set(updates, SetOptions.merge());
                        db.collection("matching_requests").document(matchId).set(updates, SetOptions.merge());
                        Toast.makeText(this, "Job cancelled", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        tvEta.setText("ETA: calculating...");
        tvDistance.setText("Distance: calculating...");

        btnStartWork.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Start Work")
                    .setMessage("Confirm starting the job timer now?")
                    .setPositiveButton("Start", (dialog, which) -> {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", "IN_PROGRESS");
                        updates.put("startTime", System.currentTimeMillis());
                        db.collection("matches").document(matchId).set(updates, SetOptions.merge());
                        db.collection("matching_requests").document(matchId).set(updates, SetOptions.merge());
                        Toast.makeText(this, "Job started", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnEndWork.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("End Work")
                    .setMessage("Confirm completion of work?")
                    .setPositiveButton("End", (dialog, which) -> {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", "COMPLETED");
                        updates.put("endTime", System.currentTimeMillis());
                        db.collection("matches").document(matchId).set(updates, SetOptions.merge());
                        db.collection("matching_requests").document(matchId).set(updates, SetOptions.merge());
                        Toast.makeText(this, "Job completed", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        listenToMatch();
    }

    private void listenToMatch() {
        if (TextUtils.isEmpty(matchId)) return;

        matchListener = db.collection("matches").document(matchId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists()) return;

                    String status = snap.getString("status");
                    workerName = snap.getString("workerName");

                    Map<String, Object> clientLoc = (Map<String, Object>) snap.get("clientLocation");
                    if (clientLoc != null) {
                        Object latObj = clientLoc.get("lat");
                        Object lngObj = clientLoc.get("lng");
                        if (latObj instanceof Number && lngObj instanceof Number) {
                            clientLatLng = new LatLng(((Number) latObj).doubleValue(), ((Number) lngObj).doubleValue());
                        }
                    }

                    if (clientLatLng != null && workerLatLng == null) {
                        workerLatLng = new LatLng(clientLatLng.latitude - 0.008, clientLatLng.longitude - 0.01);
                    }

                    Long eta = snap.getLong("etaMinutes");
                    Double dist = snap.getDouble("distanceKm");
                    if (eta != null) tvEta.setText("ETA: " + eta + " min");
                    if (dist != null) tvDistance.setText("Distance: " + String.format(Locale.getDefault(), "%.1f km", dist));

                    updateMapMarkers();
                    updateControlUi(status, snap);
                });
    }

    private void updateControlUi(String status, DocumentSnapshot snap) {
        btnArrived.setVisibility(View.GONE);
        btnCancelJob.setVisibility(View.GONE);

        if ("CANCELLED".equalsIgnoreCase(status)) {
            tvStatusDisplay.setText("Job has been cancelled");
            btnStartWork.setVisibility(View.GONE);
            btnEndWork.setVisibility(View.GONE);
            tvWorkTimer.setVisibility(View.GONE);
            stopTimer();
            new AlertDialog.Builder(this)
                    .setTitle("Job Cancelled")
                    .setMessage("This matching request has been cancelled.")
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .show();
            return;
        }

        if ("client".equalsIgnoreCase(role)) {
            if ("ACCEPTED".equalsIgnoreCase(status) || "TRAVELING".equalsIgnoreCase(status)) {
                tvStatusDisplay.setText("Worker is on the way");
                btnStartWork.setVisibility(View.VISIBLE);
                btnStartWork.setEnabled(true);
                btnEndWork.setVisibility(View.GONE);
                tvWorkTimer.setVisibility(View.GONE);
                stopTimer();
            } else if ("ARRIVED".equalsIgnoreCase(status)) {
                tvStatusDisplay.setText("Worker has arrived at your location");
                btnStartWork.setVisibility(View.VISIBLE);
                btnStartWork.setEnabled(true);
                btnEndWork.setVisibility(View.GONE);
                tvWorkTimer.setVisibility(View.GONE);
                stopTimer();
            } else if ("IN_PROGRESS".equalsIgnoreCase(status) || "STARTED".equalsIgnoreCase(status)) {
                tvStatusDisplay.setText("Work is in progress");
                btnStartWork.setVisibility(View.GONE);
                btnEndWork.setVisibility(View.VISIBLE);
                btnEndWork.setEnabled(true);
                tvWorkTimer.setVisibility(View.VISIBLE);

                Long startTime = snap.getLong("startTime");
                if (startTime != null) {
                    startTimeMillis = startTime;
                    startTimer();
                } else {
                    startTimeMillis = System.currentTimeMillis();
                    startTimer();
                }
            } else if ("COMPLETED".equalsIgnoreCase(status)) {
                tvStatusDisplay.setText("Work completed successfully");
                btnStartWork.setVisibility(View.GONE);
                btnEndWork.setVisibility(View.GONE);
                tvWorkTimer.setVisibility(View.VISIBLE);

                Long startTime = snap.getLong("startTime");
                Long endTime = snap.getLong("endTime");
                stopTimer();
                if (startTime != null && endTime != null) {
                    long seconds = (endTime - startTime) / 1000;
                    long minutes = seconds / 60;
                    seconds = seconds % 60;
                    tvWorkTimer.setText("Total Work Time: " + String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                } else {
                    tvWorkTimer.setText("Total Work Time: completed");
                }
            }
        } else {
            btnStartWork.setVisibility(View.GONE);
            btnEndWork.setVisibility(View.GONE);

            if ("ACCEPTED".equalsIgnoreCase(status) || "TRAVELING".equalsIgnoreCase(status)) {
                tvStatusDisplay.setText("Traveling to client location...");
                tvWorkTimer.setVisibility(View.GONE);
                btnArrived.setVisibility(View.VISIBLE);
                btnCancelJob.setVisibility(View.VISIBLE);
                stopTimer();
            } else if ("ARRIVED".equalsIgnoreCase(status)) {
                tvStatusDisplay.setText("Arrived. Waiting for client to start work...");
                tvWorkTimer.setVisibility(View.GONE);
                btnArrived.setVisibility(View.GONE);
                btnCancelJob.setVisibility(View.VISIBLE);
                stopTimer();
            } else if ("IN_PROGRESS".equalsIgnoreCase(status) || "STARTED".equalsIgnoreCase(status)) {
                tvStatusDisplay.setText("Work in progress...");
                tvWorkTimer.setVisibility(View.VISIBLE);
                btnArrived.setVisibility(View.GONE);
                btnCancelJob.setVisibility(View.GONE);

                Long startTime = snap.getLong("startTime");
                if (startTime != null) {
                    startTimeMillis = startTime;
                    startTimer();
                } else {
                    startTimeMillis = System.currentTimeMillis();
                    startTimer();
                }
            } else if ("COMPLETED".equalsIgnoreCase(status)) {
                tvStatusDisplay.setText("Job completed successfully");
                tvWorkTimer.setVisibility(View.VISIBLE);
                btnArrived.setVisibility(View.GONE);
                btnCancelJob.setVisibility(View.GONE);

                Long startTime = snap.getLong("startTime");
                Long endTime = snap.getLong("endTime");
                stopTimer();
                if (startTime != null && endTime != null) {
                    long seconds = (endTime - startTime) / 1000;
                    long minutes = seconds / 60;
                    seconds = seconds % 60;
                    tvWorkTimer.setText("Total Work Time: " + String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                } else {
                    tvWorkTimer.setText("Total Work Time: completed");
                }
            }
        }
    }

    private void startTimer() {
        if (!isTimerRunning) {
            isTimerRunning = true;
            timerHandler.post(timerRunnable);
        }
    }

    private void stopTimer() {
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void updateMapMarkers() {
        if (mMap == null || clientLatLng == null) return;
        mMap.clear();

        mMap.addMarker(new MarkerOptions()
                .position(clientLatLng)
                .title("Client Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        if (workerLatLng != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(workerLatLng)
                    .title(TextUtils.isEmpty(workerName) ? "Worker" : workerName)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(clientLatLng, 13f));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(clientLatLng, 13f));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        updateMapMarkers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (matchListener != null) {
            matchListener.remove();
            matchListener = null;
        }
    }
}