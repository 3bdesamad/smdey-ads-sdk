package com.smdey.ads.sdk.callbacks;

import androidx.annotation.MainThread;

/**
 * Callback invoked when the Google UMP consent gathering flow or privacy options form
 * has finished resolving.
 * <p>
 * Guaranteed to be executed on the Android UI (Main) thread.
 */
@FunctionalInterface
public interface ConsentCallback {

    @MainThread
    void onConsentCompleted();
}
