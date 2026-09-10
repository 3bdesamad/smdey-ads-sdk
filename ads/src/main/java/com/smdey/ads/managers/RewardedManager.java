package com.smdey.ads.managers;

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
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback;
import com.smdey.ads.callbacks.AdsCallback;
import com.smdey.ads.callbacks.LoadingDialogProvider;
import com.smdey.ads.callbacks.OnUserEarnedRewardListener;
import com.smdey.ads.callbacks.RewardItem;
import com.smdey.ads.core.AdsConfig;
import com.smdey.ads.core.AppExecutors;
import com.smdey.ads.core.LifecycleGuard;
import com.smdey.ads.core.SdkGate;
import com.smdey.ads.utils.LoadingDialogHelper;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages Google Rewarded Ads preloading, presentation, loading fallback, and lifecycle safety.
 */
public final class RewardedManager {

    public interface OnLoadListener {
        void onAdLoaded();
        void onAdFailedToLoad();
    }

    private static final long DEFAULT_LOADING_TIMEOUT_MS = 6000L;

    private final SdkGate sdkGate;
    private final AdsConfig config;
    private final AtomicBoolean loading = new AtomicBoolean(false);

    private volatile RewardedAd rewardedAd;
    private volatile boolean adsRemoved = false;
    private OnLoadListener activeListener;
    private AdsCallback currentDismissCallback;

    @Nullable
    private Dialog loadingDialog;
    @Nullable
    private Runnable loadingTimeoutRunnable;
    @Nullable
    private LoadingDialogProvider customDialogProvider;

    public RewardedManager(@NonNull SdkGate sdkGate) {
        this.sdkGate = sdkGate;
        this.config = sdkGate.getConfig();
    }

    public void setAdsRemoved(boolean adsRemoved) {
        this.adsRemoved = adsRemoved;
        if (adsRemoved) {
            rewardedAd = null;
        }
    }

    public boolean isAdsRemoved() {
        return adsRemoved;
    }

    public void setCustomDialogProvider(@Nullable LoadingDialogProvider provider) {
        this.customDialogProvider = provider;
    }

    public boolean isAdReady() {
        return !adsRemoved && rewardedAd != null;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public void preloadAd(@NonNull Context context) {
        if (adsRemoved || !config.isRewardedEnabled() || config.getRewardedAdUnitId() == null) {
            return;
        }
        if (rewardedAd != null || loading.get()) {
            return;
        }
        loadAd(context.getApplicationContext(), null);
    }

    public void loadAd(@NonNull Context context, @Nullable OnLoadListener listener) {
        if (context instanceof Activity && !LifecycleGuard.isActivityValid((Activity) context)) {
            if (listener != null) listener.onAdFailedToLoad();
            return;
        }

        if (adsRemoved || !config.isRewardedEnabled() || config.getRewardedAdUnitId() == null) {
            Log.w(SdkGate.TAG, "⚠️ RewardedManager - Feature disabled or no ad unit ID configured.");
            if (listener != null) listener.onAdFailedToLoad();
            return;
        }

        ConsentManager consentManager = ConsentManager.getInstance(context, config);
        if (!consentManager.canRequestAds()) {
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                consentManager.gatherConsent(activity, () -> {
                    if (!LifecycleGuard.isActivityValid(activity)) {
                        if (listener != null) listener.onAdFailedToLoad();
                        return;
                    }
                    if (consentManager.canRequestAds()) {
                        sdkGate.notifyConsentResolved(activity);
                        loadAd(activity, listener);
                    } else {
                        if (listener != null) listener.onAdFailedToLoad();
                    }
                });
            } else if (listener != null) {
                listener.onAdFailedToLoad();
            }
            return;
        }

        if (rewardedAd != null) {
            if (listener != null) listener.onAdLoaded();
            return;
        }

        if (!sdkGate.isReady()) {
            if (context instanceof Activity) {
                sdkGate.ensureInitialized((Activity) context, () -> loadAd(context, listener));
            } else if (listener != null) {
                listener.onAdFailedToLoad();
            }
            return;
        }

        if (!loading.compareAndSet(false, true)) {
            activeListener = listener;
            return;
        }

        activeListener = listener;
        AdRequest adRequest = new AdRequest.Builder(config.getRewardedAdUnitId()).build();
        RewardedAd.load(
                adRequest,
                new AdLoadCallback<RewardedAd>() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            rewardedAd = ad;
                            loading.set(false);
                            bindFullScreenCallback();
                            Log.i(SdkGate.TAG, "✅ RewardedManager - Ad loaded successfully.");
                            notifySuccess();
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            rewardedAd = null;
                            loading.set(false);
                            Log.w(SdkGate.TAG, "❌ RewardedManager - Failed to load: " + loadAdError.getMessage());
                            notifyFailed();
                        });
                    }
                }
        );
    }

    public void showRewardAdWithLoading(@NonNull Activity activity,
                                        @NonNull OnUserEarnedRewardListener rewardListener,
                                        @Nullable AdsCallback dismissCallback,
                                        @Nullable AdsCallback failCallback) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            if (failCallback != null) failCallback.onAction();
            return;
        }

        if (adsRemoved || !config.isRewardedEnabled() || config.getRewardedAdUnitId() == null) {
            if (failCallback != null) failCallback.onAction();
            return;
        }

        // Fast path: Ad is already pre-cached -> 0ms instant display without loading dialog
        if (isAdReady()) {
            Log.i(SdkGate.TAG, "⚡ RewardedManager - Ad is preloaded. Presenting instantly.");
            showAd(activity, rewardListener, dismissCallback);
            return;
        }

        // Cache-miss fallback: show loading dialog with timeout safeguard while fetching
        showLoadingDialog(activity);
        scheduleOverlayTimeout(() -> {
            dismissLoadingDialog();
            Log.w(SdkGate.TAG, "⏳ RewardedManager - Timed out waiting for ad to load.");
            if (failCallback != null) {
                failCallback.onAction();
            }
        });

        loadAd(activity, new OnLoadListener() {
            @Override
            public void onAdLoaded() {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    cancelOverlayTimeout();
                    dismissLoadingDialog();
                    if (LifecycleGuard.isActivityValid(activity)) {
                        showAd(activity, rewardListener, dismissCallback);
                    } else if (failCallback != null) {
                        failCallback.onAction();
                    }
                });
            }

            @Override
            public void onAdFailedToLoad() {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    cancelOverlayTimeout();
                    dismissLoadingDialog();
                    if (failCallback != null) {
                        failCallback.onAction();
                    }
                });
            }
        });
    }

    public void showAd(@NonNull Activity activity, @NonNull OnUserEarnedRewardListener rewardListener) {
        showAd(activity, rewardListener, null);
    }

    public void showAd(@NonNull Activity activity,
                       @NonNull OnUserEarnedRewardListener rewardListener,
                       @Nullable AdsCallback dismissCallback) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            if (dismissCallback != null) dismissCallback.onAction();
            return;
        }

        if (adsRemoved || !config.isRewardedEnabled()) {
            if (dismissCallback != null) dismissCallback.onAction();
            return;
        }

        ConsentManager consentManager = ConsentManager.getInstance(activity, config);
        if (!consentManager.canRequestAds()) {
            if (dismissCallback != null) dismissCallback.onAction();
            return;
        }

        RewardedAd readyAd = rewardedAd;
        if (readyAd == null) {
            Log.d(SdkGate.TAG, "⏳ RewardedManager - Ad is not ready yet.");
            if (dismissCallback != null) dismissCallback.onAction();
            return;
        }

        this.currentDismissCallback = dismissCallback;

        readyAd.show(activity, item -> {
            Log.i(SdkGate.TAG, "🎁 RewardedManager - Reward earned: " + item.getAmount() + " " + item.getType());
            AppExecutors.getInstance().mainThread().execute(() -> {
                rewardListener.onUserEarnedReward(new RewardItem() {
                    @Override
                    public int getAmount() {
                        return item.getAmount();
                    }

                    @NonNull
                    @Override
                    public String getType() {
                        return item.getType() != null ? item.getType() : "";
                    }
                });
            });
        });
    }

    private void bindFullScreenCallback() {
        if (rewardedAd == null) return;

        rewardedAd.setAdEventCallback(new RewardedAdEventCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                Log.i(SdkGate.TAG, "✅ RewardedManager - Ad presented.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    rewardedAd = null;
                    Log.i(SdkGate.TAG, "✅ RewardedManager - Ad dismissed.");
                    AdsCallback callback = currentDismissCallback;
                    currentDismissCallback = null;
                    if (callback != null) {
                        callback.onAction();
                    }
                    // Proactively pre-cache the next rewarded ad in background
                    preloadAd(sdkGate.getAppContext());
                });
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    rewardedAd = null;
                    Log.w(SdkGate.TAG, "❌ RewardedManager - Failed to show: " + fullScreenContentError.getMessage());
                    AdsCallback callback = currentDismissCallback;
                    currentDismissCallback = null;
                    if (callback != null) {
                        callback.onAction();
                    }
                    preloadAd(sdkGate.getAppContext());
                });
            }
        });
    }

    private void showLoadingDialog(@NonNull Activity activity) {
        dismissLoadingDialog();
        loadingDialog = LoadingDialogHelper.showLoadingDialog(activity, customDialogProvider);
    }

    private void dismissLoadingDialog() {
        LoadingDialogHelper.dismissSafely(loadingDialog);
        loadingDialog = null;
    }

    private void scheduleOverlayTimeout(@NonNull Runnable onTimeout) {
        cancelOverlayTimeout();
        loadingTimeoutRunnable = onTimeout;
        AppExecutors.getInstance().mainThread().postDelayed(loadingTimeoutRunnable, DEFAULT_LOADING_TIMEOUT_MS);
    }

    private void cancelOverlayTimeout() {
        if (loadingTimeoutRunnable != null) {
            AppExecutors.getInstance().mainThread().removeCallbacks(loadingTimeoutRunnable);
            loadingTimeoutRunnable = null;
        }
    }

    private void notifySuccess() {
        OnLoadListener listener = activeListener;
        activeListener = null;
        if (listener != null) {
            listener.onAdLoaded();
        }
    }

    private void notifyFailed() {
        OnLoadListener listener = activeListener;
        activeListener = null;
        if (listener != null) {
            listener.onAdFailedToLoad();
        }
    }
}
