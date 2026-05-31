package com.posegame.app.ui.leaderboard;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.posegame.app.R;
import com.posegame.app.data.api.ApiService;
import com.posegame.app.data.api.RetrofitClient;
import com.posegame.app.data.model.ApiResponse;
import com.posegame.app.data.model.LeaderboardEntry;
import com.posegame.app.data.model.LeaderboardResponse;
import com.posegame.app.data.model.Level;
import com.posegame.app.data.model.LevelListResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Leaderboard Activity
 */
public class LeaderboardActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvTitle;
    private Spinner spinnerLevel;
    private RecyclerView rvLeaderboard;
    private TextView tvMyRank;
    private View layoutMyRank;

    private ApiService apiService;
    private List<Level> levelList = new ArrayList<>();
    private LeaderboardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_leaderboard);
            apiService = RetrofitClient.getInstance().getApiService();
            initViews();
            setupListeners();
            loadLevels();
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        spinnerLevel = findViewById(R.id.spinnerLevel);
        rvLeaderboard = findViewById(R.id.rvLeaderboard);
        tvMyRank = findViewById(R.id.tvMyRank);
        layoutMyRank = findViewById(R.id.layoutMyRank);

        try {
            adapter = new LeaderboardAdapter(new ArrayList<>());
            rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
            rvLeaderboard.setAdapter(adapter);
            rvLeaderboard.setNestedScrollingEnabled(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (spinnerLevel == null) return;
        spinnerLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (levelList != null && position < levelList.size()) {
                    loadLeaderboard(levelList.get(position).getId());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadLevels() {
        if (apiService == null) return;
        apiService.getLevels(null).enqueue(new Callback<ApiResponse<LevelListResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LevelListResponse>> call, Response<ApiResponse<LevelListResponse>> response) {
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<LevelListResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        levelList.clear();
                        levelList.addAll(apiResponse.getData().getList());
                        updateSpinner();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LevelListResponse>> call, Throwable t) {
                // Silently fail
            }
        });
    }

    private void updateSpinner() {
        if (spinnerLevel == null || levelList == null || levelList.isEmpty()) return;

        List<String> levelNames = new ArrayList<>();
        for (Level level : levelList) {
            levelNames.add(level.getName() + " 排行榜");
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, levelNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLevel.setAdapter(spinnerAdapter);

        if (!levelList.isEmpty()) {
            loadLeaderboard(levelList.get(0).getId());
        }
    }

    private void loadLeaderboard(int levelId) {
        if (apiService == null) return;
        apiService.getLeaderboard(levelId).enqueue(new Callback<ApiResponse<LeaderboardResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LeaderboardResponse>> call, Response<ApiResponse<LeaderboardResponse>> response) {
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<LeaderboardResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        List<LeaderboardEntry> ranking = apiResponse.getData().getRanking();
                        if (ranking != null && adapter != null) {
                            adapter.updateData(ranking);
                        }
                        if (tvTitle != null && apiResponse.getData().getLevelName() != null) {
                            tvTitle.setText(apiResponse.getData().getLevelName() + " 排行榜");
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LeaderboardResponse>> call, Throwable t) {
                // Silently fail - no need to show toast for leaderboard load failure
            }
        });
    }
}