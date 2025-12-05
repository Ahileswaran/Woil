package com.example.woil.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvDisplayName, tvFullName, tvRatingCount, tvRoles, tvVerificationStatus,
            tvDob, tvGender, tvLocation, tvAddress, tvAvailability, tvJoined;
    private RatingBar ratingBar;
    private ChipGroup chipGroupSkills;
    private RecyclerView rvSkillShowcase;

    // Assuming a simple adapter for skill showcase; need to create SkillShowcaseAdapter
    // For example, if map has fields like "title", "description"

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvFullName = view.findViewById(R.id.tvFullName);
        ratingBar = view.findViewById(R.id.ratingBar);
        tvRatingCount = view.findViewById(R.id.tvRatingCount);
        tvRoles = view.findViewById(R.id.tvRoles);
        tvVerificationStatus = view.findViewById(R.id.tvVerificationStatus);
        tvDob = view.findViewById(R.id.tvDob);
        tvGender = view.findViewById(R.id.tvGender);
        tvLocation = view.findViewById(R.id.tvLocation);
        tvAddress = view.findViewById(R.id.tvAddress);
        chipGroupSkills = view.findViewById(R.id.chipGroupSkills);
        tvAvailability = view.findViewById(R.id.tvAvailability);
        rvSkillShowcase = view.findViewById(R.id.rvSkillShowcase);
        tvJoined = view.findViewById(R.id.tvJoined);

        rvSkillShowcase.setLayoutManager(new LinearLayoutManager(getContext()));

        fetchProfileData();

        return view;
    }

    private void fetchProfileData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (userId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        updateUI(documentSnapshot);
                    } else {
                        Toast.makeText(getContext(), "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error fetching profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @SuppressLint("SetTextI18n")
    private void updateUI(DocumentSnapshot doc) {
        String displayName = doc.getString("displayName");
        String firstName = doc.getString("firstName");
        String lastName = doc.getString("lastName");
        Timestamp dob = doc.getTimestamp("dob");
        String gender = doc.getString("gender");
        GeoPoint location = doc.getGeoPoint("location");
        String locationText = doc.getString("locationText");
        String address = doc.getString("address");
        Boolean isWorker = doc.getBoolean("isWorker");
        Boolean isClient = doc.getBoolean("isClient");
        List<String> skills = (List<String>) doc.get("skills");
        List<Map<String, Object>> skillShowcase = (List<Map<String, Object>>) doc.get("skillShowcase");
        Double rating = doc.getDouble("rating");
        Long ratingCount = doc.getLong("ratingCount");
        String nicVerified = doc.getString("nicVerified");
        // nicImagePath - skipping display as sensitive
        Map<String, Object> availability = (Map<String, Object>) doc.get("availability");
        // fcmToken - not displaying
        Timestamp createdAt = doc.getTimestamp("createdAt");
        // updatedAt - not displaying

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

        tvDisplayName.setText(displayName != null ? displayName : "N/A");
        tvFullName.setText((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : ""));

        if (rating != null) {
            ratingBar.setRating(rating.floatValue());
        }
        tvRatingCount.setText("(" + (ratingCount != null ? ratingCount : 0) + " reviews)");

        String roles = "Roles: ";
        if (Boolean.TRUE.equals(isWorker)) roles += "Worker ";
        if (Boolean.TRUE.equals(isClient)) roles += "Client";
        tvRoles.setText(roles.trim());

        tvVerificationStatus.setText("Verification: " + (nicVerified != null ? nicVerified : "N/A"));

        tvDob.setText("Date of Birth: " + (dob != null ? dateFormat.format(dob.toDate()) : "N/A"));
        tvGender.setText("Gender: " + (gender != null ? gender : "N/A"));
        tvLocation.setText("Location: " + (locationText != null ? locationText : (location != null ? location.getLatitude() + ", " + location.getLongitude() : "N/A")));
        tvAddress.setText("Address: " + (address != null ? address : "N/A"));

        chipGroupSkills.removeAllViews();
        if (skills != null) {
            for (String skill : skills) {
                Chip chip = new Chip(requireContext());
                chip.setText(skill);
                chipGroupSkills.addView(chip);
            }
        }

        StringBuilder availBuilder = new StringBuilder();
        if (availability != null) {
            for (Map.Entry<String, Object> entry : availability.entrySet()) {
                String day = entry.getKey();
                List<String> times = (List<String>) entry.getValue();
                if (times != null && times.size() >= 2) {
                    availBuilder.append(day).append(": ").append(times.get(0)).append(" - ").append(times.get(1)).append("\n");
                }
            }
        }
        tvAvailability.setText(availBuilder.length() > 0 ? availBuilder.toString().trim() : "No availability set");

        // For skillShowcase, need a SkillShowcaseModel and Adapter
        // Example: List<SkillShowcaseModel> showcaseList = new ArrayList<>();
        // for (Map<String, Object> map : skillShowcase) {
        //     showcaseList.add(new SkillShowcaseModel(map.get("title").toString(), ...));
        // }
        // rvSkillShowcase.setAdapter(new SkillShowcaseAdapter(showcaseList));

        tvJoined.setText("Joined: " + (createdAt != null ? dateFormat.format(createdAt.toDate()) : "N/A"));
    }
}