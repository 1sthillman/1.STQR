package com.qrmaster.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.nio.ByteBuffer;

/**
 * 📸 EKRAN YAKALAMA SERVİSİ
 * Gerçek zamanlı ekran görüntüsü alır
 */
public class ScreenCaptureService {
    
    private static final String TAG = "ScreenCapture";
    
    private Context context;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;
    
    private Handler handler;
    
    public interface ScreenCaptureCallback {
        void onScreenCaptured(Bitmap bitmap);
        void onError(String error);
    }
    
    public ScreenCaptureService(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;
        
        projectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        
        Log.d(TAG, "✅ Screen dimensions: " + screenWidth + "x" + screenHeight + " @ " + screenDensity + " dpi");
    }
    
    /**
     * MediaProjection başlat
     */
    public void startProjection(int resultCode, Intent data) {
        try {
            if (mediaProjection != null) {
                Log.w(TAG, "⚠️ Projection already started");
                return;
            }
            
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            
            if (mediaProjection == null) {
                Log.e(TAG, "❌ Failed to create MediaProjection");
                return;
            }
            
            // ImageReader oluştur
            imageReader = ImageReader.newInstance(
                screenWidth, 
                screenHeight, 
                PixelFormat.RGBA_8888, 
                2
            );
            
            // VirtualDisplay oluştur
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                handler
            );
            
            Log.d(TAG, "✅ MediaProjection started");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error starting projection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ekran görüntüsü al
     */
    public void captureScreen(ScreenCaptureCallback callback) {
        if (imageReader == null) {
            callback.onError("ImageReader not initialized");
            return;
        }
        
        try {
            Image image = imageReader.acquireLatestImage();
            
            if (image == null) {
                callback.onError("Failed to acquire image");
                return;
            }
            
            // Image'i Bitmap'e çevir
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * screenWidth;
            
            Bitmap bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            );
            bitmap.copyPixelsFromBuffer(buffer);
            
            image.close();
            
            // Crop extra padding
            if (rowPadding > 0) {
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight);
            }
            
            callback.onScreenCaptured(bitmap);
            
            Log.d(TAG, "✅ Screen captured: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error capturing screen: " + e.getMessage());
            e.printStackTrace();
            callback.onError(e.getMessage());
        }
    }
    
    /**
     * Durdur
     */
    public void stop() {
        try {
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
            
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
            
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            
            Log.d(TAG, "✅ ScreenCapture stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping: " + e.getMessage());
        }
    }
    
    public int getScreenWidth() {
        return screenWidth;
    }
    
    public int getScreenHeight() {
        return screenHeight;
    }
}



























