package com.posegame.app.ui.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
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
import com.posegame.app.ui.result.ResultActivity;
import com.posegame.app.util.ToastUtil;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Camera Activity for capturing pose images
 */
public class CameraActivity extends AppCompatActivity {

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

    private ApiService apiService;
    private int levelId;
    private String levelName;
    private boolean isPhotoMode = true;
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        apiService = RetrofitClient.getInstance().getApiService();
        levelId = getIntent().getIntExtra("level_id", 1);
        levelName = getIntent().getStringExtra("level_name");

        cameraExecutor = Executors.newSingleThreadExecutor();

        initViews();
        setupListeners();

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

        tvLevelName.setText(levelName != null ? levelName : "关卡" + levelId);
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v -> finish());

        btnSwitchCamera.setOnClickListener(v -> {
            lensFacing = lensFacing == CameraSelector.LENS_FACING_FRONT
                    ? CameraSelector.LENS_FACING_BACK
                    : CameraSelector.LENS_FACING_FRONT;
            startCamera();
        });

        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            isPhotoMode = checkedId == R.id.rbPhoto;
        });

        btnCapture.setOnClickListener(v -> startCountdown());
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

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                ToastUtil.show(this, "相机启动失败");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startCountdown() {
        btnCapture.setEnabled(false);
        tvCountdown.setVisibility(View.VISIBLE);
        tvCountdown.setText("3");

        new CountDownTimer(3000, 1000) {
            int count = 3;

            public void onTick(long millisUntilFinished) {
                tvCountdown.setText(String.valueOf(count));
                count--;
            }

            public void onFinish() {
                tvCountdown.setVisibility(View.GONE);
                captureImage();
            }
        }.start();
    }

    private void captureImage() {
        if (imageCapture == null) return;

        File photoFile = new File(getCacheDir(), "pose_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(() -> uploadImage(photoFile));
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> {
                    ToastUtil.show(CameraActivity.this, "拍照失败");
                    btnCapture.setEnabled(true);
                });
            }
        });
    }

    private void uploadImage(File photoFile) {
        showLoading(true);

        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), photoFile);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", photoFile.getName(), requestBody);

        apiService.recognizePose(imagePart, levelId).enqueue(new Callback<ApiResponse<PoseResult>>() {
            @Override
            public void onResponse(Call<ApiResponse<PoseResult>> call, Response<ApiResponse<PoseResult>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<PoseResult> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        PoseResult result = apiResponse.getData();
                        goToResult(result);
                    } else {
                        ToastUtil.show(CameraActivity.this, apiResponse.getMsg());
                        btnCapture.setEnabled(true);
                    }
                } else {
                    ToastUtil.show(CameraActivity.this, R.string.msg_network_error);
                    btnCapture.setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PoseResult>> call, Throwable t) {
                showLoading(false);
                ToastUtil.show(CameraActivity.this, R.string.msg_network_error);
                btnCapture.setEnabled(true);
            }
        });
    }

    private void goToResult(PoseResult result) {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("level_id", levelId);
        intent.putExtra("level_name", levelName);
        intent.putExtra("score", result.getScore());
        intent.putExtra("is_pass", result.isPass());
        intent.putExtra("sticker_reward", result.getStickerReward());
        intent.putExtra("match_details_head", result.getMatchDetails() != null ? result.getMatchDetails().get("head") : 0);
        intent.putExtra("match_details_shoulders", result.getMatchDetails() != null ? result.getMatchDetails().get("shoulders") : 0);
        intent.putExtra("match_details_arms", result.getMatchDetails() != null ? result.getMatchDetails().get("arms") : 0);
        intent.putExtra("match_details_body", result.getMatchDetails() != null ? result.getMatchDetails().get("body") : 0);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}