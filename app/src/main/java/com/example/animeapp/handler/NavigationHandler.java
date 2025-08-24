package com.example.animeapp.handler;

import android.content.Context;
import android.content.Intent;
import android.view.ContentInfo;
import androidx.core.content.ContextKt;
import com.example.animeapp.R;
import com.example.animeapp.activities.HistoryActivity;

public class NavigationHandler {
    public interface NavigationCallback {
        void switchSource(String sourceName, String url, String title);
    }

    private final NavigationCallback callback;
    private final Context context;

    public NavigationHandler(NavigationCallback callback, Context context) {
        this.callback = callback;
        this.context = context;
    }

    public void handleNavigationItemSelection(int itemId) {
        
        if (itemId == R.id.action_history) {
            Intent intent = new Intent(context, HistoryActivity.class);
            context.startActivity(intent);
        }
        else
        // Animemoji
        if (itemId == R.id.nav_home) {
            callback.switchSource("Animemoji", "https://animemoji.tv/", "Animemoji หน้าแรก");
        } else if (itemId == R.id.nav_dub) {
            callback.switchSource("Animemoji", "https://animemoji.tv/lang/mojianimeth/", "Animemoji พากย์ไทย");
        } else if (itemId == R.id.nav_sub) {
            callback.switchSource("Animemoji", "https://animemoji.tv/category/%e0%b8%ad%e0%b8%99%e0%b8%b4%e0%b9%80%e0%b8%a1%e0%b8%b0%e0%b8%8b%e0%b8%b1%e0%b8%9a%e0%b9%84%e0%b8%97%e0%b8%a2/", "Animemoji ซับไทย");
        } else if (itemId == R.id.nav_chinese) {
            callback.switchSource("Animemoji", "https://animemoji.tv/category/china-no1/", "Animemoji อนิเมะจีน");
        }
        // Fixmono
        else if (itemId == R.id.nav_fixmono_new) {
            callback.switchSource("Fixmono", "https://fixmono.com/serie/", "Fixmono ซีรี่ย์อัพเดท");
        } else if (itemId == R.id.nav_fixmono_th) {
            callback.switchSource("Fixmono", "https://fixmono.com/category/ซีรี่ย์จีนพากย์ไทย/", "Fixmono ซีรี่ย์จีนพากย์ไทย");
        } else if (itemId == R.id.nav_fixmono_scb) {
            callback.switchSource("Fixmono", "https://fixmono.com/category/ซีรี่ย์จีนซับไทย/", "Fixmono ซีรี่ย์จีนซับไทย");
        } else if (itemId == R.id.nav_fixmono_ke) {
            callback.switchSource("Fixmono", "https://fixmono.com/category/ซีรี่ย์เกาหลีพากย์ไทย/", "Fixmono ซีรี่ย์เกาหลีพากย์ไทย");
        } else if (itemId == R.id.nav_fixmono_keth) {
            callback.switchSource("Fixmono", "https://fixmono.com/category/ซีรี่ย์เกาหลีซับไทย/", "Fixmono ซีรี่ย์เกาหลีซับไทย");
        } else if (itemId == R.id.nav_fixmono_en) {
            callback.switchSource("Fixmono", "https://fixmono.com/category/ดูซีรี่ย์ฝรั่ง/", "Fixmono ซีรี่ย์ฝรั่ง");
        } else if (itemId == R.id.nav_fixmono_jp) {
            callback.switchSource("Fixmono", "https://fixmono.com/category/ดูซีรี่ย์ญี่ปุ่น/", "Fixmono ซีรี่ย์ญี่ปุ่น");
        }
        // GoSeries4K
        else if (itemId == R.id.goseries4k_s) {
            callback.switchSource("GoSeries4K", "https://goseries4k.com/category/ดู-ซีรี่ย์ออนไลน์/", "GoSeries4K ซีรี่ย์ออนไลน์");
        } else if (itemId == R.id.goseries4k_m) {
            callback.switchSource("GoSeries4K", "https://goseries4k.com/category/ดู-ซีรี่ย์จีน-ออนไลน์/", "GoSeries4K ซีรี่ย์จีน");
        } else if (itemId == R.id.goseries4k_l) {
            callback.switchSource("GoSeries4K", "https://goseries4k.com/category/ดูซีรี่ย์เกาหลี-ออนไลน์/", "GoSeries4K ซีรี่ย์เกาหลี");
        } else if (itemId == R.id.goseries4k_h) {
            callback.switchSource("GoSeries4K", "https://goseries4k.com/cat_category/ดูซีรี่ย์จีนพากย์ไทย/", "GoSeries4K ซีรี่ย์จีนพากย์ไทย");
        } else if (itemId == R.id.goseries4k_d) {
            callback.switchSource("GoSeries4K", "https://goseries4k.com/category/ซีรี่ย์ญี่ปุ่น/", "GoSeries4K ซีรี่ย์ญี่ปุ่น");
        } else if (itemId == R.id.goseries4k_b) {
            callback.switchSource("GoSeries4K", "https://goseries4k.com/category/ซีรี่ย์ฝรั่ง/", "GoSeries4K ซีรี่ย์ฝรั่ง");
        } else if (itemId == R.id.goseries4k_g) {
            callback.switchSource("GoSeries4K", "https://goseries4k.com/category/ซีรี่ย์ไทย/", "GoSeries4K ซีรี่ย์ไทย");
        }
        // WowDrama
        else if (itemId == R.id.wowdrama_g) {
            callback.switchSource("WowDrama", "https://wow-drama.com/category/the-series-all/", "WowDrama ซีรี่ย์ออนไลน์");
        } else if (itemId == R.id.wowdrama_k) {
            callback.switchSource("WowDrama", "https://wow-drama.com/category/doo-free-24/", "WowDrama ซีรี่ย์จีน");
        } else if (itemId == R.id.wowdrama_v) {
            callback.switchSource("WowDrama", "https://wow-drama.com/category/series-korea/", "WowDrama ซีรี่ย์เกาหลี");
        } else if (itemId == R.id.wowdrama_l) {
            callback.switchSource("WowDrama", "https://wow-drama.com/category/japan-series/", "WowDrama ซีรี่ย์ญี่ปุ่น");
        } else if (itemId == R.id.wowdrama_m) {
            callback.switchSource("WowDrama", "https://wow-drama.com/category/the-series-th/", "WowDrama ละครไทย");
        } else if (itemId == R.id.wowdrama_a) {
            callback.switchSource("WowDrama", "https://wow-drama.com/cat_category/cn-2024-th/", "WowDrama ซีรี่ย์จีนพากย์ไทย");
        } else if (itemId == R.id.wowdrama_f) {
            callback.switchSource("WowDrama", "https://wow-drama.com/cat_category/jp-thai-sound/", "WowDrama ซีรี่ย์ญี่ปุ่นพากย์ไทย");
        } else if (itemId == R.id.wowdrama_n) {
            callback.switchSource("WowDrama", "https://wow-drama.com/cat_category/korea-thai-sound/", "WowDrama ซีรี่ย์เกาหลีพากย์ไทย");
        }
        //HDD1112
        else if (itemId == R.id.hdd1112_h) {
            callback.switchSource("1112HDD", "https://1112hd2.com/ซีรี่ย์จีน-พากย์ไทย/", "1112HDD ซีรี่ย์จีน-พากย์ไทย");
        } else if (itemId == R.id.hdd1112_f) {
            callback.switchSource("1112HDD", "https://1112hd2.com/ซีรี่ย์เกาหลีพากย์ไทย/", "1112HDD ซีรี่ย์เกาหลีพากย์ไทย");
        } else if (itemId == R.id.hdd1112_g) {
            callback.switchSource("1112HDD", "https://1112hd2.com/ดูซีรี่ย์ฝรั่ง/ซีรี่ย์ฝรั่ง-พากย์ไทย/", "1112HDD ซีรี่ย์ฝรั่ง-พากย์ไทย");
        } else if (itemId == R.id.hdd1112_j) {
            callback.switchSource("1112HDD", "https://1112hd2.com/ซีรี่ย์ใหม่-2025/", "1112HDD ซีรี่ย์ใหม่-2025");
        } else if (itemId == R.id.hdd1112_k) {
            callback.switchSource("1112HDD", "https://1112hd2.com/ซีรี่ย์ไทย/", "1112HDD ซีรี่ย์ไทย");
        } else if (itemId == R.id.hdd1112_s) {
            callback.switchSource("1112HDD", "https://1112hd2.com/ดูซีรี่ย์จีน/", "1112HDD ดูซีรี่ย์จีน");
        } else if (itemId == R.id.hdd1112_l) {
            callback.switchSource("1112HDD", "https://1112hd2.com/ดูซีรี่ย์เกาหลี/", "ดูซีรี่ย์เกาหลี");
        }
        // SeriesDay
        else if (itemId == R.id.seriesday_j) {
            callback.switchSource("SeriesDay", "https://www.seriesday-hd.com/ซีรี่ย์จีน/", "SeriesDay ซีรี่ย์จีน");
        } else if (itemId == R.id.seriesday_h) {
            callback.switchSource("SeriesDay", "https://www.seriesday-hd.com/ซีรี่ย์เกาหลี/", "SeriesDay ซีรี่ย์เกาหลี");
        } else if (itemId == R.id.seriesday_s) {
            callback.switchSource("SeriesDay", "https://www.seriesday-hd.com/ซีรี่ย์ฝรั่ง/", "SeriesDay ซีรี่ย์ฝรั่ง");
        } else if (itemId == R.id.seriesday_r) {
            callback.switchSource("SeriesDay", "https://www.seriesday-hd.com/ซีรี่ย์ญี่ปุ่น/", "SeriesDay ซีรี่ย์ญี่ปุ่น");
        } else if (itemId == R.id.seriesday_a) {
            callback.switchSource("SeriesDay", "https://www.seriesday-hd.com/ซีรี่ย์ไทย/", "SeriesDay ซีรี่ย์ไทย");
        } else if (itemId == R.id.seriesday_u) {
            callback.switchSource("SeriesDay", "https://www.seriesday-hd.com/ซีรี่ย์ใหม่-2025/", "SeriesDay ซีรี่ย์ใหม่");
        } else if (itemId == R.id.seriesday_m) {
            callback.switchSource("SeriesDay", "https://www.seriesday-hd.com/netflix/", "SeriesDay netflix");
        } else if (itemId == R.id.seriesday_p) {
            callback.switchSource("SeriesDay", "https://www.seriesday-hd.com/ซีรี่ย์พากย์ไทย/", "SeriesDay ซีรี่ย์พากย์ไทย");
        }
        //หนังออนไลน์123
        else if (itemId == R.id.movie_all) {
            callback.switchSource("123HDD", "https://www.123hdtv.com/ดูหนังออนไลน์", "123HDD หนังออนไลน์");
        } else if (itemId == R.id.movie_j) {
            callback.switchSource("123HDD", "https://www.123hdtv.com/หนังใหม่-2025", "123HDD หนังใหม่-2025");
        } else if (itemId == R.id.movie_l) {
            callback.switchSource("123HDD", "https://www.123hdtv.com/หนังใหม่-2024", "123HDD หนังใหม่-2024");
        } else if (itemId == R.id.movie_u) {
            callback.switchSource("123HDD", "https://www.123hdtv.com/หนังใหม่-2023", "123HDD หนังใหม่-2023");
        } else if (itemId == R.id.movie_s) {
            callback.switchSource("123HDD", "https://www.123hdtv.com/หนังใหม่-2022", "123HDD หนังใหม่-2023");
        } else if (itemId == R.id.movie_c) {
            callback.switchSource("123HDD", "https://www.123hdtv.com/หนังใหม่-2021", "123HDD หนังใหม่-2021");
        } else if (itemId == R.id.movie_n) {
            callback.switchSource("123HDD", "https://www.123hdtv.com/หนังใหม่-2020", "123HDD หนังใหม่-2020");
        } else if (itemId == R.id.movie_t) {
            callback.switchSource("123HDD", "https://www.123hdtv.com/ดูหนังออนไลน์/หนังไทย", "123HDD หนังไทย");
        }
        //หนังออนไลน์24HD
        else if (itemId == R.id.movie24hd_a) {
            callback.switchSource("24HD", "https://www.24hd-movie.com/", "24HD หนังออนไลน์");
        } else if (itemId == R.id.movie24hd_b) {
            callback.switchSource("24HD", "https://www.24hd-movie.com/หนังใหม่-2025/", "24HD หนังใหม่-2025");
        } else if (itemId == R.id.movie24hd_c) {
            callback.switchSource("24HD", "https://www.24hd-movie.com/หนังใหม่-2024", "24HD หนังใหม่-2024");
        } else if (itemId == R.id.movie24hd_d) {
            callback.switchSource("24HD", "https://www.24hd-movie.com/หนังใหม่-2023", "24HD หนังใหม่-2023");
        } else if (itemId == R.id.movie24hd_e) {
            callback.switchSource("24HD", "https://www.24hd-movie.com/หนังชนโรง/", "24HD หนังชนโรง");
        } else if (itemId == R.id.movie24hd_f) {
            callback.switchSource("24HD", "https://www.24hd-movie.com/หนังไทย/", "24HD หนังไทย");
        } 
    }
}