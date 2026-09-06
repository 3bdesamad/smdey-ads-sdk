package com.smdey.ads.callbacks;

import androidx.annotation.NonNull;

/**
 * Listener invoked when a user completes a rewarded ad session and earns a reward.
 */
@FunctionalInterface
public interface OnUserEarnedRewardListener {

    /**
     * Called when user earned the reward.
     *
     * @param rewardItem the reward item details
     */
    void onUserEarnedReward(@NonNull RewardItem rewardItem);
}
