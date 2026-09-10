package com.smdey.ads.core;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;
import com.smdey.ads.managers.ConsentManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Coordinates safe, deferred initialization of the Google Mobile Ads (GMA Next-Gen) SDK.
 */
public final class SdkGate {

    public static final String TAG = "SmdeyAds";

    public enum State {
        IDLE,
        WAITING_CONSENT,
        WAITING_SAFE_WINDOW,
        INITIALIZING,
        READY,
        FAILED
    }

    private final Context appContext;
    private final AdsConfig config;
    private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);
    private final AtomicBoolean startupStable = new AtomicBoolean(false);
    private final AtomicBoolean initScheduled = new AtomicBoolean(false);
    private final List<Runnable> pendingReadyCallbacks = new ArrayList<>();

    public SdkGate(@NonNull Context context, @NonNull AdsConfig config) {
        this.appContext = context.getApplicationContext();
        this.config = config;
    }

    public boolean isReady() {
        return state.get() == State.READY;
    }

    @NonNull
    public State getState() {
        return state.get();
    }

    @NonNull
    public AdsConfig getConfig() {
        return config;
    }

    @NonNull
    public Context getAppContext() {
        return appContext;
    }

    public void ensureInitialized(@NonNull Activity activity, @Nullable Runnable onReady) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            Log.d(TAG, "⚠️ SdkGate - Invalid activity. Skipping initialization.");
            return;
        }

        if (isReady()) {
            if (onReady != null) {
                AppExecutors.getInstance().mainThread().execute(onReady);
            }
            return;
        }

        if (onReady != null) {
            synchronized (pendingReadyCallbacks) {
                pendingReadyCallbacks.add(onReady);
            }
        }

        attemptInitialization(activity);
    }

    public void markStartupStable(@NonNull Activity activity) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            return;
        }

        if (startupStable.compareAndSet(false, true)) {
            Log.i(TAG, "✅ SdkGate - Startup marked as stable.");
        }
        attemptInitialization(activity);
    }

    public void notifyConsentResolved(@NonNull Activity activity) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            return;
        }

        Log.i(TAG, "✅ SdkGate - Consent resolved. Can request ads: "
                + ConsentManager.getInstance(activity, config).canRequestAds());
        attemptInitialization(activity);
    }

    public void shutdown() {
        synchronized (pendingReadyCallbacks) {
            pendingReadyCallbacks.clear();
        }
    }

    private void attemptInitialization(@NonNull Activity activity) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            return;
        }

        if (isReady()) {
            flushPendingCallbacks();
            return;
        }

        if (!ConsentManager.getInstance(activity, config).canRequestAds()) {
            state.set(State.WAITING_CONSENT);
            Log.d(TAG, "⏳ SdkGate - Waiting for GDPR consent before SDK init.");
            return;
        }

        if (!startupStable.get()) {
            state.set(State.WAITING_SAFE_WINDOW);
            Log.d(TAG, "⏳ SdkGate - Waiting for safe startup window.");
            return;
        }

        if (state.get() == State.INITIALIZING) {
            return;
        }

        if (!initScheduled.compareAndSet(false, true)) {
            return;
        }

        long delay = config.getSafeStartupDelayMs();
        state.set(State.WAITING_SAFE_WINDOW);
        Log.i(TAG, "⏳ SdkGate - Initializing SDK with delay: " + delay + " ms.");

        AppExecutors.getInstance().mainThread().postDelayed(this::startInitialization, delay);
    }

    private void startInitialization() {
        if (isReady()) {
            initScheduled.set(false);
            flushPendingCallbacks();
            return;
        }

        state.set(State.INITIALIZING);
        Log.i(TAG, "⏳ SdkGate - Background SDK initialization started.");

        AppExecutors.getInstance().background().execute(() -> {
            try {
                String appId = "";
                try {
                    ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(
                            appContext.getPackageName(),
                            PackageManager.GET_META_DATA
                    );
                    if (ai != null && ai.metaData != null) {
                        String metaAppId = ai.metaData.getString("com.google.android.gms.ads.APPLICATION_ID");
                        if (metaAppId != null) {
                            appId = metaAppId;
                        }
                    }
                } catch (Exception ignored) {
                }

                InitializationConfig.Builder initConfigBuilder = new InitializationConfig.Builder(appId);

                String testDeviceId = config.getTestDeviceHashedId();
                if (testDeviceId != null && !testDeviceId.trim().isEmpty()) {
                    RequestConfiguration requestConfiguration = new RequestConfiguration.Builder()
                            .setTestDeviceIds(Collections.singletonList(testDeviceId))
                            .build();
                    initConfigBuilder.setRequestConfiguration(requestConfiguration);
                }

                InitializationConfig initConfig = initConfigBuilder.build();

                MobileAds.initialize(appContext, initConfig, initializationStatus ->
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            MobileAds.setUserControlledAppVolume(config.getAppVolume());
                            state.set(State.READY);
                            initScheduled.set(false);
                            Log.i(TAG, "✅ SdkGate - GMA Next-Gen SDK initialized successfully.");
                            flushPendingCallbacks();
                        }));
            } catch (Exception e) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    state.set(State.FAILED);
                    initScheduled.set(false);
                    Log.e(TAG, "❌ SdkGate - Initialization failed.", e);
                });
            }
        });
    }

    private void flushPendingCallbacks() {
        List<Runnable> callbacks;
        synchronized (pendingReadyCallbacks) {
            callbacks = new ArrayList<>(pendingReadyCallbacks);
            pendingReadyCallbacks.clear();
        }

        for (Runnable callback : callbacks) {
            AppExecutors.getInstance().mainThread().execute(callback);
        }
    }
}
