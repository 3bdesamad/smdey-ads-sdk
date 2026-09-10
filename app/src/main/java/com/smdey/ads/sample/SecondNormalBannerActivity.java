package com.smdey.ads.sample;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.smdey.ads.sample.databinding.ActivitySecondNormalBannerBinding;

public class SecondNormalBannerActivity extends AppCompatActivity {

    private ActivitySecondNormalBannerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);

        super.onCreate(savedInstanceState);
        binding = ActivitySecondNormalBannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupEdgeToEdgeInsets();
        initToolbar();
        initListeners();
    }

    private void initToolbar() {
        setSupportActionBar(binding.incToolbar.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Second Normal Banner");
        }
        binding.incToolbar.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initListeners() {
        binding.btnBackToFirstNormal.setOnClickListener(v -> finish());
    }

    private void setupEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );

            int actionBarHeight = getActionBarHeight();
            ViewGroup.LayoutParams lp = binding.incToolbar.toolbar.getLayoutParams();
            if (lp != null) {
                lp.height = insets.top + actionBarHeight;
                binding.incToolbar.toolbar.setLayoutParams(lp);
            }

            binding.incToolbar.toolbar.setPadding(
                    binding.incToolbar.toolbar.getPaddingLeft(),
                    insets.top,
                    binding.incToolbar.toolbar.getPaddingRight(),
                    binding.incToolbar.toolbar.getPaddingBottom()
            );
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
