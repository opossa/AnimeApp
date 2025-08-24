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


public class AnimemojiParser implements AnimeParser {
    @Override
    public Anime parseAnimeDetail(Document doc) {
        String title = doc.selectFirst("meta[property=og:title]").attr("content")
                       .replaceAll("[-–]\\s*AnimeMoji.*$", "").trim();
        String image = doc.selectFirst("meta[property=og:image]").attr("content");
       // String referer = doc.baseUri().split("/episode")[0] + "/";
        //List<Episode> episodes = parseEpisodes(doc, image);
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
        if (iframe != null && iframe.hasAttr("data-src")) {
            Document iframeDoc = Jsoup.connect(iframe.attr("data-src"))
                                    .userAgent("Mozilla/5.0")
                                    .referrer(referer)
                                    .get();
            
            Pattern pattern = Pattern.compile("var linkplay\\s*=\\s*['\"](https?://[^'\"]+\\.m3u8)['\"]");
            Matcher matcher = pattern.matcher(iframeDoc.html());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }
}