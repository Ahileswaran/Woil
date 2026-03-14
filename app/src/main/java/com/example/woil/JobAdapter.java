package com.example.woil;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.VH> {

    public interface OnJobClick {
        void onJobClick(JobModel job);
    }

    private final List<JobModel> items;
    private final Context ctx;
    private final OnJobClick listener;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

    public JobAdapter(List<JobModel> items, Context ctx, OnJobClick listener) {
        this.items = items;
        this.ctx = ctx;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        JobModel j = items.get(position);
        holder.tvTitle.setText(j.title != null ? j.title : "—");
        holder.tvLocation.setText(j.locationText != null ? j.locationText : "");
        holder.tvWage.setText(j.wageSuggestedText != null ? j.wageSuggestedText : (j.wageSuggested != null ? "Rs. " + j.wageSuggested : ""));
        String time = "";
        if (j.startAt != null && j.endAt != null) {
            time = dateFmt.format(j.startAt.toDate()) + " - " + dateFmt.format(j.endAt.toDate());
        } else if (j.startAt != null) {
            time = dateFmt.format(j.startAt.toDate());
        }
        holder.tvTime.setText(time);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onJobClick(j);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvLocation, tvTime, tvWage;
        VH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tv_job_title);
            tvLocation = v.findViewById(R.id.tv_location);
            tvTime = v.findViewById(R.id.tv_time_range);
            tvWage = v.findViewById(R.id.tv_wage_badge);
        }
    }
}