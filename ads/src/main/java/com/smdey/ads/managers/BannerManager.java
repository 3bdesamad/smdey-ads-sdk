package com.smdey.ads.managers;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.smdey.ads.core.AdsConfig;
import com.smdey.ads.core.LifecycleGuard;
import com.smdey.ads.core.SdkGate;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages anchored adaptive banner ads and collapsible banner ads with lifecycle safety.
 */
public final class BannerManager {

    public interface HostCallback {
        void onBannerLoaded();
        void onBannerFailed();
        void onBannerHidden();
        void onBannerPending();
    }

    private final SdkGate sdkGate;
    private final AdsConfig config;
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private final AtomicBoolean appOpenLocked = new AtomicBoolean(false);

    private volatile boolean adsRemoved = false;
    private volatile boolean bannerLoaded = false;

    private WeakReference<ViewGroup> currentContainerRef;
    private WeakReference<HostCallback> currentCallbackRef;
    private WeakReference<Activity> currentActivityRef;

    @Nullable
    private AdView currentAdView;

    public BannerManager(@NonNull SdkGate sdkGate) {
        this.sdkGate = sdkGate;
        this.config = sdkGate.getConfig();
    }

    public boolean isAppOpenLocked() {
        return appOpenLocked.get();
    }

    public void onAppOpenShowing() {
        appOpenLocked.set(true);
    }

    public void onAppOpenDismissed() {
        appOpenLocked.set(false);
    }

    public void setAdsRemoved(boolean adsRemoved) {
        this.adsRemoved = adsRemoved;
        if (adsRemoved) {
            destroyBanner();
            HostCallback callback = getCurrentCallback();
            if (callback != null) {
                callback.onBannerHidden();
            }
        }
    }

    public boolean isAdsRemoved() {
        return adsRemoved;
    }

    public boolean isBannerLoaded() {
        return bannerLoaded && currentAdView != null;
    }

    public void attachOrLoad(@NonNull Activity activity,
                             @NonNull ViewGroup container,
                             @NonNull HostCallback callback) {
        attachOrLoad(activity, container, config.isCollapsibleBannerEnabled(), callback);
    }

    public void attachOrLoad(@NonNull Activity activity,
                             @NonNull ViewGroup container,
                             boolean isCollapsible,
                             @NonNull HostCallback callback) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            callback.onBannerHidden();
            return;
        }

        currentContainerRef = new WeakReference<>(container);
        currentCallbackRef = new WeakReference<>(callback);

        Activity lastActivity = currentActivityRef != null ? currentActivityRef.get() : null;
        currentActivityRef = new WeakReference<>(activity);

        if (adsRemoved) {
            Log.d(SdkGate.TAG, "⚠️ BannerManager - Ads removed. Hiding banner.");
            callback.onBannerHidden();
            return;
        }

        if (!config.isBannerEnabled()) {
            Log.d(SdkGate.TAG, "⚠️ BannerManager - Feature disabled. Hiding banner.");
            callback.onBannerHidden();
            return;
        }

        // If it's a collapsible banner, or if the Activity changed, we must do a fresh load with current Activity context
        boolean shouldReload = isCollapsible || lastActivity != activity || currentAdView == null || !bannerLoaded;

        if (!shouldReload && isBannerLoaded()) {
            attachToContainer(container);
            callback.onBannerLoaded();
            return;
        }

        ConsentManager consentManager = ConsentManager.getInstance(activity, config);
        if (!consentManager.canRequestAds()) {
            callback.onBannerPending();
            consentManager.gatherConsent(activity, () -> {
                if (!LifecycleGuard.isActivityValid(activity)) {
                    return;
                }

                if (!consentManager.canRequestAds()) {
                    callback.onBannerHidden();
                    return;
                }

                attachOrLoad(activity, container, isCollapsible, callback);
            });
            return;
        }

        if (!sdkGate.isReady()) {
            callback.onBannerPending();
            sdkGate.ensureInitialized(activity, () -> attachOrLoad(activity, container, isCollapsible, callback));
            return;
        }

        if (!loading.compareAndSet(false, true)) {
            return;
        }

        // Recreate AdView with Activity context (required for Collapsible Banner window anchoring)
        recreateAdView(activity, isCollapsible);
        attachToContainer(container);

        AdRequest.Builder requestBuilder = new AdRequest.Builder();
        if (isCollapsible || config.isCollapsibleBannerEnabled()) {
            Bundle extras = new Bundle();
            extras.putString("collapsible", config.getCollapsibleGravity());
            extras.putString("collapsible_request_id", UUID.randomUUID().toString());
            requestBuilder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
            Log.i(SdkGate.TAG, "⚡ BannerManager - Requesting collapsible banner (" + config.getCollapsibleGravity() + ").");
        }

        if (currentAdView != null) {
            currentAdView.loadAd(requestBuilder.build());
            Log.i(SdkGate.TAG, "⏳ BannerManager - Loading banner ad...");
        } else {
            loading.set(false);
        }
    }

    public void detachHost(@Nullable ViewGroup container) {
        if (container == null) {
            return;
        }

        ViewGroup currentContainer = currentContainerRef != null ? currentContainerRef.get() : null;
        if (currentContainer == container) {
            currentContainerRef.clear();
            currentContainerRef = null;
            currentCallbackRef = null;
        }

        if (currentAdView != null && currentAdView.getParent() == container) {
            container.removeView(currentAdView);
        }
    }

    @NonNull
    private AdSize getBannerAdSize(@NonNull Activity activity) {
        try {
            DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
            int widthPx = displayMetrics.widthPixels;

            if (widthPx <= 0) {
                widthPx = Resources.getSystem().getDisplayMetrics().widthPixels;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    WindowMetrics metrics = activity.getWindowManager().getCurrentWindowMetrics();
                    int windowWidth = metrics.getBounds().width();
                    if (windowWidth > 0) {
                        widthPx = windowWidth;
                    }
                } catch (Exception ignored) {
                }
            }

            int adWidth = (int) (widthPx / displayMetrics.density);
            if (adWidth < 320) {
                adWidth = 320;
            }

            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
        } catch (Exception e) {
            Log.e(SdkGate.TAG, "❌ BannerManager - Error determining adaptive size.", e);
            return AdSize.BANNER;
        }
    }

    public int getBannerHeightPx(@NonNull Activity activity) {
        try {
            AdSize size = getBannerAdSize(activity);
            return size.getHeightInPixels(activity);
        } catch (Exception ignored) {
            return (int) (50 * activity.getResources().getDisplayMetrics().density);
        }
    }

    private void recreateAdView(@NonNull Activity activity, boolean isCollapsible) {
        destroyBanner();

        currentAdView = new AdView(activity);
        currentAdView.setAdUnitId(config.getBannerAdUnitId(isCollapsible));
        currentAdView.setAdSize(getBannerAdSize(activity));
        currentAdView.setBackgroundColor(Color.TRANSPARENT);
        currentAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                bannerLoaded = true;
                loading.set(false);
                attachToCurrentContainer();
                HostCallback callback = getCurrentCallback();
                if (callback != null) {
                    callback.onBannerLoaded();
                }
                Log.i(SdkGate.TAG, "✅ BannerManager - " + (isCollapsible ? "Collapsible banner" : "Banner") + " loaded successfully.");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                bannerLoaded = false;
                loading.set(false);
                HostCallback callback = getCurrentCallback();
                if (callback != null) {
                    callback.onBannerFailed();
                }
                Log.w(SdkGate.TAG, "❌ BannerManager - Failed to load banner: " + loadAdError.getMessage());
            }
        });
    }

    private void attachToCurrentContainer() {
        ViewGroup container = currentContainerRef != null ? currentContainerRef.get() : null;
        if (container != null) {
            attachToContainer(container);
        }
    }

    private void attachToContainer(@NonNull ViewGroup container) {
        if (currentAdView == null) {
            return;
        }

        ViewGroup parent = currentAdView.getParent() instanceof ViewGroup
                ? (ViewGroup) currentAdView.getParent()
                : null;
        if (parent == container) {
            return;
        }

        if (parent != null) {
            parent.removeView(currentAdView);
        }

        container.removeAllViews();
        container.addView(currentAdView);
    }

    public void destroyBanner() {
        if (currentAdView != null) {
            try {
                if (currentAdView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) currentAdView.getParent()).removeView(currentAdView);
                }
                currentAdView.destroy();
                Log.d(SdkGate.TAG, "♻️ BannerManager - Banner AdView destroyed.");
            } catch (Exception ignored) {
            }
            currentAdView = null;
        }
        bannerLoaded = false;
    }

    @Nullable
    private HostCallback getCurrentCallback() {
        return currentCallbackRef != null ? currentCallbackRef.get() : null;
    }
}
