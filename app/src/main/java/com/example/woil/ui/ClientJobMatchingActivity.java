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
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClientJobMatchingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "ClientMatching";
    private static final int REQ_PICK_LOCATION = 4101;

    // Matching coverage tiers. Main list is not limited to a small map-circle only.
    private static final double IMMEDIATE_RADIUS_KM = 10.0;
    private static final double AREA_RADIUS_KM = 25.0;
    private static final double PROVINCE_RADIUS_KM = 120.0;
    private static final double EXPANDED_RADIUS_KM = 250.0;

    private ImageButton btnBack;
    private ImageButton btnPickLocation;
    private TextView tvSelectedLocation;
    private LinearLayout layoutCategories;
    private EditText etOtherCategory;
    private RecyclerView rvMatchingWorkers;
    private RecyclerView rvScheduleWorkers;
    private TextView tvScheduleTitle;
    private MaterialButton btnFindOtherPlace;
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
    private String selectedArea = "";
    private String selectedProvince = "";

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
        btnFindOtherPlace = findViewById(R.id.btn_find_other_place);
        btnMatchNow = findViewById(R.id.btn_match_now);

        workerAdapter = new MatchingWorkerAdapter(this::openWorkerSelection);
        scheduleAdapter = new MatchingScheduleAdapter(this::openWorkerSelection);

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
        btnFindOtherPlace.setOnClickListener(v -> {
            Intent intent = new Intent(this, OtherProvinceMatchingActivity.class);
            intent.putExtra("selectedCategory", getEffectiveCategory());
            startActivity(intent);
        });
        btnMatchNow.setOnClickListener(v -> runMatching());

        loadClientDefaultLocation();
    }

    private void openWorkerSelection(MatchingWorkerModel worker) {
        Intent i = new Intent(this, ClientWorkerSelectionActivity.class);
        i.putExtra("workerUid", worker.uid);
        i.putExtra("workerName", worker.name);
        i.putExtra("workerSkill", worker.skill);
        i.putExtra("workerRating", worker.rating);
        i.putExtra("workerDistanceKm", worker.distanceKm);
        i.putExtra("workerEtaMinutes", worker.etaMinutes);
        i.putExtra("workerLocationText", worker.locationText);
        i.putExtra("workerArea", worker.area);
        i.putExtra("workerProvince", worker.province);
        i.putExtra("matchLevel", worker.matchLevel);
        i.putExtra("clientAddress", selectedAddress);
        i.putExtra("clientArea", selectedArea);
        i.putExtra("clientProvince", selectedProvince);
        i.putExtra("clientLat", selectedLatLng != null ? selectedLatLng.latitude : 0.0);
        i.putExtra("clientLng", selectedLatLng != null ? selectedLatLng.longitude : 0.0);
        i.putExtra("selectedCategory", getEffectiveCategory());
        startActivity(i);
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
                .addOnSuccessListener(userDoc -> {
                    FirebaseDebugLogger.read("client_location_read", "users/" + uid, userDoc.exists() ? 1 : 0);

                    if (applyClientLocationFromDoc(userDoc)) {
                        return;
                    }

                    db.collection("profiles").document(uid).get()
                            .addOnSuccessListener(profileDoc -> {
                                FirebaseDebugLogger.read("client_profile_location_read", "profiles/" + uid, profileDoc.exists() ? 1 : 0);
                                applyClientLocationFromDoc(profileDoc);
                            })
                            .addOnFailureListener(e -> FirebaseDebugLogger.failure("client_profile_location_read", "profiles/" + uid, e));
                })
                .addOnFailureListener(e -> FirebaseDebugLogger.failure("client_location_read", "users/" + uid, e));
    }

    private boolean applyClientLocationFromDoc(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return false;

        Object locationTextObj = firstExisting(doc, "locationText", "address");
        if (locationTextObj != null) {
            selectedAddress = String.valueOf(locationTextObj);
            selectedArea = firstNonEmpty(stringValue(doc.get("area")), extractArea(selectedAddress));
            selectedProvince = firstNonEmpty(stringValue(doc.get("province")), extractProvince(selectedAddress));
            tvSelectedLocation.setText(selectedAddress);
        }

        LatLng loc = readLatLngFromData(doc.getData());
        if (loc != null) {
            selectedLatLng = loc;
            updateMapForClientLocation();
            return true;
        }
        return false;
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
            selectedArea = extractArea(selectedAddress);
            selectedProvince = extractProvince(selectedAddress);

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
        workerAdapter.submitList(nearbyWorkers);
        scheduleAdapter.submitList(scheduleWorkers);

        Task<QuerySnapshot> usersTask = db.collection("users")
                .whereEqualTo("role", "worker")
                .get();
        Task<QuerySnapshot> profilesTask = db.collection("profiles")
                .whereEqualTo("isWorker", true)
                .get();

        Tasks.whenAllSuccess(usersTask, profilesTask)
                .addOnSuccessListener(results -> {
                    Map<String, Map<String, Object>> mergedWorkers = new HashMap<>();

                    QuerySnapshot userSnap = (QuerySnapshot) results.get(0);
                    QuerySnapshot profileSnap = (QuerySnapshot) results.get(1);

                    for (DocumentSnapshot doc : userSnap.getDocuments()) {
                        Map<String, Object> data = new HashMap<>(doc.getData() != null ? doc.getData() : Collections.emptyMap());
                        data.put("uid", doc.getId());
                        mergedWorkers.put(doc.getId(), data);
                    }

                    for (DocumentSnapshot doc : profileSnap.getDocuments()) {
                        Map<String, Object> data = mergedWorkers.containsKey(doc.getId())
                                ? mergedWorkers.get(doc.getId())
                                : new HashMap<>();
                        if (doc.getData() != null) data.putAll(doc.getData());
                        data.put("uid", doc.getId());
                        data.put("role", "worker");
                        mergedWorkers.put(doc.getId(), data);
                    }

                    for (Map<String, Object> data : mergedWorkers.values()) {
                        MatchingWorkerModel worker = parseWorker(data, effectiveCategory);
                        if (worker == null) continue;

                        // Main matching list: same area, same province, or practical radius.
                        if (isMainCoverage(worker)) {
                            nearbyWorkers.add(worker);
                        } else if (worker.distanceKm <= EXPANDED_RADIUS_KM) {
                            worker.matchLevel = "EXPANDED_SCHEDULE";
                            scheduleWorkers.add(worker);
                        }
                    }

                    sortWorkers(nearbyWorkers);
                    sortWorkers(scheduleWorkers);

                    workerAdapter.submitList(nearbyWorkers);
                    tvScheduleTitle.setVisibility(scheduleWorkers.isEmpty() ? View.GONE : View.VISIBLE);
                    rvScheduleWorkers.setVisibility(scheduleWorkers.isEmpty() ? View.GONE : View.VISIBLE);
                    scheduleAdapter.submitList(scheduleWorkers);

                    if (nearbyWorkers.isEmpty() && scheduleWorkers.isEmpty()) {
                        btnFindOtherPlace.setVisibility(View.VISIBLE);
                    } else {
                        btnFindOtherPlace.setVisibility(View.GONE);
                    }

                    FirebaseDebugLogger.read(
                            "worker_matching_query",
                            "users+profiles workers, category=" + effectiveCategory + ", area=" + selectedArea + ", province=" + selectedProvince,
                            nearbyWorkers.size() + scheduleWorkers.size()
                    );

                    if (nearbyWorkers.isEmpty() && scheduleWorkers.isEmpty()) {
                        Toast.makeText(this, "No workers found for this category/coverage", Toast.LENGTH_LONG).show();
                    } else if (nearbyWorkers.isEmpty()) {
                        Toast.makeText(this, "No province/area workers found. Showing expanded schedule fallback.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                "Matched " + nearbyWorkers.size() + " worker(s) in area/province coverage",
                                Toast.LENGTH_SHORT).show();
                    }

                    updateMapWorkerMarkers();
                })
                .addOnFailureListener(e -> {
                    FirebaseDebugLogger.failure("worker_matching_query", "users+profiles", e);
                    Log.e(TAG, "Matching failed", e);
                    Toast.makeText(this, "Failed to load workers: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean isMainCoverage(MatchingWorkerModel worker) {
        if (worker.distanceKm <= IMMEDIATE_RADIUS_KM) {
            worker.matchLevel = "IMMEDIATE_RADIUS";
            return true;
        }

        if (!TextUtils.isEmpty(selectedArea)
                && !TextUtils.isEmpty(worker.area)
                && normalize(selectedArea).equals(normalize(worker.area))) {
            worker.matchLevel = "SAME_AREA";
            return true;
        }

        if (worker.distanceKm <= AREA_RADIUS_KM) {
            worker.matchLevel = "AREA_RADIUS";
            return true;
        }

        if (!TextUtils.isEmpty(selectedProvince)
                && !TextUtils.isEmpty(worker.province)
                && normalize(selectedProvince).equals(normalize(worker.province))) {
            worker.matchLevel = "SAME_PROVINCE";
            return true;
        }

        if (worker.distanceKm <= PROVINCE_RADIUS_KM) {
            worker.matchLevel = "PROVINCE_RADIUS";
            return true;
        }

        return false;
    }

    private MatchingWorkerModel parseWorker(Map<String, Object> data, String effectiveCategory) {
        String uid = stringValue(data.get("uid"));
        String role = stringValue(data.get("role"));
        Object isWorkerObj = data.get("isWorker");
        boolean isWorker = "worker".equalsIgnoreCase(role) || Boolean.TRUE.equals(isWorkerObj);
        if (!isWorker) return null;

        if (!skillMatches(data, effectiveCategory)) return null;

        LatLng loc = readLatLngFromData(data);
        if (loc == null) return null;

        float[] results = new float[1];
        Location.distanceBetween(
                selectedLatLng.latitude, selectedLatLng.longitude,
                loc.latitude, loc.longitude,
                results
        );

        String locationText = firstNonEmpty(
                stringValue(data.get("locationText")),
                stringValue(data.get("address"))
        );
        String area = firstNonEmpty(
                stringValue(data.get("area")),
                stringValue(data.get("city")),
                extractArea(locationText)
        );
        String province = firstNonEmpty(
                stringValue(data.get("province")),
                extractProvince(locationText)
        );

        double distanceKm = results[0] / 1000.0;
        long etaMinutes = Math.max(1, Math.round(distanceKm * 4.0));

        MatchingWorkerModel worker = new MatchingWorkerModel();
        worker.uid = uid;
        worker.name = buildName(data);
        worker.skill = displaySkill(data, effectiveCategory);
        worker.photoUrl = firstNonEmpty(
                stringValue(data.get("photoUrl")),
                stringValue(data.get("photo")),
                stringValue(data.get("avatar")),
                stringValue(data.get("nicImageUrl"))
        );
        worker.rating = readDouble(data.get("rating"), 0.0);
        worker.lat = loc.latitude;
        worker.lng = loc.longitude;
        worker.distanceKm = distanceKm;
        worker.etaMinutes = etaMinutes;
        worker.available = readBoolean(data.get("available"), true);
        worker.locationText = locationText;
        worker.area = area;
        worker.province = province;
        worker.matchLevel = "UNCLASSIFIED";

        return worker;
    }

    private boolean skillMatches(Map<String, Object> data, String effectiveCategory) {
        if (TextUtils.isEmpty(effectiveCategory) || "Other".equalsIgnoreCase(effectiveCategory)) {
            return true;
        }

        String target = normalize(effectiveCategory);
        String skill = stringValue(data.get("skill"));
        String displaySkill = stringValue(data.get("displaySkill"));
        String category = stringValue(data.get("category"));

        String nSkill = normalize(skill);
        String nDisplaySkill = normalize(displaySkill);
        String nCategory = normalize(category);

        if (!TextUtils.isEmpty(nSkill) && (nSkill.contains(target) || target.contains(nSkill))) return true;
        if (!TextUtils.isEmpty(nDisplaySkill) && (nDisplaySkill.contains(target) || target.contains(nDisplaySkill))) return true;
        if (!TextUtils.isEmpty(nCategory) && (nCategory.contains(target) || target.contains(nCategory))) return true;

        Object skillsObj = data.get("skills");
        if (skillsObj instanceof List) {
            for (Object item : (List<?>) skillsObj) {
                String s = normalize(stringValue(item));
                if (!TextUtils.isEmpty(s) && (s.contains(target) || target.contains(s))) return true;
            }
        }
        return false;
    }

    private String displaySkill(Map<String, Object> data, String fallback) {
        String skill = firstNonEmpty(
                stringValue(data.get("skill")),
                stringValue(data.get("displaySkill")),
                stringValue(data.get("category"))
        );
        return !TextUtils.isEmpty(skill) ? skill : fallback;
    }

    private void sortWorkers(List<MatchingWorkerModel> list) {
        Collections.sort(list, new Comparator<MatchingWorkerModel>() {
            @Override
            public int compare(MatchingWorkerModel a, MatchingWorkerModel b) {
                int tierCompare = Integer.compare(matchTierRank(a.matchLevel), matchTierRank(b.matchLevel));
                if (tierCompare != 0) return tierCompare;
                int etaCompare = Long.compare(a.etaMinutes, b.etaMinutes);
                if (etaCompare != 0) return etaCompare;
                int distanceCompare = Double.compare(a.distanceKm, b.distanceKm);
                if (distanceCompare != 0) return distanceCompare;
                return Double.compare(b.rating, a.rating);
            }
        });
    }

    private int matchTierRank(String level) {
        if ("IMMEDIATE_RADIUS".equals(level)) return 0;
        if ("SAME_AREA".equals(level)) return 1;
        if ("AREA_RADIUS".equals(level)) return 2;
        if ("SAME_PROVINCE".equals(level)) return 3;
        if ("PROVINCE_RADIUS".equals(level)) return 4;
        if ("EXPANDED_SCHEDULE".equals(level)) return 5;
        return 9;
    }

    private String buildName(Map<String, Object> data) {
        String displayName = stringValue(data.get("displayName"));
        if (!TextUtils.isEmpty(displayName)) return displayName;

        String first = stringValue(data.get("firstName"));
        String last = stringValue(data.get("lastName"));
        String full = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
        return !TextUtils.isEmpty(full) ? full : "Worker";
    }

    private Object firstExisting(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value != null) return value;
        }
        return null;
    }

    private LatLng readLatLngFromData(Map<String, Object> data) {
        if (data == null) return null;

        LatLng fromLocationMap = readLatLng(data.get("location"));
        if (fromLocationMap != null) return fromLocationMap;

        Object latObj = firstNonNull(data.get("lat"), data.get("latitude"), data.get("selectedLatitude"));
        Object lngObj = firstNonNull(data.get("lng"), data.get("longitude"), data.get("selectedLongitude"));
        if (latObj instanceof Number && lngObj instanceof Number) {
            return new LatLng(((Number) latObj).doubleValue(), ((Number) lngObj).doubleValue());
        }
        try {
            if (latObj != null && lngObj != null) {
                return new LatLng(Double.parseDouble(String.valueOf(latObj)), Double.parseDouble(String.valueOf(lngObj)));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Object firstNonNull(Object... values) {
        if (values == null) return null;
        for (Object value : values) {
            if (value != null) return value;
        }
        return null;
    }

    private LatLng readLatLng(Object locationObj) {
        if (!(locationObj instanceof Map)) return null;
        Map<?, ?> loc = (Map<?, ?>) locationObj;
        Object latObj = loc.get("lat");
        Object lngObj = loc.get("lng");
        if (!(latObj instanceof Number) || !(lngObj instanceof Number)) return null;
        return new LatLng(((Number) latObj).doubleValue(), ((Number) lngObj).doubleValue());
    }

    private String extractProvince(String address) {
        if (TextUtils.isEmpty(address)) return "";
        String[] parts = address.split(",");
        for (String p : parts) {
            String t = p.trim();
            if (t.toLowerCase(Locale.US).contains("province")) return t;
        }
        return parts.length >= 2 ? parts[parts.length - 2].trim() : "";
    }

    private String extractArea(String address) {
        if (TextUtils.isEmpty(address)) return "";
        String[] parts = address.split(",");
        for (String p : parts) {
            String t = p.trim();
            String low = t.toLowerCase(Locale.US);
            if (TextUtils.isEmpty(t)) continue;
            if (low.contains("province") || low.equals("sri lanka")) continue;
            if (low.contains("road") || low.contains("street") || low.contains("lane")) continue;
            return t;
        }
        return parts.length > 0 ? parts[0].trim() : "";
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return null;
        for (String s : values) {
            if (!TextUtils.isEmpty(s)) return s;
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private double readDouble(Object value, double fallback) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return value != null ? Double.parseDouble(String.valueOf(value)) : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private boolean readBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        return fallback;
    }

    private void updateMapForClientLocation() {
        if (mMap == null || selectedLatLng == null) return;

        mMap.clear();
        mMap.addMarker(new MarkerOptions()
                .position(selectedLatLng)
                .title("Client location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 11f));
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
                    .snippet(String.format(Locale.getDefault(), "%s · %s · ETA %d min", worker.skill, worker.matchLevel, worker.etaMinutes))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }

        for (MatchingWorkerModel worker : scheduleWorkers) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(worker.lat, worker.lng))
                    .title(worker.name)
                    .snippet(String.format(Locale.getDefault(), "%s · Schedule fallback · %.1f km", worker.skill, worker.distanceKm))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 9.5f));
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
