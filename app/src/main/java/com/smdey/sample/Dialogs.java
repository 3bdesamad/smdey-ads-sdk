package com.smdey.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.smdey.sample.databinding.DialogLoadingAdBinding;

public class Dialogs {

    private static final float DIM_DARK = 0.15f;
    private static final float DIM_LIGHT = 1f;
    private static String[] cachedAdPhrases = null;
    private static final java.util.Random random = new java.util.Random();

    public static AlertDialog dialogLoadingAd(Activity activity) {
        if (!isValidActivity(activity)) return null;

        DialogLoadingAdBinding binding = DialogLoadingAdBinding.inflate(activity.getLayoutInflater());

        // Optimized: Zero-allocation and cached resource lookup
        try {
            if (cachedAdPhrases == null) {
                cachedAdPhrases = activity.getResources().getStringArray(R.array.ad_placeholder_phrases);
            }
            if (cachedAdPhrases.length > 0) {
                binding.title.setText(cachedAdPhrases[random.nextInt(cachedAdPhrases.length)]);
            }
        } catch (Exception e) {
            binding.title.setText(R.string.loading_ad);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(binding.getRoot());
        AlertDialog dialog = builder.create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        dimBackground(activity, DIM_DARK);
        dialog.setOnDismissListener(dialogOnClick -> restoreBackground(activity));

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    @Nullable
    public static AlertDialog showLoadingAd(Activity activity) {
        if (!isValidActivity(activity)) return null;
        AlertDialog dialog = dialogLoadingAd(activity);
        if (dialog != null) {
            try {
                dialog.show();
            } catch (Exception ignored) {
            }
        }
        return dialog;
    }

    public static void sampleDialog(Context context, @DrawableRes int iconRes, int iconSizeDp,
                                    @ColorRes int iconColorRes, @ColorRes int backgroundColorRes,
                                    String title, String msg, String posText,
                                    DialogInterface.OnClickListener onPos, String negText) {
        // 1. Safety Check
        if (context instanceof Activity && !isValidActivity((Activity) context)) return;
        // 2. Pre-load resources
        final int bgColor = backgroundColorRes != 0 ? ContextCompat.getColor(context, backgroundColorRes) : 0;
        final int iconColor = iconColorRes != 0 ? ContextCompat.getColor(context, iconColorRes) : 0;
        // 3. Build Dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(posText, onPos)
                .setNegativeButton(negText, null);

        // Add icon only if provided
        if (iconRes != 0) {
            builder.setIcon(iconRes);
        }

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // 4. Configure Window
        Window window = dialog.getWindow();
        if (window != null) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            // Set Custom Background
            if (bgColor != 0) {
                window.setBackgroundDrawable(createOptimizedBackground(bgColor, metrics.density));
            }
            // Set Width
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (metrics.widthPixels * 0.85f);
            window.setAttributes(params);
        }
        // 5. Show
        dialog.show();
        // 6. Configure Icon (only if icon was set)
        if (iconRes != 0) {
            ImageView iconView = dialog.findViewById(android.R.id.icon);
            if (iconView != null) {
                // Apply size if specified
                if (iconSizeDp > 0) {
                    int sizeInPx = (int) (iconSizeDp * context.getResources().getDisplayMetrics().density);
                    ViewGroup.LayoutParams params = iconView.getLayoutParams();
                    params.width = sizeInPx;
                    params.height = sizeInPx;
                    iconView.setLayoutParams(params);
                    iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }
                // Apply tint
                if (iconColor != 0) {
                    iconView.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                }
            }
        }
    }

    private static MaterialShapeDrawable createOptimizedBackground(int color, float density) {
        MaterialShapeDrawable drawable = new MaterialShapeDrawable();
        drawable.setShapeAppearanceModel(ShapeAppearanceModel.builder()
                .setAllCornerSizes(6f * density)
                .build());
        drawable.setFillColor(ColorStateList.valueOf(color));
        drawable.setElevation(24f * density);
        return drawable;
    }

    public static void dimBackground(Activity activity, float dim) {
        if (!isValidActivity(activity)) return;
        WindowManager.LayoutParams layoutParams = activity.getWindow().getAttributes();
        layoutParams.alpha = dim;
        activity.getWindow().setAttributes(layoutParams);
    }

    public static void restoreBackground(Activity activity) {
        if (!isValidActivity(activity)) return;
        WindowManager.LayoutParams layoutParams = activity.getWindow().getAttributes();
        layoutParams.alpha = 1.0f;
        activity.getWindow().setAttributes(layoutParams);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isValidActivity(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }
}
