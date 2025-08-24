package com.example.animeapp.sources;


import com.example.animeapp.models.AnimeItem;
import com.example.animeapp.models.PageItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class AnimemojiSource implements AnimeSource {
    @Override
    public String getSourceName() {
        return "Animemoji";
    }

    @Override
    public String getBaseUrl() {
        return "https://animemoji.tv/";
    }

    @Override
    public String getSearchUrl(String keyword) {
        return "https://animemoji.tv/?s=" + keyword;
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
                callback.onFailure("โหลดหน้าไม่สำเร็จ");
            }
        }).start();
    }

    private List<AnimeItem> parseAnimeItems(Document doc) {
    List<AnimeItem> items = new ArrayList<>();
    Elements articles = doc.select("article.ez-postthumb");

    for (Element article : articles) {
        Element link = article.selectFirst("a.ez-pt-link");
        if (link != null) {
            String title = link.attr("title");
            String url = link.attr("href");
            String imageUrl = parseImageUrl(link);

            Elements subtitleSpans = article.select("span.ez-index-tag");
            String subtitleText = "";
            for (Element span : subtitleSpans) {
                Element icon = span.selectFirst("i.fa.fa-bolt");
                if (icon != null) {
                    subtitleText = span.ownText().trim();
                    break;
                }
            }

            items.add(new AnimeItem(title, url, imageUrl, subtitleText));
        }
    }
    return items;
}
    
    

    private List<PageItem> parsePageItems(Document doc) {
        List<PageItem> pages = new ArrayList<>();
        Elements pageLinks = doc.select(".wp-pagenavi a.page");
        
        for (Element link : pageLinks) {
            String pageNumber = link.text();
            String pageHref = link.attr("href");
            pages.add(new PageItem(pageNumber, pageHref));
        }
        return pages;
    }

    private String parseImageUrl(Element link) {
        Element img = link.selectFirst("img");
        if (img != null) {
            if (img.hasAttr("data-src")) {
                return img.attr("data-src");
            } else if (img.hasAttr("src") && !img.attr("src").startsWith("data:image")) {
                return img.attr("src");
            }
        }
        return "";
    }
}