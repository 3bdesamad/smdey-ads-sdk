package com.smdey.ads.managers;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;
import com.smdey.ads.callbacks.ConsentCallback;
import com.smdey.ads.core.AdsConfig;
import com.smdey.ads.core.LifecycleGuard;
import com.smdey.ads.core.SdkGate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles Google User Messaging Platform (UMP) consent gathering and GDPR compliance.
 */
public final class ConsentManager {

    private static ConsentManager instance;
    private final ConsentInformation consentInformation;
    private final AdsConfig config;
    private final AtomicBoolean isGatheringConsent = new AtomicBoolean(false);
    private final List<ConsentCallback> pendingCallbacks = new ArrayList<>();

    private ConsentManager(@NonNull Context context, @NonNull AdsConfig config) {
        this.config = config;
        this.consentInformation = UserMessagingPlatform.getConsentInformation(context);
    }

    public static synchronized ConsentManager getInstance(@NonNull Context context, @NonNull AdsConfig config) {
        if (instance == null) {
            instance = new ConsentManager(context.getApplicationContext(), config);
        }
        return instance;
    }

    public static synchronized ConsentManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new ConsentManager(context.getApplicationContext(), new AdsConfig.Builder().build());
        }
        return instance;
    }

    public boolean canRequestAds() {
        return consentInformation.canRequestAds();
    }

    public boolean isPrivacyOptionsRequired() {
        return consentInformation.getPrivacyOptionsRequirementStatus() == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public void gatherConsent(@NonNull Activity activity, @Nullable ConsentCallback consentCallback) {
        enqueueCallback(consentCallback);

        if (!LifecycleGuard.isActivityValid(activity)) {
            Log.w(SdkGate.TAG, "⚠️ ConsentManager - Activity is invalid. Aborting consent gathering.");
            dispatchCallbacks();
            return;
        }

        if (!isGatheringConsent.compareAndSet(false, true)) {
            return;
        }

        ConsentRequestParameters params = getConsentRequestParameters(activity);

        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    if (!LifecycleGuard.isActivityValid(activity)) {
                        isGatheringConsent.set(false);
                        dispatchCallbacks();
                        return;
                    }
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                            activity,
                            (FormError formError) -> {
                                isGatheringConsent.set(false);
                                if (formError != null) {
                                    Log.w(SdkGate.TAG, "❌ ConsentManager - Form error: "
                                            + formError.getErrorCode() + ": " + formError.getMessage());
                                }
                                dispatchCallbacks();
                            }
                    );
                },
                (FormError formError) -> {
                    isGatheringConsent.set(false);
                    Log.w(SdkGate.TAG, "❌ ConsentManager - Info update failed: "
                            + formError.getErrorCode() + ": " + formError.getMessage());
                    dispatchCallbacks();
                }
        );
    }

    public void showPrivacyOptions(@NonNull Activity activity, @Nullable ConsentCallback consentCallback) {
        UserMessagingPlatform.showPrivacyOptionsForm(
                activity,
                formError -> {
                    if (formError != null) {
                        Log.w(SdkGate.TAG, "❌ ConsentManager - Privacy options error: "
                                + formError.getErrorCode() + ": " + formError.getMessage());
                    }
                    if (consentCallback != null) {
                        consentCallback.onConsentGathered();
                    }
                }
        );
    }

    public void reset() {
        if (consentInformation != null) {
            consentInformation.reset();
        }
    }

    private ConsentRequestParameters getConsentRequestParameters(Activity activity) {
        ConsentRequestParameters.Builder paramsBuilder = new ConsentRequestParameters.Builder();
        if (config.isDebugMode()) {
            ConsentDebugSettings.Builder debugSettingsBuilder = new ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA);

            if (config.getTestDeviceHashedId() != null && !config.getTestDeviceHashedId().isEmpty()) {
                debugSettingsBuilder.addTestDeviceHashedId(config.getTestDeviceHashedId());
            }

            paramsBuilder.setConsentDebugSettings(debugSettingsBuilder.build());
        }

        return paramsBuilder.build();
    }

    private void enqueueCallback(@Nullable ConsentCallback consentCallback) {
        if (consentCallback == null) {
            return;
        }

        synchronized (pendingCallbacks) {
            pendingCallbacks.add(consentCallback);
        }
    }

    private void dispatchCallbacks() {
        List<ConsentCallback> callbacks;
        synchronized (pendingCallbacks) {
            callbacks = new ArrayList<>(pendingCallbacks);
            pendingCallbacks.clear();
        }

        for (ConsentCallback callback : callbacks) {
            callback.onConsentGathered();
        }
    }
}
