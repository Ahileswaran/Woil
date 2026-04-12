package com.example.woil.models;

import android.os.Bundle;

import com.example.woil.R;


import androidx.appcompat.app.AppCompatActivity;

public class SkillShowcase extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skill_showcase);

        // Set up toolbar with back navigation




    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}