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
    private View headerView; // kept for potential future use
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
        bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;

        ivAvatar = findViewById(R.id.ivAvatar);
        tvNickname = findViewById(R.id.tvNickname);
        tvLevel = findViewById(R.id.tvLevel);
        tvCoin = findViewById(R.id.tvCoin);
    }

    private void setupHeader() {
        if (ivAvatar == null || tvNickname == null || tvLevel == null || tvCoin == null) return;

        updateHeader();

        // Quick access cards
        View cardLevels = findViewById(R.id.cardLevels);
        View cardRecords = findViewById(R.id.cardRecords);
        View cardProfile = findViewById(R.id.cardProfile);

        if (cardLevels != null) cardLevels.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_levels));
        if (cardRecords != null) cardRecords.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_records));
        if (cardProfile != null) cardProfile.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_profile));
    }

    private void updateHeader() {
        if (prefsUtil == null) return;
        if (tvNickname != null) tvNickname.setText(prefsUtil.getNickname());
        if (tvLevel != null) tvLevel.setText("Lv." + prefsUtil.getLevelUnlock());
        if (tvCoin != null) tvCoin.setText(String.valueOf(prefsUtil.getTotalScore()));
    }

    private void setupBottomNavigation() {
        if (bottomNav == null) return;
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
        if (fragment == null) return;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}