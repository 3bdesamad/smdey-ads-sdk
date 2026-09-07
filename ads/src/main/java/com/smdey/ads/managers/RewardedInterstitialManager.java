package com.smdey.ads.managers;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.smdey.ads.callbacks.AdsCallback;
import com.smdey.ads.callbacks.OnUserEarnedRewardListener;
import com.smdey.ads.callbacks.RewardItem;
import com.smdey.ads.core.AdsConfig;
import com.smdey.ads.core.LifecycleGuard;
import com.smdey.ads.core.SdkGate;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages Google Rewarded Interstitial Ads loading, presentation, and lifecycle safety.
 */
public final class RewardedInterstitialManager {

    public interface OnLoadListener {
        void onAdLoaded();
        void onAdFailedToLoad();
    }

    private final SdkGate sdkGate;
    private final AdsConfig config;
    private final AtomicBoolean loading = new AtomicBoolean(false);

    private volatile RewardedInterstitialAd rewardedInterstitialAd;
    private volatile boolean adsRemoved = false;
    private OnLoadListener activeListener;
    private AdsCallback currentDismissCallback;

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

    public boolean isAdReady() {
        return !adsRemoved && rewardedInterstitialAd != null;
    }

    public void preloadAd(@NonNull Context context) {
        if (adsRemoved || !config.isRewardedInterstitialEnabled() || config.getRewardedInterstitialAdUnitId() == null) {
            return;
        }
        if (rewardedInterstitialAd != null || loading.get()) {
            return;
        }
        loadAd(context, null);
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
        Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;

        RewardedInterstitialAd.load(
                appContext,
                config.getRewardedInterstitialAdUnitId(),
                new AdRequest.Builder().build(),
                new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                        rewardedInterstitialAd = ad;
                        loading.set(false);
                        bindFullScreenCallback();
                        Log.i(SdkGate.TAG, "✅ RewardedInterstitial - Loaded successfully.");
                        notifySuccess();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedInterstitialAd = null;
                        loading.set(false);
                        Log.w(SdkGate.TAG, "❌ RewardedInterstitial - Failed to load: " + loadAdError.getMessage());
                        notifyFailed();
                    }
                }
        );
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
    }

    private void bindFullScreenCallback() {
        if (rewardedInterstitialAd == null) return;

        rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                Log.i(SdkGate.TAG, "✅ RewardedInterstitial - Ad presented.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null;
                Log.i(SdkGate.TAG, "✅ RewardedInterstitial - Ad dismissed.");
                AdsCallback callback = currentDismissCallback;
                currentDismissCallback = null;
                if (callback != null) {
                    callback.onAction();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                rewardedInterstitialAd = null;
                Log.w(SdkGate.TAG, "❌ RewardedInterstitial - Failed to show: " + adError.getMessage());
                AdsCallback callback = currentDismissCallback;
                currentDismissCallback = null;
                if (callback != null) {
                    callback.onAction();
                }
            }
        });
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
