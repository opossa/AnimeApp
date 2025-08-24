package com.example.animeapp.listeners;

import android.widget.ProgressBar;
import android.view.View;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.UnstableApi;

import com.example.animeapp.controllers.VideoPlayerController;

@UnstableApi
public class PlayerEventListener implements Player.Listener {
    private final VideoPlayerController controller;
    private final ProgressBar bufferProgressBar;

    public PlayerEventListener(VideoPlayerController controller, ProgressBar bufferProgressBar) {
        this.controller = controller;
        this.bufferProgressBar = bufferProgressBar;
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        switch (playbackState) {
            case Player.STATE_ENDED:
                controller.playNextEpisode();
                break;
            case Player.STATE_BUFFERING:
                bufferProgressBar.setVisibility(View.VISIBLE);
                break;
            case Player.STATE_READY:
                bufferProgressBar.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onPlayerError(PlaybackException error) {
        bufferProgressBar.setVisibility(View.GONE);
    }
}