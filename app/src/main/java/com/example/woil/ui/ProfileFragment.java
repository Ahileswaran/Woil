package com.example.woil.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
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

    private TextView tvVerificationSubtitle;
    private TextView tvIdParsedDob;
    private TextView tvIdParsedGender;
    private TextView tvIdMatch;
    private TextView tvIdStatus;

    private SwitchMaterial switchDigital, switchVoice, switchSimplified;
    private SeekBar seekTextSize;
    private TextView tvTextSizeValue;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ListenerRegistration userListener;
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

        TextView txtViewSkillDetails = view.findViewById(R.id.txt_view_skill_details);
        txtViewSkillDetails.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SkillShowcaseActivity.class);
            startActivity(intent);
        });

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

        tvVerificationSubtitle = view.findViewById(R.id.tv_verification_subtitle);
        tvIdParsedDob = view.findViewById(R.id.tv_id_parsed_dob);
        tvIdParsedGender = view.findViewById(R.id.tv_id_parsed_gender);
        tvIdMatch = view.findViewById(R.id.tv_id_match);
        tvIdStatus = view.findViewById(R.id.tv_id_status);

        switchDigital = view.findViewById(R.id.switch_digital);
        switchVoice = view.findViewById(R.id.switch_voice);
        switchSimplified = view.findViewById(R.id.switch_simple);
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

        attachListeners();
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

        if (tvVerificationSubtitle != null) tvVerificationSubtitle.setText("Awaiting admin review");
        if (tvIdParsedDob != null) tvIdParsedDob.setText("ID Parsed DOB: -");
        if (tvIdParsedGender != null) tvIdParsedGender.setText("Gender: -");
        if (tvIdMatch != null) tvIdMatch.setText("ID data match: -");
        if (tvIdStatus != null) tvIdStatus.setText("Status: -");

        if (ivProfile != null) ivProfile.setImageResource(R.drawable.photo_placeholder);

        if (tvPending != null) {
            tvPending.setText("⏱ Pending");
            tvPending.setTextColor(Color.parseColor("#6B4B00"));
        }
    }

    private void attachListeners() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

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
        String phoneFromDoc = snap.getString("phone");
        String emailFromDoc = snap.getString("email");
        Boolean userNicVerified = snap.getBoolean("nicVerified");

        if (mAuth.getCurrentUser() != null) {
            String phone = mAuth.getCurrentUser().getPhoneNumber();
            String email = mAuth.getCurrentUser().getEmail();

            if (tvPhone != null) {
                tvPhone.setText(!TextUtils.isEmpty(phone) ? phone : safe(phoneFromDoc));
            }
            if (tvEmail != null) {
                tvEmail.setText(!TextUtils.isEmpty(email) ? email : safe(emailFromDoc));
            }
        } else {
            if (tvPhone != null) tvPhone.setText(safe(phoneFromDoc));
            if (tvEmail != null) tvEmail.setText(safe(emailFromDoc));
        }

        if (Boolean.TRUE.equals(userNicVerified) && tvPending != null) {
            tvPending.setText("✔ Verified");
            tvPending.setTextColor(Color.parseColor("#2E7D32"));
        }

        if (tvSubtitle != null && !TextUtils.isEmpty(role) && tvSubtitle.getText() != null) {
            String current = tvSubtitle.getText().toString();
            if (current.contains(" · ")) {
                String suffix = current.substring(current.indexOf(" · "));
                tvSubtitle.setText(capitalize(role) + suffix);
            } else {
                tvSubtitle.setText(capitalize(role));
            }
        }
    }

    private void populateFromProfileSnapshot(DocumentSnapshot snap) {
        String first = snap.getString("firstName");
        String last = snap.getString("lastName");
        String displayName = snap.getString("displayName");
        String locationText = snap.getString("locationText");
        if (TextUtils.isEmpty(locationText)) locationText = snap.getString("address");

        String fullName = !TextUtils.isEmpty(displayName)
                ? displayName
                : ((safe(first) + " " + safe(last)).trim());

        if (TextUtils.isEmpty(fullName)) fullName = "—";

        if (tvFullName != null) tvFullName.setText(fullName);

        String handle = "@" + fullName.replaceAll("\\s+", "");
        if ("@".equals(handle)) {
            handle = "@" + (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "user");
        }
        if (tvUsername != null) tvUsername.setText(handle);

        String role = snap.getString("role");
        String subtitle = (TextUtils.isEmpty(role) ? "Worker" : capitalize(role))
                + (!TextUtils.isEmpty(locationText) ? " · " + locationText : " · —");
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);

        if (tvLocation != null) tvLocation.setText(!TextUtils.isEmpty(locationText) ? locationText : "—");

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
            jobsObj = snap.get("completedJobs");
            if (jobsObj == null) jobsObj = snap.get("jobsCompleted");
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
            } else if (snap.getTimestamp("createdAt") != null && tvMemberSince != null) {
                Calendar c = Calendar.getInstance();
                c.setTime(snap.getTimestamp("createdAt").toDate());
                tvMemberSince.setText(String.valueOf(c.get(Calendar.YEAR)));
            }
        }

        String nicParsedDob = snap.getString("nicParsedDob");
        String nicParsedGender = snap.getString("nicParsedGender");
        Boolean nicMatch = snap.getBoolean("nicMatch");
        String nicVerificationStatus = snap.getString("nicVerificationStatus");
        Boolean nicVerified = snap.getBoolean("nicVerified");

        if (tvIdParsedDob != null) {
            tvIdParsedDob.setText("ID Parsed DOB: " + (!TextUtils.isEmpty(nicParsedDob) ? nicParsedDob : "-"));
        }

        if (tvIdParsedGender != null) {
            String genderLabel;
            if ("M".equalsIgnoreCase(nicParsedGender)) genderLabel = "Male";
            else if ("F".equalsIgnoreCase(nicParsedGender)) genderLabel = "Female";
            else genderLabel = "-";
            tvIdParsedGender.setText("Gender: " + genderLabel);
        }

        if (tvIdMatch != null) {
            tvIdMatch.setText("ID data match: " +
                    (nicMatch != null ? (nicMatch ? "Yes" : "No") : "-"));
        }

        if (tvIdStatus != null) {
            tvIdStatus.setText("Status: " +
                    (!TextUtils.isEmpty(nicVerificationStatus) ? nicVerificationStatus : "-"));
        }

        if (Boolean.TRUE.equals(nicVerified)) {
            if (tvPending != null) {
                tvPending.setText("✔ Verified");
                tvPending.setTextColor(Color.parseColor("#2E7D32"));
            }
            if (tvVerificationSubtitle != null) {
                tvVerificationSubtitle.setText("NIC manually approved");
            }
        } else if ("AUTO_MATCHED_PENDING_ADMIN".equals(nicVerificationStatus)) {
            if (tvPending != null) {
                tvPending.setText("⏱ Pending");
                tvPending.setTextColor(Color.parseColor("#6B4B00"));
            }
            if (tvVerificationSubtitle != null) {
                tvVerificationSubtitle.setText("Auto-matched. Awaiting admin review");
            }
        } else if ("MISMATCH".equals(nicVerificationStatus)) {
            if (tvPending != null) {
                tvPending.setText("⚠ Mismatch");
                tvPending.setTextColor(Color.parseColor("#C62828"));
            }
            if (tvVerificationSubtitle != null) {
                tvVerificationSubtitle.setText("Entered details do not match NIC data");
            }
        } else if ("NOT_PROVIDED".equals(nicVerificationStatus) || TextUtils.isEmpty(nicVerificationStatus)) {
            if (tvPending != null) {
                tvPending.setText("— Not provided");
                tvPending.setTextColor(Color.parseColor("#666666"));
            }
            if (tvVerificationSubtitle != null) {
                tvVerificationSubtitle.setText("NIC not submitted");
            }
        }

        String photoUrl = snap.getString("photoUrl");
        if (TextUtils.isEmpty(photoUrl)) photoUrl = snap.getString("photo");
        if (TextUtils.isEmpty(photoUrl)) photoUrl = snap.getString("avatar");
        if (TextUtils.isEmpty(photoUrl)) photoUrl = snap.getString("nicFrontUri");

        if (!TextUtils.isEmpty(photoUrl)) {
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
                    ivProfile.setImageResource(R.drawable.photo_placeholder);
                }
            }
        } else {
            ivProfile.setImageResource(R.drawable.photo_placeholder);
        }
    }

    private void updateTextSizeLabel(int progress) {
        double val = 0.8 + (progress / 100.0);
        DecimalFormat df = new DecimalFormat("0.0");
        if (tvTextSizeValue != null) tvTextSizeValue.setText(df.format(val) + "x");
    }

    private String capitalize(String s) {
        if (TextUtils.isEmpty(s)) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String safe(String value) {
        return value == null ? "" : value;
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