# 录像模式实时姿态检测 + 截图保存设计

## 概述

修复 `CameraActivity` 录像模式（rbVideo）不进入实时姿态检测的问题，并增加将最佳帧截图保存到闯关记录的功能。

现状：
- `imageAnalysis` 已经接入 `MediaPipePoseDetector`，每 500ms 一次本地检测
- `PoseOverlayView` 已经在画骨骼图和头/肩/手/腰/腿 5 个部位的节点
- 实时分数通过 `/pose/analyze/realtime` 持续更新
- `isPhotoMode` 标志被设置但**没有任何地方使用** —— 按钮永远走 `captureImage()` 拍照流程
- `GameRecord` 表已有 `media_url` 字段但接口未使用

## 架构

```
┌─────────────────────────────────────────────────────────────┐
│ CameraActivity                                              │
│                                                             │
│  PreviewView (相机预览)                                      │
│      ↓ 共享画面                                              │
│  ImageAnalysis (cameraExecutor 线程)                         │
│      ├─ MediaPipePoseDetector.detectFromImage()             │
│      ├─ 暂存当前帧 bitmap + keypoints（BestFrameTracker）     │
│      └─ /pose/analyze/realtime → 更新 score + bodyScores    │
│                                                             │
│  PoseOverlayView (主线程)                                    │
│      └─ 实时显示节点 + 实时分数                                │
│                                                             │
│  btnCapture 点击 → 按 isPhotoMode 分流:                      │
│      ├─ true  → 3s 倒计时 → imageCapture → /pose/recognize  │
│      └─ false → 3s 倒计时 → 取最佳帧 → 上传 → /pose/recognize│
│                                          → /game/record     │
└─────────────────────────────────────────────────────────────┘
```

## 组件改动

### 1. CameraActivity.java (修改)

**位置**: `android-app/app/src/main/java/com/posegame/app/ui/camera/CameraActivity.java`

**改动内容**：

a) 引入 `BestFrameTracker` 字段：
```java
private BestFrameTracker bestFrameTracker;
```

b) `imageAnalysis.setAnalyzer()` 回调中，检测出关键点后：
```java
// 1. ImageProxy → Bitmap（保留一份用于最佳帧）
Bitmap frameBitmap = imageProxyToBitmap(image);
if (frameBitmap == null) {
    image.close();
    return;
}

// 2. 用同一份 bitmap 跑 MediaPipe 检测
List<Keypoint> keypoints = poseDetector.detectFromBitmap(frameBitmap);
if (keypoints != null && !keypoints.isEmpty()) {
    // 3. 通知 bestFrameTracker 来了新帧
    bestFrameTracker.onFrame(frameBitmap, keypoints);
    // 4. 异步拿实时分数（响应回来时再回调 bestFrameTracker.onScore）
    uploadKeypointsForAnalysis(keypoints);
} else {
    frameBitmap.recycle();
}
image.close();
```

c) 拆分 `startCountdown()`：
```java
private void startCountdown() {
    btnCapture.setEnabled(false);
    tvCountdown.setVisibility(View.VISIBLE);
    tvCountdown.setText("3");

    new CountDownTimer(3000, 1000) {
        public void onTick(long millisUntilFinished) {
            tvCountdown.setText(String.valueOf(count));
            count--;
        }
        public void onFinish() {
            tvCountdown.setVisibility(View.GONE);
            if (isPhotoMode) {
                captureImage();
            } else {
                finishVideoRecording();
            }
        }
    }.start();
}
```

d) 新增 `finishVideoRecording()`：
```java
private void finishVideoRecording() {
    showLoading(true);

    // 1. 拿到最佳帧的 bitmap 和 keypoints
    Bitmap bestFrame = bestFrameTracker.getBestFrame();
    List<Keypoint> bestKeypoints = bestFrameTracker.getBestKeypoints();

    if (bestFrame == null || bestKeypoints == null) {
        // 兜底：实时检测全失败
        mainHandler.post(() -> {
            showLoading(false);
            ToastUtil.show(this, "未检测到姿态，请重试");
            btnCapture.setEnabled(true);
        });
        return;
    }

    // 2. 上传图片到后端（失败不阻塞）
    uploadFrameImage(bestFrame, imageUrl -> {
        // 3. 用最佳帧的 keypoints 调 /pose/recognize 拿最终分数
        uploadKeypointsForFinalScore(bestKeypoints, imageUrl);
    });
}
```

e) 新增 `uploadFrameImage(Bitmap, callback)`：
```java
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
        @Override public void onResponse(Call<ApiResponse<UploadImageResponse>> call, 
                                         Response<ApiResponse<UploadImageResponse>> response) {
            tempFile.delete();
            String url = (response.body() != null && response.body().getData() != null)
                ? response.body().getData().getImageUrl() : null;
            listener.onUploaded(url);
        }
        @Override public void onFailure(Call<ApiResponse<UploadImageResponse>> call, Throwable t) {
            tempFile.delete();
            listener.onUploaded(null);
        }
    });
}

interface OnImageUploadedListener {
    void onUploaded(String imageUrl);
}
```

f) 修改 `uploadKeypointsForAnalysis()` 响应回调，在 `mainHandler.post` 里追加：
```java
if (!isPhotoMode) {
    bestFrameTracker.onScore(currentRealtimeScore);
}
```

g) 新增 `uploadKeypointsForFinalScore(keypoints, imageUrl)`：
- 构造 `RealtimePoseRequest` 调 `/pose/recognize?level_id=...`
- 拿到 `PoseResult` 后跳转 ResultActivity，并把 `imageUrl` 也带过去（用 `putExtra`）
- ResultActivity 提交 `/api/game/record` 时把 `mediaUrl=imageUrl` 作为 query 参数传

h) RadioGroup 切换模式时调 `bestFrameTracker.reset()`，避免拍照模式的 lastest 被录像模式误用

i) `onDestroy()` 释放 `bestFrameTracker`：
```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (bestFrameTracker != null) bestFrameTracker.release();
    cameraExecutor.shutdown();
}
```

### 2. BestFrameTracker.java (新建)

**位置**: `android-app/app/src/main/java/com/posegame/app/ui/camera/BestFrameTracker.java`

**职责**：在 `imageAnalysis` 线程安全地记录"分数最高的那一帧"

**关键字段**：
```java
private Bitmap latestBitmap;       // 最近一帧
private List<Keypoint> latestKeypoints;
private Bitmap bestBitmap;         // 最高分帧
private List<Keypoint> bestKeypoints;
private int bestScore = -1;        // -1 表示还没有分数
```

**方法**（用 `synchronized` 保证线程安全，简单可靠）：
```java
// imageAnalyzer 线程：每来一帧就调一次
public synchronized void onFrame(Bitmap frame, List<Keypoint> keypoints) {
    // 释放上一帧 latest（除非它正好是 best）
    if (latestBitmap != null && latestBitmap != frame && latestBitmap != bestBitmap 
        && !latestBitmap.isRecycled()) {
        latestBitmap.recycle();
    }
    latestBitmap = frame;
    latestKeypoints = new ArrayList<>(keypoints);
}

// 主线程：实时分数响应回来时调
public synchronized void onScore(int score) {
    if (score > bestScore) {
        // 释放上一个 best（除非它正好是 latest，下一帧还会用到）
        if (bestBitmap != null && bestBitmap != latestBitmap 
            && !bestBitmap.isRecycled()) {
            bestBitmap.recycle();
        }
        bestBitmap = latestBitmap;
        bestKeypoints = latestKeypoints;
        bestScore = score;
    }
}

// 主线程：录像倒计时结束，调一次
public synchronized Bitmap getBestFrame() { return bestBitmap; }
public synchronized List<Keypoint> getBestKeypoints() { return bestKeypoints; }
public synchronized int getBestScore() { return bestScore; }

// 兜底：没有任何 best（实时分数从未回来）→ 返回 latest
public synchronized Bitmap getFallbackFrame() { return latestBitmap; }
public synchronized List<Keypoint> getFallbackKeypoints() { return latestKeypoints; }

public synchronized void reset() { 
    bestScore = -1; 
    bestBitmap = null; bestKeypoints = null;
    // latest 也清掉，释放内存
    if (latestBitmap != null && !latestBitmap.isRecycled()) latestBitmap.recycle();
    latestBitmap = null; latestKeypoints = null;
}

public synchronized void release() { reset(); }
```

**线程安全说明**：
- `onFrame()` 在 `cameraExecutor` 线程调用（imageAnalyzer 回调）
- `onScore()` 在 `mainHandler` 线程调用（retrofit 响应回来后 `mainHandler.post`）
- `getBestFrame()` / `getFallbackFrame()` 在主线程调用（按钮点击后）
- 用 `synchronized` 方法 + bitmap 引用判断（latest==best 时不重复 recycle），保证安全

**分数延迟说明**：
- 实时分数响应有约 100-300ms 网络延迟，收到时 `latestBitmap` 可能已被新帧替换
- 此时 `onScore(score)` 会把当前 `latest`（新帧）作为 best —— 即"分数和帧有一帧左右的错位"
- 在 3 秒倒计时 / 500ms 间隔下，错位影响很小（人体动作在 500ms 内变化不大）

### 3. ApiService.java (修改)

**位置**: `android-app/app/src/main/java/com/posegame/app/data/api/ApiService.java`

**改动**：
```java
// 新增：上传图片
@Multipart
@POST("game/record/media")
Call<ApiResponse<UploadImageResponse>> uploadImage(@Part MultipartBody.Part image);

// 修改：提交记录支持 mediaUrl
@POST("game/record")
Call<ApiResponse<GameRecordSubmitResponse>> submitRecord(
    @Header("Authorization") String token,
    @Query("levelId") int levelId,
    @Query("score") int score,
    @Query("isPass") boolean isPass,
    @Query("mediaUrl") String mediaUrl  // 新增，可为 null
);
```

### 4. UploadImageResponse.java (新建)

**位置**: `android-app/app/src/main/java/com/posegame/app/data/model/UploadImageResponse.java`

```java
public class UploadImageResponse {
    private String imageUrl;
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
```

### 5. RecordListResponse.java (修改)

**位置**: `android-app/app/src/main/java/com/posegame/app/data/model/RecordListResponse.java`

**改动**：增加 `mediaUrl` 字段，用于列表缩略图展示。

### 6. 后端改动（用户实现）

**文件**：`backend/app/routers/game.py`

a) 新增接口：
```python
@router.post("/record/media")
async def upload_record_media(
    image: UploadFile = File(...),
    authorization: str = Header(None),
    db: Session = Depends(get_db)
):
    get_user_id(authorization)  # 鉴权

    # 保存到 static/records/ 目录
    import os, uuid
    save_dir = "static/records"
    os.makedirs(save_dir, exist_ok=True)
    ext = os.path.splitext(image.filename)[1] or ".jpg"
    filename = f"{uuid.uuid4().hex}{ext}"
    save_path = os.path.join(save_dir, filename)
    with open(save_path, "wb") as f:
        f.write(await image.read())

    return {
        "code": 200,
        "msg": "上传成功",
        "data": {"imageUrl": f"/static/records/{filename}"}
    }
```

b) 修改 `submit_record` 增加 `mediaUrl` 参数：
```python
@router.post("/record")
def submit_record(
    levelId: int,
    score: int,
    isPass: bool,
    mediaUrl: str = None,  # 新增
    authorization: str = Header(None),
    db: Session = Depends(get_db)
):
    ...
    db_record = GameRecord(
        user_id=user_id,
        level_id=levelId,
        score=score,
        is_pass=isPass,
        media_url=mediaUrl  # 新增
    )
```

c) 修改 `get_records` 返回 `mediaUrl`：
```python
result.append({
    "id": record.id,
    "levelId": record.level_id,
    "levelName": level.name if level else "未知关卡",
    "score": record.score,
    "isPass": record.is_pass,
    "mediaUrl": record.media_url,  # 新增
    "createdAt": record.created_at.isoformat() if record.created_at else None
})
```

d) main.py 增加静态文件挂载：
```python
from fastapi.staticfiles import StaticFiles
app.mount("/static", StaticFiles(directory="static"), name="static")
```

## 数据流

### 录像模式（修复 + 截图）

```
1. 用户进入 CameraActivity
2. 相机启动，imageAnalysis 开始分析
   - 每 500ms: MediaPipe 检测 → keypoints
   - bestFrameTracker.offer(bitmap, keypoints, realtimeScore)
   - 调 /pose/analyze/realtime 更新 overlay
3. PoseOverlayView 持续显示节点和实时分数
4. 用户点击 btnCapture
   - 启动 3 秒倒计时
   - 倒计时期间: imageAnalysis 继续，累计 bestFrame
   - 倒计时结束:
     a. bestFrameTracker.getBestFrame() 拿最佳帧
     b. 上传到 /api/game/record/media → 拿 imageUrl（失败不阻塞）
     c. 用 bestKeypoints 调 /pose/recognize → 拿最终分数
     d. 调 /api/game/record 提交，附带 mediaUrl=imageUrl
     e. 跳到 ResultActivity
```

### 拍照模式（保持不变）

```
1. imageAnalysis 后台运行 + overlay 持续显示
2. btnCapture → 3s 倒计时
3. 倒计时结束 → imageCapture.takePicture() → /pose/recognize → /api/game/record → ResultActivity
```

## 错误处理

| 情况 | 行为 |
|------|------|
| 倒计时期间没检测到人 | 用 fallback 关键点（已有），score=0，bestFrame 仍为最后一帧 |
| 后端实时分数超时 | offer 时 score=0 不会更新 best，bestFrame 保留旧的 |
| /game/record/media 上传失败 | Toast 提示，imageUrl=null，继续调 /pose/recognize |
| /pose/recognize 失败 | Toast 提示，按钮恢复，停在 CameraActivity |
| 倒计时期间用户按返回 | Activity 销毁，bitmap recycle，cameraExecutor shutdown |
| 录像模式快速切拍照模式 | bestFrameTracker.reset() 清空（RadioGroup 切换时调用） |

## 边界情况

- **首次启动无 bestFrame**：score=-1 表示无，offer 时任何 score >= 0 都会更新
- **连续相同分数**：使用严格 `>` 比较，避免无意义替换
- **bitmap 内存**：用 WeakReference + 主动 recycle，最佳帧切换时释放旧帧
- **临时文件清理**：上传成功 / 失败都删除 `cacheDir/frame_xxx.jpg`

## 测试

### 单元测试

- `BestFrameTrackerTest`：6 帧 80/85/90/82/88/95 → 选 95；全 0 → 选最后；reset 后清空
- `BestFrameTrackerTest`：bitmap recycle 验证（旧 bestFrame 切换时回收）

### 手动测试

| 场景 | 期望 |
|------|------|
| 进入录像模式 | 节点（头/肩/手/腰/腿）立即可见 |
| 拍照 → 录像切换 | 节点持续显示，无白屏 |
| 录像模式点击按钮 | 3 秒倒计时，期间分数和节点更新 |
| 倒计时结束 | 跳到结果页，records 列表显示缩略图 |
| 拍照模式点击 | 行为不变，无缩略图 |
| 后端宕机（/game/record/media） | 倒计时结束不卡死，Toast 提示 |
| 后端不返回 imageUrl | 记录能正常提交，列表不显示缩略图 |
| 切换前/后摄像头 | bestFrame 清空，节点重新检测 |

## 文件变更清单

**新建**：
- `android-app/app/src/main/java/com/posegame/app/ui/camera/BestFrameTracker.java`
- `android-app/app/src/main/java/com/posegame/app/data/model/UploadImageResponse.java`

**修改**：
- `android-app/app/src/main/java/com/posegame/app/ui/camera/CameraActivity.java`
- `android-app/app/src/main/java/com/posegame/app/data/api/ApiService.java`
- `android-app/app/src/main/java/com/posegame/app/data/model/RecordListResponse.java`
- `backend/app/routers/game.py`
- `backend/app/main.py`（加 StaticFiles 挂载）

**接口变更**：
- 新增 `POST /api/game/record/media`（后端）
- `POST /api/game/record` 增加可选 `mediaUrl` query
- `GET /api/game/records` 返回 `mediaUrl` 字段
