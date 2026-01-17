package com.example.woil.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.example.woil.R;
import java.text.DecimalFormat;

public class ProfileFragment extends Fragment {

    private ImageView ivProfile;
    private TextView tvHandle, tvRoleLocation, tvRatingLabel, tvJobsCompleted, tvMemberSince;
    private TextView tvFullName, tvPhone, tvEmail, tvLocation;
    private Button btnEditProfile;
    private Button btnClient, btnWorker;

    // Accessibility UI
    private SwitchMaterial switchDigital, switchVoice, switchSimplified;
    private SeekBar sbTextSize;
    private TextView tvTextSizeValue;

    public ProfileFragment() { /* required empty constructor */ }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // profile header
        ivProfile = view.findViewById(R.id.ivProfile);
        tvHandle = view.findViewById(R.id.tvHandle);
        tvRoleLocation = view.findViewById(R.id.tvRoleLocation);
        tvRatingLabel = view.findViewById(R.id.tvRatingLabel);
        tvJobsCompleted = view.findViewById(R.id.tvJobsCompleted);
        tvMemberSince = view.findViewById(R.id.tvMemberSince);

        tvFullName = view.findViewById(R.id.tvFullName);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvLocation = view.findViewById(R.id.tvLocation);

        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnClient = view.findViewById(R.id.btnClient);
        btnWorker = view.findViewById(R.id.btnWorker);

        // accessibility
        switchDigital = view.findViewById(R.id.switchDigital);
        switchVoice = view.findViewById(R.id.switchVoice);
        switchSimplified = view.findViewById(R.id.switchSimplified);
        sbTextSize = view.findViewById(R.id.sbTextSize);
        tvTextSizeValue = view.findViewById(R.id.tvTextSizeValue);

        // Populate dummy data (replace with Firebase values)
        tvHandle.setText("@Kavitha Dissanayake");
        tvRoleLocation.setText("Worker · Colombo 5");
        tvRatingLabel.setText("★ 4.6");
        tvJobsCompleted.setText("24");
        tvMemberSince.setText("2021");

        tvFullName.setText("Kavitha Dissanayake");
        tvPhone.setText("+91-8129999999");
        tvEmail.setText("kavitha.d@gmail.com");
        tvLocation.setText("Colombo 5");

        // Toggle default: Worker selected
        setToggleState(true);

        btnClient.setOnClickListener(v -> {
            setToggleState(false);
            Toast.makeText(requireContext(), "Client mode selected (dummy)", Toast.LENGTH_SHORT).show();
        });

        btnWorker.setOnClickListener(v -> {
            setToggleState(true);
            Toast.makeText(requireContext(), "Worker mode selected (dummy)", Toast.LENGTH_SHORT).show();
        });

        btnEditProfile.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Edit profile clicked (dummy)", Toast.LENGTH_SHORT).show();
        });

        // Accessibility defaults
        switchDigital.setChecked(true);   // you can set based on user data
        switchVoice.setChecked(false);
        switchSimplified.setChecked(false);

        switchDigital.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(requireContext(),
                        "Digital proficiency switch: " + (isChecked ? "ON" : "OFF"),
                        Toast.LENGTH_SHORT).show());

        switchVoice.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(requireContext(),
                        "Voice guidance: " + (isChecked ? "ON" : "OFF"),
                        Toast.LENGTH_SHORT).show());

        switchSimplified.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(requireContext(),
                        "Simplified layout: " + (isChecked ? "ON" : "OFF"),
                        Toast.LENGTH_SHORT).show());

        // SeekBar: map [0..100] -> [0.8..1.8], default 1.3 -> progress 50
        sbTextSize.setMax(100);
        sbTextSize.setProgress(50);
        updateTextSizeLabel(50);

        sbTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTextSizeLabel(progress);
                // here you could broadcast the new size to the UI or save to preferences
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    private void updateTextSizeLabel(int progress) {
        double val = 0.8 + (progress / 100.0); // 0.8 .. 1.8
        DecimalFormat df = new DecimalFormat("0.0");
        tvTextSizeValue.setText(df.format(val) + "x");
    }

    private void setToggleState(boolean workerSelected) {
        // simple visual toggle: highlight worker or client (adjust to your colors)
        if (workerSelected) {
            btnWorker.setBackgroundTintList(requireContext().getResources().getColorStateList(R.color.purple_500));
            btnWorker.setTextColor(requireContext().getResources().getColor(android.R.color.white));
            btnClient.setBackgroundTintList(requireContext().getResources().getColorStateList(android.R.color.transparent));
            btnClient.setTextColor(requireContext().getResources().getColor(R.color.black));
        } else {
            btnClient.setBackgroundTintList(requireContext().getResources().getColorStateList(R.color.purple_500));
            btnClient.setTextColor(requireContext().getResources().getColor(android.R.color.white));
            btnWorker.setBackgroundTintList(requireContext().getResources().getColorStateList(android.R.color.transparent));
            btnWorker.setTextColor(requireContext().getResources().getColor(R.color.black));
        }
    }
}
