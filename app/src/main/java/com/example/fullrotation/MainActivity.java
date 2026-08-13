package com.example.fullrotation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class MainActivity extends AppCompatActivity {

    private SwitchCompat switchRotation;
    private TextView tvStatus;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchRotation = findViewById(R.id.switchRotation);
        tvStatus = findViewById(R.id.tvStatus);
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        // بررسی و درخواست مجوز پنجره شناور (Overlay)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 101);
        }

        // تنظیم وضعیت اولیه کلید براساس تنظیمات ذخیره‌شده
        boolean isEnabled = prefs.getBoolean("is_enabled", false);
        switchRotation.setChecked(isEnabled);
        updateStatusText(isEnabled);

        switchRotation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("is_enabled", isChecked).apply();
            updateStatusText(isChecked);

            Intent serviceIntent = new Intent(MainActivity.this, RotationService.class);
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                Toast.makeText(MainActivity.this, "چرخش خودکار فعال شد", Toast.LENGTH_SHORT).show();
            } else {
                stopService(serviceIntent);
                Toast.makeText(MainActivity.this, "چرخش خودکار غیرفعال شد", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStatusText(boolean isEnabled) {
        if (isEnabled) {
            tvStatus.setText("وضعیت: فعال");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvStatus.setText("وضعیت: غیرفعال");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }
                    }
