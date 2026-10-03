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
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "  
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "  
                        + "Chrome/120.0.0.0 Safari/537.36");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

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

        String cleaned = cleanCookieForEapi(cookies);
        authManager.saveCookie(cleaned);
        return true;
    }

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