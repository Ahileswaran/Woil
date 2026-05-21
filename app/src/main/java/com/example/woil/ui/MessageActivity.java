package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessageActivity extends AppCompatActivity implements MessageUserAdapter.OnUserClickListener {

    private ImageButton btnBack;
    private ImageButton btnNewChat;
    private RecyclerView rvChats;
    private TextView tvHeaderSubtitle;
    private TextView tvEmptyState;
    private ProgressBar progressChats;

    private MessageUserAdapter adapter;
    private final List<MessageUserModel> userList = new ArrayList<>();
    private final Map<String, MessageUserModel> chatMap = new HashMap<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration chatsListener;
    private String currentUid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        bindViews();
        setupClicks();
        setupRecyclerView();
        handleDirectOpenIntent();
        loadConversations();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btn_back);
        btnNewChat = findViewById(R.id.btn_new_chat);
        rvChats = findViewById(R.id.rv_chat_users);
        tvHeaderSubtitle = findViewById(R.id.tv_header_subtitle);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        progressChats = findViewById(R.id.progress_chats);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());
        btnNewChat.setOnClickListener(v -> Toast.makeText(this, "Open chat from a selected client or worker profile", Toast.LENGTH_SHORT).show());
    }

    private void setupRecyclerView() {
        adapter = new MessageUserAdapter(userList, this, this);
        rvChats.setLayoutManager(new LinearLayoutManager(this));
        rvChats.setAdapter(adapter);
    }

    private void handleDirectOpenIntent() {
        String contactUid = getIntent().getStringExtra(ChatFragment.ARG_CONTACT_UID);
        if (TextUtils.isEmpty(contactUid)) return;

        String contactName = getIntent().getStringExtra(ChatFragment.ARG_CONTACT_NAME);
        String contactRole = getIntent().getStringExtra(ChatFragment.ARG_CONTACT_ROLE);
        String contactPhoto = getIntent().getStringExtra(ChatFragment.ARG_CONTACT_PHOTO);
        String chatId = getIntent().getStringExtra(ChatHostActivity.EXTRA_CHAT_ID);
        String jobId = getIntent().getStringExtra(ChatHostActivity.EXTRA_JOB_ID);

        Intent intent = new Intent(this, ChatHostActivity.class);
        intent.putExtra(ChatFragment.ARG_CONTACT_UID, contactUid);
        intent.putExtra(ChatFragment.ARG_CONTACT_NAME, contactName);
        intent.putExtra(ChatFragment.ARG_CONTACT_ROLE, contactRole);
        intent.putExtra(ChatFragment.ARG_CONTACT_PHOTO, contactPhoto);
        intent.putExtra(ChatHostActivity.EXTRA_CHAT_ID, chatId);
        intent.putExtra(ChatHostActivity.EXTRA_JOB_ID, jobId);
        startActivity(intent);
    }

    private void loadConversations() {
        if (TextUtils.isEmpty(currentUid)) {
            progressChats.setVisibility(View.GONE);
            showEmptyState("Please sign in to view conversations.");
            return;
        }

        progressChats.setVisibility(View.VISIBLE);
        if (chatsListener != null) {
            chatsListener.remove();
        }

        chatsListener = db.collection("chats")
                .whereArrayContains("participants", currentUid)
                .addSnapshotListener((snap, e) -> {
                    progressChats.setVisibility(View.GONE);
                    if (e != null) {
                        showEmptyState("Unable to load conversations.");
                        Toast.makeText(this, "Chat list failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    chatMap.clear();
                    if (snap == null || snap.isEmpty()) {
                        publishList();
                        return;
                    }

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        upsertConversation(doc);
                    }
                    publishList();
                });
    }

    private void upsertConversation(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return;

        String chatId = firstNonEmpty(doc.getString("chatId"), doc.getId());
        List<String> participants = (List<String>) doc.get("participants");
        String otherUid = findOtherUid(participants);
        if (TextUtils.isEmpty(otherUid)) return;

        String lastMessage = firstNonEmpty(
                doc.getString("lastMessageText"),
                doc.getString("lastMessage"),
                "No messages yet"
        );

        Timestamp ts = doc.getTimestamp("lastMessageAt");
        if (ts == null) ts = doc.getTimestamp("updatedAt");
        if (ts == null) ts = doc.getTimestamp("createdAt");
        long lastMessageAtMillis = ts != null ? ts.toDate().getTime() : 0L;
        String timeText = formatTime(lastMessageAtMillis);
        int unreadCount = extractUnreadCount(doc, currentUid);

        MessageUserModel existing = chatMap.get(chatId);
        String existingName = existing != null ? existing.getName() : null;
        String existingRole = existing != null ? existing.getRole() : null;
        String existingPhoto = existing != null ? existing.getPhotoUrl() : null;
        boolean existingOnline = existing != null && existing.isOnline();

        chatMap.put(chatId, new MessageUserModel(
                chatId,
                otherUid,
                firstNonEmpty(existingName, "User"),
                firstNonEmpty(existingRole, "Participant"),
                existingPhoto,
                lastMessage,
                timeText,
                unreadCount,
                existingOnline,
                lastMessageAtMillis
        ));

        enrichProfile(chatId, otherUid);
    }

    private void enrichProfile(String chatId, String otherUid) {
        db.collection("profiles")
                .document(otherUid)
                .get()
                .addOnSuccessListener(profile -> {
                    MessageUserModel current = chatMap.get(chatId);
                    if (current == null) return;

                    String displayName = buildDisplayName(profile);
                    String role = firstNonEmpty(profile.getString("role"), current.getRole());
                    String photo = firstNonEmpty(profile.getString("photoUrl"), profile.getString("photo"), profile.getString("avatar"), current.getPhotoUrl());
                    boolean online = isOnline(profile.getTimestamp("lastSeenAt"));

                    chatMap.put(chatId, new MessageUserModel(
                            current.getChatId(),
                            current.getUid(),
                            firstNonEmpty(displayName, current.getName()),
                            capitalize(role),
                            photo,
                            current.getLastMessage(),
                            current.getTime(),
                            current.getUnreadCount(),
                            online,
                            current.getLastMessageAtMillis()
                    ));
                    publishList();
                });
    }

    private void publishList() {
        userList.clear();
        userList.addAll(chatMap.values());
        Collections.sort(userList, (o1, o2) -> Long.compare(o2.getLastMessageAtMillis(), o1.getLastMessageAtMillis()));
        adapter.notifyDataSetChanged();

        if (userList.isEmpty()) {
            showEmptyState("No conversations yet. Start a chat from a matched worker or client profile.");
            tvHeaderSubtitle.setText("0 conversations");
        } else {
            rvChats.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
            tvHeaderSubtitle.setText(userList.size() + (userList.size() == 1 ? " conversation" : " conversations"));
        }
    }

    private void showEmptyState(String message) {
        rvChats.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
        tvEmptyState.setText(message);
    }

    private String findOtherUid(List<String> participants) {
        if (participants == null) return null;
        for (String uid : participants) {
            if (!TextUtils.equals(uid, currentUid)) return uid;
        }
        return null;
    }

    private int extractUnreadCount(DocumentSnapshot doc, String uid) {
        Object unreadObject = doc.get("unreadCount");
        if (unreadObject instanceof Map) {
            Object value = ((Map<?, ?>) unreadObject).get(uid);
            if (value instanceof Number) return ((Number) value).intValue();
        }
        Object flat = doc.get("unread_" + uid);
        if (flat instanceof Number) return ((Number) flat).intValue();
        return 0;
    }

    private String buildDisplayName(DocumentSnapshot profile) {
        if (profile == null || !profile.exists()) return null;
        String displayName = profile.getString("displayName");
        if (!TextUtils.isEmpty(displayName)) return displayName.trim();
        String firstName = profile.getString("firstName");
        String lastName = profile.getString("lastName");
        String full = ((firstName == null ? "" : firstName.trim()) + " " + (lastName == null ? "" : lastName.trim())).trim();
        return TextUtils.isEmpty(full) ? null : full;
    }

    private boolean isOnline(Timestamp lastSeenAt) {
        if (lastSeenAt == null) return false;
        long diff = System.currentTimeMillis() - lastSeenAt.toDate().getTime();
        return diff >= 0 && diff <= (5 * 60 * 1000L);
    }

    private String formatTime(long millis) {
        if (millis <= 0L) return "";
        Date date = new Date(millis);
        long now = System.currentTimeMillis();
        long diff = now - millis;
        if (diff < 24L * 60 * 60 * 1000L) {
            return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(date);
        }
        return new SimpleDateFormat("dd MMM", Locale.getDefault()).format(date);
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

    @Override
    public void onUserClick(MessageUserModel user) {
        Intent intent = new Intent(this, ChatHostActivity.class);
        intent.putExtra(ChatHostActivity.EXTRA_CHAT_ID, user.getChatId());
        intent.putExtra(ChatFragment.ARG_CONTACT_UID, user.getUid());
        intent.putExtra(ChatFragment.ARG_CONTACT_NAME, user.getName());
        intent.putExtra(ChatFragment.ARG_CONTACT_ROLE, user.getRole());
        intent.putExtra(ChatFragment.ARG_CONTACT_PHOTO, user.getPhotoUrl());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatsListener != null) {
            chatsListener.remove();
            chatsListener = null;
        }
    }
}
