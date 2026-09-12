package com.smdey.ads.sdk.nativead;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest;
import com.smdey.ads.sdk.AdsConfig;
import com.smdey.ads.sdk.consent.ConsentManager;
import com.smdey.ads.sdk.core.AppExecutors;
import com.smdey.ads.sdk.core.LifecycleGuard;
import com.smdey.ads.sdk.core.SdkGate;

import java.util.Collections;

public final class NativeAdManager {

    /**
     * Callback for asynchronous Native ad load requests.
     * Guaranteed to execute on the Android UI (Main) thread.
     */
    public interface OnNativeAdLoadedListener {
        @androidx.annotation.MainThread
        void onAdLoaded(@NonNull NativeAd nativeAd);

        @androidx.annotation.MainThread
        void onAdFailedToLoad(@Nullable LoadAdError error);
    }

    private final AdsConfig config;
    private final SdkGate sdkGate;
    private final ConsentManager consentManager;

    public NativeAdManager(@NonNull AdsConfig config,
                           @NonNull SdkGate sdkGate,
                           @NonNull ConsentManager consentManager) {
        this.config = config;
        this.sdkGate = sdkGate;
        this.consentManager = consentManager;
    }

    public void load(@NonNull Activity activity, @NonNull OnNativeAdLoadedListener listener) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            Log.d(config.getTag(), "⚠️ NativeAd - Activity is invalid or finishing. Aborting load.");
            listener.onAdFailedToLoad(null);
            return;
        }

        if (config.isAdsRemoved()) {
            Log.d(config.getTag(), "💎 NativeAd - Ads removed (VIP user).");
            listener.onAdFailedToLoad(null);
            return;
        }

        if (!config.isNativeEnabled()) {
            Log.d(config.getTag(), "⚠️ NativeAd - Format is disabled in config.");
            listener.onAdFailedToLoad(null);
            return;
        }

        String adUnitId = config.getNativeId();
        if (adUnitId == null || adUnitId.trim().isEmpty()) {
            Log.d(config.getTag(), "⚠️ NativeAd - No ad unit ID configured.");
            listener.onAdFailedToLoad(null);
            return;
        }

        sdkGate.ensureInitialized(activity, () -> {
            if (!LifecycleGuard.isActivityValid(activity)) {
                return;
            }

            Log.i(config.getTag(), "⏳ NativeAd - Requesting ad: " + adUnitId);

            NativeAdRequest request = new NativeAdRequest.Builder(
                    adUnitId,
                    Collections.singletonList(NativeAd.NativeAdType.NATIVE)
            ).build();

            NativeAdLoader.load(request, new NativeAdLoaderCallback() {
                @Override
                public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                    AppExecutors.getInstance().mainThread().execute(() -> {
                        if (!LifecycleGuard.isActivityValid(activity)) {
                            nativeAd.destroy();
                            return;
                        }
                        Log.i(config.getTag(), "✅ NativeAd - Successfully loaded: " + adUnitId);
                        listener.onAdLoaded(nativeAd);
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    AppExecutors.getInstance().mainThread().execute(() -> {
                        Log.w(config.getTag(), "❌ NativeAd - Load failed: " + loadAdError.getMessage()
                                + " (code: " + loadAdError.getCode() + ")");
                        listener.onAdFailedToLoad(loadAdError);
                    });
                }
            });
        });
    }
}
