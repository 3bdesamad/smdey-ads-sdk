package com.smdey.ads.sdk.banner;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.smdey.ads.sdk.AdsSdk;
import com.smdey.ads.sdk.core.LifecycleGuard;

public final class SmartBannerView extends FrameLayout implements DefaultLifecycleObserver {

    private static final Object LOAD_TOKEN = new Object();
    private static final Object APP_OPEN_BRIDGE_TOKEN = new Object();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private LinearLayout adContainer;
    private ShimmerFrameLayout shimmerView;
    private View skeletonView;
    private BannerManager bannerManager;
    private boolean appOpenBridgeActive;

    private final BannerManager.HostCallback hostCallback = new BannerManager.HostCallback() {
        @Override
        public void onBannerLoaded() {
            if (appOpenBridgeActive || (bannerManager != null && bannerManager.isAppOpenLocked())) {
                return;
            }
            showBanner();
        }

        @Override
        public void onBannerFailed() {
            hideAll();
        }

        @Override
        public void onBannerHidden() {
            hideAll();
        }

        @Override
        public void onBannerPending() {
            if (isAdsRemovedOrDisabled()) {
                hideAll();
                return;
            }
            showLoadingState();
        }
    };

    public SmartBannerView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SmartBannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SmartBannerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        setBackgroundColor(Color.TRANSPARENT);

        // 1. Shimmer Container (Pure Java, zero XML inflation)
        shimmerView = new ShimmerFrameLayout(context);
        com.facebook.shimmer.Shimmer shimmer = new com.facebook.shimmer.Shimmer.AlphaHighlightBuilder()
                .setDuration(1200L)
                .build();
        shimmerView.setShimmer(shimmer);
        int pad10 = dpToPx(context, 10);
        shimmerView.setPadding(pad10, 0, pad10, 0);

        skeletonView = new View(context);
        skeletonView.setBackground(createDefaultSkeletonDrawable(context));
        shimmerView.addView(skeletonView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        addView(shimmerView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 2. Ad container
        adContainer = new LinearLayout(context);
        adContainer.setOrientation(LinearLayout.VERTICAL);
        adContainer.setVisibility(INVISIBLE);
        addView(adContainer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 3. Bind lifecycle
        Activity activity = getActivity(context);
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().addObserver(this);
        }

        if (AdsSdk.isInitialized()) {
            bannerManager = AdsSdk.getInstance().banner();
        }

        if (isAdsRemovedOrDisabled()) {
            hideAll();
            return;
        }

        if (activity != null) {
            applyDynamicHeight(activity);
        }

        setVisibility(VISIBLE);
        if (bannerManager != null && bannerManager.isBannerLoaded()) {
            if (adContainer != null) {
                adContainer.setVisibility(VISIBLE);
            }
            if (shimmerView != null) {
                shimmerView.setVisibility(GONE);
            }
        } else {
            showLoadingState();
        }
    }

    public void setSkeletonDrawable(@NonNull Drawable drawable) {
        if (skeletonView != null) {
            skeletonView.setBackground(drawable);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isAdsRemovedOrDisabled()) {
            hideAll();
            return;
        }
        Activity activity = getActivity(getContext());
        if (activity != null) {
            applyDynamicHeight(activity);
        }
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (isAdsRemovedOrDisabled()) {
            hideAll();
            return;
        }

        Activity activity = getActivity(getContext());
        if (activity == null) {
            return;
        }

        if (bannerManager == null && AdsSdk.isInitialized()) {
            bannerManager = AdsSdk.getInstance().banner();
        }

        if (bannerManager == null) {
            hideAll();
            return;
        }

        if (!LifecycleGuard.isActivityValid(activity)) {
            hideAll();
            return;
        }

        if (bannerManager.isAppOpenLocked()) {
            appOpenBridgeActive = true;
            hideAll();
            return;
        }

        appOpenBridgeActive = false;
        applyDynamicHeight(activity);

        if (bannerManager.isBannerLoaded()) {
            attachOrLoadNow();
            return;
        }

        scheduleAttachOrLoad();
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        mainHandler.removeCallbacksAndMessages(LOAD_TOKEN);
        mainHandler.removeCallbacksAndMessages(APP_OPEN_BRIDGE_TOKEN);
        if (shimmerView != null) {
            shimmerView.stopShimmer();
        }
        if (bannerManager != null) {
            bannerManager.detachHost(adContainer);
        }
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        owner.getLifecycle().removeObserver(this);
        mainHandler.removeCallbacksAndMessages(null);
        if (bannerManager != null) {
            bannerManager.detachHost(adContainer);
        }
        if (shimmerView != null) {
            shimmerView.stopShimmer();
            shimmerView = null;
        }
        adContainer = null;
    }

    public void onOpenAdShowing() {
        appOpenBridgeActive = false;
        if (bannerManager != null) {
            bannerManager.onAppOpenShowing();
        }
        showHiddenBridge();
    }

    public void onOpenAdDismissed() {
        if (bannerManager != null) {
            bannerManager.onAppOpenDismissed();
        }

        appOpenBridgeActive = true;
        mainHandler.removeCallbacksAndMessages(APP_OPEN_BRIDGE_TOKEN);
        showLoadingState();
        mainHandler.postDelayed(() -> {
            appOpenBridgeActive = false;
            Activity activity = getActivity(getContext());
            if (activity == null || !LifecycleGuard.isActivityValid(activity)) {
                hideAll();
                return;
            }

            if (bannerManager != null && bannerManager.isBannerLoaded()) {
                attachOrLoadNow();
                return;
            }

            scheduleAttachOrLoad();
        }, APP_OPEN_BRIDGE_TOKEN, 1000L);
    }

    private void scheduleAttachOrLoad() {
        scheduleAttachOrLoad(300L);
    }

    private void scheduleAttachOrLoad(long delayMs) {
        if (isAdsRemovedOrDisabled()) {
            hideAll();
            return;
        }
        mainHandler.removeCallbacksAndMessages(LOAD_TOKEN);
        mainHandler.postDelayed(() -> {
            if (isAdsRemovedOrDisabled()) {
                hideAll();
                return;
            }
            Activity currentActivity = getActivity(getContext());
            if (currentActivity == null || !LifecycleGuard.isActivityValid(currentActivity)) {
                hideAll();
                return;
            }
            attachOrLoadNow();
        }, LOAD_TOKEN, delayMs);
    }

    private void attachOrLoadNow() {
        if (isAdsRemovedOrDisabled()) {
            hideAll();
            return;
        }

        if (adContainer == null || bannerManager == null) {
            return;
        }

        Activity currentActivity = getActivity(getContext());
        if (currentActivity == null || !LifecycleGuard.isActivityValid(currentActivity)) {
            hideAll();
            return;
        }

        bannerManager.attachOrLoad(currentActivity, adContainer, hostCallback);
    }

    private void applyDynamicHeight(@NonNull Activity activity) {
        if (bannerManager == null) {
            return;
        }

        int heightPx = bannerManager.getBannerHeightPx(activity);
        if (heightPx <= 0) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null && layoutParams.height != heightPx) {
            layoutParams.height = heightPx;
            setLayoutParams(layoutParams);
        }

        if (shimmerView != null && shimmerView.getLayoutParams() != null) {
            ViewGroup.LayoutParams shimmerLayoutParams = shimmerView.getLayoutParams();
            if (shimmerLayoutParams.height != heightPx) {
                shimmerLayoutParams.height = heightPx;
                shimmerView.setLayoutParams(shimmerLayoutParams);
            }
        }
    }

    public void showLoadingState() {
        if (isAdsRemovedOrDisabled()) {
            hideAll();
            return;
        }
        setVisibility(VISIBLE);
        if (adContainer != null) {
            adContainer.setVisibility(INVISIBLE);
        }
        if (shimmerView != null) {
            shimmerView.setVisibility(VISIBLE);
            shimmerView.startShimmer();
        }
    }

    private void showHiddenBridge() {
        setVisibility(INVISIBLE);
        if (adContainer != null) {
            adContainer.setVisibility(INVISIBLE);
        }
        if (shimmerView != null) {
            shimmerView.stopShimmer();
            shimmerView.setVisibility(INVISIBLE);
        }
    }

    public void showBanner() {
        setVisibility(VISIBLE);
        if (shimmerView != null) {
            shimmerView.stopShimmer();
            shimmerView.setVisibility(GONE);
        }
        if (adContainer != null) {
            adContainer.setVisibility(VISIBLE);
        }
    }

    public void hideAll() {
        setVisibility(GONE);
        if (adContainer != null) {
            adContainer.setVisibility(GONE);
        }
        if (shimmerView != null) {
            shimmerView.stopShimmer();
            shimmerView.setVisibility(GONE);
        }
    }

    private boolean isAdsRemovedOrDisabled() {
        if (!AdsSdk.isInitialized()) {
            return false;
        }
        com.smdey.ads.sdk.AdsConfig cfg = AdsSdk.getInstance().getConfig();
        return cfg.isAdsRemoved() || !cfg.isBannerEnabled() || cfg.getBannerId() == null || cfg.getBannerId().isEmpty();
    }

    @Nullable
    private Activity getActivity(@Nullable Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private static int dpToPx(@NonNull Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    @NonNull
    private static Drawable createDefaultSkeletonDrawable(@NonNull Context context) {
        int color = 0xFFDDDDDD; // Same #DDDDDD as XML

        // 1. Icon block (Left square 40dp x 40dp, radius 4dp)
        GradientDrawable icon = new GradientDrawable();
        icon.setColor(color);
        icon.setCornerRadius(dpToPx(context, 4));

        // 2. Top bar (height 12dp, radius 2dp)
        GradientDrawable topBar = new GradientDrawable();
        topBar.setColor(color);
        topBar.setCornerRadius(dpToPx(context, 2));

        // 3. Bottom bar (width 140dp, height 12dp, radius 2dp)
        GradientDrawable bottomBar = new GradientDrawable();
        bottomBar.setColor(color);
        bottomBar.setCornerRadius(dpToPx(context, 2));

        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{icon, topBar, bottomBar});

        // Layer 0: Icon
        layerDrawable.setLayerSize(0, dpToPx(context, 40), dpToPx(context, 40));
        layerDrawable.setLayerGravity(0, Gravity.START | Gravity.CENTER_VERTICAL);
        layerDrawable.setLayerInsetStart(0, dpToPx(context, 12));

        // Layer 1: Top Bar
        layerDrawable.setLayerHeight(1, dpToPx(context, 12));
        layerDrawable.setLayerGravity(1, Gravity.TOP | Gravity.START);
        layerDrawable.setLayerInsetStart(1, dpToPx(context, 64));
        layerDrawable.setLayerInsetEnd(1, dpToPx(context, 12));
        layerDrawable.setLayerInsetTop(1, dpToPx(context, 14));

        // Layer 2: Bottom Bar
        layerDrawable.setLayerWidth(2, dpToPx(context, 140));
        layerDrawable.setLayerHeight(2, dpToPx(context, 12));
        layerDrawable.setLayerGravity(2, Gravity.BOTTOM | Gravity.START);
        layerDrawable.setLayerInsetStart(2, dpToPx(context, 64));
        layerDrawable.setLayerInsetBottom(2, dpToPx(context, 14));

        return layerDrawable;
    }
}
