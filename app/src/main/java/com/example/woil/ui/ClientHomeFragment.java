package com.example.woil.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.R;
import com.google.android.material.button.MaterialButton;

public class ClientHomeFragment extends Fragment {

    private RecyclerView rvClientPostedJobs;
    private RecyclerView rvClientMatching;
    private MaterialButton btnAddJobClientHome;

    public ClientHomeFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.client_fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        rvClientPostedJobs = view.findViewById(R.id.rvClientPostedJobs);
        rvClientMatching = view.findViewById(R.id.rvClientMatching);
        btnAddJobClientHome = view.findViewById(R.id.btn_add_job_client_home);

        rvClientPostedJobs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvClientMatching.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnAddJobClientHome.setOnClickListener(v -> {
            try {
                startActivity(new Intent(requireContext(), PostJobActivity.class));
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Can't open Post Job screen", Toast.LENGTH_SHORT).show();
            }
        });
    }
}