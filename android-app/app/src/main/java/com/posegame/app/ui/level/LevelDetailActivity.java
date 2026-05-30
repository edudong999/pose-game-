package com.posegame.app.ui.level;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.posegame.app.R;
import com.posegame.app.data.api.ApiService;
import com.posegame.app.data.api.RetrofitClient;
import com.posegame.app.data.model.ApiResponse;
import com.posegame.app.data.model.Level;
import com.posegame.app.ui.camera.CameraActivity;
import com.posegame.app.util.ToastUtil;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Level Detail Activity
 */
public class LevelDetailActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvLevelName;
    private ImageView ivPoseImage;
    private TextView tvDescription;
    private TextView tvPassScore;
    private TextView tvRewardSticker;
    private Button btnStartChallenge;

    private ApiService apiService;
    private int levelId;
    private Level currentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_detail);

        apiService = RetrofitClient.getInstance().getApiService();
        levelId = getIntent().getIntExtra("level_id", 1);

        initViews();
        setupListeners();
        loadLevelDetail();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvLevelName = findViewById(R.id.tvLevelName);
        ivPoseImage = findViewById(R.id.ivPoseImage);
        tvDescription = findViewById(R.id.tvDescription);
        tvPassScore = findViewById(R.id.tvPassScore);
        tvRewardSticker = findViewById(R.id.tvRewardSticker);
        btnStartChallenge = findViewById(R.id.btnStartChallenge);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnStartChallenge.setOnClickListener(v -> {
            if (currentLevel != null) {
                Intent intent = new Intent(this, CameraActivity.class);
                intent.putExtra("level_id", levelId);
                intent.putExtra("level_name", currentLevel.getName());
                startActivity(intent);
            }
        });
    }

    private void loadLevelDetail() {
        apiService.getLevelDetail(levelId).enqueue(new Callback<ApiResponse<Level>>() {
            @Override
            public void onResponse(Call<ApiResponse<Level>> call, Response<ApiResponse<Level>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Level> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        currentLevel = apiResponse.getData();
                        updateUI();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Level>> call, Throwable t) {
                ToastUtil.show(LevelDetailActivity.this, R.string.msg_network_error);
            }
        });
    }

    private void updateUI() {
        if (currentLevel == null) return;

        tvLevelName.setText(currentLevel.getName());
        tvDescription.setText(getString(R.string.action_description) + ": " + currentLevel.getDescription());
        tvPassScore.setText(currentLevel.getPassScore() + "分");
        tvRewardSticker.setText(currentLevel.getStickerReward());

        // Load pose image if available
        if (currentLevel.getTargetPose() != null && currentLevel.getTargetPose().getImageUrl() != null) {
            Glide.with(this)
                    .load(currentLevel.getTargetPose().getImageUrl())
                    .placeholder(R.drawable.ic_trophy)
                    .into(ivPoseImage);
        }
    }
}