package com.example.woil.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
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

import java.util.ArrayList;
import java.util.UUID;

public class SkillShowcaseActivity extends AppCompatActivity implements SkillVideoAdapter.OnVideoActionListener {

    private RecyclerView recyclerSkillVideos;
    private TextView txtEmptyState;
    private MaterialButton btnAddVideo;
    private ImageButton btnBack;

    private final ArrayList<SkillVideo> skillVideoList = new ArrayList<>();
    private SkillVideoAdapter adapter;

    private int editingPosition = -1;

    private final ActivityResultLauncher<Intent> uploadLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                    Intent data = result.getData();
                    SkillVideo returnedVideo = (SkillVideo) data.getSerializableExtra("skill_video");
                    int returnedEditPosition = data.getIntExtra("edit_position", -1);

                    if (returnedVideo != null) {
                        if (returnedEditPosition >= 0 && returnedEditPosition < skillVideoList.size()) {
                            skillVideoList.set(returnedEditPosition, returnedVideo);
                            adapter.notifyItemChanged(returnedEditPosition);
                        } else {
                            skillVideoList.add(0, returnedVideo);
                            adapter.notifyItemInserted(0);
                        }
                        updateEmptyState();
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skill_showcase);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        recyclerSkillVideos = findViewById(R.id.recycler_skill_videos);
        txtEmptyState = findViewById(R.id.txt_empty_state);
        btnAddVideo = findViewById(R.id.btn_add_video);
        btnBack = findViewById(R.id.btn_back_arrow_settings);

        adapter = new SkillVideoAdapter(this, skillVideoList, this);
        recyclerSkillVideos.setLayoutManager(new LinearLayoutManager(this));
        recyclerSkillVideos.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnAddVideo.setOnClickListener(v -> {
            Intent intent = new Intent(SkillShowcaseActivity.this, UploadSkillVideoActivity.class);
            uploadLauncher.launch(intent);
        });

        updateEmptyState();
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
        Intent intent = new Intent(SkillShowcaseActivity.this, UploadSkillVideoActivity.class);
        intent.putExtra("skill_video", video);
        intent.putExtra("edit_position", position);
        uploadLauncher.launch(intent);
    }

    @Override
    public void onDelete(SkillVideo video, int position) {
        if (position >= 0 && position < skillVideoList.size()) {
            skillVideoList.remove(position);
            adapter.notifyItemRemoved(position);
            updateEmptyState();
        }
    }
}