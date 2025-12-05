package com.example.woil;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.ui.MainActivity; // Remove if not needed

import java.util.List;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.VH> {
    private final List<TimelineModel> timelineList;

    public TimelineAdapter(List<TimelineModel> timelineList) {
        this.timelineList = timelineList;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timeline, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TimelineModel model = timelineList.get(position);
        holder.tvTitle.setText(model.title);
        holder.tvStatus.setText(model.status);
        holder.tvTime.setText(model.time);
    }

    @Override
    public int getItemCount() {
        return timelineList != null ? timelineList.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStatus, tvTime;

        VH(@NonNull View v) {
            super(v);
//tvTitle = v.findViewById(R.id.tvTitle);
           // tvStatus = v.findViewById(R.id.tvStatus);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}