
package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PanicAlertStatusActivity extends AppCompatActivity {

    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_INCIDENT = "incidentType";
    public static final String EXTRA_SEVERITY = "severity";
    public static final String EXTRA_STATE = "state";
    public static final String EXTRA_JOB_ID = "jobId";
    public static final String EXTRA_BATTERY = "battery";
    public static final String EXTRA_MOTION = "motion";
    public static final String EXTRA_AUDIO = "audio";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView tvAlertSummary;
    private String caseId;
    private String alertId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panic_alert_status);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        ImageButton btnBack = findViewById(R.id.btn_back_arrow_settings);
        MaterialButton btnOpenCase = findViewById(R.id.btn_open_ccc_case);
        MaterialButton btnViewContacts = findViewById(R.id.btn_view_ccc_contacts);
        tvAlertSummary = findViewById(R.id.tv_alert_summary);

        btnBack.setOnClickListener(v -> finish());
        btnOpenCase.setOnClickListener(v -> {
            Intent intent = new Intent(this, CccActivity.class);
            intent.putExtra("caseId", caseId);
            startActivity(intent);
        });
        btnViewContacts.setOnClickListener(v ->
                startActivity(new Intent(this, EmergencyContactsActivity.class)));

        createAlertAndCase();
    }

    private void createAlertAndCase() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (TextUtils.isEmpty(uid)) {
            tvAlertSummary.setText("Unable to send alert. Please sign in again.");
            return;
        }

        String source = first(getIntent().getStringExtra(EXTRA_SOURCE), "app");
        String incident = first(getIntent().getStringExtra(EXTRA_INCIDENT), "APP_PANIC");
        String severity = first(getIntent().getStringExtra(EXTRA_SEVERITY), "CRITICAL");
        String state = first(getIntent().getStringExtra(EXTRA_STATE), "OPEN");
        String jobId = getIntent().getStringExtra(EXTRA_JOB_ID);
        double motionValue = safeNumber(getIntent().getStringExtra(EXTRA_MOTION));
        String audioValue = first(getIntent().getStringExtra(EXTRA_AUDIO), "");
        String battery = first(getIntent().getStringExtra(EXTRA_BATTERY), "");

        tvAlertSummary.setText(
                "Sending emergency alert...\n" +
                "Source: " + source + "\n" +
                "Incident: " + incident + "\n" +
                "Severity: " + severity + "\n" +
                "State: " + state
        );

        Map<String, Object> alert = new HashMap<>();
        alert.put("workerUid", uid);
        alert.put("jobId", jobId);
        alert.put("source", source);
        alert.put("signalType", incident);
        alert.put("severity", severity);
        alert.put("status", "OPEN");
        String alertMessage;
        if ("CRITICAL_FALL".equalsIgnoreCase(incident)) {
            alertMessage = "Fall detected by WoilGuard device";
        } else if (source.equalsIgnoreCase("app")) {
            alertMessage = "In-app panic button pressed";
        } else {
            alertMessage = "Guard panic alert";
        }
        alert.put("message", alertMessage);
        alert.put("detectedByModel", "CRITICAL_FALL".equalsIgnoreCase(incident)
                || (!"APP_PANIC".equalsIgnoreCase(incident) && !"PANIC_BUTTON".equalsIgnoreCase(incident)));
        alert.put("modelConfidence", motionValue);
        alert.put("createdAt", FieldValue.serverTimestamp());

        db.collection("panic_alerts").add(alert)
                .addOnSuccessListener(alertRef -> {
                    alertId = alertRef.getId();

                    Map<String, Object> caseDoc = new HashMap<>();
                    caseDoc.put("caseType", "panic");
                    caseDoc.put("source", source);
                    caseDoc.put("sourceId", alertId);
                    caseDoc.put("workerUid", uid);
                    caseDoc.put("jobId", jobId);
                    caseDoc.put("title", "Emergency alert: " + incident);
                    caseDoc.put("summary", "Immediate alert created from " + source + " panic trigger.");
                    caseDoc.put("severity", severity);
                    caseDoc.put("status", "OPEN");
                    caseDoc.put("modelConfidence", motionValue);
                    caseDoc.put("battery", battery);
                    caseDoc.put("audio", audioValue);
                    caseDoc.put("createdAt", FieldValue.serverTimestamp());
                    caseDoc.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("core_cases").add(caseDoc)
                            .addOnSuccessListener(caseRef -> {
                                caseId = caseRef.getId();
                                logCaseAction(uid, "CREATED", "Panic alert created from " + source + " trigger.");
                                queueEmergencyContact(uid, caseId, severity);
                                createUserNotification(uid, severity, caseId);
                                tvAlertSummary.setText(
                                        "Emergency alert sent successfully.\n\n" +
                                        "Case ID: " + caseId + "\n" +
                                        "Severity: " + severity + "\n" +
                                        "Your emergency contact notification has been queued.\n" +
                                        "CCC case has also been created for follow-up."
                                );
                            })
                            .addOnFailureListener(e -> fail("Case creation failed: " + e.getMessage()));
                })
                .addOnFailureListener(e -> fail("Alert send failed: " + e.getMessage()));
    }

    private void queueEmergencyContact(String uid, String caseId, String severity) {
        db.collection("ccc_contacts").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.exists() ? first(doc.getString("name"), "Emergency contact") : "Emergency contact";
                    String phone = doc.exists() ? first(doc.getString("phone"), "") : "";
                    String relation = doc.exists() ? first(doc.getString("relation"), "") : "";

                    Map<String, Object> provider = new HashMap<>();
                    provider.put("caseId", caseId);
                    provider.put("channel", "sms");
                    provider.put("provider", "queued");
                    provider.put("targetName", name);
                    provider.put("targetPhone", phone);
                    provider.put("relation", relation);
                    provider.put("status", TextUtils.isEmpty(phone) ? "MISSING_CONTACT" : "QUEUED");
                    provider.put("message", "Woil emergency alert for " + uid + " severity " + severity);
                    provider.put("createdAt", FieldValue.serverTimestamp());
                    db.collection("provider_responses").add(provider);
                });
    }

    private void createUserNotification(String uid, String severity, String caseId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("uid", uid);
        notification.put("title", "Emergency alert sent");
        notification.put("body", "Your " + severity.toLowerCase(Locale.ROOT) + " alert was created and routed to CCC.");
        notification.put("targetType", "ccc_case");
        notification.put("targetId", caseId);
        notification.put("isRead", false);
        notification.put("createdAt", FieldValue.serverTimestamp());
        db.collection("notifications").add(notification);
    }

    private void logCaseAction(String uid, String actionType, String notes) {
        if (TextUtils.isEmpty(caseId)) return;
        Map<String, Object> action = new HashMap<>();
        action.put("caseId", caseId);
        action.put("actorUid", uid);
        action.put("actionType", actionType);
        action.put("notes", notes);
        action.put("createdAt", FieldValue.serverTimestamp());
        db.collection("core_case_action_log").add(action);
    }

    private void fail(String message) {
        tvAlertSummary.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String first(String a, String fallback) { return TextUtils.isEmpty(a) ? fallback : a; }
    private double safeNumber(String text) { try { return Double.parseDouble(first(text, "0")); } catch (Exception e) { return 0; } }
}
