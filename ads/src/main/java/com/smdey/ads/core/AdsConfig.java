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

    // Grace Period Defaults
    public static final boolean ENABLE_GRACE_PERIOD = true; // true = activate delay, false = show ads immediately
    public static final int GRACE_PERIOD_DAYS = 3;          // Number of days (e.g. 3, 5, 7)

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
    private final boolean gracePeriodEnabled;
    private final int gracePeriodDays;
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
        // Smart Auto-Enable: If explicitly toggled, use that. Otherwise, auto-enable only if ID is configured.
        this.bannerEnabled = builder.bannerEnabled != null
                ? builder.bannerEnabled
                : (builder.bannerAdUnitId != null && !builder.bannerAdUnitId.trim().isEmpty());

        this.interstitialEnabled = builder.interstitialEnabled != null
                ? builder.interstitialEnabled
                : (builder.interstitialAdUnitId != null && !builder.interstitialAdUnitId.trim().isEmpty());

        this.appOpenEnabled = builder.appOpenEnabled != null
                ? builder.appOpenEnabled
                : (builder.appOpenAdUnitId != null && !builder.appOpenAdUnitId.trim().isEmpty());

        this.rewardedEnabled = builder.rewardedEnabled != null
                ? builder.rewardedEnabled
                : (builder.rewardedAdUnitId != null && !builder.rewardedAdUnitId.trim().isEmpty());

        this.rewardedInterstitialEnabled = builder.rewardedInterstitialEnabled != null
                ? builder.rewardedInterstitialEnabled
                : (builder.rewardedInterstitialAdUnitId != null && !builder.rewardedInterstitialAdUnitId.trim().isEmpty());

        this.nativeEnabled = builder.nativeEnabled != null
                ? builder.nativeEnabled
                : (builder.nativeAdUnitId != null && !builder.nativeAdUnitId.trim().isEmpty());

        this.collapsibleBannerEnabled = builder.collapsibleBannerEnabled;
        this.collapsibleGravity = builder.collapsibleGravity != null ? builder.collapsibleGravity : "bottom";
        this.gracePeriodEnabled = builder.gracePeriodEnabled;
        this.gracePeriodDays = builder.gracePeriodDays;
        this.interstitialFrequency = builder.interstitialFrequency;
        this.interstitialCooldownMs = builder.interstitialCooldownMs;
        this.appOpenCooldownMs = builder.appOpenCooldownMs;
        this.loadingTimeoutMs = builder.loadingTimeoutMs;
        this.safeStartupDelayMs = builder.safeStartupDelayMs;
        this.appVolume = builder.appVolume;
        this.testDeviceHashedId = builder.testDeviceHashedId;
    }

    @Nullable
    public String getBannerAdUnitId() {
        return getBannerAdUnitId(false);
    }

    @Nullable
    public String getBannerAdUnitId(boolean isCollapsible) {
        if (debugMode) {
            return isCollapsible ? TEST_COLLAPSIBLE_BANNER : TEST_BANNER;
        }
        return bannerAdUnitId;
    }

    @Nullable
    public String getInterstitialAdUnitId() {
        return debugMode ? TEST_INTERSTITIAL : interstitialAdUnitId;
    }

    @Nullable
    public String getAppOpenAdUnitId() {
        return debugMode ? TEST_APP_OPEN : appOpenAdUnitId;
    }

    @Nullable
    public String getRewardedAdUnitId() {
        return debugMode ? TEST_REWARDED : rewardedAdUnitId;
    }

    @Nullable
    public String getRewardedInterstitialAdUnitId() {
        return debugMode ? TEST_REWARDED_INTERSTITIAL : rewardedInterstitialAdUnitId;
    }

    @Nullable
    public String getNativeAdUnitId() {
        return debugMode ? TEST_NATIVE : nativeAdUnitId;
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

    public boolean isGracePeriodEnabled() {
        return gracePeriodEnabled;
    }

    public int getGracePeriodDays() {
        return gracePeriodDays;
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
        private Boolean bannerEnabled = null;
        private Boolean interstitialEnabled = null;
        private Boolean appOpenEnabled = null;
        private Boolean rewardedEnabled = null;
        private Boolean rewardedInterstitialEnabled = null;
        private Boolean nativeEnabled = null;
        private boolean collapsibleBannerEnabled = false;
        private String collapsibleGravity = "bottom";
        private boolean gracePeriodEnabled = ENABLE_GRACE_PERIOD;
        private int gracePeriodDays = GRACE_PERIOD_DAYS;
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

        public Builder setGracePeriodEnabled(boolean gracePeriodEnabled) {
            this.gracePeriodEnabled = gracePeriodEnabled;
            return this;
        }

        public Builder setGracePeriodDays(int gracePeriodDays) {
            this.gracePeriodDays = gracePeriodDays;
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
