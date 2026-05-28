package com.example.woil.ui;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.woil.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class JobApplicationAdapter extends RecyclerView.Adapter<JobApplicationAdapter.VH> {
    public interface Listener {
        void onApplicationClick(JobApplicationModel item);
        void onAcceptClick(JobApplicationModel item);
        void onRejectClick(JobApplicationModel item);
    }
    private final List<JobApplicationModel> items;
    private final Listener listener;
    public JobApplicationAdapter(List<JobApplicationModel> items, Listener listener) { this.items = items; this.listener = listener; }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job_application, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        JobApplicationModel item = items.get(position);
        h.tvWorkerName.setText(first(item.getWorkerName(), "Worker"));
        h.tvWorkerRating.setText(String.format(Locale.getDefault(), "Rating %.1f • %d jobs", item.getWorkerRating(), item.getCompletedJobs()));
        String subtitle = first(item.getWorkerRole(), "Worker");
        if (!TextUtils.isEmpty(item.getWorkerLocationText())) subtitle += " • " + item.getWorkerLocationText();
        h.tvWorkerJobs.setText(subtitle);
        h.tvMeta.setText(String.format(Locale.getDefault(), "%s • %.1f km • %d min • %s",
                first(item.getCategory(), "Any job"), item.getDistanceKm(), item.getEtaMinutes(), first(item.getTimeText(), "recently")));
        String status = first(item.getStatus(), "PENDING").toUpperCase(Locale.ROOT);
        h.tvApplicationStatus.setText("Status: " + status);
        int color = Color.parseColor("#FF8A00");
        if ("ACCEPTED".equals(status)) color = Color.parseColor("#2E7D32");
        else if ("REJECTED".equals(status)) color = Color.parseColor("#C62828");
        else if ("VIEWED".equals(status)) color = Color.parseColor("#1565C0");
        h.tvApplicationStatus.setTextColor(color);
        if (!TextUtils.isEmpty(item.getWorkerPhotoUrl())) {
            Glide.with(h.itemView.getContext()).load(item.getWorkerPhotoUrl()).placeholder(R.drawable.ic_person).error(R.drawable.ic_person).circleCrop().into(h.ivWorkerPhoto);
        } else h.ivWorkerPhoto.setImageResource(R.drawable.ic_person);
        boolean pending = "PENDING".equals(status) || "VIEWED".equals(status);
        h.btnAccept.setVisibility(pending ? View.VISIBLE : View.GONE);
        h.btnReject.setVisibility(pending ? View.VISIBLE : View.GONE);
        h.itemView.setOnClickListener(v -> listener.onApplicationClick(item));
        h.btnAccept.setOnClickListener(v -> listener.onAcceptClick(item));
        h.btnReject.setOnClickListener(v -> listener.onRejectClick(item));
    }
    @Override public int getItemCount() { return items.size(); }
    private static String first(String a, String b) { return TextUtils.isEmpty(a) ? b : a; }
    static class VH extends RecyclerView.ViewHolder {
        ImageView ivWorkerPhoto; TextView tvWorkerName, tvWorkerRating, tvWorkerJobs, tvApplicationStatus, tvMeta; MaterialButton btnAccept, btnReject;
        VH(@NonNull View itemView) { super(itemView);
            ivWorkerPhoto = itemView.findViewById(R.id.iv_worker_photo);
            tvWorkerName = itemView.findViewById(R.id.tv_worker_name);
            tvWorkerRating = itemView.findViewById(R.id.tv_worker_rating);
            tvWorkerJobs = itemView.findViewById(R.id.tv_worker_jobs);
            tvApplicationStatus = itemView.findViewById(R.id.tv_application_status);
            tvMeta = itemView.findViewById(R.id.tv_application_meta);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}