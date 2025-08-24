package com.example.animeapp.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Episode implements Parcelable {
    private String title;
    private String infoUrl;
    private String imageUrl;
    private String videoUrl;
    private String referer;
    private int episodeNumber;
    private String dataId;

    public Episode(String title, String infoUrl, String imageUrl,
                   String videoUrl, String referer, int episodeNumber) {
        this.title = title;
        this.infoUrl = infoUrl;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.referer = referer;
        this.episodeNumber = episodeNumber;
    }

    protected Episode(Parcel in) {
        title = in.readString();
        infoUrl = in.readString();
        imageUrl = in.readString();
        videoUrl = in.readString();
        referer = in.readString();
        episodeNumber = in.readInt();
        dataId = in.readString();
    }

    public static final Creator<Episode> CREATOR = new Creator<Episode>() {
        @Override
        public Episode createFromParcel(Parcel in) {
            return new Episode(in);
        }

        @Override
        public Episode[] newArray(int size) {
            return new Episode[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(infoUrl);
        dest.writeString(imageUrl);
        dest.writeString(videoUrl);
        dest.writeString(referer);
        dest.writeInt(episodeNumber);
        dest.writeString(dataId);
    }

    public String getTitle() { return title; }
    public String getInfoUrl() { return infoUrl; }
    public String getImageUrl() { return imageUrl; }
    public String getVideoUrl() { return videoUrl; }
    public String getReferer() { return referer; }
    public int getEpisodeNumber() { return episodeNumber; }
    public String getDataId() { return dataId; }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
    public void setDataId(String dataId) {
        this.dataId = dataId;
    }
}