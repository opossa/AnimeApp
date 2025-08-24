package com.example.animeapp.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.example.animeapp.models.Anime;
import com.example.animeapp.models.Episode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Hd24Parser implements AnimeParser {
    private boolean isBlocked(Document doc) {
        if (doc.text().contains("Cloudflare") || 
            doc.text().contains("Access Denied")) {
            return true;
        }
        return false;
    }

    @Override
    public Anime parseAnimeDetail(Document doc) {
        try {
            if (isBlocked(doc)) {
                throw new IOException("เว็บไซต์บล็อกการเข้าถึง");
            }

            Element titleMeta = doc.selectFirst("meta[property=og:title]");
            if (titleMeta == null) {
                throw new IOException("ไม่พบชื่ออนิเมะ");
            }
            String title = titleMeta.attr("content");

            Element imageMeta = doc.selectFirst("meta[property=og:image]");
            if (imageMeta == null) {
                throw new IOException("ไม่พบรูปภาพอนิเมะ");
            }
            String image = imageMeta.attr("content");

            List<Episode> episodes = parseEpisodes(doc, image);
            return new Anime(title, doc.baseUri(), image, episodes);

        } catch (Exception e) {
            throw new RuntimeException("การประมวลผลล้มเหลว", e);
        }
    }

    @Override
    public List<Episode> parseEpisodes(Document doc, String imageUrl) {
        List<Episode> episodes = new ArrayList<>();

        try {
            Elements serverElements = doc.select("li.halim-episode span.halim-btn");

            Element downloadServer = doc.selectFirst("span[data-server='1000']");
            String mainTitle = (downloadServer != null) ? 
                downloadServer.attr("data-title") : "";

            for (Element server : serverElements) {
                try {
                    String serverId = server.attr("data-server");
                    String episodeNum = server.attr("data-episode");
                    String serverName = server.text()
                        .replaceAll("<i.*?>.*?</i>", "")
                        .trim();

                    String episodeTitle = !mainTitle.isEmpty() ? mainTitle :
                        "เซิร์ฟเวอร์ " + serverId + " - " + serverName;

                    episodes.add(new Episode(
                        episodeTitle,
                        doc.baseUri(),
                        imageUrl,
                        "",
                        doc.baseUri(),
                        Integer.parseInt(episodeNum)
                    ));

                } catch (Exception e) {
                    continue;
                }
            }

        } catch (Exception e) {
            return episodes;
        }

        return episodes;
    }

    @Override
    public String parseVideoUrl(Document epDoc, String referer, int episodeNumber) 
        throws IOException {
        
        try {
            if (isBlocked(epDoc)) {
                throw new IOException("Cloudflare บล็อกการเข้าถึง");
            }

            Element mainServer = epDoc.selectFirst("span.halim-btn[data-server='1']");
            if (mainServer == null) {
                return "";
            }

            String postId = mainServer.attr("data-post-id");
            String episode = mainServer.attr("data-episode");
            String nonce = extractNonce(epDoc.html());
            if (nonce.isEmpty()) {
                nonce = "none";
            }

            String lang = getLanguage(epDoc);

            String postData = String.format(
                "action=halim_ajax_player&nonce=%s&episode=%s&postid=%s&server=1&lang=%s",
                nonce, episode, postId, lang
            );

            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", referer);
            headers.put("Origin", "https://www.24hd-movie.com");
            headers.put("X-Requested-With", "XMLHttpRequest");
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36");

            Document response = Jsoup.connect("https://api.24hd-movie.com/get.php")
                .headers(headers)
                .ignoreContentType(true)
                .requestBody(postData)
                .post();

            Element iframe = response.selectFirst("iframe");
            if (iframe == null) {
                return "";
            }

            String videoUrl = iframe.attr("src");
            return convertToM3u8Url(videoUrl);

        } catch (Exception e) {
            throw new IOException("การดึงวิดีโอล้มเหลว", e);
        }
    }
    
    private String extractNonce(String html) {
        Pattern pattern = Pattern.compile("var halim_nonce\\s*=\\s*['\"]([a-f0-9]+)['\"]");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String getLanguage(Document doc) {
        Element langSelect = doc.selectFirst("#Lang_select");
        if (langSelect == null) {
            return "Sound Track";
        }

        boolean hasThai = false;
        boolean hasSoundTrack = false;

        for (Element option : langSelect.select("option")) {
            String value = option.attr("value").trim();
            if (value.equalsIgnoreCase("Thai")) {
                hasThai = true;
            } else if (value.equalsIgnoreCase("Sound Track")) {
                hasSoundTrack = true;
            }
        }

        if (hasThai) {
            return "Thai";
        } else if (hasSoundTrack) {
            return "Sound Track";
        } else {
            return "Sound Track";
        }
    }

    private String convertToM3u8Url(String url) {
        if (url.contains(".m3u8")) {
            return url;
        }

        Pattern pattern = Pattern.compile("id=([^&]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String id = matcher.group(1);
            return String.format(
                "https://main.24playerhd.com/m3u8/%s/%s438.m3u8", 
                id, id
            );
        }
        return url;
    }
}