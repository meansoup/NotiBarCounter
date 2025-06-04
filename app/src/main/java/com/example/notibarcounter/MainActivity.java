package com.example.notibarcounter;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;

public class MainActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "custom_notification_channel";
    private static final int NOTIFICATION_ID = 1;
    private TextView counterTextView;
    private Button permissionButton;
    private Button showNotificationButton;
    private MyNotificationListenerService notificationService;
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startService();
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
                showCustomNotification();
            }
        });

        updatePermissionButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionButton();
        if (!isServiceRunning(MyNotificationListenerService.class)) {
            startService();
        } else if (notificationService != null) {
            updateCounter(notificationService.getCurrentCount());
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
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void showCustomNotification() {
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("카운터 알림")
            .setContentText("버튼을 눌러 카운트를 증가시키세요")
            .setCustomContentView(remoteViews)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(pendingIntent);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void updatePermissionButton() {
        boolean isEnabled = isNotificationListenerEnabled();
        permissionButton.setText(isEnabled ? "권한 해제" : "권한 설정");
        permissionButton.setEnabled(true);
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

    public void updateCounter(int count) {
        counterTextView.setText("버튼 클릭 카운트: " + count);
    }

    public void setNotificationService(MyNotificationListenerService service) {
        this.notificationService = service;
        if (service != null) {
            service.setMainActivity(this);
            updateCounter(service.getCurrentCount());
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

    private void startService() {
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
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return manager != null && manager.getActiveNotifications().length > 0;
    }
} 