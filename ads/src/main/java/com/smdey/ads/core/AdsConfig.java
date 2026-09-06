package com.smdey.ads.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Immutable configuration for the Ads SDK.
 * Use {@link AdsConfig.Builder} to construct instances.
 */
public final class AdsConfig {

    // Default Google AdMob Test Ad Units
    public static final String TEST_BANNER = "ca-app-pub-3940256099942544/6300978111";
    public static final String TEST_COLLAPSIBLE_BANNER = "ca-app-pub-3940256099942544/2014213617";
    public static final String TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    public static final String TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921";
    public static final String TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917";
    public static final String TEST_REWARDED_INTERSTITIAL = "ca-app-pub-3940256099942544/5354046379";
    public static final String TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110";

    private final String bannerAdUnitId;
    private final String interstitialAdUnitId;
    private final String appOpenAdUnitId;
    private final String rewardedAdUnitId;
    private final String rewardedInterstitialAdUnitId;
    private final String nativeAdUnitId;
    private final boolean debugMode;
    private final boolean bannerEnabled;
    private final boolean interstitialEnabled;
    private final boolean appOpenEnabled;
    private final boolean rewardedEnabled;
    private final boolean rewardedInterstitialEnabled;
    private final boolean nativeEnabled;
    private final boolean collapsibleBannerEnabled;
    private final String collapsibleGravity;
    private final int interstitialFrequency;
    private final long interstitialCooldownMs;
    private final long appOpenCooldownMs;
    private final long loadingTimeoutMs;
    private final long safeStartupDelayMs;
    private final float appVolume;
    private final String testDeviceHashedId;

    private AdsConfig(Builder builder) {
        this.bannerAdUnitId = builder.bannerAdUnitId;
        this.interstitialAdUnitId = builder.interstitialAdUnitId;
        this.appOpenAdUnitId = builder.appOpenAdUnitId;
        this.rewardedAdUnitId = builder.rewardedAdUnitId;
        this.rewardedInterstitialAdUnitId = builder.rewardedInterstitialAdUnitId;
        this.nativeAdUnitId = builder.nativeAdUnitId;
        this.debugMode = builder.debugMode;
        this.bannerEnabled = builder.bannerEnabled;
        this.interstitialEnabled = builder.interstitialEnabled;
        this.appOpenEnabled = builder.appOpenEnabled;
        this.rewardedEnabled = builder.rewardedEnabled;
        this.rewardedInterstitialEnabled = builder.rewardedInterstitialEnabled;
        this.nativeEnabled = builder.nativeEnabled;
        this.collapsibleBannerEnabled = builder.collapsibleBannerEnabled;
        this.collapsibleGravity = builder.collapsibleGravity != null ? builder.collapsibleGravity : "bottom";
        this.interstitialFrequency = builder.interstitialFrequency;
        this.interstitialCooldownMs = builder.interstitialCooldownMs;
        this.appOpenCooldownMs = builder.appOpenCooldownMs;
        this.loadingTimeoutMs = builder.loadingTimeoutMs;
        this.safeStartupDelayMs = builder.safeStartupDelayMs;
        this.appVolume = builder.appVolume;
        this.testDeviceHashedId = builder.testDeviceHashedId;
    }

    public String getBannerAdUnitId() {
        return getBannerAdUnitId(false);
    }

    public String getBannerAdUnitId(boolean isCollapsible) {
        if (debugMode) {
            return isCollapsible ? TEST_COLLAPSIBLE_BANNER : TEST_BANNER;
        }
        return bannerAdUnitId != null ? bannerAdUnitId : TEST_BANNER;
    }

    public String getInterstitialAdUnitId() {
        return debugMode ? TEST_INTERSTITIAL : (interstitialAdUnitId != null ? interstitialAdUnitId : TEST_INTERSTITIAL);
    }

    public String getAppOpenAdUnitId() {
        return debugMode ? TEST_APP_OPEN : (appOpenAdUnitId != null ? appOpenAdUnitId : TEST_APP_OPEN);
    }

    public String getRewardedAdUnitId() {
        return debugMode ? TEST_REWARDED : (rewardedAdUnitId != null ? rewardedAdUnitId : TEST_REWARDED);
    }

    public String getRewardedInterstitialAdUnitId() {
        return debugMode ? TEST_REWARDED_INTERSTITIAL : (rewardedInterstitialAdUnitId != null ? rewardedInterstitialAdUnitId : TEST_REWARDED_INTERSTITIAL);
    }

    public String getNativeAdUnitId() {
        return debugMode ? TEST_NATIVE : (nativeAdUnitId != null ? nativeAdUnitId : TEST_NATIVE);
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public boolean isBannerEnabled() {
        return bannerEnabled;
    }

    public boolean isInterstitialEnabled() {
        return interstitialEnabled;
    }

    public boolean isAppOpenEnabled() {
        return appOpenEnabled;
    }

    public boolean isRewardedEnabled() {
        return rewardedEnabled;
    }

    public boolean isRewardedInterstitialEnabled() {
        return rewardedInterstitialEnabled;
    }

    public boolean isNativeEnabled() {
        return nativeEnabled;
    }

    public boolean isCollapsibleBannerEnabled() {
        return collapsibleBannerEnabled;
    }

    @NonNull
    public String getCollapsibleGravity() {
        return collapsibleGravity;
    }

    public int getInterstitialFrequency() {
        return interstitialFrequency;
    }

    public long getInterstitialCooldownMs() {
        return interstitialCooldownMs;
    }

    public long getAppOpenCooldownMs() {
        return appOpenCooldownMs;
    }

    public long getLoadingTimeoutMs() {
        return loadingTimeoutMs;
    }

    public long getSafeStartupDelayMs() {
        return safeStartupDelayMs;
    }

    public float getAppVolume() {
        return appVolume;
    }

    @Nullable
    public String getTestDeviceHashedId() {
        return testDeviceHashedId;
    }

    public static final class Builder {
        private String bannerAdUnitId;
        private String interstitialAdUnitId;
        private String appOpenAdUnitId;
        private String rewardedAdUnitId;
        private String rewardedInterstitialAdUnitId;
        private String nativeAdUnitId;
        private boolean debugMode = false;
        private boolean bannerEnabled = true;
        private boolean interstitialEnabled = true;
        private boolean appOpenEnabled = true;
        private boolean rewardedEnabled = true;
        private boolean rewardedInterstitialEnabled = true;
        private boolean nativeEnabled = true;
        private boolean collapsibleBannerEnabled = false;
        private String collapsibleGravity = "bottom";
        private int interstitialFrequency = 3;
        private long interstitialCooldownMs = 30000L;
        private long appOpenCooldownMs = 40000L;
        private long loadingTimeoutMs = 5000L;
        private long safeStartupDelayMs = 1200L;
        private float appVolume = 0.5f;
        private String testDeviceHashedId;

        public Builder setBannerId(@Nullable String bannerAdUnitId) {
            this.bannerAdUnitId = bannerAdUnitId;
            return this;
        }

        public Builder setInterstitialId(@Nullable String interstitialAdUnitId) {
            this.interstitialAdUnitId = interstitialAdUnitId;
            return this;
        }

        public Builder setAppOpenId(@Nullable String appOpenAdUnitId) {
            this.appOpenAdUnitId = appOpenAdUnitId;
            return this;
        }

        public Builder setRewardedId(@Nullable String rewardedAdUnitId) {
            this.rewardedAdUnitId = rewardedAdUnitId;
            return this;
        }

        public Builder setRewardedInterstitialId(@Nullable String rewardedInterstitialAdUnitId) {
            this.rewardedInterstitialAdUnitId = rewardedInterstitialAdUnitId;
            return this;
        }

        public Builder setNativeId(@Nullable String nativeAdUnitId) {
            this.nativeAdUnitId = nativeAdUnitId;
            return this;
        }

        public Builder setDebugMode(boolean debugMode) {
            this.debugMode = debugMode;
            return this;
        }

        public Builder setBannerEnabled(boolean bannerEnabled) {
            this.bannerEnabled = bannerEnabled;
            return this;
        }

        public Builder setInterstitialEnabled(boolean interstitialEnabled) {
            this.interstitialEnabled = interstitialEnabled;
            return this;
        }

        public Builder setAppOpenEnabled(boolean appOpenEnabled) {
            this.appOpenEnabled = appOpenEnabled;
            return this;
        }

        public Builder setRewardedEnabled(boolean rewardedEnabled) {
            this.rewardedEnabled = rewardedEnabled;
            return this;
        }

        public Builder setRewardedInterstitialEnabled(boolean rewardedInterstitialEnabled) {
            this.rewardedInterstitialEnabled = rewardedInterstitialEnabled;
            return this;
        }

        public Builder setNativeEnabled(boolean nativeEnabled) {
            this.nativeEnabled = nativeEnabled;
            return this;
        }

        public Builder setCollapsibleBannerEnabled(boolean collapsibleBannerEnabled) {
            this.collapsibleBannerEnabled = collapsibleBannerEnabled;
            return this;
        }

        public Builder setCollapsibleGravity(@NonNull String collapsibleGravity) {
            this.collapsibleGravity = collapsibleGravity;
            return this;
        }

        public Builder setInterstitialFrequency(int interstitialFrequency) {
            this.interstitialFrequency = interstitialFrequency;
            return this;
        }

        public Builder setInterstitialCooldownMs(long interstitialCooldownMs) {
            this.interstitialCooldownMs = interstitialCooldownMs;
            return this;
        }

        public Builder setAppOpenCooldownMs(long appOpenCooldownMs) {
            this.appOpenCooldownMs = appOpenCooldownMs;
            return this;
        }

        public Builder setLoadingTimeoutMs(long loadingTimeoutMs) {
            this.loadingTimeoutMs = loadingTimeoutMs;
            return this;
        }

        public Builder setSafeStartupDelayMs(long safeStartupDelayMs) {
            this.safeStartupDelayMs = safeStartupDelayMs;
            return this;
        }

        public Builder setAppVolume(float appVolume) {
            this.appVolume = appVolume;
            return this;
        }

        public Builder setTestDeviceHashedId(@Nullable String testDeviceHashedId) {
            this.testDeviceHashedId = testDeviceHashedId;
            return this;
        }

        @NonNull
        public AdsConfig build() {
            return new AdsConfig(this);
        }
    }
}
