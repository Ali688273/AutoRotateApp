package com.example.fullrotation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("is_enabled", false)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            return;
        }

        String mode = prefs.getString("rotation_mode", "auto");
        try {
            if ("auto".equals(mode)) {
                Settings.System.putInt(context.getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION, 1);
            } else {
                int rotation;
                switch (mode) {
                    case "portrait": rotation = 0; break;
                    case "landscape": rotation = 1; break;
                    case "reverse_portrait": rotation = 2; break;
                    case "reverse_landscape": rotation = 3; break;
                    default: rotation = 0;
                }
                Settings.System.putInt(context.getContentResolver(),
                        Settings.System.USER_ROTATION, rotation);
                Settings.System.putInt(context.getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION, 0);
            }

            Intent serviceIntent = new Intent(context, RotationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (SecurityException ignored) {
            // User can reopen the app and grant the special system-settings permission.
        }
    }
}
