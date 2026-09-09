package com.smdey.ads.views;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.smdey.ads.R;
import com.smdey.ads.core.AdsFacade;
import com.smdey.ads.core.LifecycleGuard;
import com.smdey.ads.managers.BannerManager;

/**
 * Self-managing banner layout with skeleton shimmer loading and adaptive height reservation.
 */
public final class SmartBannerView extends FrameLayout implements DefaultLifecycleObserver {

    private static final Object LOAD_TOKEN = new Object();
    private static final Object APP_OPEN_BRIDGE_TOKEN = new Object();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ViewGroup adContainer;
    private ShimmerFrameLayout shimmerView;
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
            showLoadingState();
        }
    };

    private boolean isCollapsible = false;

    public SmartBannerView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public SmartBannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SmartBannerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            android.content.res.TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SmartBannerView);
            try {
                isCollapsible = a.getBoolean(R.styleable.SmartBannerView_ads_collapsible, false);
            } finally {
                a.recycle();
            }
        }

        LayoutInflater.from(context).inflate(R.layout.layout_banner_ad, this, true);

        if (isInEditMode()) {
            return;
        }

        adContainer = findViewById(R.id.adContainer);
        shimmerView = findViewById(R.id.shimmerContainer);

        Activity activity = getActivity(context);
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().addObserver(this);
        }

        if (AdsFacade.isInitialized()) {
            bannerManager = AdsFacade.getInstance().banner();
        }

        if (activity != null && bannerManager != null) {
            applyDynamicHeight(activity);
        }

        setVisibility(VISIBLE);
        showLoadingState();
    }

    public void setBannerManager(@NonNull BannerManager bannerManager) {
        this.bannerManager = bannerManager;
        Activity activity = getActivity(getContext());
        if (activity != null) {
            applyDynamicHeight(activity);
        }
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        resolveBannerManagerIfNeeded();
        if (bannerManager == null) {
            return;
        }

        Activity activity = getActivity(getContext());
        if (!LifecycleGuard.isActivityValid(activity)) {
            hideAll();
            return;
        }

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
            if (!LifecycleGuard.isActivityValid(activity)) {
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

    private void resolveBannerManagerIfNeeded() {
        if (bannerManager == null && AdsFacade.isInitialized()) {
            bannerManager = AdsFacade.getInstance().banner();
        }
    }

    private void scheduleAttachOrLoad() {
        mainHandler.removeCallbacksAndMessages(LOAD_TOKEN);
        mainHandler.postDelayed(this::attachOrLoadNow, LOAD_TOKEN, 300L);
    }

    private void attachOrLoadNow() {
        if (adContainer == null) {
            return;
        }

        resolveBannerManagerIfNeeded();
        if (bannerManager == null) {
            return;
        }

        Activity currentActivity = getActivity(getContext());
        if (!LifecycleGuard.isActivityValid(currentActivity)) {
            hideAll();
            return;
        }

        bannerManager.attachOrLoad(currentActivity, adContainer, isCollapsible, hostCallback);
    }

    public void setCollapsible(boolean collapsible) {
        this.isCollapsible = collapsible;
    }

    public boolean isCollapsible() {
        return isCollapsible;
    }

    private void applyDynamicHeight(@NonNull Activity activity) {
        if (bannerManager == null) {
            return;
        }

        // If collapsible banner is requested (via attribute or global config), container must remain WRAP_CONTENT to allow expansion
        if (isCollapsible || (AdsFacade.isInitialized() && AdsFacade.getInstance().getConfig().isCollapsibleBannerEnabled())) {
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            if (layoutParams != null) {
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                setLayoutParams(layoutParams);
            }
            return;
        }

        int heightPx = bannerManager.getBannerHeightPx(activity);
        if (heightPx <= 0) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            layoutParams.height = heightPx;
            setLayoutParams(layoutParams);
        }

        if (shimmerView != null && shimmerView.getLayoutParams() != null) {
            ViewGroup.LayoutParams shimmerLayoutParams = shimmerView.getLayoutParams();
            shimmerLayoutParams.height = heightPx;
            shimmerView.setLayoutParams(shimmerLayoutParams);
        }
    }

    private void showLoadingState() {
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

    private void showBanner() {
        setVisibility(VISIBLE);
        if (shimmerView != null) {
            shimmerView.stopShimmer();
            shimmerView.setVisibility(GONE);
        }
        if (adContainer != null) {
            adContainer.setVisibility(VISIBLE);
        }
    }

    private void hideAll() {
        setVisibility(GONE);
        if (adContainer != null) {
            adContainer.setVisibility(GONE);
        }
        if (shimmerView != null) {
            shimmerView.stopShimmer();
            shimmerView.setVisibility(GONE);
        }
    }

    @Nullable
    private Activity getActivity(@NonNull Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }
}
