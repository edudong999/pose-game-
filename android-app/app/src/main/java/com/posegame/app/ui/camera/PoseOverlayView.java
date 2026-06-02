package com.posegame.app.ui.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import com.posegame.app.ui.camera.MediaPipePoseDetector.Keypoint;

/**
 * Overlay View for displaying real-time score and pose guidance hints
 * Note: Actual score comes from server after photo submission
 * This view shows static guidance UI elements
 */
public class PoseOverlayView extends View {

    private Paint scorePaint;
    private Paint hintPaint;
    private Paint hintBgPaint;
    private Paint guidePaint;

    // Skeleton drawing
    private Paint skeletonPaint;
    private Paint jointGoodPaint;
    private Paint jointBadPaint;

    // Skeleton connections for 33-point MediaPipe model
    // Index refers to position in keypoint list (0-32 = 33 points)
    private static final int[][] SKELETON_CONNECTIONS = {
        // Head (0-10): nose, eyes, ears, mouth
        {0, 1}, {0, 2}, {1, 2}, {1, 3}, {2, 4},   // nose to eyes
        {5, 6}, {5, 7}, {6, 8},                   // eye connections
        {9, 10},                                  // mouth
        // Shoulders (11-12)
        {11, 12},
        // Left arm (11 -> 13 -> 15)
        {11, 13}, {13, 15},
        // Right arm (12 -> 14 -> 16)
        {12, 14}, {14, 16},
        // Torso
        {11, 23}, {12, 24},                       // shoulders to hips
        {23, 24},                                 // hip connection
        // Left leg (23 -> 25 -> 27 -> 29)
        {23, 25}, {25, 27}, {27, 29},
        // Right leg (24 -> 26 -> 28 -> 30)
        {24, 26}, {26, 28}, {28, 30}
    };

    // Body part labels for 5 key parts: head, shoulders, hands, waist, legs
    private static final String[] BODY_PART_LABELS = {
        "头",    // 0-10: head
        "肩",    // 11-12: shoulders
        "手",    // 15-16: wrists
        "腰",    // 23-24: hips
        "腿"     // 25-30: knees and ankles
    };

    // Key joint indices for body part display (for 33-point model)
    private static final int[] KEY_JOINT_INDICES = {
        0,   // nose - head
        11,  // left_shoulder
        12,  // right_shoulder
        15,  // left_wrist
        16,  // right_wrist
        23,  // left_hip
        24,  // right_hip
        25,  // left_knee
        26,  // right_knee
        27,  // left_ankle
        28   // right_ankle
    };

    private List<Keypoint> currentKeypoints = new ArrayList<>();
    private int currentScore = 0;
    private List<String> currentHints = new ArrayList<>();

    // Body part scores from server
    private int headScore = 0;
    private int shoulderScore = 0;
    private int armScore = 0;
    private int bodyScore = 0;

    private static final int BODY_PART_HEAD = 0;
    private static final int BODY_PART_SHOULDERS = 1;
    private static final int BODY_PART_ARMS = 2;
    private static final int BODY_PART_BODY = 3;

    private boolean showGuide = true;
    private String guideText = "请摆好姿势，对准屏幕";

    public PoseOverlayView(Context context) {
        super(context);
        init();
    }

    public PoseOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PoseOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        scorePaint = new Paint();
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(80);
        scorePaint.setAntiAlias(true);
        scorePaint.setTextAlign(Paint.Align.CENTER);

        hintPaint = new Paint();
        hintPaint.setColor(Color.WHITE);
        hintPaint.setTextSize(40);
        hintPaint.setAntiAlias(true);

        hintBgPaint = new Paint();
        hintBgPaint.setColor(Color.argb(180, 0, 0, 0));
        hintBgPaint.setStyle(Paint.Style.FILL);

        guidePaint = new Paint();
        guidePaint.setColor(Color.argb(200, 255, 255, 255));
        guidePaint.setTextSize(48);
        guidePaint.setAntiAlias(true);
        guidePaint.setTextAlign(Paint.Align.CENTER);

        skeletonPaint = new Paint();
        skeletonPaint.setColor(Color.GREEN);
        skeletonPaint.setStrokeWidth(8);
        skeletonPaint.setStyle(Paint.Style.STROKE);
        skeletonPaint.setAntiAlias(true);

        jointGoodPaint = new Paint();
        jointGoodPaint.setColor(Color.GREEN);
        jointGoodPaint.setStyle(Paint.Style.FILL);

        jointBadPaint = new Paint();
        jointBadPaint.setColor(Color.RED);
        jointBadPaint.setStyle(Paint.Style.FILL);

        // Body part paints for different parts
        headPaint = new Paint();
        headPaint.setColor(Color.YELLOW);
        headPaint.setStyle(Paint.Style.FILL);

        shoulderPaint = new Paint();
        shoulderPaint.setColor(Color.CYAN);
        shoulderPaint.setStyle(Paint.Style.FILL);

        armPaint = new Paint();
        armPaint.setColor(Color.BLUE);
        armPaint.setStyle(Paint.Style.FILL);

        bodyPaint = new Paint();
        bodyPaint.setColor(Color.MAGENTA);
        bodyPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint();
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(36);
        labelPaint.setAntiAlias(true);

        labelBgPaint = new Paint();
        labelBgPaint.setColor(Color.argb(200, 0, 0, 0));
        labelBgPaint.setStyle(Paint.Style.FILL);
    }

    private Paint headPaint;
    private Paint shoulderPaint;
    private Paint armPaint;
    private Paint bodyPaint;
    private Paint labelPaint;
    private Paint labelBgPaint;

    public void updateScore(int score) {
        this.currentScore = score;
        invalidate();
    }

    public void updateKeypoints(List<Keypoint> keypoints) {
        this.currentKeypoints = keypoints;
        invalidate();
    }

    public void clear() {
        this.currentKeypoints = new ArrayList<>();
        this.currentScore = 0;
        this.currentHints = new ArrayList<>();
        this.headScore = 0;
        this.shoulderScore = 0;
        this.armScore = 0;
        this.bodyScore = 0;
        invalidate();
    }

    public void updateKeyPointHints(List<String> hints) {
        this.currentHints = hints;
        invalidate();
    }

    public void setGuideText(String text) {
        this.guideText = text;
        invalidate();
    }

    public void setShowGuide(boolean show) {
        this.showGuide = show;
        invalidate();
    }

    public void updateBodyScores(int head, int shoulders, int arms, int body) {
        this.headScore = head;
        this.shoulderScore = shoulders;
        this.armScore = arms;
        this.bodyScore = body;
        invalidate();
    }

    private Paint getConnectionPaint(int jointIndex) {
        Paint paint = new Paint();
        paint.setStrokeWidth(8);
        paint.setAntiAlias(true);

        if (jointIndex <= 10) {
            // Head - yellow
            paint.setColor(Color.YELLOW);
        } else if (jointIndex == 11 || jointIndex == 12) {
            // Shoulders - cyan
            paint.setColor(Color.CYAN);
        } else if (jointIndex >= 13 && jointIndex <= 22) {
            // Arms - blue
            paint.setColor(Color.BLUE);
        } else {
            // Body (hips, legs) - magenta
            paint.setColor(Color.MAGENTA);
        }
        return paint;
    }

    private Paint getJointPaint(int jointIndex) {
        if (jointIndex <= 10) return headPaint;
        if (jointIndex == 11 || jointIndex == 12) return shoulderPaint;
        if (jointIndex >= 13 && jointIndex <= 22) return armPaint;
        return bodyPaint;
    }

    private String getBodyPartLabel(int jointIndex) {
        if (jointIndex <= 10) return "头";
        if (jointIndex == 11 || jointIndex == 12) return "肩";
        if (jointIndex == 15 || jointIndex == 16) return "手";
        if (jointIndex == 23 || jointIndex == 24) return "腰";
        return "腿";
    }

    private int getBodyPartScore(int jointIndex) {
        if (jointIndex <= 10) return headScore;
        if (jointIndex == 11 || jointIndex == 12) return shoulderScore;
        if (jointIndex >= 13 && jointIndex <= 22) return armScore;
        return bodyScore;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw guide text at top
        if (showGuide && guideText != null) {
            float guideX = getWidth() / 2f;
            float guideY = 100;

            RectF guideBg = new RectF(guideX - 200, guideY - 50, guideX + 200, guideY + 50);
            canvas.drawRoundRect(guideBg, 20, 20, hintBgPaint);

            canvas.drawText(guideText, guideX, guideY + 15, guidePaint);
        }

        // Draw score in top-right corner
        String scoreText = currentScore > 0 ? currentScore + "分" : "--分";
        float scoreX = getWidth() - 80;
        float scoreY = 80;

        RectF scoreBg = new RectF(scoreX - 80, scoreY - 50, scoreX + 80, scoreY + 50);
        canvas.drawRoundRect(scoreBg, 20, 20, hintBgPaint);

        canvas.drawText(scoreText, scoreX, scoreY + 15, scorePaint);

        // Draw skeleton with colored body parts and labels
        if (!currentKeypoints.isEmpty()) {
            // First draw all connections in a base color
            for (int[] connection : SKELETON_CONNECTIONS) {
                if (connection[0] < currentKeypoints.size() && connection[1] < currentKeypoints.size()) {
                    Keypoint kp1 = currentKeypoints.get(connection[0]);
                    Keypoint kp2 = currentKeypoints.get(connection[1]);
                    float x1 = kp1.x * getWidth();
                    float y1 = kp1.y * getHeight();
                    float x2 = kp2.x * getWidth();
                    float y2 = kp2.y * getHeight();

                    // Determine connection color based on body part
                    Paint linePaint = getConnectionPaint(connection[0]);
                    canvas.drawLine(x1, y1, x2, y2, linePaint);
                }
            }

            // Draw key joints with body part colors and labels
            for (int idx : KEY_JOINT_INDICES) {
                if (idx < currentKeypoints.size()) {
                    Keypoint kp = currentKeypoints.get(idx);
                    float x = kp.x * getWidth();
                    float y = kp.y * getHeight();

                    Paint paint = getJointPaint(idx);
                    canvas.drawCircle(x, y, 18, paint);

                    // Draw label with score
                    String label = getBodyPartLabel(idx);
                    int score = getBodyPartScore(idx);
                    String text = label + score;

                    // Draw label background
                    float labelX = x;
                    float labelY = y - 30;
                    float textWidth = labelPaint.measureText(text);
                    RectF labelBg = new RectF(labelX - textWidth/2 - 8, labelY - 20, labelX + textWidth/2 + 8, labelY + 10);
                    canvas.drawRoundRect(labelBg, 8, 8, labelBgPaint);
                    canvas.drawText(text, labelX, labelY, labelPaint);
                }
            }
        }

        // Draw overall score at bottom center
        String overallText = "总分: " + currentScore + "分";
        float overallX = getWidth() / 2f;
        float overallY = getHeight() - 50;
        float textWidth = scorePaint.measureText(overallText);
        RectF overallBg = new RectF(overallX - textWidth/2 - 15, overallY - 35, overallX + textWidth/2 + 15, overallY + 15);
        canvas.drawRoundRect(overallBg, 10, 10, hintBgPaint);
        canvas.drawText(overallText, overallX, overallY, scorePaint);
    }
}