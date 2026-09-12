package com.smdey.sample;



public final class AdConfig {

    private AdConfig() {}

    public static final boolean IS_DEBUG = BuildConfig.DEBUG;

    // Google Official Test Ad Units
    public static final String TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713";
    private static final String TEST_BANNER = "ca-app-pub-3940256099942544/6300978111";
    private static final String TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private static final String TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917";
    private static final String TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921";
    public static final String TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110";

    // Public Ad Units: automatically switches between Test IDs and Real Production IDs
    public static final String BANNER = IS_DEBUG ? TEST_BANNER : BuildConfig.Admob_Banner;
    public static final String INTERSTITIAL = IS_DEBUG ? TEST_INTERSTITIAL : BuildConfig.Admob_Interstitial;
    public static final String REWARDED = IS_DEBUG ? TEST_REWARDED : BuildConfig.Admob_RewardedAd;
    public static final String APP_OPEN = IS_DEBUG ? TEST_APP_OPEN : BuildConfig.Admob_OpenAd;
    public static final String NATIVE = IS_DEBUG ? TEST_NATIVE : BuildConfig.Admob_Native;
}
