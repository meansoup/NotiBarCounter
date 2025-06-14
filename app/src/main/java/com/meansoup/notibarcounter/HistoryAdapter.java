package com.meansoup.notibarcounter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<NotificationHistoryItem> historyItems;
    private SimpleDateFormat dateFormat;

    public HistoryAdapter(List<NotificationHistoryItem> historyItems) {
        this.historyItems = historyItems;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationHistoryItem item = historyItems.get(position);
        String formattedDate = dateFormat.format(new Date(item.getTimestamp()));
        holder.timeTextView.setText(formattedDate);
        holder.countTextView.setText(String.valueOf(item.getCount()));
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timeTextView;
        TextView countTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            countTextView = itemView.findViewById(R.id.countTextView);
        }
    }
} 