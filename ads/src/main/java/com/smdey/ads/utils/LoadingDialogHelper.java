package com.smdey.ads.utils;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.smdey.ads.R;
import com.smdey.ads.callbacks.LoadingDialogProvider;
import com.smdey.ads.core.LifecycleGuard;
import com.smdey.ads.core.SdkGate;

/**
 * Reusable helper for displaying and safely dismissing ad loading dialogs.
 */
public final class LoadingDialogHelper {

    private LoadingDialogHelper() {
        // Private constructor for utility class
    }

    @Nullable
    public static Dialog showLoadingDialog(@NonNull Activity activity,
                                           @Nullable LoadingDialogProvider customProvider) {
        if (!LifecycleGuard.isActivityValid(activity)) {
            return null;
        }

        Dialog dialog = null;
        if (customProvider != null) {
            dialog = customProvider.createLoadingDialog(activity);
        }

        if (dialog == null) {
            dialog = createDefaultLoadingDialog(activity);
        }

        try {
            if (dialog != null && LifecycleGuard.isActivityValid(activity)) {
                dialog.show();
                return dialog;
            }
        } catch (Exception e) {
            Log.e(SdkGate.TAG, "❌ LoadingDialogHelper - Error showing dialog: " + e.getMessage());
        }
        return null;
    }

    @NonNull
    public static Dialog createDefaultLoadingDialog(@NonNull Activity activity) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_loading_ad, null);
        dialog.setContentView(view);
        dialog.setCancelable(false);

        setupDialogWindowBounds(dialog, activity);
        return dialog;
    }

    public static void setupDialogWindowBounds(@NonNull Dialog dialog, @NonNull Activity activity) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }

        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setGravity(Gravity.CENTER);

        int marginPx = activity.getResources().getDimensionPixelSize(R.dimen.ads_dialog_margin_h);
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int maxDialogWidthPx = (int) (380 * activity.getResources().getDisplayMetrics().density);

        int dialogWidth = Math.min(screenWidth - (2 * marginPx), maxDialogWidthPx);
        if (dialogWidth <= 0) {
            dialogWidth = WindowManager.LayoutParams.WRAP_CONTENT;
        }

        window.setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    public static void dismissSafely(@Nullable Dialog dialog) {
        if (dialog != null) {
            try {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
