package com.smdey.ads.managers;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd;
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.smdey.ads.callbacks.OpenAdVisibilityControl;
import com.smdey.ads.core.AdsConfig;
import com.smdey.ads.core.AppExecutors;
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
        if (!LifecycleGuard.isActivityValid(activity) || adsRemoved || !config.isAppOpenEnabled() || config.getAppOpenAdUnitId() == null) {
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

        AppExecutors.getInstance().mainThread().postDelayed(() -> {
            preloadScheduled.set(false);
            requestPreload(activity.getApplicationContext());
        }, config.getSafeStartupDelayMs());
    }

    public void requestPreload(@NonNull Context context) {
        if (adsRemoved || !config.isAppOpenEnabled() || config.getAppOpenAdUnitId() == null || !sdkGate.isReady()) {
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

        AdRequest adRequest = new AdRequest.Builder(config.getAppOpenAdUnitId()).build();
        AppOpenAd.load(
                adRequest,
                new AdLoadCallback<AppOpenAd>() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            appOpenAd = ad;
                            lastLoadTimestamp = System.currentTimeMillis();
                            isLoading.set(false);
                            Log.i(SdkGate.TAG, "✅ AppOpenManager - Cached new App Open ad.");
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            isLoading.set(false);
                            Log.w(SdkGate.TAG, "❌ AppOpenManager - Failed to cache App Open ad: " + loadAdError.getMessage());
                        });
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

        ad.setAdEventCallback(new AppOpenAdEventCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    isShowing.set(true);
                    OpenAdVisibilityControl control = visibilityControlRef != null ? visibilityControlRef.get() : null;
                    if (control != null) {
                        control.hideBannerAd();
                    }
                    Log.i(SdkGate.TAG, "✅ AppOpenManager - Showing App Open ad.");
                });
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    OpenAdVisibilityControl control = visibilityControlRef != null ? visibilityControlRef.get() : null;
                    if (control != null) {
                        control.showBannerAd();
                    }

                    appOpenAd = null;
                    isShowing.set(false);
                    lastShowTimestamp = System.currentTimeMillis();
                    Log.i(SdkGate.TAG, "✅ AppOpenManager - App Open ad dismissed.");

                    AppExecutors.getInstance().mainThread().postDelayed(
                            () -> requestPreload(activity.getApplicationContext()),
                            config.getAppOpenCooldownMs()
                    );
                });
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    OpenAdVisibilityControl control = visibilityControlRef != null ? visibilityControlRef.get() : null;
                    if (control != null) {
                        control.showBannerAd();
                    }

                    appOpenAd = null;
                    isShowing.set(false);
                    Log.w(SdkGate.TAG, "❌ AppOpenManager - Failed to show App Open ad: " + fullScreenContentError.getMessage());
                });
            }
        });

        ad.show(activity);
    }
}
