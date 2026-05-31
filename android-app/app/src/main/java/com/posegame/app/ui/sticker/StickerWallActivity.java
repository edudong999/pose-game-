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

        try {
            adapter = new StickerAdapter(stickerList);
            rvStickers.setLayoutManager(new GridLayoutManager(this, 3));
            rvStickers.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void loadStickers() {
        // First load user's stickers to know which are unlocked
        String authHeader = prefsUtil != null ? prefsUtil.getAuthHeader() : null;

        if (authHeader != null) {
            apiService.getMyStickers(authHeader).enqueue(new Callback<ApiResponse<StickerListResponse>>() {
                @Override
                public void onResponse(Call<ApiResponse<StickerListResponse>> call, Response<ApiResponse<StickerListResponse>> response) {
                    List<Integer> unlockedIds = new ArrayList<>();
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        for (Sticker s : response.body().getData().getList()) {
                            unlockedIds.add(s.getId());
                        }
                    }
                    // Then load all stickers with unlocked status
                    loadAllStickersWithUnlockStatus(unlockedIds);
                }

                @Override
                public void onFailure(Call<ApiResponse<StickerListResponse>> call, Throwable t) {
                    loadAllStickersWithUnlockStatus(new ArrayList<>());
                }
            });
        } else {
            loadAllStickersWithUnlockStatus(new ArrayList<>());
        }
    }

    private void loadAllStickersWithUnlockStatus(final List<Integer> unlockedIds) {
        apiService.getAllStickers().enqueue(new Callback<ApiResponse<StickerListResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StickerListResponse>> call, Response<ApiResponse<StickerListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<StickerListResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        stickerList.clear();
                        for (Sticker sticker : apiResponse.getData().getList()) {
                            sticker.setUnlocked(unlockedIds.contains(sticker.getId()));
                            stickerList.add(sticker);
                        }
                        totalCount = stickerList.size();
                        unlockedCount = unlockedIds.size();
                        if (adapter != null) adapter.notifyDataSetChanged();
                        updateProgress();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StickerListResponse>> call, Throwable t) {
                // Silently fail
            }
        });
    }

    private void updateProgress() {
        if (tvProgress != null) {
            tvProgress.setText(unlockedCount + " / " + totalCount);
        }
    }
}