package com.example.woil.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import com.example.woil.R;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class SkillShowcase extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skillshowcase);

        // Set up toolbar with back navigation




    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}