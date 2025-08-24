package com.example.animeapp.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HistoryItem {
    private int id;
    private String title;
    private String episode;
    private String timestamp;
    private String animeUrl;
    private String imageUrl;
    private String episodeUrl;

    public String getFormattedTimestamp() {
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            dbFormat.setTimeZone(TimeZone.getTimeZone("Asia/Bangkok"));
            
            Date date = dbFormat.parse(timestamp);
            
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            displayFormat.setTimeZone(TimeZone.getTimeZone("Asia/Bangkok"));
            
            return displayFormat.format(date);
        } catch (ParseException e) {
            return timestamp;
        }
    }

    // Getter และ Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getEpisode() { return episode; }
    public void setEpisode(String episode) { this.episode = episode; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getAnimeUrl() { return animeUrl; }
    public void setAnimeUrl(String animeUrl) { this.animeUrl = animeUrl; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public String getEpisodeUrl() { return episodeUrl; }
    public void setEpisodeUrl(String episodeUrl) { this.episodeUrl = episodeUrl; }
}