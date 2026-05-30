package com.posegame.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Level/关卡 model
 */
public class Level {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("poseType")
    private String poseType;

    @SerializedName("passScore")
    private int passScore;

    @SerializedName("stickerReward")
    private String stickerReward;

    @SerializedName("isUnlocked")
    private boolean isUnlocked;

    @SerializedName("isPassed")
    private boolean isPassed;

    @SerializedName("targetPose")
    private TargetPose targetPose;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPoseType() {
        return poseType;
    }

    public void setPoseType(String poseType) {
        this.poseType = poseType;
    }

    public int getPassScore() {
        return passScore;
    }

    public void setPassScore(int passScore) {
        this.passScore = passScore;
    }

    public String getStickerReward() {
        return stickerReward;
    }

    public void setStickerReward(String stickerReward) {
        this.stickerReward = stickerReward;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }

    public boolean isPassed() {
        return isPassed;
    }

    public void setPassed(boolean passed) {
        isPassed = passed;
    }

    public TargetPose getTargetPose() {
        return targetPose;
    }

    public void setTargetPose(TargetPose targetPose) {
        this.targetPose = targetPose;
    }

    public static class TargetPose {
        @SerializedName("keypoints")
        private List<Keypoint> keypoints;

        @SerializedName("imageUrl")
        private String imageUrl;

        public List<Keypoint> getKeypoints() {
            return keypoints;
        }

        public void setKeypoints(List<Keypoint> keypoints) {
            this.keypoints = keypoints;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }

    public static class Keypoint {
        @SerializedName("x")
        private float x;

        @SerializedName("y")
        private float y;

        @SerializedName("name")
        private String name;

        @SerializedName("confidence")
        private float confidence;

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public float getConfidence() {
            return confidence;
        }

        public void setConfidence(float confidence) {
            this.confidence = confidence;
        }
    }
}