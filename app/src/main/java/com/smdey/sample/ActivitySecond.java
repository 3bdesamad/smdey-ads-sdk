package com.smdey.sample;

import android.content.Intent;
import android.os.Bundle;

import com.smdey.sample.databinding.ActivitySecondBinding;

public class ActivitySecond extends BaseActivity {

    private ActivitySecondBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySecondBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.incToolbar.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.incToolbar.toolbar.setNavigationOnClickListener(v -> finish());

        binding.btnOpenThird.setOnClickListener(v -> {
            startActivity(new Intent(this, ActivityThird.class));
        });

        binding.btnBackToMain.setOnClickListener(v -> {
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
