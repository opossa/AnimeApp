package com.example.animeapp.parser;

import android.util.Log;

import com.example.animeapp.models.Anime;
import com.example.animeapp.models.Episode;
import com.example.animeapp.models.HostListModel;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HDD1112Parser implements AnimeParser {
    private static final String TAG = "HDD1112Parser";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private String referer;
    private String animeImageUrl;

    @Override
    public Anime parseAnimeDetail(Document doc) throws IOException {
        this.referer = doc.baseUri();

        if (!this.referer.contains("1112hd2.com")) {
            throw new IOException("ไม่สามารถดึงข้อมูลซีรี่ย์ได้");
        }

        String title = doc.selectFirst("meta[property=og:title]").attr("content");
        this.animeImageUrl = doc.selectFirst("meta[property=og:image]").attr("content");

        if (this.animeImageUrl == null || this.animeImageUrl.isEmpty()) {
            this.animeImageUrl = "https://1112hd2.com/wp-content/themes/cj/assets/images/default-poster.jpg";
        }

        String postId = extractPostId(doc);
        String nonce = extractNonce(doc);

        
        if (postId == null || nonce == null) {
            throw new IllegalStateException("ไม่พบ post_id หรือ nonce");
        }

        List<Episode> episodes = parseEpisodes(doc, this.animeImageUrl);
        return new Anime(title, doc.baseUri(), this.animeImageUrl, episodes);
    }

    @Override
    public List<Episode> parseEpisodes(Document doc, String imageUrl) {
        String postId = extractPostId(doc);
        String nonce = extractNonce(doc);

        if (postId == null || nonce == null) {
            Log.e(TAG, "parseEpisodes: missing postId/nonce");
            return new ArrayList<>();
        }

        return fetchEpisodesFromApi(postId, nonce, imageUrl);
    }

    private List<Episode> fetchEpisodesFromApi(String postId, String nonce, String animeImageUrl) {
        List<Episode> episodes = new ArrayList<>();

        try {
            String url = "https://1112hd2.com/wp-admin/admin-ajax.php";
            MediaType formMediaType = MediaType.parse("application/x-www-form-urlencoded");
            RequestBody body = RequestBody.create(
                formMediaType,
                "action=single_get_videos&post_id=" + postId + "&nonce=" + nonce
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("API failed: " + response.code());
            }

            String json = response.body().string();
            Log.d(TAG, "API response: " + json);

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (!root.has("success") || !root.get("success").getAsBoolean()) {
                throw new IOException("API success=false");
            }

            JsonObject data = root.getAsJsonObject("data");
            JsonObject series = data.getAsJsonObject("series");
            if (!series.has("videos")) throw new IOException("Missing videos");

            JsonObject videos = series.getAsJsonObject("videos");

            JsonObject selectedVideos = null;
            String versionKey = "";

            if (videos.has("th")) {
                JsonObject thObj = videos.getAsJsonObject("th");
                if (thObj.has("list") && thObj.get("list").isJsonObject()) {
                    selectedVideos = thObj;
                    versionKey = "th";
                }
            }

            if (selectedVideos == null && videos.has("sub")) {
                JsonObject subObj = videos.getAsJsonObject("sub");
                if (subObj.has("list") && subObj.get("list").isJsonObject()) {
                    selectedVideos = subObj;
                    versionKey = "sub";
                }
            }

            if (selectedVideos == null) {
                Log.e(TAG, "No valid th/sub list found for postId=" + postId);
                return episodes;
            }

            int totalEp = selectedVideos.has("total_ep") ? selectedVideos.get("total_ep").getAsInt() : 0;
            JsonObject list = selectedVideos.getAsJsonObject("list");

            Log.d(TAG, "Using version=" + versionKey + ", totalEp=" + totalEp);

            for (int i = 1; i <= totalEp; i++) {
                String epKey = "ep_" + i;
                if (list.has(epKey)) {
                    JsonObject epData = list.getAsJsonObject(epKey);
                    String videoUrl = null;

                    if (epData.has("server_1") && epData.getAsJsonObject("server_1").has("url")) {
                        videoUrl = epData.getAsJsonObject("server_1").get("url").getAsString();
                    } else if (epData.has("server_2") && epData.getAsJsonObject("server_2").has("url")) {
                        videoUrl = epData.getAsJsonObject("server_2").get("url").getAsString();
                    }

                    if (videoUrl != null) {
                        Log.d(TAG, "Episode " + i + " -> " + videoUrl);
                        episodes.add(new Episode(
                                "EP." + i,
                                videoUrl,
                                animeImageUrl,
                                "",
                                referer,
                                i
                        ));
                    } else {
                        Log.w(TAG, "EP." + i + " has no videoUrl");
                    }
                } else {
                    Log.w(TAG, "Missing " + epKey);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "fetchEpisodesFromApi error: " + e.getMessage(), e);
        }

        return episodes;
    }

    private String extractPostId(Document doc) {
        Element script = doc.selectFirst("script#wp-postviews-cache-js-extra");
        if (script != null) {
            String js = script.data();
            Pattern pattern = Pattern.compile("\"post_id\":\"(\\d+)\"");
            Matcher matcher = pattern.matcher(js);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private String extractNonce(Document doc) {
        Element script = doc.selectFirst("script#wp-postviews-cache-js-extra");
        if (script != null) {
            String js = script.data();
            Pattern pattern = Pattern.compile("\"nonce\":\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(js);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    @Override
    public String parseVideoUrl(Document epDoc, String referer, int episodeNumber) throws IOException {
        this.referer = referer;
        String videoUrl = epDoc.baseUri();

        if (videoUrl.contains("online225.com") || videoUrl.contains("oklive-1.xyz") || videoUrl.contains("mycdn-hd.xyz")) {
            try {
                Document playerDoc = Jsoup.connect(videoUrl)
                        .userAgent("Mozilla/5.0")
                        .referrer(referer)
                        .get();

                Element iframe = playerDoc.selectFirst("iframe[src]");
                if (iframe != null) {
                    return iframe.absUrl("src");
                }

                return extractFromPageScript(playerDoc.html());

            } catch (IOException e) {
                return videoUrl;
            }
        }

        return videoUrl;
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
