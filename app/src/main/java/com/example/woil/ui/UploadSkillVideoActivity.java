package com.example.woil.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;
import com.example.woil.models.SkillVideo;
import com.google.android.material.button.MaterialButton;

import java.util.UUID;

public class UploadSkillVideoActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private VideoView videoPreview;
    private TextView txtVideoPlaceholder;
    private TextView txtSelectedVideoName;
    private MaterialButton btnSelectVideo;
    private MaterialButton btnUploadVideo;
    private EditText edtTitle;
    private EditText edtDescription;
    private AutoCompleteTextView spinnerCategory;
    private LinearLayout videoPlaceholderContainer;

    private Uri selectedVideoUri;
    private SkillVideo editVideo;
    private int editPosition = -1;

    private final String[] categories = {
            "Cleaning",
            "Cooking",
            "Gardening",
            "Babysitting",
            "Elder Care",
            "Laundry",
            "Housekeeping",
            "Other"
    };

    private final ActivityResultLauncher<Intent> pickVideoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedVideoUri = result.getData().getData();

                    if (selectedVideoUri != null) {
                        getContentResolver().takePersistableUriPermission(
                                selectedVideoUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );

                        showSelectedVideo(selectedVideoUri);
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_skill_video);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        bindViews();
        setupCategoryDropdown();
        setupListeners();
        readEditDataIfAvailable();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btn_back_arrow_settings);
        videoPreview = findViewById(R.id.video_preview);
        txtVideoPlaceholder = findViewById(R.id.txt_video_placeholder);
        txtSelectedVideoName = findViewById(R.id.txt_selected_video_name);
        btnSelectVideo = findViewById(R.id.btn_select_video);
        btnUploadVideo = findViewById(R.id.btn_upload_video);
        edtTitle = findViewById(R.id.edt_title);
        edtDescription = findViewById(R.id.edt_description);
        spinnerCategory = findViewById(R.id.spinner_category);
        videoPlaceholderContainer = findViewById(R.id.video_placeholder_container);
    }

    private void setupCategoryDropdown() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        spinnerCategory.setAdapter(categoryAdapter);
        spinnerCategory.setThreshold(1);
        spinnerCategory.setText(categories[0], false);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSelectVideo.setOnClickListener(v -> openVideoPicker());

        btnUploadVideo.setOnClickListener(v -> validateAndReturnResult());

        videoPreview.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            videoPreview.start();
        });

        videoPreview.setOnClickListener(v -> {
            if (selectedVideoUri != null) {
                if (videoPreview.isPlaying()) {
                    videoPreview.pause();
                } else {
                    videoPreview.start();
                }
            }
        });

        spinnerCategory.setOnClickListener(v -> spinnerCategory.showDropDown());
        spinnerCategory.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                spinnerCategory.showDropDown();
            }
        });
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        pickVideoLauncher.launch(intent);
    }

    private void readEditDataIfAvailable() {
        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra("skill_video")) {
            return;
        }

        editVideo = (SkillVideo) intent.getSerializableExtra("skill_video");
        editPosition = intent.getIntExtra("edit_position", -1);

        if (editVideo == null) return;

        edtTitle.setText(editVideo.getTitle());
        edtDescription.setText(editVideo.getDescription());

        String editCategory = editVideo.getCategory();
        if (!TextUtils.isEmpty(editCategory)) {
            spinnerCategory.setText(editCategory, false);
        }

        if (!TextUtils.isEmpty(editVideo.getVideoUriString())) {
            selectedVideoUri = Uri.parse(editVideo.getVideoUriString());
            showSelectedVideo(selectedVideoUri);
        }
    }

    private void showSelectedVideo(Uri videoUri) {
        videoPreview.setVideoURI(videoUri);
        videoPreview.seekTo(150);

        if (videoPlaceholderContainer != null) {
            videoPlaceholderContainer.setVisibility(View.GONE);
        }

        if (txtVideoPlaceholder != null) {
            txtVideoPlaceholder.setText("");
        }

        txtSelectedVideoName.setText(getFileName(videoUri));
    }

    private void validateAndReturnResult() {
        String title = edtTitle.getText() != null ? edtTitle.getText().toString().trim() : "";
        String category = spinnerCategory.getText() != null ? spinnerCategory.getText().toString().trim() : "";
        String description = edtDescription.getText() != null ? edtDescription.getText().toString().trim() : "";

        if (selectedVideoUri == null) {
            Toast.makeText(this, "Please select a video", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(title)) {
            edtTitle.setError("Enter title");
            edtTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(category)) {
            spinnerCategory.setError("Select category");
            spinnerCategory.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            edtDescription.setError("Enter description");
            edtDescription.requestFocus();
            return;
        }

        SkillVideo resultVideo;
        if (editVideo != null) {
            resultVideo = new SkillVideo(
                    editVideo.getId(),
                    title,
                    category,
                    description,
                    editVideo.getStatus(),
                    selectedVideoUri.toString()
            );
        } else {
            resultVideo = new SkillVideo(
                    UUID.randomUUID().toString(),
                    title,
                    category,
                    description,
                    "PENDING",
                    selectedVideoUri.toString()
            );
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("skill_video", resultVideo);
        resultIntent.putExtra("edit_position", editPosition);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private String getFileName(Uri uri) {
        String result = "video_file";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (cursor.moveToFirst() && nameIndex >= 0) {
                result = cursor.getString(nameIndex);
            }
            cursor.close();
        }

        return result;
    }
}