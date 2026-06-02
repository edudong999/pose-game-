package com.posegame.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Game record / 闯关记录 model
 */
public class GameRecord {

    @SerializedName("id")
    private int id;

    @SerializedName("levelId")
    private int levelId;

    @SerializedName("levelName")
    private String levelName;

    @SerializedName("score")
    private int score;

    @SerializedName("isPass")
    private boolean isPass;

    @SerializedName("mediaUrl")
    private String mediaUrl;

    @SerializedName("createdAt")
    private String createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLevelId() {
        return levelId;
    }

    public void setLevelId(int levelId) {
        this.levelId = levelId;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isPass() {
        return isPass;
    }

    public void setPass(boolean pass) {
        isPass = pass;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}