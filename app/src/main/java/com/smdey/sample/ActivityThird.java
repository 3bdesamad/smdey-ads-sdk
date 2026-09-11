package com.smdey.sample;

import android.content.Intent;
import android.os.Bundle;

import com.smdey.sample.databinding.ActivityThirdBinding;

public class ActivityThird extends BaseActivity {

    private ActivityThirdBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityThirdBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.incToolbar.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.incToolbar.toolbar.setNavigationOnClickListener(v -> finish());

        binding.btnBackToSecond.setOnClickListener(v -> {
            finish();
        });

        binding.btnBackToMainDirectly.setOnClickListener(v -> {
            Intent intent = new Intent(this, ActivityMain.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
