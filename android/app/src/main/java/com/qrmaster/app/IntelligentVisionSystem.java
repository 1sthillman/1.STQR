package com.qrmaster.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🧠 AKILLI GÖRÜŞ SİSTEMİ
 * Ekranı görür, analiz eder, öğrenir ve adapte olur
 */
public class IntelligentVisionSystem {
    
    private static final String TAG = "IntelligentVision";
    
    private Context context;
    private ScreenCaptureService screenCapture;
    
    // Learning data
    private Map<String, FlamePattern> learnedPatterns = new HashMap<>();
    private List<BookingAttempt> attemptHistory = new ArrayList<>();
    private int successCount = 0;
    private int failCount = 0;
    
    // Adaptive parameters
    private int currentScrollSpeed = 400;
    private int currentWaitTime = 800;
    private float confidenceThreshold = 0.7f;
    
    public static class FlamePattern {
        public int x, y;
        public int avgRed, avgGreen, avgBlue;
        public String timeText;
        public boolean wasSuccessful;
        public long timestamp;
        public float confidence;
        
        @Override
        public String toString() {
            return String.format("Pattern{pos=(%d,%d), RGB=(%d,%d,%d), time=%s, success=%b, conf=%.2f}",
                x, y, avgRed, avgGreen, avgBlue, timeText, wasSuccessful, confidence);
        }
    }
    
    public static class BookingAttempt {
        public long timestamp;
        public FlamePattern pattern;
        public boolean success;
        public String errorReason;
    }
    
    public static class FlameSlot {
        public float x, y;
        public String time;
        public int colorRed, colorGreen, colorBlue;
        public float confidence; // 0.0 - 1.0
        public boolean isColorful;
        
        @Override
        public String toString() {
            return String.format("Slot{time=%s, pos=(%.0f,%.0f), RGB=(%d,%d,%d), colorful=%b, conf=%.2f}",
                time, x, y, colorRed, colorGreen, colorBlue, isColorful, confidence);
        }
    }
    
    public IntelligentVisionSystem(Context context) {
        this.context = context;
        this.screenCapture = new ScreenCaptureService(context);
        Log.d(TAG, "🧠 Intelligent Vision System initialized");
    }
    
    /**
     * 🎥 EKRAN GÖRÜNTÜSÜ AL
     */
    public Bitmap captureScreen() {
        final Bitmap[] result = {null};
        
        screenCapture.captureScreen(new ScreenCaptureService.ScreenCaptureCallback() {
            @Override
            public void onScreenCaptured(Bitmap bitmap) {
                result[0] = bitmap;
                Log.d(TAG, "📸 Screen captured: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Screen capture error: " + error);
            }
        });
        
        return result[0];
    }
    
    /**
     * 🔍 ALEVLİ SAATLERİ AKILLI TARA
     */
    public List<FlameSlot> scanForFlameSlots(Bitmap screenshot) {
        List<FlameSlot> slots = new ArrayList<>();
        
        if (screenshot == null) {
            Log.w(TAG, "⚠️ No screenshot available, using coordinate-based detection");
            return scanWithoutScreenshot();
        }
        
        try {
            int width = screenshot.getWidth();
            int height = screenshot.getHeight();
            
            Log.d(TAG, "🔍 Intelligent scan: " + width + "x" + height);
            
            // Liste alanını tara (üstten %20 - %85)
            int startY = (int)(height * 0.20f);
            int endY = (int)(height * 0.85f);
            int leftMargin = 40;
            
            // Her 100-150px'de bir satır tara (liste item yüksekliği)
            for (int y = startY; y < endY; y += 120) {
                // Sol tarafta alev ikonu ara (turuncu/kırmızı)
                FlameSlot slot = analyzeRowForFlame(screenshot, leftMargin, y, width);
                
                if (slot != null && slot.confidence > confidenceThreshold) {
                    slots.add(slot);
                    Log.d(TAG, "🔥 FOUND: " + slot);
                    
                    // Öğren
                    learnFromDetection(slot);
                }
            }
            
            Log.d(TAG, "📊 Total flames detected: " + slots.size());
            
            // Adaptive learning
            if (slots.size() == 0 && failCount > 2) {
                adjustDetectionParameters();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error scanning: " + e.getMessage());
            e.printStackTrace();
        }
        
        return slots;
    }
    
    /**
     * 🎨 BİR SATIRI ANALİZ ET
     */
    private FlameSlot analyzeRowForFlame(Bitmap screenshot, int startX, int y, int width) {
        try {
            // Sol tarafta turuncu/kırmızı piksel ara (100px genişlik)
            int flameX = -1;
            int totalRed = 0, totalGreen = 0, totalBlue = 0;
            int sampleCount = 0;
            
            for (int x = startX; x < startX + 100 && x < width; x += 5) {
                if (y >= screenshot.getHeight()) continue;
                
                int pixel = screenshot.getPixel(x, y);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                
                // Turuncu/Kırmızı alev tonu kontrolü
                if (isFlameColor(red, green, blue)) {
                    flameX = x;
                    totalRed += red;
                    totalGreen += green;
                    totalBlue += blue;
                    sampleCount++;
                }
            }
            
            if (flameX == -1 || sampleCount == 0) {
                return null; // Alev yok
            }
            
            // Ortalama renk
            int avgRed = totalRed / sampleCount;
            int avgGreen = totalGreen / sampleCount;
            int avgBlue = totalBlue / sampleCount;
            
            // RENKLİ mi SOLUK mu?
            boolean isColorful = isColorfulFlame(avgRed, avgGreen, avgBlue);
            
            if (!isColorful) {
                Log.d(TAG, String.format("⚪ DIM flame at y=%d RGB=(%d,%d,%d)", y, avgRed, avgGreen, avgBlue));
                return null; // Soluk alevleri ATLA
            }
            
            // Saat metnini tahmin et (OCR olmadan basit)
            String timeText = extractTimeFromRow(screenshot, flameX + 80, y, width);
            
            // Confidence hesapla
            float confidence = calculateConfidence(avgRed, avgGreen, avgBlue, isColorful);
            
            // Slot oluştur
            FlameSlot slot = new FlameSlot();
            slot.x = width - 80; // + butonu sağ tarafta
            slot.y = y;
            slot.time = timeText;
            slot.colorRed = avgRed;
            slot.colorGreen = avgGreen;
            slot.colorBlue = avgBlue;
            slot.isColorful = isColorful;
            slot.confidence = confidence;
            
            return slot;
            
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing row: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 🎨 ALEV RENGİ Mİ?
     */
    private boolean isFlameColor(int red, int green, int blue) {
        // Turuncu/Kırmızı ton: Yüksek kırmızı, orta yeşil, düşük mavi
        return red > 150 && green < 180 && blue < 100 && red > green;
    }
    
    /**
     * 🔥 RENKLİ ALEV Mİ? (SOLUK DEĞİL)
     */
    private boolean isColorfulFlame(int red, int green, int blue) {
        // Parlak turuncu: RGB(255, 107, 0) benzeri
        // Soluk: Daha gri tonlar
        
        boolean isVibrant = red >= 200 && green <= 120 && blue <= 60;
        boolean notGray = Math.abs(red - green) > 80 || Math.abs(red - blue) > 140;
        
        return isVibrant && notGray;
    }
    
    /**
     * 📈 GÜVENİRLİK HESAPLA
     */
    private float calculateConfidence(int red, int green, int blue, boolean isColorful) {
        float confidence = 0.5f;
        
        // Renk canlılığı
        if (red >= 220) confidence += 0.2f;
        if (green <= 100) confidence += 0.1f;
        if (blue <= 50) confidence += 0.1f;
        
        // Renkli ise bonus
        if (isColorful) confidence += 0.2f;
        
        // Learned patterns ile karşılaştır
        for (FlamePattern pattern : learnedPatterns.values()) {
            int colorDiff = Math.abs(pattern.avgRed - red) + 
                           Math.abs(pattern.avgGreen - green) + 
                           Math.abs(pattern.avgBlue - blue);
            
            if (colorDiff < 50 && pattern.wasSuccessful) {
                confidence += 0.1f;
            }
        }
        
        return Math.min(confidence, 1.0f);
    }
    
    /**
     * ⏰ SAAT METNİNİ BUL (Basit versiyon)
     */
    private String extractTimeFromRow(Bitmap screenshot, int startX, int y, int endX) {
        // Basit: Pozisyon bazlı tahmin
        // Gerçek implementasyonda Google ML Kit Text Recognition kullanılabilir
        
        // Mock: Y pozisyonundan tahmin et
        int index = y / 120;
        int hour = 11 + (index % 8);
        int minute = (index % 2) * 30;
        
        return String.format("%02d:%02d - %02d:%02d", hour, minute, hour, minute + 30);
    }
    
    /**
     * 🧠 TESPİTTEN ÖĞREN
     */
    private void learnFromDetection(FlameSlot slot) {
        FlamePattern pattern = new FlamePattern();
        pattern.x = (int)slot.x;
        pattern.y = (int)slot.y;
        pattern.avgRed = slot.colorRed;
        pattern.avgGreen = slot.colorGreen;
        pattern.avgBlue = slot.colorBlue;
        pattern.timeText = slot.time;
        pattern.confidence = slot.confidence;
        pattern.timestamp = System.currentTimeMillis();
        pattern.wasSuccessful = true; // Başlangıçta optimist
        
        String key = slot.time + "_" + (int)slot.y;
        learnedPatterns.put(key, pattern);
        
        Log.d(TAG, "🧠 LEARNED: " + pattern);
    }
    
    /**
     * ✅ BAŞARIDAN ÖĞREN
     */
    public void learnFromSuccess(FlameSlot slot) {
        successCount++;
        
        String key = slot.time + "_" + (int)slot.y;
        FlamePattern pattern = learnedPatterns.get(key);
        if (pattern != null) {
            pattern.wasSuccessful = true;
            pattern.confidence = Math.min(pattern.confidence + 0.1f, 1.0f);
        }
        
        BookingAttempt attempt = new BookingAttempt();
        attempt.timestamp = System.currentTimeMillis();
        attempt.pattern = pattern;
        attempt.success = true;
        attemptHistory.add(attempt);
        
        Log.d(TAG, "✅ SUCCESS LEARNED! Total: " + successCount);
        
        // Adaptive: Başarı oranı yüksekse hızlandır
        if (successCount > failCount * 2) {
            currentScrollSpeed = Math.max(300, currentScrollSpeed - 50);
            currentWaitTime = Math.max(500, currentWaitTime - 100);
            Log.d(TAG, "⚡ OPTIMIZING: speed=" + currentScrollSpeed + "ms, wait=" + currentWaitTime + "ms");
        }
    }
    
    /**
     * ❌ BAŞARISIZLIKTAN ÖĞREN
     */
    public void learnFromFailure(FlameSlot slot, String reason) {
        failCount++;
        
        String key = slot.time + "_" + (int)slot.y;
        FlamePattern pattern = learnedPatterns.get(key);
        if (pattern != null) {
            pattern.wasSuccessful = false;
            pattern.confidence = Math.max(pattern.confidence - 0.2f, 0.1f);
        }
        
        BookingAttempt attempt = new BookingAttempt();
        attempt.timestamp = System.currentTimeMillis();
        attempt.pattern = pattern;
        attempt.success = false;
        attempt.errorReason = reason;
        attemptHistory.add(attempt);
        
        Log.d(TAG, "❌ FAILURE LEARNED: " + reason + " Total: " + failCount);
        
        // Adaptive: Başarısızlık fazlaysa yavaşla ve daha dikkatli ol
        if (failCount > 3) {
            adjustDetectionParameters();
        }
    }
    
    /**
     * 🎯 PARAMETRELERİ AYARLA (Adaptive)
     */
    private void adjustDetectionParameters() {
        currentScrollSpeed += 100; // Daha yavaş scroll
        currentWaitTime += 200; // Daha fazla bekle
        confidenceThreshold = Math.max(0.5f, confidenceThreshold - 0.1f); // Daha esnek
        
        Log.d(TAG, "🎯 ADJUSTING: speed=" + currentScrollSpeed + "ms, wait=" + currentWaitTime + "ms, threshold=" + confidenceThreshold);
    }
    
    /**
     * 📊 İSTATİSTİKLER
     */
    public String getStats() {
        float successRate = (successCount + failCount > 0) 
            ? (float)successCount / (successCount + failCount) * 100 
            : 0;
            
        return String.format("Success: %d | Fail: %d | Rate: %.1f%% | Patterns: %d | Speed: %dms",
            successCount, failCount, successRate, learnedPatterns.size(), currentScrollSpeed);
    }
    
    /**
     * 📸 SCREENSHOT OLMADAN TARA (Fallback)
     */
    private List<FlameSlot> scanWithoutScreenshot() {
        List<FlameSlot> slots = new ArrayList<>();
        
        Log.d(TAG, "⚠️ Using fallback detection (no screenshot)");
        
        // Learned patterns kullan
        for (FlamePattern pattern : learnedPatterns.values()) {
            if (pattern.wasSuccessful && pattern.confidence > 0.6f) {
                FlameSlot slot = new FlameSlot();
                slot.x = pattern.x;
                slot.y = pattern.y;
                slot.time = pattern.timeText;
                slot.colorRed = pattern.avgRed;
                slot.colorGreen = pattern.avgGreen;
                slot.colorBlue = pattern.avgBlue;
                slot.isColorful = true;
                slot.confidence = pattern.confidence * 0.7f; // Penalty for no screenshot
                
                slots.add(slot);
            }
        }
        
        return slots;
    }
    
    public int getCurrentScrollSpeed() {
        return currentScrollSpeed;
    }
    
    public int getCurrentWaitTime() {
        return currentWaitTime;
    }
    
    public void startProjection(int resultCode, android.content.Intent data) {
        if (screenCapture != null) {
            screenCapture.startProjection(resultCode, data);
        }
    }
    
    public void stop() {
        if (screenCapture != null) {
            screenCapture.stop();
        }
    }
}



























