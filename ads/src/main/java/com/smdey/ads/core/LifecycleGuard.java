package com.smdey.ads.core;

import android.app.Activity;

import androidx.annotation.Nullable;

/**
 * Validates Activity lifecycle state to prevent window leaks and crashes.
 */
public final class LifecycleGuard {

    private LifecycleGuard() {}

    public static boolean isActivityValid(@Nullable Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }
}
