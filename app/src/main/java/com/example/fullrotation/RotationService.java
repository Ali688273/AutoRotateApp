package com.example.fullrotation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import androidx.core.app.NotificationCompat;

public class RotationService extends Service {

    private View overlayView;
    private WindowManager windowManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundService();

        if (overlayView == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            overlayView = new View(this);

            int layoutFlag;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
            }

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    0, 0,
                    layoutFlag,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSPARENT
            );

            // فعال‌سازی چرخش آزاد ۳۶۰ درجه
            params.screenOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;

            try {
                windowManager.addView(overlayView, params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return START_STICKY;
    }

    private void startForegroundService() {
        String channelId = "rotation_service_channel";
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "سرویس چرخش صفحه",
                    NotificationManager.IMPORTANCE_LOW
            );
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("چرخش خودکار")
                .setContentText("برنامه چرخش خودکار در پس‌زمینه فعال است.")
                .setSmallIcon(R.drawable.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (windowManager != null && overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
              }
