package com.smdey.ads.sample;

import android.app.Application;

import com.smdey.ads.core.AdsConfig;
import com.smdey.ads.core.AdsFacade;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Build SDK Configuration
        // Note: Providing an Ad Unit ID automatically enables that format.
        // If an ID is omitted (e.g. you don't use Rewarded or Native), that format is automatically disabled.
        AdsConfig config = new AdsConfig.Builder()
                .setBannerId("ca-app-pub-3940256099942544/6300978111")
                .setInterstitialId("ca-app-pub-3940256099942544/1033173712")
                .setAppOpenId("ca-app-pub-3940256099942544/9257395921")
                .setRewardedId("ca-app-pub-3940256099942544/5224354917")
                .setRewardedInterstitialId("ca-app-pub-3940256099942544/5354046379")
                .setNativeId("ca-app-pub-3940256099942544/2247696110")
                .setCollapsibleBannerEnabled(true)
                .setCollapsibleGravity("bottom")
                .setGracePeriodEnabled(false)       // true = activate delay, false = show ads immediately
                .setGracePeriodDays(3)             // Number of days to mute ads for new users
                .setDebugMode(BuildConfig.DEBUG)   // When true, uses Google AdMob test IDs automatically
                .setInterstitialFrequency(2)       // Show interstitial every 2 clicks
                .setInterstitialCooldownMs(30000)  // Minimum 30s between interstitials
                .setAppOpenCooldownMs(40000)       // Minimum 40s between app open ads
                .build();

        // 2. Initialize AdsFacade (Grace Period is automatically evaluated and handled internally)
        AdsFacade.init(this, config);
    }
}
