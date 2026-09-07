package com.smdey.ads.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.smdey.ads.core.AdsConfig;
import com.smdey.ads.core.SdkGate;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Manages tamper-proof ad grace periods for newly installed users.
 * Uses Android OS-level install time to survive Clear Cache & Clear Data,
 * and incorporates clock-rollback detection.
 */
public final class GracePeriodManager {

    private static final String PREFS_NAME = "smdey_ads_grace_prefs";
    private static final String KEY_LAST_KNOWN_TIME = "last_known_time";

    private GracePeriodManager() {}

    /**
     * Checks whether the user is currently within the onboarding grace period.
     *
     * @param context Application or Activity context.
     * @param config  The SDK configuration containing grace period settings.
     * @return true if ads should be paused; false if ads should be served normally.
     */
    public static boolean isGracePeriodActive(@NonNull Context context, @NonNull AdsConfig config) {
        if (!config.isGracePeriodEnabled() || config.getGracePeriodDays() <= 0) {
            return false;
        }

        try {
            long installTime = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .firstInstallTime;

            long now = System.currentTimeMillis();

            // Anti-Cheat 1: Device clock was manually set before the app install date
            if (now < installTime) {
                Log.w(SdkGate.TAG, "Clock tampering detected (now < installTime). Bypassing grace period.");
                return false;
            }

            // Anti-Cheat 2: Device clock was rolled back compared to last recorded session
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            long lastKnownTime = prefs.getLong(KEY_LAST_KNOWN_TIME, 0L);
            if (now < lastKnownTime) {
                Log.w(SdkGate.TAG, "Clock tampering detected (now < lastKnownTime). Bypassing grace period.");
                return false;
            }

            // Record current time asynchronously
            prefs.edit().putLong(KEY_LAST_KNOWN_TIME, now).apply();

            long totalGraceMs = TimeUnit.DAYS.toMillis(config.getGracePeriodDays());
            long elapsedMs = now - installTime;
            boolean inGrace = elapsedMs < totalGraceMs;

            if (inGrace) {
                long remainingHours = TimeUnit.MILLISECONDS.toHours(totalGraceMs - elapsedMs);
                Log.i(SdkGate.TAG, String.format(Locale.US, "Grace period active (%d hours remaining). Ads muted.", remainingHours));
            } else {
                Log.i(SdkGate.TAG, "Grace period expired. Ads serving normally.");
            }

            return inGrace;

        } catch (Exception e) {
            Log.e(SdkGate.TAG, "Error verifying install time. Defaulting to showing ads.", e);
            return false;
        }
    }

    /**
     * Formats human-readable status description for UI displays or debugging.
     */
    @NonNull
    public static String getStatusSummary(@NonNull Context context, @NonNull AdsConfig config) {
        if (!config.isGracePeriodEnabled()) {
            return "Grace Period: Disabled (Ads Active)";
        }

        try {
            long installTime = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .firstInstallTime;
            long now = System.currentTimeMillis();
            long totalGraceMs = TimeUnit.DAYS.toMillis(config.getGracePeriodDays());
            long elapsedMs = now - installTime;

            if (now >= installTime && elapsedMs < totalGraceMs) {
                long remainingHours = TimeUnit.MILLISECONDS.toHours(totalGraceMs - elapsedMs);
                return String.format(Locale.US, "Grace Period: ACTIVE (%d hours remaining) - Ads Paused", remainingHours);
            }
        } catch (Exception ignored) {
        }

        return "Grace Period: Expired - Ads Serving";
    }
}
