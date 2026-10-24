package com.example.fullrotation;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;

public class PerAppRotationService extends AccessibilityService {
    private SharedPreferences prefs;
    private String lastPackage = "";
    private String lastMode = "";

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        if (prefs == null) prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("is_enabled", false)) return;
        CharSequence cs = event.getPackageName();
        if (cs == null) return;
        String pkg = cs.toString();
        if (pkg.equals(getPackageName())) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) return;
        String mode = prefs.getString("rule_" + pkg, prefs.getString("rotation_mode", "auto"));
        if (pkg.equals(lastPackage) && mode.equals(lastMode)) return;
        if (applyMode(mode)) { lastPackage = pkg; lastMode = mode; }
    }

    private boolean applyMode(String mode) {
        try {
            if ("auto".equals(mode)) return Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1);
            int rotation;
            switch (mode) {
                case "landscape": case "video": rotation = 1; break;
                case "reverse_portrait": rotation = 2; break;
                case "reverse_landscape": rotation = 3; break;
                default: rotation = 0;
            }
            boolean a = Settings.System.putInt(getContentResolver(), Settings.System.USER_ROTATION, rotation);
            boolean b = Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
            return a && b;
        } catch (SecurityException ignored) { return false; }
    }
    @Override public void onInterrupt() { }
}