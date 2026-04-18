package com.example.woil.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WageFragment extends Fragment {

    private Spinner spCategory;

    private Chip chipFastCleaning, chipLaundry, chipNearby, chipOther;

    private EditText etOtherTask;
    private EditText etDistance;
    private EditText etDurationHours;
    private EditText etHumanHours;
    private EditText etBreakMinutes;
    private EditText etTips;
    private EditText etMaterials;

    private ImageView ivProfile;
    private TextView tvName;
    private TextView tvVerified;
    private TextView tvRoleLocation;
    private TextView tvRating;

    private TextView tvEarned;
    private TextView tvPending;
    private TextView tvEarnedDetail;
    private TextView tvPendingDetail;
    private TextView tvTarget;
    private TextView tvProgressHint;
    private ProgressBar progressTarget;

    private TextView tvHistory1;
    private TextView tvHistory2;
    private TextView tvHistory3;

    private TextView tvMarketRange;
    private TextView tvMarketMedian;
    private TextView tvMarketStatus;

    private TextView tvBaseAmount;
    private TextView tvExtraAmount;
    private TextView tvTravelAmount;
    private TextView tvTipAmount;
    private TextView tvMaterialAmount;
    private TextView tvTotalWage;

    private ImageButton btnBack;
    private MaterialButton btnCalculate;
    private MaterialButton btnSendOffer;

    private View layoutEarningsToggle;
    private View layoutRecentToggle;
    private View layoutEarningsContent;
    private View layoutRecentContent;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ListenerRegistration userListener;
    private ListenerRegistration profileListener;

    private String activeRole = "worker";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        bindViews(view);
        setupCategorySpinner();
        setupChips();
        setupExpandableSections();
        setupButtons();
        loadStaticDemoData();
        attachProfileListeners();
        applyRoleUi();
    }

    private void bindViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);

        spCategory = view.findViewById(R.id.sp_category);

        chipFastCleaning = view.findViewById(R.id.chip_fast_cleaning);
        chipLaundry = view.findViewById(R.id.chip_laundry);
        chipNearby = view.findViewById(R.id.chip_nearby);
        chipOther = view.findViewById(R.id.chip_other);

        etOtherTask = view.findViewById(R.id.et_other_task);
        etDistance = view.findViewById(R.id.et_distance);
        etDurationHours = view.findViewById(R.id.et_duration_hours);
        etHumanHours = view.findViewById(R.id.et_human_hours);
        etBreakMinutes = view.findViewById(R.id.et_break_minutes);
        etTips = view.findViewById(R.id.et_tips);
        etMaterials = view.findViewById(R.id.et_materials);

        ivProfile = view.findViewById(R.id.iv_profile);
        tvName = view.findViewById(R.id.tv_name);
        tvVerified = view.findViewById(R.id.tv_verified);
        tvRoleLocation = view.findViewById(R.id.tv_role_location);
        tvRating = view.findViewById(R.id.tv_rating);

        tvEarned = view.findViewById(R.id.tv_earned);
        tvPending = view.findViewById(R.id.tv_pending);
        tvEarnedDetail = view.findViewById(R.id.tv_earned_detail);
        tvPendingDetail = view.findViewById(R.id.tv_pending_detail);
        tvTarget = view.findViewById(R.id.tv_target);
        tvProgressHint = view.findViewById(R.id.tv_progress_hint);
        progressTarget = view.findViewById(R.id.progress_target);

        tvHistory1 = view.findViewById(R.id.tv_history_1);
        tvHistory2 = view.findViewById(R.id.tv_history_2);
        tvHistory3 = view.findViewById(R.id.tv_history_3);

        tvMarketRange = view.findViewById(R.id.tv_market_range);
        tvMarketMedian = view.findViewById(R.id.tv_market_median);
        tvMarketStatus = view.findViewById(R.id.tv_market_status);

        tvBaseAmount = view.findViewById(R.id.tv_base_amount);
        tvExtraAmount = view.findViewById(R.id.tv_extra_amount);
        tvTravelAmount = view.findViewById(R.id.tv_travel_amount);
        tvTipAmount = view.findViewById(R.id.tv_tip_amount);
        tvMaterialAmount = view.findViewById(R.id.tv_material_amount);
        tvTotalWage = view.findViewById(R.id.tv_total_wage);

        btnCalculate = view.findViewById(R.id.btn_calculate);
        btnSendOffer = view.findViewById(R.id.btn_send_offer);

        layoutEarningsToggle = view.findViewById(R.id.layout_earnings_toggle);
        layoutRecentToggle = view.findViewById(R.id.layout_recent_toggle);
        layoutEarningsContent = view.findViewById(R.id.layout_earnings_content);
        layoutRecentContent = view.findViewById(R.id.layout_recent_content);
    }

    private void setupCategorySpinner() {
        String[] categories = {
                "Cleaning",
                "Gardening",
                "Plumbing",
                "Housekeeping",
                "Laundry",
                "Caregiving",
                "Other"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);
    }

    private void setupChips() {
        chipOther.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etOtherTask.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }

    private void setupExpandableSections() {
        layoutEarningsToggle.setOnClickListener(v -> {
            if (layoutEarningsContent.getVisibility() == View.VISIBLE) {
                layoutEarningsContent.setVisibility(View.GONE);
            } else {
                layoutEarningsContent.setVisibility(View.VISIBLE);
                layoutRecentContent.setVisibility(View.GONE);
            }
        });

        layoutRecentToggle.setOnClickListener(v -> {
            if (layoutRecentContent.getVisibility() == View.VISIBLE) {
                layoutRecentContent.setVisibility(View.GONE);
            } else {
                layoutRecentContent.setVisibility(View.VISIBLE);
                layoutEarningsContent.setVisibility(View.GONE);
            }
        });
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        btnCalculate.setOnClickListener(v -> calculateWage());

        btnSendOffer.setOnClickListener(v -> {
            calculateWage();
            saveOfferToFirestore();
        });
    }

    private void loadStaticDemoData() {
        tvName.setText("—");
        tvRoleLocation.setText("Worker • —");
        tvRating.setVisibility(View.GONE);

        if (tvVerified != null) {
            tvVerified.setVisibility(View.GONE);
        }

        if (ivProfile != null) {
            ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }

        tvEarned.setText("Rs. 18,400");
        tvPending.setText("Rs. 3,200");

        tvEarnedDetail.setText("Rs. 18,400");
        tvPendingDetail.setText("Rs. 3,200");
        tvTarget.setText("Rs. 25,000");

        progressTarget.setProgress(74);
        tvProgressHint.setText("You are close to your target");

        tvHistory1.setText("Cleaning • Rs. 1,540 • Yesterday");
        tvHistory2.setText("Laundry • Rs. 980 • 2 days ago");
        tvHistory3.setText("Gardening • Rs. 2,200 • 4 days ago");

        tvMarketRange.setText("Rs. 1400 - 1800");
        tvMarketMedian.setText("Fair: Rs. 1650");
        tvMarketStatus.setText("WITHIN");

        tvBaseAmount.setText("Rs. 0");
        tvExtraAmount.setText("Rs. 0");
        tvTravelAmount.setText("Rs. 0");
        tvTipAmount.setText("Rs. 0");
        tvMaterialAmount.setText("Rs. 0");
        tvTotalWage.setText("Rs. 0");
    }

    private void attachProfileListeners() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        userListener = db.collection("users").document(uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists()) return;
                    populateFromUserSnapshot(snap);
                });

        profileListener = db.collection("profiles").document(uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists()) return;
                    populateFromProfileSnapshot(snap);
                });
    }

    private void populateFromUserSnapshot(DocumentSnapshot snap) {
        String role = snap.getString("role");
        Boolean nicVerified = snap.getBoolean("nicVerified");

        if (!TextUtils.isEmpty(role)) {
            activeRole = role;
            applyRoleUi();

            String existingLocation = extractLocationPart(tvRoleLocation.getText() != null
                    ? tvRoleLocation.getText().toString()
                    : "");

            if (TextUtils.isEmpty(existingLocation)) {
                existingLocation = "—";
            }

            tvRoleLocation.setText(capitalize(role) + " • " + existingLocation);
        }

        if (tvVerified != null) {
            tvVerified.setVisibility(Boolean.TRUE.equals(nicVerified) ? View.VISIBLE : View.GONE);
        }
    }

    private void populateFromProfileSnapshot(DocumentSnapshot snap) {
        String first = snap.getString("firstName");
        String last = snap.getString("lastName");
        String displayName = snap.getString("displayName");
        String role = snap.getString("role");
        String locationText = snap.getString("locationText");

        if (TextUtils.isEmpty(locationText)) {
            locationText = snap.getString("address");
        }

        String fullName = !TextUtils.isEmpty(displayName)
                ? displayName
                : ((safe(first) + " " + safe(last)).trim());

        if (TextUtils.isEmpty(fullName)) {
            fullName = "—";
        }

        tvName.setText(fullName);

        String roleLabel = TextUtils.isEmpty(role) ? activeRole : role;
        String locationLabel = TextUtils.isEmpty(locationText) ? "—" : locationText;
        tvRoleLocation.setText(capitalize(roleLabel) + " • " + locationLabel);

        Object ratingObj = snap.get("rating");
        if (ratingObj != null) {
            try {
                double rating = Double.parseDouble(ratingObj.toString());
                tvRating.setText("Rating " + String.format(Locale.getDefault(), "%.1f", rating));
                tvRating.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                tvRating.setVisibility(View.GONE);
            }
        } else {
            tvRating.setVisibility(View.GONE);
        }

        Boolean nicVerified = snap.getBoolean("nicVerified");
        if (tvVerified != null) {
            tvVerified.setVisibility(Boolean.TRUE.equals(nicVerified) ? View.VISIBLE : View.GONE);
        }

        String photoUrl = snap.getString("photoUrl");
        if (TextUtils.isEmpty(photoUrl)) photoUrl = snap.getString("photo");
        if (TextUtils.isEmpty(photoUrl)) photoUrl = snap.getString("avatar");
        if (TextUtils.isEmpty(photoUrl)) photoUrl = snap.getString("nicFrontUri");

        if (!TextUtils.isEmpty(photoUrl) && ivProfile != null) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(ivProfile);
        } else if (ivProfile != null) {
            ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void applyRoleUi() {
        if ("client".equalsIgnoreCase(activeRole)) {
            layoutEarningsToggle.setVisibility(View.GONE);
            layoutRecentToggle.setVisibility(View.GONE);
            layoutEarningsContent.setVisibility(View.GONE);
            layoutRecentContent.setVisibility(View.GONE);
            btnSendOffer.setText("Send offer");
        } else {
            layoutEarningsToggle.setVisibility(View.VISIBLE);
            layoutRecentToggle.setVisibility(View.VISIBLE);
            btnSendOffer.setText("Send quotation");
        }
    }

    private void calculateWage() {
        if (TextUtils.isEmpty(etDistance.getText().toString().trim())) {
            etDistance.setError("Enter distance");
            return;
        }

        if (TextUtils.isEmpty(etHumanHours.getText().toString().trim())) {
            etHumanHours.setError("Enter work hours");
            return;
        }

        WageCalculator.Input input = new WageCalculator.Input();
        input.category = spCategory.getSelectedItem().toString();
        input.fastCleaning = chipFastCleaning.isChecked();
        input.laundry = chipLaundry.isChecked();
        input.nearby = chipNearby.isChecked();
        input.otherSelected = chipOther.isChecked();

        input.distanceKm = parseDouble(etDistance.getText().toString());
        input.durationHours = parseDouble(etDurationHours.getText().toString());
        input.humanHours = parseDouble(etHumanHours.getText().toString());
        input.breakMinutes = (int) parseDouble(etBreakMinutes.getText().toString());
        input.tips = parseDouble(etTips.getText().toString());
        input.materials = parseDouble(etMaterials.getText().toString());

        WageCalculator.Result result = WageCalculator.calculate(input);
        bindResult(result);
    }

    private void bindResult(WageCalculator.Result result) {
        tvBaseAmount.setText(formatRs(result.baseAmount));
        tvExtraAmount.setText(formatRs(result.extraAmount));
        tvTravelAmount.setText(formatRs(result.travelAmount));
        tvTipAmount.setText(formatRs(result.tipAmount));
        tvMaterialAmount.setText(formatRs(result.materialAmount));
        tvTotalWage.setText(formatRs(result.totalAmount));

        tvMarketRange.setText(formatRs(result.marketMin) + " - " + formatRs(result.marketMax));
        tvMarketMedian.setText("Fair: " + formatRs(result.marketMedian));

        if ("BELOW RANGE".equals(result.marketStatus)) {
            tvMarketStatus.setText("BELOW");
            tvMarketStatus.setBackgroundResource(R.drawable.bg_badge_below_range);
            tvMarketStatus.setTextColor(0xFF9A6700);
        } else if ("ABOVE RANGE".equals(result.marketStatus)) {
            tvMarketStatus.setText("ABOVE");
            tvMarketStatus.setBackgroundResource(R.drawable.bg_badge_above_range);
            tvMarketStatus.setTextColor(0xFFC62828);
        } else {
            tvMarketStatus.setText("WITHIN");
            tvMarketStatus.setBackgroundResource(R.drawable.bg_badge_within_range);
            tvMarketStatus.setTextColor(0xFF1B7F45);
        }
    }

    private void saveOfferToFirestore() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = buildQuoteMap();
        data.put("status", "SENT");

        db.collection("calculator_quotes")
                .add(data)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(requireContext(), "Offer sent", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Send failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private Map<String, Object> buildQuoteMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("createdByUid", auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "");
        data.put("activeRole", activeRole);
        data.put("category", spCategory.getSelectedItem().toString());
        data.put("otherTaskText", etOtherTask.getText().toString().trim());
        data.put("distanceKm", parseDouble(etDistance.getText().toString()));
        data.put("durationHours", parseDouble(etDurationHours.getText().toString()));
        data.put("humanHours", parseDouble(etHumanHours.getText().toString()));
        data.put("breakMinutes", (int) parseDouble(etBreakMinutes.getText().toString()));
        data.put("tips", parseDouble(etTips.getText().toString()));
        data.put("materialsCost", parseDouble(etMaterials.getText().toString()));

        data.put("fastCleaning", chipFastCleaning.isChecked());
        data.put("laundry", chipLaundry.isChecked());
        data.put("nearby", chipNearby.isChecked());
        data.put("otherSelected", chipOther.isChecked());

        data.put("createdAt", System.currentTimeMillis());
        data.put("workerName", tvName.getText().toString());
        data.put("workerLocation", tvRoleLocation.getText().toString());

        data.put("baseAmount", tvBaseAmount.getText().toString());
        data.put("extraAmount", tvExtraAmount.getText().toString());
        data.put("travelAmount", tvTravelAmount.getText().toString());
        data.put("tipAmount", tvTipAmount.getText().toString());
        data.put("materialAmount", tvMaterialAmount.getText().toString());
        data.put("totalText", tvTotalWage.getText().toString());

        data.put("marketRange", tvMarketRange.getText().toString());
        data.put("marketMedian", tvMarketMedian.getText().toString());
        data.put("marketStatus", tvMarketStatus.getText().toString());

        return data;
    }

    private double parseDouble(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return 0;
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatRs(double value) {
        return String.format(Locale.getDefault(), "Rs. %.0f", value);
    }

    private String capitalize(String s) {
        if (TextUtils.isEmpty(s)) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String extractLocationPart(String roleLocationText) {
        if (TextUtils.isEmpty(roleLocationText)) return "";
        String[] parts = roleLocationText.split("•", 2);
        if (parts.length < 2) return "";
        return parts[1].trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }

        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
    }
}