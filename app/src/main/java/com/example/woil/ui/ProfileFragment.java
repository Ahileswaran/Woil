package com.example.woil.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "woil_prefs";
    private static final String KEY_ACTIVE_ROLE = "active_role";

    private CircleImageView ivProfile;
    private TextView tvUsername, tvSubtitle, tvRatingValue, tvJobsValue, tvMemberSince;
    private TextView tvFullName, tvPhone, tvEmail, tvLocation;
    private Button btnEditProfile;
    private ImageButton btnBack;
    private TextView tvPending;

    private SwitchMaterial switchDigital, switchVoice, switchSimplified;
    private SeekBar seekTextSize;
    private TextView tvTextSizeValue;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration profileListener;

    public ProfileFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ivProfile = view.findViewById(R.id.profile_image_main);
        tvUsername = view.findViewById(R.id.username);
        tvSubtitle = view.findViewById(R.id.subtitle);

        tvRatingValue = view.findViewById(R.id.rating_value);
        tvJobsValue = view.findViewById(R.id.jobs_value);
        tvMemberSince = view.findViewById(R.id.member_since_value);

        tvFullName = view.findViewById(R.id.tv_name);
        tvPhone = view.findViewById(R.id.tv_phone);
        tvEmail = view.findViewById(R.id.tv_email);
        tvLocation = view.findViewById(R.id.tv_location);

        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnBack = view.findViewById(R.id.btn_back);
        tvPending = view.findViewById(R.id.tv_pending);

        seekTextSize = view.findViewById(R.id.seek_text_size);
        tvTextSizeValue = view.findViewById(R.id.tv_text_size_value);

        setPlaceholders();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).onFragmentArrowBackToHome();
                } else {
                    requireActivity().onBackPressed();
                }
            });
        }

        Button btnClient = view.findViewById(R.id.btn_client);
        if (btnClient != null) {
            btnClient.setOnClickListener(v -> switchToClientProfile());
        }

        Button btnWorker = view.findViewById(R.id.btn_worker);
        if (btnWorker != null) {
            btnWorker.setOnClickListener(v -> switchToWorkerHome());
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(requireContext(), Class.forName("com.example.woil.ui.ProfileSetupActivity")));
                } catch (ClassNotFoundException e) {
                    Toast.makeText(requireContext(), "Profile editor not available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchDigital != null) switchDigital.setChecked(true);
        if (switchVoice != null) switchVoice.setChecked(false);
        if (switchSimplified != null) switchSimplified.setChecked(false);

        if (seekTextSize != null) {
            seekTextSize.setMax(100);
            seekTextSize.setProgress(50);
            updateTextSizeLabel(50);
            seekTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    updateTextSizeLabel(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }

        attachProfileListener();
    }

    private void switchToClientProfile() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid)
                .update("role", "client")
                .addOnSuccessListener(aVoid -> {
                    saveActiveRole("client");
                    Toast.makeText(requireContext(), "Switched to Client", Toast.LENGTH_SHORT).show();

                    try {
                        startActivity(new Intent(requireContext(), ClientActivity.class));
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Client profile not available", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Role switch failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void switchToWorkerHome() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid)
                .update("role", "worker")
                .addOnSuccessListener(aVoid -> {
                    saveActiveRole("worker");
                    Toast.makeText(requireContext(), "Switched to Worker", Toast.LENGTH_SHORT).show();

                    if (requireActivity() instanceof MainActivity) {
                        ((MainActivity) requireActivity()).reloadHomeForRole("worker");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Role switch failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void saveActiveRole(String role) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        prefs.edit().putString(KEY_ACTIVE_ROLE, role).apply();
    }

    private void setPlaceholders() {
        if (tvUsername != null) tvUsername.setText("—");
        if (tvSubtitle != null) tvSubtitle.setText("Worker · —");
        if (tvRatingValue != null) tvRatingValue.setText("★ —");
        if (tvJobsValue != null) tvJobsValue.setText("0");
        if (tvMemberSince != null) tvMemberSince.setText("—");

        if (tvFullName != null) tvFullName.setText("—");
        if (tvPhone != null) tvPhone.setText("");
        if (tvEmail != null) tvEmail.setText("");
        if (tvLocation != null) tvLocation.setText("—");

        if (ivProfile != null) ivProfile.setImageResource(R.drawable.photo_placeholder);

        if (tvPending != null) tvPending.setText("⏱ Pending");
    }

    private void attachProfileListener() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        profileListener = db.collection("users").document(uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) return;
                    if (snap != null && snap.exists()) {
                        populateUIFromSnapshot(snap);
                    }
                });
    }

    private void populateUIFromSnapshot(DocumentSnapshot snap) {
        String first = snap.getString("firstName");
        String last = snap.getString("lastName");
        String displayName = snap.getString("displayName");

        String fullName = (displayName != null && !displayName.isEmpty())
                ? displayName
                : ((first != null ? first : "") + " " + (last != null ? last : "")).trim();

        if (tvFullName != null) tvFullName.setText(fullName.isEmpty() ? "—" : fullName);

        String handle = snap.getString("handle");
        if (handle == null || handle.isEmpty()) {
            handle = fullName.isEmpty()
                    ? ("@" + (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "user"))
                    : ("@" + fullName.replaceAll("\\s+", ""));
        }
        if (tvUsername != null) tvUsername.setText(handle);

        String role = snap.getString("role");
        String address = snap.getString("address");
        String subtitle = (role != null ? capitalize(role) : "Worker")
                + (address != null && !address.isEmpty() ? " · " + address : "");
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);

        if (mAuth.getCurrentUser() != null) {
            String phone = mAuth.getCurrentUser().getPhoneNumber();
            String email = mAuth.getCurrentUser().getEmail();

            if (tvPhone != null) {
                tvPhone.setText(phone != null ? phone : (snap.getString("phone") != null ? snap.getString("phone") : ""));
            }
            if (tvEmail != null) {
                tvEmail.setText(email != null ? email : (snap.getString("email") != null ? snap.getString("email") : ""));
            }
        } else {
            if (tvPhone != null) tvPhone.setText(snap.getString("phone") != null ? snap.getString("phone") : "");
            if (tvEmail != null) tvEmail.setText(snap.getString("email") != null ? snap.getString("email") : "");
        }

        if (tvLocation != null) tvLocation.setText(address != null ? address : "—");

        Object ratingObj = snap.get("rating");
        if (ratingObj != null && tvRatingValue != null) {
            try {
                double rating = Double.parseDouble(ratingObj.toString());
                tvRatingValue.setText("★ " + new DecimalFormat("#0.0").format(rating));
            } catch (Exception ignored) {
            }
        }

        Object jobsObj;
        if ("client".equalsIgnoreCase(role)) {
            jobsObj = snap.get("jobsPosted");
            if (jobsObj == null) jobsObj = snap.get("jobs");
        } else {
            jobsObj = snap.get("jobsCompleted");
            if (jobsObj == null) jobsObj = snap.get("jobs");
        }

        if (jobsObj != null && tvJobsValue != null) {
            tvJobsValue.setText(String.valueOf(jobsObj));
        }

        Object memberSinceObj = snap.get("memberSince");
        if (memberSinceObj instanceof Timestamp && tvMemberSince != null) {
            Date d = ((Timestamp) memberSinceObj).toDate();
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            tvMemberSince.setText(String.valueOf(c.get(Calendar.YEAR)));
        } else {
            Object msMillis = snap.get("memberSinceMillis");
            if (msMillis instanceof Number && tvMemberSince != null) {
                long millis = ((Number) msMillis).longValue();
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(millis);
                tvMemberSince.setText(String.valueOf(c.get(Calendar.YEAR)));
            } else if (snap.getString("memberSinceYear") != null && tvMemberSince != null) {
                tvMemberSince.setText(snap.getString("memberSinceYear"));
            }
        }

        Boolean verified = snap.getBoolean("isVerified");
        if (verified != null && verified && tvPending != null) {
            tvPending.setText("✔ Verified");
            tvPending.setTextColor(Color.parseColor("#2E7D32"));
        } else if (tvPending != null) {
            tvPending.setText("⏱ Pending");
            tvPending.setTextColor(Color.parseColor("#6B4B00"));
        }

        String photoUrl = snap.getString("photoUrl");
        if (photoUrl == null || photoUrl.isEmpty()) photoUrl = snap.getString("photo");
        if (photoUrl == null || photoUrl.isEmpty()) photoUrl = snap.getString("nicImageUri");
        if (photoUrl == null || photoUrl.isEmpty()) photoUrl = snap.getString("avatar");

        if (photoUrl != null && !photoUrl.isEmpty()) {
            if (ivProfile != null) {
                try {
                    Glide.with(this)
                            .load(photoUrl)
                            .placeholder(R.drawable.photo_placeholder)
                            .error(R.drawable.photo_placeholder)
                            .into(ivProfile);
                } catch (Exception ex) {
                    try {
                        ivProfile.setImageURI(Uri.parse(photoUrl));
                    } catch (Exception ignored) {
                    }
                }
            }
        } else {
            if (ivProfile != null) ivProfile.setImageResource(R.drawable.photo_placeholder);
        }
    }

    private void updateTextSizeLabel(int progress) {
        double val = 0.8 + (progress / 100.0);
        DecimalFormat df = new DecimalFormat("0.0");
        if (tvTextSizeValue != null) tvTextSizeValue.setText(df.format(val) + "x");
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
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