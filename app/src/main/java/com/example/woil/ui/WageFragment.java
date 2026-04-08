package com.example.woil.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WageFragment extends Fragment {

    private Spinner spCategory;
    private Chip chipFastCleaning, chipLaundry, chipNearby, chipOther;
    private EditText etOtherTask, etDistance, etDurationHours, etHumanHours, etBreakMinutes, etTips, etMaterials;

    private TextView tvName, tvRoleLocation, tvRating;
    private TextView tvEarned, tvPending, tvTarget, tvProgressHint;
    private ProgressBar progressTarget;

    private TextView tvMarketRange, tvMarketMedian, tvMarketStatus;
    private TextView tvBaseAmount, tvExtraAmount, tvTravelAmount, tvTipAmount, tvMaterialAmount, tvTotalWage;

    private MaterialButton btnCalculate, btnSendOffer, btnSaveEstimate;
    private ImageButton btnBack;
    private MaterialCardView cardProgress, cardHistory;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String activeRole = "worker"; // change from Firestore later if needed

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
        setupButtons();
        loadStaticDemoData();
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

        tvName = view.findViewById(R.id.tv_name);
        tvRoleLocation = view.findViewById(R.id.tv_role_location);
        tvRating = view.findViewById(R.id.tv_rating);

        tvEarned = view.findViewById(R.id.tv_earned);
        tvPending = view.findViewById(R.id.tv_pending);
        tvTarget = view.findViewById(R.id.tv_target);
        tvProgressHint = view.findViewById(R.id.tv_progress_hint);
        progressTarget = view.findViewById(R.id.progress_target);

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
        btnSaveEstimate = view.findViewById(R.id.btn_save_estimate);

        cardProgress = view.findViewById(R.id.card_progress);
        cardHistory = view.findViewById(R.id.card_history);
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

    private void setupButtons() {
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        btnCalculate.setOnClickListener(v -> calculateWage());

        btnSaveEstimate.setOnClickListener(v -> saveEstimateToFirestore());

        btnSendOffer.setOnClickListener(v -> {
            calculateWage();
            saveOfferToFirestore();
        });
    }

    private void loadStaticDemoData() {
        tvName.setText("Kavitha Dissanayake");
        tvRoleLocation.setText("Worker • Colombo 5");
        tvRating.setText("Rating 4.8");

        tvEarned.setText("Rs. 18,400");
        tvPending.setText("Rs. 3,200");
        tvTarget.setText("Rs. 25,000");
        progressTarget.setProgress(74);
        tvProgressHint.setText("You are close to your target");
    }

    private void applyRoleUi() {
        if ("client".equalsIgnoreCase(activeRole)) {
            cardProgress.setVisibility(View.GONE);
            cardHistory.setVisibility(View.GONE);
            btnSendOffer.setText("Send offer");
            btnSaveEstimate.setText("Save draft");
        } else {
            cardProgress.setVisibility(View.VISIBLE);
            cardHistory.setVisibility(View.VISIBLE);
            btnSendOffer.setText("Send quotation");
            btnSaveEstimate.setText("Save estimate");
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

        tvMarketRange.setText(
                "Market range: " + formatRs(result.marketMin) + " - " + formatRs(result.marketMax)
        );
        tvMarketMedian.setText("Suggested fair wage: " + formatRs(result.marketMedian));
        tvMarketStatus.setText(result.marketStatus);

        if ("BELOW RANGE".equals(result.marketStatus)) {
            tvMarketStatus.setBackgroundResource(R.drawable.bg_badge_below_range);
            tvMarketStatus.setTextColor(0xFF9A6700);
        } else if ("ABOVE RANGE".equals(result.marketStatus)) {
            tvMarketStatus.setBackgroundResource(R.drawable.bg_badge_above_range);
            tvMarketStatus.setTextColor(0xFFC62828);
        } else {
            tvMarketStatus.setBackgroundResource(R.drawable.bg_badge_within_range);
            tvMarketStatus.setTextColor(0xFF1B7F45);
        }
    }

    private void saveEstimateToFirestore() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = buildQuoteMap();
        data.put("status", "DRAFT");

        db.collection("calculator_quotes")
                .add(data)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(requireContext(), "Estimate saved", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
        data.put("totalText", tvTotalWage.getText().toString());
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
}