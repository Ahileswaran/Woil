package com.example.woil.ui;

import android.content.Context;
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

import java.util.List;

public class MessageUserAdapter extends RecyclerView.Adapter<MessageUserAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(MessageUserModel user);
    }

    private final List<MessageUserModel> items;
    private final Context context;
    private final OnUserClickListener listener;

    public MessageUserAdapter(List<MessageUserModel> items, Context context, OnUserClickListener listener) {
        this.items = items;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        MessageUserModel item = items.get(position);

        holder.tvName.setText(item.getName());
        holder.tvRole.setText(item.getRole());
        holder.tvMessage.setText(item.getLastMessage());
        holder.tvTime.setText(item.getTime());

        if (item.getUnreadCount() > 0) {
            holder.tvUnread.setVisibility(View.VISIBLE);
            holder.tvUnread.setText(String.valueOf(item.getUnreadCount()));
        } else {
            holder.tvUnread.setVisibility(View.GONE);
        }

        holder.viewOnline.setVisibility(item.isOnline() ? View.VISIBLE : View.GONE);

        if (!TextUtils.isEmpty(item.getPhotoUrl())) {
            Glide.with(context)
                    .load(item.getPhotoUrl())
                    .placeholder(R.drawable.photo_placeholder)
                    .error(R.drawable.photo_placeholder)
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.photo_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivProfile;
        final View viewOnline;
        final TextView tvName;
        final TextView tvRole;
        final TextView tvMessage;
        final TextView tvTime;
        final TextView tvUnread;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_profile);
            viewOnline = itemView.findViewById(R.id.view_online);
            tvName = itemView.findViewById(R.id.tv_name);
            tvRole = itemView.findViewById(R.id.tv_role);
            tvMessage = itemView.findViewById(R.id.tv_last_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvUnread = itemView.findViewById(R.id.tv_unread);
        }
    }
}
