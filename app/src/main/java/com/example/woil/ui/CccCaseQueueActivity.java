
package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class CccCaseQueueActivity extends AppCompatActivity implements CccCaseAdapter.Listener {
    private ImageButton btnBack;
    private RecyclerView rvCases;
    private ProgressBar progress;
    private TextView tvEmpty;
    private final ArrayList<CccCaseModel> items = new ArrayList<>();
    private CccCaseAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ccc_case_queue);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        btnBack = findViewById(R.id.btn_back_arrow_settings);
        rvCases = findViewById(R.id.rv_ccc_cases);
        progress = findViewById(R.id.progress_cases);
        tvEmpty = findViewById(R.id.tv_empty_cases);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        adapter = new CccCaseAdapter(items, this);
        rvCases.setLayoutManager(new LinearLayoutManager(this));
        rvCases.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
    }

    @Override protected void onStart() {
        super.onStart();
        loadCases();
    }

    private void loadCases() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        progress.setVisibility(View.VISIBLE);
        db.collection("core_cases")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snap -> {
                    progress.setVisibility(View.GONE);
                    items.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd • hh:mm a", Locale.getDefault());
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String createdBy = doc.getString("createdByUid");
                        String againstUid = doc.getString("againstUid");
                        String workerUid = doc.getString("workerUid");
                        boolean visible = uid == null
                                || TextUtils.equals(createdBy, uid)
                                || TextUtils.equals(againstUid, uid)
                                || TextUtils.equals(workerUid, uid)
                                || auth.getCurrentUser().getPhoneNumber() == null; // loose fallback for prototype
                        if (!visible) continue;

                        String createdText = "";
                        if (doc.getTimestamp("createdAt") != null) {
                            createdText = sdf.format(doc.getTimestamp("createdAt").toDate());
                        }
                        items.add(new CccCaseModel(
                                doc.getId(),
                                doc.getString("caseType"),
                                first(doc.getString("title"), "CCC case"),
                                first(doc.getString("source"), "app"),
                                first(doc.getString("severity"), "LOW"),
                                first(doc.getString("status"), "OPEN"),
                                first(doc.getString("summary"), ""),
                                createdText,
                                doc.getString("sourceId")
                        ));
                    }
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Unable to load CCC cases.");
                });
    }

    @Override public void onCaseClick(CccCaseModel item) {
        Intent intent = new Intent(this, CccActivity.class);
        intent.putExtra("caseId", item.getCaseId());
        startActivity(intent);
    }

    private String first(String a, String fallback) { return TextUtils.isEmpty(a) ? fallback : a; }
}
