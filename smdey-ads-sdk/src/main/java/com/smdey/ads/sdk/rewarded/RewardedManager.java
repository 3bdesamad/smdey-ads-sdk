package com.smdey.ads.sdk.rewarded;

import android.app.Activity;
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
import com.smdey.ads.sdk.AdsConfig;
import com.smdey.ads.sdk.callbacks.OnRewardEarnedListener;
import com.smdey.ads.sdk.consent.ConsentManager;
import com.smdey.ads.sdk.core.AppExecutors;
import com.smdey.ads.sdk.core.FullScreenAdCoordinator;
import com.smdey.ads.sdk.core.LifecycleGuard;
import com.smdey.ads.sdk.core.SdkGate;

import java.util.concurrent.atomic.AtomicBoolean;

public final class RewardedManager {

    /**
     * Callback for asynchronous Rewarded ad load requests.
     * Guaranteed to execute on the Android UI (Main) thread.
     */
    public interface OnLoadListener {
        @androidx.annotation.MainThread
        void onLoaded();

        @androidx.annotation.MainThread
        void onFailed();
    }

    private final AdsConfig config;
    private final SdkGate sdkGate;
    private final ConsentManager consentManager;
    private final FullScreenAdCoordinator fullScreenCoordinator;
    private final AppExecutors executors = AppExecutors.getInstance();
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private final AtomicBoolean showing = new AtomicBoolean(false);
    private RewardedAd rewardedAd;

    public RewardedManager(@NonNull AdsConfig config,
                           @NonNull SdkGate sdkGate,
                           @Nullable ConsentManager consentManager,
                           @NonNull FullScreenAdCoordinator fullScreenCoordinator) {
        this.config = config;
        this.sdkGate = sdkGate;
        this.consentManager = consentManager;
        this.fullScreenCoordinator = fullScreenCoordinator;
    }

    public RewardedManager(@NonNull AdsConfig config, @NonNull SdkGate sdkGate) {
        this(config, sdkGate, null, new FullScreenAdCoordinator());
    }

    public boolean isAdReady() {
        return rewardedAd != null;
    }

    public void preloadAd(@NonNull Context context) {
        loadAd(context, null);
    }

    public void loadAd(@NonNull Context context, @Nullable OnLoadListener listener) {
        if (config.isAdsRemoved() || !config.isRewardedEnabled() || config.getRewardedId() == null || isAdReady()) {
            if (listener != null) {
                if (isAdReady()) listener.onLoaded();
                else listener.onFailed();
            }
            return;
        }

        if (consentManager != null && !consentManager.canRequestAds()) {
            Activity activity = (context instanceof Activity) ? (Activity) context : null;
            if (activity != null) {
                Log.i(config.getTag(), "⏳ Rewarded - Waiting for consent before loading...");
                consentManager.gatherConsent(activity, () -> {
                    if (consentManager.canRequestAds()) {
                        sdkGate.notifyConsentResolved(activity);
                        loadAd(context, listener);
                    } else if (listener != null) {
                        listener.onFailed();
                    }
                });
                return;
            }
            if (listener != null) listener.onFailed();
            return;
        }

        if (!sdkGate.isReady()) {
            Activity activity = (context instanceof Activity) ? (Activity) context : null;
            if (activity != null) {
                Log.i(config.getTag(), "⏳ Rewarded - SDK initializing. Waiting for readiness before loading...");
                sdkGate.ensureInitialized(activity, () -> loadAd(context, listener));
                return;
            }
            if (listener != null) listener.onFailed();
            return;
        }

        if (!loading.compareAndSet(false, true)) {
            return;
        }

        AdRequest request = new AdRequest.Builder(config.getRewardedId()).build();
        RewardedAd.load(request, new AdLoadCallback<RewardedAd>() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                executors.mainThread().execute(() -> {
                    rewardedAd = ad;
                    loading.set(false);
                    Log.i(config.getTag(), "✅ Rewarded - Loaded successfully (Next-Gen).");
                    if (listener != null) {
                        listener.onLoaded();
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                executors.mainThread().execute(() -> {
                    rewardedAd = null;
                    loading.set(false);
                    Log.w(config.getTag(), "❌ Rewarded - Failed to load: " + loadAdError.getMessage());
                    if (listener != null) {
                        listener.onFailed();
                    }
                });
            }
        });
    }

    public void showAd(@NonNull Activity activity, @NonNull OnRewardEarnedListener rewardListener) {
        if (!LifecycleGuard.isActivityValid(activity) || config.isAdsRemoved() || !config.isRewardedEnabled() || rewardedAd == null) {
            Log.w(config.getTag(), "⚠️ Rewarded - Not ready to show.");
            return;
        }

        if (showing.get() || fullScreenCoordinator.isFullScreenShowing()) {
            Log.w(config.getTag(), "⏳ Rewarded - Another full-screen ad is already showing. Suppressing.");
            return;
        }

        if (!fullScreenCoordinator.tryAcquireShowLock()) {
            Log.w(config.getTag(), "⏳ Rewarded - Could not acquire full-screen lock. Suppressing.");
            return;
        }

        RewardedAd ad = rewardedAd;
        rewardedAd = null;

        ad.setAdEventCallback(new RewardedAdEventCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                showing.set(true);
                Log.i(config.getTag(), "✅ Rewarded - Ad showed.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                executors.mainThread().execute(() -> {
                    showing.set(false);
                    fullScreenCoordinator.releaseShowLock();
                    Log.i(config.getTag(), "✅ Rewarded - Dismissed.");
                    loadAd(activity.getApplicationContext(), null);
                });
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                executors.mainThread().execute(() -> {
                    showing.set(false);
                    fullScreenCoordinator.releaseShowLock();
                    Log.w(config.getTag(), "❌ Rewarded - Failed to show: " + error.getMessage());
                });
            }
        });

        showing.set(true);
        try {
            ad.show(activity, rewardItem -> executors.mainThread().execute(() -> {
                int amount = rewardItem != null ? rewardItem.getAmount() : 1;
                String type = rewardItem != null && rewardItem.getType() != null ? rewardItem.getType() : "reward";
                Log.i(config.getTag(), "🎁 Rewarded - Reward earned: " + amount + " " + type);
                rewardListener.onUserEarnedReward(amount, type);
            }));
        } catch (Exception e) {
            showing.set(false);
            fullScreenCoordinator.releaseShowLock();
            Log.e(config.getTag(), "❌ Rewarded - Failed to show ad synchronously.", e);
        }
    }
}
