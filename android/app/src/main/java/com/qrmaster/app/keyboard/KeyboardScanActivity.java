package com.qrmaster.app.keyboard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class KeyboardScanActivity extends Activity {
    private static final String TAG = "KeyboardScanActivity";
    public static final int SCAN_REQUEST_CODE = 12345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "KeyboardScanActivity açıldı");
        
        // Transparent theme olduğu için arka planda MainActivity görünür
        // Capacitor barcode scanning plugin'ini tetikle
        Intent intent = new Intent("com.qrmaster.app.SCAN_BARCODE_FROM_KEYBOARD");
        sendBroadcast(intent);
        
        // Activity'yi bekletiyoruz, sonuç gelince kapatacağız
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCAN_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                String result = data.getStringExtra("barcode");
                if (result != null && !result.isEmpty()) {
                    // Klavyeye broadcast gönder
                    Intent broadcast = new Intent("com.qrmaster.app.KEYBOARD_INSERT_TEXT");
                    broadcast.putExtra("text", result);
                    sendBroadcast(broadcast);
                }
            }
            finish();
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra("scan_result")) {
            String result = intent.getStringExtra("scan_result");
            Log.d(TAG, "Scan result alındı: " + result);
            
            if (result != null && !result.isEmpty()) {
                // Klavyeye broadcast gönder
                Intent broadcast = new Intent("com.qrmaster.app.KEYBOARD_INSERT_TEXT");
                broadcast.putExtra("text", result);
                sendBroadcast(broadcast);
            }
            finish();
        }
    }
}
