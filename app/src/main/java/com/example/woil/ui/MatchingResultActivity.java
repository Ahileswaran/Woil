package com.example.woil.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MatchingResultActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setText("Matching Result");
        tv.setTextSize(22f);
        tv.setPadding(40, 120, 40, 40);
        setContentView(tv);
    }
}