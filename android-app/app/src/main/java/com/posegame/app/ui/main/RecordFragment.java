package com.posegame.app.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.posegame.app.PoseApp;
import com.posegame.app.R;
import com.posegame.app.data.api.ApiService;
import com.posegame.app.data.api.RetrofitClient;
import com.posegame.app.data.model.ApiResponse;
import com.posegame.app.data.model.GameRecord;
import com.posegame.app.util.SharedPreferencesUtil;
import com.posegame.app.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Record Fragment - Challenge history
 */
public class RecordFragment extends Fragment {

    private RecyclerView rvRecords;
    private ProgressBar progressBar;
    private RecordAdapter adapter;
    private List<GameRecord> records = new ArrayList<>();
    private ApiService apiService;
    private SharedPreferencesUtil prefsUtil;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = RetrofitClient.getInstance().getApiService();
        prefsUtil = PoseApp.getInstance().getPrefsUtil();

        rvRecords = view.findViewById(R.id.rvRecords);
        progressBar = view.findViewById(R.id.progressBar);

        setupRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecords();
    }

    private void setupRecyclerView() {
        adapter = new RecordAdapter(records);
        rvRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecords.setAdapter(adapter);
    }

    private void loadRecords() {
        progressBar.setVisibility(View.VISIBLE);

        String authHeader = prefsUtil.getAuthHeader();
        apiService.getRecords(authHeader, 1, 20).enqueue(new Callback<ApiResponse<RecordListResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<RecordListResponse>> call, Response<ApiResponse<RecordListResponse>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<RecordListResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        records.clear();
                        records.addAll(apiResponse.getData().getList());
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<RecordListResponse>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                ToastUtil.show(getContext(), R.string.msg_network_error);
            }
        });
    }
}

class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {
    private final List<GameRecord> records;

    RecordAdapter(List<GameRecord> records) {
        this.records = records;
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        holder.bind(records.get(position));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvLevelName;
        private final TextView tvScore;
        private final TextView tvDate;
        private final TextView tvStatus;

        RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLevelName = itemView.findViewById(R.id.tvLevelName);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }

        void bind(GameRecord record) {
            tvLevelName.setText(record.getLevelName());
            tvScore.setText(record.getScore() + "分");
            tvDate.setText(record.getCreatedAt());

            if (record.isPass()) {
                tvStatus.setText("通过");
                tvStatus.setTextColor(0xFF4CAF50);
            } else {
                tvStatus.setText("未通过");
                tvStatus.setTextColor(0xFFF44336);
            }
        }
    }
}