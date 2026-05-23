
package com.example.woil.ui;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.woil.R;

import java.util.List;
import java.util.Locale;

public class CccCaseAdapter extends RecyclerView.Adapter<CccCaseAdapter.VH> {
    public interface Listener { void onCaseClick(CccCaseModel item); }
    private final List<CccCaseModel> items;
    private final Listener listener;
    public CccCaseAdapter(List<CccCaseModel> items, Listener listener) { this.items = items; this.listener = listener; }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ccc_case, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        CccCaseModel item = items.get(position);
        h.tvTitle.setText(first(item.getTitle(), "CCC case"));
        h.tvSubtitle.setText(first(item.getSubtitle(), ""));
        h.tvMeta.setText(String.format(Locale.getDefault(), "%s • %s • %s",
                first(item.getType(), "case"), first(item.getSeverity(), "LOW"), first(item.getCreatedText(), "")));
        h.tvStatus.setText(first(item.getStatus(), "OPEN"));
        int color = Color.parseColor("#FF8A00");
        String status = item.getStatus() == null ? "" : item.getStatus().toUpperCase(Locale.ROOT);
        if ("CLOSED".equals(status)) color = Color.parseColor("#2E7D32");
        else if ("ESCALATED".equals(status)) color = Color.parseColor("#C62828");
        else if ("ACKNOWLEDGED".equals(status)) color = Color.parseColor("#1565C0");
        h.tvStatus.setTextColor(color);
        h.itemView.setOnClickListener(v -> listener.onCaseClick(item));
    }

    @Override public int getItemCount() { return items.size(); }

    private static String first(String a, String b) { return TextUtils.isEmpty(a) ? b : a; }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvMeta, tvStatus;
        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_case_title);
            tvSubtitle = itemView.findViewById(R.id.tv_case_subtitle);
            tvMeta = itemView.findViewById(R.id.tv_case_meta);
            tvStatus = itemView.findViewById(R.id.tv_case_status);
        }
    }
}
