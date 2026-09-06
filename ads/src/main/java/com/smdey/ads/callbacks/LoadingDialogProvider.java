package com.smdey.ads.callbacks;

import android.app.Activity;
import android.app.Dialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Optional provider to supply a custom loading dialog before Interstitial ad display.
 */
public interface LoadingDialogProvider {
    @Nullable
    Dialog createLoadingDialog(@NonNull Activity activity);
}
