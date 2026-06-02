# 录像模式实时姿态检测 + 截图保存 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 `CameraActivity` 录像模式不进入实时姿态检测的问题，并将最佳帧截图保存到闯关记录供回看

**Architecture:** 复用现有 `imageAnalysis` 实时分析流程（500ms 一帧），新增 `BestFrameTracker` 跟踪最高分帧，倒计时结束后把该帧上传到新接口 `/api/game/record/media` 拿到 URL，调 `/api/game/record` 时附带 `mediaUrl`。照片模式完全不变

**Tech Stack:** Android (Java + CameraX 1.3.1), Python (FastAPI), Retrofit 2.9

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `app/.../ui/camera/BestFrameTracker.java` | 新建 | 线程安全地记录最高分帧 |
| `app/.../data/model/UploadImageResponse.java` | 新建 | 图片上传响应模型 |
| `app/.../data/model/RecordListResponse.java` | 修改 | 加 `mediaUrl` 字段 |
| `app/.../data/api/ApiService.java` | 修改 | 加 `uploadImage` 接口、给 `submitRecord` 加 `mediaUrl` 参数 |
| `app/.../ui/camera/CameraActivity.java` | 修改 | 集成 tracker、按 mode 分流、新增录像完成流程 |
| `app/.../ui/result/ResultActivity.java` | 修改 | 提交记录时附带 `mediaUrl` |
| `backend/app/routers/game.py` | 修改 | 加 `/record/media` 接口、`mediaUrl` 入库/返回 |
| `backend/app/main.py` | 修改 | 挂载 `static` 目录 |
| `app/src/test/.../BestFrameTrackerTest.java` | 新建 | 单元测试 |

---

## Task 1: BestFrameTracker 单元测试

**Files:**
- Create: `android-app/app/src/test/java/com/posegame/app/ui/camera/BestFrameTrackerTest.java`

- [ ] **Step 1: 写失败的测试 - 基本选最高分**

```java
package com.posegame.app.ui.camera;

import android.graphics.Bitmap;

import com.posegame.app.ui.camera.MediaPipePoseDetector.Keypoint;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class BestFrameTrackerTest {

    private BestFrameTracker tracker;
    private Bitmap frame1, frame2, frame3;

    @Before
    public void setUp() {
        tracker = new BestFrameTracker();
        // 创建 3 个 1x1 的 bitmap 用于测试（不调用 recycle，便于测试）
        frame1 = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        frame2 = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        frame3 = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    @Test
    public void picksHighestScoredFrame() {
        List<Keypoint> kp1 = makeKeypoints();
        List<Keypoint> kp2 = makeKeypoints();
        List<Keypoint> kp3 = makeKeypoints();

        tracker.onFrame(frame1, kp1);
        tracker.onScore(80);

        tracker.onFrame(frame2, kp2);
        tracker.onScore(95);  // 最高

        tracker.onFrame(frame3, kp3);
        tracker.onScore(82);

        assertEquals(frame2, tracker.getBestFrame());
        assertEquals(kp2, tracker.getBestKeypoints());
        assertEquals(95, tracker.getBestScore());
    }

    @Test
    public void fallsBackToLatestWhenNoScore() {
        List<Keypoint> kp = makeKeypoints();

        tracker.onFrame(frame1, kp);
        // 永远不调 onScore

        assertNull(tracker.getBestFrame());
        assertEquals(frame1, tracker.getFallbackFrame());
    }

    @Test
    public void resetClearsEverything() {
        tracker.onFrame(frame1, makeKeypoints());
        tracker.onScore(85);

        tracker.reset();

        assertNull(tracker.getBestFrame());
        assertEquals(-1, tracker.getBestScore());
        assertNull(tracker.getFallbackFrame());
    }

    @Test
    public void lowerScoreDoesNotReplaceBest() {
        tracker.onFrame(frame1, makeKeypoints());
        tracker.onScore(90);

        tracker.onFrame(frame2, makeKeypoints());
        tracker.onScore(70);  // 较低

        assertEquals(frame1, tracker.getBestFrame());
        assertEquals(90, tracker.getBestScore());
    }

    private List<Keypoint> makeKeypoints() {
        return new ArrayList<>(Arrays.asList(
            new Keypoint("nose", 0.5f, 0.5f, 0f, 0.9f)
        ));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `cd android-app && ./gradlew test --tests "com.posegame.app.ui.camera.BestFrameTrackerTest"`
Expected: 编译失败 — `BestFrameTracker` 类不存在

---

## Task 2: BestFrameTracker 实现

**Files:**
- Create: `android-app/app/src/main/java/com/posegame/app/ui/camera/BestFrameTracker.java`

- [ ] **Step 1: 实现 BestFrameTracker**

```java
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
```

- [ ] **Step 2: 跑测试确认通过**

Run: `cd android-app && ./gradlew test --tests "com.posegame.app.ui.camera.BestFrameTrackerTest"`
Expected: 4 个测试全部 PASS

- [ ] **Step 3: 提交**

```bash
cd D:/andriod-project/pose-game
git add android-app/app/src/main/java/com/posegame/app/ui/camera/BestFrameTracker.java \
        android-app/app/src/test/java/com/posegame/app/ui/camera/BestFrameTrackerTest.java
git commit -m "feat(camera): add BestFrameTracker for tracking highest-scored frame"
```

---

## Task 3: UploadImageResponse 模型

**Files:**
- Create: `android-app/app/src/main/java/com/posegame/app/data/model/UploadImageResponse.java`

- [ ] **Step 1: 创建模型**

```java
package com.posegame.app.data.model;

/**
 * Response from POST /api/game/record/media
 */
public class UploadImageResponse {
    private String imageUrl;

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
```

- [ ] **Step 2: 提交**

```bash
cd D:/andriod-project/pose-game
git add android-app/app/src/main/java/com/posegame/app/data/model/UploadImageResponse.java
git commit -m "feat(model): add UploadImageResponse for image upload endpoint"
```

---

## Task 4: RecordListResponse 加 mediaUrl 字段

**Files:**
- Modify: `android-app/app/src/main/java/com/posegame/app/data/model/RecordListResponse.java`

- [ ] **Step 1: 读现状确认结构**

Run: `cat "android-app/app/src/main/java/com/posegame/app/data/model/RecordListResponse.java"`

- [ ] **Step 2: 加 mediaUrl 字段**

在 RecordListResponse.java 内嵌的 `RecordItem` 类（如果存在）或主类里加：

```java
private String mediaUrl;

public String getMediaUrl() { return mediaUrl; }
public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
```

如果 `RecordListResponse` 内部是 `List<RecordItem>` 结构，确保 `RecordItem` 类有上面这 3 行。如果整段是平铺结构，加在类顶层。

- [ ] **Step 3: 提交**

```bash
cd D:/andriod-project/pose-game
git add android-app/app/src/main/java/com/posegame/app/data/model/RecordListResponse.java
git commit -m "feat(model): add mediaUrl to record list response"
```

---

## Task 5: ApiService - 加 uploadImage、给 submitRecord 加 mediaUrl

**Files:**
- Modify: `android-app/app/src/main/java/com/posegame/app/data/api/ApiService.java`

- [ ] **Step 1: 加 import**

在文件顶部 import 区追加：
```java
import com.posegame.app.data.model.UploadImageResponse;
import okhttp3.MultipartBody;
```
（确认 `MultipartBody` 没重复 import）

- [ ] **Step 2: 加 uploadImage 接口**

在 `// ========== Game Record ==========` 之前插入：
```java
    // ========== Media Upload ==========

    @Multipart
    @POST("game/record/media")
    Call<ApiResponse<UploadImageResponse>> uploadImage(@Part MultipartBody.Part image);
```

- [ ] **Step 3: 修改 submitRecord 加 mediaUrl 参数**

找到原方法：
```java
    @POST("game/record")
    Call<ApiResponse<GameRecordSubmitResponse>> submitRecord(
            @Header("Authorization") String token,
            @Query("levelId") int levelId,
            @Query("score") int score,
            @Query("isPass") boolean isPass
    );
```

改为：
```java
    @POST("game/record")
    Call<ApiResponse<GameRecordSubmitResponse>> submitRecord(
            @Header("Authorization") String token,
            @Query("levelId") int levelId,
            @Query("score") int score,
            @Query("isPass") boolean isPass,
            @Query("mediaUrl") String mediaUrl
    );
```

- [ ] **Step 4: 编译验证**

Run: `cd android-app && ./gradlew compileDebugJavaWithJavac`
Expected: BUILD SUCCESSFUL（无错误）

- [ ] **Step 5: 提交**

```bash
cd D:/andriod-project/pose-game
git add android-app/app/src/main/java/com/posegame/app/data/api/ApiService.java
git commit -m "feat(api): add uploadImage endpoint and mediaUrl param to submitRecord"
```

---

## Task 6: CameraActivity - 集成 BestFrameTracker

**Files:**
- Modify: `android-app/app/src/main/java/com/posegame/app/ui/camera/CameraActivity.java`

- [ ] **Step 1: 加 import**

在 import 区追加：
```java
import com.posegame.app.data.model.ApiResponse;
import com.posegame.app.data.model.UploadImageResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
```

- [ ] **Step 2: 加字段**

在 `private AtomicInteger lastAnalysisTime` 那行附近加：
```java
    private BestFrameTracker bestFrameTracker;
    private static final int VIDEO_COUNTDOWN_MS = 3000;
```

- [ ] **Step 3: 在 initViews 初始化 tracker**

在 `poseDetector = new MediaPipePoseDetector(this);` 后加：
```java
        bestFrameTracker = new BestFrameTracker();
```

- [ ] **Step 4: 修改 RadioGroup 监听器，切换模式时 reset**

找到 `rgMode.setOnCheckedChangeListener((group, checkedId) -> { isPhotoMode = checkedId == R.id.rbPhoto; });`

改为：
```java
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            isPhotoMode = checkedId == R.id.rbPhoto;
            if (bestFrameTracker != null) bestFrameTracker.reset();
        });
```

- [ ] **Step 5: 修改 imageAnalysis 回调**

找到 `imageAnalysis.setAnalyzer(cameraExecutor, image -> {` 那块完整 lambda。整块替换为：

```java
        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            int now = (int) System.currentTimeMillis();
            if (now - lastAnalysisTime.get() < ANALYSIS_INTERVAL_MS) {
                image.close();
                return;
            }
            lastAnalysisTime.set(now);

            // 1. ImageProxy → Bitmap
            Bitmap frameBitmap = imageProxyToBitmap(image);
            if (frameBitmap == null) {
                image.close();
                return;
            }

            // 2. 跑 MediaPipe 检测
            List<MediaPipePoseDetector.Keypoint> keypoints = poseDetector.detectFromBitmap(frameBitmap);
            if (keypoints != null && !keypoints.isEmpty()) {
                // 3. 通知 tracker 来了新帧
                bestFrameTracker.onFrame(frameBitmap, keypoints);
                // 4. 异步拿实时分数
                uploadKeypointsForAnalysis(keypoints);
            } else {
                frameBitmap.recycle();
            }
            image.close();
        });
```

- [ ] **Step 6: 修改 uploadKeypointsForAnalysis 响应回调**

在 `poseOverlayView.updateBodyScores(...)` 调用之后、`mainHandler.post` 块结束前，添加：

```java
                        // 录像模式：把分数喂给 BestFrameTracker
                        if (!isPhotoMode) {
                            bestFrameTracker.onScore(currentRealtimeScore);
                        }
```

- [ ] **Step 7: 修改 startCountdown 按模式分流**

把 `startCountdown()` 方法里 `onFinish()` 的内容改为：
```java
            public void onFinish() {
                tvCountdown.setVisibility(View.GONE);
                if (isPhotoMode) {
                    captureImage();
                } else {
                    finishVideoRecording();
                }
            }
```

- [ ] **Step 8: 在 onDestroy 释放 tracker**

在 `cameraExecutor.shutdown();` 前加：
```java
        if (bestFrameTracker != null) bestFrameTracker.release();
```

- [ ] **Step 9: 加新方法 finishVideoRecording / uploadFrameImage / uploadKeypointsForFinalScore / OnImageUploadedListener**

在 `private void goToResult(...)` 之前插入整段：

```java
    private void finishVideoRecording() {
        showLoading(true);

        Bitmap bestFrame = bestFrameTracker.getBestFrame();
        List<MediaPipePoseDetector.Keypoint> bestKeypoints = bestFrameTracker.getBestKeypoints();

        if (bestFrame == null) {
            // 兜底：实时分数从未回来，用 latest
            bestFrame = bestFrameTracker.getFallbackFrame();
            bestKeypoints = bestFrameTracker.getFallbackKeypoints();
        }

        if (bestFrame == null || bestKeypoints == null) {
            mainHandler.post(() -> {
                showLoading(false);
                ToastUtil.show(this, "未检测到姿态，请重试");
                btnCapture.setEnabled(true);
            });
            return;
        }

        uploadFrameImage(bestFrame, imageUrl -> uploadKeypointsForFinalScore(bestKeypoints, imageUrl));
    }

    private void uploadFrameImage(Bitmap bitmap, OnImageUploadedListener listener) {
        File tempFile = new File(getCacheDir(), "frame_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
        } catch (IOException e) {
            listener.onUploaded(null);
            return;
        }

        RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), tempFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", tempFile.getName(), reqFile);

        apiService.uploadImage(body).enqueue(new Callback<ApiResponse<UploadImageResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UploadImageResponse>> call,
                                   Response<ApiResponse<UploadImageResponse>> response) {
                tempFile.delete();
                String url = (response.isSuccessful() && response.body() != null
                    && response.body().getData() != null)
                    ? response.body().getData().getImageUrl() : null;
                listener.onUploaded(url);
            }

            @Override
            public void onFailure(Call<ApiResponse<UploadImageResponse>> call, Throwable t) {
                tempFile.delete();
                listener.onUploaded(null);
            }
        });
    }

    private void uploadKeypointsForFinalScore(List<MediaPipePoseDetector.Keypoint> keypoints, String imageUrl) {
        List<Map<String, Object>> keypointData = new ArrayList<>();
        for (MediaPipePoseDetector.Keypoint kp : keypoints) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", kp.name);
            map.put("x", kp.x);
            map.put("y", kp.y);
            map.put("z", kp.z);
            keypointData.add(map);
        }

        RealtimePoseRequest request = new RealtimePoseRequest(keypointData);

        apiService.recognizePoseKeypoints(request, levelId).enqueue(new Callback<ApiResponse<PoseResult>>() {
            @Override
            public void onResponse(Call<ApiResponse<PoseResult>> call, Response<ApiResponse<PoseResult>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<PoseResult> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        goToResult(apiResponse.getData(), imageUrl);
                    } else {
                        mainHandler.post(() -> {
                            ToastUtil.show(CameraActivity.this, apiResponse.getMsg());
                            btnCapture.setEnabled(true);
                        });
                    }
                } else {
                    mainHandler.post(() -> {
                        ToastUtil.show(CameraActivity.this, R.string.msg_network_error);
                        btnCapture.setEnabled(true);
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PoseResult>> call, Throwable t) {
                showLoading(false);
                mainHandler.post(() -> {
                    ToastUtil.show(CameraActivity.this, R.string.msg_network_error);
                    btnCapture.setEnabled(true);
                });
            }
        });
    }

    interface OnImageUploadedListener {
        void onUploaded(String imageUrl);
    }
```

- [ ] **Step 10: 修改 goToResult 接收 imageUrl**

把原 `goToResult(PoseResult result)` 改为：
```java
    private void goToResult(PoseResult result, String imageUrl) {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("level_id", levelId);
        intent.putExtra("level_name", levelName);
        intent.putExtra("score", result.getScore());
        intent.putExtra("is_pass", result.isPass());
        intent.putExtra("sticker_reward", result.getStickerReward());
        intent.putExtra("media_url", imageUrl);  // 录像模式才有，拍照模式为 null
        if (result.getMatchDetails() != null) {
            intent.putExtra("match_details_head", result.getMatchDetails().get("head") != null ? result.getMatchDetails().get("head") : 0);
            intent.putExtra("match_details_shoulders", result.getMatchDetails().get("shoulders") != null ? result.getMatchDetails().get("shoulders") : 0);
            intent.putExtra("match_details_arms", result.getMatchDetails().get("arms") != null ? result.getMatchDetails().get("arms") : 0);
            intent.putExtra("match_details_body", result.getMatchDetails().get("body") != null ? result.getMatchDetails().get("body") : 0);
        }
        startActivity(intent);
        finish();
    }
```

然后在 `uploadImage` 那块（拍照模式）调用 `goToResult` 的地方也要更新传 `null`：
找到 `goToResult(apiResponse.getData());`（拍照流程），改为 `goToResult(apiResponse.getData(), null);`

- [ ] **Step 11: 编译验证**

Run: `cd android-app && ./gradlew compileDebugJavaWithJavac`
Expected: BUILD SUCCESSFUL

- [ ] **Step 12: 提交**

```bash
cd D:/andriod-project/pose-game
git add android-app/app/src/main/java/com/posegame/app/ui/camera/CameraActivity.java
git commit -m "feat(camera): split photo/video mode flow, upload best frame in video mode"
```

---

## Task 7: ResultActivity - 提交记录时附带 mediaUrl

**Files:**
- Modify: `android-app/app/src/main/java/com/posegame/app/ui/result/ResultActivity.java`

- [ ] **Step 1: 读 ResultActivity 找 submitRecord 调用点**

Run: `cat "android-app/app/src/main/java/com/posegame/app/ui/result/ResultActivity.java" | head -120`

找 `apiService.submitRecord(...)` 调用位置。

- [ ] **Step 2: 读 mediaUrl 来自 Intent**

在该方法（一般是 onCreate）顶部或 submitRecord 调用前加：
```java
        String mediaUrl = getIntent().getStringExtra("media_url");  // 录像模式才有
```

- [ ] **Step 3: 修改 submitRecord 调用**

把 `apiService.submitRecord(token, levelId, score, isPass)` 改为：
```java
        apiService.submitRecord(token, levelId, score, isPass, mediaUrl)
```

- [ ] **Step 4: 编译验证**

Run: `cd android-app && ./gradlew compileDebugJavaWithJavac`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
cd D:/andriod-project/pose-game
git add android-app/app/src/main/java/com/posegame/app/ui/result/ResultActivity.java
git commit -m "feat(result): pass mediaUrl to submitRecord when present"
```

---

## Task 8: 后端 - 加 /api/game/record/media 接口

**Files:**
- Modify: `backend/app/routers/game.py`

- [ ] **Step 1: 加 import**

在文件顶部 import 区追加：
```python
import os
import uuid
from fastapi import File, UploadFile
```

- [ ] **Step 2: 加新接口**

在 `submit_record` 上方插入：
```python
@router.post("/record/media")
async def upload_record_media(
    image: UploadFile = File(...),
    authorization: str = Header(None),
    db: Session = Depends(get_db)
):
    """上传闯关截图，返回可访问的 URL"""
    get_user_id(authorization)  # 鉴权，未登录直接 401

    save_dir = "static/records"
    os.makedirs(save_dir, exist_ok=True)
    ext = os.path.splitext(image.filename or "")[1] or ".jpg"
    filename = f"{uuid.uuid4().hex}{ext}"
    save_path = os.path.join(save_dir, filename)

    contents = await image.read()
    with open(save_path, "wb") as f:
        f.write(contents)

    return {
        "code": 200,
        "msg": "上传成功",
        "data": {"imageUrl": f"/static/records/{filename}"}
    }
```

- [ ] **Step 3: 修改 submit_record 加 mediaUrl**

把 `def submit_record(...)` 改为：
```python
@router.post("/record", response_model=dict)
def submit_record(
    levelId: int,
    score: int,
    isPass: bool,
    mediaUrl: str = None,
    authorization: str = Header(None),
    db: Session = Depends(get_db)
):
    user_id = get_user_id(authorization)

    level = db.query(Level).filter(Level.id == levelId).first()
    if not level:
        raise HTTPException(status_code=404, detail="关卡不存在")

    db_record = GameRecord(
        user_id=user_id,
        level_id=levelId,
        score=score,
        is_pass=isPass,
        media_url=mediaUrl
    )
    db.add(db_record)
    # ... 后面 user / stickers / commit 逻辑不变
```

- [ ] **Step 4: 修改 get_records 返回 mediaUrl**

在 `result.append({...})` 块内追加 `"mediaUrl": record.media_url,` 字段。

- [ ] **Step 5: 启动后端验证基本启动**

Run: `cd backend && python -m uvicorn app.main:app --reload --port 5000`
Expected: 服务启动无报错

- [ ] **Step 6: 测试图片上传接口（手动）**

Run:
```bash
# 准备一个测试图片
curl -X POST -H "Authorization: Bearer <test_token>" \
     -F "image=@test.jpg" \
     http://localhost:5000/api/game/record/media
```
Expected: 返回 `{"code":200, "data":{"imageUrl":"/static/records/xxx.jpg"}, ...}`

- [ ] **Step 7: 提交**

```bash
cd D:/andriod-project/pose-game
git add backend/app/routers/game.py
git commit -m "feat(backend): add /record/media upload endpoint, mediaUrl in record CRUD"
```

---

## Task 9: 后端 - main.py 挂载 static 目录

**Files:**
- Modify: `backend/app/main.py`

- [ ] **Step 1: 加 import 和挂载**

在 `app.add_middleware(CORSMiddleware, ...)` 之后插入：
```python
import os
os.makedirs("static/records", exist_ok=True)
app.mount("/static", StaticFiles(directory="static"), name="static")
```

并在文件顶部 import 区追加：
```python
from fastapi.staticfiles import StaticFiles
```

- [ ] **Step 2: 验证 static 文件可访问**

Run: 重启后端，然后
```bash
curl -I http://localhost:5000/static/records/  # 列出目录应该返回 200
```
或访问一个已知文件:
```bash
curl -I http://localhost:5000/static/records/<filename>.jpg
```
Expected: 200 OK

- [ ] **Step 3: 提交**

```bash
cd D:/andriod-project/pose-game
git add backend/app/main.py
git commit -m "feat(backend): mount static dir for serving uploaded record images"
```

---

## Task 10: 端到端验证

- [ ] **Step 1: 跑所有 Android 单元测试**

Run: `cd android-app && ./gradlew test`
Expected: 全部 PASS，无 FAILURE

- [ ] **Step 2: 编译完整 APK**

Run: `cd android-app && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL，生成 `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: 手动功能测试**

按下面清单逐项跑（需要真机/模拟器 + 后端运行）：

| # | 场景 | 期望结果 |
|---|------|----------|
| 1 | 进入 CameraActivity（录像模式默认） | 头/肩/手/腰/腿 节点立即显示 |
| 2 | 切换拍照/录像 | 节点持续显示，无白屏 |
| 3 | 录像模式点按钮 | 3-2-1 倒计时，期间分数和节点更新 |
| 4 | 倒计时结束 | 跳 ResultActivity，列表页有缩略图 |
| 5 | 拍照模式点按钮 | 行为不变，列表页无缩略图 |
| 6 | 关闭后端（media 接口 500） | Toast 提示，倒计时结束后仍能跳结果页 |
| 7 | 切换前后摄像头 | bestFrame 清空，节点重新检测 |
| 8 | 倒计时中按返回 | 不卡死，资源释放 |

- [ ] **Step 4: 最终提交（如果还有未提交的修改）**

```bash
cd D:/andriod-project/pose-game
git status  # 确认无残留
```

如果都干净，结束。

---

## 自审

- [x] **Spec 覆盖检查**：
  - 修复录像模式不进入实时检测 → Task 6 (imageAnalysis 回调、startCountdown 分流)
  - 显示节点 → 已有，Task 6 不破坏
  - 保存截图到记录 → Task 5/6/7/8/9
  - 错误处理 → Task 6 中 imageAnalysis 失败 fallback + 上传失败不阻塞
  - 测试 → Task 1-2 单元测试 + Task 10 端到端
  - 文件结构与 spec 一致
- [x] **占位符扫描**：无 TBD/TODO
- [x] **类型一致性**：
  - `BestFrameTracker.onFrame(Bitmap, List<Keypoint>)` — 与 Task 6 调用一致
  - `BestFrameTracker.onScore(int)` — 与 Task 6 调用一致
  - `BestFrameTracker.getBestFrame() / getBestKeypoints() / getBestScore() / getFallbackFrame() / getFallbackKeypoints() / reset() / release()` — 全部使用一致
  - `ApiService.uploadImage(MultipartBody.Part)` 返回 `Call<ApiResponse<UploadImageResponse>>` — 与 Task 6 回调一致
  - `ResultActivity.getStringExtra("media_url")` — 与 CameraActivity `putExtra("media_url", imageUrl)` 一致
