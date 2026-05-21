package com.example.woil.ui;

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

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messageList;

    public ChatAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).isSentByMe() ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_chat_sent, parent, false);
            return new SentMessageViewHolder(view);
        }

        View view = inflater.inflate(R.layout.item_chat_received, parent, false);
        return new ReceivedMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        if (holder instanceof SentMessageViewHolder) {
            SentMessageViewHolder sentHolder = (SentMessageViewHolder) holder;
            sentHolder.tvMessageSent.setText(message.getText());
            sentHolder.tvTimeSent.setText(message.getTime());
            return;
        }

        ReceivedMessageViewHolder receivedHolder = (ReceivedMessageViewHolder) holder;
        receivedHolder.tvMessageReceived.setText(message.getText());
        receivedHolder.tvTimeReceived.setText(message.getTime());

        if (!TextUtils.isEmpty(message.getSenderPhotoUrl())) {
            Glide.with(receivedHolder.itemView)
                    .load(message.getSenderPhotoUrl())
                    .placeholder(R.drawable.photo_placeholder)
                    .error(R.drawable.photo_placeholder)
                    .into(receivedHolder.imgSender);
        } else {
            receivedHolder.imgSender.setImageResource(R.drawable.photo_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return messageList == null ? 0 : messageList.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMessageSent;
        final TextView tvTimeSent;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageSent = itemView.findViewById(R.id.tv_message_sent);
            tvTimeSent = itemView.findViewById(R.id.tv_time_sent);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgSender;
        final TextView tvMessageReceived;
        final TextView tvTimeReceived;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSender = itemView.findViewById(R.id.img_sender);
            tvMessageReceived = itemView.findViewById(R.id.tv_message_received);
            tvTimeReceived = itemView.findViewById(R.id.tv_time_received);
        }
    }
}
