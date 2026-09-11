package com.smdey.ads.sdk.appopen;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd;
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.smdey.ads.sdk.AdsConfig;
import com.smdey.ads.sdk.callbacks.OpenAdVisibilityControl;
import com.smdey.ads.sdk.consent.ConsentManager;
import com.smdey.ads.sdk.core.AppExecutors;
import com.smdey.ads.sdk.core.FullScreenAdCoordinator;
import com.smdey.ads.sdk.core.LifecycleGuard;
import com.smdey.ads.sdk.core.SdkGate;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AppOpenManager {

    private static final long CACHE_TTL_MS = 4L * 60L * 60L * 1000L;

    private final AdsConfig config;
    private final SdkGate sdkGate;
    private final ConsentManager consentManager;
    private final FullScreenAdCoordinator fullScreenCoordinator;
    private final AppExecutors executors = AppExecutors.getInstance();
    private final AtomicBoolean preloadScheduled = new AtomicBoolean(false);
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private final AtomicBoolean showing = new AtomicBoolean(false);
    private final AtomicBoolean waitingForFocus = new AtomicBoolean(false);

    private volatile AppOpenAd appOpenAd;
    private volatile long lastLoadTimestamp = 0;
    private volatile long lastShowTimestamp = 0;
    private volatile boolean hasEnteredBackground = false;
    private WeakReference<OpenAdVisibilityControl> visibilityControlRef;

    public AppOpenManager(@NonNull AdsConfig config,
                          @NonNull SdkGate sdkGate,
                          @NonNull ConsentManager consentManager,
                          @NonNull FullScreenAdCoordinator fullScreenCoordinator) {
        this.config = config;
        this.sdkGate = sdkGate;
        this.consentManager = consentManager;
        this.fullScreenCoordinator = fullScreenCoordinator;
    }

    public boolean isAdAvailable() {
        return appOpenAd != null && wasLoadTimeLessThanTTL();
    }

    public boolean isShowing() {
        return showing.get();
    }

    public boolean isLoading() {
        return loading.get();
    }

    public boolean isCooldownActive() {
        return SystemClock.elapsedRealtime() - lastShowTimestamp < config.getAppOpenCooldownMs();
    }

    public long getRemainingCooldownMs() {
        long elapsed = SystemClock.elapsedRealtime() - lastShowTimestamp;
        return Math.max(0L, config.getAppOpenCooldownMs() - elapsed);
    }

    public void setAdVisibilityControl(@Nullable OpenAdVisibilityControl control) {
        this.visibilityControlRef = control != null ? new WeakReference<>(control) : null;
    }

    public void scheduleFirstPreloadIfAllowed(@NonNull Activity activity) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            Log.d(config.getTag(), "⚠️ App Open - Invalid activity. Skipping preload.");
            return;
        }

        if (config.isAdsRemoved()) {
            Log.d(config.getTag(), "⚠️ App Open - Ads removed. Skipping preload.");
            return;
        }

        if (!config.isAppOpenEnabled() || config.getAppOpenId() == null || config.getAppOpenId().isEmpty()) {
            Log.d(config.getTag(), "⚠️ App Open - Feature disabled or ad unit missing. Skipping preload.");
            return;
        }

        if (!consentManager.canRequestAds()) {
            Log.d(config.getTag(), "⏳ App Open - Consent not ready. Waiting before preload.");
            consentManager.gatherConsent(activity, () -> {
                if (consentManager.canRequestAds()) {
                    scheduleFirstPreloadIfAllowed(activity);
                }
            });
            return;
        }

        if (!sdkGate.isReady()) {
            Log.d(config.getTag(), "⏳ App Open - SDK not ready. Waiting before preload.");
            sdkGate.ensureInitialized(activity, () -> scheduleFirstPreloadIfAllowed(activity));
            return;
        }

        if (!preloadScheduled.compareAndSet(false, true)) {
            Log.d(config.getTag(), "⏳ App Open - Preload is already scheduled.");
            return;
        }

        long delay = config.getAppOpenPreloadDelayMs();
        Log.i(config.getTag(), "⏳ App Open - Preload scheduled in " + delay + " ms.");
        executors.mainThread().postDelayed(() -> {
            preloadScheduled.set(false);
            requestPreload(activity.getApplicationContext());
        }, delay);
    }

    public void requestPreload(@NonNull Context context) {
        preloadAd(context);
    }

    public void preloadAd(@NonNull Context context) {
        if (config.isAdsRemoved() || !config.isAppOpenEnabled() || config.getAppOpenId() == null || config.getAppOpenId().isEmpty()) {
            return;
        }

        if (!consentManager.canRequestAds()) {
            Log.d(config.getTag(), "⏳ App Open - Consent not ready. Skipping preload.");
            return;
        }

        if (!sdkGate.isReady()) {
            Log.d(config.getTag(), "⏳ App Open - SDK not ready. Skipping preload.");
            return;
        }

        if (isAdAvailable()) {
            Log.d(config.getTag(), "✅ App Open - Cached ad already available.");
            return;
        }

        if (!loading.compareAndSet(false, true)) {
            Log.d(config.getTag(), "⏳ App Open - Load already in progress.");
            return;
        }

        Log.i(config.getTag(), "⏳ App Open - Loading ad...");
        AdRequest request = new AdRequest.Builder(config.getAppOpenId()).build();
        AppOpenAd.load(request, new AdLoadCallback<AppOpenAd>() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                executors.mainThread().execute(() -> {
                    appOpenAd = ad;
                    lastLoadTimestamp = SystemClock.elapsedRealtime();
                    loading.set(false);
                    Log.i(config.getTag(), "✅ App Open - Ad loaded successfully (Next-Gen).");
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                executors.mainThread().execute(() -> {
                    loading.set(false);
                    Log.w(config.getTag(), "❌ App Open - Failed to load ad: " + loadAdError.getMessage());
                });
            }
        });
    }

    public void showAdIfAvailable(@NonNull Activity activity) {
        showIfAvailable(activity);
    }

    public void showIfAvailable(@NonNull Activity activity) {
        if (!LifecycleGuard.isActivityValid(activity) || config.isAdsRemoved() || !config.isAppOpenEnabled()) {
            return;
        }

        if (!sdkGate.isReady()) {
            Log.d(config.getTag(), "⏳ App Open - SDK not ready. Skipping show.");
            return;
        }

        if (!consentManager.canRequestAds()) {
            Log.d(config.getTag(), "⚠️ App Open - Consent not ready. Skipping show.");
            return;
        }

        if (showing.get() || fullScreenCoordinator.isFullScreenShowing()) {
            Log.d(config.getTag(), "⏳ App Open - Another full-screen ad is already showing. Suppressing.");
            return;
        }

        if (isCooldownActive()) {
            long remaining = getRemainingCooldownMs();
            long seconds = (remaining + 999L) / 1000L;
            Log.d(config.getTag(), "⏳ App Open - Cooldown active: " + remaining + " ms (~" + seconds + "s left). Skipping show.");
            return;
        }

        if (config.isActivityExcludedFromAppOpen(activity.getClass())) {
            Log.d(config.getTag(), "⏳ App Open - Suppressed: " + activity.getClass().getSimpleName() + " is excluded in AdsConfig.");
            return;
        }

        OpenAdVisibilityControl control = visibilityControlRef != null ? visibilityControlRef.get() : null;
        if (control != null && !control.canShowOpenAd()) {
            Log.d(config.getTag(), "⏳ App Open - Suppressed by VisibilityControl.");
            return;
        }

        AppOpenAd adToShow = appOpenAd;
        if (adToShow == null || !isAdAvailable()) {
            Log.d(config.getTag(), "⚠️ App Open - No cached ad available. Preloading...");
            requestPreload(activity.getApplicationContext());
            return;
        }

        if (!activity.hasWindowFocus()) {
            if (!waitingForFocus.compareAndSet(false, true)) {
                Log.d(config.getTag(), "⏳ App Open - Already waiting for window focus.");
                return;
            }
            Log.d(config.getTag(), "⏳ App Open - Waiting for window focus before showing...");
            View decorView = activity.getWindow() != null ? activity.getWindow().peekDecorView() : null;
            if (decorView != null) {
                ViewTreeObserver vto = decorView.getViewTreeObserver();
                if (vto.isAlive()) {
                    final ViewTreeObserver.OnWindowFocusChangeListener[] listenerRef = new ViewTreeObserver.OnWindowFocusChangeListener[1];
                    final Runnable[] timeoutRef = new Runnable[1];

                    listenerRef[0] = new ViewTreeObserver.OnWindowFocusChangeListener() {
                        @Override
                        public void onWindowFocusChanged(boolean hasFocus) {
                            if (hasFocus) {
                                waitingForFocus.set(false);
                                if (decorView.getViewTreeObserver().isAlive()) {
                                    decorView.getViewTreeObserver().removeOnWindowFocusChangeListener(listenerRef[0]);
                                }
                                executors.mainThread().removeCallbacks(timeoutRef[0]);
                                if (LifecycleGuard.isActivityValid(activity)) {
                                    showIfAvailable(activity);
                                }
                            }
                        }
                    };

                    timeoutRef[0] = () -> {
                        waitingForFocus.set(false);
                        if (decorView.getViewTreeObserver().isAlive()) {
                            decorView.getViewTreeObserver().removeOnWindowFocusChangeListener(listenerRef[0]);
                        }
                        if (LifecycleGuard.isActivityValid(activity)) {
                            showIfAvailable(activity);
                        }
                    };

                    vto.addOnWindowFocusChangeListener(listenerRef[0]);
                    executors.mainThread().postDelayed(timeoutRef[0], 1000L);
                    return;
                }
            }
            waitingForFocus.set(false);
        }

        if (!fullScreenCoordinator.tryAcquireShowLock()) {
            Log.d(config.getTag(), "⏳ App Open - Could not acquire full-screen lock. Suppressing.");
            return;
        }

        appOpenAd = null;

        adToShow.setAdEventCallback(new AppOpenAdEventCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                executors.mainThread().execute(() -> {
                    showing.set(true);
                    lastShowTimestamp = SystemClock.elapsedRealtime();
                    OpenAdVisibilityControl currentControl = visibilityControlRef != null ? visibilityControlRef.get() : null;
                    if (currentControl != null) {
                        currentControl.hideBannerAd();
                        Log.i(config.getTag(), "ℹ️ Banner - Temporarily hidden for App Open.");
                    }
                    Log.i(config.getTag(), "✅ App Open - Showing ad.");
                });
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                executors.mainThread().execute(() -> {
                    OpenAdVisibilityControl currentControl = visibilityControlRef != null ? visibilityControlRef.get() : null;
                    if (currentControl != null) {
                        currentControl.showBannerAd();
                        Log.i(config.getTag(), "ℹ️ Banner - Restored after App Open.");
                    }

                    showing.set(false);
                    fullScreenCoordinator.releaseShowLock();
                    lastShowTimestamp = SystemClock.elapsedRealtime();
                    Log.i(config.getTag(), "✅ App Open - Ad dismissed. Cooldown started: " + config.getAppOpenCooldownMs() + " ms (" + (config.getAppOpenCooldownMs() / 1000L) + "s).");

                    requestPreload(activity.getApplicationContext());
                });
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError adError) {
                executors.mainThread().execute(() -> {
                    OpenAdVisibilityControl currentControl = visibilityControlRef != null ? visibilityControlRef.get() : null;
                    if (currentControl != null) {
                        currentControl.showBannerAd();
                    }

                    showing.set(false);
                    fullScreenCoordinator.releaseShowLock();
                    Log.w(config.getTag(), "❌ App Open - Failed to show ad: " + adError.getMessage());
                    requestPreload(activity.getApplicationContext());
                });
            }
        });

        try {
            adToShow.show(activity);
        } catch (Exception e) {
            Log.e(config.getTag(), "❌ App Open - Exception showing ad: " + e.getMessage(), e);
            showing.set(false);
            fullScreenCoordinator.releaseShowLock();
            requestPreload(activity.getApplicationContext());
        }
    }

    public void onAppForegrounded(@NonNull Activity activity) {
        if (!hasEnteredBackground) {
            Log.d(config.getTag(), "⚠️ App Open - Cold launch. Skipping first show.");
            return;
        }

        if (config.isActivityExcludedFromAppOpen(activity.getClass())) {
            Log.d(config.getTag(), "⏳ App Open - Suppressed: " + activity.getClass().getSimpleName() + " is excluded in AdsConfig on foreground.");
            return;
        }

        OpenAdVisibilityControl control = visibilityControlRef != null ? visibilityControlRef.get() : null;
        if (control != null && !control.canShowOpenAd()) {
            Log.d(config.getTag(), "⏳ App Open - Suppressed by VisibilityControl on foreground.");
            return;
        }

        showIfAvailable(activity);
    }

    public void onAppBackgrounded(@NonNull Context context) {
        hasEnteredBackground = true;
        long remaining = getRemainingCooldownMs();
        long seconds = (remaining + 999L) / 1000L;
        String cooldownInfo = isCooldownActive()
                ? "Cooldown active: " + remaining + " ms (~" + seconds + "s left)"
                : "Cooldown: expired / ready to show";

        if (isAdAvailable()) {
            Log.d(config.getTag(), "ℹ️ App Open - Entered background (cached ad ready in RAM | " + cooldownInfo + ").");
            return;
        }

        Log.d(config.getTag(), "⏳ App Open - Entered background (cache empty | " + cooldownInfo + "). Preloading for return...");
        requestPreload(context);
    }

    private boolean wasLoadTimeLessThanTTL() {
        return SystemClock.elapsedRealtime() - lastLoadTimestamp < CACHE_TTL_MS;
    }
}
