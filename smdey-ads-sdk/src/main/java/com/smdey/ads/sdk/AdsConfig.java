package com.smdey.ads.sdk;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.smdey.ads.sdk.callbacks.AdLoadingOverlayProvider;
import com.smdey.ads.sdk.callbacks.AdsRemovedProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class AdsConfig {

    private final String appId;
    private final String bannerId;
    private final String interstitialId;
    private final String rewardedId;
    private final String appOpenId;
    private final String nativeId;
    private final AdsRemovedProvider adsRemovedProvider;
    private final AdLoadingOverlayProvider loadingOverlayProvider;
    private final String testDeviceId;
    private final boolean isDebug;
    private final boolean bannerEnabled;
    private final boolean interstitialEnabled;
    private final boolean rewardedEnabled;
    private final boolean appOpenEnabled;
    private final boolean nativeEnabled;
    private final int interstitialFrequency;
    private final String tag;
    private final Set<Class<? extends Activity>> excludedAppOpenActivities;
    private final long appOpenCooldownMs;
    private final long appOpenPreloadDelayMs;
    private final long loadingOverlayTimeoutMs;
    private final long bannerRetryCooldownMs;

    private AdsConfig(Builder builder) {
        this.appId = builder.appId;
        this.bannerId = builder.bannerId;
        this.interstitialId = builder.interstitialId;
        this.rewardedId = builder.rewardedId;
        this.appOpenId = builder.appOpenId;
        this.nativeId = builder.nativeId;
        this.testDeviceId = builder.testDeviceId;
        this.adsRemovedProvider = builder.adsRemovedProvider;
        this.loadingOverlayProvider = builder.loadingOverlayProvider;
        this.isDebug = builder.isDebug;
        this.bannerEnabled = builder.bannerEnabled;
        this.interstitialEnabled = builder.interstitialEnabled;
        this.rewardedEnabled = builder.rewardedEnabled;
        this.appOpenEnabled = builder.appOpenEnabled;
        this.nativeEnabled = builder.nativeEnabled;
        this.interstitialFrequency = builder.interstitialFrequency;
        this.tag = builder.tag;
        this.excludedAppOpenActivities = Collections.unmodifiableSet(new HashSet<>(builder.excludedAppOpenActivities));
        this.appOpenCooldownMs = builder.appOpenCooldownMs;
        this.appOpenPreloadDelayMs = builder.appOpenPreloadDelayMs;
        this.loadingOverlayTimeoutMs = builder.loadingOverlayTimeoutMs;
        this.bannerRetryCooldownMs = builder.bannerRetryCooldownMs;
    }

    @NonNull
    public String getAppId() {
        return appId != null ? appId : "";
    }

    @Nullable
    public String getBannerId() {
        return bannerId;
    }

    @Nullable
    public String getInterstitialId() {
        return interstitialId;
    }

    @Nullable
    public String getRewardedId() {
        return rewardedId;
    }

    @Nullable
    public String getAppOpenId() {
        return appOpenId;
    }

    @Nullable
    public String getNativeId() {
        return nativeId;
    }

    @Nullable
    public AdsRemovedProvider getAdsRemovedProvider() {
        return adsRemovedProvider;
    }

    public boolean isAdsRemoved() {
        return adsRemovedProvider != null && adsRemovedProvider.isAdsRemoved();
    }

    @Nullable
    public AdLoadingOverlayProvider getLoadingOverlayProvider() {
        return loadingOverlayProvider;
    }

    @Nullable
    public String getTestDeviceId() {
        return testDeviceId;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public boolean isBannerEnabled() {
        return bannerEnabled;
    }

    public boolean isInterstitialEnabled() {
        return interstitialEnabled;
    }

    public boolean isRewardedEnabled() {
        return rewardedEnabled;
    }

    public boolean isAppOpenEnabled() {
        return appOpenEnabled;
    }

    public boolean isNativeEnabled() {
        return nativeEnabled;
    }

    public int getInterstitialFrequency() {
        return interstitialFrequency;
    }

    public boolean isActivityExcludedFromAppOpen(@Nullable Class<?> activityClass) {
        return activityClass != null && excludedAppOpenActivities != null && excludedAppOpenActivities.contains(activityClass);
    }

    public long getAppOpenCooldownMs() {
        return appOpenCooldownMs;
    }

    public long getAppOpenPreloadDelayMs() {
        return appOpenPreloadDelayMs;
    }

    public long getLoadingOverlayTimeoutMs() {
        return loadingOverlayTimeoutMs;
    }

    public long getBannerRetryCooldownMs() {
        return bannerRetryCooldownMs;
    }

    @NonNull
    public String getTag() {
        return tag;
    }

    public static final class Builder {
        private String appId;
        private String bannerId;
        private String interstitialId;
        private String rewardedId;
        private String appOpenId;
        private String nativeId;
        private String testDeviceId;
        private AdsRemovedProvider adsRemovedProvider;
        private AdLoadingOverlayProvider loadingOverlayProvider;
        private boolean isDebug = false;
        private boolean bannerEnabled = true;
        private boolean interstitialEnabled = true;
        private boolean rewardedEnabled = true;
        private boolean appOpenEnabled = true;
        private boolean nativeEnabled = true;
        private int interstitialFrequency = 3;
        private String tag = "SMDEY_ADS";
        private final Set<Class<? extends Activity>> excludedAppOpenActivities = new HashSet<>();
        private long appOpenCooldownMs = 15000L;
        private long appOpenPreloadDelayMs = 3000L;
        private long loadingOverlayTimeoutMs = 4000L;
        private long bannerRetryCooldownMs = 15000L;

        public Builder(@NonNull String appId) {
            this.appId = appId;
        }

        public Builder setBannerId(String bannerId) {
            this.bannerId = bannerId;
            return this;
        }

        public Builder setInterstitialId(String interstitialId) {
            this.interstitialId = interstitialId;
            return this;
        }

        public Builder setRewardedId(String rewardedId) {
            this.rewardedId = rewardedId;
            return this;
        }

        public Builder setAppOpenId(String appOpenId) {
            this.appOpenId = appOpenId;
            return this;
        }

        public Builder setNativeId(String nativeId) {
            this.nativeId = nativeId;
            return this;
        }

        public Builder setNativeEnabled(boolean enabled) {
            this.nativeEnabled = enabled;
            return this;
        }

        public Builder setTestDeviceId(String testDeviceId) {
            this.testDeviceId = testDeviceId;
            return this;
        }

        public Builder setAdsRemovedProvider(AdsRemovedProvider provider) {
            this.adsRemovedProvider = provider;
            return this;
        }

        public Builder setLoadingOverlayProvider(AdLoadingOverlayProvider provider) {
            this.loadingOverlayProvider = provider;
            return this;
        }

        public Builder setDebug(boolean debug) {
            isDebug = debug;
            return this;
        }

        public Builder setBannerEnabled(boolean enabled) {
            bannerEnabled = enabled;
            return this;
        }

        public Builder setInterstitialEnabled(boolean enabled) {
            interstitialEnabled = enabled;
            return this;
        }

        public Builder setRewardedEnabled(boolean enabled) {
            rewardedEnabled = enabled;
            return this;
        }

        public Builder setAppOpenEnabled(boolean enabled) {
            appOpenEnabled = enabled;
            return this;
        }

        public Builder setInterstitialFrequency(int frequency) {
            interstitialFrequency = Math.max(1, frequency);
            return this;
        }

        public Builder setInterstitialInterval(int interval) {
            return setInterstitialFrequency(interval);
        }

        public Builder setTag(String tag) {
            if (tag != null && !tag.isEmpty()) {
                this.tag = tag;
            }
            return this;
        }

        @SafeVarargs
        public final Builder excludeAppOpenActivities(@NonNull Class<? extends Activity>... activities) {
            if (activities != null) {
                for (Class<? extends Activity> clazz : activities) {
                    if (clazz != null) {
                        this.excludedAppOpenActivities.add(clazz);
                    }
                }
            }
            return this;
        }

        public Builder setAppOpenCooldownMs(long cooldownMs) {
            this.appOpenCooldownMs = Math.max(0L, cooldownMs);
            return this;
        }

        public Builder setAppOpenPreloadDelayMs(long delayMs) {
            this.appOpenPreloadDelayMs = Math.max(0L, delayMs);
            return this;
        }

        public Builder setLoadingOverlayTimeoutMs(long timeoutMs) {
            this.loadingOverlayTimeoutMs = Math.max(1000L, timeoutMs);
            return this;
        }

        public Builder setBannerRetryCooldownMs(long cooldownMs) {
            this.bannerRetryCooldownMs = Math.max(1000L, cooldownMs);
            return this;
        }

        @NonNull
        public AdsConfig build() {
            return new AdsConfig(this);
        }
    }
}
