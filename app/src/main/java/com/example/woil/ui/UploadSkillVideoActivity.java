package com.example.woil.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.database.Cursor;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
    private Spinner spinnerCategory;

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
                        videoPreview.setVideoURI(selectedVideoUri);
                        videoPreview.start();
                        txtVideoPlaceholder.setText("");
                        txtSelectedVideoName.setText("Selected video: " + getFileName(selectedVideoUri));
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_skill_video);

        btnBack = findViewById(R.id.btn_back_arrow_settings);
        videoPreview = findViewById(R.id.video_preview);
        txtVideoPlaceholder = findViewById(R.id.txt_video_placeholder);
        txtSelectedVideoName = findViewById(R.id.txt_selected_video_name);
        btnSelectVideo = findViewById(R.id.btn_select_video);
        btnUploadVideo = findViewById(R.id.btn_upload_video);
        edtTitle = findViewById(R.id.edt_title);
        edtDescription = findViewById(R.id.edt_description);
        spinnerCategory = findViewById(R.id.spinner_category);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        btnBack.setOnClickListener(v -> finish());

        btnSelectVideo.setOnClickListener(v -> openVideoPicker());

        btnUploadVideo.setOnClickListener(v -> validateAndReturnResult());

        readEditDataIfAvailable();
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickVideoLauncher.launch(intent);
    }

    private void readEditDataIfAvailable() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("skill_video")) {
            editVideo = (SkillVideo) intent.getSerializableExtra("skill_video");
            editPosition = intent.getIntExtra("edit_position", -1);

            if (editVideo != null) {
                edtTitle.setText(editVideo.getTitle());
                edtDescription.setText(editVideo.getDescription());

                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equalsIgnoreCase(editVideo.getCategory())) {
                        spinnerCategory.setSelection(i);
                        break;
                    }
                }

                if (editVideo.getVideoUriString() != null && !editVideo.getVideoUriString().isEmpty()) {
                    selectedVideoUri = Uri.parse(editVideo.getVideoUriString());
                    videoPreview.setVideoURI(selectedVideoUri);
                    txtVideoPlaceholder.setText("");
                    txtSelectedVideoName.setText("Selected video: " + getFileName(selectedVideoUri));
                }
            }
        }
    }

    private void validateAndReturnResult() {
        String title = edtTitle.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String description = edtDescription.getText().toString().trim();

        if (selectedVideoUri == null) {
            Toast.makeText(this, "Please select a video", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(title)) {
            edtTitle.setError("Enter title");
            edtTitle.requestFocus();
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