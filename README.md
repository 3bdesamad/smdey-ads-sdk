# Smdey Ads Android SDK 🚀

A high-performance, lifecycle-safe, and low-end device optimized Google Mobile Ads & UMP (GDPR) Consent library for Android.

[![JitPack](https://img.shields.io/badge/JitPack-1.0.0-brightgreen.svg)](https://jitpack.io/#3bdesamad/ads)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![MinSdk](https://img.shields.io/badge/MinSdk-28-green.svg)](https://developer.android.com)
[![TargetSdk](https://img.shields.io/badge/TargetSdk-37-brightgreen.svg)](https://developer.android.com)

---

## 🌟 Supported Ad Formats & Features

- ⚡ **Cold-Start Protection (`SdkGate`)**: Defers SDK initialization to avoid main-thread jank and frame drops on budget devices.
- ♻️ **Shared Banner Reuse & Collapsible Banners**: Reuses a single `AdView` across screens and supports high-eCPM collapsible bottom/top banners.
- 🖼️ **Self-Managing Native Ads (`SmartNativeView`)**: Automatic Shimmer skeleton placeholders, Small & Medium templates, and lifecycle-safe unbinding.
- 🏆 **Rewarded Interstitial Ads**: Premium seamless rewarded transitions with Google countdown screen and high eCPMs.
- 🎁 **Standard Rewarded Ads**: User-triggered opt-in reward sessions with decoupled SDK listeners.
- ⏱️ **Debounced Interstitials & Click Throttling**: Frequency control and cooldown timers with smooth custom loading overlays.
- 📱 **App Open Cooldown**: Automatic banner auto-hide/restore bridge when App Open ads are presented.
- 🛡️ **Google UMP (GDPR) Ready**: European Economic Area (EEA) consent gathering and Privacy Options management built-in.
- ⏳ **Anti-Tamper Grace Period**: Automatically pauses ads for newly installed users with OS install-time tracking and clock rollback detection.
- 💎 **One-Line In-App Purchase Support**: Call `AdsFacade.getInstance().setAdsRemoved(true)` to instantly disable all ads.

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
                .setRewardedInterstitialId("ca-app-pub-xxxxxxxxxxxxxxxx/vvvvvvvvvv")
                .setNativeId("ca-app-pub-xxxxxxxxxxxxxxxx/nnnnnnnnnn")
                .setCollapsibleBannerEnabled(true) // Enables 2x-3x higher banner eCPM
                .setCollapsibleGravity("bottom")
                .setGracePeriodEnabled(true)       // Delay ads for new installs
                .setGracePeriodDays(3)             // Days to pause ads (e.g. 3, 5, 7)
                .setDebugMode(BuildConfig.DEBUG)   // Uses Google test IDs in debug builds
                .setInterstitialFrequency(3)       // Show interstitial every 3 clicks
                .setInterstitialCooldownMs(30000)
                .setAppOpenCooldownMs(40000)
                .build();

        AdsFacade.init(this, config);
    }
}
```

> [!TIP]
> **Smart Auto-Configuration**: All Ad Unit IDs are completely optional! If your app doesn't use a format (e.g. Rewarded or Native), **simply omit its `.set...Id()` call**. The SDK automatically detects missing IDs and disables that format with zero wasted network or memory.
> You can also explicitly pass `.setRewardedEnabled(false)` anytime (e.g. for Firebase Remote Config kill-switches).

### 2. Add Smart Banner in XML Layout

```xml
<com.smdey.ads.views.SmartBannerView
    android:id="@+id/smartBanner"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

### 3. Add Smart Native Ads in XML Layout

#### Small Template (Compact row format)
```xml
<com.smdey.ads.views.SmartNativeView
    android:id="@+id/smartNativeSmall"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:ads_template="small" />
```

#### Medium Template (Card format with MediaView)
```xml
<com.smdey.ads.views.SmartNativeView
    android:id="@+id/smartNativeMedium"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:ads_template="medium" />
```

### 4. Show Interstitial Ad on Navigation / Button Click

```java
binding.btnNextScreen.setOnClickListener(v -> {
    AdsFacade.getInstance().interstitial().navigationClickAd(this, () -> {
        // Proceed with navigation
        startActivity(new Intent(this, NextActivity.class));
    });
});
```

### 5. Load & Show Rewarded Interstitial Ads (Recommended)

```java
AdsFacade.getInstance().rewardedInterstitial().loadAd(this, new RewardedInterstitialManager.OnLoadListener() {
    @Override
    public void onAdLoaded() {
        AdsFacade.getInstance().rewardedInterstitial().showAd(MainActivity.this, rewardItem -> {
            Toast.makeText(MainActivity.this, "Reward earned: " + rewardItem.getAmount(), Toast.LENGTH_SHORT).show();
        }, () -> {
            // Ad dismissed callback
        });
    }

    @Override
    public void onAdFailedToLoad() {
        Toast.makeText(MainActivity.this, "Failed to load rewarded interstitial", Toast.LENGTH_SHORT).show();
    }
});
```

### 6. Load & Show Standard Rewarded Ads

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

### 7. Request GDPR / UMP Consent

```java
AdsFacade.getInstance().ensureConsentThenRun(this, () -> {
    // Consent resolved -> safe to request/preload ads
});
```

### 8. Remove Ads for VIP / In-App Purchases

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
