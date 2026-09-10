package com.smdey.ads.callbacks;

import android.app.Activity;
import android.app.Dialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Optional provider to supply a custom introductory opt-in/opt-out dialog
 * for Rewarded Interstitial ads.
 */
public interface IntroDialogProvider {
    @Nullable
    Dialog createIntroDialog(@NonNull Activity activity,
                             @NonNull String rewardDescription,
                             @NonNull Runnable onWatchSelected,
                             @NonNull Runnable onSkipSelected);
}
