package com.example.animeapp.parser;

import com.example.animeapp.models.Anime;
import com.example.animeapp.models.Episode;
import java.io.IOException;
import java.util.List;
import org.jsoup.nodes.Document;

public interface AnimeParser {
    Anime parseAnimeDetail(Document doc) throws IOException;
    List<Episode> parseEpisodes(Document doc, String imageUrl);
    String parseVideoUrl(Document doc, String referer, int episodeNumber) throws IOException;
}