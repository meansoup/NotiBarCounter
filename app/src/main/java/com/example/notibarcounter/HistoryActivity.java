package com.example.notibarcounter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";
    private RecyclerView historyRecyclerView;
    private HistoryAdapter historyAdapter;
    private List<NotificationHistoryItem> historyList;

    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String HISTORY_KEY = "historyList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyRecyclerView = findViewById(R.id.historyRecyclerView);

        loadHistory();
        setupRecyclerView();

        // Optional: Add a back button to the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("리셋 기록");
        }
    }

    private void setupRecyclerView() {
        historyAdapter = new HistoryAdapter(historyList);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(historyAdapter);
        Log.d(TAG, "RecyclerView setup complete");
    }

    private void loadHistory() {
        Gson gson = new Gson();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(HISTORY_KEY, null);
        Type type = new TypeToken<ArrayList<NotificationHistoryItem>>() {}.getType();
        historyList = gson.fromJson(json, type);
        if (historyList == null) {
            historyList = new ArrayList<>();
        }
        // Reverse the list to show the most recent resets first
        java.util.Collections.reverse(historyList);
        Log.d(TAG, "History loaded from SharedPreferences, items: " + historyList.size());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Handle back button click
        return true;
    }
} 