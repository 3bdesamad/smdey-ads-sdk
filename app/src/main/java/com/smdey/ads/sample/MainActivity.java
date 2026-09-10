package com.smdey.ads.sample;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.smdey.ads.callbacks.OpenAdVisibilityControl;
import com.smdey.ads.core.AdsFacade;
import com.smdey.ads.sample.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements OpenAdVisibilityControl {

    private ActivityMainBinding binding;
    private boolean isAdsRemoved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupEdgeToEdgeInsets();

        // Mark startup stable & schedule App Open preload
        AdsFacade.getInstance().markStartupStable(this);
        AdsFacade.getInstance().appOpen().setAdVisibilityControl(this);

        // Display current Grace Period / Ad status
        isAdsRemoved = AdsFacade.getInstance().isAdsRemoved();
        binding.tvStatus.setText(AdsFacade.getInstance().getGracePeriodStatusSummary());

        initListeners();
    }

    private void setupEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );

            // Maintain exact 56dp action bar content height plus status bar inset
            int actionBarHeight = getActionBarHeight();
            ViewGroup.LayoutParams lp = binding.incToolbar.toolbar.getLayoutParams();
            if (lp != null) {
                lp.height = insets.top + actionBarHeight;
                binding.incToolbar.toolbar.setLayoutParams(lp);
            }

            // Apply top status bar inset directly to the toolbar so background seamlessly fills behind status bar
            binding.incToolbar.toolbar.setPadding(
                    binding.incToolbar.toolbar.getPaddingLeft(),
                    insets.top,
                    binding.incToolbar.toolbar.getPaddingRight(),
                    binding.incToolbar.toolbar.getPaddingBottom()
            );
            // Apply bottom inset so the anchored bottom banner stays comfortably above navigation bar
            binding.getRoot().setPadding(insets.left, 0, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private int getActionBarHeight() {
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        return (int) (56 * getResources().getDisplayMetrics().density);
    }

    private void initListeners() {
        binding.btnConsent.setOnClickListener(v -> {
            AdsFacade.getInstance().ensureConsentThenRun(this, () -> runOnUiThread(() -> {
                if (binding == null) return;
                binding.tvStatus.setText("Status: GDPR Consent gathered successfully.");
                binding.smartNativeSmall.loadAd();
                binding.smartNativeMedium.loadAd();
                Toast.makeText(this, "Consent gathered!", Toast.LENGTH_SHORT).show();
            }));
        });

        binding.btnInterstitial.setOnClickListener(v -> {
            binding.tvStatus.setText("Status: Showing Interstitial Ad...");
            AdsFacade.getInstance().interstitial().navigationClickAd(this, () -> {
                if (binding == null) return;
                binding.tvStatus.setText("Status: Interstitial closed / bypassed. Resumed flow.");
            });
        });

        binding.btnRewarded.setOnClickListener(v -> {
            binding.tvStatus.setText("Status: Requesting Rewarded Ad...");
            AdsFacade.getInstance().rewarded().showRewardAdWithLoading(
                    this,
                    rewardItem -> {
                        if (binding == null) return;
                        binding.tvStatus.setText("Status: User earned reward: " + rewardItem.getAmount() + " " + rewardItem.getType());
                    },
                    () -> {
                        if (binding == null) return;
                        Toast.makeText(MainActivity.this, "Rewarded Ad closed.", Toast.LENGTH_SHORT).show();
                    },
                    () -> {
                        if (binding == null) return;
                        binding.tvStatus.setText("Status: Failed or timed out loading Rewarded Ad.");
                    }
            );
        });

        binding.btnRewardedInterstitial.setOnClickListener(v -> {
            binding.tvStatus.setText("Status: Opening Rewarded Interstitial intro...");
            AdsFacade.getInstance().rewardedInterstitial().showWithIntroDialog(
                    this,
                    "100 Gold Coins",
                    rewardItem -> {
                        if (binding == null) return;
                        binding.tvStatus.setText("Status: User earned reward: " + rewardItem.getAmount() + " " + rewardItem.getType());
                    },
                    () -> {
                        if (binding == null) return;
                        Toast.makeText(MainActivity.this, "Rewarded Interstitial flow finished.", Toast.LENGTH_SHORT).show();
                    }
            );
        });

        binding.btnRefreshNative.setOnClickListener(v -> {
            binding.tvStatus.setText("Status: Reloading Native Ads...");
            binding.smartNativeSmall.loadAd();
            binding.smartNativeMedium.loadAd();
        });

        binding.btnOpenDetail.setOnClickListener(v -> {
            binding.tvStatus.setText("Status: Navigating to Detail screen...");
            AdsFacade.getInstance().interstitial().navigationClickAd(this, () -> {
                startActivity(new Intent(MainActivity.this, DetailActivity.class));
            });
        });

        binding.btnOpenNormalBanner.setOnClickListener(v -> {
            binding.tvStatus.setText("Status: Navigating to Normal Banner screen...");
            startActivity(new Intent(MainActivity.this, NormalBannerActivity.class));
        });

        binding.btnToggleRemoveAds.setOnClickListener(v -> {
            isAdsRemoved = !isAdsRemoved;
            AdsFacade.getInstance().setAdsRemoved(isAdsRemoved);
            binding.tvStatus.setText("Status: Ads Removed = " + isAdsRemoved);
            if (isAdsRemoved) {
                binding.smartBanner.setVisibility(View.GONE);
                binding.smartNativeSmall.setVisibility(View.GONE);
                binding.smartNativeMedium.setVisibility(View.GONE);
            } else {
                binding.smartBanner.setVisibility(View.VISIBLE);
                binding.smartBanner.loadAd();
                binding.smartNativeSmall.setVisibility(View.VISIBLE);
                binding.smartNativeMedium.setVisibility(View.VISIBLE);
                binding.smartNativeSmall.loadAd();
                binding.smartNativeMedium.loadAd();
            }
            Toast.makeText(this, "Ads Removed: " + isAdsRemoved, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void hideBannerAd() {
        if (binding != null) {
            binding.smartBanner.onOpenAdShowing();
        }
    }

    @Override
    public void showBannerAd() {
        if (binding != null) {
            binding.smartBanner.onOpenAdDismissed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
