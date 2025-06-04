package com.example.notibarcounter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.widget.RemoteViews;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.core.app.NotificationCompat;

import com.example.notibarcounter.MainActivity;

public class MyNotificationListenerService extends NotificationListenerService {
    private static final String TAG = "NOTI_TIMING";
    private int buttonClickCount = 0;
    private MainActivity mainActivity;
    private static final String CHANNEL_ID = "counter_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int FOREGROUND_NOTIFICATION_ID = 2;
    private NotificationManager notificationManager;
    private Handler mainHandler;
    private Handler notificationHandler;
    private static final long NOTIFICATION_UPDATE_DELAY = 100; // 100ms 딜레이
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    
    // 캐시된 객체들
    private RemoteViews cachedViews;
    private PendingIntent cachedButtonPendingIntent;
    private NotificationCompat.Builder notificationBuilder;
    private boolean isNotificationUpdatePending = false;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = getSystemService(NotificationManager.class);
        mainHandler = new Handler(Looper.getMainLooper());
        notificationHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        initializeCachedObjects();
        startForeground();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Counter Channel",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void initializeCachedObjects() {
        // RemoteViews 캐시
        cachedViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        
        // PendingIntent 캐시 - 카운트 값을 전달
        Intent buttonIntent = new Intent(this, MyNotificationListenerService.class);
        buttonIntent.setAction("BUTTON_CLICK");
        buttonIntent.putExtra("current_count", buttonClickCount);
        cachedButtonPendingIntent = PendingIntent.getService(this, 0, buttonIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // NotificationBuilder 캐시
        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(false)
                .setOngoing(true);
    }

    private void startForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Counter Service")
                .setContentText("Service is running")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(FOREGROUND_NOTIFICATION_ID, notification);
        showCounterNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "BUTTON_CLICK".equals(intent.getAction())) {
            long startTime = System.nanoTime();
            Log.d(TAG, "==========================================");
            Log.d(TAG, "Button click received at: " + startTime);
            
            // 즉시 카운트 증가
            buttonClickCount++;
            Log.d(TAG, "Count updated to: " + buttonClickCount);
            
            // 메인 스레드에서 UI 업데이트
            mainHandler.post(() -> {
                updateCounter();
                scheduleNotificationUpdate();
                
                long endTime = System.nanoTime();
                Log.d(TAG, "Total processing time: " + (endTime - startTime) / 1000000.0 + "ms");
                Log.d(TAG, "Button click ended at: " + System.nanoTime());
                Log.d(TAG, "==========================================");
            });
        }
        return START_STICKY;
    }

    private void scheduleNotificationUpdate() {
        if (!isNotificationUpdatePending) {
            isNotificationUpdatePending = true;
            notificationHandler.removeCallbacksAndMessages(null);
            notificationHandler.postDelayed(() -> {
                showCounterNotification();
                isNotificationUpdatePending = false;
            }, NOTIFICATION_UPDATE_DELAY);
        }
    }

    private void showCounterNotification() {
        long startTime = System.nanoTime();
        
        // 텍스트만 업데이트
        cachedViews.setTextViewText(R.id.countText, "Count: " + buttonClickCount);
        cachedViews.setOnClickPendingIntent(R.id.customButton, cachedButtonPendingIntent);

        // 알림 업데이트
        Notification notification = notificationBuilder
                .setCustomContentView(cachedViews)
                .build();

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
        });


        long endTime = System.nanoTime();
        Log.d(TAG, "Notification update took: " + (endTime - startTime) / 1000000.0 + "ms");
    }

    private void updateCounter() {
        if (mainActivity != null) {
            mainActivity.updateCounter(buttonClickCount);
        }
    }

    public void setMainActivity(MainActivity activity) {
        this.mainActivity = activity;
        updateCounter();
        showCounterNotification();
    }

    public int getCurrentCount() {
        return buttonClickCount;
    }
} 