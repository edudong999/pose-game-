package com.posegame.app.ui.result;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.posegame.app.PoseApp;
import com.posegame.app.R;
import com.posegame.app.data.api.ApiService;
import com.posegame.app.data.api.RetrofitClient;
import com.posegame.app.data.model.ApiResponse;
import com.posegame.app.data.model.GameRecordSubmitResponse;
import com.posegame.app.ui.camera.CameraActivity;
import com.posegame.app.ui.level.LevelDetailActivity;
import com.posegame.app.util.SharedPreferencesUtil;
import com.posegame.app.util.ToastUtil;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Result Activity - Shows challenge result
 */
public class ResultActivity extends AppCompatActivity {

    private TextView tvResultTitle;
    private TextView tvScore;
    private TextView tvSubtitle;
    private ImageView ivCrown;
    private View headerSuccess;
    private ProgressBar progressHead;
    private ProgressBar progressShoulders;
    private ProgressBar progressArms;
    private ProgressBar progressBody;
    private TextView tvHeadScore;
    private TextView tvShouldersScore;
    private TextView tvArmsScore;
    private TextView tvBodyScore;
    private View cardStickerReward;
    private ImageView ivSticker;
    private TextView tvStickerName;
    private Button btnRetry;
    private Button btnBack;

    private ApiService apiService;
    private SharedPreferencesUtil prefsUtil;

    private int levelId;
    private String levelName;
    private int score;
    private boolean isPass;
    private String stickerReward;
    private int headScore;
    private int shouldersScore;
    private int armsScore;
    private int bodyScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        apiService = RetrofitClient.getInstance().getApiService();
        prefsUtil = PoseApp.getInstance().getPrefsUtil();

        levelId = getIntent().getIntExtra("level_id", 1);
        levelName = getIntent().getStringExtra("level_name");
        score = getIntent().getIntExtra("score", 0);
        isPass = getIntent().getBooleanExtra("is_pass", false);
        stickerReward = getIntent().getStringExtra("sticker_reward");
        headScore = getIntent().getIntExtra("match_details_head", 0);
        shouldersScore = getIntent().getIntExtra("match_details_shoulders", 0);
        armsScore = getIntent().getIntExtra("match_details_arms", 0);
        bodyScore = getIntent().getIntExtra("match_details_body", 0);

        initViews();
        setupUI();
        setupListeners();

        submitRecord();
    }

    private void initViews() {
        headerSuccess = findViewById(R.id.headerSuccess);
        ivCrown = findViewById(R.id.ivCrown);
        tvResultTitle = findViewById(R.id.tvResultTitle);
        tvScore = findViewById(R.id.tvScore);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        progressHead = findViewById(R.id.progressHead);
        progressShoulders = findViewById(R.id.progressShoulders);
        progressArms = findViewById(R.id.progressArms);
        progressBody = findViewById(R.id.progressBody);
        tvHeadScore = findViewById(R.id.tvHeadScore);
        tvShouldersScore = findViewById(R.id.tvShouldersScore);
        tvArmsScore = findViewById(R.id.tvArmsScore);
        tvBodyScore = findViewById(R.id.tvBodyScore);
        cardStickerReward = findViewById(R.id.cardStickerReward);
        ivSticker = findViewById(R.id.ivSticker);
        tvStickerName = findViewById(R.id.tvStickerName);
        btnRetry = findViewById(R.id.btnRetry);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupUI() {
        // Result title and colors
        if (isPass) {
            tvResultTitle.setText(R.string.challenge_success);
            tvResultTitle.setTextColor(getColor(R.color.success));
            tvScore.setTextColor(getColor(R.color.success));
            tvSubtitle.setText("太棒了！动作很标准！");
            headerSuccess.setBackgroundResource(R.drawable.bg_card_green);
        } else {
            tvResultTitle.setText(R.string.challenge_failed);
            tvResultTitle.setTextColor(getColor(R.color.error));
            tvScore.setTextColor(getColor(R.color.error));
            tvSubtitle.setText("继续加油，保持练习！");
            ivCrown.setVisibility(View.GONE);
        }

        // Score
        tvScore.setText(score + "分");

        // Progress bars
        progressHead.setProgress(headScore);
        progressShoulders.setProgress(shouldersScore);
        progressArms.setProgress(armsScore);
        progressBody.setProgress(bodyScore);

        tvHeadScore.setText(headScore + "");
        tvShouldersScore.setText(shouldersScore + "");
        tvArmsScore.setText(armsScore + "");
        tvBodyScore.setText(bodyScore + "");

        // Sticker reward
        if (isPass && stickerReward != null) {
            cardStickerReward.setVisibility(View.VISIBLE);
            tvStickerName.setText(stickerReward);
        }
    }

    private void setupListeners() {
        btnRetry.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra("level_id", levelId);
            intent.putExtra("level_name", levelName);
            startActivity(intent);
            finish();
        });

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, LevelDetailActivity.class);
            intent.putExtra("level_id", levelId);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void submitRecord() {
        String authHeader = prefsUtil.getAuthHeader();
        if (authHeader == null) return;

        apiService.submitRecord(authHeader, levelId, score, isPass).enqueue(new Callback<ApiResponse<GameRecordSubmitResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<GameRecordSubmitResponse>> call, Response<ApiResponse<GameRecordSubmitResponse>> response) {
                // Silently handle - already showing result to user
            }

            @Override
            public void onFailure(Call<ApiResponse<GameRecordSubmitResponse>> call, Throwable t) {
                // Silently fail - user already sees the result
            }
        });
    }
}