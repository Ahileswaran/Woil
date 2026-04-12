package com.example.woil.ui;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.woil.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class MatchingWorkerAdapter extends RecyclerView.Adapter<MatchingWorkerAdapter.VH> {

    public interface OnWorkerClickListener {
        void onWorkerClick(MatchingWorkerModel worker);
    }

    private final List<MatchingWorkerModel> items = new ArrayList<>();
    private final OnWorkerClickListener listener;

    public MatchingWorkerAdapter(OnWorkerClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<MatchingWorkerModel> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public List<MatchingWorkerModel> getItems() {
        return items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_matching_worker, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MatchingWorkerModel item = items.get(position);

        h.tvName.setText(item.name != null ? item.name : "Worker");
        h.tvSkill.setText(item.skill != null ? item.skill : "General");
        h.tvEta.setText("ETA: " + item.etaMinutes + " min");
        h.tvDistance.setText(String.format(Locale.getDefault(), "Distance: %.1f km", item.distanceKm));
        h.tvRating.setText(String.format(Locale.getDefault(), "★ %.1f", item.rating));

        if (item.photoUrl != null && !item.photoUrl.trim().isEmpty()) {
            try {
                Glide.with(h.itemView.getContext())
                        .load(item.photoUrl)
                        .placeholder(R.drawable.photo_placeholder)
                        .error(R.drawable.photo_placeholder)
                        .into(h.ivPhoto);
            } catch (Exception e) {
                try {
                    h.ivPhoto.setImageURI(Uri.parse(item.photoUrl));
                } catch (Exception ignored) {
                    h.ivPhoto.setImageResource(R.drawable.photo_placeholder);
                }
            }
        } else {
            h.ivPhoto.setImageResource(R.drawable.photo_placeholder);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onWorkerClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        CircleImageView ivPhoto;
        TextView tvName, tvSkill, tvEta, tvDistance, tvRating;

        VH(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_worker_photo);
            tvName = itemView.findViewById(R.id.tv_worker_name);
            tvSkill = itemView.findViewById(R.id.tv_worker_skill);
            tvEta = itemView.findViewById(R.id.tv_worker_eta);
            tvDistance = itemView.findViewById(R.id.tv_worker_distance);
            tvRating = itemView.findViewById(R.id.tv_worker_rating);
        }
    }
}