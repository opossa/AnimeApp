package com.example.animeapp.network;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppUpdateChecker {
    public interface UpdateListener {
        void onUpdateAvailable(String version, String apkUrl, String releaseNotes);
        void onUpToDate();
        void onError(String message);
    }

    private final Context appContext;
    private final String updateCheckUrl;
    private final UpdateListener listener;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public AppUpdateChecker(Context context, String updateCheckUrl, UpdateListener listener) {
        this.appContext = context.getApplicationContext();
        this.updateCheckUrl = updateCheckUrl;
        this.listener = listener;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void checkForUpdate() {
        executor.execute(() -> {
            try {
                String jsonResponse = fetchUpdateInfo();
                processResponse(jsonResponse);
            } catch (Exception e) {
                notifyErrorOnMainThread(e.getMessage());
            }
        });
    }

    private String fetchUpdateInfo() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(updateCheckUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (InputStream inputStream = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } finally {
            connection.disconnect();
        }
    }

    private void processResponse(String jsonResponse) throws Exception {
        JSONObject json = new JSONObject(jsonResponse);
        String latestVersion = json.getString("version");
        String apkUrl = json.getString("apk_url");
        String releaseNotes = json.optString("release_notes", "");

        PackageInfo pInfo = appContext.getPackageManager().getPackageInfo(
            appContext.getPackageName(), 0
        );
        String currentVersion = pInfo.versionName;

        if (isNewVersionAvailable(currentVersion, latestVersion)) {
            notifyUpdateAvailableOnMainThread(latestVersion, apkUrl, releaseNotes);
        } else {
            notifyUpToDateOnMainThread();
        }
    }

    private boolean isNewVersionAvailable(String current, String latest) {
        String[] currentParts = current.split("\\.");
        String[] latestParts = latest.split("\\.");

        for (int i = 0; i < Math.min(currentParts.length, latestParts.length); i++) {
            int currentVer = Integer.parseInt(currentParts[i]);
            int latestVer = Integer.parseInt(latestParts[i]);
            
            if (latestVer > currentVer) return true;
            if (latestVer < currentVer) return false;
        }
        
        return latestParts.length > currentParts.length;
    }

    private void notifyUpdateAvailableOnMainThread(String version, String apkUrl, String releaseNotes) {
        mainHandler.post(() -> listener.onUpdateAvailable(version, apkUrl, releaseNotes));
    }

    private void notifyUpToDateOnMainThread() {
        mainHandler.post(() -> listener.onUpToDate());
    }

    private void notifyErrorOnMainThread(String message) {
        mainHandler.post(() -> listener.onError(message));
    }

    public void shutdown() {
        executor.shutdown();
    }
}