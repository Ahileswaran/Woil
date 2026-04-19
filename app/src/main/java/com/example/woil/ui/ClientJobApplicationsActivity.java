package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ClientJobApplicationsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvJobSummary;
    private RecyclerView rvApplications;

    private FirebaseFirestore db;
    private String jobId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_job_applications);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        btnBack = findViewById(R.id.btn_back_arrow_settings);
        tvJobSummary = findViewById(R.id.tv_job_summary);
        rvApplications = findViewById(R.id.rv_applications);

        db = FirebaseFirestore.getInstance();
        jobId = getIntent().getStringExtra("jobId");

        btnBack.setOnClickListener(v -> finish());

        rvApplications.setLayoutManager(new LinearLayoutManager(this));
        // TODO: attach adapter and load matches where jobId == current jobId
    }

    private void acceptWorker(String matchId, String workerUid) {
        // TODO:
        // 1. matches/{matchId}.status = ACCEPTED
        // 2. jobs/{jobId}.assignedUid = workerUid
        // 3. jobs/{jobId}.status = MATCHED
        // 4. create notification for worker
        Toast.makeText(this, "Accept worker: " + workerUid, Toast.LENGTH_SHORT).show();
    }



    private void rejectWorker(String matchId, String workerUid) {
        // TODO:
        // matches/{matchId}.status = REJECTED
        // create notification for worker
        Toast.makeText(this, "Reject worker: " + workerUid, Toast.LENGTH_SHORT).show();
    }
}