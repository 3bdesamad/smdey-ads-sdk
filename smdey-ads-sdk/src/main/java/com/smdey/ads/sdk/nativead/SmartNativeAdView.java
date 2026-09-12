package com.smdey.ads.sdk.nativead;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView;
import com.smdey.ads.sdk.AdsSdk;
import com.smdey.ads.sdk.R;
import com.smdey.ads.sdk.banner.ShimmerSkeletonView;
import com.smdey.ads.sdk.core.LifecycleGuard;

public final class SmartNativeAdView extends FrameLayout implements DefaultLifecycleObserver {

    private ShimmerSkeletonView skeletonView;
    private NativeAd currentNativeAd;
    private NativeAdView currentNativeAdView;
    private View currentRootView;
    private int adLayoutResId = 0;
    private boolean isAttachedToLifecycle = false;

    public SmartNativeAdView(@NonNull Context context) {
        this(context, null);
    }

    public SmartNativeAdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmartNativeAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SmartNativeAdView);
            adLayoutResId = a.getResourceId(R.styleable.SmartNativeAdView_ad_layout, 0);
            a.recycle();
        }

        // 1. Create native hardware-accelerated GPU pulse skeleton
        skeletonView = new ShimmerSkeletonView(context);
        int defaultHeight = (int) (70 * context.getResources().getDisplayMetrics().density);
        addView(skeletonView, new LayoutParams(LayoutParams.MATCH_PARENT, defaultHeight));

        if (isInEditMode()) {
            return;
        }

        // 2. Bind to Activity lifecycle
        bindLifecycle(context);

        // 3. Auto-load if ad_layout was specified in XML
        if (adLayoutResId != 0) {
            post(this::loadAd);
        }
    }

    private void bindLifecycle(Context context) {
        if (isAttachedToLifecycle) {
            return;
        }
        Activity activity = getActivity(context);
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().addObserver(this);
            isAttachedToLifecycle = true;
        }
    }

    public void loadAd() {
        if (adLayoutResId != 0) {
            loadAd(adLayoutResId);
        }
    }

    public void loadAd(@LayoutRes int layoutResId) {
        this.adLayoutResId = layoutResId;

        if (!AdsSdk.isInitialized()) {
            return;
        }

        if (AdsSdk.getInstance().isAdsRemoved()) {
            hideAll();
            return;
        }

        Activity activity = getActivity(getContext());
        if (!LifecycleGuard.isActivityValid(activity)) {
            hideAll();
            return;
        }

        // Show pulse skeleton while loading
        if (currentRootView != null) {
            currentRootView.setVisibility(GONE);
        }
        setVisibility(VISIBLE);
        skeletonView.setVisibility(VISIBLE);
        skeletonView.startShimmer();

        AdsSdk.getInstance().nativeAd().load(activity, new NativeAdManager.OnNativeAdLoadedListener() {
            @Override
            public void onAdLoaded(@NonNull NativeAd nativeAd) {
                if (!LifecycleGuard.isActivityValid(activity)) {
                    nativeAd.destroy();
                    return;
                }
                displayAd(nativeAd);
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError error) {
                if (!LifecycleGuard.isActivityValid(activity)) {
                    return;
                }
                if (currentNativeAd == null) {
                    hideAll();
                }
            }
        });
    }

    private void displayAd(@NonNull NativeAd nativeAd) {
        if (adLayoutResId == 0) {
            nativeAd.destroy();
            hideAll();
            return;
        }

        // 1. Prevent memory leaks: destroy previous ad
        destroyCurrentAd();
        currentNativeAd = nativeAd;

        // 2. Remove previous ad view from layout
        if (currentRootView != null) {
            removeView(currentRootView);
            currentRootView = null;
        }

        // 3. Inflate host app's XML template
        View inflated = LayoutInflater.from(getContext()).inflate(adLayoutResId, this, false);

        NativeAdView nativeAdView;
        if (inflated instanceof NativeAdView) {
            nativeAdView = (NativeAdView) inflated;
        } else {
            nativeAdView = findChildNativeAdView(inflated);
        }

        if (nativeAdView != null) {
            NativeAdBinder.bind(nativeAdView, nativeAd);
            currentNativeAdView = nativeAdView;
        }

        currentRootView = inflated;
        addView(currentRootView);

        // 4. Reveal ad and hide skeleton
        skeletonView.stopShimmer();
        skeletonView.setVisibility(GONE);
        currentRootView.setVisibility(VISIBLE);
        setVisibility(VISIBLE);
    }

    private NativeAdView findChildNativeAdView(View root) {
        int id = getContext().getResources().getIdentifier("nativeAdView", "id", getContext().getPackageName());
        if (id != 0) {
            View v = root.findViewById(id);
            if (v instanceof NativeAdView) {
                return (NativeAdView) v;
            }
        }
        return null;
    }

    public void hideAll() {
        if (skeletonView != null) {
            skeletonView.stopShimmer();
            skeletonView.setVisibility(GONE);
        }
        if (currentRootView != null) {
            currentRootView.setVisibility(GONE);
        }
        setVisibility(GONE);
    }

    private void destroyCurrentAd() {
        if (currentNativeAdView != null) {
            currentNativeAdView.destroy();
            currentNativeAdView = null;
        }
        if (currentNativeAd != null) {
            currentNativeAd.destroy();
            currentNativeAd = null;
        }
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        destroyCurrentAd();
        if (skeletonView != null) {
            skeletonView.stopShimmer();
        }
        owner.getLifecycle().removeObserver(this);
        isAttachedToLifecycle = false;
    }

    private static Activity getActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
