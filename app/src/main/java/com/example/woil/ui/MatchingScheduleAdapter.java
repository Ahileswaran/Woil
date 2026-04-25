package com.example.woil.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MatchingScheduleAdapter extends RecyclerView.Adapter<MatchingScheduleAdapter.VH> {

    public interface OnWorkerClickListener {
        void onWorkerClick(MatchingWorkerModel worker);
    }

    private final List<MatchingWorkerModel> items = new ArrayList<>();
    private final OnWorkerClickListener listener;

    public MatchingScheduleAdapter() {
        this.listener = null;
    }

    public MatchingScheduleAdapter(OnWorkerClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<MatchingWorkerModel> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_matching_schedule, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MatchingWorkerModel item = items.get(position);

        h.tvName.setText(item.name != null ? item.name : "Worker");
        String areaProvince = "";
        if (item.area != null && !item.area.trim().isEmpty()) areaProvince = item.area;
        if (item.province != null && !item.province.trim().isEmpty()) {
            areaProvince = areaProvince.isEmpty() ? item.province : areaProvince + ", " + item.province;
        }
        h.tvLocation.setText(String.format(Locale.getDefault(), "%s · %.1f km away",
                areaProvince.isEmpty() ? "Expanded area" : areaProvince,
                item.distanceKm));
        h.tvNextAvailable.setText("Next available: Schedule later");
        h.tvRating.setText(String.format(Locale.getDefault(), "★ %.1f", item.rating));

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onWorkerClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvLocation, tvNextAvailable, tvRating;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_schedule_worker_name);
            tvLocation = itemView.findViewById(R.id.tv_schedule_location);
            tvNextAvailable = itemView.findViewById(R.id.tv_schedule_next_available);
            tvRating = itemView.findViewById(R.id.tv_schedule_rating);
        }
    }
}
