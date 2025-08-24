package com.example.animeapp.activities;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import androidx.appcompat.widget.Toolbar;

import com.example.animeapp.handler.NavigationHandler;
import com.example.animeapp.sources.Hd24Source;
import com.example.animeapp.sources.Hdd1112Source;
import java.util.concurrent.TimeUnit;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animeapp.R;
import com.example.animeapp.adapters.AnimeGridAdapter;
import com.example.animeapp.adapters.PageAdapter;
import com.example.animeapp.models.AnimeItem;
import com.example.animeapp.models.PageItem;
import com.example.animeapp.network.AppUpdateChecker;
import com.example.animeapp.network.AppUpdater;
import com.example.animeapp.sources.AnimemojiSource;
import com.example.animeapp.sources.AnimeSource;
import com.example.animeapp.sources.FixmonoSource;
import com.example.animeapp.sources.GoSeriesSource;
import com.example.animeapp.sources.Hdd123Source;
import com.example.animeapp.sources.SeriesDaySource;
import com.example.animeapp.sources.WowDramaSource;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import android.os.Build;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements AppUpdateChecker.UpdateListener, NavigationHandler.NavigationCallback {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private RecyclerView animeRecyclerView;
    private RecyclerView pageRecyclerView;
    
    private AnimeGridAdapter animeAdapter;
    private PageAdapter pageAdapter;
    
    private List<AnimeItem> animeItems = new ArrayList<>();
    private final Map<String, AnimeSource> animeSources = new HashMap<>();
    private AnimeSource currentSource;
    
    private AppUpdater appUpdater;
    private ProgressDialog progressDialog;
    private SharedPreferences prefs;
    private NavigationHandler navigationHandler;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        navigationHandler = new NavigationHandler(this, this);
        
        if (appUpdater == null) {
            appUpdater = new AppUpdater(getApplicationContext());
        }
        
        if (prefs == null) {
            prefs = getSharedPreferences("AppUpdatePrefs", MODE_PRIVATE);
        }
        
        initializeAnimeSources();
        setupToolbar();
        setupAnimeRecyclerView();
        setupPageRecyclerView();
        setupNavigationDrawer();
        loadInitialData();
        checkForUpdate();
    }

    private void setupNavigationMenu(NavigationView navView) {
        navView.setNavigationItemSelectedListener(menuItem -> {
            drawerLayout.closeDrawers();
            navigationHandler.handleNavigationItemSelection(menuItem.getItemId());
            return true;
        });
    }

    @Override
    public void switchSource(String sourceName, String url, String title) {
        currentSource = animeSources.get(sourceName);
        toolbar.setTitle(title);
        loadPage(url);
    }
    
    private void checkForUpdate() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("กำลังตรวจสอบอัพเดท...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String updateCheckUrl = "https://pastebin.com/raw/zUBjTEJi";
        AppUpdateChecker updateChecker = new AppUpdateChecker(getApplicationContext(), updateCheckUrl, this);
        updateChecker.checkForUpdate();
    }

    @Override
    public void onUpdateAvailable(String version, String apkUrl, String releaseNotes) {
        dismissProgressDialog();
        
        if (prefs == null) {
            prefs = getSharedPreferences("AppUpdatePrefs", MODE_PRIVATE);
        }

        long lastPromptTime = prefs.getLong("last_update_prompt", 0);
        if (System.currentTimeMillis() - lastPromptTime > TimeUnit.DAYS.toMillis(1)) {
            new AlertDialog.Builder(this)
                .setTitle("มีเวอร์ชันใหม่ " + version)
                .setMessage(releaseNotes)
                .setPositiveButton("อัพเดท", (dialog, which) -> {
                    if (appUpdater != null) {
                        appUpdater.downloadAndInstallUpdate(apkUrl);
                    }
                })
                .setNegativeButton("ภายหลัง", (dialog, which) -> {
                    if (prefs != null) {
                        prefs.edit().putLong("last_update_prompt", System.currentTimeMillis()).apply();
                    }
                })
                .setCancelable(false)
                .show();
        }
    }

    @Override
    public void onUpToDate() {
        dismissProgressDialog();
    }

    @Override
    public void onError(String message) {
        dismissProgressDialog();
        Toast.makeText(this, "ตรวจสอบอัพเดทไม่สำเร็จ: " + message, Toast.LENGTH_SHORT).show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                Log.e("MainActivity", "Error dismissing progress dialog", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appUpdater != null) {
            appUpdater.cleanup();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgressDialog();
        
        if (appUpdater != null) {
            appUpdater.cleanup();
        }
    }


    private void initializeAnimeSources() {
        animeSources.put("Animemoji", new AnimemojiSource());
        animeSources.put("Fixmono", new FixmonoSource());
        animeSources.put("GoSeries4K", new GoSeriesSource());
        animeSources.put("WowDrama", new WowDramaSource());
        animeSources.put("SeriesDay", new SeriesDaySource());
        animeSources.put("123HDD", new Hdd123Source()); 
        animeSources.put("24HD", new Hd24Source()); 
        animeSources.put("1112HDD", new Hdd1112Source()); 
        currentSource = animeSources.get("Fixmono");
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
    }

    private void setupAnimeRecyclerView() {
        animeRecyclerView = findViewById(R.id.anime_recycler_view);
        int itemWidth = (int) (180 * getResources().getDisplayMetrics().density);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int spanCount = Math.max(1, screenWidth / itemWidth);
        
        animeRecyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        animeAdapter = new AnimeGridAdapter(animeItems, this::onAnimeClick);
        animeRecyclerView.setAdapter(animeAdapter);
        setupRecyclerViewFocus();
    }

    private void setupPageRecyclerView() {
        pageRecyclerView = findViewById(R.id.page_recycler_view);
        pageRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        pageAdapter = new PageAdapter(new ArrayList<>(), this::loadPage);
        pageRecyclerView.setAdapter(pageAdapter);
    }

    private void setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        
        setupDrawerToggle();
        setupVersionInfo(navView);
        setupNavigationMenu(navView);
    }

    private void setupDrawerToggle() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupVersionInfo(NavigationView navView) {
        View headerView = navView.getHeaderView(0);
        TextView versionText = headerView.findViewById(R.id.vers);

        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            versionText.setText("เวอร์ชั่น: " + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            versionText.setText("เวอร์ชั่นไม่ทราบ");
        }
    }

    private void loadInitialData() {
        if (currentSource != null) {
            loadPage(currentSource.getBaseUrl());
        }
    }

    private void loadPage(String pageUrl) {
        currentSource.fetchAnimeList(pageUrl, new AnimeSource.AnimeLoadCallback() {
            @Override
            public void onSuccess(List<AnimeItem> animeItems, List<PageItem> pageItems) {
                runOnUiThread(() -> {
                    updateAnimeList(animeItems);
                    updatePageList(pageItems);
                    animeRecyclerView.smoothScrollToPosition(0);
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> 
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void updateAnimeList(List<AnimeItem> newItems) {
        animeItems.clear();
        animeItems.addAll(newItems);
        animeAdapter.notifyDataSetChanged();
    }

    private void updatePageList(List<PageItem> newPages) {
        pageAdapter.setPages(newPages);
    }

    private void setupRecyclerViewFocus() {
        animeRecyclerView.post(() -> animeRecyclerView.requestFocus());
        animeRecyclerView.setFocusable(true);
        animeRecyclerView.setFocusableInTouchMode(true);
        animeRecyclerView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setQueryHint("ค้นหาอนิเมะ...");

        EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchEditText.setTextColor(Color.WHITE); 
        searchEditText.setHintTextColor(Color.LTGRAY); 

        ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
        searchIcon.setColorFilter(Color.WHITE);

        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        performSearch(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        return false;
                    }
                });

        return true;
    }

    private void performSearch(String keyword) {
        String searchUrl = currentSource.getSearchUrl(keyword);
        loadPage(searchUrl);
    }

    

    private void onAnimeClick(AnimeItem animeItem) {
        AnimeDetailActivity.start(this, animeItem.getUrl(), animeItem.getTitle());
    }
}