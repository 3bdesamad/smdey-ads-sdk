package com.smdey.ads.sdk.core;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;
import com.smdey.ads.sdk.AdsConfig;
import com.smdey.ads.sdk.consent.ConsentManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class SdkGate {

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
    private final ConsentManager consentManager;
    private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);
    private final AtomicBoolean startupStable = new AtomicBoolean(false);
    private final AtomicBoolean initScheduled = new AtomicBoolean(false);
    private final List<Runnable> pendingReadyCallbacks = new ArrayList<>();

    public SdkGate(@NonNull Context context, @NonNull AdsConfig config, @NonNull ConsentManager consentManager) {
        this.appContext = context.getApplicationContext();
        this.config = config;
        this.consentManager = consentManager;
    }

    public boolean isReady() {
        return state.get() == State.READY;
    }

    @NonNull
    public State getState() {
        return state.get();
    }

    public void ensureInitialized(@NonNull Activity activity, @Nullable Runnable onReady) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            Log.d(config.getTag(), "⚠️ SDK - Invalid activity. Skipping initialization.");
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

        startupStable.set(true);
        attemptInitialization(activity);
    }

    public void markStartupStable(@NonNull Activity activity) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            return;
        }

        if (startupStable.compareAndSet(false, true)) {
            Log.i(config.getTag(), "✅ SDK - Startup stable.");
        }
        attemptInitialization(activity);
    }

    public void notifyConsentResolved(@NonNull Activity activity) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            return;
        }

        Log.i(config.getTag(), "✅ SDK - Consent resolved. Can request ads: " + consentManager.canRequestAds());
        attemptInitialization(activity);
    }

    public void shutdown() {
        // No-op: Background threads are centrally managed by AppExecutors
    }

    private void attemptInitialization(@NonNull Activity activity) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            return;
        }

        if (isReady()) {
            flushPendingCallbacks();
            return;
        }

        if (!consentManager.canRequestAds()) {
            state.set(State.WAITING_CONSENT);
            Log.d(config.getTag(), "⏳ SDK - Waiting for consent. Requesting UMP consent flow...");
            consentManager.gatherConsent(activity, () -> {
                if (consentManager.canRequestAds()) {
                    notifyConsentResolved(activity);
                } else {
                    Log.w(config.getTag(), "⚠️ SDK - Consent not granted after UMP flow.");
                }
            });
            return;
        }

        if (!startupStable.get()) {
            state.set(State.WAITING_SAFE_WINDOW);
            Log.d(config.getTag(), "⏳ SDK - Waiting for safe startup window.");
            return;
        }

        if (state.get() == State.INITIALIZING) {
            return;
        }

        if (!initScheduled.compareAndSet(false, true)) {
            return;
        }

        long delay = 800L; // Safe deferral to let Activity first frame draw smoothly
        state.set(State.WAITING_SAFE_WINDOW);

        AppExecutors.getInstance().mainThread().postDelayed(this::startInitialization, delay);
    }

    private void startInitialization() {
        if (isReady()) {
            initScheduled.set(false);
            flushPendingCallbacks();
            return;
        }

        state.set(State.INITIALIZING);
        Log.i(config.getTag(), "⏳ SDK - Initialization started (Next-Gen).");

        AppExecutors.getInstance().background().execute(() -> {
            try {
                String appId = config.getAppId();
                InitializationConfig initConfig = new InitializationConfig.Builder(appId).build();

                MobileAds.initialize(appContext, initConfig, initializationStatus ->
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            state.set(State.READY);
                            initScheduled.set(false);
                            Log.i(config.getTag(), "✅ SDK - Initialized successfully (Next-Gen).");
                            flushPendingCallbacks();
                        }));
            } catch (Exception e) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    state.set(State.FAILED);
                    initScheduled.set(false);
                    Log.e(config.getTag(), "❌ SDK - Initialization failed.", e);
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
