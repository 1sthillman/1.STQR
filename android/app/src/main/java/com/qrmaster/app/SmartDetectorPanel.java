package com.qrmaster.app;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔍 SMART DETECTOR PANEL
 * 
 * Ekranı tarar ve kullanıcıya tüm elemanları gösterir
 * Kullanıcı hangi elemanları kullanmak istediğini seçebilir
 */
public class SmartDetectorPanel {
    
    private static final String TAG = "SmartDetector";
    
    private Context context;
    private List<DetectedElement> detectedElements = new ArrayList<>();
    private DetectionResultListener listener;
    
    public interface DetectionResultListener {
        void onElementSelected(DetectedElement element);
        void onScanComplete(List<DetectedElement> elements);
    }
    
    // ══════════════════════════════════════════════════════════════
    // DETECTED ELEMENT
    // ══════════════════════════════════════════════════════════════
    
    public static class DetectedElement {
        public String id;
        public ElementType type;
        public String text;
        public int x, y;
        public int width, height;
        public float confidence;
        public boolean isClickable;
        public boolean hasFlame;
        public String extraInfo;
        
        public enum ElementType {
            TEXT,           // 📝 Text element
            BUTTON,         // 🔘 Button
            TIME_SLOT,      // ⏰ Saat slot'u
            FLAME_SLOT,     // 🔥 Alevli slot
            IMAGE,          // 🖼️ Resim
            COUNTER,        // 🔢 Sayaç (5/12 gibi)
            UNKNOWN         // ❓ Bilinmeyen
        }
        
        @Override
        public String toString() {
            return String.format("%s: '%s' at (%d,%d) [%.0f%% confidence]", 
                type, text, x, y, confidence * 100);
        }
        
        public String getDisplayText() {
            String icon = getIcon();
            String posInfo = String.format("(%d,%d)", x, y);
            String confInfo = String.format("%.0f%%", confidence * 100);
            
            return String.format("%s %s\n%s | %s", icon, text, posInfo, confInfo);
        }
        
        public String getIcon() {
            switch (type) {
                case TEXT: return "📝";
                case BUTTON: return "🔘";
                case TIME_SLOT: return "⏰";
                case FLAME_SLOT: return "🔥";
                case IMAGE: return "🖼️";
                case COUNTER: return "🔢";
                default: return "❓";
            }
        }
    }
    
    public SmartDetectorPanel(Context context) {
        this.context = context;
    }
    
    public void setListener(DetectionResultListener listener) {
        this.listener = listener;
    }
    
    // ══════════════════════════════════════════════════════════════
    // SCAN SCREEN
    // ══════════════════════════════════════════════════════════════
    
    public List<DetectedElement> scanScreen() {
        detectedElements.clear();
        
        Log.d(TAG, "🔍 SCANNING SCREEN...");
        
        try {
            // Accessibility Service'ten view'ları al
            AutoClickerAccessibilityService service = AutoClickerAccessibilityService.getInstance();
            
            if (service == null) {
                Log.e(TAG, "❌ Accessibility service is null!");
                Toast.makeText(context, "⚠️ Accessibility servis aktif değil!", Toast.LENGTH_SHORT).show();
                return detectedElements;
            }
            
            List<AutoClickerAccessibilityService.ViewInfo> views = service.analyzeScreen();
            
            Log.d(TAG, "📊 Found " + views.size() + " views");
            
            // Her view'ı analiz et ve sınıflandır
            for (AutoClickerAccessibilityService.ViewInfo view : views) {
                DetectedElement element = analyzeView(view);
                if (element != null) {
                    detectedElements.add(element);
                }
            }
            
            Log.d(TAG, "✅ SCAN COMPLETE: " + detectedElements.size() + " elements detected");
            
            // Listener'a bildir
            if (listener != null) {
                listener.onScanComplete(detectedElements);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error scanning screen: " + e.getMessage());
            e.printStackTrace();
        }
        
        return detectedElements;
    }
    
    // ══════════════════════════════════════════════════════════════
    // ANALYZE VIEW
    // ══════════════════════════════════════════════════════════════
    
    private DetectedElement analyzeView(AutoClickerAccessibilityService.ViewInfo view) {
        DetectedElement element = new DetectedElement();
        element.id = "elem_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        element.text = view.text;
        element.x = view.x;
        element.y = view.y;
        element.width = view.width;
        element.height = view.height;
        
        // Tip tespiti
        if (view.containsFlameEmoji()) {
            element.type = DetectedElement.ElementType.FLAME_SLOT;
            element.hasFlame = true;
            element.confidence = 0.95f;
            element.isClickable = true;
            element.extraInfo = "Alevli rezervasyon slot'u";
            
        } else if (view.containsTime()) {
            element.type = DetectedElement.ElementType.TIME_SLOT;
            element.confidence = 0.85f;
            element.isClickable = true;
            element.extraInfo = "Saat slot'u";
            
        } else if (view.text.matches("\\d+/\\d+")) {
            element.type = DetectedElement.ElementType.COUNTER;
            element.confidence = 0.9f;
            element.isClickable = false;
            element.extraInfo = "Seçim sayacı";
            
        } else if (view.className.contains("Button") || view.text.contains("+") || view.text.contains("✕")) {
            element.type = DetectedElement.ElementType.BUTTON;
            element.confidence = 0.8f;
            element.isClickable = true;
            element.extraInfo = "Buton";
            
        } else if (view.className.contains("Image")) {
            element.type = DetectedElement.ElementType.IMAGE;
            element.confidence = 0.7f;
            element.isClickable = false;
            element.extraInfo = "Resim/İkon";
            
        } else if (!view.text.isEmpty()) {
            element.type = DetectedElement.ElementType.TEXT;
            element.confidence = 0.6f;
            element.isClickable = false;
            element.extraInfo = "Text elementi";
            
        } else {
            return null; // Skip empty views
        }
        
        Log.d(TAG, "✅ " + element.toString());
        
        return element;
    }
    
    // ══════════════════════════════════════════════════════════════
    // GET RESULTS
    // ══════════════════════════════════════════════════════════════
    
    public List<DetectedElement> getDetectedElements() {
        return detectedElements;
    }
    
    public List<DetectedElement> getFlameSlots() {
        List<DetectedElement> flames = new ArrayList<>();
        for (DetectedElement elem : detectedElements) {
            if (elem.type == DetectedElement.ElementType.FLAME_SLOT) {
                flames.add(elem);
            }
        }
        return flames;
    }
    
    public List<DetectedElement> getTimeSlots() {
        List<DetectedElement> times = new ArrayList<>();
        for (DetectedElement elem : detectedElements) {
            if (elem.type == DetectedElement.ElementType.TIME_SLOT || 
                elem.type == DetectedElement.ElementType.FLAME_SLOT) {
                times.add(elem);
            }
        }
        return times;
    }
    
    public List<DetectedElement> getButtons() {
        List<DetectedElement> buttons = new ArrayList<>();
        for (DetectedElement elem : detectedElements) {
            if (elem.type == DetectedElement.ElementType.BUTTON) {
                buttons.add(elem);
            }
        }
        return buttons;
    }
    
    // ══════════════════════════════════════════════════════════════
    // STATISTICS
    // ══════════════════════════════════════════════════════════════
    
    public String getStats() {
        int flames = 0, times = 0, buttons = 0, others = 0;
        
        for (DetectedElement elem : detectedElements) {
            switch (elem.type) {
                case FLAME_SLOT: flames++; break;
                case TIME_SLOT: times++; break;
                case BUTTON: buttons++; break;
                default: others++; break;
            }
        }
        
        return String.format("🔥 %d | ⏰ %d | 🔘 %d | 📝 %d", flames, times, buttons, others);
    }
}

