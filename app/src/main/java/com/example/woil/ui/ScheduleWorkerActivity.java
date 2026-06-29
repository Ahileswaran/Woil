package com.example.woil.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ScheduleWorkerActivity extends AppCompatActivity {

    private TextView tvWorkerInfo;
    private EditText etDate;
    private EditText etTime;
    private MaterialButton btnConfirm;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String workerUid;
    private String workerName;
    private String workerSkill;
    private String clientAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_worker);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvWorkerInfo = findViewById(R.id.tv_worker_info);
        etDate = findViewById(R.id.et_schedule_date);
        etTime = findViewById(R.id.et_schedule_time);
        btnConfirm = findViewById(R.id.btn_confirm_schedule);

        if (getIntent() != null) {
            workerUid = getIntent().getStringExtra("workerUid");
            workerName = getIntent().getStringExtra("workerName");
            workerSkill = getIntent().getStringExtra("workerSkill");
            clientAddress = getIntent().getStringExtra("clientAddress");
            double dist = getIntent().getDoubleExtra("workerDistanceKm", 0);

            tvWorkerInfo.setText(workerName + "\n" + workerSkill + " - " + String.format("%.1f km away", dist));
        }

        btnConfirm.setOnClickListener(v -> scheduleJob());
    }

    private void scheduleJob() {
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();

        if (TextUtils.isEmpty(date) || TextUtils.isEmpty(time)) {
            Toast.makeText(this, "Please enter date and time", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (mAuth.getCurrentUser() == null) return;
        String clientId = mAuth.getCurrentUser().getUid();

        Map<String, Object> job = new HashMap<>();
        job.put("clientId", clientId);
        job.put("workerId", workerUid);
        job.put("workerName", workerName);
        job.put("skill", workerSkill);
        job.put("clientAddress", clientAddress);
        job.put("scheduledDate", date);
        job.put("scheduledTime", time);
        job.put("status", "SCHEDULED_PENDING");
        job.put("createdAt", System.currentTimeMillis());

        db.collection("scheduled_jobs").add(job)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Worker Scheduled Successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to schedule: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
