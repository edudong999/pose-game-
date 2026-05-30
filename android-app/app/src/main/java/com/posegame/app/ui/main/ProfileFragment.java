package com.posegame.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.posegame.app.PoseApp;
import com.posegame.app.R;
import com.posegame.app.data.api.ApiService;
import com.posegame.app.data.api.RetrofitClient;
import com.posegame.app.data.model.ApiResponse;
import com.posegame.app.data.model.Sticker;
import com.posegame.app.data.model.StickerListResponse;
import com.posegame.app.data.model.UserInfo;
import com.posegame.app.ui.leaderboard.LeaderboardActivity;
import com.posegame.app.ui.login.LoginActivity;
import com.posegame.app.ui.sticker.StickerWallActivity;
import com.posegame.app.util.SharedPreferencesUtil;
import com.posegame.app.util.ToastUtil;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Profile Fragment
 */
public class ProfileFragment extends Fragment {

    private TextView tvNickname;
    private TextView tvTotalScore;
    private TextView tvLevelUnlock;
    private TextView tvStickersCount;
    private LinearLayout menuStickerWall;
    private LinearLayout menuLeaderboard;
    private LinearLayout menuLogout;

    private ApiService apiService;
    private SharedPreferencesUtil prefsUtil;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = RetrofitClient.getInstance().getApiService();
        prefsUtil = PoseApp.getInstance().getPrefsUtil();

        initViews(view);
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateProfile();
        loadStickersCount();
    }

    private void initViews(View view) {
        tvNickname = view.findViewById(R.id.tvNickname);
        tvTotalScore = view.findViewById(R.id.tvTotalScore);
        tvLevelUnlock = view.findViewById(R.id.tvLevelUnlock);
        tvStickersCount = view.findViewById(R.id.tvStickersCount);
        menuStickerWall = view.findViewById(R.id.menuStickerWall);
        menuLeaderboard = view.findViewById(R.id.menuLeaderboard);
        menuLogout = view.findViewById(R.id.menuLogout);
    }

    private void setupListeners() {
        menuStickerWall.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), StickerWallActivity.class));
        });

        menuLeaderboard.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), LeaderboardActivity.class));
        });

        menuLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.logout)
                    .setMessage("确定要退出登录吗？")
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        prefsUtil.clearAll();
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private void updateProfile() {
        tvNickname.setText(prefsUtil.getNickname());
        tvTotalScore.setText(String.valueOf(prefsUtil.getTotalScore()));
        tvLevelUnlock.setText(String.valueOf(prefsUtil.getLevelUnlock()));
    }

    private void loadStickersCount() {
        String authHeader = prefsUtil.getAuthHeader();
        if (authHeader == null) return;

        apiService.getMyStickers(authHeader).enqueue(new Callback<ApiResponse<StickerListResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StickerListResponse>> call, Response<ApiResponse<StickerListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<StickerListResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        tvStickersCount.setText(String.valueOf(apiResponse.getData().getTotal()));
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StickerListResponse>> call, Throwable t) {
                // Silently fail
            }
        });
    }
}