package com.smdey.ads.sdk.callbacks;

import android.app.Activity;
import android.app.Dialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Interface allowing host applications to plug in custom loading overlay dialogs
 * (e.g. dialog_loading_ad.xml) before interstitial ads are displayed.
 */
@FunctionalInterface
public interface AdLoadingOverlayProvider {

    @Nullable
    Dialog showLoading(@NonNull Activity activity);

    default void dismissLoading(@Nullable Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (Exception ignored) {
            }
        }
    }
}
