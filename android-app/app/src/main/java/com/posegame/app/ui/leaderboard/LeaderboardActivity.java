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

import com.posegame.app.PoseApp;
import com.posegame.app.R;
import com.posegame.app.data.api.ApiService;
import com.posegame.app.data.api.RetrofitClient;
import com.posegame.app.data.model.ApiResponse;
import com.posegame.app.data.model.LeaderboardEntry;
import com.posegame.app.data.model.LeaderboardResponse;
import com.posegame.app.data.model.Level;
import com.posegame.app.data.model.LevelListResponse;
import com.posegame.app.util.ToastUtil;

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
        setContentView(R.layout.activity_leaderboard);

        apiService = RetrofitClient.getInstance().getApiService();

        initViews();
        setupListeners();
        loadLevels();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        spinnerLevel = findViewById(R.id.spinnerLevel);
        rvLeaderboard = findViewById(R.id.rvLeaderboard);
        tvMyRank = findViewById(R.id.tvMyRank);
        layoutMyRank = findViewById(R.id.layoutMyRank);

        adapter = new LeaderboardAdapter(new ArrayList<>());
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        rvLeaderboard.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        spinnerLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < levelList.size()) {
                    loadLeaderboard(levelList.get(position).getId());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadLevels() {
        apiService.getLevels().enqueue(new Callback<ApiResponse<LevelListResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LevelListResponse>> call, Response<ApiResponse<LevelListResponse>> response) {
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
                ToastUtil.show(LeaderboardActivity.this, R.string.msg_network_error);
            }
        });
    }

    private void updateSpinner() {
        List<String> levelNames = new ArrayList<>();
        for (Level level : levelList) {
            levelNames.add(level.getName() + " 排行榜");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, levelNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLevel.setAdapter(adapter);

        if (!levelList.isEmpty()) {
            loadLeaderboard(levelList.get(0).getId());
        }
    }

    private void loadLeaderboard(int levelId) {
        apiService.getLeaderboard(levelId).enqueue(new Callback<ApiResponse<LeaderboardResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LeaderboardResponse>> call, Response<ApiResponse<LeaderboardResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<LeaderboardResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        adapter.updateData(apiResponse.getData().getRanking());
                        tvTitle.setText(apiResponse.getData().getLevelName() + " 排行榜");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LeaderboardResponse>> call, Throwable t) {
                ToastUtil.show(LeaderboardActivity.this, R.string.msg_network_error);
            }
        });
    }
}