package com.smdey.ads.managers;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest;
import com.smdey.ads.callbacks.OnNativeAdLoadedListener;
import com.smdey.ads.core.AdsConfig;
import com.smdey.ads.core.AppExecutors;
import com.smdey.ads.core.LifecycleGuard;
import com.smdey.ads.core.SdkGate;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages Google Native Ads loading, preloading cache, and lifecycle destruction.
 */
public final class NativeManager {

    private final SdkGate sdkGate;
    private final AdsConfig config;
    private final AtomicReference<NativeAd> cachedNativeAd = new AtomicReference<>(null);

    private volatile boolean adsRemoved = false;

    public NativeManager(@NonNull SdkGate sdkGate) {
        this.sdkGate = sdkGate;
        this.config = sdkGate.getConfig();
    }

    public void setAdsRemoved(boolean adsRemoved) {
        this.adsRemoved = adsRemoved;
        if (adsRemoved) {
            destroyCachedAd();
        }
    }

    public boolean isAdsRemoved() {
        return adsRemoved;
    }

    public boolean isAdReady() {
        return !adsRemoved && cachedNativeAd.get() != null;
    }

    @Nullable
    public NativeAd getPreloadedAd() {
        if (adsRemoved) {
            destroyCachedAd();
            return null;
        }
        return cachedNativeAd.getAndSet(null);
    }

    public void destroyCachedAd() {
        NativeAd ad = cachedNativeAd.getAndSet(null);
        if (ad != null) {
            try {
                ad.destroy();
                Log.d(SdkGate.TAG, "♻️ NativeManager - Cached native ad destroyed.");
            } catch (Exception e) {
                Log.w(SdkGate.TAG, "⚠️ NativeManager - Error destroying cached native ad", e);
            }
        }
    }

    public void destroyAd(@Nullable NativeAd nativeAd) {
        if (nativeAd != null) {
            try {
                nativeAd.destroy();
                Log.d(SdkGate.TAG, "♻️ NativeManager - Native ad destroyed.");
            } catch (Exception e) {
                Log.w(SdkGate.TAG, "⚠️ NativeManager - Error destroying native ad", e);
            }
        }
    }

    public void preloadAd(@NonNull Context context) {
        if (adsRemoved || !config.isNativeEnabled() || config.getNativeAdUnitId() == null) {
            return;
        }

        if (cachedNativeAd.get() != null) {
            return;
        }

        loadAd(context, null);
    }

    public void loadAd(@NonNull Context context, @Nullable OnNativeAdLoadedListener listener) {
        if (context instanceof Activity && !LifecycleGuard.isActivityValid((Activity) context)) {
            if (listener != null) listener.onAdFailedToLoad();
            return;
        }

        if (adsRemoved || !config.isNativeEnabled() || config.getNativeAdUnitId() == null) {
            Log.w(SdkGate.TAG, "⚠️ NativeManager - Feature disabled or no ad unit ID configured.");
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

        // Check if a preloaded ad is ready in cache
        NativeAd cached = cachedNativeAd.getAndSet(null);
        if (cached != null) {
            if (listener != null) {
                listener.onAdLoaded(cached);
            }
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

        NativeAdRequest nativeAdRequest = new NativeAdRequest.Builder(
                config.getNativeAdUnitId(),
                Collections.singletonList(NativeAd.NativeAdType.NATIVE)
        )
                .setAdChoicesPlacement(AdChoicesPlacement.TOP_RIGHT)
                .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                .build();

        Log.i(SdkGate.TAG, "⏳ NativeManager - Loading native ad...");
        NativeAdLoader.load(nativeAdRequest, new NativeAdLoaderCallback() {
            @Override
            public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    Log.i(SdkGate.TAG, "✅ NativeManager - Native ad loaded successfully.");
                    if (listener != null) {
                        listener.onAdLoaded(nativeAd);
                    } else {
                        // Cache for later use
                        destroyCachedAd();
                        cachedNativeAd.set(nativeAd);
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    Log.w(SdkGate.TAG, "❌ NativeManager - Failed to load native ad: " + loadAdError.getMessage());
                    if (listener != null) {
                        listener.onAdFailedToLoad();
                    }
                });
            }
        });
    }
}
