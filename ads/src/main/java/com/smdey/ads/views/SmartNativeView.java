package com.smdey.ads.views;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.smdey.ads.R;
import com.smdey.ads.callbacks.OnNativeAdLoadedListener;
import com.smdey.ads.core.AdsFacade;
import com.smdey.ads.core.LifecycleGuard;
import com.smdey.ads.core.SdkGate;
import com.smdey.ads.managers.NativeManager;

/**
 * Self-managing Native Ad container with customizable Small and Medium templates,
 * automatic shimmer skeleton loading, and strict lifecycle memory management.
 */
public final class SmartNativeView extends FrameLayout implements DefaultLifecycleObserver {

    public enum Template {
        SMALL,
        MEDIUM
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Template template = Template.SMALL;
    private boolean autoLoad = true;
    private boolean isAdLoaded = false;
    private boolean isLoading = false;

    @Nullable
    private ShimmerFrameLayout shimmerView;
    @Nullable
    private NativeAd currentNativeAd;
    @Nullable
    private NativeManager nativeManager;

    public SmartNativeView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public SmartNativeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SmartNativeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SmartNativeView);
            try {
                int templateVal = a.getInt(R.styleable.SmartNativeView_ads_template, 0);
                template = templateVal == 1 ? Template.MEDIUM : Template.SMALL;
                autoLoad = a.getBoolean(R.styleable.SmartNativeView_ads_auto_load, true);
            } finally {
                a.recycle();
            }
        }

        if (isInEditMode()) {
            return;
        }

        Activity activity = getActivity(context);
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().addObserver(this);
        }

        if (AdsFacade.isInitialized()) {
            nativeManager = AdsFacade.getInstance().nativeAd();
        }

        inflateSkeleton();
    }

    public void setTemplate(@NonNull Template template) {
        if (this.template != template) {
            this.template = template;
            if (!isAdLoaded) {
                inflateSkeleton();
            } else if (currentNativeAd != null) {
                bindNativeAd(currentNativeAd);
            }
        }
    }

    @NonNull
    public Template getTemplate() {
        return template;
    }

    public void setAutoLoad(boolean autoLoad) {
        this.autoLoad = autoLoad;
    }

    public boolean isAdLoaded() {
        return isAdLoaded;
    }

    public void setNativeManager(@NonNull NativeManager nativeManager) {
        this.nativeManager = nativeManager;
    }

    private void inflateSkeleton() {
        removeAllViews();
        int skeletonLayout = template == Template.MEDIUM
                ? R.layout.layout_native_skeleton_medium
                : R.layout.layout_native_skeleton_small;

        View skeletonRoot = LayoutInflater.from(getContext()).inflate(skeletonLayout, this, false);
        addView(skeletonRoot);

        if (skeletonRoot instanceof ShimmerFrameLayout) {
            shimmerView = (ShimmerFrameLayout) skeletonRoot;
            shimmerView.startShimmer();
        }
    }

    public void loadAd() {
        loadAd(null);
    }

    public void loadAd(@Nullable OnNativeAdLoadedListener listener) {
        resolveNativeManagerIfNeeded();
        if (nativeManager == null || nativeManager.isAdsRemoved()) {
            hideAll();
            if (listener != null) listener.onAdFailedToLoad();
            return;
        }

        Activity activity = getActivity(getContext());
        Context targetContext = activity != null ? activity : getContext();

        if (activity != null && !LifecycleGuard.isActivityValid(activity)) {
            hideAll();
            if (listener != null) listener.onAdFailedToLoad();
            return;
        }

        if (isLoading) {
            return;
        }

        isLoading = true;
        showLoadingState();

        nativeManager.loadAd(targetContext, new OnNativeAdLoadedListener() {
            @Override
            public void onAdLoaded(@NonNull NativeAd nativeAd) {
                isLoading = false;
                Activity currentActivity = getActivity(getContext());
                if (currentActivity != null && !LifecycleGuard.isActivityValid(currentActivity)) {
                    nativeAd.destroy();
                    return;
                }
                setNativeAd(nativeAd);
                if (listener != null) {
                    listener.onAdLoaded(nativeAd);
                }
            }

            @Override
            public void onAdFailedToLoad() {
                isLoading = false;
                hideAll();
                if (listener != null) {
                    listener.onAdFailedToLoad();
                }
            }
        });
    }

    public void setNativeAd(@NonNull NativeAd nativeAd) {
        destroyCurrentAd();
        this.currentNativeAd = nativeAd;
        this.isAdLoaded = true;
        bindNativeAd(nativeAd);
    }

    private void bindNativeAd(@NonNull NativeAd nativeAd) {
        removeAllViews();
        if (shimmerView != null) {
            shimmerView.stopShimmer();
            shimmerView = null;
        }

        int layoutRes = template == Template.MEDIUM
                ? R.layout.layout_native_ad_medium
                : R.layout.layout_native_ad_small;

        NativeAdView nativeAdView = (NativeAdView) LayoutInflater.from(getContext()).inflate(layoutRes, this, false);

        // 1. Headline
        TextView headlineView = nativeAdView.findViewById(R.id.ad_headline);
        if (headlineView != null) {
            if (nativeAd.getHeadline() != null && !nativeAd.getHeadline().isEmpty()) {
                headlineView.setText(nativeAd.getHeadline());
                headlineView.setVisibility(VISIBLE);
                nativeAdView.setHeadlineView(headlineView);
            } else {
                headlineView.setVisibility(GONE);
            }
        }

        // 2. Call to action button
        View ctaView = nativeAdView.findViewById(R.id.ad_call_to_action);
        if (ctaView instanceof TextView) {
            TextView ctaTextView = (TextView) ctaView;
            if (nativeAd.getCallToAction() != null && !nativeAd.getCallToAction().isEmpty()) {
                ctaTextView.setText(nativeAd.getCallToAction());
                ctaTextView.setVisibility(VISIBLE);
                nativeAdView.setCallToActionView(ctaTextView);
            } else {
                ctaTextView.setVisibility(GONE);
            }
        }

        // 3. Icon
        ImageView iconView = nativeAdView.findViewById(R.id.ad_app_icon);
        if (iconView != null) {
            NativeAd.Image icon = nativeAd.getIcon();
            if (icon != null && icon.getDrawable() != null) {
                iconView.setImageDrawable(icon.getDrawable());
                iconView.setVisibility(VISIBLE);
                nativeAdView.setIconView(iconView);
            } else {
                iconView.setVisibility(GONE);
            }
        }

        // 4. Body
        TextView bodyView = nativeAdView.findViewById(R.id.ad_body);
        if (bodyView != null) {
            if (nativeAd.getBody() != null && !nativeAd.getBody().isEmpty()) {
                bodyView.setText(nativeAd.getBody());
                bodyView.setVisibility(VISIBLE);
                nativeAdView.setBodyView(bodyView);
            } else {
                bodyView.setVisibility(GONE);
            }
        }

        // 5. Advertiser
        TextView advertiserView = nativeAdView.findViewById(R.id.ad_advertiser);
        if (advertiserView != null) {
            if (nativeAd.getAdvertiser() != null && !nativeAd.getAdvertiser().isEmpty()) {
                advertiserView.setText(nativeAd.getAdvertiser());
                advertiserView.setVisibility(VISIBLE);
                nativeAdView.setAdvertiserView(advertiserView);
            } else {
                advertiserView.setVisibility(GONE);
            }
        }

        // 6. Star Rating Bar
        android.widget.RatingBar ratingBar = nativeAdView.findViewById(R.id.ad_stars);
        if (ratingBar != null) {
            if (nativeAd.getStarRating() != null && nativeAd.getStarRating() > 0) {
                ratingBar.setRating(nativeAd.getStarRating().floatValue());
                ratingBar.setVisibility(VISIBLE);
                nativeAdView.setStarRatingView(ratingBar);
            } else {
                ratingBar.setVisibility(GONE);
            }
        }

        // 6. MediaView (for Medium template)
        MediaView mediaView = nativeAdView.findViewById(R.id.ad_media);
        if (mediaView != null) {
            if (nativeAd.getMediaContent() != null) {
                mediaView.setMediaContent(nativeAd.getMediaContent());
                mediaView.setVisibility(VISIBLE);
                nativeAdView.setMediaView(mediaView);
            } else {
                mediaView.setVisibility(GONE);
            }
        }

        // Register NativeAd with view
        nativeAdView.setNativeAd(nativeAd);

        addView(nativeAdView);
        setVisibility(VISIBLE);
        Log.i(SdkGate.TAG, "✅ SmartNativeView - Native ad bound successfully (" + template.name() + ").");
    }

    private void showLoadingState() {
        setVisibility(VISIBLE);
        if (!isAdLoaded) {
            if (shimmerView == null) {
                inflateSkeleton();
            } else {
                shimmerView.startShimmer();
            }
        }
    }

    private void hideAll() {
        if (shimmerView != null) {
            shimmerView.stopShimmer();
        }
        setVisibility(GONE);
    }

    private void resolveNativeManagerIfNeeded() {
        if (nativeManager == null && AdsFacade.isInitialized()) {
            nativeManager = AdsFacade.getInstance().nativeAd();
        }
    }

    private void destroyCurrentAd() {
        if (currentNativeAd != null) {
            try {
                currentNativeAd.destroy();
                Log.d(SdkGate.TAG, "♻️ SmartNativeView - Previous NativeAd destroyed.");
            } catch (Exception ignored) {
            }
            currentNativeAd = null;
        }
        isAdLoaded = false;
    }

    public void destroy() {
        destroyCurrentAd();
        removeAllViews();
        if (shimmerView != null) {
            shimmerView.stopShimmer();
            shimmerView = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (autoLoad && !isAdLoaded) {
            mainHandler.postDelayed(this::loadAd, 300L);
        }
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (autoLoad && !isAdLoaded) {
            mainHandler.postDelayed(this::loadAd, 300L);
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (shimmerView != null) {
            shimmerView.stopShimmer();
        }
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        owner.getLifecycle().removeObserver(this);
        mainHandler.removeCallbacksAndMessages(null);
        destroy();
    }

    @Nullable
    private Activity getActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
