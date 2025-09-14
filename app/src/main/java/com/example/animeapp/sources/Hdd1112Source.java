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

public class Hdd1112Source implements AnimeSource {

    @Override
    public String getSourceName() {
        return "1112HDD";
    }

    @Override
    public String getBaseUrl() {
        return "https://1112hd2.com/ซีรี่ย์จีน-พากย์ไทย/";
    }

    @Override
    public String getSearchUrl(String keyword) {
        return "https://1112hd2.com/?s=" + keyword;
    }

    public List<AnimeItem> parseAnimeItems(Document doc) {
        List<AnimeItem> items = new ArrayList<>();
        Elements articles = doc.select("div.post-item");

        for (Element article : articles) {
            Element link = article.selectFirst("a");
            if (link != null) {
                String url = link.attr("href");

                Element titleElement = link.selectFirst("h3.post-grid-title");
                String title = titleElement != null ? titleElement.text() : "";

                Element img = link.selectFirst("img");
                String imageUrl = img != null ? img.attr("src") : "";

                Element viewsElement = link.selectFirst("div.flex span");
                String info = viewsElement != null ? viewsElement.text() : "";

                items.add(new AnimeItem(title, url, imageUrl, info));
            }
        }
        return items;
    }

    public List<PageItem> parsePageItems(Document doc) {
    List<PageItem> pages = new ArrayList<>();

    Element pagination = doc.selectFirst(".cj-pagination");
    if (pagination == null) {
        return pages;
    }

    Element prevLink = pagination.selectFirst("a.prev.page-numbers");
    if (prevLink != null) {
        pages.add(new PageItem("«", prevLink.attr("href")));
    }

    Elements pageItems = pagination.select("ul.page-numbers > li:not(:has(a.prev, a.next))");

    for (Element item : pageItems) {
        Element link = item.selectFirst("a.page-numbers");
        Element currentPage = item.selectFirst("span.page-numbers.current");
        Element dots = item.selectFirst("span.page-numbers.dots"); // จุดสามจุด "..."

        if (link != null && !link.text().trim().isEmpty()) {
            pages.add(new PageItem(link.text().trim(), link.attr("href")));
        }
    }

    Element nextLink = pagination.selectFirst("a.next.page-numbers");
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
                        .header("Referer", "https://1112hd2.com")
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
