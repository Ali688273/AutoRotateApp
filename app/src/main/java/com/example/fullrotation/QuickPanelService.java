package com.example.fullrotation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class QuickPanelService extends Service {
    private WindowManager wm;
    private LinearLayout panel;
    private int index=0;
    private final String[] modes={"auto","portrait","landscape","reverse_portrait","reverse_landscape"};
    private String[] labels;
    private boolean fa;

    @Override public void onCreate(){
        super.onCreate();
        android.content.SharedPreferences prefs=getSharedPreferences("app_prefs",MODE_PRIVATE);
        fa="fa".equals(prefs.getString("language","fa"));
        labels=fa?new String[]{"خودکار","عمودی","افقی","عمودی معکوس","افقی معکوس"}:new String[]{"AUTO","PORTRAIT","LANDSCAPE","REVERSE P","REVERSE L"};
        String saved=prefs.getString("rotation_mode","auto");
        for(int n=0;n<modes.length;n++)if(modes[n].equals(saved)){index=n;break;}
        if("reading".equals(saved))index=1; else if("video".equals(saved))index=2;
        startForegroundNotification();
        if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this)){stopSelf();return;}
        wm=(WindowManager)getSystemService(WINDOW_SERVICE); buildPanel();
    }
    private void startForegroundNotification(){
        String id="rotation_panel";
        if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(id,"Rotation quick panel",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,id):new Notification.Builder(this);
        b.setContentTitle(fa?"چرخش صفحه":"Screen Rotation").setContentText(fa?"پنل شناور چرخش فعال است":"Floating rotation controls are active").setSmallIcon(android.R.drawable.ic_menu_rotate).setOngoing(true);
        startForeground(8102,b.build());
    }
    private void buildPanel(){
        panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(10,8,10,8);
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.BLACK);bg.setCornerRadius(24);bg.setStroke(2,Color.WHITE);panel.setBackground(bg);
        TextView title=new TextView(this);title.setText(fa?"↻ چرخش صفحه":"↻ ROTATION");title.setTextColor(Color.WHITE);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);panel.addView(title);
        Button cycle=new Button(this);cycle.setText(labels[index]+" ↻");cycle.setAllCaps(false);panel.addView(cycle);
        cycle.setOnClickListener(v->{index=(index+1)%modes.length;cycle.setText(labels[index]+"  ↻");
            android.content.SharedPreferences prefs=getSharedPreferences("app_prefs",MODE_PRIVATE);
            prefs.edit().putString("rotation_mode",modes[index]).putBoolean("is_enabled",true).apply();
            if(!prefs.getBoolean("original_settings_saved",false)){
                prefs.edit().putInt("original_accelerometer_rotation",Settings.System.getInt(getContentResolver(),Settings.System.ACCELEROMETER_ROTATION,1))
                    .putInt("original_user_rotation",Settings.System.getInt(getContentResolver(),Settings.System.USER_ROTATION,0)).putBoolean("original_settings_saved",true).apply();
            }
            apply(modes[index]);});
        Button close=new Button(this);close.setText(fa?"بستن ×":"Close ×");close.setAllCaps(false);panel.addView(close);close.setOnClickListener(v->stopSelf());
        int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.END;p.x=12;p.y=160;
        try{wm.addView(panel,p);}catch(Exception e){stopSelf();return;}
        title.setOnTouchListener(new android.view.View.OnTouchListener(){
            private int startX,startY; private float downX,downY;
            @Override public boolean onTouch(android.view.View v,android.view.MotionEvent event){
                switch(event.getAction()){
                    case android.view.MotionEvent.ACTION_DOWN:
                        startX=p.x;startY=p.y;downX=event.getRawX();downY=event.getRawY();return true;
                    case android.view.MotionEvent.ACTION_MOVE:
                        p.x=startX+(int)(downX-event.getRawX());p.y=startY+(int)(event.getRawY()-downY);
                        try{wm.updateViewLayout(panel,p);}catch(Exception ignored){}return true;
                    default:return true;
                }
            }
        });
    }
    private void apply(String mode){
        if(Build.VERSION.SDK_INT>=23&&!Settings.System.canWrite(this))return;
        try{
            if("auto".equals(mode)){Settings.System.putInt(getContentResolver(),Settings.System.USER_ROTATION,0);Settings.System.putInt(getContentResolver(),Settings.System.ACCELEROMETER_ROTATION,1);}
            else{int r;switch(mode){case "landscape":r=1;break;case "reverse_portrait":r=2;break;case "reverse_landscape":r=3;break;default:r=0;}
                Settings.System.putInt(getContentResolver(),Settings.System.USER_ROTATION,r);Settings.System.putInt(getContentResolver(),Settings.System.ACCELEROMETER_ROTATION,0);}
        }catch(SecurityException ignored){}
    }
    @Override public int onStartCommand(Intent i,int flags,int id){return START_STICKY;}
    @Override public void onDestroy(){if(panel!=null&&wm!=null)try{wm.removeView(panel);}catch(Exception ignored){}stopForeground(true);super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}