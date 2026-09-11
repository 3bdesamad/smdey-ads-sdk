package com.smdey.ads.sdk.callbacks;

import androidx.annotation.NonNull;

public interface OnRewardEarnedListener {
    void onUserEarnedReward(int amount, @NonNull String type);
}
