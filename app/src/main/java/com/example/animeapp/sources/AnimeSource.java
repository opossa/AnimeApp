package com.example.animeapp.sources;

import com.example.animeapp.models.AnimeItem;
import com.example.animeapp.models.PageItem;
import java.util.List;

public interface AnimeSource {
    String getSourceName();
    String getBaseUrl();
    String getSearchUrl(String keyword);
    
    void fetchAnimeList(String pageUrl, AnimeLoadCallback callback);
    
    interface AnimeLoadCallback {
        void onSuccess(List<AnimeItem> animeItems, List<PageItem> pageItems);
        void onFailure(String errorMessage);
    }
}