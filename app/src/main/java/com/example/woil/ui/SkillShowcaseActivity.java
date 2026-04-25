package com.example.woil.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;

public class SkillShowcaseActivity extends AppCompatActivity implements SkillVideoAdapter.OnVideoActionListener {

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
                if (result.getResultCode() == RESULT_OK) {
                    loadVideosFromFirebase();
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
        btnAddVideo.setOnClickListener(v -> uploadLauncher.launch(new Intent(this, UploadSkillVideoActivity.class)));

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

        videosListener = db.collection("profile_showcase_skill_videos")
                .whereEqualTo("uid", uid)
                .orderBy("uploadedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        FirebaseDebugLogger.failure("skill_video_listen", "profile_showcase_skill_videos?uid=" + uid, e);
                        Toast.makeText(this, "Failed to load videos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    skillVideoList.clear();
                    if (snap != null) {
                        FirebaseDebugLogger.read("skill_video_listen", "profile_showcase_skill_videos?uid=" + uid, snap.size());
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String title = doc.getString("title");
                            String category = doc.getString("category");
                            String description = doc.getString("description");
                            String status = doc.getString("status");
                            String videoUrl = doc.getString("videoUrl");
                            if (TextUtils.isEmpty(videoUrl)) videoUrl = doc.getString("videoUri");
                            skillVideoList.add(new SkillVideo(
                                    doc.getId(),
                                    TextUtils.isEmpty(title) ? "Skill video" : title,
                                    TextUtils.isEmpty(category) ? "Other" : category,
                                    TextUtils.isEmpty(description) ? "" : description,
                                    TextUtils.isEmpty(status) ? "PENDING" : status,
                                    videoUrl
                            ));
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
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
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_video_preview);
        VideoView dialogVideoView = dialog.findViewById(R.id.dialog_video_view);
        Uri videoUri = video.getVideoUri();
        if (videoUri != null) {
            dialogVideoView.setVideoURI(videoUri);
            dialogVideoView.start();
        }
        dialog.show();
    }

    @Override
    public void onEdit(SkillVideo video, int position) {
        Intent intent = new Intent(this, UploadSkillVideoActivity.class);
        intent.putExtra("skill_video", video);
        intent.putExtra("edit_position", position);
        uploadLauncher.launch(intent);
    }

    @Override
    public void onDelete(SkillVideo video, int position) {
        String uid = FirebaseDebugLogger.requireUid(this, mAuth, "skill_video_delete");
        if (uid == null || video == null || TextUtils.isEmpty(video.getId())) return;

        db.collection("profile_showcase_skill_videos").document(video.getId())
                .delete()
                .continueWithTask(task -> db.collection("media").document(video.getId()).delete())
                .addOnSuccessListener(unused -> {
                    FirebaseDebugLogger.success("skill_video_delete", "profile_showcase_skill_videos", video.getId());
                    Toast.makeText(this, "Video deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    FirebaseDebugLogger.failure("skill_video_delete", "profile_showcase_skill_videos/" + video.getId(), e);
                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
