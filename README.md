# Smdey Ads Android SDK 🚀

A high-performance, lifecycle-safe, and low-end device optimized Google Mobile Ads & UMP (GDPR) Consent library for Android.

[![JitPack](https://jitpack.io/v/3bdesamad/ads.svg)](https://jitpack.io/#3bdesamad/ads)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![MinSdk](https://img.shields.io/badge/MinSdk-28-green.svg)](https://developer.android.com)
[![TargetSdk](https://img.shields.io/badge/TargetSdk-37-brightgreen.svg)](https://developer.android.com)

---

## 🌟 Key Highlights & Features

- ⚡ **Cold-Start Protection (`SdkGate`)**: Defers SDK initialization to avoid main-thread jank and frame drops on budget devices.
- ♻️ **Shared Banner Reuse**: Reuses a single `AdView` across screens to eliminate GC churn and UI flickering.
- 🔒 **Zero Memory Leaks**: Uses `WeakReference` for Activities and ViewGroups with automated `LifecycleGuard` safety checks.
- 🛡️ **Google UMP (GDPR) Ready**: Full European Economic Area (EEA) consent gathering and Privacy Options management built-in.
- ⏱️ **Debounced Interstitials & Click Throttling**: Frequency control and cooldown timers with smooth custom loading overlays.
- 📱 **App Open Cooldown**: Automatic banner auto-hide/restore bridge when App Open ads are presented.
- 💎 **One-Line In-App Purchase Support**: Call `AdsFacade.getInstance().setAdsRemoved(true)` to instantly disable all banners, interstitials, and app open ads.

---

## 📦 Installation

### 1. Add JitPack repository

In your root `settings.gradle`:
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = 'https://jitpack.io' }
    }
}
```

### 2. Add the dependency

In your `app/build.gradle`:
```groovy
dependencies {
    implementation 'com.github.3bdesamad:ads:1.0.0'
}
```

### 3. Add your AdMob App ID

In your `app/src/main/AndroidManifest.xml`:
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <!-- Replace with your actual AdMob App ID -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" />
    </application>
</manifest>
```

---

## 🛠️ Quick Start & Code Examples

### 1. Initialize in `Application.java`

```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        AdsConfig config = new AdsConfig.Builder()
                .setBannerId("ca-app-pub-xxxxxxxxxxxxxxxx/bbbbbbbbbb")
                .setInterstitialId("ca-app-pub-xxxxxxxxxxxxxxxx/iiiiiiiiii")
                .setAppOpenId("ca-app-pub-xxxxxxxxxxxxxxxx/oooooooooo")
                .setRewardedId("ca-app-pub-xxxxxxxxxxxxxxxx/rrrrrrrrrr")
                .setDebugMode(BuildConfig.DEBUG) // Automatically uses Google test IDs in debug builds
                .setInterstitialFrequency(3)     // Show interstitial every 3 clicks
                .setInterstitialCooldownMs(30000)
                .setAppOpenCooldownMs(40000)
                .build();

        AdsFacade.init(this, config);
    }
}
```

### 2. Add Smart Banner in XML Layout

```xml
<com.smdey.ads.views.SmartBannerView
    android:id="@+id/smartBanner"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

### 3. Show Interstitial Ad on Navigation / Button Click

```java
binding.btnNextScreen.setOnClickListener(v -> {
    AdsFacade.getInstance().interstitial().navigationClickAd(this, () -> {
        // Proceed with navigation
        startActivity(new Intent(this, NextActivity.class));
    });
});
```

### 4. Load & Show Rewarded Ads

```java
AdsFacade.getInstance().rewarded().loadAd(this, new RewardedManager.OnLoadListener() {
    @Override
    public void onAdLoaded() {
        AdsFacade.getInstance().rewarded().showAd(MainActivity.this, rewardItem -> {
            Toast.makeText(MainActivity.this, "Reward earned: " + rewardItem.getAmount(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onAdFailedToLoad() {
        Toast.makeText(MainActivity.this, "Failed to load rewarded ad", Toast.LENGTH_SHORT).show();
    }
});
```

### 5. Request GDPR / UMP Consent

```java
AdsFacade.getInstance().ensureConsentThenRun(this, () -> {
    // Consent resolved -> safe to request/preload ads
});
```

### 6. Remove Ads for VIP / In-App Purchases

```java
// When user purchases "Remove Ads"
AdsFacade.getInstance().setAdsRemoved(true);
```

---

## 🔒 Proguard Rules

The library automatically includes consumer Proguard rules. If needed manually:
```proguard
-keep class com.smdey.ads.** { *; }
-keepclassmembers class com.smdey.ads.** { *; }
```

---

## 📄 License & Author

Developed by **Abdessamad** (`3bdesamad`).  
Licensed under the [Apache License 2.0](LICENSE).
