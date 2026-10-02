package com.deliciousbread481.maltmix.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.CookieManager;

public class BiliAuthManager {

    private static final String PREF_NAME = "bili_auth";
    private static final String KEY_SESSDATA = "sessdata";
    private static final String KEY_BILI_JCT = "bili_jct";
    private static final String KEY_IMG_KEY = "img_key";
    private static final String KEY_SUB_KEY = "sub_key";

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

    public String getImgKey() {
        return prefs.getString(KEY_IMG_KEY, null);
    }

    public String getSubKey() {
        return prefs.getString(KEY_SUB_KEY, null);
    }

    public void setWbiKeys(String imgKey, String subKey) {
        prefs.edit()
                .putString(KEY_IMG_KEY, imgKey)
                .putString(KEY_SUB_KEY, subKey)
                .apply();
    }

    public String getBuvid3() {
        CookieManager cm = CookieManager.getInstance();
        String cookies = cm.getCookie("https://www.bilibili.com");
        if (cookies == null) return null;
        return extractCookie(cookies, "buvid3");
    }

    public boolean isLoggedIn() {
        String s = getSessdata();
        return s != null && !s.isEmpty();
    }

    public String buildCookieHeader() {
        if (!isLoggedIn()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("SESSDATA=").append(getSessdata());
        if (getBiliJct() != null) {
            sb.append("; bili_jct=").append(getBiliJct());
        }
        String buvid3 = getBuvid3();
        if (buvid3 != null && !buvid3.isEmpty()) {
            sb.append("; buvid3=").append(buvid3);
        }
        return sb.toString();
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