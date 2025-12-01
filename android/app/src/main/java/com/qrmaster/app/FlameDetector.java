package com.qrmaster.app;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🔥 ALEV TESPİT SİSTEMİ
 * Ekran görüntüsünden alevli saatleri bulur
 */
public class FlameDetector {
    
    private static final String TAG = "FlameDetector";
    
    // Renk eşikleri
    private static final int FLAME_RED_MIN = 200;
    private static final int FLAME_GREEN_MAX = 120;
    private static final int FLAME_BLUE_MAX = 60;
    
    private static final int GRAY_MAX = 180; // Soluk renk için
    
    public static class FlameSlot {
        public String time;
        public float x;
        public float y;
        public boolean isColorful;
        public int colorSample; // RGB değeri
        
        @Override
        public String toString() {
            return "FlameSlot{" +
                    "time='" + time + '\'' +
                    ", x=" + x +
                    ", y=" + y +
                    ", colorful=" + isColorful +
                    ", color=#" + Integer.toHexString(colorSample) +
                    '}';
        }
    }
    
    /**
     * 🔍 EKRANI TARA VE ALEVLİ SAATLERİ BUL
     */
    public static List<FlameSlot> detectFlameSlots(Bitmap screenshot) {
        List<FlameSlot> slots = new ArrayList<>();
        
        if (screenshot == null) {
            Log.e(TAG, "❌ Screenshot is null");
            return slots;
        }
        
        try {
            int width = screenshot.getWidth();
            int height = screenshot.getHeight();
            
            Log.d(TAG, "🔍 Scanning screenshot: " + width + "x" + height);
            
            // Liste alanını tara (orta kısım, üst header hariç)
            int startY = height / 4; // Üstten %25
            int endY = height * 3 / 4; // Alta kadar %75
            int leftX = 50; // Sol kenardan 50px
            
            // Her 100px'de bir satır tara
            for (int y = startY; y < endY; y += 100) {
                // Sol tarafta alev ikonu ara
                FlameSlot slot = scanRowForFlame(screenshot, leftX, y, width);
                if (slot != null) {
                    slots.add(slot);
                    Log.d(TAG, "🔥 Found flame slot: " + slot);
                }
            }
            
            Log.d(TAG, "📊 Total flames found: " + slots.size());
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error detecting flames: " + e.getMessage());
            e.printStackTrace();
        }
        
        return slots;
    }
    
    /**
     * Bir satırı tara
     */
    private static FlameSlot scanRowForFlame(Bitmap bitmap, int startX, int y, int width) {
        try {
            // Sol tarafta alev ikonu var mı kontrol et (turuncu/kırmızı pixel)
            boolean hasFlameIcon = false;
            int flameX = startX;
            
            for (int x = startX; x < startX + 100 && x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                
                if (isFlameColor(pixel)) {
                    hasFlameIcon = true;
                    flameX = x;
                    break;
                }
            }
            
            if (!hasFlameIcon) {
                return null;
            }
            
            // Renk kontrolü - renkli mi soluk mu?
            int colorSample = bitmap.getPixel(flameX, y);
            boolean isColorful = isColorfulFlame(colorSample);
            
            // SADECE RENKLİ ALEVLER
            if (!isColorful) {
                Log.d(TAG, "⚪ Skipping dim flame at y=" + y);
                return null;
            }
            
            // Saat metnini bul (alev ikonunun sağında)
            String time = extractTimeFromRow(bitmap, flameX + 50, y, width);
            
            if (time == null || time.isEmpty()) {
                return null;
            }
            
            // + butonu koordinatı (sağ tarafta)
            float plusButtonX = width - 80; // Sağdan 80px
            
            FlameSlot slot = new FlameSlot();
            slot.time = time;
            slot.x = plusButtonX;
            slot.y = y;
            slot.isColorful = isColorful;
            slot.colorSample = colorSample;
            
            return slot;
            
        } catch (Exception e) {
            Log.e(TAG, "Error scanning row: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 🎨 ALEV RENGİ KONTROLÜ
     */
    private static boolean isFlameColor(int pixel) {
        int red = Color.red(pixel);
        int green = Color.green(pixel);
        int blue = Color.blue(pixel);
        
        // Turuncu/Kırmızı ton kontrolü
        return red > 150 && green < 150 && blue < 100;
    }
    
    /**
     * 🔥 RENKLİ ALEV KONTROLÜ (SOLUK DEĞİL)
     */
    private static boolean isColorfulFlame(int pixel) {
        int red = Color.red(pixel);
        int green = Color.green(pixel);
        int blue = Color.blue(pixel);
        
        // Parlak turuncu: RGB(255, 107, 0) benzeri
        boolean isColorful = red >= FLAME_RED_MIN && 
                            green <= FLAME_GREEN_MAX && 
                            blue <= FLAME_BLUE_MAX;
        
        // Soluk gri kontrolü
        boolean isGray = red < GRAY_MAX && green < GRAY_MAX && blue < GRAY_MAX;
        
        Log.d(TAG, String.format("Color check: RGB(%d,%d,%d) -> colorful=%b, gray=%b", 
            red, green, blue, isColorful, isGray));
        
        return isColorful && !isGray;
    }
    
    /**
     * ⏰ SAAT METNİNİ BUL
     */
    private static String extractTimeFromRow(Bitmap bitmap, int startX, int y, int endX) {
        try {
            // Basit OCR: Koyu renkli piksellerin yoğun olduğu alanları bul
            // Gerçek OCR için Google ML Kit veya Tesseract kullanılabilir
            
            // Şimdilik: Metin olabilecek alanı tespit et
            // Gerçek implementasyonda ML Kit Text Recognition kullanılmalı
            
            // Mock: Piksel yoğunluğuna göre tahmin
            // Format: "11:30 - 12:00" veya benzeri
            
            // NOT: Bu basit versiyon. Gerçek OCR için ayrı kütüphane gerekli.
            
            return null; // Gerçek OCR implementasyonu gerekli
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 🎯 EN ERKEN SAATİ BUL
     */
    public static FlameSlot findEarliestSlot(List<FlameSlot> slots) {
        if (slots.isEmpty()) return null;
        
        FlameSlot earliest = null;
        int earliestMinutes = Integer.MAX_VALUE;
        
        for (FlameSlot slot : slots) {
            int minutes = parseTimeToMinutes(slot.time);
            if (minutes < earliestMinutes) {
                earliestMinutes = minutes;
                earliest = slot;
            }
        }
        
        return earliest;
    }
    
    private static int parseTimeToMinutes(String time) {
        try {
            Pattern pattern = Pattern.compile("(\\d+):(\\d+)");
            Matcher matcher = pattern.matcher(time);
            
            if (matcher.find()) {
                int hour = Integer.parseInt(matcher.group(1));
                int minute = Integer.parseInt(matcher.group(2));
                return hour * 60 + minute;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing time: " + e.getMessage());
        }
        
        return Integer.MAX_VALUE;
    }
}



























