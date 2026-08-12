package com.example.fullrotation;

import android.os.Bundle;
import android.content.pm.ActivityInfo;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // فعال‌سازی چرخش کاملاً آزاد (شامل ۱۸۰ درجه و برعکس عمودی)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }
}
