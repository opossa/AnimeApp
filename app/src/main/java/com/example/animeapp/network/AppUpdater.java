package com.example.animeapp.network;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AppUpdater {
    private static final String TAG = "AppUpdater";
    private final Context context;
    private long downloadId;
    private DownloadManager downloadManager;
    private String downloadedApkName;
    private String apkUrl;

    public AppUpdater(Context context) {
        this.context = context.getApplicationContext();
    }

    public void downloadAndInstallUpdate(String apkUrl) {
        this.apkUrl = apkUrl;

        if (!isNetworkAvailable()) {
            showToast("กรุณาตรวจสอบการเชื่อมต่ออินเทอร์เน็ต");
            return;
        }

        downloadedApkName = getFileNameFromUrl(apkUrl);
        if (downloadedApkName == null || !downloadedApkName.endsWith(".apk")) {
            downloadedApkName = "update_" + System.currentTimeMillis() + ".apk";
        }

        if (isDownloadManagerAvailable()) {
            startDownloadWithDownloadManager();
        } else {
            fallbackToHttpDownload();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private boolean isDownloadManagerAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    private String getFileNameFromUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            return uri.getLastPathSegment();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing URL: " + e.getMessage());
            return null;
        }
    }

    private File getDestinationFile(String fileName) {
        try {
            File downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (downloadDir == null) {
                downloadDir = context.getFilesDir();
            }
            if (!downloadDir.exists() && !downloadDir.mkdirs()) {
                return null;
            }
            return new File(downloadDir, fileName);
        } catch (Exception e) {
            Log.e(TAG, "Error getting destination file: " + e.getMessage());
            return null;
        }
    }

    private void startDownloadWithDownloadManager() {
        File apkFile = getDestinationFile(downloadedApkName);
        if (apkFile == null) {
            showToast("ไม่สามารถเข้าถึงพื้นที่เก็บข้อมูล");
            return;
        }

        if (apkFile.exists()) {
            apkFile.delete();
        }

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl))
                    .setTitle("กำลังอัปเดตแอปพลิเคชัน")
                    .setDescription("กำลังดาวน์โหลดเวอร์ชันใหม่")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, downloadedApkName)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);

            downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                fallbackToHttpDownload();
                return;
            }

            downloadId = downloadManager.enqueue(request);
            context.registerReceiver(downloadCompleteReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            showToast("เริ่มดาวน์โหลด...");
        } catch (Exception e) {
            Log.e(TAG, "Error starting download: " + e.getMessage());
            fallbackToHttpDownload();
        }
    }

    private final BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            long receivedDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId == receivedDownloadId) {
                checkDownloadStatus();
                context.unregisterReceiver(this);
            }
        }
    };

    private void checkDownloadStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);

        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                switch (status) {
                    case DownloadManager.STATUS_SUCCESSFUL:
                        File apkFile = getDestinationFile(downloadedApkName);
                        if (apkFile != null && apkFile.exists()) {
                            installApk(apkFile);
                        } else {
                            showToast("ดาวน์โหลดสำเร็จแต่ไม่พบไฟล์");
                        }
                        break;
                    case DownloadManager.STATUS_FAILED:
                        showToast("ดาวน์โหลดล้มเหลว");
                        break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking download status: " + e.getMessage());
            showToast("เกิดข้อผิดพลาดในการตรวจสอบสถานะ");
        }
    }

    private void fallbackToHttpDownload() {
        showToast("กำลังใช้ระบบดาวน์โหลดสำรอง...");
        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(apkUrl).openConnection();
                connection.connect();

                File apkFile = getDestinationFile(downloadedApkName);
                if (apkFile == null) {
                    showToastOnUiThread("ไม่สามารถสร้างไฟล์ปลายทาง");
                    return;
                }

                try (InputStream input = connection.getInputStream();
                     OutputStream output = new FileOutputStream(apkFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                }

                showToastOnUiThread("ดาวน์โหลดสำเร็จ");
                installApk(apkFile);
            } catch (Exception e) {
                showToastOnUiThread("ดาวน์โหลดล้มเหลว: " + e.getMessage());
                Log.e(TAG, "HTTP download failed: " + e.getMessage());
            }
        }).start();
    }

    private void installApk(File apkFile) {
        if (!apkFile.exists()) {
            showToast("ไม่พบไฟล์สำหรับติดตั้ง");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                showToast("กรุณาอนุญาตการติดตั้งแอปจากแหล่งที่ไม่รู้จัก");
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:" + context.getPackageName()))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return;
            }
        }

        Uri apkUri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".provider", apkFile);

        Intent installIntent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(installIntent);
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private void showToastOnUiThread(String message) {
        new Handler(Looper.getMainLooper()).post(() -> showToast(message));
    }

    public void cleanup() {
        if (downloadedApkName != null) {
            File apkFile = getDestinationFile(downloadedApkName);
            if (apkFile != null && apkFile.exists()) {
                boolean deleted = apkFile.delete();
                Log.d(TAG, "Cleanup " + (deleted ? "successful" : "failed"));
            }
        }
    }
}