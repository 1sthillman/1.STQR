package com.qrmaster.app;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🧠 AKILLI TESPİT MOTORU
 * AccessibilityService verilerini kullanarak GERÇEKTEN akıllı tespit yapar
 */
public class SmartDetectionEngine {
    
    private static final String TAG = "SmartDetection";
    
    // Öğrenme verileri
    private Map<String, DetectionPattern> patterns = new HashMap<>();
    private List<ScanResult> scanHistory = new ArrayList<>();
    
    // Adaptive parametreler
    private int successfulDetections = 0;
    private int failedDetections = 0;
    private long lastDetectionTime = 0;
    
    public static class FlameSlot {
        public String time;
        public int x, y;
        public float confidence;
        public boolean isFlameDetected;
        public String detectionMethod; // "VIEW_TEXT", "EMOJI", "POSITION", "LEARNED"
        
        @Override
        public String toString() {
            return String.format("FlameSlot{time=%s, pos=(%d,%d), conf=%.2f, method=%s}",
                time, x, y, confidence, detectionMethod);
        }
    }
    
    private static class DetectionPattern {
        public String timePattern;
        public int avgX, avgY;
        public int detectionCount;
        public int successCount;
        public long lastSeen;
        
        public float getSuccessRate() {
            return detectionCount > 0 ? (float)successCount / detectionCount : 0f;
        }
    }
    
    private static class ScanResult {
        public long timestamp;
        public int flamesFound;
        public List<FlameSlot> slots;
    }
    
    /**
     * 🔍 EKRANI AKILLI TARA
     */
    public List<FlameSlot> detectFlameSlots(List<AutoClickerAccessibilityService.ViewInfo> views) {
        List<FlameSlot> detectedSlots = new ArrayList<>();
        
        if (views == null || views.isEmpty()) {
            Log.w(TAG, "⚠️ No views available, using fallback detection");
            return useFallbackDetection();
        }
        
        Log.d(TAG, "🧠 SMART DETECTION: Analyzing " + views.size() + " views");
        
        // 1. EMOJI TABLI TESPİT (En güvenilir)
        detectedSlots.addAll(detectByEmoji(views));
        
        // 2. SAAT METNİ TABLI TESPİT
        detectedSlots.addAll(detectByTimeText(views));
        
        // 3. POZİSYON TABLI TESPİT (Öğrenilen patternler)
        detectedSlots.addAll(detectByLearnedPatterns(views));
        
        // 4. DUPLİKALARI KALDIR
        List<FlameSlot> uniqueSlots = removeDuplicates(detectedSlots);
        
        Log.d(TAG, "📊 DETECTION RESULT: " + uniqueSlots.size() + " unique flame slots");
        
        // Sonuçları kaydet (öğren)
        saveDetectionResult(uniqueSlots);
        
        return uniqueSlots;
    }
    
    /**
     * 🔥 EMOJI İLE TESPİT
     */
    private List<FlameSlot> detectByEmoji(List<AutoClickerAccessibilityService.ViewInfo> views) {
        List<FlameSlot> slots = new ArrayList<>();
        
        for (AutoClickerAccessibilityService.ViewInfo view : views) {
            if (view.containsFlameEmoji() && view.containsTime()) {
                String time = extractTime(view.text);
                
                if (time != null) {
                    FlameSlot slot = new FlameSlot();
                    slot.time = time;
                    slot.x = view.x;
                    slot.y = view.y;
                    slot.confidence = 0.95f; // Çok güvenilir
                    slot.isFlameDetected = true;
                    slot.detectionMethod = "EMOJI";
                    
                    slots.add(slot);
                    Log.d(TAG, "🔥 EMOJI DETECTION: " + slot);
                }
            }
        }
        
        return slots;
    }
    
    /**
     * ⏰ SAAT METNİ İLE TESPİT
     */
    private List<FlameSlot> detectByTimeText(List<AutoClickerAccessibilityService.ViewInfo> views) {
        List<FlameSlot> slots = new ArrayList<>();
        
        for (AutoClickerAccessibilityService.ViewInfo view : views) {
            if (view.containsTime()) {
                String time = extractTime(view.text);
                
                if (time != null && isValidTimeRange(time)) {
                    // Sağ tarafta + butonu olup olmadığını kontrol et
                    boolean hasPlusButton = checkForPlusButton(views, view.y);
                    
                    if (hasPlusButton) {
                        FlameSlot slot = new FlameSlot();
                        slot.time = time;
                        slot.x = view.x + 300; // + buton yaklaşık sağda
                        slot.y = view.y;
                        slot.confidence = 0.75f;
                        slot.isFlameDetected = true;
                        slot.detectionMethod = "TIME_TEXT";
                        
                        slots.add(slot);
                        Log.d(TAG, "⏰ TIME DETECTION: " + slot);
                    }
                }
            }
        }
        
        return slots;
    }
    
    /**
     * 🧠 ÖĞRENİLEN PATTERNLER İLE TESPİT
     */
    private List<FlameSlot> detectByLearnedPatterns(List<AutoClickerAccessibilityService.ViewInfo> views) {
        List<FlameSlot> slots = new ArrayList<>();
        
        for (DetectionPattern pattern : patterns.values()) {
            // Başarı oranı yüksek patternleri kullan
            if (pattern.getSuccessRate() > 0.6f && 
                System.currentTimeMillis() - pattern.lastSeen < 3600000) { // 1 saat içinde
                
                // Bu pozisyonda hala view var mı?
                boolean stillValid = checkPatternValidity(views, pattern);
                
                if (stillValid) {
                    FlameSlot slot = new FlameSlot();
                    slot.time = pattern.timePattern;
                    slot.x = pattern.avgX;
                    slot.y = pattern.avgY;
                    slot.confidence = pattern.getSuccessRate() * 0.7f; // Penalty for learned
                    slot.isFlameDetected = true;
                    slot.detectionMethod = "LEARNED";
                    
                    slots.add(slot);
                    Log.d(TAG, "🧠 LEARNED PATTERN: " + slot);
                }
            }
        }
        
        return slots;
    }
    
    /**
     * ⏰ SAAT METNİNİ ÇIKAR
     */
    private String extractTime(String text) {
        // Pattern: "11:30 - 12:00" veya "11:30" gibi
        Pattern pattern = Pattern.compile("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(0); // Tam eşleşme
        }
        
        // Tek saat pattern
        pattern = Pattern.compile("\\d{1,2}:\\d{2}");
        matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(0);
        }
        
        return null;
    }
    
    /**
     * ✅ GEÇERLİ SAAT ARALIĞI MI?
     */
    private boolean isValidTimeRange(String time) {
        // 11:00 - 23:00 arası geçerli
        try {
            Pattern pattern = Pattern.compile("(\\d{1,2}):(\\d{2})");
            Matcher matcher = pattern.matcher(time);
            
            if (matcher.find()) {
                int hour = Integer.parseInt(matcher.group(1));
                return hour >= 11 && hour <= 23;
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return false;
    }
    
    /**
     * ➕ + BUTONU VAR MI?
     */
    private boolean checkForPlusButton(List<AutoClickerAccessibilityService.ViewInfo> views, int targetY) {
        for (AutoClickerAccessibilityService.ViewInfo view : views) {
            // Aynı satırda (±50px) ve "+" içeren view
            if (Math.abs(view.y - targetY) < 50 && 
                (view.text.contains("+") || view.description.contains("plus") || 
                 view.description.contains("add"))) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 🔄 DUPLİKALARI KALDIR
     */
    private List<FlameSlot> removeDuplicates(List<FlameSlot> slots) {
        List<FlameSlot> unique = new ArrayList<>();
        
        for (FlameSlot slot : slots) {
            boolean isDuplicate = false;
            
            for (FlameSlot existing : unique) {
                // Aynı pozisyon (±100px) veya aynı saat
                if ((Math.abs(slot.x - existing.x) < 100 && Math.abs(slot.y - existing.y) < 100) ||
                    (slot.time != null && slot.time.equals(existing.time))) {
                    
                    isDuplicate = true;
                    
                    // Daha yüksek confidence olanı tut
                    if (slot.confidence > existing.confidence) {
                        unique.remove(existing);
                        unique.add(slot);
                    }
                    
                    break;
                }
            }
            
            if (!isDuplicate) {
                unique.add(slot);
            }
        }
        
        return unique;
    }
    
    /**
     * 💾 TESPİT SONUCUNU KAYDET (Öğren)
     */
    private void saveDetectionResult(List<FlameSlot> slots) {
        ScanResult result = new ScanResult();
        result.timestamp = System.currentTimeMillis();
        result.flamesFound = slots.size();
        result.slots = slots;
        
        scanHistory.add(result);
        
        // Son 50 taramayı tut
        if (scanHistory.size() > 50) {
            scanHistory.remove(0);
        }
        
        // Patternleri güncelle
        for (FlameSlot slot : slots) {
            String key = slot.time + "_" + (slot.y / 100) * 100;
            
            DetectionPattern pattern = patterns.get(key);
            if (pattern == null) {
                pattern = new DetectionPattern();
                pattern.timePattern = slot.time;
                pattern.avgX = slot.x;
                pattern.avgY = slot.y;
                patterns.put(key, pattern);
            }
            
            pattern.detectionCount++;
            pattern.lastSeen = System.currentTimeMillis();
            
            // Ortalama pozisyon güncelle
            pattern.avgX = (pattern.avgX + slot.x) / 2;
            pattern.avgY = (pattern.avgY + slot.y) / 2;
        }
        
        lastDetectionTime = System.currentTimeMillis();
    }
    
    /**
     * ✅ BAŞARI BİLDİR
     */
    public void reportSuccess(FlameSlot slot) {
        successfulDetections++;
        
        String key = slot.time + "_" + (slot.y / 100) * 100;
        DetectionPattern pattern = patterns.get(key);
        
        if (pattern != null) {
            pattern.successCount++;
        }
        
        Log.d(TAG, "✅ SUCCESS REPORTED: " + slot.time + " (Total: " + successfulDetections + ")");
    }
    
    /**
     * ❌ BAŞARISIZLIK BİLDİR
     */
    public void reportFailure(FlameSlot slot) {
        failedDetections++;
        
        Log.d(TAG, "❌ FAILURE REPORTED: " + slot.time + " (Total: " + failedDetections + ")");
    }
    
    /**
     * 📊 İSTATİSTİKLER
     */
    public String getStats() {
        float successRate = (successfulDetections + failedDetections > 0)
            ? (float)successfulDetections / (successfulDetections + failedDetections) * 100
            : 0;
            
        return String.format("Detections: %d✅ %d❌ | Rate: %.1f%% | Patterns: %d | History: %d",
            successfulDetections, failedDetections, successRate, patterns.size(), scanHistory.size());
    }
    
    /**
     * 🔄 FALLBACK TESPİT (View yoksa)
     */
    private List<FlameSlot> useFallbackDetection() {
        List<FlameSlot> slots = new ArrayList<>();
        
        // Öğrenilen en başarılı patternleri kullan
        for (DetectionPattern pattern : patterns.values()) {
            if (pattern.getSuccessRate() > 0.7f) {
                FlameSlot slot = new FlameSlot();
                slot.time = pattern.timePattern;
                slot.x = pattern.avgX;
                slot.y = pattern.avgY;
                slot.confidence = pattern.getSuccessRate() * 0.5f;
                slot.detectionMethod = "FALLBACK";
                
                slots.add(slot);
            }
        }
        
        Log.d(TAG, "🔄 FALLBACK: Using " + slots.size() + " learned patterns");
        
        return slots;
    }
    
    private boolean checkPatternValidity(List<AutoClickerAccessibilityService.ViewInfo> views, DetectionPattern pattern) {
        // Bu pozisyonda hala view var mı kontrol et
        for (AutoClickerAccessibilityService.ViewInfo view : views) {
            if (Math.abs(view.y - pattern.avgY) < 100 && view.containsTime()) {
                return true;
            }
        }
        
        return false;
    }
}



























