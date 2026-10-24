package com.example.fullrotation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class MainActivity extends AppCompatActivity {

    private SwitchCompat switchRotation;
    private TextView tvStatus;
    private Spinner spinnerMode;
    private SharedPreferences prefs;
    private boolean changingSwitch;

    private final String[] modeLabels = {
            "چرخش خودکار با حسگر",
            "عمودی",
            "عمودی وارونه (۱۸۰ درجه)",
            "افقی",
            "افقی وارونه"
    };

    private final String[] modeValues = {"auto", "portrait", "reverse_portrait", "landscape", "reverse_landscape"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchRotation = findViewById(R.id.switchRotation);
        tvStatus = findViewById(R.id.tvStatus);
        spinnerMode = findViewById(R.id.spinnerMode);
        Button buttonApply = findViewById(R.id.buttonApply);
        Button buttonPermission = findViewById(R.id.buttonPermission);
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, modeLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMode.setAdapter(adapter);

        String savedMode = prefs.getString("rotation_mode", "auto");
        spinnerMode.setSelection(indexOfMode(savedMode));
        switchRotation.setChecked(prefs.getBoolean("is_enabled", false));
        updateStatus();

        switchRotation.setOnCheckedChangeListener((button, checked) -> {
            if (changingSwitch) return;
            if (checked) {
                if (!canWriteSettings()) {
                    changingSwitch = true;
                    switchRotation.setChecked(false);
                    changingSwitch = false;
                    requestWriteSettings();
                    return;
                }
                enableRotation();
            } else {
                disableRotation();
            }
        });

        buttonApply.setOnClickListener(v -> {
            String mode = modeValues[spinnerMode.getSelectedItemPosition()];
            prefs.edit().putString("rotation_mode", mode).apply();
            if (prefs.getBoolean("is_enabled", false)) {
                if (!canWriteSettings()) {
                    requestWriteSettings();
                    return;
                }
                if (applyMode(mode)) {
                    Toast.makeText(this, "حالت چرخش اعمال شد", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "گوشی اجازه تغییر تنظیم چرخش را نداد", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "حالت ذخیره شد؛ برای فعال شدن کلید را روشن کنید", Toast.LENGTH_SHORT).show();
            }
            updateStatus();
        });

        buttonPermission.setOnClickListener(v -> requestWriteSettings());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs != null && prefs.getBoolean("is_enabled", false) && canWriteSettings()) {
            applyMode(prefs.getString("rotation_mode", "auto"));
        }
        updateStatus();
    }

    private boolean canWriteSettings() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(this);
    }

    private void requestWriteSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            Toast.makeText(this, "مجوز تغییر تنظیمات سیستم فعال است", Toast.LENGTH_SHORT).show();
        }
    }

    private void enableRotation() {
        if (!canWriteSettings()) {
            requestWriteSettings();
            return;
        }
        if (!prefs.getBoolean("original_settings_saved", false)) {
            int autoRotate = Settings.System.getInt(getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, 1);
            int userRotation = Settings.System.getInt(getContentResolver(),
                    Settings.System.USER_ROTATION, 0);
            prefs.edit()
                    .putInt("original_accelerometer_rotation", autoRotate)
                    .putInt("original_user_rotation", userRotation)
                    .putBoolean("original_settings_saved", true)
                    .apply();
        }

        String mode = modeValues[spinnerMode.getSelectedItemPosition()];
        prefs.edit().putBoolean("is_enabled", true).putString("rotation_mode", mode).apply();
        if (!applyMode(mode)) {
            prefs.edit().putBoolean("is_enabled", false).apply();
            changingSwitch = true;
            switchRotation.setChecked(false);
            changingSwitch = false;
            Toast.makeText(this, "تغییر چرخش در این دستگاه مجاز نشد", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "کنترل چرخش فعال شد", Toast.LENGTH_SHORT).show();
        }
        updateStatus();
    }

    private void disableRotation() {
        prefs.edit().putBoolean("is_enabled", false).apply();
        if (canWriteSettings() && prefs.getBoolean("original_settings_saved", false)) {
            try {
                Settings.System.putInt(getContentResolver(), Settings.System.USER_ROTATION,
                        prefs.getInt("original_user_rotation", 0));
                Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION,
                        prefs.getInt("original_accelerometer_rotation", 1));
                prefs.edit().putBoolean("original_settings_saved", false).apply();
            } catch (SecurityException ignored) {
                Toast.makeText(this, "تنظیم قبلی بازیابی نشد؛ مجوز تنظیمات را بررسی کنید", Toast.LENGTH_LONG).show();
            }
        }
        updateStatus();
        Toast.makeText(this, "کنترل چرخش غیرفعال شد", Toast.LENGTH_SHORT).show();
    }

    private boolean applyMode(String mode) {
        if (!canWriteSettings()) return false;
        try {
            if ("auto".equals(mode)) {
                return Settings.System.putInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION, 1);
            }

            int rotation;
            switch (mode) {
                case "portrait": rotation = 0; break;
                case "landscape": rotation = 1; break;
                case "reverse_portrait": rotation = 2; break;
                case "reverse_landscape": rotation = 3; break;
                default: rotation = 0;
            }
            boolean rotationSet = Settings.System.putInt(getContentResolver(),
                    Settings.System.USER_ROTATION, rotation);
            boolean autoDisabled = Settings.System.putInt(getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, 0);
            return rotationSet && autoDisabled;
        } catch (SecurityException e) {
            return false;
        }
    }

    private int indexOfMode(String mode) {
        for (int i = 0; i < modeValues.length; i++) {
            if (modeValues[i].equals(mode)) return i;
        }
        return 0;
    }

    private void updateStatus() {
        if (tvStatus == null || prefs == null) return;
        boolean enabled = prefs.getBoolean("is_enabled", false);
        if (enabled && !canWriteSettings()) {
            tvStatus.setText("وضعیت: نیازمند مجوز تنظیمات سیستم");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else if (enabled) {
            tvStatus.setText("وضعیت: فعال — " + modeLabels[indexOfMode(prefs.getString("rotation_mode", "auto"))]);
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvStatus.setText("وضعیت: غیرفعال");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }
}
