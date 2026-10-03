package com.deliciousbread481.maltmix.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.deliciousbread481.maltmix.model.Song;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LocalFavStore {

    private static final String PREF_NAME = "local_fav";
    private static final String KEY_FOLDERS = "folders";
    private static final String KEY_SONGS_PREFIX = "songs_";

    private final SharedPreferences prefs;

    public LocalFavStore(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<String> getFolderNames() {
        List<String> names = new ArrayList<>();
        String json = prefs.getString(KEY_FOLDERS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                names.add(arr.getString(i));
            }
        } catch (Exception ignored) {
        }
        return names;
    }

    public boolean addFolder(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        String trimmed = name.trim();
        List<String> names = getFolderNames();
        if (names.contains(trimmed)) return false;
        names.add(trimmed);
        saveFolders(names);
        return true;
    }

    public void removeFolder(String name) {
        List<String> names = getFolderNames();
        names.remove(name);
        saveFolders(names);
        prefs.edit().remove(KEY_SONGS_PREFIX + name).apply();
    }

    private void saveFolders(List<String> names) {
        JSONArray arr = new JSONArray();
        for (String n : names) {
            arr.put(n);
        }
        prefs.edit().putString(KEY_FOLDERS, arr.toString()).apply();
    }

    // ---------- 歌曲管理 ----------

    public List<Song> getSongs(String folderName) {
        List<Song> result = new ArrayList<>();
        String json = prefs.getString(KEY_SONGS_PREFIX + folderName, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Song song = new Song(
                        obj.optString("id", ""),
                        obj.optString("title", ""),
                        obj.optString("artist", ""),
                        obj.optString("coverUrl", ""),
                        obj.optString("playUrl", ""),
                        obj.optString("source", "")
                );
                result.add(song);
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    public boolean addSong(String folderName, Song song) {
        List<Song> songs = getSongs(folderName);
        for (Song s : songs) {
            if (s.getId().equals(song.getId())) {
                return false;
            }
        }
        songs.add(0,song);
        saveSongs(folderName, songs);
        return true;
    }

    public void removeSong(String folderName, Song song) {
        List<Song> songs = getSongs(folderName);
        songs.remove(song);
        saveSongs(folderName, songs);
    }

    public void saveSongs(String folderName, List<Song> songs) {
        JSONArray arr = new JSONArray();
        for (Song s : songs) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", s.getId() == null ? "" : s.getId());
                obj.put("title", s.getTitle() == null ? "" : s.getTitle());
                obj.put("artist", s.getArtist() == null ? "" : s.getArtist());
                obj.put("coverUrl", s.getCoverUrl() == null ? "" : s.getCoverUrl());
                obj.put("playUrl", s.getPlayUrl() == null ? "" : s.getPlayUrl());
                obj.put("source", s.getSource() == null ? "" : s.getSource());
                arr.put(obj);
            } catch (Exception ignored) {
            }
        }
        prefs.edit().putString(KEY_SONGS_PREFIX + folderName, arr.toString()).apply();
    }
}