package com.smdey.ads.sdk.consent;

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
import com.smdey.ads.sdk.AdsConfig;
import com.smdey.ads.sdk.callbacks.ConsentCallback;
import com.smdey.ads.sdk.core.AppExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConsentManager {

    private final ConsentInformation consentInformation;
    private final AdsConfig config;
    private final AtomicBoolean isGatheringConsent = new AtomicBoolean(false);
    private final List<ConsentCallback> pendingCallbacks = new ArrayList<>();

    public ConsentManager(@NonNull Context context, @NonNull AdsConfig config) {
        this.config = config;
        this.consentInformation = UserMessagingPlatform.getConsentInformation(context);
    }

    public boolean canRequestAds() {
        return consentInformation.canRequestAds();
    }

    public void gatherConsent(@NonNull Activity activity, @Nullable ConsentCallback consentCallback) {
        enqueueCallback(consentCallback);

        if (!isGatheringConsent.compareAndSet(false, true)) {
            return;
        }

        ConsentRequestParameters params = getConsentRequestParameters(activity);

        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    Log.i(config.getTag(), "✅ Consent - Info update succeeded. Status: "
                            + consentInformation.getConsentStatus()
                            + ", canRequestAds: " + canRequestAds());
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                            activity,
                            (FormError formError) -> {
                                isGatheringConsent.set(false);
                                if (formError != null) {
                                    Log.w(config.getTag(), "❌ Consent - Form error "
                                            + formError.getErrorCode() + ": " + formError.getMessage());
                                } else {
                                    Log.i(config.getTag(), "✅ Consent - Form dismissed or not required. canRequestAds: "
                                            + canRequestAds());
                                }
                                dispatchCallbacks();
                            }
                    );
                },
                (formError) -> {
                    isGatheringConsent.set(false);
                    Log.w(config.getTag(), "❌ Consent - Info update failed "
                            + formError.getErrorCode() + ": " + formError.getMessage());
                    dispatchCallbacks();
                }
        );
    }

    public void reset() {
        if (consentInformation != null) {
            consentInformation.reset();
        }
    }

    public boolean isPrivacyOptionsRequired() {
        return consentInformation.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public void showPrivacyOptions(@NonNull Activity activity, @Nullable ConsentCallback consentCallback) {
        UserMessagingPlatform.showPrivacyOptionsForm(
                activity,
                formError -> {
                    if (formError != null) {
                        Log.w(config.getTag(), "❌ Consent - Privacy options error "
                                + formError.getErrorCode() + ": " + formError.getMessage());
                    }
                    if (consentCallback != null) {
                        AppExecutors.getInstance().mainThread().execute(consentCallback::onConsentCompleted);
                    }
                }
        );
    }

    private ConsentRequestParameters getConsentRequestParameters(Activity activity) {
        ConsentRequestParameters.Builder paramsBuilder = new ConsentRequestParameters.Builder();
        if (config.isDebug()) {
            ConsentDebugSettings.Builder debugBuilder = new ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA);
            if (config.getTestDeviceId() != null && !config.getTestDeviceId().isEmpty()) {
                debugBuilder.addTestDeviceHashedId(config.getTestDeviceId());
            }
            paramsBuilder.setConsentDebugSettings(debugBuilder.build());
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

        AppExecutors.getInstance().mainThread().execute(() -> {
            for (ConsentCallback callback : callbacks) {
                callback.onConsentCompleted();
            }
        });
    }
}
