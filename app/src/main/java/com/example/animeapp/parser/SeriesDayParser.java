package com.example.animeapp.parser;

import org.jsoup.nodes.Document;
import com.example.animeapp.models.Anime;
import java.util.List;
import com.example.animeapp.models.Episode;
import java.io.IOException;
import java.util.ArrayList;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import org.jsoup.Jsoup;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SeriesDayParser implements AnimeParser {
    
    @Override
    public Anime parseAnimeDetail(Document doc) {
        String title = doc.selectFirst("meta[property=og:title]").attr("content");
        String image = doc.selectFirst("img.attachment-full.size-full.wp-post-image").attr("data-lazy-src");
        String referer = "https://www.seriesday-hd.com/";
        
        return new Anime(title, doc.baseUri(), image, parseEpisodes(doc, image));
    }

    @Override
    public List<Episode> parseEpisodes(Document doc, String imageUrl) {
        List<Episode> episodes = new ArrayList<>();
        Elements options = doc.select("select[name=Sequel_select] option");
        
        for (int i = 0; i < options.size(); i++) {
            Element option = options.get(i);
            String episodeName = option.text().trim();
            String episodeUrl = option.attr("value");
            
            episodes.add(new Episode(
                episodeName,
                "https://www.seriesday-hd.com" + episodeUrl,
                imageUrl,
                "", // videoUrl
                doc.baseUri(),
                i + 1
            ));
            
            
        }
        return episodes;
    }

    @Override
    public String parseVideoUrl(Document epDoc, String referer, int episodeNumber) throws IOException {
        Element langSelect = epDoc.selectFirst("select#Lang_select");
        Element defaultOption = langSelect.select("option[value=Thai]").first();
        if (defaultOption == null) {
            defaultOption = langSelect.select("option[value=Sound Track]").first();
        }
        
        String defaultLang = defaultOption.attr("value");
        Element lsub = epDoc.selectFirst("span.halim-btn.halim-btn-2.active.halim-info-2-1.box-shadow");
        
        String dataPostId = lsub.attr("data-post-id");
        String dataServer = lsub.attr("data-server");
        String dataType = lsub.attr("data-type");
        
        String postUrl = "https://www.seriesday-hd.com/api/get.php";
        String postData = String.format(
            "action=halim_ajax_player&nonce=%s&episode=%d&postid=%s&lang=%s&server=%s",
            dataType, episodeNumber, dataPostId, defaultLang, dataServer
        );
        
        try {
            Document response = Jsoup.connect(postUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .userAgent("Mozilla/5.0")
                    .referrer(referer)
                    .requestBody(postData)
                    .post();
            
            Element iframe = response.selectFirst("iframe");
            if (iframe != null) {
                String videoUrl = iframe.attr("src");
                return convertUrl(videoUrl);
            }
            
            postData = postData.replace("server=" + dataServer, "server=3");
            response = Jsoup.connect(postUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .userAgent("Mozilla/5.0")
                    .referrer(referer)
                    .requestBody(postData)
                    .post();
            
            iframe = response.selectFirst("iframe");
            if (iframe != null) {
                String videoUrl = iframe.attr("src");
                return convertUrl(videoUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return "";
    }

    private String convertUrl(String inputUrl) {
        Pattern pattern = Pattern.compile("id=([^&]+)");
        Matcher matcher = pattern.matcher(inputUrl);
        
        if (matcher.find()) {
            String id = matcher.group(1);
            return String.format("https://main.24playerhd.com/m3u8/%s/%s438.m3u8", id, id);
        }
       // https://main.24playerhd.com/m3u8/1b1abd74144a2f5706f87f4f/1b1abd74144a2f5706f87f4f438.m3u8
        return inputUrl;
    }
}