package com.example.animeapp.sources;

import com.example.animeapp.models.AnimeItem;
import com.example.animeapp.models.PageItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WowDramaSource implements AnimeSource {

    @Override
    public String getSourceName() {
        return "WowDrama";
    }

    @Override
    public String getBaseUrl() {
        return "https://wow-drama.com/category/the-series-all/";
    }

    @Override
    public String getSearchUrl(String keyword) {
        return "https://wow-drama.com/?s=" + keyword;
    }

    @Override
    public void fetchAnimeList(String pageUrl, AnimeLoadCallback callback) {
        new Thread(() -> {
            try {
                Document doc = Jsoup.connect(pageUrl)
                        .userAgent("Mozilla/5.0")
                        .get();

                List<AnimeItem> animeItems = parseAnimeItems(doc);
                List<PageItem> pageItems = parsePageItems(doc);

                callback.onSuccess(animeItems, pageItems);
            } catch (IOException e) {
                callback.onFailure("โหลดข้อมูลไม่สำเร็จ: " + e.getMessage());
            }
        }).start();
    }

    private List<AnimeItem> parseAnimeItems(Document doc) {
        List<AnimeItem> items = new ArrayList<>();
        Elements movies = doc.select("div.-movie");

        for (Element movie : movies) {
            Element link = movie.selectFirst("div.pic a");
            Element img = movie.selectFirst("div.pic img");
            Element titleElement = movie.selectFirst("h2.entry-title a");

            if (link != null && img != null && titleElement != null) {
                String url = link.attr("href");
                String title = titleElement.text();
                String imageUrl = img.hasAttr("data-src") ? img.attr("data-src") : img.attr("src");
                Element sub = movie.selectFirst("div.imdb");
                String subtitleText = sub != null ? sub.text().trim() : "";

                items.add(new AnimeItem(title, url, imageUrl, subtitleText));
            }
        }
        return items;
    }

    private List<PageItem> parsePageItems(Document doc) {
    List<PageItem> pages = new ArrayList<>();
    
    Element prevLink = doc.select("div.content-pagination a.prev.page-numbers").first();
    if (prevLink != null) {
        String prevUrl = prevLink.attr("href");
        pages.add(new PageItem("«", prevUrl));
    }
    
    Elements pageLinks = doc.select("div.content-pagination a.page-numbers:not(.prev):not(.next)");
    for (Element link : pageLinks) {
        String pageNumber = link.text();
        String pageUrl = link.attr("href");
        pages.add(new PageItem(pageNumber, pageUrl));
    }
    
    Element nextLink = doc.select("div.content-pagination a.next.page-numbers").first();
    if (nextLink != null) {
        String nextUrl = nextLink.attr("href");
        pages.add(new PageItem("»", nextUrl));
    }
    
    return pages;
}
}