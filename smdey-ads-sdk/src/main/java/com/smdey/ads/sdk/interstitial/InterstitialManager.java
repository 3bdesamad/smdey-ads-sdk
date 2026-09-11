package com.smdey.ads.sdk.interstitial;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback;
import com.smdey.ads.sdk.AdsConfig;
import com.smdey.ads.sdk.callbacks.AdLoadingOverlayProvider;
import com.smdey.ads.sdk.callbacks.AdsCallback;
import com.smdey.ads.sdk.callbacks.NavigationCallback;
import com.smdey.ads.sdk.consent.ConsentManager;
import com.smdey.ads.sdk.core.AppExecutors;
import com.smdey.ads.sdk.core.FullScreenAdCoordinator;
import com.smdey.ads.sdk.core.LifecycleGuard;
import com.smdey.ads.sdk.core.SdkGate;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

public final class InterstitialManager {

    private final AdsConfig config;
    private final SdkGate sdkGate;
    private final ConsentManager consentManager;
    private final FullScreenAdCoordinator fullScreenCoordinator;
    private final AppExecutors executors = AppExecutors.getInstance();
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private final AtomicBoolean showing = new AtomicBoolean(false);
    private int clickCount = 0;
    private InterstitialAd interstitialAd;
    private Dialog loadingDialog;
    private Runnable loadingTimeoutRunnable;
    private PendingShowRequest pendingShowRequest;

    private static final class PendingShowRequest {
        final WeakReference<Activity> activityRef;
        final AdsCallback continuationCallback;
        final NavigationCallback navigationCallback;

        PendingShowRequest(Activity activity, AdsCallback continuationCallback, NavigationCallback navigationCallback) {
            this.activityRef = new WeakReference<>(activity);
            this.continuationCallback = continuationCallback;
            this.navigationCallback = navigationCallback;
        }

        Activity getActivity() {
            return activityRef.get();
        }
    }

    public InterstitialManager(@NonNull AdsConfig config,
                               @NonNull SdkGate sdkGate,
                               @NonNull ConsentManager consentManager,
                               @NonNull FullScreenAdCoordinator fullScreenCoordinator) {
        this.config = config;
        this.sdkGate = sdkGate;
        this.consentManager = consentManager;
        this.fullScreenCoordinator = fullScreenCoordinator;
    }

    public InterstitialManager(@NonNull AdsConfig config, @NonNull SdkGate sdkGate, @NonNull ConsentManager consentManager) {
        this(config, sdkGate, consentManager, new FullScreenAdCoordinator());
    }

    public boolean isAdReady() {
        return interstitialAd != null;
    }

    public void preloadAd(@NonNull Context context) {
        if (config.isAdsRemoved() || !config.isInterstitialEnabled() || isAdReady() || loading.get()) {
            return;
        }
        if (!consentManager.canRequestAds() || !sdkGate.isReady()) {
            return;
        }
        requestLoad(context, true);
    }

    public void showAdWithLoadingOverlay(@NonNull Activity activity, @Nullable AdsCallback callback) {
        if (config.isAdsRemoved()) {
            complete(callback);
            return;
        }

        if (!consentManager.canRequestAds() || !sdkGate.isReady()) {
            complete(callback);
            return;
        }

        showWithLoadingOverlay(activity, callback, null);
    }

    public void showAdWithLoadingOverlayByClick(@NonNull Activity activity, @Nullable AdsCallback callback) {
        showAdWithLoadingOverlayByClick(activity, config.getInterstitialFrequency(), callback);
    }

    public void showAdWithLoadingOverlayByClick(@NonNull Activity activity, int clickInterval, @Nullable AdsCallback callback) {
        if (config.isAdsRemoved() || !config.isInterstitialEnabled()) {
            complete(callback);
            return;
        }

        clickCount++;
        int targetInterval = Math.max(1, clickInterval);
        int preloadThreshold = Math.max(1, targetInterval <= 3 ? targetInterval - 1 : targetInterval - 2);

        if (clickCount >= targetInterval) {
            Log.i(config.getTag(), "🎯 Interstitial - Target reached (" + clickCount + "/" + targetInterval + " clicks). Showing ad with loading overlay...");
            clickCount = 0;
            showWithLoadingOverlay(activity, callback, null);
        } else {
            if (clickCount >= preloadThreshold) {
                if (isAdReady()) {
                    Log.d(config.getTag(), "ℹ️ Interstitial - Click " + clickCount + "/" + targetInterval + ". Preloaded ad already ready in RAM.");
                } else {
                    Log.i(config.getTag(), "⏳ Interstitial - Click " + clickCount + "/" + targetInterval + " reached threshold (" + preloadThreshold + "). Starting smart preload...");
                    preloadAd(activity);
                }
            } else {
                Log.d(config.getTag(), "ℹ️ Interstitial - Click " + clickCount + "/" + targetInterval + " (below threshold " + preloadThreshold + "). Preload skipped.");
            }
            complete(callback);
        }
    }

    public int getClickCount() {
        return clickCount;
    }

    public void resetClickCount() {
        clickCount = 0;
    }

    public void navigationClickAd(@NonNull Activity activity, @NonNull NavigationCallback navigationCallback) {
        if (config.isAdsRemoved() || !config.isInterstitialEnabled()) {
            navigate(navigationCallback);
            return;
        }

        clickCount++;
        int targetInterval = config.getInterstitialFrequency();
        int preloadThreshold = Math.max(1, targetInterval <= 3 ? targetInterval - 1 : targetInterval - 2);

        if (clickCount >= targetInterval) {
            Log.i(config.getTag(), "🎯 Interstitial - Navigation target reached (" + clickCount + "/" + targetInterval + " clicks). Showing ad...");
            clickCount = 0;
            showWithLoadingOverlay(activity, null, navigationCallback);
        } else {
            if (clickCount >= preloadThreshold) {
                if (isAdReady()) {
                    Log.d(config.getTag(), "ℹ️ Interstitial - Click " + clickCount + "/" + targetInterval + ". Preloaded ad already ready in RAM.");
                } else {
                    Log.i(config.getTag(), "⏳ Interstitial - Click " + clickCount + "/" + targetInterval + " reached threshold (" + preloadThreshold + "). Starting smart preload...");
                    preloadAd(activity);
                }
            } else {
                Log.d(config.getTag(), "ℹ️ Interstitial - Click " + clickCount + "/" + targetInterval + " (below threshold " + preloadThreshold + "). Preload skipped.");
            }
            navigate(navigationCallback);
        }
    }

    private void showWithLoadingOverlay(@NonNull Activity activity,
                                        @Nullable AdsCallback continuationCallback,
                                        @Nullable NavigationCallback navigationCallback) {
        if (!LifecycleGuard.isActivityValid(activity) || !config.isInterstitialEnabled() || config.isAdsRemoved()) {
            complete(continuationCallback);
            navigate(navigationCallback);
            return;
        }

        if (!consentManager.canRequestAds() || !sdkGate.isReady()) {
            complete(continuationCallback);
            navigate(navigationCallback);
            return;
        }

        if (showing.get() || fullScreenCoordinator.isFullScreenShowing() || pendingShowRequest != null) {
            complete(continuationCallback);
            navigate(navigationCallback);
            return;
        }

        if (!fullScreenCoordinator.tryAcquireShowLock()) {
            complete(continuationCallback);
            navigate(navigationCallback);
            return;
        }

        pendingShowRequest = new PendingShowRequest(activity, continuationCallback, navigationCallback);
        showLoadingDialog(activity);
        scheduleOverlayTimeout();

        if (interstitialAd != null) {
            executors.mainThread().execute(this::maybeShowPendingAd);
            return;
        }

        requestLoad(activity, false);
    }

    private void requestLoad(@NonNull Context context, boolean allowWithoutPendingRequest) {
        if (config.isAdsRemoved() || config.getInterstitialId() == null) {
            failPendingRequest();
            return;
        }

        if (!consentManager.canRequestAds() || !sdkGate.isReady()) {
            failPendingRequest();
            return;
        }

        if (!allowWithoutPendingRequest && pendingShowRequest == null) {
            return;
        }

        if (!loading.compareAndSet(false, true)) {
            return;
        }

        Log.i(config.getTag(), "⏳ Interstitial - Loading ad request (Next-Gen)...");
        AdRequest request = new AdRequest.Builder(config.getInterstitialId()).build();
        InterstitialAd.load(request, new AdLoadCallback<InterstitialAd>() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                executors.mainThread().execute(() -> {
                    interstitialAd = ad;
                    loading.set(false);
                    Log.i(config.getTag(), "✅ Interstitial - Loaded and cached in RAM (Next-Gen).");
                    maybeShowPendingAd();
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                executors.mainThread().execute(() -> {
                    interstitialAd = null;
                    loading.set(false);
                    Log.w(config.getTag(), "❌ Interstitial - Load failed: " + loadAdError.getMessage());
                    failPendingRequest();
                });
            }
        });
    }

    private void maybeShowPendingAd() {
        if (pendingShowRequest == null) {
            return;
        }

        if (interstitialAd == null) {
            failPendingRequest();
            return;
        }

        Activity activity = pendingShowRequest.getActivity();
        if (!LifecycleGuard.isActivityValid(activity)) {
            failPendingRequest();
            return;
        }

        cancelOverlayTimeout();
        dismissLoadingDialog();

        InterstitialAd ad = interstitialAd;
        interstitialAd = null;

        ad.setAdEventCallback(new InterstitialAdEventCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                showing.set(true);
                Log.i(config.getTag(), "✅ Interstitial - Showed.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                executors.mainThread().execute(() -> {
                    showing.set(false);
                    Log.i(config.getTag(), "✅ Interstitial - Dismissed.");
                    finishPendingRequest();
                });
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                executors.mainThread().execute(() -> {
                    showing.set(false);
                    Log.w(config.getTag(), "❌ Interstitial - Show failed: " + error.getMessage());
                    finishPendingRequest();
                });
            }
        });

        showing.set(true);
        try {
            ad.show(activity);
        } catch (Exception e) {
            showing.set(false);
            Log.e(config.getTag(), "❌ Interstitial - Failed to show ad synchronously.", e);
            failPendingRequest();
        }
    }

    private void scheduleOverlayTimeout() {
        cancelOverlayTimeout();
        loadingTimeoutRunnable = () -> {
            if (pendingShowRequest != null) {
                Log.w(config.getTag(), "⏳ Interstitial - Loading overlay timed out.");
                failPendingRequest();
            }
        };
        executors.mainThread().postDelayed(loadingTimeoutRunnable, config.getLoadingOverlayTimeoutMs());
    }

    private void cancelOverlayTimeout() {
        if (loadingTimeoutRunnable != null) {
            executors.mainThread().removeCallbacks(loadingTimeoutRunnable);
            loadingTimeoutRunnable = null;
        }
    }

    private void showLoadingDialog(@NonNull Activity activity) {
        dismissLoadingDialog();
        AdLoadingOverlayProvider provider = config.getLoadingOverlayProvider();
        if (provider != null) {
            try {
                loadingDialog = provider.showLoading(activity);
            } catch (Exception e) {
                Log.e(config.getTag(), "❌ Interstitial - Failed to show loading dialog.", e);
                loadingDialog = null;
            }
        }
    }

    private void dismissLoadingDialog() {
        Dialog dialog = loadingDialog;
        loadingDialog = null;
        if (dialog != null) {
            AdLoadingOverlayProvider provider = config.getLoadingOverlayProvider();
            if (provider != null) {
                try {
                    provider.dismissLoading(dialog);
                } catch (Exception ignored) {
                }
            } else {
                try {
                    if (dialog.isShowing()) dialog.dismiss();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void finishPendingRequest() {
        PendingShowRequest request = pendingShowRequest;
        pendingShowRequest = null;
        cancelOverlayTimeout();
        dismissLoadingDialog();
        fullScreenCoordinator.releaseShowLock();

        if (request != null) {
            complete(request.continuationCallback);
            navigate(request.navigationCallback);
        }
    }

    private void failPendingRequest() {
        finishPendingRequest();
    }

    private void complete(@Nullable AdsCallback callback) {
        if (callback != null) {
            executors.mainThread().execute(callback::onComplete);
        }
    }

    private void navigate(@Nullable NavigationCallback callback) {
        if (callback != null) {
            executors.mainThread().execute(callback::navigate);
        }
    }
}
