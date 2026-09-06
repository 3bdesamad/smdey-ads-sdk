package com.smdey.ads.sample;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.smdey.ads.sample.databinding.ActivityDetailBinding;

public class DetailActivity extends AppCompatActivity {

    private ActivityDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);

        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupEdgeToEdgeInsets();

        // Setup Toolbar
        setSupportActionBar(binding.incToolbar.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Detail Screen");
        }
        binding.incToolbar.toolbar.setNavigationOnClickListener(v -> finish());

        // Refresh Button
        binding.btnReloadDetailBanner.setOnClickListener(v -> refreshBanner());
    }

    private void setupEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            // Apply top status bar inset directly to the toolbar so blue background seamlessly fills behind status bar
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            refreshBanner();
            return true;
        }else if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshBanner() {
        if (binding != null && binding.detailSmartBanner != null) {
            binding.detailSmartBanner.onOpenAdDismissed();
            Toast.makeText(this, "Refreshing Collapsible Banner...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
