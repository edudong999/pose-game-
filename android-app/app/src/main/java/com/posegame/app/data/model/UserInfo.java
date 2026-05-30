package com.posegame.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * User information model
 */
public class UserInfo {

    @SerializedName("userId")
    private int userId;

    @SerializedName("nickname")
    private String nickname;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("totalScore")
    private int totalScore;

    @SerializedName("levelUnlock")
    private int levelUnlock;

    @SerializedName("stickers")
    private List<String> stickers;

    @SerializedName("token")
    private String token;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getLevelUnlock() {
        return levelUnlock;
    }

    public void setLevelUnlock(int levelUnlock) {
        this.levelUnlock = levelUnlock;
    }

    public List<String> getStickers() {
        return stickers;
    }

    public void setStickers(List<String> stickers) {
        this.stickers = stickers;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}