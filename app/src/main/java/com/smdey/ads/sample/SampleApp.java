package com.smdey.ads.sample;

import android.app.Application;

import com.smdey.ads.core.AdsConfig;
import com.smdey.ads.core.AdsFacade;

public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Build SDK Configuration
        AdsConfig config = new AdsConfig.Builder()
                .setDebugMode(true) // Uses Google AdMob test IDs automatically
                .setBannerEnabled(true)
                .setInterstitialEnabled(true)
                .setAppOpenEnabled(true)
                .setRewardedEnabled(true)
                .setRewardedInterstitialEnabled(true)
                .setNativeEnabled(true)
                .setCollapsibleBannerEnabled(true)
                .setInterstitialFrequency(2)
                .build();

        // 2. Initialize AdsFacade
        AdsFacade.init(this, config);

        // 3. Apply Grace Period decision to SDK
        boolean inGracePeriod = GracePeriodManager.isGracePeriodActive(this);
        AdsFacade.getInstance().setAdsRemoved(inGracePeriod);
    }
}
