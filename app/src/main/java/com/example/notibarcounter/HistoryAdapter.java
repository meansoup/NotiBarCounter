package com.example.notibarcounter;

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

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<NotificationHistoryItem> historyList;

    public HistoryAdapter(List<NotificationHistoryItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        NotificationHistoryItem item = historyList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView timestampTextView;
        TextView countTextView;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampTextView = itemView.findViewById(R.id.historyTimestamp);
            countTextView = itemView.findViewById(R.id.historyCount);
        }

        void bind(NotificationHistoryItem item) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String formattedDate = sdf.format(new Date(item.getTimestamp()));
            timestampTextView.setText("리셋 시간: " + formattedDate);
            countTextView.setText("카운트: " + item.getCount());
        }
    }
} 