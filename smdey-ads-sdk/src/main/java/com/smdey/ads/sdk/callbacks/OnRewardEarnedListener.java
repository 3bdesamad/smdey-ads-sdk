package com.smdey.ads.sdk.callbacks;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

/**
 * Listener invoked when a user successfully watches a rewarded video ad and earns an in-game
 * or in-app reward item.
 * <p>
 * Guaranteed to be executed on the Android UI (Main) thread.
 */
@FunctionalInterface
public interface OnRewardEarnedListener {

    @MainThread
    void onUserEarnedReward(int amount, @NonNull String type);
}
