
package com.example.woil.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.R;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {
    public interface Listener { void onNotificationClick(NotificationItem item); }
    private final List<NotificationItem> items;
    private final Listener listener;
    public NotificationAdapter(List<NotificationItem> items, Listener listener) { this.items = items; this.listener = listener; }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        NotificationItem item = items.get(position);
        h.tvTitle.setText(item.getTitle());
        h.tvBody.setText(item.getBody());
        h.tvTime.setText(item.getTimeText());
        h.itemView.setOnClickListener(v -> listener.onNotificationClick(item));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBody, tvTime;
        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notification_title);
            tvBody = itemView.findViewById(R.id.tv_notification_body);
            tvTime = itemView.findViewById(R.id.tv_notification_time);
        }
    }
}
