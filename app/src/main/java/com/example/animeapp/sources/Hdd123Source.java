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

public class Hdd123Source implements AnimeSource {

    @Override
    public String getSourceName() {
        return "123HDD";
    }

    @Override
    public String getBaseUrl() {
        return "https://www.123hdtv.com/";
    }

    @Override
    public String getSearchUrl(String keyword) {
        return getBaseUrl() + "?s=" + keyword;
    }

    public List<AnimeItem> parseAnimeItems(Document doc) {
        List<AnimeItem> items = new ArrayList<>();
        Elements movieItems = doc.select("article.halim-item, div.halim-item, [class*=halim-item]");
        
        for (Element item : movieItems) {
            Element link = item.selectFirst("a.halim-thumb");
            if (link != null) {
                String url = link.attr("href");
                Element titleElement = item.selectFirst("h2.entry-title");
                String title = titleElement != null ? titleElement.text() : link.attr("title");
                Element img = link.selectFirst("img.lazyload");
                String imageUrl = img != null ? img.attr("data-src") : "";
                
                if (!imageUrl.startsWith("http") && !imageUrl.isEmpty()) {
                    imageUrl = getBaseUrl() + imageUrl;
                }
                
                String soundInfo = item.selectFirst("span.soundsub") != null ? 
                                 item.selectFirst("span.soundsub").text() : "";
                String status = item.selectFirst("span.status") != null ? 
                               item.selectFirst("span.status").text() : "";
                
                items.add(new AnimeItem(title, url, imageUrl, soundInfo + " " + status));
            }
        }
        return items;
    }

    public List<PageItem> parsePageItems(Document doc) {
        List<PageItem> pages = new ArrayList<>();
        Element prevLink = doc.selectFirst("a.prev.page-numbers");
        
        if (prevLink != null) {
            pages.add(new PageItem("«", prevLink.attr("href")));
        }
        
        Elements pageLinks = doc.select("ul.page-numbers a.page-numbers:not(.prev):not(.next)");
        for (Element link : pageLinks) {
            String pageText = link.text();
            if (!pageText.equals("...")) {
                pages.add(new PageItem(pageText, link.attr("href")));
            }
        }
        
        Element currentPage = doc.selectFirst("span.page-numbers.current");
        if (currentPage != null) {
            pages.add(new PageItem(currentPage.text(), ""));
        }
        
        Element nextLink = doc.selectFirst("a.next.page-numbers");
        if (nextLink != null) {
            pages.add(new PageItem("»", nextLink.attr("href")));
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
                        callback.onFailure("ได้รับข้อมูล HTML น้อยเกินไป");
                        return;
                    }
                    
                    Document doc = Jsoup.parse(html);
                    List<AnimeItem> animeItems = parseAnimeItems(doc);
                    List<PageItem> pageItems = parsePageItems(doc);
                    
                    callback.onSuccess(animeItems, pageItems);
                }
            } catch (Exception e) {
                callback.onFailure("เกิดข้อผิดพลาด: " + e.getMessage());
            }
        }).start();
    }
}