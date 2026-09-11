# Smdey Ads SDK (Next-Gen)

A lightweight, high-performance, lifecycle-aware Google Mobile Ads (GMA) SDK wrapper for Android.
Optimized for **low-end Android devices** with **zero main-thread blocking**, instant startup, automatic UMP consent handling, adaptive banner pooling, and full-screen ad coordination.

---

## Features

- **🚀 Low-End Device Optimized:** Zero UI jank, zero allocations in hot paths, asynchronous SDK initialization.
- **🛡️ Full-Screen Ad Coordinator:** Prevents duplicate ad overlaps and AdMob policy violations.
- **🔄 Window Focus Guard:** Prevents Samsung OneUI / Android launcher transition ad collisions.
- **⏱️ Configurable Cooldowns:** Full control over App Open cooldowns, preload delays, and loading dialog timeouts.
- **🚫 Declarative Activity Exclusion:** Easily suppress App Open ads on specific screens (e.g., Splash, Settings, Paywalls).
- **📋 Automatic UMP Consent Gathering:** Seamless integration with Google User Messaging Platform (GDPR/EEA).
- **📱 Smart Adaptive Banner View:** Single shared AdView across activities with zero memory leaks.

---

## Installation (JitPack)

### 1. Add repository to settings.gradle:
`groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
`

### 2. Add dependency to your pp/build.gradle:
`groovy
dependencies {
    implementation 'com.github.YOUR_USERNAME:smdey-ads-sdk:1.0.0'
}
`

---

## Quick Start

### 1. In AndroidManifest.xml
Add your AdMob Application ID:
`xml
<manifest>
    <application ...>
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY" />
    </application>
</manifest>
`

### 2. Initialize in MyApplication.java
`java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        AdsSdk.init(this, new AdsConfig.Builder("ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY")
                .setBannerId("ca-app-pub-XXXXXXXXXXXXXXXX/BANNER_ID")
                .setInterstitialId("ca-app-pub-XXXXXXXXXXXXXXXX/INTERSTITIAL_ID")
                .setRewardedId("ca-app-pub-XXXXXXXXXXXXXXXX/REWARDED_ID")
                .setAppOpenId("ca-app-pub-XXXXXXXXXXXXXXXX/APP_OPEN_ID")
                .setDebug(BuildConfig.DEBUG)
                .setInterstitialInterval(6) // Show interstitial every 6 clicks
                
                // Exclude specific Activities from showing App Open ads
                .excludeAppOpenActivities(LauncherActivity.class, ActivitySettings.class)
                
                // Optional cooldown & timeout customizations:
                .setAppOpenCooldownMs(15000L)         // 15s cooldown between App Open ads
                .setAppOpenPreloadDelayMs(3000L)       // 3s delay after startup before preloading
                .setLoadingOverlayTimeoutMs(4000L)     // 4s max wait for interstitial loading overlay
                .setBannerRetryCooldownMs(15000L)      // 15s cooldown after failed banner load
                
                .setLoadingOverlayProvider(Dialogs::showLoadingAd)
                .build()
        );
    }
}
`

---

## Running the Demo App

The included :app module is a ready-to-run demo configured with Google's official public test ad units:
1. Clone this repository.
2. Open in Android Studio.
3. Click **Run** — test ads work immediately out of the box!
