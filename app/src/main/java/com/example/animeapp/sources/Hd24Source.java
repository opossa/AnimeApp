package com.example.animeapp.sources;

import com.example.animeapp.models.AnimeItem;
import com.example.animeapp.models.PageItem;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Hd24Source implements AnimeSource {

    @Override
    public String getSourceName() {
        return "24HD";
    }

    @Override
    public String getBaseUrl() {
        return "https://www.24hd-movie.com/";
    }

    @Override
    public String getSearchUrl(String keyword) {
        return "https://www.24hd-movie.com/search_movie?keyword=" + keyword;
    }

    public List<AnimeItem> parseAnimeItems(Document doc) {
        List<AnimeItem> items = new ArrayList<>();

        Elements gridMovies = doc.select("div.grid-movie");

        Element targetGrid = gridMovies.size() > 1 ? gridMovies.get(1) : gridMovies.first();

        if (targetGrid != null) {
            Elements movieItems = targetGrid.select("div.box");

            for (Element item : movieItems) {
                Element link = item.selectFirst("a");
                if (link != null) {
                    String url = link.attr("href");

                    Element titleElement = item.selectFirst("div.p2");
                    String title = titleElement != null ? titleElement.text() : "";

                    Element img = link.selectFirst("img");
                    String imageUrl = img != null ? img.attr("data-lazy-src") : "";

                    if (imageUrl.isEmpty() && img != null) {
                        imageUrl = img.attr("src");
                    }

                    Element infoElement = item.selectFirst("div.p1");
                    String info = infoElement != null ? infoElement.text() : "";

                    Element ratingElement = item.selectFirst("span.info1");
                    String rating = ratingElement != null ? ratingElement.text() : "";

                    items.add(new AnimeItem(title, url, imageUrl, info + " | " + rating));
                }
            }
        }

        return items;
    }

    public List<PageItem> parsePageItems(Document doc) {
        List<PageItem> pages = new ArrayList<>();
        Element pagination = doc.selectFirst("nav.navigation.pagination");
        
        if (pagination != null) {
            Element prevLink = pagination.selectFirst("a.prev.page-numbers");
            if (prevLink != null) {
                pages.add(new PageItem("❮", prevLink.attr("href")));
            }
            
            Elements pageLinks = pagination.select("a.page-numbers:not(.prev):not(.next)");
            for (Element link : pageLinks) {
                String pageText = link.text();
                if (!pageText.equals("...")) {
                    pages.add(new PageItem(pageText, link.attr("href")));
                }
            }
            
            
            Element nextLink = pagination.selectFirst("a.next.page-numbers");
            if (nextLink != null) {
                pages.add(new PageItem("❯", nextLink.attr("href")));
            }
        }
        
        return pages;
    }

    @Override
    public void fetchAnimeList(String pageUrl, AnimeLoadCallback callback) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                    .url(pageUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.134 Safari/537.36")
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .addHeader("Referer", getBaseUrl())
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    String html = response.body().string();
                    
                    if (html.length() < 1000) {
                        callback.onFailure("Received HTML is too short");
                        return;
                    }
                    
                    Document doc = Jsoup.parse(html);
                    List<AnimeItem> animeItems = parseAnimeItems(doc);
                    List<PageItem> pageItems = parsePageItems(doc);
                    
                    callback.onSuccess(animeItems, pageItems);
                }
            } catch (Exception e) {
                callback.onFailure("Error: " + e.getMessage());
            }
        }).start();
    }
}