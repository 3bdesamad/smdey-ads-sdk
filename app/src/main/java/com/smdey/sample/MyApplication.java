package com.smdey.sample;

import android.app.Application;

import com.smdey.ads.sdk.AdsConfig;
import com.smdey.ads.sdk.AdsSdk;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        AdsSdk.init(this, new AdsConfig.Builder(AdConfig.TEST_APP_ID)
                .setBannerId(AdConfig.BANNER)
                .setInterstitialId(AdConfig.INTERSTITIAL)
                .setRewardedId(AdConfig.REWARDED)
                .setAppOpenId(AdConfig.APP_OPEN)
                .setNativeId(AdConfig.NATIVE)
                .setDebug(AdConfig.IS_DEBUG)
                .setTag(Constants.TAG)
                .setInterstitialInterval(8) // Show interstitial every x clicks
                .excludeAppOpenActivities(LauncherActivity.class /*,ActivitySettings.class*/) // Exclude activities from App Open ads
                .setAppOpenCooldownMs(40000L) // 40s cooldown between App Open ads
                .setAppOpenPreloadDelayMs(3000L) // 3s delay after cold start before first preload
                .setLoadingOverlayTimeoutMs(4000L) // 4s max wait for interstitial loading overlay
                .setBannerRetryCooldownMs(15000L) // 15s retry cooldown after banner failure
                //.setTestDeviceId("69EE32AE3D1D93549B74FC6C246AA265") // Put your ID here
                .setLoadingOverlayProvider(Dialogs::showLoadingAd)
                .setAdsRemovedProvider(SharedPref.getInstance(this)::isAdsRemoved)
                .build()
        );
    }
}
