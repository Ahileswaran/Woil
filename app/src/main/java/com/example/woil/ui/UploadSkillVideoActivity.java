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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
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

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private Uri selectedVideoUri;
    private SkillVideo editVideo;
    private int editPosition = -1;

    private final String[] categories = {
            "Cleaning", "Cooking", "Gardening", "Babysitting", "Elder Care", "Laundry", "Housekeeping", "Other"
    };

    private final ActivityResultLauncher<Intent> pickVideoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedVideoUri = result.getData().getData();
                    if (selectedVideoUri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(selectedVideoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException ignored) {
                        }
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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

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
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        spinnerCategory.setAdapter(categoryAdapter);
        spinnerCategory.setThreshold(1);
        spinnerCategory.setText(categories[0], false);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSelectVideo.setOnClickListener(v -> openVideoPicker());
        btnUploadVideo.setOnClickListener(v -> validateAndUploadToFirebase());
        videoPreview.setOnPreparedListener(mp -> { mp.setLooping(true); videoPreview.start(); });
        videoPreview.setOnClickListener(v -> {
            if (selectedVideoUri != null) {
                if (videoPreview.isPlaying()) videoPreview.pause(); else videoPreview.start();
            }
        });
        spinnerCategory.setOnClickListener(v -> spinnerCategory.showDropDown());
        spinnerCategory.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) spinnerCategory.showDropDown(); });
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
        if (intent == null || !intent.hasExtra("skill_video")) return;
        editVideo = (SkillVideo) intent.getSerializableExtra("skill_video");
        editPosition = intent.getIntExtra("edit_position", -1);
        if (editVideo == null) return;
        edtTitle.setText(editVideo.getTitle());
        edtDescription.setText(editVideo.getDescription());
        if (!TextUtils.isEmpty(editVideo.getCategory())) spinnerCategory.setText(editVideo.getCategory(), false);
        if (!TextUtils.isEmpty(editVideo.getVideoUriString())) {
            selectedVideoUri = Uri.parse(editVideo.getVideoUriString());
            showSelectedVideo(selectedVideoUri);
        }
    }

    private void showSelectedVideo(Uri videoUri) {
        videoPreview.setVideoURI(videoUri);
        videoPreview.seekTo(150);
        if (videoPlaceholderContainer != null) videoPlaceholderContainer.setVisibility(View.GONE);
        if (txtVideoPlaceholder != null) txtVideoPlaceholder.setText("");
        txtSelectedVideoName.setText(getFileName(videoUri));
    }

    private void validateAndUploadToFirebase() {
        String uid = FirebaseDebugLogger.requireUid(this, mAuth, "skill_video_upload");
        if (uid == null) return;

        String title = edtTitle.getText() != null ? edtTitle.getText().toString().trim() : "";
        String category = spinnerCategory.getText() != null ? spinnerCategory.getText().toString().trim() : "";
        String description = edtDescription.getText() != null ? edtDescription.getText().toString().trim() : "";

        if (selectedVideoUri == null) { Toast.makeText(this, "Please select a video", Toast.LENGTH_SHORT).show(); return; }
        if (TextUtils.isEmpty(title)) { edtTitle.setError("Enter title"); edtTitle.requestFocus(); return; }
        if (TextUtils.isEmpty(category)) { spinnerCategory.setError("Select category"); spinnerCategory.requestFocus(); return; }
        if (TextUtils.isEmpty(description)) { edtDescription.setError("Enter description"); edtDescription.requestFocus(); return; }

        btnUploadVideo.setEnabled(false);
        btnUploadVideo.setText("Uploading...");

        String videoId = editVideo != null && !TextUtils.isEmpty(editVideo.getId()) ? editVideo.getId() : UUID.randomUUID().toString();
        String path = "skill_videos/" + uid + "/" + videoId + ".mp4";
        StorageReference ref = storage.getReference().child(path);

        ref.putFile(selectedVideoUri)
                .addOnProgressListener(snapshot -> FirebaseDebugLogger.read("skill_video_upload_progress", path,
                        (int) Math.min(100, (100 * snapshot.getBytesTransferred()) / Math.max(1, snapshot.getTotalByteCount()))))
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> writeSkillVideoDocs(uid, videoId, title, category, description, downloadUri.toString(), path))
                .addOnFailureListener(e -> {
                    btnUploadVideo.setEnabled(true);
                    btnUploadVideo.setText("Upload Video");
                    FirebaseDebugLogger.failure("skill_video_upload", path, e);
                    Toast.makeText(this, "Video upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void writeSkillVideoDocs(String uid, String videoId, String title, String category,
                                     String description, String downloadUrl, String storagePath) {
        Map<String, Object> media = new HashMap<>();
        media.put("uid", uid);
        media.put("ownerUid", uid);
        media.put("type", "skill_video");
        media.put("title", title);
        media.put("category", category);
        media.put("description", description);
        media.put("storagePath", storagePath);
        media.put("url", downloadUrl);
        media.put("createdAt", FieldValue.serverTimestamp());
        media.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> skillVideo = new HashMap<>();
        skillVideo.put("uid", uid);
        skillVideo.put("videoUrl", downloadUrl);
        skillVideo.put("videoUri", downloadUrl);
        skillVideo.put("title", title);
        skillVideo.put("category", category);
        skillVideo.put("description", description);
        skillVideo.put("status", "PENDING");
        skillVideo.put("storagePath", storagePath);
        skillVideo.put("uploadedAt", FieldValue.serverTimestamp());
        skillVideo.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("media").document(videoId).set(media, SetOptions.merge())
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) throw task.getException();
                    return db.collection("profile_showcase_skill_videos").document(videoId).set(skillVideo, SetOptions.merge());
                })
                .addOnSuccessListener(unused -> {
                    FirebaseDebugLogger.success("skill_video_firestore_write", "profile_showcase_skill_videos", videoId);
                    SkillVideo resultVideo = new SkillVideo(videoId, title, category, description, "PENDING", downloadUrl);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("skill_video", resultVideo);
                    resultIntent.putExtra("edit_position", editPosition);
                    setResult(RESULT_OK, resultIntent);
                    Toast.makeText(this, "Video submitted for approval", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnUploadVideo.setEnabled(true);
                    btnUploadVideo.setText("Upload Video");
                    FirebaseDebugLogger.failure("skill_video_firestore_write", "profile_showcase_skill_videos/" + videoId, e);
                    Toast.makeText(this, "Firestore save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String getFileName(Uri uri) {
        String result = "video_file";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (cursor.moveToFirst() && nameIndex >= 0) result = cursor.getString(nameIndex);
            cursor.close();
        }
        return result;
    }
}
