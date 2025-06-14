package com.meansoup.notibarcounter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.widget.RemoteViews;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;

import androidx.core.app.NotificationCompat;

public class MyNotificationListenerService extends NotificationListenerService {
    private static final String TAG = "NOTI_TIMING";
    public static int buttonClickCount = 0;  // static으로 변경
    private MainActivity mainActivity;
    private static final String CHANNEL_ID = "counter_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int FOREGROUND_NOTIFICATION_ID = 2;
    private NotificationManager notificationManager;
    private Handler mainHandler;
    private Handler notificationHandler;
    private static final long NOTIFICATION_UPDATE_DELAY = 100;
    private final IBinder binder = new LocalBinder();

    // 캐시된 객체들
    private RemoteViews cachedViews;
    private PendingIntent upButtonPendingIntent;
    private PendingIntent downButtonPendingIntent;
    private NotificationCompat.Builder notificationBuilder;
    private boolean isNotificationUpdatePending = false;

    public class LocalBinder extends Binder {
        MyNotificationListenerService getService() {
            return MyNotificationListenerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bound");
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = getSystemService(NotificationManager.class);
        mainHandler = new Handler(Looper.getMainLooper());
        notificationHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        initializeCachedObjects();
        startForeground();
        Log.d(TAG, "Service created, initial count: " + buttonClickCount);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Counter Channel",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created");
        }
    }

    private void initializeCachedObjects() {
        // RemoteViews 캐시
        cachedViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        
        // Up 버튼 PendingIntent
        Intent upIntent = new Intent(this, MyNotificationListenerService.class);
        upIntent.setAction("BUTTON_UP");
        upButtonPendingIntent = PendingIntent.getService(this, 0, upIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Down 버튼 PendingIntent
        Intent downIntent = new Intent(this, MyNotificationListenerService.class);
        downIntent.setAction("BUTTON_DOWN");
        downButtonPendingIntent = PendingIntent.getService(this, 1, downIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // NotificationBuilder 캐시
        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_noti)
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        
        Log.d(TAG, "Cached objects initialized");
    }

    private void startForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Counter Service")
                .setContentText("Service is running")
                .setSmallIcon(R.drawable.ic_noti)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        startForeground(FOREGROUND_NOTIFICATION_ID, notification);
        showCounterNotification();
        Log.d(TAG, "Service started in foreground");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("BUTTON_UP".equals(action) || "BUTTON_DOWN".equals(action)) {
                long startTime = System.nanoTime();
                Log.d(TAG, "Button click received at: " + startTime);
                
                // 카운트 증가/감소
                if ("BUTTON_UP".equals(action)) {
                    buttonClickCount++;
                } else {
                    buttonClickCount--;
                }
                Log.d(TAG, "Count updated to: " + buttonClickCount);
                
                // 메인 스레드에서 UI 업데이트
                mainHandler.post(() -> {
                    updateCounterInActivity();
                    scheduleNotificationUpdate();
                    
                    long endTime = System.nanoTime();
                    Log.d(TAG, "Total processing time: " + (endTime - startTime) / 1000000.0 + "ms");
                    Log.d(TAG, "Button click ended at: " + System.nanoTime());
                });
            }
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

    public void showCounterNotification() {
        long startTime = System.nanoTime();
        Log.d(TAG, "Showing counter notification, count: " + buttonClickCount);
        
        // 텍스트만 업데이트
        cachedViews.setTextViewText(R.id.countText, String.valueOf(buttonClickCount));
        cachedViews.setOnClickPendingIntent(R.id.upButton, upButtonPendingIntent);
        cachedViews.setOnClickPendingIntent(R.id.downButton, downButtonPendingIntent);

        // 앱 실행을 위한 PendingIntent 생성
        Intent appIntent = new Intent(this, MainActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent appPendingIntent = PendingIntent.getActivity(this, 0, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 알림 업데이트
        Notification notification = notificationBuilder
                .setCustomContentView(cachedViews)
                .setContentIntent(appPendingIntent)
                .build();

        try {
            notificationManager.notify(NOTIFICATION_ID, notification);
            Log.d(TAG, "Notification updated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification: " + e.getMessage());
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        Log.d(TAG, "Notification update took: " + (endTime - startTime) / 1000000.0 + "ms");
    }

    private void updateCounterInActivity() {
        Log.d(TAG, "updateCounterInActivity called, count: " + buttonClickCount);
        if (mainActivity != null) {
            mainActivity.updateCounter();
            Log.d(TAG, "Counter updated in MainActivity: " + buttonClickCount);
        } else {
            Log.e(TAG, "MainActivity is null, cannot update counter in activity");
        }
    }

    public void setMainActivity(MainActivity activity) {
        Log.d(TAG, "setMainActivity called with activity: " + (activity != null));
        this.mainActivity = activity;
        if (activity != null) {
            updateCounterInActivity();
            showCounterNotification();
            Log.d(TAG, "MainActivity set and counter updated");
        } else {
            Log.e(TAG, "Setting null MainActivity");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
    }
} 