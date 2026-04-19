package com.example.woil.ui;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.woil.R;

public class ChatHostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_host);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);


        if (savedInstanceState == null) {
            String contactUid = getIntent().getStringExtra(ChatFragment.ARG_CONTACT_UID);
            String contactName = getIntent().getStringExtra(ChatFragment.ARG_CONTACT_NAME);
            String contactRole = getIntent().getStringExtra(ChatFragment.ARG_CONTACT_ROLE);
            String contactPhoto = getIntent().getStringExtra(ChatFragment.ARG_CONTACT_PHOTO);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.chat_fragment_container,
                            ChatFragment.newInstance(contactUid, contactName, contactRole, contactPhoto)
                    )
                    .commit();
        }
    }
}