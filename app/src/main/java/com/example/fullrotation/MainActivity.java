package com.example.fullrotation;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private LinearLayout page, tabBar;
    private LinearLayout[] pages;
    private ScrollView[] tabScrolls;
    private Button[] tabButtons;
    private int activeTab = 0;
    private boolean dark;
    private String lang;
    private int bg, card, fg, muted, accent;
    private final String[] modes={"auto","portrait","reverse_portrait","landscape","reverse_landscape","reading","video"};
    private String[] labels;
    private TextView status;
    private FrameLayout standardBannerContainer;
    private FrameLayout nativeBannerContainer;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs=getSharedPreferences("app_prefs",MODE_PRIVATE);
        lang=prefs.getString("language","fa"); dark=prefs.getBoolean("dark_theme",false);
        setTheme(dark ? R.style.Theme_AutoRotateApp_Dark : R.style.Theme_AutoRotateApp);
        build();
    }
    private String t(String fa,String en){return lang.equals("fa")?fa:en;}
    private void build(){
        bg=dark?Color.rgb(12,12,12):Color.rgb(248,248,248);
        card=dark?Color.rgb(30,30,30):Color.WHITE; fg=dark?Color.WHITE:Color.BLACK;
        muted=dark?Color.LTGRAY:Color.DKGRAY; accent=dark?Color.WHITE:Color.BLACK;
        labels=lang.equals("fa")?new String[]{"خودکار با حسگر","عمودی","عمودی معکوس","افقی","افقی معکوس","مطالعه (عمودی)","ویدئو (افقی)"}:
            new String[]{"Auto sensor","Portrait","Reverse portrait","Landscape","Reverse landscape","Reading (portrait)","Video (landscape)"};
        if(Build.VERSION.SDK_INT>=21){getWindow().setStatusBarColor(dark?Color.BLACK:Color.WHITE);getWindow().setNavigationBarColor(dark?Color.BLACK:Color.WHITE);}
        if(Build.VERSION.SDK_INT>=23){int flags=dark?0:android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;if(Build.VERSION.SDK_INT>=26&&!dark)flags|=android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;getWindow().getDecorView().setSystemUiVisibility(flags);}
        pages=new LinearLayout[3];
        tabScrolls=new ScrollView[3];
        for(int i=0;i<3;i++){
            tabScrolls[i]=new ScrollView(this);
            tabScrolls[i].setFillViewport(true);
            tabScrolls[i].setBackgroundColor(bg);
            pages[i]=new LinearLayout(this);
            pages[i].setOrientation(LinearLayout.VERTICAL);
            pages[i].setPadding(dp(18),dp(18),dp(18),dp(30));
            tabScrolls[i].addView(pages[i]);
        }
        page=pages[0];
        tabBar=new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setPadding(dp(8),dp(4),dp(8),dp(8));
        tabButtons=new Button[3];
        String[] tabNames={t("چرخش","Rotation"),t("برنامه‌ها","Apps"),t("تنظیمات","Settings")};
        for(int i=0;i<3;i++){
            final int target=i;
            Button b=button(tabNames[i]);
            b.setTextSize(13);
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(46),1f);
            bp.setMargins(dp(3),0,dp(3),0);
            tabBar.addView(b,bp);
            tabButtons[i]=b;
            b.setOnClickListener(v->showTab(target));
        }
        TextView title=txt(t("چرخش صفحه","Screen Rotation"),28,true,fg); title.setGravity(Gravity.CENTER); add(title,0,8);
        TextView desc=txt(t("کنترل هوشمند جهت صفحه","Smart orientation control"),14,false,muted); desc.setGravity(Gravity.CENTER); add(desc,0,18);
        nativeBannerContainer = new FrameLayout(this);
        LinearLayout.LayoutParams nativeAdParams = new LinearLayout.LayoutParams(-1, -2);
        nativeAdParams.setMargins(0, dp(4), 0, dp(10));
        page.addView(nativeBannerContainer, nativeAdParams);

        section(t("کنترل عمومی","Global control"));
        Switch enabled=new Switch(this); enabled.setText(t("فعال‌سازی کنترل چرخش","Enable rotation control")); enabled.setTextColor(fg);
        enabled.setChecked(prefs.getBoolean("is_enabled",false)); cardView(enabled);
        enabled.setOnCheckedChangeListener((v,on)->{
            if(on&&!canWrite()){v.setChecked(false);requestWrite();return;}
            prefs.edit().putBoolean("is_enabled",on).apply();
            if(on){saveOriginal();apply(prefs.getString("rotation_mode","auto"));}
            else restoreOriginal();
            updateStatus();
        });

        section(t("حالت پیش‌فرض","Default orientation"));
        Button mode=button(""); setModeButton(mode); add(mode,4,6);
        mode.setOnClickListener(v->chooseMode(null));
        Button apply=button(t("اعمال حالت انتخاب‌شده","Apply selected mode")); add(apply,4,6);
        apply.setOnClickListener(v->{if(!canWrite()){requestWrite();return;} boolean ok=apply(prefs.getString("rotation_mode","auto")); toast(ok?t("حالت اعمال شد","Orientation applied"):t("اعمال نشد؛ مجوز و سازگاری دستگاه را بررسی کن","Could not apply; check permission and device support"));
            });

        page=pages[1];
        section(t("قوانین اختصاصی برنامه‌ها","Per-app rules"));
        add(txt(t("ابتدا کنترل عمومی را فعال کن؛ سپس برای هر برنامه جهت جداگانه تعیین کن و سرویس دسترس‌پذیری را هم فعال کن.","First enable global rotation control, then set a separate orientation for each app and enable the Accessibility service."),14,false,muted),4,6);
        Button choose=button(t("انتخاب برنامه و جهت","Choose app and orientation")); add(choose,4,6); choose.setOnClickListener(v->chooseApp());
        Button access=button(t("فعال‌سازی سرویس دسترس‌پذیری","Enable Accessibility service")); add(access,4,6);
        access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        renderRules();

        page=pages[2];
        section(t("پنل شناور سریع","Floating quick panel"));
        add(txt(t("پنل شناور تغییر سریع جهت؛ نیازمند مجوز نمایش روی برنامه‌ها.","Floating orientation controls; overlay permission is required."),14,false,muted),4,6);
        Button floating=button(t("فعال‌سازی پنل شناور","Start floating panel")); add(floating,4,6);
        floating.setOnClickListener(v->{
            if(!canWrite()){requestWrite();return;}
            if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this)){
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));
            }else{Intent i=new Intent(this,QuickPanelService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}
        });

        section(t("زبان و ظاهر","Language & appearance"));
        Button language=button(lang.equals("fa")?"English":"فارسی"); add(language,4,6);
        language.setOnClickListener(v->{prefs.edit().putString("language",lang.equals("fa")?"en":"fa").apply();recreate();});
        Button theme=button(t(dark?"تغییر به تم سفید":"تغییر به تم سیاه",dark?"Switch to white theme":"Switch to black theme")); add(theme,4,6);
        theme.setOnClickListener(v->{prefs.edit().putBoolean("dark_theme",!dark).apply();recreate();});

        section(t("پاداش اختیاری","Optional reward"));
        Button rewardAd=button(t("تماشای تبلیغ و دریافت ۲۴ ساعت بدون تبلیغ تمام‌صفحه","Watch an ad for 24 hours without full-screen ads")); add(rewardAd,4,6);
        rewardAd.setOnClickListener(v->AdsManager.showRewardedAd(this));

        section(t("وضعیت و عیب‌یابی","Status & troubleshooting"));
        status=txt("",14,true,fg); status.setPadding(dp(12),dp(12),dp(12),dp(12)); cardView(status);
        Button permission=button(t("مجوز تنظیمات سیستم","System settings permission")); add(permission,4,6); permission.setOnClickListener(v->requestWrite());
        Button battery=button(t("راهنمای باتری و پس‌زمینه","Battery/background help")); add(battery,4,6);
        battery.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(t("پایداری پس‌زمینه","Background reliability"))
            .setMessage(t("برای اجرای قوانین برنامه‌ها، در تنظیمات باتری این برنامه را روی «بدون محدودیت» قرار بده. قوانین برنامه‌ها به سرویس دسترس‌پذیری و مجوز تنظیمات سیستم نیاز دارند.","Set battery usage to Unrestricted for reliable per-app rules. Per-app switching requires Accessibility and system settings permission."))
            .setPositiveButton(t("تنظیمات باتری","Battery settings"),(d,w)->{try{startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));}catch(Exception ignored){}}).setNegativeButton(t("بستن","Close"),null).show());
        updateStatus();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        root.addView(tabBar,new LinearLayout.LayoutParams(-1,-2));
        for(int i=0;i<3;i++)root.addView(tabScrolls[i],new LinearLayout.LayoutParams(-1,0,1f));
        standardBannerContainer = new FrameLayout(this);
        standardBannerContainer.setMinimumHeight(dp(50));
        root.addView(standardBannerContainer, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        showTab(0);
        AdsManager.loadNativeBanner(this, nativeBannerContainer);
        AdsManager.loadStandardBanner(this, standardBannerContainer);
        AdsManager.prepareFullScreen(this);
    }
    private void showTab(int tab){
        activeTab=Math.max(0,Math.min(2,tab));
        if(tabScrolls==null)return;
        for(int i=0;i<tabScrolls.length;i++){
            tabScrolls[i].setVisibility(i==activeTab?View.VISIBLE:View.GONE);
            if(tabButtons!=null&&tabButtons[i]!=null){
                tabButtons[i].setTextColor(i==activeTab?(dark?Color.BLACK:Color.WHITE):fg);
                tabButtons[i].setBackground(shape(i==activeTab?accent:card));
            }
        }
    }
    private void section(String s){TextView v=txt(s,18,true,fg);add(v,dp(20),dp(8));}
    private TextView txt(String s,int size,boolean bold,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setGravity(lang.equals("fa")?Gravity.RIGHT:Gravity.LEFT);v.setPadding(dp(4),dp(4),dp(4),dp(4));return v;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(dark?Color.BLACK:Color.WHITE);b.setAllCaps(false);b.setBackground(shape(accent));return b;}
    private void add(android.view.View v,int top,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,top,0,bottom);page.addView(v,p);}
    private void cardView(android.view.View v){LinearLayout box=new LinearLayout(this);box.setPadding(dp(12),dp(10),dp(12),dp(10));box.setBackground(shape(card));box.addView(v,new LinearLayout.LayoutParams(-1,-2));add(box,dp(4),dp(6));}
    private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));if(!dark)d.setStroke(dp(1),0xffdddddd);return d;}
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    private void setModeButton(Button b){b.setText(labels[index(prefs.getString("rotation_mode","auto"))]+"  ▾");}
    private int index(String m){for(int i=0;i<modes.length;i++)if(modes[i].equals(m))return i;return 0;}
    private void chooseMode(String pkg){
        String current=pkg==null?prefs.getString("rotation_mode","auto"):(prefs.contains("rule_"+pkg)?prefs.getString("rule_"+pkg,"auto"):prefs.getString("rotation_mode","auto"));
        int selectedIndex = pkg != null && !prefs.contains("rule_"+pkg) ? modes.length : index(current);
        String[] options=labels;
        if(pkg!=null){options=Arrays.copyOf(labels,labels.length+1);options[labels.length]=t("استفاده از حالت پیش‌فرض","Use global default");}
        final String[] choices=options;
        new AlertDialog.Builder(this).setTitle(t("انتخاب جهت","Choose orientation")).setSingleChoiceItems(choices,selectedIndex,(d,n)->{
            if(pkg==null){prefs.edit().putString("rotation_mode",modes[n]).apply();if(prefs.getBoolean("is_enabled",false)&&canWrite())apply(modes[n]);}
            else if(n==modes.length)prefs.edit().remove("rule_"+pkg).apply();
            else prefs.edit().putString("rule_"+pkg,modes[n]).apply();
            d.dismiss();recreate();
        }).setNegativeButton(t("لغو","Cancel"),null).show();
    }
    private void chooseApp(){
        toast(t("فهرست برنامه‌ها در حال آماده‌شدن است…","Loading installed apps…"));
        new Thread(()->{
            Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);
            List<android.content.pm.ResolveInfo> apps=getPackageManager().queryIntentActivities(i,0);
            List<AppChoice> filtered=new ArrayList<>();
            for(android.content.pm.ResolveInfo a:apps){
                if(a.activityInfo==null||a.activityInfo.packageName.equals(getPackageName()))continue;
                String label;
                try{label=String.valueOf(a.loadLabel(getPackageManager()));}catch(Exception e){label=a.activityInfo.packageName;}
                filtered.add(new AppChoice(label,a.activityInfo.packageName));
            }
            Collections.sort(filtered,Comparator.comparing(a->a.label.toLowerCase(Locale.ROOT)));
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                if(filtered.isEmpty()){toast(t("برنامه‌ای برای انتخاب پیدا نشد.","No launchable apps were found."));return;}
                String[] names=new String[filtered.size()];
                for(int n=0;n<names.length;n++)names[n]=filtered.get(n).label;
                new AlertDialog.Builder(this).setTitle(t("انتخاب برنامه","Choose app")).setItems(names,(d,n)->chooseMode(filtered.get(n).pkg)).setNegativeButton(t("لغو","Cancel"),null).show();
            });
        },"load-launcher-apps").start();
    }
    private static final class AppChoice{
        final String label,pkg;
        AppChoice(String label,String pkg){this.label=label;this.pkg=pkg;}
    }
    private void renderRules(){
        List<String> keys=new ArrayList<>();for(String k:prefs.getAll().keySet())if(k.startsWith("rule_"))keys.add(k.substring(5));Collections.sort(keys);
        if(keys.isEmpty()){add(txt(t("هنوز قانونی ثبت نشده است.","No app-specific rules saved yet."),13,false,muted),4,4);return;}
        for(String pkg:keys){String label=pkg;try{label=String.valueOf(getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)));}catch(Exception ignored){}
            final String target=pkg;Button b=button(label+" — "+labels[index(prefs.getString("rule_"+pkg,"auto"))]);add(b,3,3);b.setOnClickListener(v->chooseMode(target));}
    }
    private boolean canWrite(){return Build.VERSION.SDK_INT<23||Settings.System.canWrite(this);}
    private void requestWrite(){if(Build.VERSION.SDK_INT>=23&&!Settings.System.canWrite(this))startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName())));else toast(t("مجوز تنظیمات فعال است","Settings permission granted"));}
    private void saveOriginal(){if(prefs.getBoolean("original_settings_saved",false))return;prefs.edit().putInt("original_accelerometer_rotation",Settings.System.getInt(getContentResolver(),Settings.System.ACCELEROMETER_ROTATION,1)).putInt("original_user_rotation",Settings.System.getInt(getContentResolver(),Settings.System.USER_ROTATION,0)).putBoolean("original_settings_saved",true).apply();}
    private void restoreOriginal(){if(!canWrite()||!prefs.getBoolean("original_settings_saved",false))return;try{Settings.System.putInt(getContentResolver(),Settings.System.USER_ROTATION,prefs.getInt("original_user_rotation",0));Settings.System.putInt(getContentResolver(),Settings.System.ACCELEROMETER_ROTATION,prefs.getInt("original_accelerometer_rotation",1));prefs.edit().putBoolean("original_settings_saved",false).apply();}catch(SecurityException ignored){}}
    private boolean apply(String mode){
        if(!canWrite())return false;try{if("auto".equals(mode))return Settings.System.putInt(getContentResolver(),Settings.System.ACCELEROMETER_ROTATION,1);
            int r;switch(mode){case "landscape":case "video":r=1;break;case "reverse_portrait":r=2;break;case "reverse_landscape":r=3;break;default:r=0;}
            boolean a=Settings.System.putInt(getContentResolver(),Settings.System.USER_ROTATION,r);boolean b=Settings.System.putInt(getContentResolver(),Settings.System.ACCELEROMETER_ROTATION,0);return a&&b;
        }catch(SecurityException e){return false;}
    }
    private boolean accessibilityEnabled(){AccessibilityManager m=(AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE);if(m==null)return false;for(AccessibilityServiceInfo i:m.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK))if(i.getId()!=null&&i.getId().contains("PerAppRotationService"))return true;return false;}
    @Override protected void onPause(){ AdsManager.onActivityPaused(); super.onPause(); }
    @Override protected void onResume(){ super.onResume(); if(prefs!=null&&prefs.getBoolean("is_enabled",false)&&canWrite())apply(prefs.getString("rotation_mode","auto")); if(status!=null)updateStatus(); }
    @Override protected void onDestroy(){ AdsManager.cleanup(this, standardBannerContainer); super.onDestroy(); }
    private void updateStatus(){if(status==null)return;status.setText((prefs.getBoolean("is_enabled",false)?t("کنترل عمومی: فعال","Global control: ON"):t("کنترل عمومی: خاموش","Global control: OFF"))+"\n"+(canWrite()?t("مجوز تنظیمات: فعال","Settings permission: granted"):t("مجوز تنظیمات: لازم است","Settings permission: required"))+"\n"+(accessibilityEnabled()?t("قوانین برنامه‌ها: آماده","Per-app rules: ready"):t("قوانین برنامه‌ها: سرویس دسترس‌پذیری را فعال کن","Per-app rules: enable Accessibility service")));}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}