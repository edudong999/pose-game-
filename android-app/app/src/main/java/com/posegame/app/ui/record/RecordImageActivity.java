package com.posegame.app.ui.record;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.posegame.app.R;

/**
 * 全屏查看闯关记录截图
 * - Intent extras: media_url, level_name
 * - 点击图片切换顶部 toolbar 显示/隐藏
 * - 关闭按钮 / 系统返回键 → finish
 */
public class RecordImageActivity extends AppCompatActivity {

    private ImageView ivFullImage;
    private View topBar;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_image);

        // 全屏沉浸
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ivFullImage = findViewById(R.id.ivFullImage);
        topBar = findViewById(R.id.topBar);
        ImageButton btnClose = findViewById(R.id.btnClose);
        TextView tvLevelName = findViewById(R.id.tvLevelName);
        progressBar = findViewById(R.id.progressBar);

        String mediaUrl = getIntent().getStringExtra("media_url");
        String levelName = getIntent().getStringExtra("level_name");
        tvLevelName.setText(levelName != null ? levelName : "");

        if (mediaUrl == null || mediaUrl.isEmpty()) {
            finish();
            return;
        }

        String fullUrl = mediaUrl.startsWith("http")
            ? mediaUrl
            : "http://10.78.207.58:5000" + mediaUrl;

        progressBar.setVisibility(View.VISIBLE);
        Glide.with(this)
            .load(fullUrl)
            .placeholder(android.R.color.black)
            .into(ivFullImage);
        // 加载完成后通过监听器隐藏 progressBar（Glide 简单版靠 listener）
        Glide.with(this).load(fullUrl).listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
            @Override
            public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                progressBar.setVisibility(View.GONE);
                return false;
            }
            @Override
            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                progressBar.setVisibility(View.GONE);
                return false;
            }
        }).into(ivFullImage);

        btnClose.setOnClickListener(v -> finish());

        // 点击图片切换 toolbar
        ivFullImage.setOnClickListener(v -> toggleTopBar());
    }

    private void toggleTopBar() {
        topBar.setVisibility(topBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }
}
