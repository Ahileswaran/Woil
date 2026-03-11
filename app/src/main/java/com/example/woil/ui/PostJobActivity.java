package com.example.woil.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.woil.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PostJobActivity extends AppCompatActivity {

    private static final int REQ_PICK_LOCATION = 1001;

    private MaterialButton btnPostJob;
    private ImageButton btnBack;
    private android.widget.EditText etDescription;
    private android.widget.EditText etTitle, etCategory, etWageSuggested;
    private TextView tvSetLocation;
    private ImageView imgMap;
    private TextView tvStartAt, tvEndAt; // optional (if you add pickers)

    // selected location
    private String selectedAddress = "";
    private Double selectedLat = null;
    private Double selectedLng = null;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_job);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnPostJob = findViewById(R.id.btn_post_job);
        btnBack = findViewById(R.id.btn_back);
        etDescription = findViewById(R.id.et_description);
       // etTitle = findViewById(R.id.et_title);             // may be null if not added to layout
       // etCategory = findViewById(R.id.et_category);       // may be null
       // etWageSuggested = findViewById(R.id.et_wage_suggested); // may be null
        tvSetLocation = findViewById(R.id.tv_set_location);
        imgMap = findViewById(R.id.img_map);
      //  tvStartAt = findViewById(R.id.tv_start_at);        // optional
      //  tvEndAt = findViewById(R.id.tv_end_at);            // optional

        btnBack.setOnClickListener(v -> finish());

        android.view.View.OnClickListener openMapPicker = v -> {
            Intent i = new Intent(PostJobActivity.this, MapPickerActivity.class);
            startActivityForResult(i, REQ_PICK_LOCATION);
        };

        tvSetLocation.setOnClickListener(openMapPicker);
        imgMap.setOnClickListener(openMapPicker);

        btnPostJob.setOnClickListener(v -> {
            String description = etDescription != null ? etDescription.getText().toString().trim() : "";
            if (TextUtils.isEmpty(description)) {
                Toast.makeText(this, "Please enter a job description", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedLat == null || selectedLng == null || TextUtils.isEmpty(selectedAddress)) {
                Toast.makeText(this, "Please set a location", Toast.LENGTH_SHORT).show();
                return;
            }

            postJobToFirestore();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_LOCATION && resultCode == Activity.RESULT_OK && data != null) {
            selectedAddress = data.getStringExtra("address");
            selectedLat = data.getDoubleExtra("lat", 0.0);
            selectedLng = data.getDoubleExtra("lng", 0.0);

            tvSetLocation.setText(selectedAddress != null ? selectedAddress : "Selected location");
            // Optionally set an image in imgMap using the Static Maps API (not included here).
        }
    }

    private void postJobToFirestore() {
        String uid = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : null;

        Map<String, Object> job = new HashMap<>();
        // required/important fields per schema:
        job.put("clientUid", uid); // client uid (schema: clientUid). :contentReference[oaicite:6]{index=6}

        // optional UI fields (if present)
        String title = etTitle != null ? etTitle.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(title)) job.put("title", title); // schema: title. :contentReference[oaicite:7]{index=7}

        String category = etCategory != null ? etCategory.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(category)) job.put("category", category); // schema: category. :contentReference[oaicite:8]{index=8}

        job.put("locationText", selectedAddress); // schema: locationText. :contentReference[oaicite:9]{index=9}

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("lat", selectedLat);
        locationMap.put("lng", selectedLng);
        job.put("location", locationMap); // schema: location is a map with lat/lng. :contentReference[oaicite:10]{index=10}

        // startAt / endAt (optional) — here we leave null if tvStartAt/tvEndAt not present or empty.
        String startAtStr = tvStartAt != null ? tvStartAt.getText().toString().trim() : "";
        String endAtStr = tvEndAt != null ? tvEndAt.getText().toString().trim() : "";
        // If you parse start/end into Timestamps, convert and put them as Timestamp objects:
        // job.put("startAt", Timestamp.of(...));
        // job.put("endAt", Timestamp.of(...));

        // wageSuggested numeric
        if (etWageSuggested != null) {
            String wageS = etWageSuggested.getText().toString().trim();
            if (!TextUtils.isEmpty(wageS)) {
                try {
                    double wage = Double.parseDouble(wageS);
                    job.put("wageSuggested", wage); // schema: wageSuggested. :contentReference[oaicite:11]{index=11}
                } catch (NumberFormatException ignored) { }
            }
        }

        job.put("description", etDescription.getText().toString().trim());
        job.put("assignedUid", null); // not assigned yet
        job.put("status", "OPEN"); // schema examples show "OPEN" / "MATCHED" etc. :contentReference[oaicite:12]{index=12}
        job.put("createdAt", Timestamp.now());

        // Write to Firestore jobs collection
        db.collection("jobs")
                .add(job)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(PostJobActivity.this, "Job posted (id: " + docRef.getId() + ")", Toast.LENGTH_SHORT).show();
                    Intent result = new Intent();
                    result.putExtra("jobId", docRef.getId());
                    setResult(Activity.RESULT_OK, result);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PostJobActivity.this, "Failed to post job: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}