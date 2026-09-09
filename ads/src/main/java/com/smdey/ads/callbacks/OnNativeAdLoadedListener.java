package com.smdey.ads.callbacks;

import androidx.annotation.NonNull;

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;

/**
 * Listener invoked when a Native Ad is loaded or fails to load.
 */
public interface OnNativeAdLoadedListener {

    /**
     * Called when a native ad is successfully loaded.
     *
     * @param nativeAd the loaded native ad
     */
    void onAdLoaded(@NonNull NativeAd nativeAd);

    /**
     * Called when the native ad fails to load.
     */
    void onAdFailedToLoad();
}
