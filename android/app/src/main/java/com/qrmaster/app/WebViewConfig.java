package com.qrmaster.app;

import android.webkit.WebView;
import android.webkit.WebSettings;
import com.getcapacitor.Bridge;
import com.getcapacitor.Plugin;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "WebViewConfig")
public class WebViewConfig extends Plugin {
    
    public static void configureWebView(Bridge bridge) {
        if (bridge != null && bridge.getWebView() != null) {
            WebView webView = bridge.getWebView();
            WebSettings settings = webView.getSettings();
            
            // Kamera ve medya erişimi için gerekli
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setMediaPlaybackRequiresUserGesture(false);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            
            // Hardware acceleration
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        }
    }
}









