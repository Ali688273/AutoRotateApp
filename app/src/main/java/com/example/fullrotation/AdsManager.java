package com.example.fullrotation;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.adivery.sdk.Adivery;
import com.adivery.sdk.AdiveryListener;
import com.adivery.sdk.AdiveryAdListener;
import com.adivery.sdk.AdiveryBannerAdView;
import com.adivery.sdk.BannerSize;

import ir.tapsell.plus.AdHolder;
import ir.tapsell.plus.AdRequestCallback;
import ir.tapsell.plus.AdShowListener;
import ir.tapsell.plus.TapsellPlus;
import ir.tapsell.plus.TapsellPlusBannerType;
import ir.tapsell.plus.model.TapsellPlusAdModel;
import ir.tapsell.plus.model.TapsellPlusErrorModel;

/** Best-effort ad loading with lifecycle cleanup and a shared full-screen frequency cap. */
public final class AdsManager {
    private static final String TAG = "AutoRotateAds";
    private static String standardBannerResponse;
    private static String nativeBannerResponse;
    private static String interstitialResponse;
    private static long lastFullScreenAt;
    private static long lastPauseAt;
    private static boolean appOpenPrepared;

    private AdsManager() {}

    public static void loadStandardBanner(Activity activity, FrameLayout container) {
        if (!AdsApplication.isTapsellReady() || activity.isFinishing() || activity.isDestroyed()) return;
        try {
            TapsellPlus.requestStandardBannerAd(activity, AdsConfig.TAPSELL_STANDARD_BANNER,
                    TapsellPlusBannerType.BANNER_320x50, new AdRequestCallback() {
                        @Override public void response(TapsellPlusAdModel model) {
                            super.response(model);
                            if (activity.isFinishing() || activity.isDestroyed() || model == null) return;
                            standardBannerResponse = model.getResponseId();
                            if (standardBannerResponse == null || standardBannerResponse.isEmpty()) return;
                            try {
                                TapsellPlus.showStandardBannerAd(activity, standardBannerResponse, container,
                                        new AdShowListener() {
                                            @Override public void onOpened(TapsellPlusAdModel ad) {
                                                super.onOpened(ad);
                                            }
                                            @Override public void onError(TapsellPlusErrorModel error) {
                                                super.onError(error);
                                                Log.w(TAG, "Tapsell standard banner show error: " + error);
                                            }
                                        });
                            } catch (Throwable error) {
                                Log.w(TAG, "Tapsell standard banner could not be shown", error);
                            }
                        }
                        @Override public void error(@NonNull String message) {
                            Log.w(TAG, "Tapsell standard banner request failed: " + message);
                        }
                    });
        } catch (Throwable error) {
            Log.w(TAG, "Tapsell standard banner request failed safely", error);
        }
    }

    public static void loadNativeBanner(Activity activity, FrameLayout container) {
        if (!AdsApplication.isTapsellReady() || activity.isFinishing() || activity.isDestroyed()) return;
        try {
            AdHolder holder = TapsellPlus.createAdHolder(activity, container, ir.tapsell.plus.R.layout.native_banner);
            TapsellPlus.requestNativeAd(activity, AdsConfig.TAPSELL_NATIVE_BANNER, new AdRequestCallback() {
                @Override public void response(TapsellPlusAdModel model) {
                    super.response(model);
                    if (activity.isFinishing() || activity.isDestroyed() || model == null) return;
                    nativeBannerResponse = model.getResponseId();
                    if (nativeBannerResponse == null || nativeBannerResponse.isEmpty()) return;
                    try {
                        TapsellPlus.showNativeAd(activity, nativeBannerResponse, holder, new AdShowListener() {
                            @Override public void onOpened(TapsellPlusAdModel ad) { super.onOpened(ad); }
                            @Override public void onError(TapsellPlusErrorModel error) {
                                super.onError(error);
                                Log.w(TAG, "Tapsell native banner show error: " + error);
                            }
                        });
                    } catch (Throwable error) {
                        Log.w(TAG, "Tapsell native banner could not be shown", error);
                    }
                }
                @Override public void error(@NonNull String message) {
                    Log.w(TAG, "Tapsell native banner request failed: " + message);
                }
            });
        } catch (Throwable error) {
            Log.w(TAG, "Tapsell native banner request failed safely", error);
        }
    }

    public static void prepareFullScreen(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        try {
            Adivery.addPlacementListener(AdsConfig.ADIVERY_INTERSTITIAL, new AdiveryListener() {
                @Override public void onInterstitialAdLoaded(@NonNull String placementId) { }
                @Override public void onInterstitialAdShown(@NonNull String placementId) { lastFullScreenAt = System.currentTimeMillis(); }
                @Override public void onInterstitialAdClicked(@NonNull String placementId) { }
                @Override public void onInterstitialAdClosed(@NonNull String placementId) { }
                @Override public void log(@NonNull String placementId, String message) {
                    Log.d(TAG, "Adivery interstitial: " + message);
                }
            });
            Adivery.prepareInterstitialAd(activity, AdsConfig.ADIVERY_INTERSTITIAL);
            Adivery.addPlacementListener(AdsConfig.ADIVERY_APP_OPEN, new AdiveryListener() {
                @Override public void onAppOpenAdLoaded(@NonNull String placementId) { appOpenPrepared = true; }
                @Override public void onAppOpenAdShown(@NonNull String placementId) {
                    appOpenPrepared = false;
                    lastFullScreenAt = System.currentTimeMillis();
                }
                @Override public void onAppOpenAdClicked(@NonNull String placementId) { }
                @Override public void onAppOpenAdClosed(@NonNull String placementId) {
                    appOpenPrepared = false;
                    try { Adivery.prepareAppOpenAd(activity, AdsConfig.ADIVERY_APP_OPEN); }
                    catch (Throwable error) { Log.w(TAG, "Could not reload app-open ad", error); }
                }
                @Override public void log(@NonNull String placementId, String message) {
                    Log.d(TAG, "Adivery app-open: " + message);
                }
            });
            Adivery.prepareAppOpenAd(activity, AdsConfig.ADIVERY_APP_OPEN);
        } catch (Throwable error) {
            Log.w(TAG, "Adivery full-screen ad preparation failed safely", error);
        }
    }

    public static void onActivityPaused() {
        lastPauseAt = System.currentTimeMillis();
    }

    public static void onActivityResumed(Activity activity) {
        long away = System.currentTimeMillis() - lastPauseAt;
        if (lastPauseAt > 0 && away >= 5000 && away < 6 * 60 * 60 * 1000L
                && System.currentTimeMillis() - lastFullScreenAt >= 10 * 60 * 1000L
                && appOpenPrepared && !activity.isFinishing() && !activity.isDestroyed()) {
            try {
                Adivery.showAppOpenAd(activity, AdsConfig.ADIVERY_APP_OPEN);
                appOpenPrepared = false;
            } catch (Throwable error) {
                Log.w(TAG, "App-open ad skipped safely", error);
            }
        } else {
            try {
                if (!Adivery.isLoaded(AdsConfig.ADIVERY_APP_OPEN)) {
                    Adivery.prepareAppOpenAd(activity, AdsConfig.ADIVERY_APP_OPEN);
                }
            } catch (Throwable ignored) { }
        }
        try {
            if (!Adivery.isLoaded(AdsConfig.ADIVERY_INTERSTITIAL)) {
                Adivery.prepareInterstitialAd(activity, AdsConfig.ADIVERY_INTERSTITIAL);
            }
        } catch (Throwable ignored) { }
    }

    public static void maybeShowInterstitial(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()
                || System.currentTimeMillis() - lastFullScreenAt < 10 * 60 * 1000L) return;
        try {
            String placement = AdsConfig.ADIVERY_INTERSTITIAL;
            if (Adivery.isLoaded(placement)) {
                Adivery.showAd(placement);
            } else {
                Adivery.prepareInterstitialAd(activity, placement);
            }
        } catch (Throwable error) {
            Log.w(TAG, "Interstitial skipped safely", error);
        }
    }

    public static void cleanup(Activity activity, FrameLayout bannerContainer) {
        try {
            if (standardBannerResponse != null) {
                TapsellPlus.destroyStandardBanner(activity, standardBannerResponse, bannerContainer);
                standardBannerResponse = null;
            }
        } catch (Throwable error) { Log.d(TAG, "Standard banner cleanup skipped", error); }
        try {
            if (nativeBannerResponse != null) {
                TapsellPlus.destroyNativeBanner(activity, nativeBannerResponse);
                nativeBannerResponse = null;
            }
        } catch (Throwable error) { Log.d(TAG, "Native banner cleanup skipped", error); }
        try { Adivery.removePlacementListener(AdsConfig.ADIVERY_INTERSTITIAL); } catch (Throwable ignored) { }
        try { Adivery.removePlacementListener(AdsConfig.ADIVERY_APP_OPEN); } catch (Throwable ignored) { }
    }
}
