package com.smdey.sample;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.smdey.ads.sdk.AdsSdk;
import com.smdey.ads.sdk.banner.SmartBannerView;
import com.smdey.ads.sdk.callbacks.OpenAdVisibilityControl;

public class BaseActivity extends AppCompatActivity implements OpenAdVisibilityControl {

    private WindowInsetsControllerCompat windowInsetsController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        applyThemeToSystemBarsAndStatusBar();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        applyEdgeToEdge();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applyEdgeToEdge();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        applyEdgeToEdge();
    }

    private void applyEdgeToEdge() {
        View decorView = getWindow().getDecorView();
        final View incToolbar = findViewById(R.id.incToolbar);

        View rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout == null) {
            rootLayout = findViewById(android.R.id.content);
        }
        final View finalRootLayout = rootLayout;

        final int typeMask = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();

        ViewCompat.setOnApplyWindowInsetsListener(decorView, (v, windowInsets) -> {

            Insets insets = windowInsets.getInsets(typeMask);

            // 1. Top: Add padding to AppBar
            if (incToolbar != null) {
                incToolbar.setPadding(
                        incToolbar.getPaddingLeft(),
                        insets.top,
                        incToolbar.getPaddingRight(),
                        incToolbar.getPaddingBottom()
                );
            }

            // 2. Root: Add padding to Bottom, Left, Right
            if (finalRootLayout != null) {
                finalRootLayout.setPadding(
                        insets.left,
                        finalRootLayout.getPaddingTop(),
                        insets.right,
                        insets.bottom
                );
            }

            return windowInsets;
        });
    }

    private void applyThemeToSystemBarsAndStatusBar() {
        if (windowInsetsController == null) return;
        boolean isLight = !isThemeDark();
        windowInsetsController.setAppearanceLightNavigationBars(isLight);
        windowInsetsController.setAppearanceLightStatusBars(isLight);
    }

    protected void applyLightThemeToSystemBarsAndStatusBar() {
        if (windowInsetsController == null) return;
        windowInsetsController.setAppearanceLightNavigationBars(true);
        windowInsetsController.setAppearanceLightStatusBars(true);
    }

    private boolean isThemeDark() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    @NonNull
    public AdsSdk getAds() {
        return AdsSdk.getInstance();
    }

    public boolean isSafeToUpdate() {
        return !isFinishing() && !isDestroyed();
    }

    @Override
    public void hideBannerAd() {
        View bannerHost = findViewById(R.id.bannerView);
        if (bannerHost instanceof SmartBannerView) {
            ((SmartBannerView) bannerHost).onOpenAdShowing();
        } else if (bannerHost != null) {
            bannerHost.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void showBannerAd() {
        View bannerHost = findViewById(R.id.bannerView);
        if (bannerHost instanceof SmartBannerView) {
            ((SmartBannerView) bannerHost).onOpenAdDismissed();
        } else if (bannerHost != null) {
            bannerHost.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean canShowOpenAd() {
        return true;
    }
}
