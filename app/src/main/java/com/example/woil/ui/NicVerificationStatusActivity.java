
package com.example.woil.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class NicVerificationStatusActivity extends AppCompatActivity {

    private TextView tvStatus;
    private TextView tvSummary;
    private TextView tvQueueId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nic_verification_status);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        ((ImageButton) findViewById(R.id.btn_back_arrow_settings)).setOnClickListener(v -> finish());
        tvStatus = findViewById(R.id.tv_status_value);
        tvSummary = findViewById(R.id.tv_status_summary);
        tvQueueId = findViewById(R.id.tv_queue_id);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (TextUtils.isEmpty(uid)) {
            tvStatus.setText("UNKNOWN");
            tvSummary.setText("Please sign in again to view NIC verification state.");
            return;
        }

        FirebaseFirestore.getInstance().collection("profiles").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String status = doc.getString("nicVerificationStatus");
                    String queueId = doc.getString("nicQueueId");
                    tvStatus.setText(TextUtils.isEmpty(status) ? "NOT_PROVIDED" : status);
                    tvQueueId.setText(TextUtils.isEmpty(queueId) ? "-" : queueId);

                    String summary;
                    if ("VERIFIED".equalsIgnoreCase(status)) {
                        summary = "Your NIC was manually verified by the CCC team.";
                    } else if ("AUTO_MATCHED_PENDING_ADMIN".equalsIgnoreCase(status)) {
                        summary = "Your NIC matched the entered data and is waiting for CCC approval.";
                    } else if ("PENDING_MANUAL_REVIEW".equalsIgnoreCase(status)) {
                        summary = "Your NIC is in manual review. The CCC team will review it.";
                    } else if ("MISMATCH".equalsIgnoreCase(status)) {
                        summary = "The submitted NIC data did not match and may need resubmission.";
                    } else {
                        summary = "No NIC verification request is currently active.";
                    }
                    tvSummary.setText(summary);
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("ERROR");
                    tvSummary.setText("Failed to load NIC verification state: " + e.getMessage());
                });
    }
}
