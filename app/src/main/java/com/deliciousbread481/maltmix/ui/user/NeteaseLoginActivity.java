package com.deliciousbread481.maltmix.ui.user;

import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.deliciousbread481.maltmix.api.NeteaseAuthManager;
import com.deliciousbread481.maltmix.util.LogDialog;

public class NeteaseLoginActivity extends AppCompatActivity {

    private WebView webView;
    private NeteaseAuthManager authManager;

    /** 官方客户端设备信息（模拟 Android 客户端）
    private static final String DEVICE_ID = "MDAwMDAwMDAwMDAwMDAwMA==\t02:00:00\t5106025eb79a5247\t70ffbaac7";
    private static final String OSVER = "10";
    private static final String APPVER = "9.1.65";
    private static final String VERSIONCODE = "140";
    private static final String MOBILENAME = "M2012K11AC";
    private static final String BUILDVER = "1690000000000";
    private static final String RESOLUTION = "1920x1080";
    private static final String OS = "android";
    private static final String CHANNEL = "netease";
    */

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = new NeteaseAuthManager(this);

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUserAgentString(
                "NeteaseMusic/9.1.65.240927161425 (800x1280;Android-30)");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // ★★★ 关键：在加载登录页之前，先注入官方客户端设备信息 Cookie ★★★
        //injectDeviceCookies();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                tryAutoSaveCookie();
            }
        });

        Button btnDone = new Button(this);
        btnDone.setText("我已完成登录");
        btnDone.setOnClickListener(v -> {
            if (saveCookieFromWebView()) {
                Toast.makeText(this, "网易云登录成功", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                LogDialog.error(this, "未检测到登录 Cookie\n\n"
                        + "请确认已登录，或换用手机号验证码登录");
            }
        });

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(webView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        layout.addView(btnDone);
        setContentView(layout);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        webView.loadUrl("https://music.163.com/login");
    }

    /**
     * 在加载登录页之前，通过 CookieManager 注入官方客户端设备信息。
     * 这一步让网易云服务器认为请求来自官方 Android 客户端。
     
    private void injectDeviceCookies() {
        CookieManager cm = CookieManager.getInstance();
        String domain = "https://music.163.com";

        // 注入 eapi 必需的设备信息字段
        cm.setCookie(domain, "osver=" + OSVER);
        cm.setCookie(domain, "deviceId=" + DEVICE_ID);
        cm.setCookie(domain, "appver=" + APPVER);
        cm.setCookie(domain, "versioncode=" + VERSIONCODE);
        cm.setCookie(domain, "mobilename=" + MOBILENAME);
        cm.setCookie(domain, "buildver=" + BUILDVER);
        cm.setCookie(domain, "resolution=" + RESOLUTION);
        cm.setCookie(domain, "os=" + OS);
        cm.setCookie(domain, "channel=" + CHANNEL);
        cm.setCookie(domain, "requestId=" + System.currentTimeMillis() + "_"
                + String.format("%04d", (int)(Math.random() * 10000)));

        // 确保 Cookie 生效
        cm.flush();
    }*/
    
    private void tryAutoSaveCookie() {  
        String cookies = CookieManager.getInstance().getCookie("https://music.163.com");  
        if (cookies != null && cookies.contains("MUSIC_U=")) {  
            if (saveCookieFromWebView()) {  
                Toast.makeText(this, "网易云登录成功", Toast.LENGTH_SHORT).show();  
                setResult(RESULT_OK);  
                finish();  
            }  
        }  
    }

    private boolean saveCookieFromWebView() {
        CookieManager cm = CookieManager.getInstance();
        String cookies = cm.getCookie("https://music.163.com");
        if (cookies == null || cookies.isEmpty()) return false;
        if (!cookies.contains("MUSIC_U=")) return false;

        // ★★★ 保存前清理网页端多余 Cookie，只保留 eapi 必需字段 ★★★
        String cleaned = cleanCookieForEapi(cookies);
        authManager.saveCookie(cleaned);
        return true;
    }

    /**
     * 清理 Cookie，只保留 eapi 接口必需的核心字段。
     * 网页版 Cookie 包含大量无关字段（HMACCOUNT、JSESSIONID 等），
     * 过长的 Cookie 会导致 eapi 接口拒绝服务。
     */
    private String cleanCookieForEapi(String rawCookie) {
        StringBuilder sb = new StringBuilder();
        String[] keepKeys = {
                "MUSIC_U", "__csrf", "osver", "deviceId", "appver",
                "versioncode", "mobilename", "buildver", "resolution",
                "os", "channel", "requestId", "NMTID", "__remember_me"
        };
    
        for (String part : rawCookie.split(";")) {
            String trimmed = part.trim();
            for (String key : keepKeys) {
                if (trimmed.startsWith(key + "=")) {
                    if (sb.length() > 0) sb.append("; ");
                    sb.append(trimmed);
                    break;
                }
            }
        }
        String result = sb.toString();
        return result.isEmpty() ? rawCookie : result;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }
}