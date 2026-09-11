package com.smdey.sample;

import android.content.Context;
import android.content.SharedPreferences;


public class SharedPref {

    private static volatile SharedPref mInstance;
    private final SharedPreferences sharedPreferences;

    private SharedPref(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.MAIN_PREF, Context.MODE_PRIVATE);
    }

    public static SharedPref getInstance(Context context) {
        if (mInstance == null) {
            synchronized (SharedPref.class) {
                if (mInstance == null) {
                    mInstance = new SharedPref(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    // =============================================================================================
    // Helper Methods
    // =============================================================================================

    private void saveString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    private void saveInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    private void saveBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    private void saveLong(String key, long value) {
        sharedPreferences.edit().putLong(key, value).apply();
    }

    private void saveFloat(String key, float value) {
        sharedPreferences.edit().putFloat(key, value).apply();
    }

    // =============================================================================================
    // Getters & Setters
    // =============================================================================================

    public void setAdsRemoved(boolean isRemoved) {
        saveBoolean(Constants.KEY_ADS_REMOVED, isRemoved);
    }

    public boolean isAdsRemoved() {
        return sharedPreferences.getBoolean(Constants.KEY_ADS_REMOVED, false);
    }









}
