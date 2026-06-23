package com.example.woil.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import com.example.woil.R;

import java.util.ArrayList;

public class VideoPlayerActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;
    private ImageButton btnBack;
    private ImageButton btnMute;
    private boolean isMuted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        // Enable edge-to-edge for true fullscreen
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        playerView = findViewById(R.id.player_view);
        btnBack = findViewById(R.id.btn_back_video);
        btnMute = findViewById(R.id.btn_mute);

        btnBack.setOnClickListener(v -> finish());
        
        btnMute.setOnClickListener(v -> {
            if (player != null) {
                isMuted = !isMuted;
                player.setVolume(isMuted ? 0f : 1f);
                // We can toggle icon here if needed
                Toast.makeText(this, isMuted ? "Muted" : "Unmuted", Toast.LENGTH_SHORT).show();
            }
        });

        initializePlayer();
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        playerView.setFullscreenButtonClickListener(isFullScreen -> {
            if (isFullScreen) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        });

        ArrayList<String> videoUrls = getIntent().getStringArrayListExtra("video_urls");
        int startIndex = getIntent().getIntExtra("start_index", 0);

        if (videoUrls == null || videoUrls.isEmpty()) {
            Toast.makeText(this, "No videos to play", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        for (String url : videoUrls) {
            MediaItem mediaItem = MediaItem.fromUri(url);
            player.addMediaItem(mediaItem);
        }

        player.prepare();
        player.seekTo(startIndex, 0);
        player.play();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
