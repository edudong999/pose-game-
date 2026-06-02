package com.posegame.app.ui.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.AspectRatio;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.posegame.app.R;
import com.posegame.app.data.api.ApiService;
import com.posegame.app.data.api.RetrofitClient;
import com.posegame.app.data.model.ApiResponse;
import com.posegame.app.data.model.PoseResult;
import com.posegame.app.data.model.RealtimePoseRequest;
import com.posegame.app.data.model.RealtimePoseResult;
import com.posegame.app.data.model.UploadImageResponse;
import com.posegame.app.ui.result.ResultActivity;
import com.posegame.app.util.ToastUtil;
import com.posegame.app.data.model.Level;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Camera Activity with CameraX for proper aspect ratio handling
 */
public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "PoseCamera";

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private PreviewView previewView;
    private ImageButton btnClose;
    private ImageButton btnSwitchCamera;
    private ImageButton btnCapture;
    private TextView tvCountdown;
    private TextView tvLevelName;
    private RadioGroup rgMode;
    private FrameLayout loadingOverlay;
    private PoseOverlayView poseOverlayView;
    private TextView tvDebugStatus;

    private ApiService apiService;
    private Level currentLevel;

    private int levelId;
    private String levelName;
    private boolean isPhotoMode = true;
    private boolean lensFacingFront = true;

    private ExecutorService cameraExecutor;
    private Handler mainHandler;
    private java.util.concurrent.atomic.AtomicLong lastProcessLogMs = new java.util.concurrent.atomic.AtomicLong(0);

    // CameraX
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Preview preview;

    // Pose detection
    private MediaPipePoseDetector poseDetector;
    private AtomicInteger analyzerInvocationCount = new AtomicInteger(0);
    private int currentRealtimeScore = 0;
    private BestFrameTracker bestFrameTracker;

    // 帧轮询：录像模式倒计时期间用 imageCapture.takePicture 定时抓帧。
    // 解决 Oplus 设备上 imageAnalysis 不投递帧的问题。
    private Handler framePollingHandler;
    private Runnable framePollingRunnable;
    private boolean isPolling = false;
    private static final int FRAME_POLL_INTERVAL_MS = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        setDebugStatus("onCreate levelId=" + levelId);

        apiService = RetrofitClient.getInstance().getApiService();
        levelId = getIntent().getIntExtra("level_id", 1);
        levelName = getIntent().getStringExtra("level_name");

        cameraExecutor = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        setupListeners();
        loadLevelData();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        btnClose = findViewById(R.id.btnClose);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        btnCapture = findViewById(R.id.btnCapture);
        tvCountdown = findViewById(R.id.tvCountdown);
        tvLevelName = findViewById(R.id.tvLevelName);
        rgMode = findViewById(R.id.rgMode);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        poseOverlayView = findViewById(R.id.poseOverlayView);
        tvDebugStatus = findViewById(R.id.tvDebugStatus);

        tvLevelName.setText(levelName != null ? levelName : "关卡" + levelId);

        // Set scale type for proper aspect ratio
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);

        // Initialize MediaPipe pose detector
        poseDetector = new MediaPipePoseDetector(this);
        bestFrameTracker = new BestFrameTracker();
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v -> finish());
        btnSwitchCamera.setOnClickListener(v -> switchCamera());
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            isPhotoMode = checkedId == R.id.rbPhoto;
            if (bestFrameTracker != null) bestFrameTracker.reset();
        });
        btnCapture.setOnClickListener(v -> startCountdown());
    }

    private void loadLevelData() {
        apiService.getLevelDetail(levelId).enqueue(new Callback<ApiResponse<Level>>() {
            @Override
            public void onResponse(Call<ApiResponse<Level>> call, Response<ApiResponse<Level>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    currentLevel = response.body().getData();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Level>> call, Throwable t) { }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
            ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                mainHandler.post(() -> ToastUtil.show(this, "相机初始化失败"));
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        setDebugStatus("bindCamera lensFront=" + lensFacingFront);
        // Unbind all use cases before rebinding
        cameraProvider.unbindAll();

        // Camera selector - front or back
        CameraSelector cameraSelector = new CameraSelector.Builder()
            .requireLensFacing(lensFacingFront ?
                CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK)
            .build();

        // Preview use case
        preview = new Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // ImageCapture use case
        imageCapture = new ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build();

        // 之前用 ImageAnalysis 走实时检测，但在 Oplus 设备上 ImageAnalysis 跟 ImageCapture
        // 同时绑定时框架不投递帧（bind OK 但 analyzer 永远不被调用）。改用 imageCapture
        // 轮询：startCountdown 期间每隔 500ms takePicture 一次，绕过 ImageAnalysis 链路。
        // 详见 startFramePolling() / processImageProxy()。

        try {
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture
            );
            setDebugStatus("bind OK usecases=2 (preview+imageCapture)");
        } catch (Exception e) {
            setDebugStatus("BIND FAIL: " + e.getClass().getSimpleName() + " " + e.getMessage());
            mainHandler.post(() -> ToastUtil.show(this, "相机启动失败"));
        }
    }

    private void switchCamera() {
        lensFacingFront = !lensFacingFront;
        poseDetector.setFrontCamera(lensFacingFront);
        bindCameraUseCases();
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                ToastUtil.show(this, "相机权限被拒绝");
                finish();
            }
        }
    }

    private void startCountdown() {
        btnCapture.setEnabled(false);
        tvCountdown.setVisibility(View.VISIBLE);
        tvCountdown.setText("3");

        if (!isPhotoMode) {
            // 录像模式：倒计时期间持续抓帧喂给 bestFrameTracker
            startFramePolling();
        }

        new android.os.CountDownTimer(3000, 1000) {
            int count = 3;

            public void onTick(long millisUntilFinished) {
                tvCountdown.setText(String.valueOf(count));
                count--;
            }

            public void onFinish() {
                tvCountdown.setVisibility(View.GONE);
                if (isPhotoMode) {
                    captureImage();
                } else {
                    stopFramePolling();
                    finishVideoRecording();
                }
            }
        }.start();
    }

    private void captureImage() {
        if (imageCapture == null) {
            ToastUtil.show(this, "相机未就绪");
            btnCapture.setEnabled(true);
            return;
        }

        showLoading(true);

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = imageProxyToBitmap(image);
                image.close();

                if (bitmap != null) {
                    uploadImage(bitmap);
                } else {
                    mainHandler.post(() -> {
                        showLoading(false);
                        ToastUtil.show(CameraActivity.this, "拍照失败");
                        btnCapture.setEnabled(true);
                    });
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                mainHandler.post(() -> {
                    showLoading(false);
                    ToastUtil.show(CameraActivity.this, "拍照失败: " + exception.getMessage());
                    btnCapture.setEnabled(true);
                });
            }
        });
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        try {
            int format = image.getFormat();
            Bitmap original;
            if (format == android.graphics.ImageFormat.JPEG) {
                // Photo capture path: plane[0] holds the encoded JPEG bytes
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                options.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888;
                original = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            } else {
                // imageAnalysis default: YUV_420_888 (3 planes). Pack into NV21, then
                // let android.graphics.YuvImage do the YUV→JPEG conversion.
                ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
                ImageProxy.PlaneProxy uPlane = image.getPlanes()[1];
                ImageProxy.PlaneProxy vPlane = image.getPlanes()[2];

                ByteBuffer yBuf = yPlane.getBuffer();
                ByteBuffer uBuf = uPlane.getBuffer();
                ByteBuffer vBuf = vPlane.getBuffer();
                int ySize = yBuf.remaining();
                int uSize = uBuf.remaining();
                int vSize = vBuf.remaining();

                // NV21: Y plane then VU interleaved (YuvImage's expected layout)
                byte[] nv21 = new byte[ySize + uSize + vSize];
                yBuf.get(nv21, 0, ySize);
                vBuf.get(nv21, ySize, vSize);
                uBuf.get(nv21, ySize + vSize, uSize);

                android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(
                    nv21, android.graphics.ImageFormat.NV21,
                    image.getWidth(), image.getHeight(), null);
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                yuvImage.compressToJpeg(
                    new android.graphics.Rect(0, 0, image.getWidth(), image.getHeight()),
                    95, out);
                byte[] jpegBytes = out.toByteArray();
                original = android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
            }

            if (original == null) return null;

            // Rotate if needed based on image rotation
            int rotation = image.getImageInfo().getRotationDegrees();
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                if (lensFacingFront) {
                    matrix.postScale(-1, 1); // Mirror for front camera
                }
                Bitmap rotated = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
                original.recycle();
                return rotated;
            }

            return original;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ========== 帧轮询（Oplus 设备 imageAnalysis 兜底） ==========

    private void startFramePolling() {
        if (isPolling) return;
        isPolling = true;
        if (framePollingHandler == null) {
            framePollingHandler = new Handler(Looper.getMainLooper());
        }
        framePollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPolling || imageCapture == null) return;
                imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        processImageProxy(image);
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        // takePicture 偶发失败不阻塞下一轮
                    }
                });
                framePollingHandler.postDelayed(this, FRAME_POLL_INTERVAL_MS);
            }
        };
        framePollingHandler.post(framePollingRunnable);
    }

    private void stopFramePolling() {
        isPolling = false;
        if (framePollingHandler != null && framePollingRunnable != null) {
            framePollingHandler.removeCallbacks(framePollingRunnable);
        }
    }

    private void processImageProxy(ImageProxy image) {
        analyzerInvocationCount.incrementAndGet();
        try {
            long t0 = System.currentTimeMillis();

            Bitmap frameBitmap = imageProxyToBitmap(image);
            long tBitmap = System.currentTimeMillis() - t0;
            if (frameBitmap == null) {
                setDebugStatus("BITMAP NULL format=" + image.getFormat()
                    + " " + image.getWidth() + "x" + image.getHeight()
                    + " bitmap=" + tBitmap + "ms");
                return;
            }

            long tMp = System.currentTimeMillis();
            List<MediaPipePoseDetector.Keypoint> keypoints = poseDetector.detectFromBitmap(frameBitmap);
            long tMpMs = System.currentTimeMillis() - tMp;
            if (keypoints != null && !keypoints.isEmpty()) {
                setDebugStatus("OK " + keypoints.size() + " kp  bitmap=" + tBitmap
                    + "ms mp=" + tMpMs + "ms");
                final List<MediaPipePoseDetector.Keypoint> finalKeypoints = keypoints;
                mainHandler.post(() -> {
                    if (poseOverlayView != null) {
                        poseOverlayView.updateKeypoints(finalKeypoints);
                    }
                });
                bestFrameTracker.onFrame(frameBitmap, keypoints);
                uploadKeypointsForAnalysis(keypoints);
            } else {
                setDebugStatus("MEDIAPIPE EMPTY bitmap=" + tBitmap + "ms");
                frameBitmap.recycle();
            }
        } catch (Throwable t) {
            setDebugStatus("THROWABLE: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        } finally {
            image.close();
        }
    }

    private void uploadImage(Bitmap bitmap) {
        // First detect pose keypoints from bitmap using MediaPipe
        List<MediaPipePoseDetector.Keypoint> keypoints = poseDetector.detectFromBitmap(bitmap);

        if (keypoints == null || keypoints.isEmpty()) {
            mainHandler.post(() -> {
                showLoading(false);
                ToastUtil.show(this, "无法检测到人体姿势");
                btnCapture.setEnabled(true);
            });
            return;
        }

        // Convert keypoints to the format expected by API
        List<Map<String, Object>> keypointData = new ArrayList<>();
        for (MediaPipePoseDetector.Keypoint kp : keypoints) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", kp.name);
            map.put("x", kp.x);
            map.put("y", kp.y);
            map.put("z", kp.z);
            map.put("confidence", kp.confidence);
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
                        goToResult(apiResponse.getData(), null);
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

    private void uploadKeypointsForAnalysis(List<MediaPipePoseDetector.Keypoint> keypoints) {
        // Convert keypoints to map format for JSON
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

        apiService.analyzeRealtimePose(request, levelId).enqueue(new Callback<ApiResponse<RealtimePoseResult>>() {
            @Override
            public void onResponse(Call<ApiResponse<RealtimePoseResult>> call, Response<ApiResponse<RealtimePoseResult>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    RealtimePoseResult result = response.body().getData();
                    currentRealtimeScore = result.getScore();
                    // Update overlay with real-time score and body part scores
                    // (keypoints already updated locally in the analyzer; avoid double-invalidate)
                    mainHandler.post(() -> {
                        if (poseOverlayView != null) {
                            poseOverlayView.updateScore(currentRealtimeScore);
                            poseOverlayView.updateBodyScores(
                                result.getHeadScore(),
                                result.getShoulderScore(),
                                result.getArmScore(),
                                result.getBodyScore()
                            );
                        }
                        // 录像模式：把分数喂给 BestFrameTracker
                        if (!isPhotoMode) {
                            bestFrameTracker.onScore(currentRealtimeScore);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<RealtimePoseResult>> call, Throwable t) {
                // Silently fail for realtime updates
            }
        });
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE));
    }

    private void setDebugStatus(String msg) {
        Log.e(TAG, msg);
        runOnUiThread(() -> {
            if (tvDebugStatus != null) {
                tvDebugStatus.setText("DEBUG: " + msg);
            }
        });
    }

    private void finishVideoRecording() {
        showLoading(true);

        Bitmap bestFrame = bestFrameTracker.getBestFrame();
        List<MediaPipePoseDetector.Keypoint> bestKeypoints = bestFrameTracker.getBestKeypoints();

        if (bestFrame == null) {
            // 兜底：实时分数从未回来，用 latest
            bestFrame = bestFrameTracker.getFallbackFrame();
            bestKeypoints = bestFrameTracker.getFallbackKeypoints();
        }

        setDebugStatus("FINISH frame=" + (bestFrame != null)
            + " kp=" + (bestKeypoints != null ? bestKeypoints.size() : "null")
            + " score=" + bestFrameTracker.getBestScore()
            + " latest=" + (bestFrameTracker.getFallbackFrame() != null));

        if (bestFrame == null || bestKeypoints == null) {
            mainHandler.post(() -> {
                showLoading(false);
                ToastUtil.show(this, "未检测到姿态，请重试");
                btnCapture.setEnabled(true);
            });
            return;
        }

        final Bitmap finalFrame = bestFrame;
        final List<MediaPipePoseDetector.Keypoint> finalKeypoints = bestKeypoints;
        uploadFrameImage(finalFrame, imageUrl -> uploadKeypointsForFinalScore(finalKeypoints, imageUrl));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bestFrameTracker != null) bestFrameTracker.release();
        cameraExecutor.shutdown();
    }
}