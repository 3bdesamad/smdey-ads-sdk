package com.smdey.sample;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.smdey.sample.databinding.ActivityMainBinding;
import com.smdey.ads.sdk.AdsSdk;
import com.smdey.ads.sdk.rewarded.RewardedManager;

public class ActivityMain extends BaseActivity {

    private static final int REWARD_COINS_AMOUNT = 50;

    private ActivityMainBinding binding;
    private AlertDialog adLoadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.incToolbar.toolbar);

        binding.btnInterstitial.setOnClickListener(v -> {
            binding.tvStatus.setText("Showing Interstitial with loading overlay...");
            AdsSdk.getInstance().interstitial().showAdWithLoadingOverlay(this, () -> {
                if (!isSafeToUpdate() || binding == null) return;
                binding.tvStatus.setText("Interstitial completed.");
                Toast.makeText(this, "Interstitial Finished!", Toast.LENGTH_SHORT).show();
            });
        });

        binding.btnRewarded.setOnClickListener(v -> {
            Dialogs.sampleDialog(
                    this,
                    R.drawable.ic_reward_gift,
                    48,
                    R.color.green_400,
                    R.color.cardColor,
                    getString(R.string.rewarded_dialog_title),
                    getString(R.string.rewarded_dialog_message, REWARD_COINS_AMOUNT),
                    getString(R.string.rewarded_dialog_positive),
                    (dialog, which) -> showRewardedAd(),
                    getString(R.string.rewarded_dialog_negative)
            );
        });

        binding.btnAppOpen.setOnClickListener(v -> {
            if (AdsSdk.getInstance().appOpen().isAdAvailable()) {
                binding.tvStatus.setText("Showing App Open ad...");
                AdsSdk.getInstance().appOpen().showIfAvailable(this);
            } else {
                binding.tvStatus.setText("Preloading App Open ad. Try again shortly...");
                Toast.makeText(this, "Preloading App Open Ad...", Toast.LENGTH_SHORT).show();
                AdsSdk.getInstance().appOpen().preloadAd(this);
            }
        });

        binding.btnOpenSecond.setOnClickListener(v -> {
            AdsSdk.getInstance().interstitial().showAdWithLoadingOverlayByClick(this, () -> {
                if (!isSafeToUpdate() || binding == null) return;
                startActivity(new Intent(this, ActivitySecond.class));
            });
        });

        binding.btnConsent.setOnClickListener(v -> {
            if (AdsSdk.getInstance().consent().isPrivacyOptionsRequired()) {
                AdsSdk.getInstance().consent().showPrivacyOptions(this, () ->
                        Toast.makeText(this, "Privacy options updated.", Toast.LENGTH_SHORT).show());
            } else {
                AdsSdk.getInstance().consent().gatherConsent(this, () -> {
                    boolean canRequest = AdsSdk.getInstance().consent().canRequestAds();
                    Toast.makeText(this, "Consent checked. Can request ads: " + canRequest, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showRewardedAd() {
        if (!isSafeToUpdate() || binding == null) {
            return;
        }

        if (AdsSdk.getInstance().rewarded().isAdReady()) {
            binding.tvStatus.setText("Showing Rewarded ad...");
            AdsSdk.getInstance().rewarded().showAd(this, (amount, type) -> {
                if (!isSafeToUpdate() || binding == null) return;
                String msg = getString(R.string.rewarded_ad_earned, amount, type);
                binding.tvStatus.setText(msg);
                Toast.makeText(ActivityMain.this, msg, Toast.LENGTH_LONG).show();
            });
            return;
        }

        toggleLoadingAd(true);
        binding.tvStatus.setText(R.string.loading_ad);

        AdsSdk.getInstance().rewarded().loadAd(this, new RewardedManager.OnLoadListener() {
            @Override
            public void onLoaded() {
                toggleLoadingAd(false);
                if (!isSafeToUpdate() || binding == null) return;
                showRewardedAd();
            }

            @Override
            public void onFailed() {
                toggleLoadingAd(false);
                if (!isSafeToUpdate() || binding == null) return;
                binding.tvStatus.setText(R.string.rewarded_ad_failed);
                Toast.makeText(ActivityMain.this, R.string.rewarded_ad_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleLoadingAd(boolean show) {
        if (show) {
            if (adLoadingDialog != null && adLoadingDialog.isShowing()) {
                return;
            }
            adLoadingDialog = Dialogs.dialogLoadingAd(this);
            if (adLoadingDialog != null) {
                adLoadingDialog.show();
            }
        } else if (adLoadingDialog != null) {
            adLoadingDialog.dismiss();
            adLoadingDialog = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        toggleLoadingAd(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        toggleLoadingAd(false);
        binding = null;
    }
}
