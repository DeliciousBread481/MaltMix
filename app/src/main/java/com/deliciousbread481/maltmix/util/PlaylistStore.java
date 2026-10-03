package com.deliciousbread481.maltmix.util;  
  
import android.content.Context;  
import android.content.SharedPreferences;  
  
import com.deliciousbread481.maltmix.model.Song;  
import com.google.gson.Gson;  
import com.google.gson.reflect.TypeToken;  
  
import java.lang.reflect.Type;  
import java.util.ArrayList;  
import java.util.List;  
  
public class PlaylistStore {  
  
    private static final String PREF_NAME = "playlist_store";  
    private static final String KEY_LIST = "list";  
    private static final String KEY_INDEX = "index";  
    private static final String KEY_MODE = "mode";  
  
    private static SharedPreferences prefs(Context ctx) {  
        return ctx.getApplicationContext()  
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);  
    }  
  
    public static void saveList(Context ctx, List<Song> list) {  
        List<Song> copy = new ArrayList<>();  
        if (list != null) {  
            for (Song s : list) {  
                copy.add(new Song(s.getId(), s.getTitle(), s.getArtist(),  
                        s.getCoverUrl(), null, s.getSource()));  
            }  
        }  
        prefs(ctx).edit()  
                .putString(KEY_LIST, new Gson().toJson(copy))  
                .apply();  
    }  
  
    public static List<Song> loadList(Context ctx) {  
        String json = prefs(ctx).getString(KEY_LIST, null);  
        if (json == null) return new ArrayList<>();  
        try {  
            Type t = new TypeToken<List<Song>>() {}.getType();  
            List<Song> list = new Gson().fromJson(json, t);  
            return list != null ? list : new ArrayList<>();  
        } catch (Exception e) {  
            return new ArrayList<>();  
        }  
    }  
  
    public static void saveIndex(Context ctx, int index) {  
        prefs(ctx).edit().putInt(KEY_INDEX, index).apply();  
    }  
  
    public static int loadIndex(Context ctx) {  
        return prefs(ctx).getInt(KEY_INDEX, -1);  
    }  
  
    public static void saveMode(Context ctx, int mode) {  
        prefs(ctx).edit().putInt(KEY_MODE, mode).apply();  
    }  
  
    public static int loadMode(Context ctx) {  
        return prefs(ctx).getInt(KEY_MODE, 0);  
    }  
}