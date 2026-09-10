package com.smdey.ads.managers;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback;
import com.smdey.ads.R;
import com.smdey.ads.callbacks.AdsCallback;
import com.smdey.ads.callbacks.IntroDialogProvider;
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
 * Manages Google Rewarded Interstitial Ads loading, presentation, Google policy-compliant
 * introductory screen, loading fallback, and lifecycle safety.
 */
public final class RewardedInterstitialManager {

    public interface OnLoadListener {
        void onAdLoaded();
        void onAdFailedToLoad();
    }

    private static final long DEFAULT_LOADING_TIMEOUT_MS = 6000L;

    private final SdkGate sdkGate;
    private final AdsConfig config;
    private final AtomicBoolean loading = new AtomicBoolean(false);

    private volatile RewardedInterstitialAd rewardedInterstitialAd;
    private volatile boolean adsRemoved = false;
    private OnLoadListener activeListener;
    private AdsCallback currentDismissCallback;

    @Nullable
    private Dialog loadingDialog;
    @Nullable
    private Dialog introDialog;
    @Nullable
    private Runnable loadingTimeoutRunnable;
    @Nullable
    private LoadingDialogProvider customLoadingDialogProvider;
    @Nullable
    private IntroDialogProvider customIntroDialogProvider;

    public RewardedInterstitialManager(@NonNull SdkGate sdkGate) {
        this.sdkGate = sdkGate;
        this.config = sdkGate.getConfig();
    }

    public void setAdsRemoved(boolean adsRemoved) {
        this.adsRemoved = adsRemoved;
        if (adsRemoved) {
            rewardedInterstitialAd = null;
        }
    }

    public boolean isAdsRemoved() {
        return adsRemoved;
    }

    public void setCustomLoadingDialogProvider(@Nullable LoadingDialogProvider provider) {
        this.customLoadingDialogProvider = provider;
    }

    public void setCustomIntroDialogProvider(@Nullable IntroDialogProvider provider) {
        this.customIntroDialogProvider = provider;
    }

    public boolean isAdReady() {
        return !adsRemoved && rewardedInterstitialAd != null;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public void preloadAd(@NonNull Context context) {
        if (adsRemoved || !config.isRewardedInterstitialEnabled() || config.getRewardedInterstitialAdUnitId() == null) {
            return;
        }
        if (rewardedInterstitialAd != null || loading.get()) {
            return;
        }
        loadAd(context.getApplicationContext(), null);
    }

    public void loadAd(@NonNull Context context, @Nullable OnLoadListener listener) {
        if (context instanceof Activity && !LifecycleGuard.isActivityValid((Activity) context)) {
            if (listener != null) listener.onAdFailedToLoad();
            return;
        }

        if (adsRemoved || !config.isRewardedInterstitialEnabled() || config.getRewardedInterstitialAdUnitId() == null) {
            Log.w(SdkGate.TAG, "⚠️ RewardedInterstitialManager - Feature disabled or no ad unit ID configured.");
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

        if (rewardedInterstitialAd != null) {
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

        AdRequest adRequest = new AdRequest.Builder(config.getRewardedInterstitialAdUnitId()).build();
        RewardedInterstitialAd.load(
                adRequest,
                new AdLoadCallback<RewardedInterstitialAd>() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            rewardedInterstitialAd = ad;
                            loading.set(false);
                            bindFullScreenCallback();
                            Log.i(SdkGate.TAG, "✅ RewardedInterstitial - Loaded successfully.");
                            notifySuccess();
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            rewardedInterstitialAd = null;
                            loading.set(false);
                            Log.w(SdkGate.TAG, "❌ RewardedInterstitial - Failed to load: " + loadAdError.getMessage());
                            notifyFailed();
                        });
                    }
                }
        );
    }

    /**
     * Presents a Google Policy-Compliant Introductory Screen before showing the Rewarded Interstitial ad.
     * The user is informed of the reward and given an unobstructed option to Skip ("No Thanks").
     */
    public void showWithIntroDialog(@NonNull Activity activity,
                                    @NonNull String rewardDescription,
                                    @NonNull OnUserEarnedRewardListener rewardListener,
                                    @Nullable AdsCallback dismissCallback) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            if (dismissCallback != null) dismissCallback.onAction();
            return;
        }

        if (adsRemoved || !config.isRewardedInterstitialEnabled() || config.getRewardedInterstitialAdUnitId() == null) {
            if (dismissCallback != null) dismissCallback.onAction();
            return;
        }

        dismissIntroDialog();

        Runnable onWatch = () -> {
            dismissIntroDialog();
            showAdWithLoading(activity, rewardListener, dismissCallback, dismissCallback);
        };

        Runnable onSkip = () -> {
            dismissIntroDialog();
            Log.i(SdkGate.TAG, "ℹ️ RewardedInterstitial - User opted out of watching rewarded ad.");
            if (dismissCallback != null) {
                dismissCallback.onAction();
            }
        };

        if (customIntroDialogProvider != null) {
            introDialog = customIntroDialogProvider.createIntroDialog(activity, rewardDescription, onWatch, onSkip);
        }

        if (introDialog == null) {
            introDialog = createDefaultIntroDialog(activity, rewardDescription, onWatch, onSkip);
        }

        try {
            if (introDialog != null && LifecycleGuard.isActivityValid(activity)) {
                introDialog.show();
            } else {
                onWatch.run();
            }
        } catch (Exception e) {
            Log.e(SdkGate.TAG, "❌ RewardedInterstitial - Error showing intro dialog: " + e.getMessage());
            onWatch.run();
        }
    }

    public void showAdWithLoading(@NonNull Activity activity,
                                  @NonNull OnUserEarnedRewardListener rewardListener,
                                  @Nullable AdsCallback dismissCallback,
                                  @Nullable AdsCallback failCallback) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            if (failCallback != null) failCallback.onAction();
            return;
        }

        if (adsRemoved || !config.isRewardedInterstitialEnabled() || config.getRewardedInterstitialAdUnitId() == null) {
            if (failCallback != null) failCallback.onAction();
            return;
        }

        // Fast path: Ad is already pre-cached -> 0ms instant display without loading dialog
        if (isAdReady()) {
            Log.i(SdkGate.TAG, "⚡ RewardedInterstitial - Ad is preloaded. Presenting instantly.");
            showAd(activity, rewardListener, dismissCallback);
            return;
        }

        // Cache-miss fallback: show loading dialog with timeout safeguard while fetching
        showLoadingDialog(activity);
        scheduleOverlayTimeout(() -> {
            dismissLoadingDialog();
            Log.w(SdkGate.TAG, "⏳ RewardedInterstitial - Timed out waiting for ad to load.");
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

        if (adsRemoved || !config.isRewardedInterstitialEnabled()) {
            if (dismissCallback != null) dismissCallback.onAction();
            return;
        }

        ConsentManager consentManager = ConsentManager.getInstance(activity, config);
        if (!consentManager.canRequestAds()) {
            if (dismissCallback != null) dismissCallback.onAction();
            return;
        }

        RewardedInterstitialAd readyAd = rewardedInterstitialAd;
        if (readyAd == null) {
            Log.d(SdkGate.TAG, "⏳ RewardedInterstitial - Ad is not ready yet.");
            if (dismissCallback != null) dismissCallback.onAction();
            return;
        }

        this.currentDismissCallback = dismissCallback;

        readyAd.show(activity, item -> {
            Log.i(SdkGate.TAG, "🎁 RewardedInterstitial - Reward earned: " + item.getAmount() + " " + item.getType());
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
        if (rewardedInterstitialAd == null) return;

        rewardedInterstitialAd.setAdEventCallback(new RewardedInterstitialAdEventCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                Log.i(SdkGate.TAG, "✅ RewardedInterstitial - Ad presented.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    rewardedInterstitialAd = null;
                    Log.i(SdkGate.TAG, "✅ RewardedInterstitial - Ad dismissed.");
                    AdsCallback callback = currentDismissCallback;
                    currentDismissCallback = null;
                    if (callback != null) {
                        callback.onAction();
                    }
                    // Auto-preload the next rewarded interstitial in background
                    preloadAd(sdkGate.getAppContext());
                });
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    rewardedInterstitialAd = null;
                    Log.w(SdkGate.TAG, "❌ RewardedInterstitial - Failed to show: " + fullScreenContentError.getMessage());
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

    @NonNull
    private Dialog createDefaultIntroDialog(@NonNull Activity activity,
                                            @NonNull String rewardDescription,
                                            @NonNull Runnable onWatchSelected,
                                            @NonNull Runnable onSkipSelected) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_rewarded_interstitial_intro, null);
        dialog.setContentView(view);
        dialog.setCancelable(false);

        LoadingDialogHelper.setupDialogWindowBounds(dialog, activity);

        TextView tvReward = view.findViewById(R.id.tv_intro_reward);
        View btnSkip = view.findViewById(R.id.btn_skip);
        View btnWatch = view.findViewById(R.id.btn_watch);

        if (tvReward != null) {
            tvReward.setText(rewardDescription);
        }

        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> {
                try {
                    dialog.dismiss();
                } catch (Exception ignored) {}
                onSkipSelected.run();
            });
        }

        if (btnWatch != null) {
            btnWatch.setOnClickListener(v -> {
                try {
                    dialog.dismiss();
                } catch (Exception ignored) {}
                onWatchSelected.run();
            });
        }

        return dialog;
    }

    private void showLoadingDialog(@NonNull Activity activity) {
        dismissLoadingDialog();
        loadingDialog = LoadingDialogHelper.showLoadingDialog(activity, customLoadingDialogProvider);
    }

    private void dismissLoadingDialog() {
        LoadingDialogHelper.dismissSafely(loadingDialog);
        loadingDialog = null;
    }

    private void dismissIntroDialog() {
        LoadingDialogHelper.dismissSafely(introDialog);
        introDialog = null;
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
