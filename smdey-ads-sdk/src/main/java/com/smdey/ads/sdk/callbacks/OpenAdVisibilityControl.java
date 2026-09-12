package com.smdey.ads.sdk.callbacks;

import androidx.annotation.MainThread;

/**
 * Interface implemented by Activities to control and coordinate the visibility of
 * App Open and Banner ads on specific screens (e.g. Splash, Onboarding, Paywall).
 * <p>
 * Methods are executed on the Android UI (Main) thread.
 */
public interface OpenAdVisibilityControl {

    @MainThread
    void hideBannerAd();

    @MainThread
    void showBannerAd();

    @MainThread
    default boolean canShowOpenAd() {
        return true;
    }
}
