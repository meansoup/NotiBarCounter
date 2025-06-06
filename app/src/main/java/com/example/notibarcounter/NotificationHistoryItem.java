package com.example.notibarcounter;

public class NotificationHistoryItem {
    private long timestamp;
    private int count;

    public NotificationHistoryItem(long timestamp, int count) {
        this.timestamp = timestamp;
        this.count = count;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getCount() {
        return count;
    }
} 