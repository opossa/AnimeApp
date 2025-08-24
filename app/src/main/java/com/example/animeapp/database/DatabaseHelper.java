package com.example.animeapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.animeapp.models.HistoryItem;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "anime_history.db";
    private static final int DATABASE_VERSION = 1;
    
    private static final String TABLE_HISTORY = "watch_history";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_IMAGE_URL = "image_url";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_EPISODE = "episode";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_ANIME_URL = "anime_url";
    private static final String COLUMN_EPISODE_URL = "episode_url";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_HISTORY_TABLE = "CREATE TABLE " + TABLE_HISTORY + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_EPISODE + " TEXT,"
                + COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + COLUMN_ANIME_URL + " TEXT,"
                + COLUMN_IMAGE_URL + " TEXT,"
                + COLUMN_EPISODE_URL + " TEXT"
                + ")";
        db.execSQL(CREATE_HISTORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    public void addOrUpdateHistory(String title, String episode, String animeUrl, String imageUrl, String episodeUrl) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_EPISODE, episode);
        values.put(COLUMN_ANIME_URL, animeUrl);
        values.put(COLUMN_IMAGE_URL, imageUrl);
        values.put(COLUMN_EPISODE_URL, episodeUrl);
        values.put(COLUMN_TIMESTAMP, getCurrentThaiTime());
        
        int rowsAffected = db.update(TABLE_HISTORY, values, 
                COLUMN_ANIME_URL + " = ?", new String[]{animeUrl});
        
        if (rowsAffected == 0) {
            db.insert(TABLE_HISTORY, null, values);
        }
        
        db.close();
    }

    public void updateHistory(String title, String episode, String animeUrl, String imageUrl, String episodeUrl) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_EPISODE, episode);
        values.put(COLUMN_ANIME_URL, animeUrl);
        values.put(COLUMN_IMAGE_URL, imageUrl);
        values.put(COLUMN_EPISODE_URL, episodeUrl);
        values.put(COLUMN_TIMESTAMP, getCurrentThaiTime());
        
        db.update(TABLE_HISTORY, values, 
                COLUMN_ANIME_URL + " = ?", new String[]{animeUrl});
        
        db.close();
    }
    
    private String getCurrentThaiTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Bangkok"));
        return sdf.format(new Date());
    }

    public List<HistoryItem> getAllHistory() {
        List<HistoryItem> historyList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_HISTORY + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        
        if (cursor.moveToFirst()) {
            do {
                HistoryItem item = new HistoryItem();
                item.setId(cursor.getInt(0));
                item.setTitle(cursor.getString(1));
                item.setEpisode(cursor.getString(2));
                item.setTimestamp(cursor.getString(3));
                item.setAnimeUrl(cursor.getString(4));
                item.setImageUrl(cursor.getString(5));
                item.setEpisodeUrl(cursor.getString(6));
                
                historyList.add(item);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return historyList;
    }

    public void deleteHistory(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}