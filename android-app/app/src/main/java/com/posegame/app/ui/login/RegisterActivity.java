package com.posegame.app.ui.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.posegame.app.util.SharedPreferencesUtil;
import com.posegame.app.util.ToastUtil;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Register Activity
 */
public class RegisterActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etNickname;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private CheckBox cbAgreement;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private ApiService apiService;
    private SharedPreferencesUtil prefsUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiService = RetrofitClient.getInstance().getApiService();
        prefsUtil = PoseApp.getInstance().getPrefsUtil();

        initViews();
        setListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etNickname = findViewById(R.id.etNickname);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        cbAgreement = findViewById(R.id.cbAgreement);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> attemptRegister());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String nickname = etNickname.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(nickname)) {
            etNickname.setError(getString(R.string.error_empty_nickname));
            etNickname.requestFocus();
            return;
        }
        if (nickname.length() < 2 || nickname.length() > 20) {
            etNickname.setError(getString(R.string.error_nickname_length));
            etNickname.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.error_empty_password));
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError(getString(R.string.error_password_length));
            etPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError(getString(R.string.error_password_mismatch));
            etConfirmPassword.requestFocus();
            return;
        }
        if (!cbAgreement.isChecked()) {
            ToastUtil.show(this, R.string.error_agreement_required);
            return;
        }

        showLoading(true);

        Map<String, String> params = new HashMap<>();
        params.put("nickname", nickname);
        params.put("password", password);

        apiService.register(params).enqueue(new Callback<ApiResponse<UserInfo>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserInfo>> call, Response<ApiResponse<UserInfo>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<UserInfo> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        UserInfo userInfo = apiResponse.getData();
                        saveUserSession(userInfo);
                        ToastUtil.show(RegisterActivity.this, R.string.msg_register_success);
                        finish();
                    } else {
                        ToastUtil.show(RegisterActivity.this, apiResponse.getMsg());
                    }
                } else {
                    ToastUtil.show(RegisterActivity.this, R.string.msg_register_failed);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserInfo>> call, Throwable t) {
                showLoading(false);
                ToastUtil.show(RegisterActivity.this, R.string.msg_network_error);
            }
        });
    }

    private void saveUserSession(UserInfo userInfo) {
        prefsUtil.saveToken(userInfo.getToken());
        prefsUtil.saveUserId(userInfo.getUserId());
        prefsUtil.saveNickname(userInfo.getNickname());
        prefsUtil.saveTotalScore(0);
        prefsUtil.saveLevelUnlock(1);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
    }
}