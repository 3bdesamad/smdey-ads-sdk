package com.smdey.ads.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.smdey.ads.sdk.appopen.AppOpenManager;
import com.smdey.ads.sdk.banner.BannerManager;
import com.smdey.ads.sdk.callbacks.OpenAdVisibilityControl;
import com.smdey.ads.sdk.consent.ConsentManager;
import com.smdey.ads.sdk.core.AppExecutors;
import com.smdey.ads.sdk.core.FullScreenAdCoordinator;
import com.smdey.ads.sdk.core.LifecycleGuard;
import com.smdey.ads.sdk.core.SdkGate;
import com.smdey.ads.sdk.interstitial.InterstitialManager;
import com.smdey.ads.sdk.rewarded.RewardedManager;

import java.lang.ref.WeakReference;

public final class AdsSdk {

    private static volatile AdsSdk instance;

    private final Context appContext;
    private final AdsConfig config;
    private final ConsentManager consentManager;
    private final SdkGate sdkGate;
    private final FullScreenAdCoordinator fullScreenCoordinator;
    private final BannerManager bannerManager;
    private final InterstitialManager interstitialManager;
    private final RewardedManager rewardedManager;
    private final AppOpenManager appOpenManager;
    private WeakReference<Activity> currentActivityRef;
    private boolean startupStableDone = false;

    private AdsSdk(@NonNull Context context, @NonNull AdsConfig config) {
        this.appContext = context.getApplicationContext();
        this.config = config;
        this.consentManager = new ConsentManager(appContext, config);
        this.sdkGate = new SdkGate(appContext, config, consentManager);
        this.fullScreenCoordinator = new FullScreenAdCoordinator();
        this.bannerManager = new BannerManager(config, sdkGate, consentManager);
        this.interstitialManager = new InterstitialManager(config, sdkGate, consentManager, fullScreenCoordinator);
        this.rewardedManager = new RewardedManager(config, sdkGate, consentManager, fullScreenCoordinator);
        this.appOpenManager = new AppOpenManager(config, sdkGate, consentManager, fullScreenCoordinator);

        if (context instanceof Application) {
            registerLifecycleCallbacks((Application) context);
            hookProcessLifecycle();
        }

        Log.i(config.getTag(), "✅ AdsSdk - Initialized (v1.0.0 Next-Gen).");
    }

    public static synchronized void init(@NonNull Context context, @NonNull AdsConfig config) {
        if (instance == null) {
            instance = new AdsSdk(context, config);
        }
    }

    @NonNull
    public static AdsSdk getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AdsSdk is not initialized. Call AdsSdk.init(context, config) first.");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    @NonNull
    public AdsConfig getConfig() {
        return config;
    }

    @NonNull
    public SdkGate sdk() {
        return sdkGate;
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
    public AppOpenManager appOpen() {
        return appOpenManager;
    }

    @NonNull
    public ConsentManager consent() {
        return consentManager;
    }

    @NonNull
    public FullScreenAdCoordinator fullScreenCoordinator() {
        return fullScreenCoordinator;
    }

    @Nullable
    public Activity getCurrentActivity() {
        return currentActivityRef != null ? currentActivityRef.get() : null;
    }

    public void markStartupStable(@NonNull Activity activity) {
        sdkGate.markStartupStable(activity);
    }

    private void triggerStartupConsent(@NonNull Activity activity) {
        consentManager.gatherConsent(activity, () -> {
            if (consentManager.canRequestAds()) {
                sdkGate.notifyConsentResolved(activity);
            }
        });
    }

    private void hookProcessLifecycle() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_START) {
                    AppExecutors.getInstance().mainThread().execute(() -> {
                        Activity current = getCurrentActivity();
                        if (current != null && LifecycleGuard.isActivityValid(current)) {
                            appOpenManager.onAppForegrounded(current);
                        }
                    });
                } else if (event == Lifecycle.Event.ON_STOP) {
                    appOpenManager.onAppBackgrounded(appContext);
                }
            }
        });
    }

    private void registerLifecycleCallbacks(@NonNull Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                currentActivityRef = new WeakReference<>(activity);
                if (activity instanceof OpenAdVisibilityControl) {
                    appOpenManager.setAdVisibilityControl((OpenAdVisibilityControl) activity);
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                currentActivityRef = new WeakReference<>(activity);
                if (!startupStableDone && LifecycleGuard.isActivityValid(activity)) {
                    if (activity instanceof OpenAdVisibilityControl && !((OpenAdVisibilityControl) activity).canShowOpenAd()) {
                        return;
                    }
                    startupStableDone = true;
                    markStartupStable(activity);
                    triggerStartupConsent(activity);
                    appOpenManager.scheduleFirstPreloadIfAllowed(activity);
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                Activity current = getCurrentActivity();
                if (current == activity) {
                    appOpenManager.setAdVisibilityControl(null);
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                bannerManager.onActivityDestroyed(activity);
                Activity current = getCurrentActivity();
                if (current == activity && currentActivityRef != null) {
                    currentActivityRef.clear();
                }
            }
        });
    }
}
