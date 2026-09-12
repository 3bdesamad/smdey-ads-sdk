package com.smdey.ads.sdk.banner;

import android.app.Activity;
import android.content.Context;
import android.content.MutableContextWrapper;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowMetrics;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.ads.mobile.sdk.banner.AdSize;
import com.google.android.libraries.ads.mobile.sdk.banner.AdView;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.smdey.ads.sdk.AdsConfig;
import com.smdey.ads.sdk.consent.ConsentManager;
import com.smdey.ads.sdk.core.AppExecutors;
import com.smdey.ads.sdk.core.LifecycleGuard;
import com.smdey.ads.sdk.core.SdkGate;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BannerManager {

    /**
     * Host container callback for banner load, failure, and visibility states.
     * <p>
     * Guaranteed to be executed on the Android UI (Main) thread.
     */
    public interface HostCallback {
        @MainThread
        void onBannerLoaded();

        @MainThread
        void onBannerFailed();

        @MainThread
        void onBannerHidden();

        @MainThread
        void onBannerPending();
    }

    private final AdsConfig config;
    private final SdkGate sdkGate;
    private final ConsentManager consentManager;
    private final AppExecutors executors = AppExecutors.getInstance();
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private final AtomicBoolean appOpenLocked = new AtomicBoolean(false);
    private volatile long lastRequestTimestamp;
    private volatile int lastRequestActivityHash = 0;
    private volatile boolean bannerLoaded;
    private MutableContextWrapper contextWrapper;
    private AdView sharedAdView;
    private WeakReference<ViewGroup> currentContainerRef;
    private WeakReference<HostCallback> currentCallbackRef;

    public BannerManager(@NonNull AdsConfig config, @NonNull SdkGate sdkGate, @NonNull ConsentManager consentManager) {
        this.config = config;
        this.sdkGate = sdkGate;
        this.consentManager = consentManager;
    }

    public boolean isAppOpenLocked() {
        return appOpenLocked.get();
    }

    public boolean isBannerLoaded() {
        return sharedAdView != null && bannerLoaded;
    }

    public void onAppOpenShowing() {
        appOpenLocked.set(true);
        Log.i(config.getTag(), "✅ Banner - Hidden while App Open is showing.");
    }

    public void onAppOpenDismissed() {
        appOpenLocked.set(false);
        Log.i(config.getTag(), "⏳ Banner - Restored after App Open.");
    }

    public boolean isCooldownActive(@NonNull Activity activity) {
        if (activity.hashCode() != lastRequestActivityHash) {
            return false;
        }
        return SystemClock.elapsedRealtime() - lastRequestTimestamp < config.getBannerRetryCooldownMs();
    }

    public int getBannerHeightPx(@NonNull Activity activity) {
        try {
            return getBannerAdSize(activity).getHeightInPixels(activity);
        } catch (Exception e) {
            Log.e(config.getTag(), "❌ Banner - Failed to resolve banner height.", e);
            return 0;
        }
    }

    public void attachOrLoad(@NonNull Activity activity,
                             @NonNull ViewGroup container,
                             @NonNull HostCallback callback) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            callback.onBannerHidden();
            return;
        }

        updateContext(activity);
        currentContainerRef = new WeakReference<>(container);
        currentCallbackRef = new WeakReference<>(callback);

        if (config.isAdsRemoved()) {
            callback.onBannerHidden();
            return;
        }

        if (!config.isBannerEnabled() || config.getBannerId() == null || config.getBannerId().isEmpty()) {
            callback.onBannerHidden();
            return;
        }

        if (isBannerLoaded()) {
            attachToContainer(container);
            callback.onBannerLoaded();
            Log.d(config.getTag(), "✅ Banner - Attached to " + activity.getClass().getSimpleName() + ".");
            return;
        }

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
                attachOrLoad(activity, container, callback);
            });
            return;
        }

        if (!sdkGate.isReady()) {
            callback.onBannerPending();
            sdkGate.ensureInitialized(activity, () -> attachOrLoad(activity, container, callback));
            return;
        }

        if (isCooldownActive(activity)) {
            callback.onBannerHidden();
            return;
        }

        if (!loading.compareAndSet(false, true)) {
            if (sharedAdView != null) {
                attachToContainer(container);
            }
            return;
        }

        lastRequestActivityHash = activity.hashCode();
        lastRequestTimestamp = SystemClock.elapsedRealtime();
        createAdViewIfNeeded(activity);
        attachToContainer(container);

        Log.i(config.getTag(), "⏳ Banner - Loading on " + activity.getClass().getSimpleName() + "...");
        BannerAdRequest request = new BannerAdRequest.Builder(
                config.getBannerId(),
                getBannerAdSize(activity)
        ).build();

        sharedAdView.loadAd(request, new AdLoadCallback<BannerAd>() {
            @Override
            public void onAdLoaded(@NonNull BannerAd bannerAd) {
                bannerAd.setAdEventCallback(new BannerAdEventCallback() {
                    @Override
                    public void onAdImpression() {
                        Log.d(config.getTag(), "✅ Banner - Impression recorded on " + activity.getClass().getSimpleName() + ".");
                    }
                });

                executors.mainThread().execute(() -> {
                    bannerLoaded = true;
                    loading.set(false);
                    attachToCurrentContainer();
                    HostCallback host = getCurrentCallback();
                    if (host != null) {
                        host.onBannerLoaded();
                    }
                    Log.i(config.getTag(), "✅ Banner - Loaded successfully on " + activity.getClass().getSimpleName() + " (Next-Gen).");
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                executors.mainThread().execute(() -> {
                    bannerLoaded = false;
                    loading.set(false);
                    HostCallback host = getCurrentCallback();
                    if (host != null) {
                        host.onBannerFailed();
                    }
                    Log.w(config.getTag(), "❌ Banner - Failed to load: " + loadAdError.getMessage());
                });
            }
        });
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

        if (sharedAdView != null && sharedAdView.getParent() == container) {
            container.removeView(sharedAdView);
        }
    }

    @NonNull
    @SuppressWarnings("deprecation")
    private AdSize getBannerAdSize(@NonNull Activity activity) {
        Context context = activity.getApplicationContext() != null ? activity.getApplicationContext() : activity;

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

            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth);
        } catch (Exception e) {
            Log.e(config.getTag(), "❌ Banner - Failed to resolve adaptive size.", e);
            return AdSize.BANNER;
        }
    }

    private void createAdViewIfNeeded(@NonNull Activity activity) {
        if (sharedAdView != null) {
            updateContext(activity);
            return;
        }

        contextWrapper = new MutableContextWrapper(activity);
        sharedAdView = new AdView(contextWrapper);
        sharedAdView.setBackgroundColor(Color.TRANSPARENT);
    }

    private void updateContext(@NonNull Activity activity) {
        if (contextWrapper != null && contextWrapper.getBaseContext() != activity) {
            contextWrapper.setBaseContext(activity);
        }
    }

    public void onActivityDestroyed(@NonNull Activity activity) {
        if (contextWrapper != null && contextWrapper.getBaseContext() == activity) {
            contextWrapper.setBaseContext(activity.getApplicationContext());
        }
        ViewGroup container = currentContainerRef != null ? currentContainerRef.get() : null;
        if (container != null && container.getContext() == activity) {
            detachHost(container);
        }
    }

    private void attachToCurrentContainer() {
        ViewGroup container = currentContainerRef != null ? currentContainerRef.get() : null;
        if (container != null) {
            attachToContainer(container);
        }
    }

    private void attachToContainer(@NonNull ViewGroup container) {
        if (sharedAdView == null) {
            return;
        }

        ViewGroup parent = sharedAdView.getParent() instanceof ViewGroup
                ? (ViewGroup) sharedAdView.getParent()
                : null;
        if (parent == container) {
            return;
        }

        if (parent != null) {
            parent.removeView(sharedAdView);
        }

        container.removeAllViews();
        container.addView(sharedAdView);
    }

    @Nullable
    private HostCallback getCurrentCallback() {
        return currentCallbackRef != null ? currentCallbackRef.get() : null;
    }
}
