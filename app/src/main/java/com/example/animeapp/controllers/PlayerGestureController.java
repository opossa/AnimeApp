package com.example.animeapp.controllers;

import android.app.Activity;
import androidx.media3.ui.PlayerView;
import com.example.animeapp.managers.BrightnessVolumeManager;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.os.Handler;
import android.content.res.Resources;
import android.util.DisplayMetrics;

public class PlayerGestureController {
    private final Activity activity;
    private final PlayerView playerView;
    private final BrightnessVolumeManager brightnessVolumeManager;
    private final LinearLayout brightnessVolumeContainer;
    private final ImageView brightnessIcon;
    private final ImageView volumeIcon;
    private final TextView brightVolumeTV;

    private boolean shouldShowController = true;
    private int touchPositionX;
    private static final int SHOW_MAX_BRIGHTNESS = 100;
    private final GestureDetector gestureDetector;

    public PlayerGestureController(Activity activity, PlayerView playerView,
                                   BrightnessVolumeManager brightnessVolumeManager,
                                   LinearLayout brightnessVolumeContainer,
                                   ImageView brightnessIcon, ImageView volumeIcon, TextView brightVolumeTV) {
        this.activity = activity;
        this.playerView = playerView;
        this.brightnessVolumeManager = brightnessVolumeManager;
        this.brightnessVolumeContainer = brightnessVolumeContainer;
        this.brightnessIcon = brightnessIcon;
        this.volumeIcon = volumeIcon;
        this.brightVolumeTV = brightVolumeTV;
        this.gestureDetector = new GestureDetector(activity, gestureListener);
        setupGestureControls();
    }

    private void setupGestureControls() {
        playerView.setOnTouchListener((view, motionEvent) -> {
            if (gestureDetector.onTouchEvent(motionEvent)) {
                return true;
            }

            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                brightnessVolumeContainer.setVisibility(View.GONE);

                if (!shouldShowController) {
                    playerView.setUseController(false);
                    new Handler().postDelayed(() -> {
                        shouldShowController = true;
                        playerView.setUseController(true);
                    }, 500);
                }
            }

            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                touchPositionX = (int) motionEvent.getX();
            }
            return false;
        });
    }

    private final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            handleScrollGesture(distanceX, distanceY);
            return true;
        }
    };

    private void handleScrollGesture(float distanceX, float distanceY) {
        int deviceWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        if (Math.abs(distanceY) > Math.abs(distanceX)) {
            brightnessVolumeContainer.setVisibility(View.VISIBLE);
            shouldShowController = false;

            if (touchPositionX < deviceWidth / 2) {
                adjustBrightness(distanceY);
            } else {
                adjustVolume(distanceY);
            }
        }
    }

    private void adjustBrightness(float distanceY) {
        volumeIcon.setVisibility(View.GONE);
        brightnessIcon.setVisibility(View.VISIBLE);

        boolean increase = distanceY > 0; // แก้ทิศทาง: ลากขึ้น = เพิ่ม
        int currentBrightness = brightnessVolumeManager.getBrightness();
        int newValue = increase ? currentBrightness + 1 : currentBrightness - 1;

        if (newValue >= 0 && newValue <= SHOW_MAX_BRIGHTNESS) {
            brightnessVolumeManager.setScreenBrightness(newValue);
        }

        brightVolumeTV.setText(String.valueOf(brightnessVolumeManager.getBrightness()));
    }

    private void adjustVolume(float distanceY) {
        volumeIcon.setVisibility(View.VISIBLE);
        brightnessIcon.setVisibility(View.GONE);

        boolean increase = distanceY > 0; // แก้ทิศทาง: ลากขึ้น = เพิ่ม
        int currentVolume = brightnessVolumeManager.getVolume();
        int maxVolume = brightnessVolumeManager.getMaxVolume();
        int newValue = increase ? currentVolume + 1 : currentVolume - 1;

        if (newValue >= 0 && newValue <= maxVolume) {
            brightnessVolumeManager.setVolume(newValue);
        }

        brightVolumeTV.setText(String.valueOf(brightnessVolumeManager.getVolume()));
    }
        }
