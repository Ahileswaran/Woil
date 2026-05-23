
package com.example.woil.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.woil.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CccActivity extends AppCompatActivity {

    private TextView tvCaseSummary;
    private Button btnAck, btnEscalate, btnClose, btnMessageContact, btnRaiseComplaint;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String caseId;
    private String workerUid;
    private String jobId;
    private String incident;
    private String severity;
    private String state;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ccc);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvCaseSummary = findViewById(R.id.tvCaseSummary);
        btnAck = findViewById(R.id.btnAck);
        btnEscalate = findViewById(R.id.btnEscalate);
        btnClose = findViewById(R.id.btnClose);
        btnMessageContact = findViewById(R.id.btnMessageContact);
        btnRaiseComplaint = findViewById(R.id.btnRaiseComplaint);

        caseId = getIntent().getStringExtra("caseId");
        incident = getIntent().getStringExtra("incidentType");
        severity = getIntent().getStringExtra("severity");
        state = getIntent().getStringExtra("state");
        workerUid = getIntent().getStringExtra("workerUid");
        jobId = getIntent().getStringExtra("jobId");

        if (!TextUtils.isEmpty(caseId)) {
            loadCase(caseId);
        } else {
            String battery = first(getIntent().getStringExtra("battery"), "");
            String motion = first(getIntent().getStringExtra("motion"), "");
            String audio = first(getIntent().getStringExtra("audio"), "");
            tvCaseSummary.setText(
                    "Incident: " + incident + "\n" +
                            "Severity: " + severity + "\n" +
                            "State: " + state + "\n" +
                            "Battery: " + battery + "\n" +
                            "Motion/Fall: " + motion + "\n" +
                            "Audio: " + audio + "\n" +
                            "Status: OPEN"
            );
            createPanicCase();
        }

        btnAck.setOnClickListener(v -> updateCaseStatus("ACKNOWLEDGED", "Operator acknowledged case"));
        btnEscalate.setOnClickListener(v -> updateCaseStatus("ESCALATED", "Case escalated"));
        btnClose.setOnClickListener(v -> updateCaseStatus("CLOSED", "Case closed"));
        btnMessageContact.setOnClickListener(v -> createContactNotification());
        btnRaiseComplaint.setOnClickListener(v -> {
            Intent intent = new Intent(this, DisputeReportActivity.class);
            intent.putExtra(DisputeReportActivity.EXTRA_JOB_ID, jobId);
            intent.putExtra(DisputeReportActivity.EXTRA_AGAINST_UID, workerUid);
            intent.putExtra(DisputeReportActivity.EXTRA_SOURCE, "ccc");
            startActivity(intent);
        });
    }

    private void createPanicCase() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        Map<String, Object> alert = new HashMap<>();
        alert.put("workerUid", uid);
        alert.put("jobId", jobId);
        alert.put("source", "wearable");
        alert.put("signalType", first(incident, "panic"));
        alert.put("severity", first(severity, "HIGH"));
        alert.put("message", "Woil Guard incident detected");
        alert.put("status", "OPEN");
        alert.put("detectedByModel", !"PANIC_BUTTON".equalsIgnoreCase(first(incident, "")));
        alert.put("modelConfidence", safeNumber(getIntent().getStringExtra("motion")));
        alert.put("createdAt", FieldValue.serverTimestamp());

        db.collection("panic_alerts").add(alert)
                .addOnSuccessListener(alertRef -> {
                    Map<String, Object> caseDoc = new HashMap<>();
                    caseDoc.put("caseType", "panic");
                    caseDoc.put("source", "wearable");
                    caseDoc.put("sourceId", alertRef.getId());
                    caseDoc.put("workerUid", uid);
                    caseDoc.put("jobId", jobId);
                    caseDoc.put("title", "Guard alert: " + first(incident, "panic"));
                    caseDoc.put("summary", "Guard state=" + first(state, "unknown"));
                    caseDoc.put("severity", first(severity, "HIGH"));
                    caseDoc.put("status", "OPEN");
                    caseDoc.put("createdAt", FieldValue.serverTimestamp());
                    caseDoc.put("updatedAt", FieldValue.serverTimestamp());
                    caseDoc.put("modelConfidence", safeNumber(getIntent().getStringExtra("motion")));
                    db.collection("core_cases").add(caseDoc)
                            .addOnSuccessListener(caseRef -> {
                                caseId = caseRef.getId();
                                logCaseAction("CREATED", "Case created from wearable/app incident");
                                createNotification(uid, "Guard alert created", "A CCC case has been opened for your safety alert.", "ccc_case", caseId);
                            });
                });
    }

    private void loadCase(String caseId) {
        db.collection("core_cases").document(caseId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    incident = first(doc.getString("title"), incident);
                    severity = first(doc.getString("severity"), severity);
                    String status = first(doc.getString("status"), "OPEN");
                    String summary = first(doc.getString("summary"), "");
                    workerUid = first(doc.getString("workerUid"), workerUid);
                    jobId = first(doc.getString("jobId"), jobId);

                    tvCaseSummary.setText(
                            "Case ID: " + doc.getId() + "\n" +
                            "Title: " + incident + "\n" +
                            "Severity: " + severity + "\n" +
                            "Source: " + first(doc.getString("source"), "app") + "\n" +
                            "Summary: " + summary + "\n" +
                            "Status: " + status
                    );
                });
    }

    private void updateCaseStatus(String newStatus, String notes) {
        if (TextUtils.isEmpty(caseId)) {
            Toast.makeText(this, "Case has not been created yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("core_cases").document(caseId).set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    logCaseAction(newStatus, notes);
                    tvCaseSummary.append("\nAction: " + newStatus);
                    Toast.makeText(this, "Case " + newStatus.toLowerCase(Locale.ROOT), Toast.LENGTH_SHORT).show();
                    String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
                    createNotification(uid, "CCC case " + newStatus.toLowerCase(Locale.ROOT), "Your CCC case status is now " + newStatus + ".", "ccc_case", caseId);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void logCaseAction(String actionType, String notes) {
        if (TextUtils.isEmpty(caseId)) return;
        Map<String, Object> action = new HashMap<>();
        action.put("caseId", caseId);
        action.put("actorUid", auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null);
        action.put("actionType", actionType);
        action.put("notes", notes);
        action.put("createdAt", FieldValue.serverTimestamp());
        db.collection("core_case_action_log").add(action);
    }

    private void createContactNotification() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        createNotification(uid, "Emergency contact alert", "Your emergency contact notification has been queued.", "ccc_case", caseId);
        Toast.makeText(this, "Emergency contact notification queued.", Toast.LENGTH_SHORT).show();
    }

    private void createNotification(String uid, String title, String body, String targetType, String targetId) {
        if (TextUtils.isEmpty(uid)) return;
        Map<String, Object> notification = new HashMap<>();
        notification.put("uid", uid);
        notification.put("title", title);
        notification.put("body", body);
        notification.put("targetType", targetType);
        notification.put("targetId", targetId);
        notification.put("isRead", false);
        notification.put("createdAt", FieldValue.serverTimestamp());
        db.collection("notifications").add(notification);
    }

    private String first(String a, String fallback) { return TextUtils.isEmpty(a) ? fallback : a; }

    private double safeNumber(String text) {
        try { return Double.parseDouble(first(text, "0")); } catch (Exception e) { return 0; }
    }
}
