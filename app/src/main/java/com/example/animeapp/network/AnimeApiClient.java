package com.example.animeapp.network;

import android.util.Log;
import com.example.animeapp.models.Anime;
import com.example.animeapp.models.Episode;
import com.example.animeapp.parser.AnimemojiParser;
import com.example.animeapp.parser.FixmonoParser;
import com.example.animeapp.parser.HDD1112Parser;
import com.example.animeapp.parser.Hd24Parser;
import com.example.animeapp.parser.Hdd123Parser;
import com.example.animeapp.parser.SeriesDayParser;
import com.example.animeapp.parser.AnimeParser;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class AnimeApiClient {
    private static final Map<String, AnimeParser> PARSERS = new HashMap<>();
    
    static {
        PARSERS.put("animemoji.tv", new AnimemojiParser());
        PARSERS.put("fixmono.com", new FixmonoParser("fixmono.com"));
        PARSERS.put("goseries4k.com", new FixmonoParser("goseries4k.com"));
        PARSERS.put("wow-drama.com", new FixmonoParser("wow-drama.com"));
        PARSERS.put("seriesday-hd.com", new SeriesDayParser());
        PARSERS.put("www.123hdtv.com", new Hdd123Parser());
        PARSERS.put("www.24hd-movie.com", new Hd24Parser());
        PARSERS.put("1112hd2.com", new HDD1112Parser());
        PARSERS.put("oklive-1.xyz", new HDD1112Parser());
        PARSERS.put("online225.com", new HDD1112Parser());
        PARSERS.put("mycdn-hd.xyz", new HDD1112Parser());
    }

    public static Anime fetchAnimeData(String url, AnimeLoadListener listener) throws IOException {
        AnimeParser parser = getParserForUrl(url);
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36")
                .header("Referer", "https://www.google.com")
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response.code());
        }

        String html = response.body().string();
        Document doc = Jsoup.parse(html, url);
        response.close();

        Anime anime = parser.parseAnimeDetail(doc);

        if (listener != null) {
            listener.onTotalEpisodes(anime.getEpisodes().size());
            for (int i = 0; i < anime.getEpisodes().size(); i++) {
                listener.onProgress(i + 1, anime.getEpisodes().size());
            }
        }

        return anime;
    }

    public static String fetchVideoUrl(String epUrl, String referer, int episodeNumber) throws IOException {
        AnimeParser parser = getParserForUrl(epUrl);
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(epUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36")
                .header("Referer", referer)
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Unexpected HTTP response: " + response.code());
        }

        String html = response.body().string();
        Document doc = Jsoup.parse(html, epUrl);
        response.close();

        return parser.parseVideoUrl(doc, referer, episodeNumber);
    }

    private static AnimeParser getParserForUrl(String url) {
        for (Map.Entry<String, AnimeParser> entry : PARSERS.entrySet()) {
            if (url.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException("No parser available for URL: " + url);
    }
}