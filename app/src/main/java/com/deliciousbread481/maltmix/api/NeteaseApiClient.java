package com.deliciousbread481.maltmix.api;  
  
import android.os.Handler;  
import android.os.Looper;  
import android.util.Log;  
  
import com.deliciousbread481.maltmix.model.NeteasePlaylist;  
import com.deliciousbread481.maltmix.model.NeteaseSong;  
  
import org.json.JSONArray;  
import org.json.JSONObject;  
  
import java.io.IOException;  
import java.net.URLEncoder;  
import java.util.ArrayList;  
import java.util.LinkedHashMap;  
import java.util.List;  
import java.util.Map;  
import java.util.concurrent.TimeUnit;  
  
import okhttp3.Call;  
import okhttp3.Callback;  
import okhttp3.FormBody;  
import okhttp3.Interceptor;  
import okhttp3.OkHttpClient;  
import okhttp3.Request;  
import okhttp3.RequestBody;  
import okhttp3.Response;  
  
public class NeteaseApiClient {  
  
    private static final String TAG = "MaltMix";  
  
    // 读接口主域；失败时可用 interface3 兜底  
    private static final String API_URL = "https://interface.music.163.com";  
    private static final String API_URL_FALLBACK = "https://interface3.music.163.com";  
  
    private static final String USER_AGENT =  
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "  
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";  
    private static final String IOS_UA =  
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) "  
                    + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 "  
                    + "Mobile/15E148 Safari/604.1";  
    private static final String REFERER = "https://music.163.com/";  
  
    private static final OkHttpClient client = new OkHttpClient.Builder()  
            .connectTimeout(15, TimeUnit.SECONDS)  
            .readTimeout(30, TimeUnit.SECONDS)  
            .build();  
  
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());  
  
    private static String cookie = "";  
  
    public static void setCookie(String c) {  
        cookie = c == null ? "" : c;  
        Log.d(TAG, "已设置 eapi Cookie，长度 = " + cookie.length());  
    }  
  
    // ---------- 回调接口 ----------  
  
    public interface SongUrlCallback {  
        void onSuccess(String url);  
        void onFailure(String error);  
    }  
  
    public interface AccountCallback {  
        void onSuccess(long uid);  
        void onFailure(String error);  
    }  
  
    public interface PlaylistsCallback {  
        void onSuccess(List<NeteasePlaylist> playlists);  
        void onFailure(String error);  
    }  
  
    public interface PlaylistDetailCallback {  
        void onSuccess(List<NeteaseSong> songs);  
        void onFailure(String error);  
    }  
  
    // ---------- 账号 ----------  
  
    public static void getAccount(AccountCallback callback) {  
        Map<String, String> payload = new LinkedHashMap<>();  
        sendEapiRequest("/eapi/nuser/account/get", payload, new EapiResponseCallback() {  
            @Override  
            public void onSuccess(JSONObject json) {  
                JSONObject account = json.optJSONObject("account");  
                JSONObject profile = json.optJSONObject("profile");  
                JSONObject src = account != null ? account : profile;  
                if (src == null) {  
                    callback.onFailure("未登录或 Cookie 已失效\n\n原始响应：\n" + json);  
                    return;  
                }  
                long uid = src.optLong("id", src.optLong("userId", 0));  
                if (uid <= 0) {  
                    callback.onFailure("无法获取用户 ID\n\n原始响应：\n" + json);  
                    return;  
                }  
                callback.onSuccess(uid);  
            }  
  
            @Override  
            public void onFailure(String error) {  
                callback.onFailure(error);  
            }  
        });  
    }  
  
    // ---------- 用户歌单 ----------  
  
    public static void getUserPlaylists(long uid, PlaylistsCallback callback) {  
        Map<String, String> payload = new LinkedHashMap<>();  
        payload.put("uid", String.valueOf(uid));  
        payload.put("limit", "1000");  
        payload.put("offset", "0");  
        payload.put("includeVideo", "false");  
  
        sendEapiRequest("/eapi/user/playlist", payload, new EapiResponseCallback() {  
            @Override  
            public void onSuccess(JSONObject json) {  
                JSONArray array = json.optJSONArray("playlist");  
                if (array == null) {  
                    callback.onFailure("响应里没有 playlist 字段\n\n原始响应：\n" + json);  
                    return;  
                }  
                List<NeteasePlaylist> result = new ArrayList<>();  
                for (int i = 0; i < array.length(); i++) {  
                    JSONObject item = array.optJSONObject(i);  
                    if (item == null) continue;  
                    result.add(new NeteasePlaylist(  
                            item.optLong("id", 0),  
                            item.optString("name", "未命名歌单"),  
                            item.optInt("trackCount", 0),  
                            item.optString("coverImgUrl", "")));  
                }  
                callback.onSuccess(result);  
            }  
  
            @Override  
            public void onFailure(String error) {  
                callback.onFailure(error);  
            }  
        });  
    }  
  
    // ---------- 歌单详情 ----------  
  
    public static void getPlaylistDetail(long playlistId, PlaylistDetailCallback callback) {  
        Map<String, String> payload = new LinkedHashMap<>();  
        payload.put("id", String.valueOf(playlistId));  
        payload.put("n", "1000");  
        payload.put("s", "8");  
  
        sendEapiRequest("/eapi/v6/playlist/detail", payload, new EapiResponseCallback() {  
            @Override  
            public void onSuccess(JSONObject json) {  
                JSONObject playlist = json.optJSONObject("playlist");  
                if (playlist == null) {  
                    callback.onFailure("歌单不存在或无权访问\n\n原始响应：\n" + json);  
                    return;  
                }  
                JSONArray tracks = playlist.optJSONArray("tracks");  
                List<NeteaseSong> result = new ArrayList<>();  
                if (tracks != null) {  
                    for (int i = 0; i < tracks.length(); i++) {  
                        JSONObject track = tracks.optJSONObject(i);  
                        if (track == null) continue;  
                        String id = track.optString("id", "");  
                        String name = track.optString("name", "未知歌曲");  
                        String artist = "未知歌手";  
                        JSONArray ar = track.optJSONArray("ar");  
                        if (ar != null && ar.length() > 0) {  
                            artist = ar.optJSONObject(0).optString("name", "未知歌手");  
                        }  
                        String cover = "";  
                        JSONObject al = track.optJSONObject("al");  
                        if (al != null) cover = al.optString("picUrl", "");  
                        long duration = track.optLong("dt", 0);  
                        result.add(new NeteaseSong(id, name, artist, "", cover, duration));  
                    }  
                }  
                callback.onSuccess(result);  
            }  
  
            @Override  
            public void onFailure(String error) {  
                callback.onFailure(error);  
            }  
        });  
    }  
  
    // ---------- 歌曲播放地址 ----------  
  
    public static void getSongUrl(String songId, SongUrlCallback callback) {  
        Map<String, String> payload = new LinkedHashMap<>();  
        payload.put("ids", "[" + songId + "]");  
        payload.put("level", "exhigh");  
        payload.put("encodeType", "flac");  
  
        sendEapiRequest("/eapi/song/enhance/player/url/v1", payload,  
                new EapiResponseCallback() {  
            @Override  
            public void onSuccess(JSONObject json) {  
                JSONArray data = json.optJSONArray("data");  
                if (data == null || data.length() == 0) {  
                    callback.onFailure("无播放地址\n\n原始响应：\n" + json);  
                    return;  
                }  
                String url = data.optJSONObject(0).optString("url", "");  
                if (url.isEmpty()) {  
                    callback.onFailure("VIP歌曲需要登录后播放\n\n原始响应：\n" + json);  
                    return;  
                }  
                callback.onSuccess(url);  
            }  
  
            @Override  
            public void onFailure(String error) {  
                callback.onFailure(error);  
            }  
        });  
    }  
  
    // ---------- eapi 请求核心 ----------  
  
    private interface EapiResponseCallback {  
        void onSuccess(JSONObject json);  
        void onFailure(String error);  
    }  
  
    /**  
     * 对齐 Ncrust RetrofitClient.eapiPost：  
     * 直接把登录 Cookie 放 Cookie 头，payload 不做 header 注入。  
     * 空响应时自动用 interface3 域名重试一次。  
     */  
    private static void sendEapiRequest(String eapiPath, Map<String, String> payload,  
                                        EapiResponseCallback callback) {  
        doEapi(API_URL + eapiPath, payload, callback, true);  
    }  
  
    private static void doEapi(String url, Map<String, String> payload,  
                               EapiResponseCallback callback, boolean canRetry) {  
        final String params;  
        try {  
            params = EapiCrypto.encryptParams(url, payload);  
        } catch (Exception e) {  
            mainHandler.post(() -> callback.onFailure("加密失败：" + e.getMessage()));  
            return;  
        }  
  
        RequestBody formBody = new FormBody.Builder()  
                .add("params", params)  
                .build();  
  
        Request.Builder rb = new Request.Builder()  
                .url(url)  
                .post(formBody)  
                .header("User-Agent", USER_AGENT)  
                .header("Referer", REFERER);  
        if (!cookie.isEmpty()) rb.header("Cookie", cookie);  
  
        client.newCall(rb.build()).enqueue(new Callback() {  
            @Override  
            public void onFailure(Call call, IOException e) {  
                mainHandler.post(() -> callback.onFailure(  
                        "网络错误：" + e.getMessage() + "\nURL：" + url));  
            }  
  
            @Override  
            public void onResponse(Call call, Response response) {  
                String body = "";  
                try {  
                    body = response.body() != null ? response.body().string() : "";  
                } catch (Exception e) {  
                    Log.e(TAG, "读取 body 失败", e);  
                }  
  
                if (body.isEmpty() && canRetry) {  
                    // 换 interface3 域名重试一次  
                    String fallbackUrl = url.replace(API_URL, API_URL_FALLBACK);  
                    Log.w(TAG, "空响应，改用 " + fallbackUrl + " 重试");  
                    doEapi(fallbackUrl, payload, callback, false);  
                    return;  
                }  
  
                final String finalBody = body;  
                if (finalBody.isEmpty()) {  
                    String diag = "服务器返回空响应\n\n"  
                            + "【HTTP 状态码】" + response.code() + "\n"  
                            + "【请求 URL】" + url + "\n"  
                            + "【Cookie 长度】" + cookie.length() + "\n\n"  
                            + "已尝试主域+interface3 均空包，疑似账号风控或加密参数不被接受";  
                    mainHandler.post(() -> callback.onFailure(diag));  
                    return;  
                }  
  
                try {  
                    JSONObject json = new JSONObject(finalBody);  
                    mainHandler.post(() -> callback.onSuccess(json));  
                } catch (Exception e) {  
                    mainHandler.post(() -> callback.onFailure(  
                            "响应解析失败：" + e.getMessage() + "\n\n【原始响应】\n" + finalBody));  
                }  
            }  
        });  
    }  
  
    // ---------- 写接口专用（点赞/收藏等，对齐 eapiPostOfficial） ----------  
  
    /**  
     * 官方客户端指纹版：设备字段以 Cookie 形式发送，同时写入加密 body 的 header。  
     * 读接口不要用这个，仅写接口（如 /eapi/radio/like）需要。  
     */  
    private static void sendEapiRequestOfficial(String eapiPath, Map<String, String> payload,  
                                                EapiResponseCallback callback) {  
        try {  
            Map<String, String> mus = parseCookie(cookie);  
            Map<String, String> header = new LinkedHashMap<>();  
            header.put("os", "iphone");  
            header.put("appver", "8.9.60");  
            header.put("deviceId", mus.containsKey("deviceId")  
                    ? mus.get("deviceId") : randomHex(20));  
            header.put("osver", mus.containsKey("osver") ? mus.get("osver") : "16.0");  
            header.put("versioncode", "140");  
            header.put("mobilename", "");  
            header.put("buildver", String.valueOf(System.currentTimeMillis() / 1000));  
            header.put("resolution", "1920x1080");  
            header.put("__csrf", mus.containsKey("__csrf") ? mus.get("__csrf") : "");  
            header.put("channel", "yykj");  
            header.put("requestId",  
                    String.valueOf(20000000 + (int) (Math.random() * 10000000)));  
            if (mus.containsKey("MUSIC_U")) header.put("MUSIC_U", mus.get("MUSIC_U"));  
            if (mus.containsKey("MUSIC_A")) header.put("MUSIC_A", mus.get("MUSIC_A"));  
  
            // 设备字段以 URL 编码的 Cookie 串形式发送  
            StringBuilder cookieStr = new StringBuilder();  
            for (Map.Entry<String, String> e : header.entrySet()) {  
                if (cookieStr.length() > 0) cookieStr.append(";");  
                cookieStr.append(URLEncoder.encode(e.getKey(), "UTF-8"))  
                        .append("=")  
                        .append(URLEncoder.encode(e.getValue(), "UTF-8"));  
            }  
  
            // header 同时写入加密 payload  
            Map<String, String> data = new LinkedHashMap<>(payload);  
            data.put("header", new JSONObject(header).toString());  
  
            String url = API_URL + eapiPath;  
            String params = EapiCrypto.encryptParams(url, data);  
  
            Request request = new Request.Builder()  
                    .url(url)  
                    .post(new FormBody.Builder().add("params", params).build())  
                    .header("User-Agent", IOS_UA)  
                    .header("Referer", REFERER)  
                    .header("Cookie", cookieStr.toString())  
                    .build();  
  
            client.newCall(request).enqueue(new Callback() {  
                @Override  
                public void onFailure(Call call, IOException e) {  
                    mainHandler.post(() -> callback.onFailure("网络错误：" + e.getMessage()));  
                }  
  
                @Override  
                public void onResponse(Call call, Response response) throws IOException {  
                    String body = response.body() != null ? response.body().string() : "";  
                    try {  
                        JSONObject json = new JSONObject(body);  
                        mainHandler.post(() -> callback.onSuccess(json));  
                    } catch (Exception e) {  
                        String b = body;  
                        mainHandler.post(() -> callback.onFailure(  
                                "响应解析失败\n\n【原始响应】\n" + b));  
                    }  
                }  
            });  
        } catch (Exception e) {  
            mainHandler.post(() -> callback.onFailure("加密失败：" + e.getMessage()));  
        }  
    }  
  
    private static Map<String, String> parseCookie(String c) {  
        Map<String, String> map = new LinkedHashMap<>();  
        if (c == null) return map;  
        for (String part : c.split(";")) {  
            String t = part.trim();  
            int idx = t.indexOf('=');  
            if (idx > 0) map.put(t.substring(0, idx).trim(), t.substring(idx + 1));  
        }  
        return map;
    }  
  
    private static String randomHex(int len) {  
        String chars = "0123456789abcdef";  
        StringBuilder sb = new StringBuilder(len);  
        for (int i = 0; i < len; i++)  
            sb.append(chars.charAt((int) (Math.random() * 16)));  
        return sb.toString();  
    }  
}