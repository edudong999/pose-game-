package com.posegame.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Record list response wrapper
 */
class RecordListResponse {
    @SerializedName("list")
    private List<GameRecord> list;

    @SerializedName("total")
    private int total;

    @SerializedName("page")
    private int page;

    @SerializedName("pageSize")
    private int pageSize;

    public List<GameRecord> getList() {
        return list;
    }

    public void setList(List<GameRecord> list) {
        this.list = list;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}

/**
 * Leaderboard entry
 */
class LeaderboardEntry {
    @SerializedName("rank")
    private int rank;

    @SerializedName("nickname")
    private String nickname;

    @SerializedName("score")
    private int score;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}

/**
 * Leaderboard response
 */
class LeaderboardResponse {
    @SerializedName("levelId")
    private int levelId;

    @SerializedName("levelName")
    private String levelName;

    @SerializedName("ranking")
    private List<LeaderboardEntry> ranking;

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

    public List<LeaderboardEntry> getRanking() {
        return ranking;
    }

    public void setRanking(List<LeaderboardEntry> ranking) {
        this.ranking = ranking;
    }
}

/**
 * Sticker model
 */
class Sticker {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("unlockLevel")
    private int unlockLevel;

    @SerializedName("isUnlocked")
    private boolean isUnlocked;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getUnlockLevel() {
        return unlockLevel;
    }

    public void setUnlockLevel(int unlockLevel) {
        this.unlockLevel = unlockLevel;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }
}

/**
 * Sticker list response
 */
class StickerListResponse {
    @SerializedName("list")
    private List<Sticker> list;

    @SerializedName("total")
    private int total;

    public List<Sticker> getList() {
        return list;
    }

    public void setList(List<Sticker> list) {
        this.list = list;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}