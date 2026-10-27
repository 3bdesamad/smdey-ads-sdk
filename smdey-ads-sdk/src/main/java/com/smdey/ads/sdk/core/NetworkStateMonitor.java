package com.smdey.ads.sdk.core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

public final class NetworkStateMonitor {

    private static volatile boolean isOnline = true;
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    private NetworkStateMonitor() {}

    public static void init(@NonNull Context context) {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        final Context app = context.getApplicationContext();
        AppExecutors.getInstance().background().execute(() -> {
            ConnectivityManager cm =
                    (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm == null) {
                isOnline = false;
                return;
            }

            try {
                Network active = cm.getActiveNetwork();

                if (active != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(active);
                    isOnline = isValidated(caps);
                } else {
                    isOnline = false;
                }

                cm.registerDefaultNetworkCallback(
                        new ConnectivityManager.NetworkCallback() {

                            @Override
                            public void onCapabilitiesChanged(
                                    @NonNull Network network,
                                    @NonNull NetworkCapabilities caps) {

                                isOnline = isValidated(caps);
                            }

                            @Override
                            public void onLost(@NonNull Network network) {
                                Network currentActive = cm.getActiveNetwork();

                                if (currentActive == null) {
                                    isOnline = false;
                                    return;
                                }

                                NetworkCapabilities caps =
                                        cm.getNetworkCapabilities(currentActive);

                                isOnline = isValidated(caps);
                            }
                        }
                );

            } catch (Exception ignored) {
                isOnline = false;
            }
        });
    }

    private static boolean isValidated(NetworkCapabilities caps) {
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    public static boolean isOnline() {
        return isOnline;
    }
}
