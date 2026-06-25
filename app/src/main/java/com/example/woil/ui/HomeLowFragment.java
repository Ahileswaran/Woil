package com.example.woil.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.woil.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class HomeLowFragment extends Fragment {

    private ImageView profileImage;
    private TextView tvName;
    private TextView tvRole;
    private TextView boxWage;
    private TextView boxJobDone;
    private TextView boxNext;
    private TextView boxFind;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ListenerRegistration userListener;
    private ListenerRegistration profileListener;

    public HomeLowFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_low_profile_dashboard, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindProfileViews(view);
        setPlaceholders();
        bindActions(view);
        setupSwipeToSettings(view);
        attachProfileListeners();

        return view;
    }

    private void bindProfileViews(@NonNull View view) {
        profileImage = view.findViewById(R.id.profile_image);
        tvName = view.findViewById(R.id.tv_name);
        tvRole = view.findViewById(R.id.tv_role);
        boxWage = view.findViewById(R.id.box_wage);
        boxJobDone = view.findViewById(R.id.box_job_done);
        boxNext = view.findViewById(R.id.box_next);
        boxFind = view.findViewById(R.id.box_find);
    }

    private void setPlaceholders() {
        if (tvName != null) tvName.setText("—");
        if (tvRole != null) tvRole.setText("Worker");
        if (boxWage != null) boxWage.setText("Wage\nLKR 0");
        if (boxJobDone != null) boxJobDone.setText("Done\n0");
        if (boxNext != null) boxNext.setText("Next\n—");
        if (boxFind != null) boxFind.setText("Alerts\n0");

        if (profileImage != null) {
            profileImage.setImageResource(R.drawable.low_profile_pic);
        }
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
        if (!TextUtils.isEmpty(role) && tvRole != null) {
            tvRole.setText(capitalize(role));
        }

        Object alertsObj = snap.get("alertsCount");
        if (alertsObj == null) alertsObj = snap.get("alerts");
        if (alertsObj == null) alertsObj = snap.get("jobAlerts");

        if (boxFind != null) {
            boxFind.setText("Alerts\n" + (alertsObj != null ? alertsObj.toString() : "0"));
        }
    }

    private void populateFromProfileSnapshot(DocumentSnapshot snap) {
        String first = snap.getString("firstName");
        String last = snap.getString("lastName");
        String displayName = snap.getString("displayName");

        String fullName = !TextUtils.isEmpty(displayName)
                ? displayName
                : ((safe(first) + " " + safe(last)).trim());

        if (TextUtils.isEmpty(fullName)) fullName = "—";

        if (tvName != null) tvName.setText(fullName);

        String role = snap.getString("role");
        if (!TextUtils.isEmpty(role) && tvRole != null) {
            tvRole.setText(capitalize(role));
        }

        Object wageObj = snap.get("wage");
        if (wageObj == null) wageObj = snap.get("dailyWage");
        if (wageObj == null) wageObj = snap.get("expectedWage");
        if (wageObj == null) wageObj = snap.get("rate");

        if (boxWage != null) {
            boxWage.setText("Wage\nLKR " + formatValueOrDefault(wageObj, "0"));
        }

        Object jobsObj = snap.get("completedJobs");
        if (jobsObj == null) jobsObj = snap.get("jobsCompleted");
        if (jobsObj == null) jobsObj = snap.get("jobs");
        if (jobsObj == null) jobsObj = snap.get("jobsDone");

        if (boxJobDone != null) {
            boxJobDone.setText("Done\n" + formatValueOrDefault(jobsObj, "0"));
        }

        String nextJob = snap.getString("nextJob");
        if (TextUtils.isEmpty(nextJob)) nextJob = snap.getString("nextTask");
        if (TextUtils.isEmpty(nextJob)) nextJob = snap.getString("preferredCategory");

        if (boxNext != null) {
            boxNext.setText("Next\n" + (!TextUtils.isEmpty(nextJob) ? nextJob : "—"));
        }

        Object alertsObj = snap.get("alertsCount");
        if (alertsObj == null) alertsObj = snap.get("alerts");
        if (alertsObj == null) alertsObj = snap.get("jobAlerts");

        if (boxFind != null) {
            boxFind.setText("Alerts\n" + formatValueOrDefault(alertsObj, "0"));
        }

        String photoUrl = snap.getString("photoUrl");
        if (TextUtils.isEmpty(photoUrl)) photoUrl = snap.getString("photo");
        if (TextUtils.isEmpty(photoUrl)) photoUrl = snap.getString("avatar");

        if (!TextUtils.isEmpty(photoUrl) && profileImage != null) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.low_profile_pic)
                    .error(R.drawable.low_profile_pic)
                    .into(profileImage);
        } else if (profileImage != null) {
            profileImage.setImageResource(R.drawable.low_profile_pic);
        }
    }

    private String formatValueOrDefault(Object value, String fallback) {
        if (value == null) return fallback;
        String text = value.toString().trim();
        return text.isEmpty() ? fallback : text;
    }

    private String capitalize(String s) {
        if (TextUtils.isEmpty(s)) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void bindActions(@NonNull View view) {
        ImageButton panicButton = view.findViewById(R.id.btn_panic);
        Button callAlert1 = view.findViewById(R.id.btn_call_alert1);
        Button callAlert2 = view.findViewById(R.id.btn_call_alert2);
        Button callAlert3 = view.findViewById(R.id.btn_call_alert3);
        Button calculateButton = view.findViewById(R.id.btn_calculate);

        ImageButton btnPhone = view.findViewById(R.id.btn_phone);
        ImageButton btnMessage = view.findViewById(R.id.btn_message);
        ImageButton btnVideo = view.findViewById(R.id.btn_video);

        if (panicButton != null) {
            panicButton.setOnClickListener(v -> {
                android.widget.Toast.makeText(requireContext(), "Panic button clicked (Low)", android.widget.Toast.LENGTH_SHORT).show();
                android.util.Log.d("PanicButton", "Panic button clicked in HomeLowFragment");
                openPanicAlert();
            });
        }

        if (callAlert1 != null) {
            callAlert1.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Calling client for alert 1", Toast.LENGTH_SHORT).show()
            );
        }

        if (callAlert2 != null) {
            callAlert2.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Calling client for alert 2", Toast.LENGTH_SHORT).show()
            );
        }

        if (callAlert3 != null) {
            callAlert3.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Calling client for alert 3", Toast.LENGTH_SHORT).show()
            );
        }

        if (calculateButton != null) {
            calculateButton.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity())
                            .navigateToFragment(new WageFragment(), true, "wage");
                }
            });
        }

        if (btnPhone != null) {
            btnPhone.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Phone action", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnMessage != null) {
            btnMessage.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity())
                            .navigateToFragment(new ChatFragment(), true, "chat");
                }
            });
        }

        if (btnVideo != null) {
            btnVideo.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Video action", Toast.LENGTH_SHORT).show()
            );
        }
    }


    private void openPanicAlert() {
        try {
            startActivity(new android.content.Intent(requireContext(), PanicAlertStatusActivity.class)
                    .putExtra("incidentType", "APP_PANIC")
                    .putExtra("severity", "CRITICAL")
                    .putExtra(PanicAlertStatusActivity.EXTRA_SOURCE, "app")
                    .putExtra("state", "OPEN"));
        } catch (Exception e) {
            android.util.Log.e("PanicButton", "Error starting PanicAlertStatusActivity in Low", e);
            Toast.makeText(requireContext(), "Unable to start emergency alert: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupSwipeToSettings(@NonNull View rootView) {
        final View swipeTarget = rootView.findViewById(R.id.scroll_wage) != null
                ? rootView.findViewById(R.id.scroll_wage)
                : rootView;

        final GestureDetector gestureDetector = new GestureDetector(
                requireContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    private static final int SWIPE_THRESHOLD = 120;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 120;

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {
                        if (e1 == null || e2 == null) return false;

                        float diffX = e2.getX() - e1.getX();
                        float diffY = e2.getY() - e1.getY();

                        if (Math.abs(diffX) > Math.abs(diffY)) {
                            int width = swipeTarget.getWidth();
                            boolean startedNearRight = (width == 0) || (e1.getX() > width * 0.65f);

                            if (startedNearRight
                                    && diffX < -SWIPE_THRESHOLD
                                    && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                                openSettingsFragmentWithAnimation();
                                return true;
                            }
                        }
                        return false;
                    }
                }
        );

        swipeTarget.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void openSettingsFragmentWithAnimation() {
        SettingsFragment settingsFragment = new SettingsFragment();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).navigateToFragment(settingsFragment, true, "settings");
            return;
        }

        FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();

        try {
            ft.setCustomAnimations(
                    R.anim.enter_from_right,
                    R.anim.exit_to_left,
                    R.anim.enter_from_left,
                    R.anim.exit_to_right
            );
        } catch (Resources.NotFoundException ignored) { }

        ft.replace(R.id.nav_host_fragment, settingsFragment);
        ft.addToBackStack("settings");
        ft.commit();
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