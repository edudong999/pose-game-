package com.posegame.app.ui.camera;

import android.graphics.Bitmap;

import com.posegame.app.ui.camera.MediaPipePoseDetector.Keypoint;

import java.util.ArrayList;
import java.util.List;

/**
 * 线程安全地跟踪 imageAnalysis 持续送来的帧和分数。
 * - onFrame 每次来一帧都调（cameraExecutor 线程）
 * - onScore 在实时分数响应回来时调（mainHandler 线程）
 * - 录像倒计时结束后调 getBestFrame/getBestKeypoints
 * - 兜底用 getFallbackFrame（当实时分数从未回来过）
 */
public class BestFrameTracker {

    private Bitmap latestBitmap;
    private List<Keypoint> latestKeypoints;
    private Bitmap bestBitmap;
    private List<Keypoint> bestKeypoints;
    private int bestScore = -1;

    public synchronized void onFrame(Bitmap frame, List<Keypoint> keypoints) {
        if (latestBitmap != null && latestBitmap != frame && latestBitmap != bestBitmap
            && !latestBitmap.isRecycled()) {
            latestBitmap.recycle();
        }
        latestBitmap = frame;
        latestKeypoints = new ArrayList<>(keypoints);
    }

    public synchronized void onScore(int score) {
        if (score > bestScore) {
            if (bestBitmap != null && bestBitmap != latestBitmap
                && !bestBitmap.isRecycled()) {
                bestBitmap.recycle();
            }
            bestBitmap = latestBitmap;
            bestKeypoints = latestKeypoints;
            bestScore = score;
        }
    }

    public synchronized Bitmap getBestFrame() { return bestBitmap; }
    public synchronized List<Keypoint> getBestKeypoints() { return bestKeypoints; }
    public synchronized int getBestScore() { return bestScore; }

    public synchronized Bitmap getFallbackFrame() { return latestBitmap; }
    public synchronized List<Keypoint> getFallbackKeypoints() { return latestKeypoints; }

    public synchronized void reset() {
        bestScore = -1;
        bestBitmap = null;
        bestKeypoints = null;
        if (latestBitmap != null && !latestBitmap.isRecycled()) {
            latestBitmap.recycle();
        }
        latestBitmap = null;
        latestKeypoints = null;
    }

    public synchronized void release() { reset(); }
}
