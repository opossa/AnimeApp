package com.example.animeapp.models;

public class PageItem {
    private String pageNumber;
    private String url;

    public PageItem(String pageNumber, String url) {
        this.pageNumber = pageNumber;
        this.url = url;
    }

    public String getPageNumber() {
        return pageNumber;
    }

    public String getUrl() {
        return url;
    }
}