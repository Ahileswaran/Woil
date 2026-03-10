package com.example.woil.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.woil.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class PostJobActivity extends AppCompatActivity {

    private MaterialButton btnPostJob;
    private ImageButton btnBack;
    private android.widget.EditText etDescription;
    private android.widget.TextView tvLocation; // reference to "Set location" textview if used

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_job); // ensure this is the name of your post-job layout file

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnPostJob = findViewById(R.id.btn_post_job);
        btnBack = findViewById(R.id.btn_back);
        etDescription = findViewById(R.id.et_description);
        tvLocation = findViewById(R.id.tv_set_location);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnPostJob != null) {
            btnPostJob.setOnClickListener(v -> {
                String description = etDescription != null ? etDescription.getText().toString().trim() : "";
                String location = tvLocation != null ? tvLocation.getText().toString().trim() : "";

                if (TextUtils.isEmpty(description)) {
                    Toast.makeText(this, "Please enter a job description", Toast.LENGTH_SHORT).show();
                    return;
                }

                postJobToFirestore(description, location);
            });
        }
    }

    private void postJobToFirestore(String description, String location) {
        String uid = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : null;

        Map<String, Object> job = new HashMap<>();
        job.put("ownerId", uid);
        job.put("description", description);
        job.put("location", location);
        job.put("createdAt", Timestamp.now());
        job.put("status", "open");

        // optional extra fields you can add later: wage, dateTime, category, title, allowOffers, etc.

        db.collection("jobs")
                .add(job)
                .addOnCompleteListener(new OnCompleteListener<com.google.firebase.firestore.DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<com.google.firebase.firestore.DocumentReference> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(PostJobActivity.this, "Job posted", Toast.LENGTH_SHORT).show();
                            // you may want to return the new doc id to previous screen via setResult + Intent
                            finish();
                        } else {
                            Toast.makeText(PostJobActivity.this, "Failed to post job: " + (task.getException() != null ? task.getException().getMessage() : "unknown"), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}