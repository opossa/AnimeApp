package com.example.animeapp.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.example.animeapp.R;
import com.example.animeapp.controllers.PlayerGestureController;
import com.example.animeapp.controllers.VideoPlayerController;
import com.example.animeapp.database.DatabaseHelper;
import com.example.animeapp.databinding.ActivityVideoPlayerBinding;

import com.example.animeapp.dialog.VideoInfoDialog;
import com.example.animeapp.listeners.PlayerEventListener;
import com.example.animeapp.managers.BrightnessVolumeManager;
import com.example.animeapp.models.Episode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class VideoPlayerActivity extends AppCompatActivity implements VideoPlayerController.EpisodeChangeListener{
    private ActivityVideoPlayerBinding binding;
    private VideoPlayerController playerController;
    private BrightnessVolumeManager brightnessVolumeManager;
    private PlayerGestureController gestureController;
    private VideoInfoDialog videoInfoDialog;
    private DatabaseHelper dbHelper;
    
    private List<Episode> episodes;
    private int currentEpisodeIndex;
    private boolean isTelevision;
    private String animeTitle;
    private String animeUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWindow();
        initializeBinding();
        checkDeviceType();
        initializeManagers();
        retrieveIntentData();
        
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

    private void initializeBinding() {
        binding = ActivityVideoPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
    
    private void saveWatchHistory() {
        if (episodes == null || currentEpisodeIndex < 0 || currentEpisodeIndex >= episodes.size()) {
            return;
        }

        Episode currentEpisode = episodes.get(currentEpisodeIndex);
            dbHelper.addOrUpdateHistory(
                animeTitle,
                String.valueOf(currentEpisode.getEpisodeNumber()),
                animeUrl,
                currentEpisode.getImageUrl(),
                currentEpisode.getInfoUrl()
            );
    }

    private void checkDeviceType() {
        isTelevision = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    private void retrieveIntentData() {
        this.episodes = getIntent().getParcelableArrayListExtra("EPISODES");
        this.currentEpisodeIndex = getIntent().getIntExtra("CURRENT_INDEX", -1);
        this.animeTitle = getIntent().getStringExtra("ANIME_TITLE");
        this.animeUrl = getIntent().getStringExtra("ANIME_URL");

        Episode episode = getIntent().getParcelableExtra("EPISODE");
        if (episode == null && episodes != null && currentEpisodeIndex >= 0) {
            episode = episodes.get(currentEpisodeIndex);
        }

        if (episode != null) {
            initializePlayer(episode);
            setupUI(episode);
            saveWatchHistory();
        } else {
            Toast.makeText(this, "ไม่มีข้อมูลตอนนี้", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    
    private void initializeManagers() {
        dbHelper = new DatabaseHelper(this);
        brightnessVolumeManager = new BrightnessVolumeManager(this);
        videoInfoDialog = new VideoInfoDialog(this);
        
        LinearLayout brightnessVolumeContainer = findViewById(R.id.brightness_volume_container);
        ImageView brightnessIcon = findViewById(R.id.brightness_icon);
        ImageView volumeIcon = findViewById(R.id.volume_icon);
        TextView brightVolumeTV = findViewById(R.id.brightness_volume_tv);
        
        gestureController = new PlayerGestureController(this, binding.playerView, 
                brightnessVolumeManager, brightnessVolumeContainer, 
                brightnessIcon, volumeIcon, brightVolumeTV);
    }

    private void initializePlayer(Episode episode) {
        playerController = new VideoPlayerController(this, binding.playerView, episodes, currentEpisodeIndex);
        playerController.setEpisodeChangeListener(this);
        playerController.initializePlayer(episode);
        
        loadPlaybackPosition(episodes.get(currentEpisodeIndex).getInfoUrl());
        
        ProgressBar bufferProgressBar = findViewById(R.id.buffer_progressbar);
        playerController.getPlayer().addListener(new PlayerEventListener(playerController, bufferProgressBar));
    }
    
    private void loadPlaybackPosition(String episodeUrl) {
        SharedPreferences prefs = getSharedPreferences("PLAYBACK_POSITIONS", MODE_PRIVATE);
        long position = prefs.getLong(episodeUrl, 0);
        
        if (position > 0 && playerController != null && playerController.getPlayer() != null) {
            playerController.getPlayer().seekTo(position);
        }
    }
    
    private void savePlaybackPosition(String episodeUrl, long position) {
        SharedPreferences prefs = getSharedPreferences("PLAYBACK_POSITIONS", MODE_PRIVATE);
        prefs.edit().putLong(episodeUrl, position).apply();
    }
    
    @Override
    public void onEpisodeChanged(int newEpisodeIndex) {
        saveCurrentPlaybackPosition();
        this.currentEpisodeIndex = newEpisodeIndex;
        saveWatchHistory();
        loadPlaybackPosition(episodes.get(newEpisodeIndex).getInfoUrl());
    }
    
   private void saveCurrentPlaybackPosition() {
        if (playerController != null && playerController.getPlayer() != null) {
            Episode currentEpisode = episodes.get(currentEpisodeIndex);
            long position = playerController.getPlayer().getCurrentPosition();
            savePlaybackPosition(currentEpisode.getInfoUrl(), position);
        }
    }

    private void setupUI(Episode episode) {
        setupVideoTitle(episode);
        setupNavigationButtons();
        setupButtonListeners();
        setupResizeModeControls();
        hideSystemUI();
        
    }

    private void setupVideoTitle(Episode episode) {
        TextView titleView = binding.playerView.findViewById(R.id.videoTitle);
        titleView.setText(episode.getTitle());
    }

    private void setupNavigationButtons() {
        ImageButton prevButton = findViewById(R.id.prev_episode);
        ImageButton nextButton = findViewById(R.id.next_episode);
        TextView titleView = binding.playerView.findViewById(R.id.videoTitle);
        playerController.setupNavigationButtons(prevButton, nextButton, titleView);
    }

    private void setupButtonListeners() {
        setupSkipButtons();
        setupEpisodeNavigationButtons();
        setupUtilityButtons();
        
        if (isTelevision) {
            setupTVControls();
        }
    }

    private void setupSkipButtons() {
        ImageButton backward10 = findViewById(R.id.backward_10);
        ImageButton forward10 = findViewById(R.id.forward_10);
        
        backward10.setOnClickListener(v -> playerController.seekBackward(10000));
        forward10.setOnClickListener(v -> playerController.seekForward(10000));
    }

    private void setupEpisodeNavigationButtons() {
        ImageButton prevButton = findViewById(R.id.prev_episode);
        ImageButton nextButton = findViewById(R.id.next_episode);
        
        prevButton.setOnClickListener(v -> playerController.playPreviousEpisode());
        nextButton.setOnClickListener(v -> playerController.playNextEpisode());
    }

    private void setupUtilityButtons() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnDetails = findViewById(R.id.btnDetails);
        ImageButton btnSpeed = findViewById(R.id.btnSpeed);
        
        btnBack.setOnClickListener(v -> finish());
        btnDetails.setOnClickListener(v -> showVideoDetails());
        btnSpeed.setOnClickListener(v -> showSpeedDialog());
    }
    

    private void setupTVControls() {
        ImageButton backward10 = findViewById(R.id.backward_10);
        ImageButton forward10 = findViewById(R.id.forward_10);
        ImageButton prevButton = findViewById(R.id.prev_episode);
        ImageButton nextButton = findViewById(R.id.next_episode);
        
        backward10.setOnKeyListener(this::handleKeyEvent);
        forward10.setOnKeyListener(this::handleKeyEvent);
        prevButton.setOnKeyListener(this::handleKeyEvent);
        nextButton.setOnKeyListener(this::handleKeyEvent);
    }

    private boolean handleKeyEvent(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (v.getId() == R.id.backward_10) {
                        playerController.seekBackward(10000);
                        return true;
                    } else if (v.getId() == R.id.prev_episode) {
                        playerController.playPreviousEpisode();
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (v.getId() == R.id.forward_10) {
                        playerController.seekForward(10000);
                        return true;
                    } else if (v.getId() == R.id.next_episode) {
                        playerController.playNextEpisode();
                        return true;
                    }
                    break;
            }
        }
        return false;
    }

    private void setupResizeModeControls() {
        final int[] resizeModes = {
                AspectRatioFrameLayout.RESIZE_MODE_FIT,
                AspectRatioFrameLayout.RESIZE_MODE_FILL,
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        };
        final String[] resizeLabels = {"พอดี", "เต็ม", "ซูม"};
        final int[] currentModeIndex = {0};

        binding.playerView.findViewById(R.id.btnRotate).setOnClickListener(v -> {
            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });

        binding.playerView.findViewById(R.id.btnResize).setOnClickListener(v -> {
            currentModeIndex[0] = (currentModeIndex[0] + 1) % resizeModes.length;
            binding.playerView.setResizeMode(resizeModes[currentModeIndex[0]]);
            Toast.makeText(this, "โหมด: " + resizeLabels[currentModeIndex[0]], Toast.LENGTH_SHORT).show();
        });
    }

    private void showVideoDetails() {
        videoInfoDialog.showVideoDetails(playerController.getPlayer(), episodes, currentEpisodeIndex);
    }
    
    private void showSpeedDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MaterialAlertDialog);

    LayoutInflater inflater = getLayoutInflater();
    View dialogView = inflater.inflate(R.layout.speed_dialog, null);
    builder.setView(dialogView);

    RadioGroup speedGroup = dialogView.findViewById(R.id.speed_group);

    float currentSpeed = 1.0f;
    if (playerController != null && playerController.getPlayer() != null) {
        currentSpeed = playerController.getPlayer().getPlaybackParameters().speed;
    }

    if (currentSpeed == 0.5f) {
        speedGroup.check(R.id.speed_0_5);
    } else if (currentSpeed == 1.0f) {
        speedGroup.check(R.id.speed_1_0);
    } else if (currentSpeed == 1.25f) {
        speedGroup.check(R.id.speed_1_25);
    } else if (currentSpeed == 1.5f) {
        speedGroup.check(R.id.speed_1_5);
    } else if (currentSpeed == 2.0f) {
        speedGroup.check(R.id.speed_2_0);
    } else if (currentSpeed == 2.5f) {
        speedGroup.check(R.id.speed_2_5);
    } else if (currentSpeed == 3.0f) {
        speedGroup.check(R.id.speed_3_0);
    }

    builder.setPositiveButton("ตกลง", (dialog, which) -> {
        int checkedId = speedGroup.getCheckedRadioButtonId();
        onSpeedSelected(checkedId);
    });

    builder.setNegativeButton("ยกเลิก", null);
    builder.show();
}
    
    public void onSpeedSelected(int id) {
    float speed = 1.0f;

    if (id == R.id.speed_0_5) {
        speed = 0.5f;
    } else if (id == R.id.speed_1_0) {
        speed = 1.0f;
    } else if (id == R.id.speed_1_25) {
        speed = 1.25f;
    } else if (id == R.id.speed_1_5) {
        speed = 1.5f;
    } else if (id == R.id.speed_2_0) {
        speed = 2.0f;
    } else if (id == R.id.speed_2_5) {
        speed = 2.5f;
    } else if (id == R.id.speed_3_0) {
        speed = 3.0f;
    }

    if (playerController != null && playerController.getPlayer() != null) {
        playerController.getPlayer().setPlaybackSpeed(speed);
        Toast.makeText(this, "ความเร็ว: " + speed + "x", Toast.LENGTH_SHORT).show();
    }
}

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playerController != null) {
            playerController.releasePlayer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentPlaybackPosition();
        if (playerController != null) {
            playerController.pausePlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (playerController != null) {
            playerController.pausePlayer();
        }
    }

    public static void start(AppCompatActivity activity, List<Episode> episodes, int currentIndex, String animeTitle, String animeUrl) {
        Intent intent = new Intent(activity, VideoPlayerActivity.class);
        intent.putParcelableArrayListExtra("EPISODES", new ArrayList<>(episodes));
        intent.putExtra("CURRENT_INDEX", currentIndex);
        intent.putExtra("ANIME_TITLE", animeTitle);
        intent.putExtra("ANIME_URL", animeUrl);
        activity.startActivity(intent);
    }

    public static void start(AppCompatActivity activity, Episode episode, String animeTitle, String animeUrl) {
        Intent intent = new Intent(activity, VideoPlayerActivity.class);
        intent.putExtra("EPISODE", episode);
        intent.putExtra("ANIME_TITLE", animeTitle);
        intent.putExtra("ANIME_URL", animeUrl);
        activity.startActivity(intent);
    }
}