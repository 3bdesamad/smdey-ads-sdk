package com.smdey.ads.managers;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.smdey.ads.R;
import com.smdey.ads.callbacks.AdsCallback;
import com.smdey.ads.callbacks.LoadingDialogProvider;
import com.smdey.ads.callbacks.NavigationCallback;
import com.smdey.ads.core.AdsConfig;
import com.smdey.ads.core.LifecycleGuard;
import com.smdey.ads.core.SdkGate;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages Interstitial Ads, click-frequency counters, preloading, and safe loading overlays.
 */
public final class InterstitialManager {

    private static final long CLICK_DEBOUNCE_MS = 300L;
    private static final int PREFETCH_THRESHOLD = 2;

    private final SdkGate sdkGate;
    private final AdsConfig config;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private final AtomicBoolean showing = new AtomicBoolean(false);

    @Nullable
    private InterstitialAd interstitialAd;
    @Nullable
    private Dialog loadingDialog;
    @Nullable
    private Runnable loadingTimeoutRunnable;
    @Nullable
    private LoadingDialogProvider customDialogProvider;

    private volatile boolean adsRemoved = false;
    private int clickCount = 0;
    private long lastClickTimestamp;
    private long lastAdShowTimestamp;

    private record PendingRequest(
            @NonNull WeakReference<Activity> activityRef,
            @Nullable AdsCallback continuationCallback,
            @Nullable NavigationCallback navigationCallback
    ) {}

    @Nullable
    private PendingRequest pendingRequest;

    public InterstitialManager(@NonNull SdkGate sdkGate) {
        this.sdkGate = sdkGate;
        this.config = sdkGate.getConfig();
    }

    public void setAdsRemoved(boolean adsRemoved) {
        this.adsRemoved = adsRemoved;
        if (adsRemoved) {
            interstitialAd = null;
        }
    }

    public boolean isAdsRemoved() {
        return adsRemoved;
    }

    public void setCustomDialogProvider(@Nullable LoadingDialogProvider provider) {
        this.customDialogProvider = provider;
    }

    public boolean isAdReady() {
        return interstitialAd != null;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public int getClickCount() {
        return clickCount;
    }

    public void resetClickCount() {
        this.clickCount = 0;
    }

    public void preloadAd(@NonNull Activity activity) {
        if (adsRemoved || !config.isInterstitialEnabled()) {
            return;
        }

        ConsentManager consentManager = ConsentManager.getInstance(activity, config);
        if (!consentManager.canRequestAds()) {
            return;
        }

        requestLoad(activity.getApplicationContext(), activity, true);
    }

    public void showAdWithLoadingOverlay(@NonNull Activity activity, @Nullable AdsCallback callback) {
        if (adsRemoved || !config.isInterstitialEnabled()) {
            if (callback != null) callback.onAction();
            return;
        }

        ConsentManager consentManager = ConsentManager.getInstance(activity, config);
        if (!consentManager.canRequestAds()) {
            consentManager.gatherConsent(activity, () -> {
                if (consentManager.canRequestAds()) {
                    startShowFlow(activity, callback, null);
                } else if (callback != null) {
                    callback.onAction();
                }
            });
            return;
        }

        startShowFlow(activity, callback, null);
    }

    public void navigationClickAd(@NonNull Activity activity, @NonNull NavigationCallback navigationCallback) {
        navigationClickAd(activity, navigationCallback, config.getInterstitialFrequency());
    }

    public void navigationClickAd(@NonNull Activity activity, @NonNull NavigationCallback navigationCallback, int frequency) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            navigationCallback.navigate();
            return;
        }

        if (adsRemoved || !config.isInterstitialEnabled()) {
            navigationCallback.navigate();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastClickTimestamp < CLICK_DEBOUNCE_MS) {
            return;
        }
        lastClickTimestamp = now;

        clickCount++;
        Log.d(SdkGate.TAG, "🔢 Interstitial - Click count: " + clickCount + " / " + frequency);

        if (frequency <= 0) {
            navigationCallback.navigate();
            return;
        }

        if (clickCount % frequency == 0) {
            if (now - lastAdShowTimestamp >= config.getInterstitialCooldownMs()) {
                lastAdShowTimestamp = now;
                clickCount = 0;
                startShowFlow(activity, null, navigationCallback);
                return;
            }
            clickCount = 0;
            navigationCallback.navigate();
            return;
        }

        int remaining = frequency - (clickCount % frequency);
        if (remaining <= PREFETCH_THRESHOLD) {
            preloadAd(activity);
        }

        navigationCallback.navigate();
    }

    private void startShowFlow(@NonNull Activity activity,
                               @Nullable AdsCallback continuationCallback,
                               @Nullable NavigationCallback navigationCallback) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            if (continuationCallback != null) continuationCallback.onAction();
            if (navigationCallback != null) navigationCallback.navigate();
            return;
        }

        if (showing.get() || pendingRequest != null) {
            if (continuationCallback != null) continuationCallback.onAction();
            if (navigationCallback != null) navigationCallback.navigate();
            return;
        }

        pendingRequest = new PendingRequest(new WeakReference<>(activity), continuationCallback, navigationCallback);
        showLoadingDialog(activity);
        scheduleOverlayTimeout();

        if (interstitialAd != null) {
            mainHandler.post(this::maybeShowPendingAd);
            return;
        }

        requestLoad(activity.getApplicationContext(), activity, false);
    }

    private void requestLoad(@NonNull Context context,
                             @Nullable Activity activity,
                             boolean isBackgroundPreload) {
        if (adsRemoved || !config.isInterstitialEnabled()) {
            failPendingRequest(true);
            return;
        }

        if (interstitialAd != null) {
            maybeShowPendingAd();
            return;
        }

        if (!sdkGate.isReady()) {
            if (activity != null && LifecycleGuard.isActivityValid(activity)) {
                sdkGate.ensureInitialized(activity, () -> requestLoad(context, activity, isBackgroundPreload));
            } else {
                failPendingRequest(true);
            }
            return;
        }

        if (!loading.compareAndSet(false, true)) {
            return;
        }

        InterstitialAd.load(
                context,
                config.getInterstitialAdUnitId(),
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        loading.set(false);
                        Log.i(SdkGate.TAG, "✅ Interstitial - Loaded successfully.");
                        maybeShowPendingAd();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialAd = null;
                        loading.set(false);
                        Log.w(SdkGate.TAG, "❌ Interstitial - Failed to load: " + loadAdError.getMessage());
                        failPendingRequest(true);
                    }
                }
        );
    }

    private void maybeShowPendingAd() {
        PendingRequest request = pendingRequest;
        InterstitialAd readyAd = interstitialAd;

        if (request == null || readyAd == null) {
            return;
        }

        Activity activity = request.activityRef.get();
        if (!LifecycleGuard.isActivityValid(activity)) {
            failPendingRequest(false);
            return;
        }

        cancelOverlayTimeout();
        dismissLoadingDialog();

        pendingRequest = null;
        interstitialAd = null;
        showing.set(true);

        readyAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                Log.i(SdkGate.TAG, "✅ Interstitial - Ad presented.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                showing.set(false);
                Log.i(SdkGate.TAG, "✅ Interstitial - Ad dismissed.");
                if (request.continuationCallback != null) request.continuationCallback.onAction();
                if (request.navigationCallback != null) request.navigationCallback.navigate();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                showing.set(false);
                Log.w(SdkGate.TAG, "❌ Interstitial - Failed to show: " + adError.getMessage());
                if (request.continuationCallback != null) request.continuationCallback.onAction();
                if (request.navigationCallback != null) request.navigationCallback.navigate();
            }
        });

        readyAd.show(activity);
    }

    private void showLoadingDialog(@NonNull Activity activity) {
        dismissLoadingDialog();

        if (customDialogProvider != null) {
            loadingDialog = customDialogProvider.createLoadingDialog(activity);
        }

        if (loadingDialog == null) {
            loadingDialog = createDefaultLoadingDialog(activity);
        }

        try {
            if (loadingDialog != null) {
                loadingDialog.show();
            }
        } catch (Exception e) {
            Log.e(SdkGate.TAG, "❌ Interstitial - Error showing loading dialog.", e);
            loadingDialog = null;
        }
    }

    private Dialog createDefaultLoadingDialog(@NonNull Activity activity) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_loading_ad, null);
        dialog.setContentView(view);
        dialog.setCancelable(false);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null) {
            try {
                if (loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
            } catch (Exception ignored) {
            }
            loadingDialog = null;
        }
    }

    private void scheduleOverlayTimeout() {
        cancelOverlayTimeout();
        loadingTimeoutRunnable = () -> {
            if (pendingRequest != null) {
                Log.w(SdkGate.TAG, "⏳ Interstitial - Loading timed out.");
                failPendingRequest(true);
            }
        };
        mainHandler.postDelayed(loadingTimeoutRunnable, config.getLoadingTimeoutMs());
    }

    private void cancelOverlayTimeout() {
        if (loadingTimeoutRunnable != null) {
            mainHandler.removeCallbacks(loadingTimeoutRunnable);
            loadingTimeoutRunnable = null;
        }
    }

    private void failPendingRequest(boolean navigate) {
        PendingRequest request = pendingRequest;
        pendingRequest = null;
        cancelOverlayTimeout();
        dismissLoadingDialog();

        if (request != null) {
            if (request.continuationCallback != null) request.continuationCallback.onAction();
            if (navigate && request.navigationCallback != null) request.navigationCallback.navigate();
        }
    }
}
