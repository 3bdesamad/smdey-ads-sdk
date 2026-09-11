package com.smdey.ads.sdk.core;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Global executor pools for the Ads SDK.
 * Strictly adheres to GEMINI.md global executor pool requirements.
 */
public final class AppExecutors {

    private static volatile AppExecutors instance;

    private final ExecutorService background;
    private final MainThreadExecutor mainThread;

    private AppExecutors() {
        // Capped 2-thread pool with background priority to prevent CPU starvation on low-end devices
        ThreadFactory backgroundThreadFactory = runnable -> new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            runnable.run();
        }, "smdey-ads-bg");

        this.background = Executors.newFixedThreadPool(2, backgroundThreadFactory);
        this.mainThread = new MainThreadExecutor();
    }

    @NonNull
    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    @NonNull
    public ExecutorService background() {
        return background;
    }

    @NonNull
    public MainThreadExecutor mainThread() {
        return mainThread;
    }

    /**
     * Main thread executor with zero-latency fast-path for UI execution.
     */
    public static final class MainThreadExecutor implements Executor {
        private final Handler mainHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                command.run(); // O(1) Fast path: execute immediately if already on the UI thread
            } else {
                mainHandler.post(command);
            }
        }

        public void postDelayed(@NonNull Runnable command, long delayMillis) {
            mainHandler.postDelayed(command, delayMillis);
        }

        public void removeCallbacks(@NonNull Runnable command) {
            mainHandler.removeCallbacks(command);
        }
    }
}
