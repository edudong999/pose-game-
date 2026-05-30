package com.posegame.app.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences utility for storing user session data
 */
public class SharedPreferencesUtil {

    private static final String PREF_NAME = "pose_game_prefs";
    private static final String KEY_TOKEN = "user_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_AVATAR_URL = "avatar_url";
    private static final String KEY_TOTAL_SCORE = "total_score";
    private static final String KEY_LEVEL_UNLOCK = "level_unlock";

    private final SharedPreferences prefs;

    public SharedPreferencesUtil(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Token
    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    // User ID
    public void saveUserId(int userId) {
        prefs.edit().putInt(KEY_USER_ID, userId).apply();
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, 0);
    }

    // Nickname
    public void saveNickname(String nickname) {
        prefs.edit().putString(KEY_NICKNAME, nickname).apply();
    }

    public String getNickname() {
        return prefs.getString(KEY_NICKNAME, "");
    }

    // Avatar URL
    public void saveAvatarUrl(String url) {
        prefs.edit().putString(KEY_AVATAR_URL, url).apply();
    }

    public String getAvatarUrl() {
        return prefs.getString(KEY_AVATAR_URL, null);
    }

    // Total Score
    public void saveTotalScore(int score) {
        prefs.edit().putInt(KEY_TOTAL_SCORE, score).apply();
    }

    public int getTotalScore() {
        return prefs.getInt(KEY_TOTAL_SCORE, 0);
    }

    // Level Unlock
    public void saveLevelUnlock(int level) {
        prefs.edit().putInt(KEY_LEVEL_UNLOCK, level).apply();
    }

    public int getLevelUnlock() {
        return prefs.getInt(KEY_LEVEL_UNLOCK, 1);
    }

    // Check if logged in
    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    // Get Authorization header value
    public String getAuthHeader() {
        String token = getToken();
        return token != null ? "Bearer " + token : null;
    }

    // Clear all data (logout)
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}