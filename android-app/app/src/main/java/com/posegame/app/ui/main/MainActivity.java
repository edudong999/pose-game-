package com.posegame.app.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.posegame.app.PoseApp;
import com.posegame.app.R;
import com.posegame.app.util.SharedPreferencesUtil;

/**
 * Main Activity with bottom navigation
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private View headerView;
    private ImageView ivAvatar;
    private TextView tvNickname;
    private TextView tvLevel;
    private TextView tvCoin;

    private SharedPreferencesUtil prefsUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefsUtil = PoseApp.getInstance().getPrefsUtil();

        initViews();
        setupHeader();
        setupBottomNavigation();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new LevelListFragment());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHeader();
    }

    private void initViews() {
        headerView = findViewById(R.id.header);
        bottomNav = findViewById(R.id.bottomNav);
    }

    private void setupHeader() {
        ivAvatar = findViewById(R.id.ivAvatar);
        tvNickname = findViewById(R.id.tvNickname);
        tvLevel = findViewById(R.id.tvLevel);
        tvCoin = findViewById(R.id.tvCoin);

        updateHeader();

        // Quick access cards
        findViewById(R.id.cardLevels).setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_levels));
        findViewById(R.id.cardRecords).setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_records));
        findViewById(R.id.cardProfile).setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_profile));
    }

    private void updateHeader() {
        tvNickname.setText(prefsUtil.getNickname());
        tvLevel.setText("Lv." + prefsUtil.getLevelUnlock());
        tvCoin.setText(String.valueOf(prefsUtil.getTotalScore()));
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_levels) {
                fragment = new LevelListFragment();
            } else if (itemId == R.id.nav_records) {
                fragment = new RecordFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}