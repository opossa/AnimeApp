package com.example.animeapp.controllers;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.ui.PlayerView;

import com.example.animeapp.R;
import com.example.animeapp.models.Episode;
import com.example.animeapp.network.AnimeApiClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoPlayerController {
    private ExoPlayer player;
    private PlayerView playerView;
    private Context context;
    private List<Episode> episodes;
    private int currentEpisodeIndex;
    private ImageButton prevEpisodeButton;
    private ImageButton nextEpisodeButton;
    private TextView videoTitleView;
    
    private EpisodeChangeListener episodeChangeListener;

    public interface EpisodeChangeListener {
        void onEpisodeChanged(int newEpisodeIndex);
    }

    public void setEpisodeChangeListener(EpisodeChangeListener listener) {
        this.episodeChangeListener = listener;
    }
    

    public VideoPlayerController(Context context, PlayerView playerView, 
                               List<Episode> episodes, int currentEpisodeIndex) {
        this.context = context;
        this.playerView = playerView;
        this.episodes = episodes;
        this.currentEpisodeIndex = currentEpisodeIndex;
        
    }

    public ExoPlayer getPlayer() {
        return player;
    }

    public void initializePlayer(Episode episode) {
        player = new ExoPlayer.Builder(context).build();
        playerView.setPlayer(player);

        String videoUrl = episode.getVideoUrl();
        String referer = episode.getReferer();

        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", referer);
        headers.put("Accept", "*/*");
        dataSourceFactory.setDefaultRequestProperties(headers);

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(videoUrl)
                .build();

        MediaSource mediaSource;
        if (videoUrl.endsWith(".m3u8") || videoUrl.endsWith(".txt")) {
            mediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
        } else {
            mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
        }

        player.setMediaSource(mediaSource);
        player.prepare();
        player.play();
    }

    public void setupNavigationButtons(ImageButton prevButton, ImageButton nextButton, TextView titleView) {
        this.prevEpisodeButton = prevButton;
        this.nextEpisodeButton = nextButton;
        this.videoTitleView = titleView;
        updateNavigationButtons();
    }

    public void seekBackward(long milliseconds) {
        if (player != null) {
            player.seekTo(player.getCurrentPosition() - milliseconds);
        }
    }

    public void seekForward(long milliseconds) {
        if (player != null) {
            player.seekTo(player.getCurrentPosition() + milliseconds);
        }
    }

    public void playNextEpisode() {
        Log.d("PlayerControl", "Attempting to play next episode");
        if (episodes != null && currentEpisodeIndex < episodes.size() - 1) {
            currentEpisodeIndex++;
            Episode nextEpisode = episodes.get(currentEpisodeIndex);
            Log.d("PlayerControl", "Playing next episode: " + nextEpisode.getTitle());
            playEpisode(currentEpisodeIndex);
        } else {
            Toast.makeText(context, "ไม่มีตอนถัดไป", Toast.LENGTH_SHORT).show();
        }
    }

    public void playPreviousEpisode() {
        Log.d("PlayerControl", "Attempting to play previous episode");
        if (currentEpisodeIndex > 0) {
            currentEpisodeIndex--;
            Episode prevEpisode = episodes.get(currentEpisodeIndex);
            Log.d("PlayerControl", "Playing previous episode: " + prevEpisode.getTitle());
            playEpisode(currentEpisodeIndex);
        } else {
            Toast.makeText(context, "ไม่มีตอนก่อนหน้า", Toast.LENGTH_SHORT).show();
        }
    }

    public void playEpisode(int index) {
    if (index < 0 || index >= episodes.size()) {
        Toast.makeText(context, "ไม่พบตอน", Toast.LENGTH_SHORT).show();
        return;
    }
    if (player != null) {
        player.stop(); 
        player.clearMediaItems(); 
    }

    currentEpisodeIndex = index;
    Episode episode = episodes.get(index);

    String savedUrl = episode.getVideoUrl();
        
        if (episodeChangeListener != null) {
            episodeChangeListener.onEpisodeChanged(currentEpisodeIndex);
        }

    if (savedUrl != null && !savedUrl.isEmpty()) {
        initializePlayer(episode);
        updateNavigationButtons();
        if (videoTitleView != null) {
            videoTitleView.setText(episode.getTitle());
        }
        return;
    }

    Toast.makeText(context, "กำลังโหลดลิงก์วิดีโอ...", Toast.LENGTH_SHORT).show();

    new Thread(() -> {
        try {
            String videoUrl = AnimeApiClient.fetchVideoUrl(
                episode.getInfoUrl(), episode.getReferer(), episode.getEpisodeNumber()
            );

            ((Activity) context).runOnUiThread(() -> {
                if (videoUrl != null && !videoUrl.isEmpty()) {
                    Log.d("PlayerControl", "Video URL obtained: " + videoUrl);
                    episode.setVideoUrl(videoUrl);

                    initializePlayer(episode);
                    updateNavigationButtons();
                    if (videoTitleView != null) {
                        videoTitleView.setText(episode.getTitle());
                    }
                } else {
                    Toast.makeText(context, "ไม่สามารถดึงลิงก์วิดีโอได้", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("PlayerControl", "เกิดข้อผิดพลาด: " + e.getMessage(), e);
            ((Activity) context).runOnUiThread(() ->
                Toast.makeText(context, "เกิดข้อผิดพลาดในการโหลดลิงก์", Toast.LENGTH_SHORT).show()
            );
        }
    }).start();
}

    private void updateNavigationButtons() {
        if (prevEpisodeButton == null || nextEpisodeButton == null) {
            Log.e("PlayerControl", "Navigation buttons are null");
            return;
        }

        boolean hasPrevious = currentEpisodeIndex > 0;
        boolean hasNext = episodes != null && currentEpisodeIndex < episodes.size() - 1;

        Log.d("PlayerControl", "Updating buttons - hasPrevious: " + hasPrevious + ", hasNext: " + hasNext);

        prevEpisodeButton.setImageResource(hasPrevious ? 
            R.drawable.ic_prev_episode_active : R.drawable.ic_prev_episode_inactive);
        prevEpisodeButton.setEnabled(hasPrevious);
        prevEpisodeButton.setAlpha(hasPrevious ? 1.0f : 0.5f);

        nextEpisodeButton.setImageResource(hasNext ? 
            R.drawable.ic_next_episode_active : R.drawable.ic_next_episode_inactive);
        nextEpisodeButton.setEnabled(hasNext);
        nextEpisodeButton.setAlpha(hasNext ? 1.0f : 0.5f);
    }

    public void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    public void pausePlayer() {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }
}