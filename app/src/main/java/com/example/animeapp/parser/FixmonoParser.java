package com.example.animeapp.parser;

import com.example.animeapp.models.Anime;
import com.example.animeapp.models.Episode;
import com.example.animeapp.models.HostListModel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FixmonoParser implements AnimeParser {
    private final OkHttpClient client = new OkHttpClient();
    private String referer;
    private String domain;
    private String action;

    public FixmonoParser(String domain) {
        this.domain = domain;
        if (domain.contains("fixmono.com")) {
            this.action = "mix_get_player";
        } else {
            this.action = "miru_custom_player";
        }
    }

    @Override
    public Anime parseAnimeDetail(Document doc) {
        this.referer = doc.baseUri();
        this.domain = doc.baseUri().replaceAll("^(https?://)?(www\\.)?([^/]+).*$", "$3");

        String title = doc.selectFirst("meta[property=og:title]").attr("content")
                .replaceAll("[\\[\\].:]", "").trim();
        String image = doc.selectFirst("meta[property=og:image]").attr("content");

        return new Anime(title, doc.baseUri(), image, parseEpisodes(doc, image));
    }

    @Override
    public List<Episode> parseEpisodes(Document doc, String imageUrl) {
        List<Episode> episodes = new ArrayList<>();

        Elements episodeElements = doc.select("span.episode[episode-id], .mp-ep-btn[data-id]");
        for (int i = 0; i < episodeElements.size(); i++) {
            Element ep = episodeElements.get(i);
            String episodeId = ep.attr("episode-id");
            if (episodeId.isEmpty()) {
                episodeId = ep.attr("data-id");
            }

            String fakeUrl = referer + "?episode-id=" + episodeId;

            episodes.add(new Episode(
                    "ตอนที่ " + (i + 1),
                    fakeUrl,
                    imageUrl,
                    "",
                    referer,
                    i + 1
            ));
        }

        return episodes;
    }

    @Override
    public String parseVideoUrl(Document epDoc, String referer, int episodeNumber) throws IOException {
        String url = epDoc.baseUri();
        Pattern pattern = Pattern.compile("episode-id=([^&]+)");
        Matcher matcher = pattern.matcher(url);

        if (!matcher.find()) {
            throw new IOException("ไม่พบ episode-id ใน URL");
        }

        String episodeId = matcher.group(1);
        try {
            return getVideoUrlAjax(episodeId);
        } catch (IOException e) {
            System.out.println("วิธีหลักไม่สำเร็จ, ลองวิธีสำรอง: " + e.getMessage());
            return tryAlternativeMethods(episodeId, epDoc);
        }
    }

    private String getVideoUrlAjax(String episodeId) throws IOException {
        String ajaxUrl = "https://" + domain + "/wp-admin/admin-ajax.php";

        FormBody formBody = new FormBody.Builder()
                .add("action", action)
                .add("post_id", episodeId)
                .build();

        Request request = new Request.Builder()
                .url(ajaxUrl)
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", referer)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("AJAX request failed: " + response.code());
            }

            String responseBody = response.body().string();
            
            if (responseBody.contains("jwplayer(") || responseBody.contains("sources: [")) {
                return parseJwPlayerResponse(responseBody);
            }
            
            try {
                JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
                if (root.has("success") && root.get("success").getAsBoolean()) {
                    JsonObject player = root.getAsJsonObject("player");
                    if (player != null) {
                        if (player.has("primary")) {
                            String primaryUrl = player.get("primary").getAsString().replace("\\/", "/");
                            String videoUrl = processIframeUrlFull(primaryUrl);
                            if (videoUrl != null) return videoUrl;
                        }

                        if (player.has("data")) {
                            JsonObject data = player.getAsJsonObject("data");
                            if (data.has("soundtrack")) {
                                JsonArray soundtrack = data.getAsJsonArray("soundtrack");
                                for (JsonElement elem : soundtrack) {
                                    JsonObject track = elem.getAsJsonObject();
                                    if ("iframe".equals(track.get("type").getAsString()) && track.has("url")) {
                                        String iframeUrl = track.get("url").getAsString().replace("\\/", "/");
                                        String videoUrl = processIframeUrlFull(iframeUrl);
                                        if (videoUrl != null) return videoUrl;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
            
            Document doc = org.jsoup.Jsoup.parse(responseBody);
            Element iframe = doc.selectFirst("iframe");
            if (iframe != null) {
                String src = iframe.attr("src");
                if (src != null && !src.isEmpty()) {
                    String videoUrl = processIframeUrlFull(src);
                    if (videoUrl != null) return videoUrl;
                }
            }
            
            throw new IOException("ไม่พบลิงก์วิดีโอจาก ajax response");
        }
    }

    private String parseJwPlayerResponse(String html) throws IOException {
        Pattern jwPattern = Pattern.compile(
            "sources:\\s*\\[\\s*\\{\\s*file:\\s*\"([^\"]+\\.m3u8)\"",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = jwPattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).replace("\\/", "/");
        }
        
        Pattern sourcesPattern = Pattern.compile(
            "\"sources\"\\s*:\\s*\\[\\s*\\{\\s*\"file\"\\s*:\\s*\"([^\"]+\\.m3u8)\"",
            Pattern.CASE_INSENSITIVE
        );
        matcher = sourcesPattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).replace("\\/", "/");
        }
        
        throw new IOException("ไม่พบลิงก์วิดีโอใน JW Player response");
    }

    private String tryAlternativeMethods(String episodeId, Document epDoc) throws IOException {
        String directUrl = findDirectVideoUrl(epDoc.html());
        if (directUrl != null) {
            return directUrl;
        }

        String iframeUrl = findIframeUrl(epDoc.html());
        if (iframeUrl != null) {
            try {
                return processIframeUrlFull(iframeUrl);
            } catch (IOException e) {
                System.out.println("การประมวลผล iframe ล้มเหลว: " + e.getMessage());
            }
        }

        String scriptUrl = findVideoUrlInScripts(epDoc);
        if (scriptUrl != null) {
            return scriptUrl;
        }

        Elements servers = epDoc.select("div.mp-s-sl");
        if (!servers.isEmpty()) {
            for (Element server : servers) {
                String serverUrl = server.attr("data-id");
                if (serverUrl != null && !serverUrl.isEmpty()) {
                    try {
                        String videoUrl = extractFromBackupServer(serverUrl);
                        if (videoUrl != null) {
                            return videoUrl;
                        }
                    } catch (IOException e) {
                        System.out.println("ไม่สามารถดึงจากเซิร์ฟเวอร์สำรองได้: " + serverUrl);
                    }
                }
            }
        }

        throw new IOException("ไม่พบลิงก์วิดีโอจากวิธีใดๆ ทั้งหมด");
    }

    private String findDirectVideoUrl(String html) {
        Pattern pattern = Pattern.compile(
            "(https?:\\/\\/[^\\s\"']+\\.(?:m3u8|mp4|mkv|webm))",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String findIframeUrl(String html) {
        Pattern iframePattern = Pattern.compile(
            "<iframe[^>]+src=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
        );
        Matcher iframeMatcher = iframePattern.matcher(html);
        return iframeMatcher.find() ? iframeMatcher.group(1) : null;
    }

    private String findVideoUrlInScripts(Document doc) {
        Elements scripts = doc.select("script");
        for (Element script : scripts) {
            String scriptText = script.html();
            
            String jsonUrl = findJsonVideoUrl(scriptText);
            if (jsonUrl != null) return jsonUrl;
            
            String jsUrl = findJsVideoUrl(scriptText);
            if (jsUrl != null) return jsUrl;
        }
        return null;
    }

    private String findJsonVideoUrl(String text) {
        Pattern jsonPattern = Pattern.compile(
            "\"(?:file|url|src)\"\\s*:\\s*\"([^\"]+\\.(?:m3u8|mp4))\"",
            Pattern.CASE_INSENSITIVE
        );
        Matcher jsonMatcher = jsonPattern.matcher(text);
        return jsonMatcher.find() ? jsonMatcher.group(1).replace("\\/", "/") : null;
    }

    private String findJsVideoUrl(String text) {
        Pattern jsPattern = Pattern.compile(
            "(?:videoSources|sources|jwplayer\\().*?file.*?\"(https?:\\\\/\\\\/[^\"]+)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher jsMatcher = jsPattern.matcher(text);
        return jsMatcher.find() ? jsMatcher.group(1).replace("\\/", "/") : null;
    }

    private String extractFromBackupServer(String serverUrl) throws IOException {
        Request request = new Request.Builder()
            .url(serverUrl)
            .header("User-Agent", "Mozilla/5.0")
            .header("Referer", referer)
            .build();

        try (Response response = client.newCall(request).execute()) {
            String html = response.body().string();
            Document doc = org.jsoup.Jsoup.parse(html);
            
            String directUrl = findDirectVideoUrl(html);
            if (directUrl != null) return directUrl;
            
            String iframeUrl = findIframeUrl(html);
            if (iframeUrl != null) return processIframeUrlFull(iframeUrl);
            
            String scriptUrl = findVideoUrlInScripts(doc);
            if (scriptUrl != null) return scriptUrl;
        }
        return null;
    }

    private String processIframeUrlFull(String iframeUrl) throws IOException {
        try {
            return originalIframeProcessing(iframeUrl);
        } catch (IOException e) {
            System.out.println("วิธีเดิมไม่ทำงานใน iframe, ลองวิธีใหม่: " + e.getMessage());
            
            Request request = new Request.Builder()
                .url(iframeUrl)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", referer)
                .build();

            try (Response response = client.newCall(request).execute()) {
                String html = response.body().string();
                
                String directUrl = findDirectVideoUrl(html);
                if (directUrl != null) return directUrl;
                
                String nestedIframe = findIframeUrl(html);
                if (nestedIframe != null) return processIframeUrlFull(nestedIframe);
                
                Document doc = org.jsoup.Jsoup.parse(html);
                String scriptUrl = findVideoUrlInScripts(doc);
                if (scriptUrl != null) return scriptUrl;
            }
            
            throw new IOException("ไม่พบวิดีโอใน iframe");
        }
    }

    private String originalIframeProcessing(String iframeUrl) throws IOException {
        Request request = new Request.Builder()
                .url(iframeUrl)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", referer)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String html = response.body().string();

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

                        Gson gson = new Gson();
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
                                    "https://" + domain + "/api/files/");

                            videoUrl = videoUrl.replace("\\/", "/");

                            return videoUrl;
                        }
                    }
                }
            }
        }

        throw new IOException("ไม่พบ video URL ใน iframe script");
    }

    public String getAction() {
        return action;
    }

    public String getPostData(String postId) {
        return "action=" + action + "&post=" + postId;
    }
}