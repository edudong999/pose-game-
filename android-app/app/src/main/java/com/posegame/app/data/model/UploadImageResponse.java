package com.posegame.app.data.model;

/**
 * Response from POST /api/game/record/media
 */
public class UploadImageResponse {
    private String imageUrl;

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
