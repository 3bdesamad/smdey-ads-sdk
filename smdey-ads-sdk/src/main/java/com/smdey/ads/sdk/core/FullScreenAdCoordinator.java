package com.smdey.ads.sdk.core;

import java.util.concurrent.atomic.AtomicBoolean;

public final class FullScreenAdCoordinator {

    private final AtomicBoolean fullScreenShowing = new AtomicBoolean(false);

    public boolean isFullScreenShowing() {
        return fullScreenShowing.get();
    }

    public boolean tryAcquireShowLock() {
        return fullScreenShowing.compareAndSet(false, true);
    }

    public void releaseShowLock() {
        fullScreenShowing.set(false);
    }
}
