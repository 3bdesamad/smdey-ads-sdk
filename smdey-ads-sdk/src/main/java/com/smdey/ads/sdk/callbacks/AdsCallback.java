package com.smdey.ads.sdk.callbacks;

import androidx.annotation.MainThread;

/**
 * General callback invoked when an ad operation completes (e.g. interstitial dismissed,
 * skipped, or failed to show).
 * <p>
 * Guaranteed to be executed on the Android UI (Main) thread.
 */
@FunctionalInterface
public interface AdsCallback {

    @MainThread
    void onComplete();
}
