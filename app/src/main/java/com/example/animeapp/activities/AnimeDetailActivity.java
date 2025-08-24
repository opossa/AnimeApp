package com.example.animeapp.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.animeapp.databinding.ActivityAnimeDetailBinding;
import com.example.animeapp.R;
import com.example.animeapp.adapters.EpisodeAdapter;
import com.example.animeapp.managers.AnimeDataManager;
import com.example.animeapp.models.AnimeItem;
import com.example.animeapp.models.Episode;
import com.example.animeapp.network.AnimeApiClient;
import java.util.ArrayList;
import java.util.List;

public class AnimeDetailActivity extends AppCompatActivity implements AnimeDataManager.DataLoadListener {

    private ActivityAnimeDetailBinding binding;
    private String animeUrl;
    private List<Episode> episodes = new ArrayList<>();
    private EpisodeAdapter episodeAdapter;
    private int currentEpisodeIndex = -1;
    private boolean isTelevision;
    private AnimeDataManager animeDataManager;
    private AnimeItem currentAnime;

    public static void start(Context context, String url, String title) {
        Intent intent = new Intent(context, AnimeDetailActivity.class);
        intent.putExtra("anime_url", url);
        intent.putExtra("anime_title", title);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWindow();
        setupBinding();
        setupToolbar();
        setupHelpers();
        setupRecyclerView();
        loadAnimeData();
        
        isTelevision = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    @Override
    public void onDataLoadFailed(String errorMessage) {
        runOnUiThread(() -> {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            hideProgress();
        });
    }

    private void setupWindow() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(layoutParams);
        }
    }

    private void setupBinding() {
        binding = ActivityAnimeDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbar;
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        
        String title = getIntent().getStringExtra("anime_title");
        String filteredTitle = filterThaiAndNumbers(title);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(filteredTitle);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupHelpers() {
        animeUrl = getIntent().getStringExtra("anime_url");
        
        animeDataManager = new AnimeDataManager(
            this, 
            binding.progressText, 
            binding.progressBar, 
            binding.progressContainer
        );
    }

    private void setupRecyclerView() {
        episodeAdapter = new EpisodeAdapter(episodes, this::onEpisodeClick);

        int itemWidth = (int) (180 * getResources().getDisplayMetrics().density);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int spanCount = Math.max(1, screenWidth / itemWidth);

        binding.episodesRecyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        binding.episodesRecyclerView.setAdapter(episodeAdapter);
        binding.episodesRecyclerView.post(() -> binding.episodesRecyclerView.requestFocus());
    }

    private void loadAnimeData() {
        animeDataManager.loadAnimeData(animeUrl, this);
    }

    private void onEpisodeClick(Episode episode) {
    currentEpisodeIndex = episodes.indexOf(episode);
    
    ProgressDialog progressDialog = new ProgressDialog(this);
    progressDialog.setMessage("กำลังโหลดลิงก์วิดีโอ...");
    progressDialog.setCancelable(false);
    progressDialog.show();

    new Thread(() -> {
        try {
            Log.d("EpisodeClick", "เริ่มดึงลิงก์วิดีโอจาก: " + episode.getInfoUrl());

            String videoUrl = AnimeApiClient.fetchVideoUrl(
                episode.getInfoUrl(),
                episode.getReferer(),
                episode.getEpisodeNumber()
            );

            Log.d("EpisodeClick", "ผลลัพธ์ที่ได้ videoUrl: " + videoUrl);

            runOnUiThread(() -> {
                progressDialog.dismiss();
                if (videoUrl != null && !videoUrl.isEmpty()) {
                    episode.setVideoUrl(videoUrl);
                    Log.d("EpisodeClick", "กำลังเริ่ม VideoPlayerActivity ด้วยลิงก์: " + videoUrl);

                    VideoPlayerActivity.start(this, episodes, currentEpisodeIndex, getTitle().toString(), animeUrl);
                } else {
                    Log.e("EpisodeClick", "ลิงก์วิดีโอเป็น null หรือว่าง");
                    Toast.makeText(this, "ไม่พบลิงก์วิดีโอ", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("EpisodeClick", "เกิดข้อผิดพลาดในการโหลดลิงก์วิดีโอ", e);

            runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(this, "ไม่สามารถโหลดลิงก์วิดีโอ", Toast.LENGTH_SHORT).show();
            });
        }
    }).start();
}

    @Override
    public void onTitleLoaded(String title) {
        setTitle(title);
    }

    @Override
    public void onEpisodesLoaded(List<Episode> episodes) {
        this.episodes = episodes;
        episodeAdapter.updateEpisodes(episodes);
    }

    public static String filterThaiAndNumbers(String input) {
        boolean hasThai = input.matches(".*[ก-๙].*");
        if (hasThai) {
            return input.replaceAll("[^ก-๙0-9\\s]", "").trim();
        } else {
            return input.replaceAll("[^a-zA-Z0-9\\s]", "").trim();
        }
    }

    private void hideProgress() {
        binding.progressContainer.setVisibility(View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.anime_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            animeDataManager.refreshAnimeData(animeUrl, this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}