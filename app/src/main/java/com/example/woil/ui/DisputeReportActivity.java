
package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class DisputeReportActivity extends AppCompatActivity {
    public static final String EXTRA_JOB_ID = "jobId";
    public static final String EXTRA_AGAINST_UID = "againstUid";
    public static final String EXTRA_CASE_TITLE = "caseTitle";
    public static final String EXTRA_SOURCE = "source";

    private ImageButton btnBack;
    private AutoCompleteTextView actType;
    private EditText etAgainstUid, etJobId, etReason;
    private MaterialButton btnSubmit;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispute_report);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnBack = findViewById(R.id.btn_back_arrow_settings);
        actType = findViewById(R.id.act_dispute_type);
        etAgainstUid = findViewById(R.id.et_against_uid);
        etJobId = findViewById(R.id.et_job_id);
        etReason = findViewById(R.id.et_dispute_reason);
        btnSubmit = findViewById(R.id.btn_submit_dispute);

        String[] issueTypes = new String[]{"payment_issue","cancellation","harassment","unsafe_work","abuse","other"};
        actType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, issueTypes));

        Intent i = getIntent();
        etJobId.setText(first(i.getStringExtra(EXTRA_JOB_ID), ""));
        etAgainstUid.setText(first(i.getStringExtra(EXTRA_AGAINST_UID), ""));
        actType.setText(first(i.getStringExtra(EXTRA_CASE_TITLE), "payment_issue"), false);

        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitDispute());
    }

    private void submitDispute() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (TextUtils.isEmpty(uid)) {
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = actType.getText() == null ? "" : actType.getText().toString().trim();
        String jobId = etJobId.getText().toString().trim();
        String againstUid = etAgainstUid.getText().toString().trim();
        String reason = etReason.getText().toString().trim();

        if (TextUtils.isEmpty(type) || TextUtils.isEmpty(reason)) {
            Toast.makeText(this, "Select issue type and enter complaint details.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);

        Map<String, Object> dispute = new HashMap<>();
        dispute.put("createdByUid", uid);
        dispute.put("jobId", jobId);
        dispute.put("againstUid", againstUid);
        dispute.put("type", type);
        dispute.put("description", reason);
        dispute.put("status", "OPEN");
        dispute.put("source", first(getIntent().getStringExtra(EXTRA_SOURCE), "app"));
        dispute.put("createdAt", FieldValue.serverTimestamp());
        dispute.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("formal_disputes").add(dispute)
                .addOnSuccessListener(ref -> {
                    Map<String, Object> caseDoc = new HashMap<>();
                    caseDoc.put("caseType", "dispute");
                    caseDoc.put("source", "app");
                    caseDoc.put("sourceId", ref.getId());
                    caseDoc.put("createdByUid", uid);
                    caseDoc.put("jobId", jobId);
                    caseDoc.put("againstUid", againstUid);
                    caseDoc.put("title", "Dispute: " + type);
                    caseDoc.put("summary", reason);
                    caseDoc.put("severity", "MEDIUM");
                    caseDoc.put("status", "OPEN");
                    caseDoc.put("createdAt", FieldValue.serverTimestamp());
                    caseDoc.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("core_cases").add(caseDoc)
                            .addOnSuccessListener(caseRef -> {
                                Map<String, Object> action = new HashMap<>();
                                action.put("caseId", caseRef.getId());
                                action.put("actorUid", uid);
                                action.put("actionType", "CREATED");
                                action.put("notes", "Complaint raised from mobile app");
                                action.put("createdAt", FieldValue.serverTimestamp());
                                db.collection("core_case_action_log").add(action);

                                Map<String, Object> notification = new HashMap<>();
                                notification.put("uid", uid);
                                notification.put("title", "Complaint submitted");
                                notification.put("body", "Your complaint has been sent to the Core Control Center.");
                                notification.put("targetType", "ccc_case");
                                notification.put("targetId", caseRef.getId());
                                notification.put("createdAt", FieldValue.serverTimestamp());
                                notification.put("isRead", false);
                                db.collection("notifications").add(notification);

                                Toast.makeText(this, "Complaint submitted successfully.", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, CccCaseQueueActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnSubmit.setEnabled(true);
                                Toast.makeText(this, "Case creation failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Complaint submit failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String first(String a, String fallback) { return TextUtils.isEmpty(a) ? fallback : a; }
}
