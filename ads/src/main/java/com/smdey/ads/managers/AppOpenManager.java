package com.smdey.ads.managers;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.smdey.ads.callbacks.OpenAdVisibilityControl;
import com.smdey.ads.core.AdsConfig;
import com.smdey.ads.core.LifecycleGuard;
import com.smdey.ads.core.SdkGate;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages Google App Open Ads, preloading cache, and cooldown windows.
 */
public final class AppOpenManager {

    private static final long CACHE_TTL_MS = 4L * 60L * 60L * 1000L; // 4 Hours

    private final SdkGate sdkGate;
    private final AdsConfig config;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean preloadScheduled = new AtomicBoolean(false);
    private final AtomicBoolean isLoading = new AtomicBoolean(false);
    private final AtomicBoolean isShowing = new AtomicBoolean(false);

    private volatile AppOpenAd appOpenAd;
    private volatile long lastShowTimestamp;
    private volatile long lastLoadTimestamp;
    private volatile boolean adsRemoved = false;

    private WeakReference<OpenAdVisibilityControl> visibilityControlRef;

    public AppOpenManager(@NonNull SdkGate sdkGate) {
        this.sdkGate = sdkGate;
        this.config = sdkGate.getConfig();
    }

    public void setAdsRemoved(boolean adsRemoved) {
        this.adsRemoved = adsRemoved;
        if (adsRemoved) {
            appOpenAd = null;
        }
    }

    public boolean isAdsRemoved() {
        return adsRemoved;
    }

    public void setAdVisibilityControl(@Nullable OpenAdVisibilityControl control) {
        visibilityControlRef = control == null ? null : new WeakReference<>(control);
    }

    public boolean isCooldownActive() {
        return System.currentTimeMillis() - lastShowTimestamp < config.getAppOpenCooldownMs();
    }

    public boolean isAdAvailable() {
        return appOpenAd != null
                && System.currentTimeMillis() - lastLoadTimestamp < CACHE_TTL_MS;
    }

    public boolean isLoading() {
        return isLoading.get();
    }

    public void scheduleFirstPreload(@NonNull Activity activity) {
        if (!LifecycleGuard.isActivityValid(activity) || adsRemoved || !config.isAppOpenEnabled()) {
            return;
        }

        if (!ConsentManager.getInstance(activity, config).canRequestAds()) {
            return;
        }

        if (!sdkGate.isReady()) {
            return;
        }

        if (!preloadScheduled.compareAndSet(false, true)) {
            return;
        }

        mainHandler.postDelayed(() -> {
            preloadScheduled.set(false);
            requestPreload(activity.getApplicationContext());
        }, config.getSafeStartupDelayMs());
    }

    public void requestPreload(@NonNull Context context) {
        if (adsRemoved || !config.isAppOpenEnabled() || !sdkGate.isReady()) {
            return;
        }

        if (!ConsentManager.getInstance(context, config).canRequestAds()) {
            return;
        }

        if (isAdAvailable() || isCooldownActive()) {
            return;
        }

        if (!isLoading.compareAndSet(false, true)) {
            return;
        }

        AppOpenAd.load(
                context,
                config.getAppOpenAdUnitId(),
                new AdRequest.Builder().build(),
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        appOpenAd = ad;
                        lastLoadTimestamp = System.currentTimeMillis();
                        isLoading.set(false);
                        Log.i(SdkGate.TAG, "✅ AppOpenManager - Cached new App Open ad.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        isLoading.set(false);
                        Log.w(SdkGate.TAG, "❌ AppOpenManager - Failed to cache App Open ad: " + loadAdError.getMessage());
                    }
                }
        );
    }

    public void showIfAvailable(@NonNull Activity activity) {
        if (!LifecycleGuard.isActivityValid(activity) || adsRemoved || !config.isAppOpenEnabled()) {
            return;
        }

        if (!sdkGate.isReady() || !ConsentManager.getInstance(activity, config).canRequestAds()) {
            return;
        }

        if (isShowing.get() || isCooldownActive()) {
            return;
        }

        AppOpenAd ad = appOpenAd;
        if (ad == null) {
            return;
        }

        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                isShowing.set(true);
                OpenAdVisibilityControl control = visibilityControlRef != null ? visibilityControlRef.get() : null;
                if (control != null) {
                    control.hideBannerAd();
                }
                Log.i(SdkGate.TAG, "✅ AppOpenManager - Showing App Open ad.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                OpenAdVisibilityControl control = visibilityControlRef != null ? visibilityControlRef.get() : null;
                if (control != null) {
                    control.showBannerAd();
                }

                appOpenAd = null;
                isShowing.set(false);
                lastShowTimestamp = System.currentTimeMillis();
                Log.i(SdkGate.TAG, "✅ AppOpenManager - App Open ad dismissed.");

                mainHandler.postDelayed(
                        () -> requestPreload(activity.getApplicationContext()),
                        config.getAppOpenCooldownMs()
                );
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                OpenAdVisibilityControl control = visibilityControlRef != null ? visibilityControlRef.get() : null;
                if (control != null) {
                    control.showBannerAd();
                }

                appOpenAd = null;
                isShowing.set(false);
                Log.w(SdkGate.TAG, "❌ AppOpenManager - Failed to show App Open ad: " + adError.getMessage());
            }
        });

        ad.show(activity);
    }
}
