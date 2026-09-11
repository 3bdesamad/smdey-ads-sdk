package com.smdey.ads.sdk.core;

import android.app.Activity;

import androidx.annotation.Nullable;

public final class LifecycleGuard {

    private LifecycleGuard() {}

    public static boolean isActivityValid(@Nullable Activity activity) {
        if (activity == null) {
            return false;
        }
        return !activity.isFinishing() && !activity.isDestroyed();
    }

    public static boolean isSafeToUpdate(@Nullable Activity activity) {
        return isActivityValid(activity);
    }
}
