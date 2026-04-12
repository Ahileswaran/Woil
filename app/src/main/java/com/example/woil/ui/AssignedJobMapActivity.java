package com.example.woil.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.google.android.gms.maps.OnMapReadyCallback;

public class AssignedJobMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView tvEta, tvDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assigned_job_map);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        tvEta = findViewById(R.id.tv_eta);
        tvDistance = findViewById(R.id.tv_distance);

        tvEta.setText("ETA: calculating...");
        tvDistance.setText("Distance: calculating...");
    }

    @Override
    public void onMapReady(com.google.android.gms.maps.GoogleMap googleMap) {
        // TODO add worker/client markers and route polyline later
    }
}