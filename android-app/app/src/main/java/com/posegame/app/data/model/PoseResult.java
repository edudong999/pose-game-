package com.posegame.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 * Pose recognition result / 姿态识别结果
 */
public class PoseResult {

    @SerializedName("score")
    private int score;

    @SerializedName("isPass")
    private boolean isPass;

    @SerializedName("detectedPose")
    private Level.TargetPose detectedPose;

    @SerializedName("targetPose")
    private Level.TargetPose targetPose;

    @SerializedName("matchDetails")
    private Map<String, Integer> matchDetails;

    @SerializedName("stickerReward")
    private String stickerReward;

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

    public Level.TargetPose getDetectedPose() {
        return detectedPose;
    }

    public void setDetectedPose(Level.TargetPose detectedPose) {
        this.detectedPose = detectedPose;
    }

    public Level.TargetPose getTargetPose() {
        return targetPose;
    }

    public void setTargetPose(Level.TargetPose targetPose) {
        this.targetPose = targetPose;
    }

    public Map<String, Integer> getMatchDetails() {
        return matchDetails;
    }

    public void setMatchDetails(Map<String, Integer> matchDetails) {
        this.matchDetails = matchDetails;
    }

    public String getStickerReward() {
        return stickerReward;
    }

    public void setStickerReward(String stickerReward) {
        this.stickerReward = stickerReward;
    }
}