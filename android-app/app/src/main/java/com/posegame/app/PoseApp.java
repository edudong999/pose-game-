package com.posegame.app;

import android.app.Application;
import android.util.Log;

import com.posegame.app.util.SharedPreferencesUtil;

/**
 * Application class for global state
 */
public class PoseApp extends Application {

    private static final String TAG = "PoseApp";
    private static PoseApp instance;
    private SharedPreferencesUtil prefsUtil;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        try {
            prefsUtil = new SharedPreferencesUtil(this);
            Log.d(TAG, "PoseApp initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing PoseApp", e);
        }
    }

    public static PoseApp getInstance() {
        return instance;
    }

    public SharedPreferencesUtil getPrefsUtil() {
        return prefsUtil;
    }
}