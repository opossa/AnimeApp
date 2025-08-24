package com.example.animeapp.managers;

import android.content.Context;
import android.media.AudioManager;
import android.content.SharedPreferences;
import android.app.Activity;
import android.view.WindowManager;

public class BrightnessVolumeManager {
    private static final int SHOW_MAX_BRIGHTNESS = 100;
    private static final int SHOW_MAX_VOLUME = 10;
    private static final String PREFS_NAME = "VideoPlayerSettings";

    private final Context context;
    private final AudioManager audioManager;
    private final SharedPreferences prefs;
    
    private int brightness;
    private int volume;

    public BrightnessVolumeManager(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadSavedSettings();
    }

    private void loadSavedSettings() {
        brightness = prefs.getInt("brightness", SHOW_MAX_BRIGHTNESS / 2);
        volume = prefs.getInt("volume", SHOW_MAX_VOLUME / 2);
        setScreenBrightness(brightness);
        setVolume(volume);
    }

    public void setScreenBrightness(int brightness) {
        this.brightness = brightness;
        float d = 1.0f / SHOW_MAX_BRIGHTNESS;
        WindowManager.LayoutParams lp = ((Activity) context).getWindow().getAttributes();
        lp.screenBrightness = d * brightness;
        ((Activity) context).getWindow().setAttributes(lp);
        saveBrightnessToPreferences(brightness);
    }

    private void saveBrightnessToPreferences(int brightness) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("brightness", brightness);
        editor.apply();
    }

    public void setVolume(int volume) {
        this.volume = volume;
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float d = (maxVolume * 1.0f) / SHOW_MAX_VOLUME;
        int newVolume = (int) (d * volume);

        if (newVolume > maxVolume) {
            newVolume = maxVolume;
        }
        if (volume == SHOW_MAX_VOLUME && newVolume < maxVolume) {
            newVolume = maxVolume;
        }

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
        saveVolumeToPreferences(volume);
    }

    private void saveVolumeToPreferences(int volume) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("volume", volume);
        editor.apply();
    }

    public int getBrightness() {
        return brightness;
    }

    public int getVolume() {
        return volume;
    }
}
