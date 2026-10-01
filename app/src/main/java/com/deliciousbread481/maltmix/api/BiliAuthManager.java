package com.deliciousbread481.maltmix.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.CookieManager;

public class BiliAuthManager {

    private static final String PREF_NAME = "bili_auth";
    private static final String KEY_SESSDATA = "sessdata";
    private static final String KEY_BILI_JCT = "bili_jct";

    private final SharedPreferences prefs;

    public BiliAuthManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public boolean saveCookiesFromWebView() {
        CookieManager cm = CookieManager.getInstance();
        String cookies = cm.getCookie("https://www.bilibili.com");
        if (cookies == null) return false;

        String sessdata = extractCookie(cookies, "SESSDATA");
        String biliJct = extractCookie(cookies, "bili_jct");

        if (sessdata == null || sessdata.isEmpty()) return false;

        prefs.edit()
                .putString(KEY_SESSDATA, sessdata)
                .putString(KEY_BILI_JCT, biliJct != null ? biliJct : "")
                .apply();
        return true;
    }

    public String getSessdata() {
        return prefs.getString(KEY_SESSDATA, null);
    }

    public String getBiliJct() {
        return prefs.getString(KEY_BILI_JCT, null);
    }

    public boolean isLoggedIn() {
        String s = getSessdata();
        return s != null && !s.isEmpty();
    }

    public String buildCookieHeader() {
        if (!isLoggedIn()) return "";
        return "SESSDATA=" + getSessdata() + "; bili_jct=" + getBiliJct();
    }

    public void logout() {
        prefs.edit().clear().apply();
        CookieManager.getInstance().removeAllCookies(null);
    }

    private static String extractCookie(String cookies, String key) {
        for (String part : cookies.split(";")) {
            part = part.trim();
            if (part.startsWith(key + "=")) {
                return part.substring(key.length() + 1);
            }
        }
        return null;
    }
}