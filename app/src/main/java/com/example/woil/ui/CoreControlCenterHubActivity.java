
package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;

public class CoreControlCenterHubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_core_control_center);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        ImageButton btnBack = findViewById(R.id.btn_back_arrow_settings);
        MaterialButton btnEmergencyContacts = findViewById(R.id.btn_ccc_emergency_contacts);
        MaterialButton btnCaseHistory = findViewById(R.id.btn_ccc_case_history);
        MaterialButton btnReportIssue = findViewById(R.id.btn_ccc_report_issue);
        MaterialButton btnNotifications = findViewById(R.id.btn_ccc_notifications);

        btnBack.setOnClickListener(v -> finish());
        btnEmergencyContacts.setOnClickListener(v ->
                startActivity(new Intent(this, EmergencyContactsActivity.class)));
        btnCaseHistory.setOnClickListener(v ->
                startActivity(new Intent(this, CccCaseQueueActivity.class)));
        btnReportIssue.setOnClickListener(v ->
                startActivity(new Intent(this, DisputeReportActivity.class)));
        btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
    }
}
