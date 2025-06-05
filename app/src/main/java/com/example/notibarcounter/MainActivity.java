package com.example.notibarcounter;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.widget.RemoteViews;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String CHANNEL_ID = "custom_notification_channel";
    private static final int NOTIFICATION_ID = 1;
    private TextView counterTextView;
    private Button permissionButton;
    private Button showNotificationButton;
    private MyNotificationListenerService notificationService;
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            MyNotificationListenerService.LocalBinder binder = (MyNotificationListenerService.LocalBinder) service;
            notificationService = binder.getService();
            setNotificationService(notificationService);
            isBound = true;
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
        permissionButton = findViewById(R.id.permissionButton);
        showNotificationButton = findViewById(R.id.showNotificationButton);

        createNotificationChannel();

        permissionButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        });

        showNotificationButton.setOnClickListener(v -> {
            if (checkNotificationPermission()) {
                if (notificationService != null) {
                    notificationService.showCounterNotification();
                    Log.d(TAG, "Showing counter notification");
                } else {
                    Log.e(TAG, "Notification service is null");
                    startAndBindService();
                }
            }
        });

        updatePermissionButton();
        startAndBindService();
        updateCounter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionButton();
        if (!isServiceRunning(MyNotificationListenerService.class)) {
            startAndBindService();
        }
        updateCounter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
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

    private void updatePermissionButton() {
        boolean isEnabled = isNotificationListenerEnabled();
        permissionButton.setText(isEnabled ? "권한 해제" : "권한 설정");
        permissionButton.setEnabled(true);
        Log.d(TAG, "Permission status: " + isEnabled);
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
        Log.d(TAG, "updateCounter called with count: " + MyNotificationListenerService.buttonClickCount);
        mainHandler.post(() -> {
            counterTextView.setText("버튼 클릭 카운트: " + MyNotificationListenerService.buttonClickCount);
            Log.d(TAG, "Counter text updated to: " + MyNotificationListenerService.buttonClickCount);
        });
    }

    public void setNotificationService(MyNotificationListenerService service) {
        Log.d(TAG, "setNotificationService called with service: " + (service != null));
        this.notificationService = service;
        if (service != null) {
            service.setMainActivity(this);
            updateCounter();
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
        if (!isNotificationListenerEnabled()) {
            Toast.makeText(this, "알림 접근 권한을 허용해주세요", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
            return;
        }

        Intent serviceIntent = new Intent(this, MyNotificationListenerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Started and bound notification service");
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        boolean isRunning = manager != null && manager.getActiveNotifications().length > 0;
        Log.d(TAG, "Service running check: " + isRunning);
        return isRunning;
    }
} 