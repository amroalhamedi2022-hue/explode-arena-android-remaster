package com.explodearena.remaster;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceResponse;
import android.net.Uri;
import java.io.ByteArrayInputStream;
import androidx.webkit.WebViewAssetLoader;

/** Offline WebView container for the continuously maintained game mainline.
 * The actual original-game mechanics are NOT yet reproduced.
 * No internet, remote permissions, login or externally loaded HTML required. */
public class MainActivity extends Activity {
    private WebView webView;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setDomStorageEnabled(true); // Local game preferences only
        webView.getSettings().setMediaPlaybackRequiresUserGesture(true);
        webView.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        webView.setBackgroundColor(android.graphics.Color.BLACK);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                Uri uri = request.getUrl();
                return !"https".equals(uri.getScheme()) || !"appassets.androidplatform.net".equals(uri.getHost());
            }

            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(
                    WebView view, android.webkit.WebResourceRequest request) {
                Uri uri=request.getUrl();
                if (!"https".equals(uri.getScheme()) || !"appassets.androidplatform.net".equals(uri.getHost())) {
                    return new WebResourceResponse("text/plain", "UTF-8", 403, "Blocked",
                        java.util.Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));
                }
                WebResourceResponse response = loader.shouldInterceptRequest(uri);
                // Do not attempt a network fetch when a local asset does not exist.
                if (response != null) return response;
                return new WebResourceResponse("text/plain", "UTF-8", 404, "Asset not found",
                    java.util.Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));
            }
        });
        setContentView(webView);
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }
    @Override protected void onPause() { if(webView!=null){webView.evaluateJavascript("window.EAApp && window.EAApp.pause()",null);webView.onPause();} super.onPause(); }
    @Override protected void onResume() { super.onResume(); if(webView!=null) webView.onResume(); }
    @Override public void onBackPressed() { if(webView!=null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { if(webView!=null){webView.destroy();webView=null;} super.onDestroy(); }
}
