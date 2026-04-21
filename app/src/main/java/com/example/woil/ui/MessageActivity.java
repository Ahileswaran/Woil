package com.example.woil.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.R;

import java.util.ArrayList;
import java.util.List;

public class MessageActivity extends AppCompatActivity implements MessageUserAdapter.OnUserClickListener {

    private ImageButton btnBack;
    private ImageButton btnNewChat;
    private RecyclerView rvChats;
    private TextView tvHeaderSubtitle;

    private MessageUserAdapter adapter;
    private final List<MessageUserModel> userList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        bindViews();
        setupClicks();
        setupRecyclerView();
        loadDummyUsers();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }


    private void bindViews() {
        btnBack = findViewById(R.id.btn_back);
        btnNewChat = findViewById(R.id.btn_new_chat);
        rvChats = findViewById(R.id.rv_chat_users);
        tvHeaderSubtitle = findViewById(R.id.tv_header_subtitle);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());

        btnNewChat.setOnClickListener(v ->
                Toast.makeText(this, "Select new user", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupRecyclerView() {
        adapter = new MessageUserAdapter(userList, this, this);
        rvChats.setLayoutManager(new LinearLayoutManager(this));
        rvChats.setAdapter(adapter);
    }

    private void loadDummyUsers() {
        userList.clear();

        userList.add(new MessageUserModel(
                "uid_1",
                "Balakirush Ahilan",
                "Worker",
                "",
                "Hi Ravi, yes I’ll be available from 2 PM to 4 PM",
                "12:57 AM",
                2,
                true
        ));

        userList.add(new MessageUserModel(
                "uid_2",
                "Kavitha Dissanayake",
                "Cleaner",
                "",
                "Can you come tomorrow morning?",
                "11:20 PM",
                0,
                false
        ));

        userList.add(new MessageUserModel(
                "uid_3",
                "Ravi Senthilkumar",
                "Client",
                "",
                "Sounds great! See you tomorrow",
                "Yesterday",
                1,
                true
        ));

        userList.add(new MessageUserModel(
                "uid_4",
                "Nisansala Perera",
                "Caregiver",
                "",
                "Thank you for the update",
                "Yesterday",
                0,
                false
        ));

        adapter.notifyDataSetChanged();

        if (tvHeaderSubtitle != null) {
            tvHeaderSubtitle.setText(userList.size() + " conversations");
        }
    }

    @Override
    public void onUserClick(MessageUserModel user) {
        Intent intent = new Intent(this, ChatHostActivity.class);
        intent.putExtra(ChatFragment.ARG_CONTACT_UID, user.getUid());
        intent.putExtra(ChatFragment.ARG_CONTACT_NAME, user.getName());
        intent.putExtra(ChatFragment.ARG_CONTACT_ROLE, user.getRole());
        intent.putExtra(ChatFragment.ARG_CONTACT_PHOTO, user.getPhotoUrl());
        startActivity(intent);
    }
}