package com.deliciousbread481.maltmix.api;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BiliApi {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String REFERER = "https://www.bilibili.com/";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // ---------- 回调接口 ----------

    public interface VideoInfoCallback {
        void onSuccess(String title, String cover, long cid);
        void onFailure(String error);
    }

    public interface AudioUrlCallback {
        void onSuccess(String audioUrl);
        void onFailure(String error);
    }

    public interface UserInfoCallback {
        void onSuccess(long mid, String uname, String face);
        void onFailure(String error);
    }

    public interface FavFolderCallback {
        void onSuccess(java.util.List<com.deliciousbread481.maltmix.model.FavFolder> folders);
        void onFailure(String error);
    }

    public interface FavVideoCallback {
        void onSuccess(java.util.List<com.deliciousbread481.maltmix.model.FavVideo> videos);
        void onFailure(String error);
    }

    // ---------- 普通请求（无 Cookie） ----------

    public static void fetchVideoInfo(String bvid, VideoInfoCallback callback) {
        String url = "https://api.bilibili.com/x/web-interface/view?bvid=" + bvid;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Referer", REFERER)
                .addHeader("User-Agent", UA)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure("网络错误：" + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONObject json = new JSONObject(body);
                    int code = json.getInt("code");
                    if (code != 0) {
                        String msg = json.optString("message", "未知错误");
                        mainHandler.post(() -> callback.onFailure(
                                "B站返回 code=" + code + " message=" + msg
                                        + "\n原始响应：" + body));
                        return;
                    }
                    JSONObject data = json.getJSONObject("data");
                    String title = data.getString("title");
                    String cover = data.getString("pic");
                    long cid = data.getLong("cid");
                    mainHandler.post(() -> callback.onSuccess(title, cover, cid));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure(
                            "JSON 解析异常：" + e.getMessage() + "\n原始响应：\n" + body));
                }
            }
        });
    }

    public static void fetchAudioUrl(String bvid, long cid, AudioUrlCallback callback) {
        String url = "https://api.bilibili.com/x/player/playurl"
                + "?bvid=" + bvid
                + "&cid=" + cid
                + "&fnval=16&fnver=0&fourk=1";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Referer", REFERER)
                .addHeader("User-Agent", UA)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure("网络错误：" + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONObject json = new JSONObject(body);
                    int code = json.getInt("code");
                    if (code != 0) {
                        String msg = json.optString("message", "未知错误");
                        mainHandler.post(() -> callback.onFailure(
                                "B站返回 code=" + code + " message=" + msg
                                        + "\n原始响应：" + body));
                        return;
                    }
                    JSONObject dash = json.getJSONObject("data").getJSONObject("dash");
                    JSONArray audioArray = dash.getJSONArray("audio");
                    if (audioArray.length() == 0) {
                        mainHandler.post(() -> callback.onFailure(
                                "dash.audio 为空，原始响应：\n" + body));
                        return;
                    }
                    String audioUrl = audioArray.getJSONObject(0).getString("baseUrl");
                    mainHandler.post(() -> callback.onSuccess(audioUrl));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure(
                            "JSON 解析异常：" + e.getMessage() + "\n原始响应：\n" + body));
                }
            }
        });
    }

    // ---------- 带 Cookie 的请求 ----------

    public static void fetchUserInfo(String sessdata, UserInfoCallback callback) {
        String url = "https://api.bilibili.com/x/web-interface/nav";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Referer", REFERER)
                .addHeader("User-Agent", UA)
                .addHeader("Cookie", "SESSDATA=" + sessdata)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure("网络错误：" + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONObject json = new JSONObject(body);
                    int code = json.getInt("code");
                    if (code != 0) {
                        String msg = json.optString("message", "未知错误");
                        mainHandler.post(() -> callback.onFailure(
                                "B站返回 code=" + code + " message=" + msg
                                        + "\n原始响应：" + body));
                        return;
                    }
                    JSONObject data = json.getJSONObject("data");
                    if (!data.optBoolean("isLogin", false)) {
                        mainHandler.post(() -> callback.onFailure(
                                "Cookie 已失效，请重新登录"));
                        return;
                    }
                    long mid = data.getLong("mid");
                    String uname = data.getString("uname");
                    String face = data.optString("face", "");
                    mainHandler.post(() -> callback.onSuccess(mid, uname, face));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure(
                            "JSON 解析异常：" + e.getMessage() + "\n原始响应：\n" + body));
                }
            }
        });
    }

    public static void fetchFavFolders(long mid, String sessdata, FavFolderCallback callback) {
        String url = "https://api.bilibili.com/x/v3/fav/folder/created/list-all"
                + "?up_mid=" + mid;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Referer", REFERER)
                .addHeader("User-Agent", UA)
                .addHeader("Cookie", "SESSDATA=" + sessdata)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure("网络错误：" + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONObject json = new JSONObject(body);
                    int code = json.getInt("code");
                    if (code != 0) {
                        String msg = json.optString("message", "未知错误");
                        mainHandler.post(() -> callback.onFailure(
                                "B站返回 code=" + code + " message=" + msg
                                        + "\n原始响应：" + body));
                        return;
                    }
                    JSONObject data = json.getJSONObject("data");
                    JSONArray list = data.optJSONArray("list");
                    java.util.List<com.deliciousbread481.maltmix.model.FavFolder> result
                            = new java.util.ArrayList<>();
                    if (list != null) {
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject item = list.getJSONObject(i);
                            long id = item.getLong("id");
                            String title = item.getString("title");
                            int count = item.optInt("media_count", 0);
                            result.add(new com.deliciousbread481.maltmix.model.FavFolder(
                                    id, title, count));
                        }
                    }
                    mainHandler.post(() -> callback.onSuccess(result));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure(
                            "JSON 解析异常：" + e.getMessage() + "\n原始响应：\n" + body));
                }
            }
        });
    }

    public static void fetchFavVideos(long mediaId, String sessdata, FavVideoCallback callback) {
        String url = "https://api.bilibili.com/x/v3/fav/resource/list"
                + "?media_id=" + mediaId
                + "&pn=1&ps=20&order=mtime&type=0&platform=web";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Referer", REFERER)
                .addHeader("User-Agent", UA)
                .addHeader("Cookie", "SESSDATA=" + sessdata)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure("网络错误：" + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONObject json = new JSONObject(body);
                    int code = json.getInt("code");
                    if (code != 0) {
                        String msg = json.optString("message", "未知错误");
                        mainHandler.post(() -> callback.onFailure(
                                "B站返回 code=" + code + " message=" + msg
                                        + "\n原始响应：" + body));
                        return;
                    }
                    JSONObject data = json.getJSONObject("data");
                    JSONArray medias = data.optJSONArray("medias");
                    java.util.List<com.deliciousbread481.maltmix.model.FavVideo> result
                            = new java.util.ArrayList<>();
                    if (medias != null) {
                        for (int i = 0; i < medias.length(); i++) {
                            JSONObject item = medias.getJSONObject(i);
                            String bvid = item.optString("bvid", "");
                            long cid = item.optLong("cid", 0);
                            String title = item.optString("title", "未知标题");
                            String cover = item.optString("cover", "");
                            String upper = item.optJSONObject("upper") != null
                                    ? item.getJSONObject("upper").optString("name", "未知UP")
                                    : "未知UP";
                            int duration = item.optInt("duration", 0);
                            result.add(new com.deliciousbread481.maltmix.model.FavVideo(
                                    bvid, cid, title, cover, upper, duration));
                        }
                    }
                    mainHandler.post(() -> callback.onSuccess(result));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure(
                            "JSON 解析异常：" + e.getMessage() + "\n原始响应：\n" + body));
                }
            }
        });
    }
}