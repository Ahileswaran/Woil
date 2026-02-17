package com.example.woil.ui;

import android.net.Uri;
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

import com.bumptech.glide.Glide;
import com.example.woil.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

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

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration profileListener;

    public ProfileFragment() { /* required empty constructor */ }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // view refs
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

        // Firebase init
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // default UI placeholders while waiting for Firestore
        setPlaceholders();

        // attach Firestore listener to update UI when data arrives/changes
        attachProfileListener();

        // button actions (adapt to your navigation)
        btnEditProfile.setOnClickListener(v -> {
            // open the profile setup activity for editing
            // ensure ProfileSetupActivity is declared in manifest
            startActivity(new android.content.Intent(requireContext(), ProfileSetupActivity.class));
        });

        btnClient.setOnClickListener(v -> {
            setToggleState(false);
            Toast.makeText(requireContext(), "Client mode selected", Toast.LENGTH_SHORT).show();
            // optionally write mode to UI state or to Firestore
        });

        btnWorker.setOnClickListener(v -> {
            setToggleState(true);
            Toast.makeText(requireContext(), "Worker mode selected", Toast.LENGTH_SHORT).show();
            // optionally write mode to UI state or to Firestore
        });

        // accessibility defaults (you can override from saved preferences)
        switchDigital.setChecked(true);
        switchVoice.setChecked(false);
        switchSimplified.setChecked(false);

        switchDigital.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(requireContext(),
                        "Digital proficiency: " + (isChecked ? "ON" : "OFF"),
                        Toast.LENGTH_SHORT).show());

        switchVoice.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(requireContext(),
                        "Voice guidance: " + (isChecked ? "ON" : "OFF"),
                        Toast.LENGTH_SHORT).show());

        switchSimplified.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(requireContext(),
                        "Simplified layout: " + (isChecked ? "ON" : "OFF"),
                        Toast.LENGTH_SHORT).show());

        sbTextSize.setMax(100);
        sbTextSize.setProgress(50);
        updateTextSizeLabel(50);
        sbTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTextSizeLabel(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    private void setPlaceholders() {
        tvHandle.setText("—");
        tvRoleLocation.setText("—");
        tvRatingLabel.setText("★ —");
        tvJobsCompleted.setText("0");
        tvMemberSince.setText("—");

        tvFullName.setText("—");
        tvPhone.setText("");
        tvEmail.setText("");
        tvLocation.setText("—");

        // placeholder image
        ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
        // default toggle state: worker
        setToggleState(true);
    }

    private void attachProfileListener() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        profileListener = db.collection("users").document(uid)
                .addSnapshotListener((DocumentSnapshot snap, com.google.firebase.firestore.FirebaseFirestoreException e) -> {
                    if (e != null) {
                        // Log or show error if you want
                        return;
                    }
                    if (snap != null && snap.exists()) {
                        populateUIFromSnapshot(snap);
                    }
                });
    }

    private void populateUIFromSnapshot(DocumentSnapshot snap) {
        // name
        String first = snap.getString("firstName");
        String last = snap.getString("lastName");
        String fullName = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
        tvFullName.setText(fullName.isEmpty() ? "—" : fullName);

        // handle (@name) - using first + last or fallback to uid
        String handle = (first != null || last != null) ? ("@" + (fullName.isEmpty() ? mAuth.getCurrentUser().getUid() : fullName)) : "@" + mAuth.getCurrentUser().getUid();
        tvHandle.setText(handle);

        // address / location
        String address = snap.getString("address");
        tvLocation.setText(address != null ? address : "—");

        // role + location
        String role = snap.getString("role");
        String roleText = role != null ? (role.substring(0,1).toUpperCase() + role.substring(1)) : "";
        tvRoleLocation.setText(roleText + (address != null && !address.isEmpty() ? " · " + address : ""));

        // phone/email from FirebaseAuth (Auth is canonical for these)
        if (mAuth.getCurrentUser() != null) {
            String phone = mAuth.getCurrentUser().getPhoneNumber();
            String email = mAuth.getCurrentUser().getEmail();
            tvPhone.setText(phone != null ? phone : "");
            tvEmail.setText(email != null ? email : "");
        }

        // rating and jobsCompleted (if present)
        Object ratingObj = snap.get("rating");
        if (ratingObj != null) {
            try {
                double rating = Double.parseDouble(ratingObj.toString());
                tvRatingLabel.setText("★ " + new DecimalFormat("#0.0").format(rating));
            } catch (Exception ignored) { }
        }

        Object jobsObj = snap.get("jobsCompleted");
        if (jobsObj != null) tvJobsCompleted.setText(String.valueOf(jobsObj));

        // member since (handle Timestamp or millis)
        Object ms = snap.get("memberSince");
        if (ms instanceof Timestamp) {
            Date d = ((Timestamp) ms).toDate();
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            tvMemberSince.setText(String.valueOf(c.get(Calendar.YEAR)));
        } else {
            // try millis
            Object msMillis = snap.get("memberSinceMillis");
            if (msMillis instanceof Number) {
                long millis = ((Number) msMillis).longValue();
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(millis);
                tvMemberSince.setText(String.valueOf(c.get(Calendar.YEAR)));
            }
        }

        // profile image - nicImageUri could be a download URL or content:// uri saved earlier
        String nicImageUri = snap.getString("nicImageUri");
        if (nicImageUri != null && !nicImageUri.isEmpty()) {
            // Glide handles http(s) and local URIs
            try {
                Glide.with(this)
                        .load(nicImageUri)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(ivProfile);
            } catch (Exception ex) {
                // fallback to direct Uri set
                try { ivProfile.setImageURI(Uri.parse(nicImageUri)); } catch (Exception ignored) { }
            }
        } else {
            ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // choose toggle based on role
        setToggleState("worker".equalsIgnoreCase(role));
    }

    private void updateTextSizeLabel(int progress) {
        double val = 0.8 + (progress / 100.0); // 0.8 .. 1.8
        DecimalFormat df = new DecimalFormat("0.0");
        tvTextSizeValue.setText(df.format(val) + "x");
    }

    private void setToggleState(boolean workerSelected) {
        if (requireContext() == null) return;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
    }
}
