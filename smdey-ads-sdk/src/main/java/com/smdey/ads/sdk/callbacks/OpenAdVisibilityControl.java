package com.smdey.ads.sdk.callbacks;

public interface OpenAdVisibilityControl {
    void hideBannerAd();
    void showBannerAd();
    default boolean canShowOpenAd() {
        return true;
    }
}
