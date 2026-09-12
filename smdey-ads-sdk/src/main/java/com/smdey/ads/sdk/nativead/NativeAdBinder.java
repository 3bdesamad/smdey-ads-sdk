package com.smdey.ads.sdk.nativead;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.libraries.ads.mobile.sdk.common.Image;
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NativeAdBinder {

    /**
     * Fast O(1) thread-safe cache for resolved layout resource IDs.
     * Eliminates repeated string table lookups via getIdentifier() on low-end devices.
     */
    private static final Map<String, Integer> RES_ID_CACHE = new ConcurrentHashMap<>();

    private NativeAdBinder() {}

    /**
     * Binds Next-Gen NativeAd to the host app's inflated NativeAdView.
     * Searches dynamically for standard view IDs (ad_headline, ad_body, ad_call_to_action,
     * ad_app_icon, ad_stars, ad_advertiser, ad_media) without requiring static R.id dependencies.
     */
    public static void bind(@NonNull NativeAdView nativeAdView, @NonNull NativeAd nativeAd) {
        Context context = nativeAdView.getContext();

        // 1. Headline
        TextView headlineView = findView(nativeAdView, context, "ad_headline");
        if (headlineView != null) {
            headlineView.setText(nativeAd.getHeadline());
            nativeAdView.setHeadlineView(headlineView);
        }

        // 2. Call to Action Button
        View ctaView = findView(nativeAdView, context, "ad_call_to_action");
        if (ctaView != null) {
            if (ctaView instanceof TextView) {
                ((TextView) ctaView).setText(nativeAd.getCallToAction());
            }
            nativeAdView.setCallToActionView(ctaView);
        }

        // 3. App Icon
        ImageView iconView = findView(nativeAdView, context, "ad_app_icon");
        if (iconView != null) {
            Image icon = nativeAd.getIcon();
            if (icon != null && icon.getDrawable() != null) {
                iconView.setImageDrawable(icon.getDrawable());
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }
            nativeAdView.setIconView(iconView);
        }

        // 4. Body Text
        TextView bodyView = findView(nativeAdView, context, "ad_body");
        if (bodyView != null) {
            CharSequence body = nativeAd.getBody();
            if (body != null && body.length() > 0) {
                bodyView.setText(body);
                bodyView.setVisibility(View.VISIBLE);
            } else {
                bodyView.setVisibility(View.GONE);
            }
            nativeAdView.setBodyView(bodyView);
        }

        // 5. Star Rating
        RatingBar starsView = findView(nativeAdView, context, "ad_stars");
        if (starsView != null) {
            Double rating = nativeAd.getStarRating();
            if (rating != null && rating > 0) {
                starsView.setRating(rating.floatValue());
                starsView.setVisibility(View.VISIBLE);
            } else {
                starsView.setVisibility(View.GONE);
            }
            nativeAdView.setStarRatingView(starsView);
        }

        // 6. Advertiser
        TextView advertiserView = findView(nativeAdView, context, "ad_advertiser");
        if (advertiserView != null) {
            CharSequence advertiser = nativeAd.getAdvertiser();
            if (advertiser != null && advertiser.length() > 0) {
                advertiserView.setText(advertiser);
                advertiserView.setVisibility(View.VISIBLE);
            } else {
                advertiserView.setVisibility(View.GONE);
            }
            nativeAdView.setAdvertiserView(advertiserView);
        }

        // 7. MediaView
        MediaView mediaView = findView(nativeAdView, context, "ad_media");

        // 8. Register NativeAd to container
        nativeAdView.registerNativeAd(nativeAd, mediaView);
    }

    @SuppressWarnings("unchecked")
    private static <T extends View> T findView(@NonNull View root, @NonNull Context context, @NonNull String idName) {
        Integer cachedId = RES_ID_CACHE.get(idName);
        int id;
        if (cachedId != null) {
            id = cachedId;
        } else {
            id = context.getResources().getIdentifier(idName, "id", context.getPackageName());
            RES_ID_CACHE.put(idName, id);
        }
        return id != 0 ? (T) root.findViewById(id) : null;
    }
}
