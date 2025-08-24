package com.example.animeapp.parser;

import com.example.animeapp.models.Anime;
import com.example.animeapp.models.Episode;
import com.example.animeapp.models.HostListModel;
import com.google.gson.Gson;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HDD1112Parser implements AnimeParser {
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private String referer;

    @Override
    public Anime parseAnimeDetail(Document doc) {
        this.referer = doc.baseUri();
        String title = doc.selectFirst("meta[property=og:title]").attr("content");
        String image = doc.selectFirst("meta[property=og:image]").attr("content");
        return new Anime(title, doc.baseUri(), image, parseEpisodes(doc, image));
    }

    @Override
    public List<Episode> parseEpisodes(Document doc, String imageUrl) {
        List<Episode> episodes = new ArrayList<>();

        Elements episodeDivs = doc.select("div.tab_movie_grop.grop_m.main div.btab_ct[data-url]");

        for (int i = 0; i < episodeDivs.size(); i++) {
            Element ep = episodeDivs.get(i);
            String title = ep.text().trim();
            String videoPageUrl = ep.attr("data-url").trim();

            if (!videoPageUrl.isEmpty()) {
                episodes.add(new Episode(title, videoPageUrl, imageUrl, "", this.referer, i + 1));
            }
        }

        return episodes;
    }

    @Override
    public String parseVideoUrl(Document epDoc, String referer, int episodeNumber) throws IOException {
        this.referer = referer;

        Element iframe = epDoc.selectFirst("iframe[src]");
        if (iframe != null) {
            String iframeUrl = iframe.absUrl("src");
            if (!iframeUrl.isEmpty()) {
                try {
                    return originalIframeProcessing(iframeUrl);
                } catch (IOException e) {
                }
            }
        }

        return extractFromPageScript(epDoc.html());
    }

    private String originalIframeProcessing(String iframeUrl) throws IOException {
        Request request = new Request.Builder()
            .url(iframeUrl)
            .header("User-Agent", "Mozilla/5.0")
            .header("Referer", referer)
            .build();

        try (Response response = client.newCall(request).execute()) {
            String html = response.body().string();
            return extractFromPageScript(html);
        }
    }

    private String extractFromPageScript(String html) throws IOException {
        Pattern scriptPattern = Pattern.compile("<script[^>]*>(.*?)</script>", Pattern.DOTALL);
        Matcher scriptMatcher = scriptPattern.matcher(html);

        while (scriptMatcher.find()) {
            String scriptContent = scriptMatcher.group(1);

            if (scriptContent.contains("videoSources")) {
                Pattern videoServerPattern = Pattern.compile("\"videoServer\":\"(\\d+)\"");
                Pattern videoSourcesPattern = Pattern.compile("\"videoSources\":\\[\\{\"file\":\"([^\"]+)\"");
                Pattern hostListPattern = Pattern.compile("\"hostList\":(\\{.*?\\})");

                Matcher videoServerMatcher = videoServerPattern.matcher(scriptContent);
                Matcher videoSourcesMatcher = videoSourcesPattern.matcher(scriptContent);
                Matcher hostListMatcher = hostListPattern.matcher(scriptContent);

                if (videoServerMatcher.find() && videoSourcesMatcher.find() && hostListMatcher.find()) {
                    String videoServer = videoServerMatcher.group(1);
                    String videoFile = videoSourcesMatcher.group(1);
                    String hostListJson = hostListMatcher.group(1);

                    HostListModel hostList = gson.fromJson("{\"hostList\":" + hostListJson + "}", HostListModel.class);

                    List<String> selectedDomainList = hostList.getHostList().get(videoServer);
                    if (selectedDomainList != null && !selectedDomainList.isEmpty()) {
                        String domain = selectedDomainList.get(0)
                            .replace("[", "")
                            .replace("]", "")
                            .replace("'", "")
                            .trim();

                        String videoUrl = videoFile.replaceAll(
                            "https:\\\\/\\\\/\\d+\\\\/cdn\\\\/hls\\\\/",
                            "https://" + domain + "/api/files/"
                        );

                        videoUrl = videoUrl.replace("\\/", "/");

                        return videoUrl;
                    }
                }
            }
        }

        throw new IOException("ไม่พบ video URL ในสคริปต์");
    }
}