package com.example.woil.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.R;
import com.example.woil.adapters.SkillVideoAdapter;
import com.example.woil.models.SkillVideo;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SkillShowcaseActivity extends AppCompatActivity implements SkillVideoAdapter.OnVideoActionListener {

    private static final String TAG = "SkillShowcaseActivity";

    private RecyclerView recyclerSkillVideos;
    private TextView txtEmptyState;
    private MaterialButton btnAddVideo;
    private ImageButton btnBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration videosListener;

    private final ArrayList<SkillVideo> skillVideoList = new ArrayList<>();
    private SkillVideoAdapter adapter;

    private final ActivityResultLauncher<Intent> uploadLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Log.d(TAG, "Upload activity returned resultCode=" + result.getResultCode());
                if (result.getResultCode() == RESULT_OK) {
                    FirebaseDebugLogger.success("skill_video_upload_flow", "UploadSkillVideoActivity", "RESULT_OK");
                    loadVideosFromFirebase();
                } else {
                    FirebaseDebugLogger.failure(
                            "skill_video_upload_flow",
                            "UploadSkillVideoActivity",
                            new RuntimeException("Upload activity returned non-OK result: " + result.getResultCode())
                    );
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skill_showcase);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerSkillVideos = findViewById(R.id.recycler_skill_videos);
        txtEmptyState = findViewById(R.id.txt_empty_state);
        btnAddVideo = findViewById(R.id.btn_add_video);
        btnBack = findViewById(R.id.btn_back_arrow_settings);

        adapter = new SkillVideoAdapter(this, skillVideoList, this);
        recyclerSkillVideos.setLayoutManager(new LinearLayoutManager(this));
        recyclerSkillVideos.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnAddVideo.setOnClickListener(v -> {
            Log.d(TAG, "Add video clicked. Opening UploadSkillVideoActivity");
            FirebaseDebugLogger.success("skill_video_add_click", "UploadSkillVideoActivity", "launch");
            uploadLauncher.launch(new Intent(this, UploadSkillVideoActivity.class));
        });

        updateEmptyState();
        loadVideosFromFirebase();
    }

    private void loadVideosFromFirebase() {
        String uid = FirebaseDebugLogger.requireUid(this, mAuth, "skill_video_listen");
        if (uid == null) return;

        if (videosListener != null) {
            videosListener.remove();
            videosListener = null;
        }

        Log.d(TAG, "Loading showcase videos for uid=" + uid);
        videosListener = attachSkillVideoListener(uid, true);
    }

    private ListenerRegistration attachSkillVideoListener(String uid, boolean useOrderBy) {
        Query query = db.collection("profile_showcase_skill_videos")
                .whereEqualTo("uid", uid);

        if (useOrderBy) {
            query = query.orderBy("uploadedAt", Query.Direction.DESCENDING);
        }

        final String queryLabel = useOrderBy
                ? "profile_showcase_skill_videos?uid=" + uid + "&orderBy=uploadedAt_desc"
                : "profile_showcase_skill_videos?uid=" + uid + "&fallbackSort=client_side";

        return query.addSnapshotListener((snap, e) -> {
            if (e != null) {
                FirebaseDebugLogger.failure("skill_video_listen", queryLabel, e);
                Log.e(TAG, "Skill videos query failed (" + queryLabel + ")", e);

                if (shouldFallbackToClientSort(e, useOrderBy)) {
                    Log.w(TAG, "Query requires an index. Falling back to client-side query sorting.");
                    FirebaseDebugLogger.success("skill_video_listen_fallback", queryLabel, "using unordered query");
                    if (videosListener != null) {
                        videosListener.remove();
                        videosListener = null;
                    }
                    videosListener = attachSkillVideoListener(uid, false);
                    return;
                }

                Toast.makeText(this,
                        "Failed to load videos: " + friendlyMessage(e),
                        Toast.LENGTH_LONG).show();
                return;
            }

            skillVideoList.clear();

            if (snap == null) {
                Log.w(TAG, "Skill showcase snapshot is null for uid=" + uid);
                FirebaseDebugLogger.read("skill_video_listen", queryLabel, 0);
                adapter.notifyDataSetChanged();
                updateEmptyState();
                return;
            }

            FirebaseDebugLogger.read("skill_video_listen", queryLabel, snap.size());
            Log.d(TAG, String.format(Locale.getDefault(),
                    "Loaded %d skill showcase documents for uid=%s", snap.size(), uid));

            List<SkillVideo> temp = new ArrayList<>();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                String title = doc.getString("title");
                String category = doc.getString("category");
                String description = doc.getString("description");
                String status = doc.getString("status");
                String videoUrl = doc.getString("videoUrl");
                if (TextUtils.isEmpty(videoUrl)) videoUrl = doc.getString("videoUri");
                temp.add(new SkillVideo(
                        doc.getId(),
                        TextUtils.isEmpty(title) ? "Skill video" : title,
                        TextUtils.isEmpty(category) ? "Other" : category,
                        TextUtils.isEmpty(description) ? "" : description,
                        TextUtils.isEmpty(status) ? "PENDING" : status,
                        videoUrl
                ));
            }

            if (!useOrderBy) {
                Collections.sort(temp, (a, b) -> {
                    int byTitle = safe(a.getTitle()).compareToIgnoreCase(safe(b.getTitle()));
                    if (byTitle != 0) return byTitle;
                    return safe(a.getStatus()).compareToIgnoreCase(safe(b.getStatus()));
                });
            }

            skillVideoList.addAll(temp);
            adapter.notifyDataSetChanged();
            updateEmptyState();
        });
    }

    private boolean shouldFallbackToClientSort(Exception e, boolean usedOrderedQuery) {
        if (!usedOrderedQuery) return false;
        if (!(e instanceof FirebaseFirestoreException)) return false;

        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
        FirebaseFirestoreException fse = (FirebaseFirestoreException) e;
        return fse.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION
                || message.contains("requires an index")
                || message.contains("index");
    }

    private String friendlyMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) return "Unknown error";
        if (message.contains("requires an index")) {
            return "Missing Firestore index";
        }
        if (message.contains("permission-denied")) {
            return "Permission denied";
        }
        return message;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private void updateEmptyState() {
        if (skillVideoList.isEmpty()) {
            txtEmptyState.setVisibility(TextView.VISIBLE);
            recyclerSkillVideos.setVisibility(RecyclerView.GONE);
        } else {
            txtEmptyState.setVisibility(TextView.GONE);
            recyclerSkillVideos.setVisibility(RecyclerView.VISIBLE);
        }
    }

    @Override
    public void onPreview(SkillVideo video) {
        Log.d(TAG, "Preview requested for skill video id=" + (video != null ? video.getId() : "null"));
        if (video == null || video.getVideoUri() == null) {
            Toast.makeText(this, "No video URL available for preview", Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<String> videoUrls = new ArrayList<>();
        int startIndex = 0;
        for (int i = 0; i < skillVideoList.size(); i++) {
            SkillVideo v = skillVideoList.get(i);
            if (v.getVideoUri() != null) {
                videoUrls.add(v.getVideoUri().toString());
                if (v.getId().equals(video.getId())) {
                    startIndex = videoUrls.size() - 1;
                }
            }
        }
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putStringArrayListExtra("video_urls", videoUrls);
        intent.putExtra("start_index", startIndex);
        startActivity(intent);
    }

    @Override
    public void onEdit(SkillVideo video, int position) {
        Log.d(TAG, "Edit requested for skill video id=" + (video != null ? video.getId() : "null") + ", position=" + position);
        Intent intent = new Intent(this, UploadSkillVideoActivity.class);
        intent.putExtra("skill_video", video);
        intent.putExtra("edit_position", position);
        uploadLauncher.launch(intent);
    }

    @Override
    public void onDelete(SkillVideo video, int position) {
        String uid = FirebaseDebugLogger.requireUid(this, mAuth, "skill_video_delete");
        if (uid == null || video == null || TextUtils.isEmpty(video.getId())) {
            Log.w(TAG, "Delete aborted. uid=" + uid + ", video=" + (video == null ? "null" : video.getId()));
            return;
        }

        Log.d(TAG, "Deleting skill showcase video id=" + video.getId() + " at position=" + position);

        db.collection("profile_showcase_skill_videos").document(video.getId())
                .delete()
                .continueWithTask(task -> db.collection("media").document(video.getId()).delete())
                .addOnSuccessListener(unused -> {
                    FirebaseDebugLogger.success("skill_video_delete", "profile_showcase_skill_videos", video.getId());
                    Toast.makeText(this, "Video deleted", Toast.LENGTH_SHORT).show();
                    loadVideosFromFirebase();
                })
                .addOnFailureListener(e -> {
                    FirebaseDebugLogger.failure("skill_video_delete", "profile_showcase_skill_videos/" + video.getId(), e);
                    Log.e(TAG, "Delete failed for id=" + video.getId(), e);
                    Toast.makeText(this, "Delete failed: " + friendlyMessage(e), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videosListener != null) {
            videosListener.remove();
            videosListener = null;
        }
    }
}
