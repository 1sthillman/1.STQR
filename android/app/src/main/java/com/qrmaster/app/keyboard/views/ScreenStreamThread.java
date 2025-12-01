package com.qrmaster.app.keyboard.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Screen streaming thread - MJPEG stream reader
 */
public class ScreenStreamThread extends Thread {
    private static final String TAG = "ScreenStreamThread";
    
    private final String streamUrl;
    private final ImageView targetImageView;
    private final Handler mainHandler;
    private volatile boolean running = false;
    
    public ScreenStreamThread(String serverIp, int httpPort, ImageView imageView) {
        this.streamUrl = "http://" + serverIp + ":" + httpPort + "/screen";
        this.targetImageView = imageView;
        this.mainHandler = new Handler(Looper.getMainLooper());
        setDaemon(true);
    }
    
    @Override
    public void run() {
        running = true;
        Log.d(TAG, "📺 Screen stream başlatıldı: " + streamUrl);
        
        mainHandler.post(() -> {
            android.widget.Toast.makeText(targetImageView.getContext(), 
                "🔄 Bağlanıyor: " + streamUrl, android.widget.Toast.LENGTH_SHORT).show();
        });
        
        try {
            URL url = new URL(streamUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            
            Log.d(TAG, "🔗 Bağlanıyor: " + streamUrl);
            conn.connect();
            
            int responseCode = conn.getResponseCode();
            Log.d(TAG, "📡 HTTP Response: " + responseCode);
            
            if (responseCode != 200) {
                Log.e(TAG, "❌ HTTP Error: " + responseCode + " - " + conn.getResponseMessage());
                mainHandler.post(() -> {
                    android.widget.Toast.makeText(targetImageView.getContext(), 
                        "❌ HTTP Error: " + responseCode, android.widget.Toast.LENGTH_LONG).show();
                });
                return;
            }
            
            InputStream inputStream = new BufferedInputStream(conn.getInputStream());
            Log.d(TAG, "✅ Stream bağlandı, veri alınıyor...");
            
            // MJPEG parser
            byte[] buffer = new byte[4096];
            int bytesRead;
            byte[] frameBuffer = new byte[1024 * 1024]; // 1MB max frame
            int frameSize = 0;
            boolean inFrame = false;
            
            while (running && (bytesRead = inputStream.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    if (!inFrame) {
                        // Look for JPEG start marker (FF D8)
                        if (buffer[i] == (byte)0xFF && i + 1 < bytesRead && buffer[i + 1] == (byte)0xD8) {
                            inFrame = true;
                            frameSize = 0;
                            frameBuffer[frameSize++] = buffer[i];
                            frameBuffer[frameSize++] = buffer[++i];
                        }
                    } else {
                        frameBuffer[frameSize++] = buffer[i];
                        
                        // Look for JPEG end marker (FF D9)
                        if (buffer[i] == (byte)0xFF && i + 1 < bytesRead && buffer[i + 1] == (byte)0xD9) {
                            frameBuffer[frameSize++] = buffer[++i];
                            inFrame = false;
                            
                            // Decode and display frame
                            final byte[] finalFrame = new byte[frameSize];
                            System.arraycopy(frameBuffer, 0, finalFrame, 0, frameSize);
                            
                            mainHandler.post(() -> {
                                try {
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(finalFrame, 0, finalFrame.length);
                                    if (bitmap != null) {
                                        targetImageView.setImageBitmap(bitmap);
                                        Log.d(TAG, "🖼️ Frame gösterildi: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                                    } else {
                                        Log.e(TAG, "❌ Bitmap null!");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Frame decode error", e);
                                }
                            });
                            
                            frameSize = 0;
                        }
                        
                        // Buffer overflow protection
                        if (frameSize >= frameBuffer.length - 10) {
                            inFrame = false;
                            frameSize = 0;
                        }
                    }
                }
            }
            
            inputStream.close();
            conn.disconnect();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Stream error: " + e.getMessage(), e);
            mainHandler.post(() -> {
                android.widget.Toast.makeText(targetImageView.getContext(), 
                    "❌ Stream hatası: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            });
        } finally {
            running = false;
            Log.d(TAG, "📺 Screen stream durduruldu");
        }
    }
    
    public void stopStream() {
        running = false;
        interrupt();
    }
    
    public boolean isRunning() {
        return running;
    }
}

