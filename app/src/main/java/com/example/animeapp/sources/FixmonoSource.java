package com.example.animeapp.sources;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.example.animeapp.models.AnimeItem;
import com.example.animeapp.models.PageItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FixmonoSource implements AnimeSource {
    
    @Override
    public String getSourceName() {
        return "Fixmono";
    }

    @Override
    public String getBaseUrl() {
        return "https://fixmono.com/serie/";
    }

    @Override
    public String getSearchUrl(String keyword) {
        return "https://fixmono.com/?s=" + keyword;
    }

    public List<AnimeItem> parseAnimeItems(Document doc) {
        List<AnimeItem> items = new ArrayList<>();
        Elements articles = doc.select("article");

        for (Element article : articles) {
            Element link = article.selectFirst("a[rel=bookmark]");
            if (link != null) {
                String title = link.attr("title");
                String url = link.attr("href");
                
                Element img = link.selectFirst("img.wp-post-image");
                String imageUrl = img != null ? img.absUrl("src") : "";
                Element dubSpan = article.selectFirst("span.lang-22-color");
                String dubText = dubSpan != null ? dubSpan.text().trim() : "";
                
                items.add(new AnimeItem(title, url, imageUrl, dubText));
            }
        }
        return items;
    }


    public List<PageItem> parsePageItems(Document doc) {
        List<PageItem> pages = new ArrayList<>();
        Elements pageLinks = doc.select("div.pagination a.page-link:not(.dots)");
        
        for (Element link : pageLinks) {
            if (!link.text().equals("»")) {
                pages.add(new PageItem(link.text(), link.attr("href")));
            }
        }
        return pages;
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
                callback.onFailure("โหลดหน้าไม่สำเร็จ: " + e.getMessage());
            }
        }).start();
    }
}