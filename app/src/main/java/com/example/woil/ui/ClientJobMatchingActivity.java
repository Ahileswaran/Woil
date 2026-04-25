package com.example.woil.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.CategoryModel;
import com.example.woil.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ClientJobMatchingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "ClientMatching";
    private static final int REQ_PICK_LOCATION = 4101;

    private ImageButton btnBack;
    private ImageButton btnPickLocation;
    private TextView tvSelectedLocation;
    private LinearLayout layoutCategories;
    private EditText etOtherCategory;
    private RecyclerView rvMatchingWorkers;
    private RecyclerView rvScheduleWorkers;
    private TextView tvScheduleTitle;
    private MaterialButton btnMatchNow;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private GoogleMap mMap;

    private MatchingWorkerAdapter workerAdapter;
    private MatchingScheduleAdapter scheduleAdapter;

    private final List<CategoryModel> categoryList = new ArrayList<>();
    private String selectedCategory = "Cleaning";
    private String selectedAddress = "";
    private LatLng selectedLatLng = null;

    private final List<MatchingWorkerModel> nearbyWorkers = new ArrayList<>();
    private final List<MatchingWorkerModel> scheduleWorkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_job_matching);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        btnBack = findViewById(R.id.btn_back_arrow_settings);
        btnPickLocation = findViewById(R.id.btn_pick_location);
        tvSelectedLocation = findViewById(R.id.tv_selected_location);
        layoutCategories = findViewById(R.id.layout_categories);
        etOtherCategory = findViewById(R.id.et_other_category);
        rvMatchingWorkers = findViewById(R.id.rv_matching_workers);
        rvScheduleWorkers = findViewById(R.id.rv_schedule_workers);
        tvScheduleTitle = findViewById(R.id.tv_schedule_title);
        btnMatchNow = findViewById(R.id.btn_match_now);

        workerAdapter = new MatchingWorkerAdapter(worker -> {
            Intent i = new Intent(this, ClientWorkerSelectionActivity.class);
            i.putExtra("workerUid", worker.uid);
            i.putExtra("workerName", worker.name);
            i.putExtra("workerSkill", worker.skill);
            i.putExtra("workerRating", worker.rating);
            i.putExtra("workerDistanceKm", worker.distanceKm);
            i.putExtra("workerEtaMinutes", worker.etaMinutes);
            i.putExtra("clientAddress", selectedAddress);
            i.putExtra("selectedCategory", getEffectiveCategory());
            startActivity(i);
        });

        scheduleAdapter = new MatchingScheduleAdapter();

        rvMatchingWorkers.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        rvScheduleWorkers.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        rvMatchingWorkers.setAdapter(workerAdapter);
        rvScheduleWorkers.setAdapter(scheduleAdapter);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupCategories();
        renderCategoryButtons();

        btnBack.setOnClickListener(v -> finish());
        btnPickLocation.setOnClickListener(v -> openMapPicker());

        tvSelectedLocation.setOnClickListener(v -> openMapPicker());

        btnMatchNow.setOnClickListener(v -> runMatching());

        loadClientDefaultLocation();
    }

    private void setupCategories() {
        categoryList.clear();
        categoryList.add(new CategoryModel("Cleaning", R.drawable.cleaning));
        categoryList.add(new CategoryModel("Electrician", R.drawable.electic));
        categoryList.add(new CategoryModel("Caregiver", R.drawable.caregiver));
        categoryList.add(new CategoryModel("Appliance Repair", R.drawable.repair));
        categoryList.add(new CategoryModel("Masonry", R.drawable.masanory));
        categoryList.add(new CategoryModel("Laundry", R.drawable.landury));
        categoryList.add(new CategoryModel("Painting", R.drawable.painting));
        categoryList.add(new CategoryModel("Plumbing", R.drawable.plumbing));
        categoryList.add(new CategoryModel("Gardening", R.drawable.gardining));
        categoryList.add(new CategoryModel("Carpentry", R.drawable.carpentary));
    }

    private void renderCategoryButtons() {
        layoutCategories.removeAllViews();

        for (CategoryModel category : categoryList) {
            TextView chip = new TextView(this);

            String categoryName = category.title != null ? category.title : "";

            chip.setText(categoryName);
            chip.setTextSize(14f);
            chip.setPadding(28, 16, 28, 16);
            chip.setGravity(Gravity.CENTER);
            chip.setBackgroundResource(
                    categoryName.equalsIgnoreCase(selectedCategory)
                            ? R.drawable.bg_chip_soft_orange
                            : R.drawable.bg_field
            );
            chip.setTextColor(
                    categoryName.equalsIgnoreCase(selectedCategory)
                            ? Color.parseColor("#E67E00")
                            : Color.parseColor("#424242")
            );

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd(16);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                selectedCategory = categoryName;
                renderCategoryButtons();
            });

            layoutCategories.addView(chip);
        }
    }

    private void loadClientDefaultLocation() {
        String uid = FirebaseDebugLogger.requireUid(this, mAuth, "client_location_read");
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    FirebaseDebugLogger.read("client_location_read", "users/" + uid, doc.exists() ? 1 : 0);
                    if (!doc.exists()) return;

                    Object locationObj = doc.get("location");
                    Object locationTextObj = doc.get("locationText");

                    if (locationTextObj != null) {
                        selectedAddress = String.valueOf(locationTextObj);
                        tvSelectedLocation.setText(selectedAddress);
                    }

                    if (locationObj instanceof java.util.Map) {
                        java.util.Map<?, ?> map = (java.util.Map<?, ?>) locationObj;
                        Object latObj = map.get("lat");
                        Object lngObj = map.get("lng");

                        if (latObj instanceof Number && lngObj instanceof Number) {
                            selectedLatLng = new LatLng(
                                    ((Number) latObj).doubleValue(),
                                    ((Number) lngObj).doubleValue()
                            );
                            updateMapForClientLocation();
                        }
                    }
                })
                .addOnFailureListener(e -> FirebaseDebugLogger.failure("client_location_read", "users/" + uid, e));
    }

    private void openMapPicker() {
        Intent i = new Intent(this, MapPickerActivity.class);
        startActivityForResult(i, REQ_PICK_LOCATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PICK_LOCATION && resultCode == Activity.RESULT_OK && data != null) {
            double lat = data.getDoubleExtra("lat", 0.0);
            double lng = data.getDoubleExtra("lng", 0.0);
            selectedAddress = data.getStringExtra("address");

            selectedLatLng = new LatLng(lat, lng);
            tvSelectedLocation.setText(!TextUtils.isEmpty(selectedAddress) ? selectedAddress : lat + ", " + lng);
            updateMapForClientLocation();
        }
    }

    private String getEffectiveCategory() {
        String manual = etOtherCategory.getText().toString().trim();
        if (!TextUtils.isEmpty(manual)) return manual;
        return selectedCategory;
    }

    private void runMatching() {
        if (selectedLatLng == null) {
            Toast.makeText(this, "Please select client location first", Toast.LENGTH_SHORT).show();
            return;
        }

        String effectiveCategory = getEffectiveCategory();
        if (TextUtils.isEmpty(effectiveCategory)) {
            Toast.makeText(this, "Please choose a category", Toast.LENGTH_SHORT).show();
            return;
        }

        nearbyWorkers.clear();
        scheduleWorkers.clear();

        db.collection("users")
                .whereEqualTo("role", "worker")
                .get()
                .addOnSuccessListener(snap -> {
                    FirebaseDebugLogger.read("worker_matching_query", "users?role=worker", snap.size());
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        MatchingWorkerModel worker = parseWorker(doc, effectiveCategory);
                        if (worker == null) continue;

                        if (worker.distanceKm <= 10.0) {
                            nearbyWorkers.add(worker);
                        } else {
                            scheduleWorkers.add(worker);
                        }
                    }

                    sortWorkers(nearbyWorkers);
                    sortWorkers(scheduleWorkers);

                    workerAdapter.submitList(nearbyWorkers);

                    if (nearbyWorkers.isEmpty()) {
                        tvScheduleTitle.setVisibility(View.VISIBLE);
                        rvScheduleWorkers.setVisibility(View.VISIBLE);
                        scheduleAdapter.submitList(scheduleWorkers);
                        Toast.makeText(this, "No nearby workers found. Showing schedule fallback.", Toast.LENGTH_SHORT).show();
                    } else {
                        tvScheduleTitle.setVisibility(scheduleWorkers.isEmpty() ? View.GONE : View.VISIBLE);
                        rvScheduleWorkers.setVisibility(scheduleWorkers.isEmpty() ? View.GONE : View.VISIBLE);
                        scheduleAdapter.submitList(scheduleWorkers);
                    }

                    updateMapWorkerMarkers();
                })
                .addOnFailureListener(e -> {
                    FirebaseDebugLogger.failure("worker_matching_query", "users?role=worker", e);
                    Log.e(TAG, "Matching failed", e);
                    Toast.makeText(this, "Failed to load workers: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private MatchingWorkerModel parseWorker(DocumentSnapshot doc, String effectiveCategory) {
        String uid = doc.getId();
        String role = doc.getString("role");
        if (!"worker".equalsIgnoreCase(role)) return null;

        String skill = doc.getString("skill");
        if (TextUtils.isEmpty(skill)) skill = doc.getString("displaySkill");

        if (!TextUtils.isEmpty(skill) && !skill.equalsIgnoreCase(effectiveCategory)) {
            return null;
        }

        Object locationObj = doc.get("location");
        if (!(locationObj instanceof java.util.Map)) return null;

        java.util.Map<?, ?> loc = (java.util.Map<?, ?>) locationObj;
        Object latObj = loc.get("lat");
        Object lngObj = loc.get("lng");
        if (!(latObj instanceof Number) || !(lngObj instanceof Number)) return null;

        double lat = ((Number) latObj).doubleValue();
        double lng = ((Number) lngObj).doubleValue();

        float[] results = new float[1];
        Location.distanceBetween(
                selectedLatLng.latitude, selectedLatLng.longitude,
                lat, lng,
                results
        );

        double distanceKm = results[0] / 1000.0;
        long etaMinutes = Math.max(1, Math.round(distanceKm * 4.0)); // simple placeholder estimate

        MatchingWorkerModel worker = new MatchingWorkerModel();
        worker.uid = uid;
        worker.name = buildName(doc);
        worker.skill = !TextUtils.isEmpty(skill) ? skill : effectiveCategory;
        worker.photoUrl = firstNonEmpty(
                doc.getString("photoUrl"),
                doc.getString("photo"),
                doc.getString("avatar"),
                doc.getString("nicImageUrl")
        );
        worker.rating = readDouble(doc, "rating", 0.0);
        worker.lat = lat;
        worker.lng = lng;
        worker.distanceKm = distanceKm;
        worker.etaMinutes = etaMinutes;
        worker.available = true;

        return worker;
    }

    private void sortWorkers(List<MatchingWorkerModel> list) {
        Collections.sort(list, new Comparator<MatchingWorkerModel>() {
            @Override
            public int compare(MatchingWorkerModel a, MatchingWorkerModel b) {
                int etaCompare = Long.compare(a.etaMinutes, b.etaMinutes);
                if (etaCompare != 0) return etaCompare;
                int distanceCompare = Double.compare(a.distanceKm, b.distanceKm);
                if (distanceCompare != 0) return distanceCompare;
                return Double.compare(b.rating, a.rating);
            }
        });
    }

    private String buildName(DocumentSnapshot doc) {
        String displayName = doc.getString("displayName");
        if (!TextUtils.isEmpty(displayName)) return displayName;

        String first = doc.getString("firstName");
        String last = doc.getString("lastName");
        String full = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
        return !TextUtils.isEmpty(full) ? full : "Worker";
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return null;
        for (String s : values) {
            if (!TextUtils.isEmpty(s)) return s;
        }
        return null;
    }

    private double readDouble(DocumentSnapshot doc, String key, double fallback) {
        Object value = doc.get(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return value != null ? Double.parseDouble(String.valueOf(value)) : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private void updateMapForClientLocation() {
        if (mMap == null || selectedLatLng == null) return;

        mMap.clear();
        mMap.addMarker(new MarkerOptions()
                .position(selectedLatLng)
                .title("Client location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 13f));
    }

    private void updateMapWorkerMarkers() {
        if (mMap == null || selectedLatLng == null) return;

        mMap.clear();

        mMap.addMarker(new MarkerOptions()
                .position(selectedLatLng)
                .title("Client location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        for (MatchingWorkerModel worker : nearbyWorkers) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(worker.lat, worker.lng))
                    .title(worker.name)
                    .snippet(String.format(Locale.getDefault(), "%s · ETA %d min", worker.skill, worker.etaMinutes))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 13f));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (selectedLatLng != null) {
            updateMapForClientLocation();
        } else {
            LatLng defaultLatLng = new LatLng(6.9271, 79.8612);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 11f));
        }
    }
}