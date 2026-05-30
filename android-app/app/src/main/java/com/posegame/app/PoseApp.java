package com.posegame.app;

import android.app.Application;
import com.posegame.app.util.SharedPreferencesUtil;

/**
 * Application class for global state
 */
public class PoseApp extends Application {

    private static PoseApp instance;
    private SharedPreferencesUtil prefsUtil;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        prefsUtil = new SharedPreferencesUtil(this);
    }

    public static PoseApp getInstance() {
        return instance;
    }

    public SharedPreferencesUtil getPrefsUtil() {
        return prefsUtil;
    }
}