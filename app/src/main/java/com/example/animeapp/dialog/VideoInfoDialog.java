package com.example.animeapp.dialog;

import android.app.Activity;
import android.view.WindowManager;
import androidx.appcompat.app.AlertDialog;
import androidx.media3.exoplayer.ExoPlayer;
import java.util.List;
import com.example.animeapp.models.Episode;
import android.widget.TextView;
import androidx.media3.common.Format;
import android.widget.Toast;
import android.view.Window;
import android.util.DisplayMetrics;
import androidx.media3.common.C;
import java.util.Locale;
import com.example.animeapp.R;
import java.util.concurrent.TimeUnit;

public class VideoInfoDialog {
    private final Activity activity;
    private AlertDialog currentInfoDialog;
    
    public VideoInfoDialog(Activity activity) {
        this.activity = activity;
    }

    public void showVideoDetails(ExoPlayer player, List<Episode> episodes, int currentEpisodeIndex) {
        dismissCurrentDialogIfShowing();

        if (episodes == null || currentEpisodeIndex < 0 || currentEpisodeIndex >= episodes.size()) {
            Toast.makeText(activity, "ไม่พบข้อมูลตอน", Toast.LENGTH_SHORT).show();
            return;
        }

        String videoInfo = getVideoTechDetails(player);
        createAndShowInfoDialog(videoInfo);
    }

    private void dismissCurrentDialogIfShowing() {
        if (currentInfoDialog != null && currentInfoDialog.isShowing()) {
            currentInfoDialog.dismiss();
        }
    }

    private void createAndShowInfoDialog(String content) {
        TextView textView = createDialogTextView(content);
        AlertDialog dialog = buildDialog(textView);
        setupDialogWindow(dialog);
        
        currentInfoDialog = dialog;
        dialog.show();
    }

    private TextView createDialogTextView(String content) {
        TextView textView = new TextView(activity);
        textView.setText(content);
        textView.setTextColor(activity.getResources().getColor(android.R.color.white));
        textView.setPadding(40, 30, 40, 30);
        textView.setTextSize(16);
        return textView;
    }

    private AlertDialog buildDialog(TextView contentView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.CustomAlertDialog);
        builder.setTitle("รายละเอียดวีดีโอ");
        builder.setView(contentView);
        builder.setPositiveButton("ปิด", (dialog, which) -> dialog.dismiss());
        return builder.create();
    }

    private void setupDialogWindow(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            int dialogWidth = (int) (metrics.widthPixels * 0.8);
            window.setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private String getVideoTechDetails(ExoPlayer player) {
        Format videoFormat = player.getVideoFormat();
        Format audioFormat = player.getAudioFormat();
        
        StringBuilder info = new StringBuilder();
        appendDurationInfo(player, info);
        appendVideoFormatInfo(videoFormat, info);
        appendAudioFormatInfo(audioFormat, info);
        
        return info.toString();
    }

    private void appendDurationInfo(ExoPlayer player, StringBuilder info) {
        long durationMs = player.getDuration();
        String duration = durationMs != C.TIME_UNSET ? 
                String.format("%02d:%02d:%02d", 
                    TimeUnit.MILLISECONDS.toHours(durationMs),
                    TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60) : "ไม่ทราบ";
        info.append("ความยาว: ").append(duration).append("\n");
    }

    private void appendVideoFormatInfo(Format videoFormat, StringBuilder info) {
        if (videoFormat != null) {
            info.append("ความละเอียด: ")
                .append(videoFormat.width).append("x").append(videoFormat.height).append("\n")
                .append("เฟรมเรต: ")
                .append(String.format(Locale.getDefault(), "%.2f", videoFormat.frameRate))
                .append(" FPS\n")
                .append("รหัสวิดีโอ: ")
                .append(videoFormat.codecs != null ? videoFormat.codecs : "ไม่ทราบ").append("\n")
                .append("บิตเรต: ")
                .append(videoFormat.bitrate != Format.NO_VALUE ? 
                       String.format("%,d kbps", videoFormat.bitrate / 1000) : "ไม่ทราบ")
                .append("\n");
        } else {
            info.append("ข้อมูลวิดีโอ: กำลังโหลด...\n");
        }
    }

    private void appendAudioFormatInfo(Format audioFormat, StringBuilder info) {
        if (audioFormat != null) {
            info.append("รหัสเสียง: ")
                .append(audioFormat.codecs != null ? audioFormat.codecs : "ไม่ทราบ").append("\n")
                .append("ความถี่เสียง: ")
                .append(audioFormat.sampleRate != Format.NO_VALUE ? 
                       String.format("%,d Hz", audioFormat.sampleRate) : "ไม่ทราบ").append("\n")
                .append("บิตเรตเสียง: ")
                .append(audioFormat.bitrate != Format.NO_VALUE ? 
                       String.format("%,d kbps", audioFormat.bitrate / 1000) : "ไม่ทราบ")
                .append("\n");
        } else {
            info.append("ข้อมูลเสียง: กำลังโหลด...\n");
        }
    }
}