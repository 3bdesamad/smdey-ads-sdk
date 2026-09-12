package com.smdey.ads.sdk.callbacks;

import androidx.annotation.AnyThread;

/**
 * Functional interface allowing the host app to provide real-time ad removal status
 * (e.g. user purchased VIP / In-App Purchase to remove ads).
 * <p>
 * Thread-safe: can be evaluated from any thread.
 */
@FunctionalInterface
public interface AdsRemovedProvider {

    @AnyThread
    boolean isAdsRemoved();
}
