package com.posegame.app.ui.sticker;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.posegame.app.PoseApp;
import com.posegame.app.R;
import com.posegame.app.data.api.ApiService;
import com.posegame.app.data.api.RetrofitClient;
import com.posegame.app.data.model.ApiResponse;
import com.posegame.app.data.model.Sticker;
import com.posegame.app.data.model.StickerListResponse;
import com.posegame.app.util.SharedPreferencesUtil;
import com.posegame.app.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Sticker Wall Activity
 */
public class StickerWallActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvProgress;
    private RecyclerView rvStickers;

    private ApiService apiService;
    private SharedPreferencesUtil prefsUtil;
    private StickerAdapter adapter;
    private List<Sticker> stickerList = new ArrayList<>();
    private int unlockedCount = 0;
    private int totalCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_wall);

        apiService = RetrofitClient.getInstance().getApiService();
        prefsUtil = PoseApp.getInstance().getPrefsUtil();

        initViews();
        setupListeners();
        loadStickers();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvProgress = findViewById(R.id.tvProgress);
        rvStickers = findViewById(R.id.rvStickers);

        adapter = new StickerAdapter(stickerList);
        rvStickers.setLayoutManager(new GridLayoutManager(this, 3));
        rvStickers.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadStickers() {
        apiService.getAllStickers().enqueue(new Callback<ApiResponse<StickerListResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StickerListResponse>> call, Response<ApiResponse<StickerListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<StickerListResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        stickerList.clear();
                        stickerList.addAll(apiResponse.getData().getList());
                        totalCount = stickerList.size();

                        // Count unlocked
                        unlockedCount = 0;
                        for (Sticker sticker : stickerList) {
                            if (sticker.isUnlocked()) {
                                unlockedCount++;
                            }
                        }

                        adapter.notifyDataSetChanged();
                        updateProgress();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StickerListResponse>> call, Throwable t) {
                ToastUtil.show(StickerWallActivity.this, R.string.msg_network_error);
            }
        });
    }

    private void updateProgress() {
        tvProgress.setText(String.format(getString(R.string.sticker_count_format), unlockedCount, totalCount));
    }
}