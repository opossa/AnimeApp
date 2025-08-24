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

public class SeriesDaySource implements AnimeSource {

    @Override
    public String getSourceName() {
        return "SeriesDay";
    }

    @Override
    public String getBaseUrl() {
        return "https://www.seriesday-hd.com/ซีรี่ย์จีน/";
    }

    @Override
    public String getSearchUrl(String keyword) {
        return "https://www.seriesday-hd.com/search_movie/?keyword=" + keyword;
    }

    public List<AnimeItem> parseAnimeItems(Document doc) {
        List<AnimeItem> items = new ArrayList<>();
        Elements boxes = doc.select("div.grid-movie > div.box");

        for (Element box : boxes) {
            Element link = box.selectFirst("a");
            if (link != null) {
                String url = link.attr("href");
                
                // ชื่อเรื่อง
                Element p2 = link.selectFirst(".p-box .p2");
                String title = p2 != null ? p2.text() : "";
                
                // รูปภาพ
                Element img = link.selectFirst(".box-img img");
                String imageUrl = img != null ? img.attr("data-lazy-src") : "";
                Element p1 = link.selectFirst(".p-box .p1");
                String info = p1 != null ? p1.text() : "";
                /*
                // ปีและเสียง
                Element p1 = link.selectFirst(".p-box .p1");
                String info = p1 != null ? p1.text() : "";
                
                // เรตติ้ง
                Element rating = box.selectFirst(".info1");
                String ratingText = rating != null ? rating.text().replaceAll("[^0-9.]", "") : "0";
                float ratingValue = Float.parseFloat(ratingText);
                
                // สถานะ (จบแล้ว)
                Element status = box.selectFirst(".EP");
                boolean isCompleted = status != null && status.text().equals("จบแล้ว");
                */
                
                items.add(new AnimeItem(title, url, imageUrl, info));
            }
        }
        return items;
    }


    public List<PageItem> parsePageItems(Document doc) {
        List<PageItem> pages = new ArrayList<>();
        Elements pageLinks = doc.select(".pagination a.page-numbers:not(.next):not(.prev)");
        
        for (Element link : pageLinks) {
            String pageText = link.text();
            if (!pageText.equals("<<") && !pageText.equals(">>") && !pageText.equals("...")) {
                pages.add(new PageItem(pageText, link.attr("href")));
            }
        }
        
        Element prevLink = doc.selectFirst(".pagination a.prev.page-numbers");
        if (prevLink != null) {
            pages.add(0, new PageItem("«", prevLink.attr("href")));
        }
        
        Element nextLink = doc.selectFirst(".pagination a.next.page-numbers");
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
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Referer", "https://www.seriesday-hd.com")
                .build();

            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                String html = response.body().string();

                Document doc = Jsoup.parse(html);

                List<AnimeItem> animeItems = parseAnimeItems(doc);
                List<PageItem> pageItems = parsePageItems(doc);

                callback.onSuccess(animeItems, pageItems);
            } else {
                callback.onFailure("รหัสผิดพลาด: " + response.code());
            }

        } catch (IOException e) {
            callback.onFailure("เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }).start();
}
}