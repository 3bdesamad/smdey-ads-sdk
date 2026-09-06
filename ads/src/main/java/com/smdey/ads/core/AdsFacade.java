package com.smdey.ads.core;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.smdey.ads.callbacks.ConsentCallback;
import com.smdey.ads.managers.AppOpenManager;
import com.smdey.ads.managers.BannerManager;
import com.smdey.ads.managers.ConsentManager;
import com.smdey.ads.managers.InterstitialManager;
import com.smdey.ads.managers.NativeManager;
import com.smdey.ads.managers.RewardedInterstitialManager;
import com.smdey.ads.managers.RewardedManager;

/**
 * Unified Facade for the Smdey Ads SDK.
 */
public final class AdsFacade {

    private static volatile AdsFacade instance;

    private final Context appContext;
    private final AdsConfig config;
    private final SdkGate sdkGate;
    private final ConsentManager consentManager;
    private final AppOpenManager appOpenManager;
    private final BannerManager bannerManager;
    private final InterstitialManager interstitialManager;
    private final RewardedManager rewardedManager;
    private final RewardedInterstitialManager rewardedInterstitialManager;
    private final NativeManager nativeManager;

    private volatile boolean adsRemoved = false;

    public static synchronized AdsFacade init(@NonNull Context context, @NonNull AdsConfig config) {
        if (instance == null) {
            instance = new AdsFacade(context, config);
        }
        return instance;
    }

    public static AdsFacade getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Smdey AdsFacade is not initialized. Call AdsFacade.init(context, config) first in Application.onCreate().");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public AdsFacade(@NonNull Context context, @NonNull AdsConfig config) {
        this.appContext = context.getApplicationContext();
        this.config = config;
        this.sdkGate = new SdkGate(appContext, config);
        this.consentManager = ConsentManager.getInstance(appContext, config);
        this.appOpenManager = new AppOpenManager(sdkGate);
        this.bannerManager = new BannerManager(sdkGate);
        this.interstitialManager = new InterstitialManager(sdkGate);
        this.rewardedManager = new RewardedManager(sdkGate);
        this.rewardedInterstitialManager = new RewardedInterstitialManager(sdkGate);
        this.nativeManager = new NativeManager(sdkGate);
        Log.i(SdkGate.TAG, "✅ AdsFacade - Ready.");
    }

    @NonNull
    public SdkGate sdk() {
        return sdkGate;
    }

    @NonNull
    public ConsentManager consent() {
        return consentManager;
    }

    @NonNull
    public AppOpenManager appOpen() {
        return appOpenManager;
    }

    @NonNull
    public BannerManager banner() {
        return bannerManager;
    }

    @NonNull
    public InterstitialManager interstitial() {
        return interstitialManager;
    }

    @NonNull
    public RewardedManager rewarded() {
        return rewardedManager;
    }

    @NonNull
    public RewardedInterstitialManager rewardedInterstitial() {
        return rewardedInterstitialManager;
    }

    @NonNull
    public NativeManager nativeAd() {
        return nativeManager;
    }

    @NonNull
    public NativeManager nativeAds() {
        return nativeManager;
    }

    @NonNull
    public AdsConfig getConfig() {
        return config;
    }

    public void setAdsRemoved(boolean adsRemoved) {
        this.adsRemoved = adsRemoved;
        this.bannerManager.setAdsRemoved(adsRemoved);
        this.interstitialManager.setAdsRemoved(adsRemoved);
        this.appOpenManager.setAdsRemoved(adsRemoved);
        this.rewardedInterstitialManager.setAdsRemoved(adsRemoved);
        this.nativeManager.setAdsRemoved(adsRemoved);
    }

    public boolean isAdsRemoved() {
        return adsRemoved;
    }

    public boolean canRequestAds() {
        return consentManager.canRequestAds();
    }

    public void markStartupStable(@NonNull Activity activity) {
        sdkGate.markStartupStable(activity);
        appOpenManager.scheduleFirstPreload(activity);
    }

    public void ensureConsentThenRun(@NonNull Activity activity, @NonNull Runnable action) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            return;
        }

        final boolean alreadyHasConsent = consentManager.canRequestAds();
        Log.d(SdkGate.TAG, "⏳ AdsFacade - Initial consent check: " + alreadyHasConsent);

        if (alreadyHasConsent) {
            // Fast path: User consented in a prior session, allow immediate ad requests
            sdkGate.notifyConsentResolved(activity);
            action.run();
        }

        // Always query Google UMP on launch to validate and synchronize the TCF TC string
        Log.i(SdkGate.TAG, "⏳ AdsFacade - Requesting UMP consent synchronization.");
        consentManager.gatherConsent(activity, () -> {
            if (!LifecycleGuard.isActivityValid(activity)) {
                return;
            }

            boolean allowed = consentManager.canRequestAds();
            Log.i(SdkGate.TAG, "✅ AdsFacade - Consent sync finished. Ads can request: " + allowed);

            if (allowed) {
                sdkGate.notifyConsentResolved(activity);
                if (!alreadyHasConsent) {
                    action.run();
                }
            } else if (alreadyHasConsent) {
                // Consent was revoked or expired during sync; stop active ads immediately
                setAdsRemoved(true);
            }
        });
    }

    public void showPrivacyOptions(@NonNull Activity activity, @Nullable ConsentCallback callback) {
        consentManager.showPrivacyOptions(activity, callback);
    }

    public void shutdown() {
        sdkGate.shutdown();
    }
}
