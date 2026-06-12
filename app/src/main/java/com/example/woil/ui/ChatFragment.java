package com.example.woil.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.woil.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatFragment extends Fragment {

    public static final String ARG_CHAT_ID = "chat_id";
    public static final String ARG_CONTACT_UID = "contact_uid";
    public static final String ARG_CONTACT_NAME = "contact_name";
    public static final String ARG_CONTACT_ROLE = "contact_role";
    public static final String ARG_CONTACT_PHOTO = "contact_photo";
    public static final String ARG_JOB_ID = "job_id";

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ProgressBar progressMessages;
    private TextView tvEmptyMessages;

    private ImageButton btnSend;
    private ImageButton btnBack;
    private ImageButton btnCall;
    private ImageButton btnVideo;
    private ImageButton btnMore;
    private ImageButton btnCamera;
    private ImageButton btnAttach;

    private ImageView ivProfileImage;
    private TextView tvContactName;
    private TextView tvContactRole;
    private View inputWrapper;

    private ChatAdapter chatAdapter;
    private final List<Message> messageList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration messageListener;
    private String chatId;
    private String jobId;

    private String currentUid;
    private String contactUid;
    private String contactNameArg;
    private String contactRoleArg;
    private String contactPhotoArg;
    private String receiverPhotoUrl;

    public ChatFragment() {
    }

    public static ChatFragment newInstance(String chatId,
                                           String contactUid,
                                           String contactName,
                                           String contactRole,
                                           String contactPhoto,
                                           String jobId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAT_ID, chatId);
        args.putString(ARG_CONTACT_UID, contactUid);
        args.putString(ARG_CONTACT_NAME, contactName);
        args.putString(ARG_CONTACT_ROLE, contactRole);
        args.putString(ARG_CONTACT_PHOTO, contactPhoto);
        args.putString(ARG_JOB_ID, jobId);
        fragment.setArguments(args);
        return fragment;
    }

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    Toast.makeText(requireContext(), "Camera capture ready. Attach upload can be added next.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCameraIntent();
                } else {
                    Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK &&
                        result.getData() != null &&
                        result.getData().getData() != null) {
                    Uri selectedFileUri = result.getData().getData();
                    String fileName = selectedFileUri.getLastPathSegment();
                    Toast.makeText(requireContext(), TextUtils.isEmpty(fileName) ? "File selected" : fileName, Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        readArguments();
        bindViews(view);
        setupWindowInsets(view);
        setupRecyclerView();
        setupClickListeners();
        loadChatHeader();
        resolveChatAndListen();
    }

    private void readArguments() {
        Bundle args = getArguments();
        if (args == null) return;

        chatId = args.getString(ARG_CHAT_ID);
        jobId = args.getString(ARG_JOB_ID);
        contactUid = args.getString(ARG_CONTACT_UID);
        contactNameArg = args.getString(ARG_CONTACT_NAME);
        contactRoleArg = args.getString(ARG_CONTACT_ROLE);
        contactPhotoArg = args.getString(ARG_CONTACT_PHOTO);
    }

    private void bindViews(@NonNull View view) {
        rvMessages = view.findViewById(R.id.rv_messages);
        etMessage = view.findViewById(R.id.et_message);
        progressMessages = view.findViewById(R.id.progress_messages);
        tvEmptyMessages = view.findViewById(R.id.tv_empty_messages);

        btnSend = view.findViewById(R.id.btn_send);
        btnBack = view.findViewById(R.id.btn_back);
        btnCall = view.findViewById(R.id.btn_call);
        btnVideo = view.findViewById(R.id.btn_video);
        btnMore = view.findViewById(R.id.btn_more);
        btnCamera = view.findViewById(R.id.btn_camera);
        btnAttach = view.findViewById(R.id.btn_attach);

        ivProfileImage = view.findViewById(R.id.profile_image_card);
        tvContactName = view.findViewById(R.id.contact_name_card);
        tvContactRole = view.findViewById(R.id.contact_role_card);
        inputWrapper = view.findViewById(R.id.input_wrapper);
    }

    private void setupWindowInsets(@NonNull View root) {
        final int inputStart = inputWrapper.getPaddingStart();
        final int inputTop = inputWrapper.getPaddingTop();
        final int inputEnd = inputWrapper.getPaddingEnd();
        final int inputBottom = inputWrapper.getPaddingBottom();

        final int rvStart = rvMessages.getPaddingStart();
        final int rvTop = rvMessages.getPaddingTop();
        final int rvEnd = rvMessages.getPaddingEnd();
        final int rvBottom = rvMessages.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(systemBars.bottom, ime.bottom);

            inputWrapper.setPadding(inputStart, inputTop, inputEnd, inputBottom + bottomInset);
            rvMessages.setPadding(rvStart, rvTop, rvEnd, rvBottom + bottomInset + dpToPx(8));
            scrollToBottom();
            return insets;
        });
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendMessage());
        btnBack.setOnClickListener(v -> requireActivity().finish());
        btnCall.setOnClickListener(v -> {
            if (TextUtils.isEmpty(contactUid)) {
                Toast.makeText(requireContext(), "Cannot start call: invalid contact Uid", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(requireContext(), CallActivity.class);
            intent.putExtra("contactUid", contactUid);
            intent.putExtra("contactName", contactNameArg);
            intent.putExtra("callType", "audio");
            startActivity(intent);
        });
        btnVideo.setOnClickListener(v -> {
            if (TextUtils.isEmpty(contactUid)) {
                Toast.makeText(requireContext(), "Cannot start call: invalid contact Uid", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(requireContext(), CallActivity.class);
            intent.putExtra("contactUid", contactUid);
            intent.putExtra("contactName", contactNameArg);
            intent.putExtra("callType", "video");
            startActivity(intent);
        });
        btnMore.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), DisputeReportActivity.class);
            intent.putExtra(DisputeReportActivity.EXTRA_JOB_ID, jobId);
            intent.putExtra(DisputeReportActivity.EXTRA_AGAINST_UID, contactUid);
            intent.putExtra(DisputeReportActivity.EXTRA_SOURCE, "chat");
            startActivity(intent);
        });
        btnCamera.setOnClickListener(v -> openCamera());
        btnAttach.setOnClickListener(v -> openFilePicker());
    }

    private void resolveChatAndListen() {
        if (TextUtils.isEmpty(currentUid) || TextUtils.isEmpty(contactUid)) {
            progressMessages.setVisibility(View.GONE);
            tvEmptyMessages.setVisibility(View.VISIBLE);
            tvEmptyMessages.setText("Chat is unavailable until both users are identified.");
            return;
        }

        if (TextUtils.isEmpty(chatId)) {
            chatId = buildChatId(currentUid, contactUid);
        }
        ensureChatDocument().addOnCompleteListener(task -> listenForMessages());
    }

    private Task<Void> ensureChatDocument() {
        Map<String, Object> chat = new HashMap<>();
        chat.put("chatId", chatId);
        chat.put("participants", Arrays.asList(currentUid, contactUid));
        chat.put("createdAt", FieldValue.serverTimestamp());
        chat.put("updatedAt", FieldValue.serverTimestamp());
        if (!TextUtils.isEmpty(jobId)) {
            chat.put("jobId", jobId);
        }
        if (!TextUtils.isEmpty(contactNameArg)) {
            Map<String, Object> participantNames = new HashMap<>();
            participantNames.put(contactUid, contactNameArg);
            chat.put("participantNames", participantNames);
        }
        if (!TextUtils.isEmpty(contactPhotoArg)) {
            Map<String, Object> participantPhotos = new HashMap<>();
            participantPhotos.put(contactUid, contactPhotoArg);
            chat.put("participantPhotos", participantPhotos);
        }
        Map<String, Object> unread = new HashMap<>();
        unread.put(currentUid, 0L);
        unread.put(contactUid, 0L);
        chat.put("unreadCount", unread);
        return db.collection("chats").document(chatId).set(chat, SetOptions.merge());
    }

    private void listenForMessages() {
        if (TextUtils.isEmpty(chatId)) return;
        progressMessages.setVisibility(View.VISIBLE);
        tvEmptyMessages.setVisibility(View.GONE);

        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }

        messageListener = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    progressMessages.setVisibility(View.GONE);
                    if (e != null) {
                        tvEmptyMessages.setVisibility(View.VISIBLE);
                        tvEmptyMessages.setText("Unable to load messages.");
                        Toast.makeText(requireContext(), "Messages failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    messageList.clear();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String text = doc.getString("content");
                            String senderUid = doc.getString("senderUid");
                            String type = firstNonEmpty(doc.getString("type"), "text");
                            if (TextUtils.isEmpty(text) && TextUtils.equals(type, "text")) {
                                continue;
                            }
                            Timestamp createdAt = doc.getTimestamp("createdAt");
                            long createdAtMillis = createdAt != null ? createdAt.toDate().getTime() : 0L;
                            String time = formatTime(createdAtMillis);
                            if (TextUtils.isEmpty(time)) {
                                time = firstNonEmpty(doc.getString("timeText"), "Sending...");
                            }
                            String photoUrl = TextUtils.equals(senderUid, contactUid) ? receiverPhotoUrl : null;
                            messageList.add(new Message(
                                    doc.getId(),
                                    firstNonEmpty(text, ""),
                                    time,
                                    TextUtils.equals(currentUid, senderUid),
                                    senderUid,
                                    photoUrl,
                                    createdAtMillis,
                                    type
                            ));
                        }
                    }

                    chatAdapter.notifyDataSetChanged();
                    if (messageList.isEmpty()) {
                        tvEmptyMessages.setVisibility(View.VISIBLE);
                        tvEmptyMessages.setText("No messages yet. Start the conversation.");
                    } else {
                        tvEmptyMessages.setVisibility(View.GONE);
                    }
                    scrollToBottom();
                    markChatRead();
                });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            return;
        }
        if (TextUtils.isEmpty(currentUid) || TextUtils.isEmpty(contactUid) || TextUtils.isEmpty(chatId)) {
            Toast.makeText(requireContext(), "Unable to send message right now.", Toast.LENGTH_SHORT).show();
            return;
        }

        etMessage.setText("");

        DocumentReference messageRef = db.collection("chats").document(chatId).collection("messages").document();
        Map<String, Object> message = new HashMap<>();
        message.put("messageId", messageRef.getId());
        message.put("chatId", chatId);
        message.put("senderUid", currentUid);
        message.put("receiverUid", contactUid);
        message.put("type", "text");
        message.put("content", text);
        message.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> chat = new HashMap<>();
        chat.put("chatId", chatId);
        chat.put("participants", Arrays.asList(currentUid, contactUid));
        chat.put("lastMessage", text);
        chat.put("lastMessageText", text);
        chat.put("lastMessageType", "text");
        chat.put("lastSenderUid", currentUid);
        chat.put("lastMessageAt", FieldValue.serverTimestamp());
        chat.put("updatedAt", FieldValue.serverTimestamp());
        chat.put("createdAt", FieldValue.serverTimestamp());
        if (!TextUtils.isEmpty(jobId)) {
            chat.put("jobId", jobId);
        }
        chat.put("unreadCount." + currentUid, 0L);
        chat.put("unreadCount." + contactUid, FieldValue.increment(1));

        db.collection("chats")
                .document(chatId)
                .set(chat, SetOptions.merge())
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return messageRef.set(message);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Message failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    etMessage.setText(text);
                    etMessage.setSelection(etMessage.getText().length());
                });
    }

    private void markChatRead() {
        if (TextUtils.isEmpty(chatId) || TextUtils.isEmpty(currentUid)) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("unreadCount." + currentUid, 0L);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("chats").document(chatId).set(updates, SetOptions.merge());
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCameraIntent();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            cameraLauncher.launch(intent);
        } else {
            Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            filePickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open file picker", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildChatId(String uid1, String uid2) {
        if (TextUtils.isEmpty(uid1) || TextUtils.isEmpty(uid2)) return null;
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    private void loadChatHeader() {
        if (!TextUtils.isEmpty(contactUid)) {
            db.collection("profiles")
                    .document(contactUid)
                    .get()
                    .addOnSuccessListener(this::bindProfileToHeader)
                    .addOnFailureListener(e -> bindHeaderFromArguments());
            return;
        }
        bindHeaderFromArguments();
    }

    private void bindHeaderFromArguments() {
        String name = TextUtils.isEmpty(contactNameArg) ? "User" : contactNameArg.trim();
        String role = TextUtils.isEmpty(contactRoleArg) ? "Participant" : capitalize(contactRoleArg.trim());
        tvContactName.setText(name);
        tvContactRole.setText(role);
        receiverPhotoUrl = contactPhotoArg;
        loadImageIntoHeader(contactPhotoArg);
    }

    private void bindProfileToHeader(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            bindHeaderFromArguments();
            return;
        }

        String fullName = buildDisplayName(snapshot);
        String role = firstNonEmpty(snapshot.getString("role"), contactRoleArg, "Participant");
        receiverPhotoUrl = firstNonEmpty(snapshot.getString("photoUrl"), snapshot.getString("photo"), snapshot.getString("avatar"), contactPhotoArg);

        tvContactName.setText(firstNonEmpty(fullName, "User"));
        tvContactRole.setText(capitalize(role));
        loadImageIntoHeader(receiverPhotoUrl);
    }

    private String buildDisplayName(DocumentSnapshot snapshot) {
        String displayName = snapshot.getString("displayName");
        if (!TextUtils.isEmpty(displayName)) return displayName.trim();
        String firstName = snapshot.getString("firstName");
        String lastName = snapshot.getString("lastName");
        String full = ((firstName == null ? "" : firstName.trim()) + " " + (lastName == null ? "" : lastName.trim())).trim();
        return TextUtils.isEmpty(full) ? null : full;
    }

    private void loadImageIntoHeader(@Nullable String photoUrl) {
        if (!isAdded() || ivProfileImage == null) return;
        if (TextUtils.isEmpty(photoUrl)) {
            ivProfileImage.setImageResource(R.drawable.photo_placeholder);
            return;
        }
        Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.photo_placeholder)
                .error(R.drawable.photo_placeholder)
                .into(ivProfileImage);
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) return value;
        }
        return null;
    }

    private String capitalize(String value) {
        if (TextUtils.isEmpty(value)) return "Participant";
        return value.substring(0, 1).toUpperCase(Locale.getDefault()) + value.substring(1);
    }

    private String formatTime(long millis) {
        if (millis <= 0L) return null;
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(millis));
    }

    private void scrollToBottom() {
        if (rvMessages != null && !messageList.isEmpty()) {
            rvMessages.post(() -> rvMessages.scrollToPosition(messageList.size() - 1));
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
    }
}
