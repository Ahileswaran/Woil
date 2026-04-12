package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;
import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class ClientActivity extends AppCompatActivity {

    private CircleImageView ivProfile;
    private TextView tvUsername, tvSubtitle, tvRatingValue, tvJobsValue, tvMemberSince;
    private TextView tvFullName, tvPhone, tvEmail, tvLocation;
    private Button btnEditProfile;
    private ImageButton btnBack;
    private TextView tvPending;

    // Optional UI from client layout (ads / work progress)
    private ImageView ad1Image, ad2Image;
    private TextView ad1Date, ad1Title, ad2Date, ad2Title;
    private ProgressBar wpTask1Progress, wpTask2Progress;
    private TextView wpTask1Title, wpTask1Date, wpTask2Title, wpTask2Date;

    // toggle buttons
    private Button btnClientToggle, btnWorkerToggle;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration profileListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Replace with the actual layout filename you saved for the client layout
        setContentView(R.layout.activity_client);

        // Allow content to lay out behind system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);


        // Transparent bars so fragment header can draw behind them
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);


        // init firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // find views (IDs match your client XML)
        ivProfile = findViewById(R.id.profile_image_main);
        tvUsername = findViewById(R.id.username);
        tvSubtitle = findViewById(R.id.subtitle);

        tvRatingValue = findViewById(R.id.rating_value);
        tvJobsValue = findViewById(R.id.jobs_value);
        tvMemberSince = findViewById(R.id.member_since_value);

        tvFullName = findViewById(R.id.tv_name);
        tvPhone = findViewById(R.id.tv_phone);
        tvEmail = findViewById(R.id.tv_email);
        tvLocation = findViewById(R.id.tv_location);

        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnBack = findViewById(R.id.btn_back);
      //  tvPending = findViewById(R.id.tv_pending);

        // ads & work progress (optional; null-checks used)
        ad1Image = findViewById(R.id.ad1_image);
        ad1Date = findViewById(R.id.ad1_date);
        ad1Title = findViewById(R.id.ad1_title);
        ad2Image = findViewById(R.id.ad2_image);
        ad2Date = findViewById(R.id.ad2_date);
        ad2Title = findViewById(R.id.ad2_title);

        wpTask1Progress = findViewById(R.id.wp_task1_progress);
        wpTask2Progress = findViewById(R.id.wp_task2_progress);
        wpTask1Title = findViewById(R.id.wp_task1_title);
        wpTask1Date = findViewById(R.id.wp_task1_date);
        wpTask2Title = findViewById(R.id.wp_task2_title);
        wpTask2Date = findViewById(R.id.wp_task2_date);

        btnClientToggle = findViewById(R.id.btn_client); // same id as the toggle in the layout
        btnWorkerToggle = findViewById(R.id.btn_worker);

        setPlaceholders();

        // back button -> finish
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // edit profile -> launch ProfileSetupActivity (fallback toast if missing)
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, Class.forName("com.example.woil.ui.ProfileSetupActivity")));
                } catch (ClassNotFoundException e) {
                    Toast.makeText(this, "Profile editor not available", Toast.LENGTH_SHORT).show();
                }
            });
        }


        MaterialButton btnAddJob = findViewById(R.id.btn_add_job);

        if (btnAddJob != null) {
            btnAddJob.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, PostJobActivity.class));
                } catch (Exception e) {
                    Toast.makeText(this, "Can't open Post Job screen", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Toggle: if user taps "Worker" while on client view, just finish() and return to previous UI.
        if (btnWorkerToggle != null) {
            btnWorkerToggle.setOnClickListener(v -> {
                // if your app uses a fragment for the worker profile, returning (finish) will show it again.
                finish();
            });
        }

        // Make sure the client toggle is visually active
        if (btnClientToggle != null) {
            // optional: set style to active
            btnClientToggle.setEnabled(false);
        }

        attachProfileListener();
    }

    private void setPlaceholders() {
        if (tvUsername != null) tvUsername.setText("—");
        if (tvSubtitle != null) tvSubtitle.setText("Client · —");
        if (tvRatingValue != null) tvRatingValue.setText("★ —");
        if (tvJobsValue != null) tvJobsValue.setText("0");
        if (tvMemberSince != null) tvMemberSince.setText("—");

        if (tvFullName != null) tvFullName.setText("—");
        if (tvPhone != null) tvPhone.setText("");
        if (tvEmail != null) tvEmail.setText("");
        if (tvLocation != null) tvLocation.setText("—");

        if (ivProfile != null) ivProfile.setImageResource(R.drawable.photo_placeholder);

        if (tvPending != null) tvPending.setText("⏱ Pending");

        // optional ad placeholders
        if (ad1Date != null) ad1Date.setText("");
        if (ad1Title != null) ad1Title.setText("");
        if (ad2Date != null) ad2Date.setText("");
        if (ad2Title != null) ad2Title.setText("");
    }

    private void attachProfileListener() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        profileListener = db.collection("users").document(uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        // silent return; you can log
                        return;
                    }
                    if (snap != null && snap.exists()) {
                        populateUIFromSnapshot(snap);
                    }
                });
    }

    private void populateUIFromSnapshot(DocumentSnapshot snap) {
        // NAME / USERNAME / SUBTITLE
        String first = snap.getString("firstName");
        String last  = snap.getString("lastName");
        String displayName = snap.getString("displayName"); // optional

        String fullName = (displayName != null && !displayName.isEmpty())
                ? displayName
                : ((first != null ? first : "") + " " + (last != null ? last : "")).trim();

        if (tvFullName != null) tvFullName.setText(fullName.isEmpty() ? "—" : fullName);

        // header username (handle) - try "handle" then @displayName fallback
        String handle = snap.getString("handle");
        if (handle == null || handle.isEmpty()) {
            handle = fullName.isEmpty() ? ("@" + (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "user")) : ("@" + fullName.replaceAll("\\s+", ""));
        }
        if (tvUsername != null) tvUsername.setText(handle);

        // For client view override role to Client (even if role field exists)
        String address = snap.getString("address");
        String subtitle = "Client" + (address != null && !address.isEmpty() ? " · " + address : "");
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);

        // CONTACTS FROM AUTH (canonical)
        if (mAuth.getCurrentUser() != null) {
            String phone = mAuth.getCurrentUser().getPhoneNumber();
            String email = mAuth.getCurrentUser().getEmail();
            if (tvPhone != null) tvPhone.setText(phone != null ? phone : (snap.getString("phone") != null ? snap.getString("phone") : ""));
            if (tvEmail != null) tvEmail.setText(email != null ? email : (snap.getString("email") != null ? snap.getString("email") : ""));
        } else {
            if (tvPhone != null) tvPhone.setText(snap.getString("phone") != null ? snap.getString("phone") : "");
            if (tvEmail != null) tvEmail.setText(snap.getString("email") != null ? snap.getString("email") : "");
        }

        if (tvLocation != null) tvLocation.setText(address != null ? address : "—");

        // RATING / JOBS / MEMBER SINCE
        Object ratingObj = snap.get("rating");
        if (ratingObj != null && tvRatingValue != null) {
            try {
                double rating = Double.parseDouble(ratingObj.toString());
                tvRatingValue.setText("★ " + new DecimalFormat("#0.0").format(rating));
            } catch (Exception ignored) { }
        }

        Object jobsObj = snap.get("jobsPosted"); // client uses jobsPosted field if present
        if (jobsObj == null) jobsObj = snap.get("jobs"); // fallback
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

        // VERIFIED / PENDING
        Boolean verified = snap.getBoolean("isVerified");
        if (verified != null && verified && tvPending != null) {
            tvPending.setText("✔ Verified");
            tvPending.setTextColor(Color.parseColor("#2E7D32")); // green
        } else if (tvPending != null) {
            tvPending.setText("⏱ Pending");
            tvPending.setTextColor(Color.parseColor("#6B4B00"));
        }

        // PROFILE IMAGE (try common fields)
        String photoUrl = snap.getString("photoUrl");
        if (photoUrl == null || photoUrl.isEmpty()) photoUrl = snap.getString("photo");
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
                    try { ivProfile.setImageURI(Uri.parse(photoUrl)); } catch (Exception ignored) {}
                }
            }
        } else {
            if (ivProfile != null) ivProfile.setImageResource(R.drawable.photo_placeholder);
        }

        // OPTIONAL: populate ads & work progress demo fields if present in doc
        if (ad1Title != null && snap.getString("ad1_title") != null) {
            ad1Title.setText(snap.getString("ad1_title"));
        }
        if (ad1Date != null && snap.getString("ad1_date") != null) {
            ad1Date.setText(snap.getString("ad1_date"));
        }
        if (ad2Title != null && snap.getString("ad2_title") != null) {
            ad2Title.setText(snap.getString("ad2_title"));
        }
        if (ad2Date != null && snap.getString("ad2_date") != null) {
            ad2Date.setText(snap.getString("ad2_date"));
        }

        // Demo progress values if stored as numbers
        Object p1 = snap.get("wp_task1_progress");
        if (p1 instanceof Number && wpTask1Progress != null) {
            wpTask1Progress.setProgress(((Number) p1).intValue());
        }
        Object p2 = snap.get("wp_task2_progress");
        if (p2 instanceof Number && wpTask2Progress != null) {
            wpTask2Progress.setProgress(((Number) p2).intValue());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
    }
}