package com.example.animeapp.sources;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.animeapp.models.AnimeItem;
import com.example.animeapp.models.PageItem;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class AnimemojiSource implements AnimeSource {

    private final Context context;

    public AnimemojiSource(Context context) {
        this.context = context;
    }

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
        new Handler(Looper.getMainLooper()).post(() -> {
            final WebView webView = new WebView(context);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    view.evaluateJavascript(
                        "(function() { return document.documentElement.outerHTML; })();",
                        html -> {
                            try {
                                if (html == null || html.equals("null")) {
                                    callback.onFailure("โหลดหน้าไม่สำเร็จ (html null)");
                                } else {
                                    String cleanHtml = html;
                                    cleanHtml = cleanHtml.replaceAll("^\"|\"$", "");
                                    cleanHtml = cleanHtml.replace("\\u003C", "<")
                                                         .replace("\\n", "")
                                                         .replace("\\t", "")
                                                         .replace("\\\"", "\"")
                                                         .replaceAll("\\\\/", "/");

                                    Document doc = Jsoup.parse(cleanHtml);

                                    List<AnimeItem> animeItems = parseAnimeItems(doc);
                                    List<PageItem> pageItems = parsePageItems(doc);

                                    callback.onSuccess(animeItems, pageItems);
                                }
                            } catch (Exception e) {
                                callback.onFailure("โหลดหน้าไม่สำเร็จ: " + e.getMessage());
                            } finally {
                                try {
                                    webView.stopLoading();
                                    webView.loadUrl("about:blank");
                                    webView.removeAllViews();
                                    webView.clearHistory();
                                    webView.destroy();
                                } catch (Exception ignored) {}
                            }
                        }
                    );
                }
            });

            webView.loadUrl(pageUrl);
        });
    }

    private List<AnimeItem> parseAnimeItems(Document doc) {
        List<AnimeItem> items = new ArrayList<>();
        Elements articles = doc.select("article.ez-postthumb");

        for (Element article : articles) {
            Element link = article.selectFirst("a.ez-pt-link");
            if (link != null) {
                String title = link.hasAttr("title") ? link.attr("title") : "";
                String url = link.hasAttr("href") ? link.attr("href") : "";
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
        if (link == null) return "";
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
