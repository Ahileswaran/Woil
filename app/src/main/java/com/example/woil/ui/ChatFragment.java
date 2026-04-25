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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatFragment extends Fragment {

    public static final String ARG_CONTACT_UID = "contact_uid";
    public static final String ARG_CONTACT_NAME = "contact_name";
    public static final String ARG_CONTACT_ROLE = "contact_role";
    public static final String ARG_CONTACT_PHOTO = "contact_photo";

    private RecyclerView rvMessages;
    private EditText etMessage;

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

    private String contactUid;
    private String contactNameArg;
    private String contactRoleArg;
    private String contactPhotoArg;

    public ChatFragment() {
    }

    public static ChatFragment newInstance(String contactUid, String contactName,
                                           String contactRole, String contactPhoto) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONTACT_UID, contactUid);
        args.putString(ARG_CONTACT_NAME, contactName);
        args.putString(ARG_CONTACT_ROLE, contactRole);
        args.putString(ARG_CONTACT_PHOTO, contactPhoto);
        fragment.setArguments(args);
        return fragment;
    }

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    Toast.makeText(requireContext(), "Image captured", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Camera cancelled", Toast.LENGTH_SHORT).show();
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

                    if (TextUtils.isEmpty(fileName)) {
                        fileName = "File selected";
                    }

                    Toast.makeText(requireContext(), fileName, Toast.LENGTH_SHORT).show();
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

        readArguments();
        bindViews(view);
        setupWindowInsets(view);
        setupRecyclerView();
        setupClickListeners();
        loadChatHeader();
        listenForMessages();
    }

    private void readArguments() {
        Bundle args = getArguments();
        if (args == null) return;

        contactUid = args.getString(ARG_CONTACT_UID);
        contactNameArg = args.getString(ARG_CONTACT_NAME);
        contactRoleArg = args.getString(ARG_CONTACT_ROLE);
        contactPhotoArg = args.getString(ARG_CONTACT_PHOTO);
    }

    private void bindViews(@NonNull View view) {
        rvMessages = view.findViewById(R.id.rv_messages);
        etMessage = view.findViewById(R.id.et_message);

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

        btnBack.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).onFragmentArrowBackToHome();
            } else {
                requireActivity().onBackPressed();
            }
        });

        btnCall.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Call button clicked", Toast.LENGTH_SHORT).show()
        );

        btnVideo.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Video button clicked", Toast.LENGTH_SHORT).show()
        );

        btnMore.setOnClickListener(v ->
                Toast.makeText(requireContext(), "More options", Toast.LENGTH_SHORT).show()
        );

        btnCamera.setOnClickListener(v -> openCamera());
        btnAttach.setOnClickListener(v -> openFilePicker());
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
            Toast.makeText(requireContext(), "No camera app found on this device", Toast.LENGTH_SHORT).show();
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

    private void listenForMessages() {
        String myUid = FirebaseDebugLogger.requireUid(requireContext(), mAuth, "chat_messages_listen");
        if (myUid == null || TextUtils.isEmpty(contactUid)) return;

        chatId = buildChatId(myUid, contactUid);
        if (TextUtils.isEmpty(chatId)) return;

        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }

        messageListener = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        FirebaseDebugLogger.failure("chat_messages_listen", "chats/" + chatId + "/messages", e);
                        return;
                    }

                    messageList.clear();
                    if (snap != null) {
                        FirebaseDebugLogger.read("chat_messages_listen", "chats/" + chatId + "/messages", snap.size());
                        String now = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String text = doc.getString("content");
                            String senderUid = doc.getString("senderUid");
                            String time = doc.getString("timeText");
                            if (TextUtils.isEmpty(time)) time = now;
                            if (!TextUtils.isEmpty(text)) {
                                messageList.add(new Message(text, time, myUid.equals(senderUid)));
                            }
                        }
                    }
                    chatAdapter.notifyDataSetChanged();
                    scrollToBottom();
                });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        String myUid = FirebaseDebugLogger.requireUid(requireContext(), mAuth, "chat_message_send");
        if (myUid == null || TextUtils.isEmpty(contactUid)) return;

        if (TextUtils.isEmpty(chatId)) {
            chatId = buildChatId(myUid, contactUid);
        }
        if (TextUtils.isEmpty(chatId)) return;

        String currentTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());

        Map<String, Object> chat = new HashMap<>();
        chat.put("chatId", chatId);
        chat.put("participants", java.util.Arrays.asList(myUid, contactUid));
        chat.put("lastMessage", text);
        chat.put("lastMessageAt", FieldValue.serverTimestamp());
        chat.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> message = new HashMap<>();
        message.put("chatId", chatId);
        message.put("senderUid", myUid);
        message.put("receiverUid", contactUid);
        message.put("type", "text");
        message.put("content", text);
        message.put("timeText", currentTime);
        message.put("createdAt", FieldValue.serverTimestamp());

        etMessage.setText("");

        db.collection("chats").document(chatId).set(chat, com.google.firebase.firestore.SetOptions.merge())
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) throw task.getException();
                    return db.collection("chats").document(chatId).collection("messages").add(message);
                })
                .addOnSuccessListener(docRef -> {
                    FirebaseDebugLogger.success("chat_message_send", "chats/" + chatId + "/messages", docRef.getId());
                })
                .addOnFailureListener(e -> {
                    FirebaseDebugLogger.failure("chat_message_send", "chats/" + chatId + "/messages", e);
                    Toast.makeText(requireContext(), "Message failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void scrollToBottom() {
        if (rvMessages != null && !messageList.isEmpty()) {
            rvMessages.post(() -> rvMessages.scrollToPosition(messageList.size() - 1));
        }
    }

    private void loadChatHeader() {
        if (!TextUtils.isEmpty(contactUid)) {
            db.collection("profiles")
                    .document(contactUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        FirebaseDebugLogger.read("chat_header_read", "profiles/" + contactUid, doc.exists() ? 1 : 0);
                        bindProfileToHeader(doc);
                    })
                    .addOnFailureListener(e -> {
                        FirebaseDebugLogger.failure("chat_header_read", "profiles/" + contactUid, e);
                        bindHeaderFromArguments();
                    });
            return;
        }

        bindHeaderFromArguments();
    }

    private void bindHeaderFromArguments() {
        String name = TextUtils.isEmpty(contactNameArg) ? "User" : contactNameArg.trim();
        String role = TextUtils.isEmpty(contactRoleArg) ? "Worker" : capitalize(contactRoleArg.trim());

        tvContactName.setText(name);
        tvContactRole.setText(role);

        loadImageIntoHeader(contactPhotoArg);
    }

    private void bindProfileToHeader(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            bindHeaderFromArguments();
            return;
        }

        String firstName = snapshot.getString("firstName");
        String lastName = snapshot.getString("lastName");
        String displayName = snapshot.getString("displayName");
        String role = snapshot.getString("role");

        String fullName = !TextUtils.isEmpty(displayName)
                ? displayName.trim()
                : ((firstName == null ? "" : firstName.trim()) + " " +
                (lastName == null ? "" : lastName.trim())).trim();

        if (TextUtils.isEmpty(fullName)) fullName = "User";
        if (TextUtils.isEmpty(role)) role = "Worker";

        tvContactName.setText(fullName);
        tvContactRole.setText(capitalize(role));

        String photoUrl = snapshot.getString("photoUrl");
        if (TextUtils.isEmpty(photoUrl)) photoUrl = snapshot.getString("photo");
        if (TextUtils.isEmpty(photoUrl)) photoUrl = snapshot.getString("avatar");

        loadImageIntoHeader(photoUrl);
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

    private String capitalize(String value) {
        if (TextUtils.isEmpty(value)) return "";
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
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
