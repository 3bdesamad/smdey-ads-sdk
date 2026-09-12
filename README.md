# Smdey Ads Android SDK 🚀

A high-performance, lifecycle-safe, and low-end device optimized Google Mobile Ads (GMA Next-Gen 1.4.0) & UMP (GDPR) Consent library for Android.

[![JitPack](https://img.shields.io/badge/JitPack-1.0.2-brightgreen.svg)](https://jitpack.io/#3bdesamad/smdey-ads-sdk)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![MinSdk](https://img.shields.io/badge/MinSdk-28-green.svg)](https://developer.android.com)
[![TargetSdk](https://img.shields.io/badge/TargetSdk-37-brightgreen.svg)](https://developer.android.com)
[![GMA Next-Gen](https://img.shields.io/badge/GMA_Next--Gen-1.4.0-blue.svg)](https://developers.google.com/admob/android/next-gen)

---

## 🌟 Supported Ad Formats & Features

- ⚡ **Cold-Start Protection (`SdkGate`)**: Defers SDK initialization off critical paths to ensure 60fps startup and zero UI freezes on low-end devices.
- 🛡️ **Full-Screen Ad Coordinator**: Thread-safe synchronization preventing Interstitial, Rewarded, and App Open ads from colliding or showing concurrently.
- ♻️ **Shared Banner View (`SmartBannerView`)**: Pooled, shared `AdView` reused across screens with dynamic adaptive height calculation, built-in native pulse skeleton placeholders, and zero memory leaks.
- ⏱️ **Debounced Interstitials & Frequency Clicks**: Frequency click counter (`showAdWithLoadingOverlayByClick`), smart pre-caching, and customizable loading overlay dialogs.
- 🎁 **Standard Rewarded Ads**: User-triggered opt-in reward sessions with decoupled load and show callbacks.
- 📱 **Lifecycle-Aware App Open Ads**: Automatic foreground detection via `ProcessLifecycleOwner`, configurable cooldown timer, startup preload delay, and window focus guard.
- 🛡️ **Google UMP (GDPR) Ready**: European Economic Area (EEA) consent gathering and Privacy Options form management built-in.
- 🔄 **Automatic Test Ad Unit Switching**: Never change IDs between dev and release. Supply your real production IDs in `AdsConfig.Builder` — when `.setDebug(BuildConfig.DEBUG)` is enabled, the SDK automatically serves Google's official test ad units, protecting your account from policy violations.
- 💎 **Dynamic In-App Purchase Provider**: Supply `.setAdsRemovedProvider(SharedPref.getInstance(this)::isAdsRemoved)` to instantly disable all ad requests and views across the app.
- 🚫 **Declarative Activity Exclusion**: Easily suppress App Open ads on selected screens (e.g., Splash, Onboarding, Paywalls).

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
    // GMA Next-Gen SDK & UMP runtime dependencies (controlled by your app)
    implementation 'com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.4.0'
    implementation 'com.google.android.ump:user-messaging-platform:4.0.0'

    // Smdey Ads SDK
    implementation 'com.github.3bdesamad:smdey-ads-sdk:1.0.2'
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

        // 💡 Pass your real AdMob production IDs here.
        // You NEVER have to change or swap IDs manually: when .setDebug(BuildConfig.DEBUG)
        // is true, the SDK automatically serves Google's official test ad units!
        AdsConfig config = new AdsConfig.Builder("ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy")
                .setBannerId("ca-app-pub-xxxxxxxxxxxxxxxx/bbbbbbbbbb")
                .setInterstitialId("ca-app-pub-xxxxxxxxxxxxxxxx/iiiiiiiiii")
                .setRewardedId("ca-app-pub-xxxxxxxxxxxxxxxx/rrrrrrrrrr")
                .setAppOpenId("ca-app-pub-xxxxxxxxxxxxxxxx/oooooooooo")
                .setDebug(BuildConfig.DEBUG)          // Automatically switches to Google test IDs in debug builds!
                .setTag("SMDEY_ADS")
                .setInterstitialInterval(6)           // Show interstitial every 6 clicks
                .excludeAppOpenActivities(LauncherActivity.class) // Suppress App Open on splash/launcher
                .setAppOpenCooldownMs(30000L)         // 30s cooldown between App Open ads
                .setAppOpenPreloadDelayMs(3000L)       // 3s delay after cold start before first preload
                .setLoadingOverlayTimeoutMs(4000L)     // 4s max wait for interstitial loading overlay
                .setBannerRetryCooldownMs(15000L)      // 15s retry cooldown after banner failure
                //.setTestDeviceId("YOUR_HASHED_TEST_DEVICE_ID")
                .setAdsRemovedProvider(SharedPref.getInstance(this)::isAdsRemoved) // VIP / In-App Purchase provider
                .setLoadingOverlayProvider(Dialogs::showLoadingAd) // Custom loading dialog
                .build();

        AdsSdk.init(this, config);
    }
}
```

> [!TIP]
> **Automatic Test Ad Unit Switching**: You do not have to write ternary checks or manage test IDs manually. Simply set your real production IDs in `AdsConfig.Builder`. When `.setDebug(BuildConfig.DEBUG)` is enabled, the SDK automatically serves official Google test ad units (`TEST_APP_ID`, `TEST_BANNER`, `TEST_INTERSTITIAL`, `TEST_REWARDED`, `TEST_APP_OPEN`), protecting your AdMob account from self-clicking strikes during development.
>
> **Smart Auto-Configuration**: All Ad Unit IDs are completely optional! If your app does not use a format (e.g. Rewarded or App Open), **simply omit its `.set...Id()` call**. The SDK automatically disables that format with zero wasted network or memory allocations.

---

### 2. Add Smart Banner in XML Layout

Place `SmartBannerView` anywhere in your XML layout:

```xml
<com.smdey.ads.sdk.banner.SmartBannerView
    android:id="@+id/smartBanner"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

> [!NOTE]
> **Zero Java Setup Needed**: `SmartBannerView` attaches automatically to the shared `BannerManager`, respects Activity lifecycle, displays a built-in native pulse skeleton while loading, and automatically hides/restores during App Open ads.

---

### 3. Show Interstitial Ad on Navigation / Button Click

#### Frequency Click Interstitial (e.g., every 6 clicks):
```java
binding.btnNextScreen.setOnClickListener(v -> {
    AdsSdk.getInstance().interstitial().showAdWithLoadingOverlayByClick(this, () -> {
        // Runs after the ad is dismissed, or immediately if below click threshold / ads removed
        if (!isSafeToUpdate()) return;
        startActivity(new Intent(this, SecondActivity.class));
    });
});
```

#### Direct Navigation Click:
```java
AdsSdk.getInstance().interstitial().navigationClickAd(this, () -> {
    startActivity(new Intent(this, SecondActivity.class));
});
```

#### Direct Full-Screen Interstitial with Loading Overlay:
```java
AdsSdk.getInstance().interstitial().showAdWithLoadingOverlay(this, () -> {
    // Action to perform after interstitial
});
```

---

### 4. Load & Show Standard Rewarded Ads

```java
// 1. If ad is already cached in RAM, show immediately:
if (AdsSdk.getInstance().rewarded().isAdReady()) {
    AdsSdk.getInstance().rewarded().showAd(this, (amount, type) -> {
        Toast.makeText(this, "Reward earned: " + amount + " " + type, Toast.LENGTH_SHORT).show();
    });
} else {
    // 2. Otherwise load and show upon completion:
    AdsSdk.getInstance().rewarded().loadAd(this, new RewardedManager.OnLoadListener() {
        @Override
        public void onLoaded() {
            AdsSdk.getInstance().rewarded().showAd(MainActivity.this, (amount, type) -> {
                Toast.makeText(MainActivity.this, "Reward earned: " + amount + " " + type, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onFailed() {
            Toast.makeText(MainActivity.this, "Failed to load rewarded ad", Toast.LENGTH_SHORT).show();
        }
    });
}
```

---

### 5. App Open Ads

App Open ads are automatically displayed when returning from background via `ProcessLifecycleOwner`.

#### Manual Triggering & Preloading:
```java
if (AdsSdk.getInstance().appOpen().isAdAvailable()) {
    AdsSdk.getInstance().appOpen().showIfAvailable(this);
} else {
    AdsSdk.getInstance().appOpen().preloadAd(this);
}
```

#### Suppress App Open on Specific Screens:
Either pass the Activity class to `excludeAppOpenActivities(...)` during initialization:
```java
new AdsConfig.Builder(...)
    .excludeAppOpenActivities(LauncherActivity.class, PaywallActivity.class)
```

Or implement `OpenAdVisibilityControl` on your Activity:
```java
public class LauncherActivity extends BaseActivity implements OpenAdVisibilityControl {
    @Override
    public boolean canShowOpenAd() {
        return false; // Suppresses App Open ads on this screen
    }

    @Override
    public void hideBannerAd() {}

    @Override
    public void showBannerAd() {}
}
```

---

### 6. Request GDPR / UMP Consent & Privacy Options

#### Gather Consent on App Launch:
```java
AdsSdk.getInstance().consent().gatherConsent(this, () -> {
    boolean canRequestAds = AdsSdk.getInstance().consent().canRequestAds();
    Log.d("APP", "Consent resolved. Can request ads: " + canRequestAds);
});
```

#### Privacy Options Button (Required by Google Play in EEA):
```java
binding.btnPrivacyOptions.setOnClickListener(v -> {
    if (AdsSdk.getInstance().consent().isPrivacyOptionsRequired()) {
        AdsSdk.getInstance().consent().showPrivacyOptions(this, () -> {
            Toast.makeText(this, "Privacy options updated", Toast.LENGTH_SHORT).show();
        });
    }
});
```

---

### 7. Remove Ads for VIP / In-App Purchases

Configure the dynamic provider in `AdsConfig`:
```java
new AdsConfig.Builder(...)
    .setAdsRemovedProvider(SharedPref.getInstance(this)::isAdsRemoved)
    .build();
```
Whenever `isAdsRemoved()` returns `true`, all ad requests, banners, and full-screen interstitials are instantly disabled across the entire application with zero performance penalty.

---

### 8. Custom Loading Dialog Overlay

Provide your own custom dialog to be displayed while loading interstitials:
```java
new AdsConfig.Builder(...)
    .setLoadingOverlayProvider(new AdLoadingOverlayProvider() {
        @Override
        public Dialog showLoading(@NonNull Activity activity) {
            Dialog dialog = new Dialog(activity, R.style.TransparentDialog);
            dialog.setContentView(R.layout.dialog_loading_ad);
            dialog.setCancelable(false);
            dialog.show();
            return dialog;
        }

        @Override
        public void dismissLoading(@Nullable Dialog dialog) {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    })
    .build();
```

---

## 🔒 Proguard / R8 Rules

The library automatically ships consumer ProGuard rules inside the AAR (`consumer-rules.pro`). If configuring manually:

```proguard
# smdey-ads-sdk Public APIs & Managers
-keep public class com.smdey.ads.sdk.** {
    public *;
}

# SmartBannerView XML & Public API
-keep class com.smdey.ads.sdk.banner.SmartBannerView {
    public <init>(...);
    public *;
}

-keepattributes Signature
-keepattributes *Annotation*

# GMA Next-Gen & UMP
-dontwarn com.google.android.libraries.ads.mobile.sdk.**
-dontwarn com.google.android.ump.**
-dontwarn com.google.android.gms.internal.ads.**

# ShimmerSkeletonView
-keep class com.smdey.ads.sdk.banner.ShimmerSkeletonView {
    public <init>(...);
    public *;
}
```

---

## 🏃 Running the Demo App

The included `:app` module is a fully working demo configured with Google's official public test ad units:
1. Clone this repository.
2. Open in Android Studio (Giraffe / Hedgehog / Iguana / Ladybug / Meerkat+).
3. Click **Run** — test ads run immediately without any extra setup!

---

## 📄 License & Author

Developed by **Abdessamad** (`3bdesamad`).  
Licensed under the [Apache License 2.0](LICENSE).
