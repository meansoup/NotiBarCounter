package com.meansoup.notibarcounter;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String CHANNEL_ID = "custom_notification_channel";
    private static final int NOTIFICATION_ID = 1;
    private TextView counterTextView;
    private Button showNotificationButton;
    private Button upButton;
    private Button downButton;
    private Button resetButton;
    private MaterialCardView counterCardView;
    private MyNotificationListenerService notificationService;
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isBound = false;

    private List<NotificationHistoryItem> historyList;
    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String HISTORY_KEY = "historyList";

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            MyNotificationListenerService.LocalBinder binder = (MyNotificationListenerService.LocalBinder) service;
            notificationService = binder.getService();
            setNotificationService(notificationService);
            isBound = true;
            updateCounter(); // Update counter when service is connected
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
            notificationService = null;
            isBound = false;
        }
    };

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startAndBindService();
                } else {
                    Toast.makeText(this, "알림 권한이 필요합니다", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        counterTextView = findViewById(R.id.counterTextView);
        showNotificationButton = findViewById(R.id.showNotificationButton);
        upButton = findViewById(R.id.upButton);
        downButton = findViewById(R.id.downButton);
        resetButton = findViewById(R.id.resetButton);
        counterCardView = findViewById(R.id.counterCardView);

        createNotificationChannel();
        loadHistory();

        showNotificationButton.setOnClickListener(v -> {
            if (checkNotificationPermission()) {
                if (notificationService != null) {
                    notificationService.showCounterNotification();
                    Log.d(TAG, "Showing counter notification");
                } else {
                    Log.e(TAG, "Notification service is null, starting and binding");
                    startAndBindService();
                }
            }
        });

        upButton.setOnClickListener(v -> {
            Log.d(TAG, "Up button clicked in MainActivity");
            if (notificationService != null) {
                 Intent intent = new Intent(this, MyNotificationListenerService.class);
                 intent.setAction("BUTTON_UP");
                 startService(intent);
            } else {
                Log.e(TAG, "Notification service is null, cannot increment counter");
                startAndBindService();
            }
        });

        downButton.setOnClickListener(v -> {
            Log.d(TAG, "Down button clicked in MainActivity");
            if (notificationService != null) {
                Intent intent = new Intent(this, MyNotificationListenerService.class);
                intent.setAction("BUTTON_DOWN");
                startService(intent);
            } else {
                 Log.e(TAG, "Notification service is null, cannot decrement counter");
                 startAndBindService();
            }
        });

        resetButton.setOnClickListener(v -> {
            Log.d(TAG, "Reset button clicked");
            saveCurrentCountToHistory();
            resetCounter();
        });

        counterCardView.setOnClickListener(v -> {
            Log.d(TAG, "Card view clicked, showing history");
            Intent historyIntent = new Intent(MainActivity.this, HistoryActivity.class);
            // History data will be loaded in HistoryActivity from SharedPreferences
            startActivity(historyIntent);
        });


        startAndBindService();
        updateCounter(); // Initial update
    }

    private void saveCurrentCountToHistory() {
        int currentCount = MyNotificationListenerService.buttonClickCount;
        long currentTime = System.currentTimeMillis();
        NotificationHistoryItem historyItem = new NotificationHistoryItem(currentTime, currentCount);
        historyList.add(historyItem);
        saveHistory();
        Log.d(TAG, "Saved history item: count=" + currentCount + ", time=" + new Date(currentTime).toString());
    }

    private void resetCounter() {
        MyNotificationListenerService.buttonClickCount = 0;
        updateCounter(); // Update MainActivity UI
        if (notificationService != null) {
            notificationService.showCounterNotification(); // Update Notification UI
        } else {
             Log.e(TAG, "Notification service is null, cannot update notification after reset");
        }
        Log.d(TAG, "Counter reset to 0");
    }

    private void saveHistory() {
        Gson gson = new Gson();
        String json = gson.toJson(historyList);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(HISTORY_KEY, json)
                .apply();
        Log.d(TAG, "History saved to SharedPreferences");
    }

    private void loadHistory() {
        Gson gson = new Gson();
        String json = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(HISTORY_KEY, null);
        Type type = new TypeToken<ArrayList<NotificationHistoryItem>>() {}.getType();
        historyList = gson.fromJson(json, type);
        if (historyList == null) {
            historyList = new ArrayList<>();
        }
        Log.d(TAG, "History loaded from SharedPreferences, items: " + historyList.size());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isBound && isNotificationListenerEnabled()) { // Check if service is running and not bound
             startAndBindService();
        }
        updateCounter(); // Update counter on resume
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
            Log.d(TAG, "Service unbound");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Custom Notification Channel",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created");
        }
    }

    private boolean isNotificationListenerEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (flat != null && !flat.isEmpty()) {
            return flat.contains(pkgName);
        }
        return false;
    }

    public void updateCounter() {
        // Access the static counter directly from the service
        final int currentCount = MyNotificationListenerService.buttonClickCount;
        Log.d(TAG, "updateCounter called with count: " + currentCount);
        mainHandler.post(() -> {
            // Use the simple count display as per the previous request, without the label
            counterTextView.setText(String.valueOf(currentCount));
            Log.d(TAG, "Counter text updated to: " + currentCount);
        });
    }

    public void setNotificationService(MyNotificationListenerService service) {
        Log.d(TAG, "setNotificationService called with service: " + (service != null));
        this.notificationService = service;
        if (service != null) {
            service.setMainActivity(this); // Keep this if service still needs MainActivity reference
            updateCounter(); // Update counter when service is set
            Log.d(TAG, "Notification service set, current count: " + MyNotificationListenerService.buttonClickCount);
        } else {
            Log.e(TAG, "Setting null notification service");
        }
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                return false;
            }
        }
        return true;
    }

    private void startAndBindService() {
        // Permission check is done before calling this method

        Intent serviceIntent = new Intent(this, MyNotificationListenerService.class);
        startForegroundService(serviceIntent);
        // Only bind if not already bound
        if (!isBound) {
             bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
             Log.d(TAG, "Attempting to bind service");
        }
        Log.d(TAG, "Started notification service");
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        boolean hasActiveNotifications = manager != null && manager.getActiveNotifications().length > 0;
        return false; // Returning false to always attempt binding if not bound
    }
}