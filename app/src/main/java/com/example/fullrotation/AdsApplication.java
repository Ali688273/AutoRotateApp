package com.example.fullrotation;

import android.app.Application;
import android.util.Log;

import com.adivery.sdk.Adivery;
import ir.tapsell.plus.TapsellPlus;
import ir.tapsell.plus.TapsellPlusInitListener;
import ir.tapsell.plus.model.AdNetworkError;
import ir.tapsell.plus.model.AdNetworks;

/** Initializes advertising SDKs once, without tying initialization to an Activity. */
public final class AdsApplication extends Application {
    private static final String TAG = "AutoRotateAds";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Adivery.setLoggingEnabled(BuildConfig.DEBUG);
            Adivery.configure(this, AdsConfig.ADIVERY_APP_ID);
        } catch (Throwable error) {
            Log.e(TAG, "Adivery initialization failed", error);
        }

        try {
            TapsellPlus.initialize(this, AdsConfig.TAPSELL_KEY, new TapsellPlusInitListener() {
                @Override
                public void onInitializeSuccess(AdNetworks adNetworks) {
                    Log.d(TAG, "TapsellPlus initialized: " + adNetworks.name());
                }

                @Override
                public void onInitializeFailed(AdNetworks adNetworks, AdNetworkError error) {
                    Log.e(TAG, "TapsellPlus initialization failed for " + adNetworks.name()
                            + ": " + (error == null ? "unknown error" : error.getErrorMessage()));
                }
            });
        } catch (Throwable error) {
            Log.e(TAG, "TapsellPlus initialization failed", error);
        }
    }
}
