package com.qrmaster.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.PermissionRequest;
import android.webkit.WebSettings;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebChromeClient;

import com.qrmaster.app.keyboard.KeyboardManagerPlugin;
import com.qrmaster.app.keyboard.KeyboardScanActivity;

public class MainActivity extends BridgeActivity {
    
    private static final String TAG = "QRMasterApp";
    
    // Klavyeden QR tarama isteği için broadcast receiver
    private final BroadcastReceiver keyboardScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Klavyeden QR tarama isteği alındı!");
            startBarcodeScanFromKeyboard();
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        android.util.Log.i(TAG, "========================================");
        android.util.Log.i(TAG, "🚀 MAINACTIVITY onCreate -> PLUGIN REGISTRATION BAŞLIYOR");
        android.util.Log.i(TAG, "========================================");

        try {
            registerPlugin(EyeTrackerPluginStub.class);
            android.util.Log.i(TAG, "✅ EyeTrackerPluginStub registered");
            
            registerPlugin(FloatingQRPlugin.class);
            android.util.Log.i(TAG, "✅ FloatingQRPlugin registered");
            
            registerPlugin(OCRPlugin.class);
            android.util.Log.i(TAG, "✅ OCRPlugin registered");
            
            // AutoClicker - STUB OLARAK AKTİF (hata vermez, sadece false döner)
            registerPlugin(AutoClickerPlugin.class);
            android.util.Log.i(TAG, "✅ AutoClickerPlugin registered (STUB MODE)");

            registerPlugin(KeyboardManagerPlugin.class);
            android.util.Log.i(TAG, "✅ KeyboardManager registered");
            
            android.util.Log.i(TAG, "========================================");
            android.util.Log.i(TAG, "✅ ALL PLUGINS REGISTERED SUCCESSFULLY");
            android.util.Log.i(TAG, "========================================");
        } catch (Exception e) {
            android.util.Log.e(TAG, "❌ PLUGIN REGISTRATION ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        super.onCreate(savedInstanceState);

        configureWebViewPermissions();
        
        // Klavye broadcast receiver'ını kaydet
        IntentFilter filter = new IntentFilter("com.qrmaster.app.SCAN_BARCODE_FROM_KEYBOARD");
        registerReceiver(keyboardScanReceiver, filter);

        android.util.Log.i(TAG, "========================================");
        android.util.Log.i(TAG, "✅ MAINACTIVITY ONCREATE TAMAMLANDI");
        android.util.Log.i(TAG, "========================================");
    }
    
    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(keyboardScanReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Receiver unregister error", e);
        }
        super.onDestroy();
    }
    
    private void startBarcodeScanFromKeyboard() {
        Log.d(TAG, "Klavyeden QR tarama başlatılıyor...");
        
        // JavaScript'e QR tarama başlatması için mesaj gönder
        runOnUiThread(() -> {
            try {
                getBridge().eval("window.startKeyboardQRScan && window.startKeyboardQRScan()", null);
            } catch (Exception e) {
                Log.e(TAG, "QR tarama başlatma hatası", e);
            }
        });
    }

    private void configureWebViewPermissions() {
        if (getBridge() == null || getBridge().getWebView() == null) {
            Log.w(TAG, "⚠️ Bridge veya WebView hazır değil, kamera izin yapılandırması atlandı.");
            return;
        }

        var webView = getBridge().getWebView();
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        webView.setWebChromeClient(new BridgeWebChromeClient(getBridge()) {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                Log.d(TAG, "🌐 WebView izin isteği: " + java.util.Arrays.toString(request.getResources()));
                runOnUiThread(() -> {
                    try {
                        String[] resources = request.getResources();
                        if (resources == null || resources.length == 0) {
                            request.deny();
                            Log.w(TAG, "⚠️ WebView izni reddedildi: kaynak listesi boş");
                            return;
                        }

                        request.grant(resources);
                        Log.d(TAG, "✅ WebView izinleri verildi: " + java.util.Arrays.toString(resources));
                    } catch (Exception e) {
                        Log.e(TAG, "❌ WebView izni verilirken hata: " + e.getMessage());
                        request.deny();
                    }
                });
            }
        });
    }
}





