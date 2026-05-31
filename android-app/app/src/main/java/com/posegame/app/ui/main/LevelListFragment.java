package com.posegame.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.posegame.app.PoseApp;
import com.posegame.app.R;
import com.posegame.app.data.api.ApiService;
import com.posegame.app.data.api.RetrofitClient;
import com.posegame.app.data.model.ApiResponse;
import com.posegame.app.data.model.Level;
import com.posegame.app.data.model.LevelListResponse;
import com.posegame.app.ui.level.LevelDetailActivity;
import com.posegame.app.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Level List Fragment
 */
public class LevelListFragment extends Fragment {

    private RecyclerView rvLevels;
    private ProgressBar progressBar;
    private LevelAdapter adapter;
    private List<Level> levelList = new ArrayList<>();
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_level_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = RetrofitClient.getInstance().getApiService();

        rvLevels = view.findViewById(R.id.rvLevels);
        progressBar = view.findViewById(R.id.progressBar);

        setupRecyclerView();
        loadLevels();
    }

    private void setupRecyclerView() {
        adapter = new LevelAdapter(levelList, levelId -> {
            Intent intent = new Intent(getActivity(), LevelDetailActivity.class);
            intent.putExtra("level_id", levelId);
            startActivity(intent);
        });

        rvLevels.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvLevels.setAdapter(adapter);
    }

    private void loadLevels() {
        progressBar.setVisibility(View.VISIBLE);

        String authHeader = null;
        try {
            authHeader = PoseApp.getInstance().getPrefsUtil().getAuthHeader();
        } catch (Exception e) {
            // Not logged in
        }

        final String header = authHeader;
        apiService.getLevels(header).enqueue(new Callback<ApiResponse<LevelListResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LevelListResponse>> call, Response<ApiResponse<LevelListResponse>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<LevelListResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        levelList.clear();
                        levelList.addAll(apiResponse.getData().getList());
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LevelListResponse>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                ToastUtil.show(getContext(), R.string.msg_network_error);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLevels();
    }
}

/**
 * Level Adapter for RecyclerView
 */
class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.LevelViewHolder> {

    private final List<Level> levels;
    private final OnLevelClickListener listener;

    interface OnLevelClickListener {
        void onLevelClick(int levelId);
    }

    LevelAdapter(List<Level> levels, OnLevelClickListener listener) {
        this.levels = levels;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LevelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_level_card, parent, false);
        return new LevelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LevelViewHolder holder, int position) {
        Level level = levels.get(position);
        holder.bind(level);
    }

    @Override
    public int getItemCount() {
        return levels.size();
    }

    class LevelViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvStatus;

        LevelViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvLevelName);
            tvStatus = itemView.findViewById(R.id.tvLevelStatus);
        }

        void bind(Level level) {
            tvName.setText(level.getName());

            if (level.isPassed()) {
                tvStatus.setText("已通过");
                tvStatus.setTextColor(0xFF4CAF50);
            } else if (level.isUnlocked()) {
                tvStatus.setText("可挑战");
                tvStatus.setTextColor(0xFF7C4DFF);
            } else {
                tvStatus.setText("未解锁");
                tvStatus.setTextColor(0xFF9E9E9E);
            }

            itemView.setOnClickListener(v -> {
                if (level.isUnlocked()) {
                    listener.onLevelClick(level.getId());
                }
            });
        }
    }
}