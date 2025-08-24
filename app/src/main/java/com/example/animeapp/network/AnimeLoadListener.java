package com.example.animeapp.network;

public interface AnimeLoadListener {
    void onProgress(int currentEpisode, int totalEpisodes);
    void onTotalEpisodes(int totalEpisodes);
}