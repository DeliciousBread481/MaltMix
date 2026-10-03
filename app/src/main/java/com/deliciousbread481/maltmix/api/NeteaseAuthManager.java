package com.deliciousbread481.maltmix.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.CookieManager;

public class NeteaseAuthManager {

    private static final String PREF_NAME = "netease_auth";
    private static final String KEY_COOKIE = "cookie";
    private static final String KEY_UID = "uid";

    private final SharedPreferences prefs;

    public NeteaseAuthManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveCookie(String cookie) {
        prefs.edit().putString(KEY_COOKIE, cookie).apply();
        NeteaseApiClient.setCookie(cookie);
    }

    public String getCookie() {
        return prefs.getString(KEY_COOKIE, "");
    }

    public void saveUid(String uid) {
        prefs.edit().putString(KEY_UID, uid).apply();
    }

    public String getUid() {
        return prefs.getString(KEY_UID, "");
    }

    public boolean isLoggedIn() {
        String cookie = getCookie();
        return cookie != null && !cookie.isEmpty();
    }

    /**
     * 退出登录：清空本地存储，同时清空 WebView CookieManager 里
     * music.163.com 域的 Cookie，避免下次打开登录页时被误判为已登录。
     */
    public void logout() {
        prefs.edit().clear().apply();
        NeteaseApiClient.setCookie("");
        clearWebViewCookies();
    }

    private void clearWebViewCookies() {
        CookieManager cm = CookieManager.getInstance();
        // 逐个删除 music.163.com 域下的关键 Cookie
        String[] keys = {"MUSIC_U", "__csrf", "NMTID", "__remember_me",
                "osver", "deviceId", "appver", "versioncode",
                "mobilename", "buildver", "resolution", "os",
                "channel", "requestId"};
        for (String key : keys) {
            cm.setCookie("https://music.163.com", key + "=");
        }
        cm.flush();
    }
}