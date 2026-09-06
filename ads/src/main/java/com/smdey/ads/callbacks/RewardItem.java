package com.smdey.ads.callbacks;

import androidx.annotation.NonNull;

/**
 * Encapsulates the reward data earned by a user when watching a rewarded ad.
 */
public interface RewardItem {

    /**
     * Gets the reward amount.
     *
     * @return reward amount
     */
    int getAmount();

    /**
     * Gets the reward item type / currency name.
     *
     * @return reward type description
     */
    @NonNull
    String getType();
}
