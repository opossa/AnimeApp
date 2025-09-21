package com.example.animeapp.parser;

import org.jsoup.nodes.Document;
import com.example.animeapp.models.Anime;
import java.util.List;
import com.example.animeapp.models.Episode;
import java.io.IOException;
import java.util.ArrayList;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.json.JSONArray;
import org.json.JSONObject;

public class AnimemojiParser implements AnimeParser {
    @Override
    public Anime parseAnimeDetail(Document doc) {
        String title = doc.selectFirst("meta[property=og:title]").attr("content")
                       .replaceAll("[-–]\\s*AnimeMoji.*$", "").trim();

        String image = null;
        Element imageEl = doc.selectFirst("meta[property=og:image]");
        if (imageEl != null) {
            image = imageEl.attr("content");
        }

        if (image == null || image.isEmpty()) {
            Element jsonLd = doc.selectFirst("script[type=application/ld+json]");
            if (jsonLd != null) {
                try {
                    JSONObject json = new JSONObject(jsonLd.html());
                    if (json.has("@graph")) {
                        JSONArray graph = json.getJSONArray("@graph");
                        for (int i = 0; i < graph.length(); i++) {
                            JSONObject obj = graph.getJSONObject(i);
                            if (obj.has("@type") && obj.get("@type").toString().contains("ImageObject")) {
                                if (obj.has("url")) {
                                    image = obj.getString("url");
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (image == null) {
            image = "";
        }

        return new Anime(title, doc.baseUri(), image, parseEpisodes(doc, image));
    }

    @Override
    public List<Episode> parseEpisodes(Document doc, String imageUrl) {
        List<Episode> episodes = new ArrayList<>();
        Elements episodeLinks = doc.select("div.episode-b a");

        for (int i = 0; i < episodeLinks.size(); i++) {
            Element link = episodeLinks.get(i);
            episodes.add(new Episode(
                "ตอนที่ " + (i + 1),
                link.attr("href"),
                imageUrl,
                "", // videoUrl
                doc.baseUri().split("/episode")[0] + "/",
                i + 1
            ));
        }
        return episodes;
    }

    @Override
    public String parseVideoUrl(Document epDoc, String referer, int episodeNumber) throws IOException {
        Element iframe = epDoc.selectFirst("iframe.perfmatters-lazy");
        if (iframe != null) {
            String iframeUrl = iframe.hasAttr("data-src") ? iframe.attr("data-src") : iframe.attr("src");

            if (iframeUrl != null && !iframeUrl.isEmpty()) {
                Pattern idPattern = Pattern.compile("embed/([^/?]+)");
                Matcher idMatcher = idPattern.matcher(iframeUrl);

                if (idMatcher.find()) {
                    String videoId = idMatcher.group(1);
                    return "https://moji.abcdxzy.xyz:8443/vod/" + videoId + "/video.mp4/playlist.m3u8";
                }
            }
        }
        return "";
    }
                }
