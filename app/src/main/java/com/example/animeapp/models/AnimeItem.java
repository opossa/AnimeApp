package com.example.animeapp.models;

public class AnimeItem {
    private String title;
    private String url;
    private String imageUrl;
    private String dubText;

    public AnimeItem(String title, String url, String imageUrl, String dubText) {
        this.title = title;
        this.url = url;
        this.imageUrl = imageUrl;
        this.dubText = dubText;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getImageUrl() {
        return imageUrl;
    }
   
   public String getDubText() {
    return dubText;
   }
}
