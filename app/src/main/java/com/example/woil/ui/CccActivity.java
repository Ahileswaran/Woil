package com.example.woil.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.woil.R;

public class CccActivity extends AppCompatActivity {

    private TextView tvCaseSummary;
    private Button btnAck, btnEscalate, btnClose;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ccc);

        tvCaseSummary = findViewById(R.id.tvCaseSummary);
        btnAck = findViewById(R.id.btnAck);
        btnEscalate = findViewById(R.id.btnEscalate);
        btnClose = findViewById(R.id.btnClose);

        String incident = getIntent().getStringExtra("incidentType");
        String severity = getIntent().getStringExtra("severity");
        String state = getIntent().getStringExtra("state");

        tvCaseSummary.setText(
                "Incident: " + incident + "\n" +
                        "Severity: " + severity + "\n" +
                        "State: " + state + "\n" +
                        "Status: OPEN"
        );

        btnAck.setOnClickListener(v -> tvCaseSummary.append("\nAction: ACKNOWLEDGED"));
        btnEscalate.setOnClickListener(v -> tvCaseSummary.append("\nAction: ESCALATED"));
        btnClose.setOnClickListener(v -> tvCaseSummary.append("\nAction: CLOSED"));
    }
}