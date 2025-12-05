package com.example.woil;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

    private final List<CategoryModel> items;
    private final Context ctx;

    public CategoryAdapter(List<CategoryModel> items, Context ctx) {
        this.items = items;
        this.ctx = ctx;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CategoryModel c = items.get(position);
        holder.tvCat.setText(c.title != null ? c.title : "");

        // handle icon either as int drawable id or as String url
        if (c.icon instanceof Integer) {
            Glide.with(ctx).load((Integer) c.icon).into(holder.imgCat);
        } else if (c.icon instanceof String) {
            String url = (String) c.icon;
            if (!url.isEmpty()) Glide.with(ctx).load(url).into(holder.imgCat);
            else holder.imgCat.setImageResource(android.R.color.transparent);
        } else {
            holder.imgCat.setImageResource(android.R.color.transparent);
        }

        holder.itemView.setOnClickListener(v -> {
            // TODO: handle category click, e.g., open filtered job list
            // Example: Toast.makeText(ctx, "Clicked: " + c.title, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        ShapeableImageView imgCat;
        TextView tvCat;

        VH(@NonNull View v) {
            super(v);
            imgCat = v.findViewById(R.id.imgCat);
            tvCat = v.findViewById(R.id.tvCat);
        }
    }
}
