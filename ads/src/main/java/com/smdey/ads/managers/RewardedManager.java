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
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.smdey.ads.callbacks.OnUserEarnedRewardListener;
import com.smdey.ads.callbacks.RewardItem;
import com.smdey.ads.core.AdsConfig;
import com.smdey.ads.core.LifecycleGuard;
import com.smdey.ads.core.SdkGate;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages Rewarded Ads loading and presentation.
 */
public final class RewardedManager {

    public interface OnLoadListener {
        void onAdLoaded();
        void onAdFailedToLoad();
    }

    private final SdkGate sdkGate;
    private final AdsConfig config;
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private RewardedAd rewardedAd;
    private OnLoadListener activeListener;

    public RewardedManager(@NonNull SdkGate sdkGate) {
        this.sdkGate = sdkGate;
        this.config = sdkGate.getConfig();
    }

    public boolean isAdReady() {
        return rewardedAd != null;
    }

    public void loadAd(@NonNull Context context, @Nullable OnLoadListener listener) {
        if (context instanceof Activity && !LifecycleGuard.isActivityValid((Activity) context)) {
            if (listener != null) listener.onAdFailedToLoad();
            return;
        }

        if (!config.isRewardedEnabled() || config.getRewardedAdUnitId() == null) {
            Log.w(SdkGate.TAG, "⚠️ RewardedManager - Feature disabled or no ad unit ID configured.");
            if (listener != null) listener.onAdFailedToLoad();
            return;
        }

        ConsentManager consentManager = ConsentManager.getInstance(context, config);
        if (!consentManager.canRequestAds()) {
            if (listener != null) listener.onAdFailedToLoad();
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
        RewardedAd.load(
                context,
                config.getRewardedAdUnitId(),
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        loading.set(false);
                        bindFullScreenCallback();
                        Log.i(SdkGate.TAG, "✅ RewardedManager - Ad loaded successfully.");
                        notifySuccess();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedAd = null;
                        loading.set(false);
                        Log.w(SdkGate.TAG, "❌ RewardedManager - Failed to load: " + loadAdError.getMessage());
                        notifyFailed();
                    }
                }
        );
    }

    public void showAd(@NonNull Activity activity, @NonNull OnUserEarnedRewardListener rewardListener) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            return;
        }

        ConsentManager consentManager = ConsentManager.getInstance(activity, config);
        if (!consentManager.canRequestAds()) {
            return;
        }

        RewardedAd readyAd = rewardedAd;
        if (readyAd == null) {
            Log.d(SdkGate.TAG, "⏳ RewardedManager - Ad is not ready yet.");
            return;
        }

        readyAd.show(activity, item -> {
            Log.i(SdkGate.TAG, "🎁 RewardedManager - Reward earned: " + item.getAmount() + " " + item.getType());
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
        if (rewardedAd == null) return;

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                Log.i(SdkGate.TAG, "✅ RewardedManager - Ad presented.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                Log.i(SdkGate.TAG, "✅ RewardedManager - Ad dismissed.");
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                rewardedAd = null;
                Log.w(SdkGate.TAG, "❌ RewardedManager - Failed to show: " + adError.getMessage());
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
