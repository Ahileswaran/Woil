package com.example.woil;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.VH> {
    private static final String TAG = "TimelineAdapter";
    private final List<TimelineModel> timelineList;

    public TimelineAdapter(List<TimelineModel> timelineList) {
        this.timelineList = timelineList;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TimelineModel model = timelineList.get(position);

        // Defensive: check nulls and avoid NPE
        if (holder.tvName != null) holder.tvName.setText(safe(model.title));
        else Log.w(TAG, "tvName is null for position " + position);

        if (holder.tvLocation != null) holder.tvLocation.setText(safe(model.location));
        else Log.w(TAG, "tvLocation is null for position " + position);

        if (holder.tvDesc != null) holder.tvDesc.setText(safe(model.description));
        else Log.w(TAG, "tvDesc is null for position " + position);

        if (holder.tvTime != null) holder.tvTime.setText(safe(model.time));
        else Log.w(TAG, "tvTime is null for position " + position);
    }

    @Override
    public int getItemCount() {
        return timelineList != null ? timelineList.size() : 0;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvLocation;
        final TextView tvDesc;
        final TextView tvTime;

        VH(@NonNull View v) {
            super(v);
            // IMPORTANT: use itemView/findViewById on the inflated view
            tvName = v.findViewById(R.id.tvName);
            tvLocation = v.findViewById(R.id.tvLocation);
            tvDesc = v.findViewById(R.id.tvDesc);
            tvTime = v.findViewById(R.id.tvTime);

            // optional debug log to surface missing ids quickly
            if (tvName == null) Log.w(TAG, "tvName view not found in item_timeline.xml");
            if (tvLocation == null) Log.w(TAG, "tvLocation view not found in item_timeline.xml");
            if (tvDesc == null) Log.w(TAG, "tvDesc view not found in item_timeline.xml");
            if (tvTime == null) Log.w(TAG, "tvTime view not found in item_timeline.xml");
        }
    }
}
