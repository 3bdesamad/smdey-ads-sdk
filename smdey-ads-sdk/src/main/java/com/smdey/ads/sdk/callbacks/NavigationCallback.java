package com.smdey.ads.sdk.callbacks;

import androidx.annotation.MainThread;

/**
 * Callback invoked to execute screen navigation after an ad has been displayed,
 * dismissed, or when ad frequency criteria allow immediate navigation.
 * <p>
 * Guaranteed to be executed on the Android UI (Main) thread.
 */
@FunctionalInterface
public interface NavigationCallback {

    @MainThread
    void navigate();
}
