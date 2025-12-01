package com.qrmaster.app;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

/**
 * 🔥 EMERGENCY: Mock Gaze Processor (MediaPipe YOK!)
 * Sadece crash testi için - MediaPipe'sız çalışır
 */
public class MockGazeProcessor implements ImageAnalysis.Analyzer {
    
    private static final String TAG = "MockGazeProcessor";
    
    public interface GazeCallback {
        void onGazeUpdate(float x, float y);
        void onBlink();
        void onFPSUpdate(int fps);
    }
    
    private final GazeCallback callback;
    private int frameCount = 0;
    private float mockX = 0.5f;
    private float mockY = 0.5f;
    
    public MockGazeProcessor(Context context, GazeCallback callback) {
        this.callback = callback;
        Log.i(TAG, "✅ MockGazeProcessor (MediaPipe YOK - TEST MODE)");
    }
    
    public boolean isReady() {
        return true; // Her zaman hazır
    }
    
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        try {
            frameCount++;
            
            // Mock gaze data (yavaşça hareket eden)
            mockX = 0.5f + (float) Math.sin(frameCount * 0.05) * 0.3f;
            mockY = 0.5f + (float) Math.cos(frameCount * 0.05) * 0.3f;
            
            // Her 30 frame'de callback
            if (frameCount % 30 == 0) {
                if (callback != null) {
                    callback.onGazeUpdate(mockX, mockY);
                    callback.onFPSUpdate(30);
                }
            }
            
            // Her 180 frame'de (6 saniye) mock blink
            if (frameCount % 180 == 0) {
                if (callback != null) {
                    callback.onBlink();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Mock error: " + e.getMessage());
        } finally {
            imageProxy.close();
        }
    }
    
    public void close() {
        Log.i(TAG, "MockGazeProcessor closed");
    }
}





























