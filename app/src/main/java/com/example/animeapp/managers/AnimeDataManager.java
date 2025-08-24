package com.example.animeapp.managers;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.animeapp.R;
import com.example.animeapp.models.Anime;
import com.example.animeapp.models.Episode;
import com.example.animeapp.network.AnimeApiClient;
import com.example.animeapp.network.AnimeLoadListener;

import java.io.IOException;
import java.util.List;

public class AnimeDataManager {
    private Activity activity;
    private TextView progressTextView;
    private ProgressBar progressBar;
    private View progressContainer;
    private Anime cachedAnime;
    private boolean isRefreshing = false;

    public AnimeDataManager(Activity activity, 
                          TextView progressTextView, 
                          ProgressBar progressBar, 
                          View progressContainer) {
        this.activity = activity;
        this.progressTextView = progressTextView;
        this.progressBar = progressBar;
        this.progressContainer = progressContainer;
    }

    public void loadAnimeData(String animeUrl, DataLoadListener listener) {
        if (!isNetworkAvailable()) {
            showNoInternetToast();
            return;
        }

        isRefreshing = false;
        showProgress("กำลังโหลดข้อมูล...");
        new FetchAnimeDataTask(animeUrl, listener).execute();
    }

    public void refreshAnimeData(String animeUrl, DataLoadListener listener) {
        if (!isNetworkAvailable()) {
            showNoInternetToast();
            return;
        }

        isRefreshing = true;
        showProgress("กำลังอัปเดตข้อมูล...");
        new FetchAnimeDataTask(animeUrl, listener).execute();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void showNoInternetToast() {
        activity.runOnUiThread(() -> 
            Toast.makeText(activity, "ไม่มีอินเทอร์เน็ต", Toast.LENGTH_SHORT).show()
        );
    }

    private void showProgress(String message) {
        activity.runOnUiThread(() -> {
            progressContainer.setVisibility(View.VISIBLE);
            progressTextView.setText(message);
            progressBar.setProgress(0);
            progressBar.setMax(100);
        });
    }

    private void hideProgress() {
        activity.runOnUiThread(() -> progressContainer.setVisibility(View.GONE));
    }
    
    /*

    private void updateProgress(int current, int total) {
        activity.runOnUiThread(() -> {
            int percent = (int) (((float)current / total) * 100);
            progressTextView.setText(
                String.format("กำลังดึงข้อมูล: ตอนที่ %d จาก %d (%d%%)", current, total, percent)
            );
            progressBar.setProgress(percent);
            
            if (current == total) {
                progressTextView.setText("ดึงข้อมูลเสร็จสิ้น");
                new Handler().postDelayed(this::hideProgress, 1500);
            }
        });
    }
    */
    
    private void updateProgress(int current, int total) {
    activity.runOnUiThread(() -> {
        int percent = (int) (((float) current / total) * 100);

        // ใช้ string resource แทนการเขียนตรงๆ
        String progressText = activity.getString(
                R.string.loading_progress,
                String.valueOf(current),
                String.valueOf(total)
        );

        progressTextView.setText(
                String.format("%s (%d%%)", progressText, percent)
        );

        progressBar.setProgress(percent);

        if (current == total) {
            progressTextView.setText("ดึงข้อมูลเสร็จสิ้น");
            new Handler().postDelayed(this::hideProgress, 1500);
        }
    });
}

    public Anime getCachedAnime() {
        return cachedAnime;
    }

    public interface DataLoadListener {
        void onTitleLoaded(String title);
        void onEpisodesLoaded(List<Episode> episodes);
        void onDataLoadFailed(String errorMessage);
    }

    private class FetchAnimeDataTask extends AsyncTask<Void, Integer, Anime> {
        private final String animeUrl;
        private final DataLoadListener listener;
        private String errorMessage = "";

        public FetchAnimeDataTask(String animeUrl, DataLoadListener listener) {
            this.animeUrl = animeUrl;
            this.listener = listener;
        }

        @Override
        protected Anime doInBackground(Void... voids) {
            try {
                return AnimeApiClient.fetchAnimeData(animeUrl, new AnimeLoadListener() {
                    @Override
                    public void onProgress(int current, int total) {
                        publishProgress(current, total);
                    }

                    @Override
                    public void onTotalEpisodes(int total) {
                        activity.runOnUiThread(() -> progressBar.setMax(total));
                    }
                });
            } catch (IOException e) {
                errorMessage = "การเชื่อมต่อล้มเหลว: " + e.getMessage();
                Log.e("AnimeFetch", "IOException", e);
                return null;
            } catch (Exception e) {
                errorMessage = "เกิดข้อผิดพลาดในการโหลดข้อมูล";
                Log.e("AnimeFetch", "Exception", e);
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            updateProgress(values[0], values[1]);
        }

        @Override
        protected void onPostExecute(Anime anime) {
            if (anime != null) {
                cachedAnime = anime;
                listener.onTitleLoaded(anime.getTitle());
                listener.onEpisodesLoaded(anime.getEpisodes());
                
                if (isRefreshing) {
                    showSuccessToast("อัปเดตข้อมูลเรียบร้อย");
                }
            } else {
                listener.onDataLoadFailed(errorMessage.isEmpty() ? "ไม่สามารถดึงข้อมูลได้" : errorMessage);
                showErrorToast(errorMessage);
            }
        }

        private void showSuccessToast(String message) {
            activity.runOnUiThread(() -> 
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            );
        }

        private void showErrorToast(String message) {
            activity.runOnUiThread(() -> {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                hideProgress();
            });
        }
    }
}