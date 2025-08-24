package com.example.animeapp.models;

import java.util.List;

public class Anime {
    private String title;
    private String url;
    private String imageUrl;
    private List<Episode> episodes;
    private String source;

    public Anime(String title, String url, String imageUrl, List<Episode> episodes) {
        this.title = title;
        this.url = url;
        this.imageUrl = imageUrl;
        this.episodes = episodes;
    }

    // Getters
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getImageUrl() { return imageUrl; }
    public List<Episode> getEpisodes() { return episodes; }
    public String getSource() { return source; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setUrl(String url) { this.url = url; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setEpisodes(List<Episode> episodes) { this.episodes = episodes; }
    public void setSource(String source) { this.source = source; }
}