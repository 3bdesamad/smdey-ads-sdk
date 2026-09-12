package com.smdey.ads.sdk.callbacks;

import android.app.Activity;
import android.app.Dialog;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Interface allowing host applications to plug in custom loading overlay dialogs
 * (e.g. dialog_loading_ad.xml) before interstitial ads are displayed.
 * <p>
 * Methods are guaranteed to execute on the Android UI (Main) thread.
 */
@FunctionalInterface
public interface AdLoadingOverlayProvider {

    @MainThread
    @Nullable
    Dialog showLoading(@NonNull Activity activity);

    @MainThread
    default void dismissLoading(@Nullable Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (Exception ignored) {
            }
        }
    }
}
