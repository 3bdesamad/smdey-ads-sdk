package com.smdey.ads.sample;

/**
 * Global App Configuration & Ad Controls.
 */
public final class Constants {

    private Constants() {}

    // 🎛️ CONTROLS: Change these anytime in your app (or fetch from Firebase Remote Config)
    public static final boolean ENABLE_GRACE_PERIOD = true; // true = activate delay, false = show ads immediately
    public static final int GRACE_PERIOD_DAYS = 3;          // Number of days (e.g. 3, 5, 7)

    // Storage Keys
    public static final String PREFS_NAME = "app_ads_prefs";
    public static final String KEY_LAST_KNOWN_TIME = "last_known_time";
}
