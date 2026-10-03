package com.deliciousbread481.maltmix.api;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final int[] MIXIN_KEY_ENC_TAB = {
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
            37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
            22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    };

    public interface VideoInfoCallback {
        void onSuccess(String title, String cover, long cid);
        void onFailure(String error);
    }

    public interface AudioUrlCallback {
        void onSuccess(String audioUrl);
        void onFailure(String error);
    }

    public interface UserInfoCallback {
        void onSuccess(long mid, String uname, String face,
                       String imgKey, String subKey);
        void onFailure(String error);
    }

    public interface FavFolderCallback {
        void onSuccess(List<com.deliciousbread481.maltmix.model.FavFolder> folders);
        void onFailure(String error);
    }

    public interface FavVideoCallback {
        void onSuccess(List<com.deliciousbread481.maltmix.model.FavVideo> videos);
        void onFailure(String error);
    }

    // ---------- WBI 签名工具 ----------

    private static String getMixinKey(String imgKey, String subKey) {
        String raw = imgKey + subKey;
        StringBuilder sb = new StringBuilder();
        for (int i : MIXIN_KEY_ENC_TAB) {
            if (i < raw.length()) {
                sb.append(raw.charAt(i));
            }
        }
        return sb.substring(0, Math.min(32, sb.length()));
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String wbiSign(Map<String, String> params,
                                  String imgKey, String subKey) {
        String mixinKey = getMixinKey(imgKey, subKey);
        params.put("wts", String.valueOf(System.currentTimeMillis() / 1000));

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder query = new StringBuilder();
        for (String key : keys) {
            String value = params.get(key);
            if (value == null) continue;
            value = value.replaceAll("[!'()*]", "");
            try {
                String encoded = URLEncoder.encode(value, "UTF-8")
                        .replace("+", "%20");
                if (query.length() > 0) query.append("&");
                query.append(key).append("=").append(encoded);
            } catch (UnsupportedEncodingException ignored) {
            }
        }

        String wRid = md5(query.toString() + mixinKey);
        return query.toString() + "&w_rid=" + wRid;
    }

    private static String extractWbiKey(String url) {
        if (url == null || url.isEmpty()) return "";
        int lastSlash = url.lastIndexOf('/');
        int lastDot = url.lastIndexOf('.');
        if (lastSlash < 0 || lastDot < 0 || lastDot <= lastSlash) return "";
        return url.substring(lastSlash + 1, lastDot);
    }

    /** 通过 bvid 获取视频信息 */
    public static void fetchVideoInfo(String bvid, String sessdata,
                                      String imgKey, String subKey,
                                      VideoInfoCallback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("bvid", bvid);

        String query;
        if (imgKey != null && subKey != null
                && !imgKey.isEmpty() && !subKey.isEmpty()) {
            query = wbiSign(params, imgKey, subKey);
        } else {
            query = "bvid=" + bvid;
        }

        final String url =
                "https://api.bilibili.com/x/web-interface/wbi/view?" + query;

        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("Referer", REFERER)
                .addHeader("User-Agent", UA);
        if (sessdata != null && !sessdata.isEmpty()) {
            builder.addHeader("Cookie", "SESSDATA=" + sessdata);
        }
        Request request = builder.build();

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
                                        + "\n\n【请求 URL】\n" + url
                                        + "\n\n【原始响应】\n" + body));
                        return;
                    }
                    JSONObject data = json.getJSONObject("data");
                    String title = data.getString("title");
                    String cover = data.getString("pic");

                    long cid = data.optLong("cid", 0);
                    if (cid <= 0) {
                        JSONArray pages = data.optJSONArray("pages");
                        if (pages != null && pages.length() > 0) {
                            cid = pages.getJSONObject(0).optLong("cid", 0);
                        }
                    }

                    if (cid <= 0) {
                        mainHandler.post(() -> callback.onFailure(
                                "view 接口返回 cid=0"
                                        + "\n\n【请求 URL】\n" + url
                                        + "\n\n【原始响应】\n" + body));
                        return;
                    }

                    final long finalCid = cid;
                    mainHandler.post(() -> callback.onSuccess(title, cover, finalCid));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure(
                            "JSON 解析异常：" + e.getMessage()
                                    + "\n\n【请求 URL】\n" + url
                                    + "\n\n【原始响应】\n" + body));
                }
            }
        });
    }

    /** 通过 bvid + cid 获取音频流 URL */
    public static void fetchAudioUrl(String bvid, long cid,
                                     String sessdata,
                                     String imgKey, String subKey,
                                     String buvid3,
                                     AudioUrlCallback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("bvid", bvid);
        params.put("cid", String.valueOf(cid));
        params.put("fnval", "16");
        params.put("fnver", "0");
        params.put("fourk", "1");

        String query;
        if (imgKey != null && subKey != null
                && !imgKey.isEmpty() && !subKey.isEmpty()) {
            query = wbiSign(params, imgKey, subKey);
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (sb.length() > 0) sb.append("&");
                sb.append(e.getKey()).append("=").append(e.getValue());
            }
            query = sb.toString();
        }

        final String url =
                "https://api.bilibili.com/x/player/wbi/playurl?" + query;

        StringBuilder cookieSb = new StringBuilder();
        if (sessdata != null && !sessdata.isEmpty()) {
            cookieSb.append("SESSDATA=").append(sessdata);
        }
        if (buvid3 != null && !buvid3.isEmpty()) {
            if (cookieSb.length() > 0) cookieSb.append("; ");
            cookieSb.append("buvid3=").append(buvid3);
        }

        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("Referer", REFERER)
                .addHeader("User-Agent", UA);
        if (cookieSb.length() > 0) {
            builder.addHeader("Cookie", cookieSb.toString());
        }
        Request request = builder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure(
                        "网络错误：" + e.getMessage()
                                + "\n\n【请求 URL】\n" + url));
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
                                        + "\n\n【请求 URL】\n" + url
                                        + "\n\n【原始响应】\n" + body));
                        return;
                    }
                    JSONObject dash = json.getJSONObject("data")
                            .getJSONObject("dash");
                    JSONArray audioArray = dash.getJSONArray("audio");
                    if (audioArray.length() == 0) {
                        mainHandler.post(() -> callback.onFailure(
                                "dash.audio 为空"
                                        + "\n\n【请求 URL】\n" + url
                                        + "\n\n【原始响应】\n" + body));
                        return;
                    }
                    String audioUrl = audioArray.getJSONObject(0)
                            .getString("baseUrl");
                    mainHandler.post(() -> callback.onSuccess(audioUrl));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure(
                            "JSON 解析异常：" + e.getMessage()
                                    + "\n\n【请求 URL】\n" + url
                                    + "\n\n【原始响应】\n" + body));
                }
            }
        });
    }

    /** 获取当前登录用户信息 */
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

                    String imgKey = "";
                    String subKey = "";
                    JSONObject wbiImg = data.optJSONObject("wbi_img");
                    if (wbiImg != null) {
                        imgKey = extractWbiKey(wbiImg.optString("img_url", ""));
                        subKey = extractWbiKey(wbiImg.optString("sub_url", ""));
                    }

                    final String finalImgKey = imgKey;
                    final String finalSubKey = subKey;
                    mainHandler.post(() -> callback.onSuccess(
                            mid, uname, face, finalImgKey, finalSubKey));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure(
                            "JSON 解析异常：" + e.getMessage() + "\n原始响应：\n" + body));
                }
            }
        });
    }

    public static void fetchFavFolders(long mid, String sessdata,
                                       FavFolderCallback callback) {
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
                    List<com.deliciousbread481.maltmix.model.FavFolder> result
                            = new ArrayList<>();
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

    public static void fetchFavVideos(long mediaId, String sessdata,  
                                      FavVideoCallback callback) {  
        fetchFavVideosPage(mediaId, sessdata, 1, new ArrayList<>(), callback);  
    }  
  
    private static void fetchFavVideosPage(long mediaId, String sessdata,  
                                           int pn,  
                                           List<com.deliciousbread481.maltmix.model.FavVideo> accumulated,  
                                           FavVideoCallback callback) {  
        String url = "https://api.bilibili.com/x/v3/fav/resource/list"  
                + "?media_id=" + mediaId  
                + "&pn=" + pn + "&ps=20&order=mtime&type=0&platform=web";  
        Request request = new Request.Builder()  
                .url(url)  
                .addHeader("Referer", REFERER)  
                .addHeader("User-Agent", UA)  
                .addHeader("Cookie", "SESSDATA=" + sessdata)  
                .build();  
  
        client.newCall(request).enqueue(new Callback() {  
            @Override  
            public void onFailure(Call call, IOException e) {  
                mainHandler.post(() -> {  
                    if (accumulated.isEmpty()) {  
                        callback.onFailure("网络错误：" + e.getMessage());  
                    } else {  
                        callback.onSuccess(accumulated);  
                    }  
                });  
            }  
  
            @Override  
            public void onResponse(Call call, Response response) throws IOException {  
                String body = response.body().string();  
                try {  
                    JSONObject json = new JSONObject(body);  
                    int code = json.getInt("code");  
                    if (code != 0) {  
                        String msg = json.optString("message", "未知错误");  
                        mainHandler.post(() -> {  
                            if (accumulated.isEmpty()) {  
                                callback.onFailure("B站返回 code=" + code  
                                        + " message=" + msg + "\n原始响应：" + body);  
                            } else {  
                                callback.onSuccess(accumulated);  
                            }  
                        });  
                        return;  
                    }  
                    JSONObject data = json.getJSONObject("data");  
                    JSONArray medias = data.optJSONArray("medias");  
                    boolean hasMore = data.optBoolean("has_more", false);  
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
                            accumulated.add(new com.deliciousbread481.maltmix.model.FavVideo(  
                                    bvid, cid, title, cover, upper, duration));  
                        }  
                    }  
                    if (hasMore && medias != null && medias.length() > 0) {  
                        fetchFavVideosPage(mediaId, sessdata, pn + 1, accumulated, callback);  
                    } else {  
                        mainHandler.post(() -> callback.onSuccess(accumulated));  
                    }  
                } catch (Exception e) {  
                    mainHandler.post(() -> {  
                        if (accumulated.isEmpty()) {  
                            callback.onFailure("JSON 解析异常：" + e.getMessage()  
                                    + "\n原始响应：\n" + body);  
                        } else {  
                            callback.onSuccess(accumulated);  
                        }  
                    });  
                }  
            }  
        });  
    }
}