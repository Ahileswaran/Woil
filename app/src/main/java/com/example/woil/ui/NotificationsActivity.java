
package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.Listener {

    private ImageButton btnBack;
    private RecyclerView rvNotifications;
    private TextView tvEmpty;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private NotificationAdapter adapter;
    private final ArrayList<NotificationItem> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        btnBack = findViewById(R.id.btn_back_arrow_settings);
        rvNotifications = findViewById(R.id.rv_notifications);
        tvEmpty = findViewById(R.id.tv_empty_notifications);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        btnBack.setOnClickListener(v -> finish());
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(items, this);
        rvNotifications.setAdapter(adapter);
    }

    @Override protected void onStart() {
        super.onStart();
        loadNotifications();
    }

    private void loadNotifications() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (TextUtils.isEmpty(uid)) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        db.collection("notifications")
                .whereEqualTo("uid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snap -> {
                    items.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd • hh:mm a", Locale.getDefault());
                    snap.getDocuments().forEach(doc -> {
                        String time = doc.getTimestamp("createdAt") != null ? sdf.format(doc.getTimestamp("createdAt").toDate()) : "";
                        items.add(new NotificationItem(
                                doc.getId(),
                                first(doc.getString("title"), "Notification"),
                                first(doc.getString("body"), ""),
                                time,
                                doc.getString("targetType"),
                                doc.getString("targetId")
                        ));
                    });
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> tvEmpty.setVisibility(View.VISIBLE));
    }

    @Override public void onNotificationClick(NotificationItem item) {
        if ("ccc_case".equalsIgnoreCase(item.getTargetType())) {
            Intent intent = new Intent(this, CccActivity.class);
            intent.putExtra("caseId", item.getTargetId());
            startActivity(intent);
        } else if ("chat".equalsIgnoreCase(item.getTargetType())) {
            startActivity(new Intent(this, MessageActivity.class));
        }
    }

    private String first(String a, String fallback) { return TextUtils.isEmpty(a) ? fallback : a; }
}
