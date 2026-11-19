package com.semijoias.app;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Hospeda o painel admin da loja em uma WebView, injeta os botões de compartilhamento
 * (assets/inject.js) e expõe a ponte nativa de compartilhamento ({@link ShareBridge}).
 */
public class MainActivity extends AppCompatActivity {

    private static final String BRIDGE_NAME = "AndroidShare";
    private static final long INJECT_DELAY_MS = 1500;

    private WebView webView;
    private ShareBridge shareBridge;
    private volatile boolean pageTrusted;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webView);

        configureSettings(webView.getSettings());

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        shareBridge = new ShareBridge(this, () -> pageTrusted);
        webView.addJavascriptInterface(shareBridge, BRIDGE_NAME);
        webView.setWebViewClient(new StoreWebViewClient(loadInjectScript()));

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

        webView.loadUrl(StoreConfig.START_URL);
    }

    @Override
    protected void onDestroy() {
        shareBridge.shutdown();
        webView.removeJavascriptInterface(BRIDGE_NAME);
        webView.destroy();
        super.onDestroy();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureSettings(WebSettings settings) {
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        // Força o layout mobile completo do painel com um User-Agent de Chrome mobile
        settings.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/120.0.0.0 Mobile Safari/537.36");
    }

    private String loadInjectScript() {
        try (InputStream in = getAssets().open("inject.js")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new IllegalStateException("inject.js ausente dos assets", e);
        }
    }

    private final class StoreWebViewClient extends WebViewClient {

        private final String injectScript;

        StoreWebViewClient(String injectScript) {
            this.injectScript = injectScript;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (StoreConfig.isTrusted(url)) {
                return false;
            }
            // WhatsApp e qualquer origem externa saem da WebView
            openExternally(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            pageTrusted = StoreConfig.isTrusted(url);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (StoreConfig.isTrusted(url)) {
                view.postDelayed(() -> view.evaluateJavascript(injectScript, null), INJECT_DELAY_MS);
            }
        }

        private void openExternally(String url) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (ActivityNotFoundException ignored) {
                // Nenhum app para tratar o link (ex.: WhatsApp não instalado)
            }
        }
    }
}
