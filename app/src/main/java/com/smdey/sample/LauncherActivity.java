package com.smdey.sample;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LauncherActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If this activity is not the root of the task, finish immediately
        if (shouldFinishActivity()) {
            return;
        }

        // Test: set ads removed
//        SharedPref.getInstance(this).setAdsRemoved(false);
//        Toast.makeText(this, "Thank you! Ads have been removed.", Toast.LENGTH_SHORT).show();

        navigateToNextScreen();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        if (shouldFinishActivity()) {
            return;
        }
        navigateToNextScreen();
    }

    @Override
    public boolean canShowOpenAd() {
        // Suppress App Open ads during initial launcher / splash routing
        return false;
    }

    private boolean shouldFinishActivity() {
        if (!isTaskRoot()
                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
                && getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_MAIN)) {
            finish();
            return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private void navigateToNextScreen() {
        Intent intent = new Intent(this, ActivityMain.class);
        startActivity(intent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0);
        } else {
            overridePendingTransition(0, 0);
        }

        finish();
    }
}
