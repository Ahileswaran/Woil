package com.example.woil.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.R;
import com.example.woil.models.SkillVideo;

import java.util.ArrayList;

public class SkillVideoAdapter extends RecyclerView.Adapter<SkillVideoAdapter.VideoViewHolder> {

    public interface OnVideoActionListener {
        void onPreview(SkillVideo video);
        void onEdit(SkillVideo video, int position);
        void onDelete(SkillVideo video, int position);
    }

    private final Context context;
    private final ArrayList<SkillVideo> videoList;
    private final OnVideoActionListener listener;
    private final boolean isClientView;

    public SkillVideoAdapter(Context context, ArrayList<SkillVideo> videoList, OnVideoActionListener listener) {
        this(context, videoList, listener, false);
    }

    public SkillVideoAdapter(Context context, ArrayList<SkillVideo> videoList, OnVideoActionListener listener, boolean isClientView) {
        this.context = context;
        this.videoList = videoList;
        this.listener = listener;
        this.isClientView = isClientView;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_skill_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        SkillVideo video = videoList.get(position);

        holder.txtTitle.setText(video.getTitle());
        holder.txtCategory.setText(video.getCategory());
        holder.txtDescription.setText(video.getDescription());
        holder.txtStatus.setText(video.getStatus());

        setThumbnail(holder.imgThumbnail, video.getVideoUri());

        holder.btnPreview.setOnClickListener(v -> listener.onPreview(video));

        if (isClientView) {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
            holder.txtStatus.setVisibility(View.GONE);
        } else {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.txtStatus.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> listener.onEdit(video, holder.getAdapterPosition()));
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Delete video")
                        .setMessage("Are you sure you want to delete this video?")
                        .setPositiveButton("Delete", (dialog, which) ->
                                listener.onDelete(video, holder.getAdapterPosition()))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    private void setThumbnail(ImageView imageView, Uri uri) {
        if (uri == null) {
            imageView.setImageResource(R.drawable.photo_placeholder);
            return;
        }

        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, uri);
            Bitmap bitmap = retriever.getFrameAtTime(1000000);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.photo_placeholder);
            }
            retriever.release();
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.photo_placeholder);
        }
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail;
        TextView txtTitle, txtCategory, txtDescription, txtStatus;
        TextView btnPreview, btnEdit, btnDelete;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.img_thumbnail);
            txtTitle = itemView.findViewById(R.id.txt_title);
            txtCategory = itemView.findViewById(R.id.txt_category);
            txtDescription = itemView.findViewById(R.id.txt_description);
            txtStatus = itemView.findViewById(R.id.txt_status);
            btnPreview = itemView.findViewById(R.id.btn_preview);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}