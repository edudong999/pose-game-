package com.posegame.app.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.posegame.app.PoseApp;
import com.posegame.app.R;
import com.posegame.app.data.api.ApiService;
import com.posegame.app.data.api.RetrofitClient;
import com.posegame.app.data.model.ApiResponse;
import com.posegame.app.data.model.UserInfo;
import com.posegame.app.ui.main.MainActivity;
import com.posegame.app.util.SharedPreferencesUtil;
import com.posegame.app.util.ToastUtil;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Login Activity
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etNickname;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ProgressBar progressBar;

    private ApiService apiService;
    private SharedPreferencesUtil prefsUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if already logged in
        try {
            PoseApp app = PoseApp.getInstance();
            if (app != null && app.getPrefsUtil() != null && app.getPrefsUtil().isLoggedIn()) {
                goToMain();
                return;
            }
        } catch (Exception e) {
            // Continue to login screen if prefs check fails
        }

        setContentView(R.layout.activity_login);

        try {
            apiService = RetrofitClient.getInstance().getApiService();
            PoseApp app = PoseApp.getInstance();
            if (app != null) {
                prefsUtil = app.getPrefsUtil();
            }
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "Failed to init RetrofitClient", e);
        }

        initViews();
        setListeners();
    }

    private void initViews() {
        etNickname = findViewById(R.id.etNickname);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void attemptLogin() {
        String nickname = etNickname.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(nickname)) {
            etNickname.setError(getString(R.string.error_empty_nickname));
            etNickname.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.error_empty_password));
            etPassword.requestFocus();
            return;
        }

        showLoading(true);

        Map<String, String> params = new HashMap<>();
        params.put("nickname", nickname);
        params.put("password", password);

        apiService.login(params).enqueue(new Callback<ApiResponse<UserInfo>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserInfo>> call, Response<ApiResponse<UserInfo>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<UserInfo> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        UserInfo userInfo = apiResponse.getData();
                        saveUserSession(userInfo);
                        ToastUtil.show(LoginActivity.this, R.string.msg_login_success);
                        goToMain();
                    } else {
                        ToastUtil.show(LoginActivity.this, apiResponse.getMsg());
                    }
                } else {
                    ToastUtil.show(LoginActivity.this, R.string.msg_login_failed);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserInfo>> call, Throwable t) {
                showLoading(false);
                ToastUtil.show(LoginActivity.this, R.string.msg_network_error);
            }
        });
    }

    private void saveUserSession(UserInfo userInfo) {
        if (prefsUtil == null) return;
        prefsUtil.saveToken(userInfo.getToken());
        prefsUtil.saveUserId(userInfo.getUserId());
        prefsUtil.saveNickname(userInfo.getNickname());
        if (userInfo.getAvatarUrl() != null) {
            prefsUtil.saveAvatarUrl(userInfo.getAvatarUrl());
        }
        prefsUtil.saveTotalScore(userInfo.getTotalScore());
        prefsUtil.saveLevelUnlock(userInfo.getLevelUnlock());
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
    }
}