package com.example.animeapp.parser;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.example.animeapp.models.Anime;
import com.example.animeapp.models.Episode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Hdd123Parser implements AnimeParser {
    
    @Override
    public Anime parseAnimeDetail(Document doc) {
        String title = doc.selectFirst("meta[property=og:title]").attr("content");
        String image = doc.selectFirst("meta[property=og:image]").attr("content");
        String referer = "https://www.123hdtv.com/";
        return new Anime(title, doc.baseUri(), image, parseEpisodes(doc, image));
    }

    @Override
    public List<Episode> parseEpisodes(Document doc, String imageUrl) {
        List<Episode> episodes = new ArrayList<>();
        Elements serverLists = doc.select("div.list-eps-ajax");
        
        for (Element serverList : serverLists) {
            Elements episodeElements = serverList.select("li.halim-episode");
            
            for (Element episodeElement : episodeElements) {
                Element span = episodeElement.selectFirst("span.halim-btn");
                if (span != null) {
                    String episodeTitle = span.attr("data-title");
                    String dataPostId = span.attr("data-post-id");
                    String dataEpisode = span.attr("data-episode");
                    
                    episodes.add(new Episode(
                        episodeTitle,
                        doc.baseUri(),
                        imageUrl,
                        "",
                        doc.baseUri(),
                        Integer.parseInt(dataEpisode)
                    ));
                }
            }
        }
        return episodes;
    }

    @Override
    public String parseVideoUrl(Document epDoc, String referer, int episodeNumber) throws IOException {
        String nonce = extractNonce(epDoc.html());
        Element activeSpan = epDoc.selectFirst("span.halim-btn.active");

        if (activeSpan == null) {
            return "";
        }

        String dataPostId = activeSpan.attr("data-post-id");
        String dataServer = activeSpan.attr("data-server");
        String dataEpisode = activeSpan.attr("data-episode");

        String postData = String.format(
            "action=halim_ajax_player&nonce=%s&episode=%s&postid=%s&server=%s",
            nonce, dataEpisode, dataPostId, dataServer
        );

        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(
            postData,
            MediaType.parse("application/x-www-form-urlencoded")
        );

        Request request = new Request.Builder()
            .url("https://www.123hdtv.com/api/get.php")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Referer", referer)
            .header("User-Agent", "Mozilla/5.0")
            .post(body)
            .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            response.close();
            return "";
        }

        String html = response.body().string();
        response.close();

        Document iframeDoc = Jsoup.parse(html);
        Element iframe = iframeDoc.selectFirst("iframe");

        if (iframe != null) {
            String videoUrl = iframe.attr("src");
            return convertToM3u8Url(videoUrl);
        }

        return "";
    }

    private String extractNonce(String html) {
        Pattern pattern = Pattern.compile("ajax_player.+?\"nonce\":\"(.+?)\"");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String convertToM3u8Url(String inputUrl) {
        Pattern pattern = Pattern.compile("id=([^&]+)");
        Matcher matcher = pattern.matcher(inputUrl);
        
        if (matcher.find()) {
            String id = matcher.group(1);
            return String.format("https://main.24playerhd.com/m3u8/%s/%s438.m3u8", id, id);
        }
        return inputUrl;
    }
}