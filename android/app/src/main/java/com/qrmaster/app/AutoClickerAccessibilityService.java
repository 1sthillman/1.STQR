package com.qrmaster.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

/**
 * ✅ AUTO CLICKER - Accessibility Service
 * 
 * ÖZELLİKLER:
 * - Tap (tek/çift/üçlü)
 * - Swipe (yukarı/aşağı/sağa/sola)
 * - Long press
 * - Multi-point taps (çoklu nokta)
 * - Gesture recording & replay
 */
public class AutoClickerAccessibilityService extends AccessibilityService {
    
    private static final String TAG = "AutoClickerAccess";
    private static AutoClickerAccessibilityService instance;
    
    // ═══════════════════════════════════════════════════════════════
    // 📌 INSTANCE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════
    
    public static AutoClickerAccessibilityService getInstance() {
        return instance;
    }
    
    /**
     * ✅ Erişilebilirlik servisinin aktif olup olmadığını kontrol et
     */
    public static boolean isServiceEnabled(Context context) {
        // ✅ DÜZGÜN ACCESSIBILITY KONTROLÜ
        try {
            int accessibilityEnabled = 0;
            final String service = context.getPackageName() + "/" + AutoClickerAccessibilityService.class.getCanonicalName();
            
            try {
                accessibilityEnabled = Settings.Secure.getInt(
                    context.getApplicationContext().getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
                );
            } catch (Settings.SettingNotFoundException e) {
                Log.d(TAG, "Accessibility setting not found");
            }
            
            if (accessibilityEnabled == 1) {
                String settingValue = Settings.Secure.getString(
                    context.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                );
                
                if (settingValue != null) {
                    TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
                    splitter.setString(settingValue);
                    
                    while (splitter.hasNext()) {
                        String accessibilityService = splitter.next();
                        if (accessibilityService.equalsIgnoreCase(service)) {
                            Log.d(TAG, "✅ Auto Clicker Accessibility Service is ENABLED");
                            return true;
                        }
                    }
                }
            }
            
            Log.d(TAG, "❌ Auto Clicker Accessibility Service is DISABLED");
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking accessibility: " + e.getMessage());
            return instance != null;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "✅ Auto Clicker Accessibility Service CREATED");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "🛑 Auto Clicker Accessibility Service DESTROYED");
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // SADECE GESTURE İÇİN - EVENT İŞLEME YOK
        // Telefonu yavaşlatmamak için boş bırakıldı
    }
    
    /**
     * 📱 EKRAN İÇERİĞİ DEĞİŞTİ - Smart Booker'a bildir
     */
    private void notifyContentChanged() {
        // Smart Booker servisi dinliyorsa bilgilendir
        // Bu sayede gerçek zamanlı ekran analizi yapabiliriz
    }
    
    /**
     * 🔍 EKRANI ANALİZ ET - DEVRE DIŞI (Telefonu yavaşlatıyor)
     */
    public List<ViewInfo> analyzeScreen() {
        // Telefonu yavaşlattığı için devre dışı
        Log.w(TAG, "⚠️ analyzeScreen() is disabled to prevent lag");
        return new ArrayList<>();
    }
    
    /**
     * 🔍 NODE'U TARA (Recursive)
     */
    private void scanNode(AccessibilityNodeInfo node, List<ViewInfo> views, int depth) {
        if (node == null || depth > 10) return; // Max depth
        
        try {
            // Text içeren view'ları bul
            CharSequence text = node.getText();
            CharSequence contentDesc = node.getContentDescription();
            
            if (text != null || contentDesc != null) {
                ViewInfo info = new ViewInfo();
                info.text = text != null ? text.toString() : "";
                info.description = contentDesc != null ? contentDesc.toString() : "";
                info.className = node.getClassName() != null ? node.getClassName().toString() : "";
                
                // Koordinatları al
                android.graphics.Rect bounds = new android.graphics.Rect();
                node.getBoundsInScreen(bounds);
                info.x = bounds.centerX();
                info.y = bounds.centerY();
                info.width = bounds.width();
                info.height = bounds.height();
                
                views.add(info);
                
                Log.d(TAG, String.format("View: '%s' at (%d,%d) [%s]", 
                    info.text, info.x, info.y, info.className));
            }
            
            // Alt node'ları tara
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    scanNode(child, views, depth + 1);
                    child.recycle();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error scanning node: " + e.getMessage());
        }
    }
    
    public static class ViewInfo {
        public String text = "";
        public String description = "";
        public String className = "";
        public int x, y, width, height;
        
        public boolean containsTime() {
            return text.matches(".*\\d{1,2}:\\d{2}.*");
        }
        
        public boolean containsFlameEmoji() {
            return text.contains("🔥") || description.contains("🔥") || 
                   text.contains("flame") || description.contains("flame");
        }
        
        @Override
        public String toString() {
            return String.format("ViewInfo{text='%s', pos=(%d,%d), size=%dx%d}", 
                text, x, y, width, height);
        }
    }
    
    @Override
    public void onInterrupt() {
        Log.w(TAG, "⚠️ Auto Clicker Accessibility Service interrupted");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // 📌 TAP GESTURES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * 🔥 STATIC TAP - Smart Booker için
     */
    public static void performTapStatic(float x, float y, long duration) {
        if (instance != null) {
            instance.performTap(x, y, duration);
        } else {
            Log.e(TAG, "❌ Accessibility service not running!");
        }
    }
    
    /**
     * Tek tıklama
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean performTap(float x, float y, long durationMs) {
        try {
            Path path = new Path();
            path.moveTo(x, y);
            
            GestureDescription.StrokeDescription stroke = 
                new GestureDescription.StrokeDescription(path, 0, durationMs);
            
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(stroke);
            
            boolean dispatched = dispatchGesture(builder.build(), null, null);
            if (dispatched) {
                Log.d(TAG, String.format("✅ TAP: (%.0f, %.0f) %dms", x, y, durationMs));
            }
            return dispatched;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ TAP ERROR: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Çift tıklama
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean performDoubleTap(float x, float y, long tapDuration, long intervalMs) {
        try {
            Path path1 = new Path();
            path1.moveTo(x, y);
            
            Path path2 = new Path();
            path2.moveTo(x, y);
            
            long start2 = tapDuration + intervalMs;
            
            GestureDescription.StrokeDescription stroke1 = 
                new GestureDescription.StrokeDescription(path1, 0, tapDuration);
            GestureDescription.StrokeDescription stroke2 = 
                new GestureDescription.StrokeDescription(path2, start2, tapDuration);
            
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(stroke1);
            builder.addStroke(stroke2);
            
            boolean dispatched = dispatchGesture(builder.build(), null, null);
            if (dispatched) {
                Log.d(TAG, String.format("✅ DOUBLE TAP: (%.0f, %.0f)", x, y));
            }
            return dispatched;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ DOUBLE TAP ERROR: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Çoklu nokta tıklama (multi-point)
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean performMultiTap(List<PointF> points, long durationMs) {
        try {
            GestureDescription.Builder builder = new GestureDescription.Builder();
            
            for (int i = 0; i < points.size() && i < 10; i++) { // Max 10 nokta
                PointF point = points.get(i);
                Path path = new Path();
                path.moveTo(point.x, point.y);
                
                GestureDescription.StrokeDescription stroke = 
                    new GestureDescription.StrokeDescription(path, i * 10, durationMs);
                builder.addStroke(stroke);
            }
            
            boolean dispatched = dispatchGesture(builder.build(), null, null);
            if (dispatched) {
                Log.d(TAG, String.format("✅ MULTI TAP: %d points", points.size()));
            }
            return dispatched;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ MULTI TAP ERROR: " + e.getMessage());
            return false;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // 📌 SWIPE GESTURES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Swipe (kaydırma)
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean performSwipe(float startX, float startY, float endX, float endY, long durationMs) {
        try {
            Path path = new Path();
            path.moveTo(startX, startY);
            path.lineTo(endX, endY);
            
            GestureDescription.StrokeDescription stroke = 
                new GestureDescription.StrokeDescription(path, 0, durationMs);
            
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(stroke);
            
            boolean dispatched = dispatchGesture(builder.build(), null, null);
            if (dispatched) {
                Log.d(TAG, String.format("✅ SWIPE: (%.0f,%.0f) → (%.0f,%.0f) %dms", 
                    startX, startY, endX, endY, durationMs));
            }
            return dispatched;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ SWIPE ERROR: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Pinch (yakınlaştır/uzaklaştır)
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean performPinch(float centerX, float centerY, float distance, boolean zoomIn, long durationMs) {
        try {
            float half = distance / 2;
            
            Path path1 = new Path();
            Path path2 = new Path();
            
            if (zoomIn) {
                // Dışarıdan içe (yakınlaştır)
                path1.moveTo(centerX - half, centerY);
                path1.lineTo(centerX - 20, centerY);
                
                path2.moveTo(centerX + half, centerY);
                path2.lineTo(centerX + 20, centerY);
            } else {
                // İçten dışa (uzaklaştır)
                path1.moveTo(centerX - 20, centerY);
                path1.lineTo(centerX - half, centerY);
                
                path2.moveTo(centerX + 20, centerY);
                path2.lineTo(centerX + half, centerY);
            }
            
            GestureDescription.StrokeDescription stroke1 = 
                new GestureDescription.StrokeDescription(path1, 0, durationMs);
            GestureDescription.StrokeDescription stroke2 = 
                new GestureDescription.StrokeDescription(path2, 0, durationMs);
            
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(stroke1);
            builder.addStroke(stroke2);
            
            boolean dispatched = dispatchGesture(builder.build(), null, null);
            if (dispatched) {
                Log.d(TAG, String.format("✅ PINCH: %s at (%.0f, %.0f)", 
                    zoomIn ? "ZOOM IN" : "ZOOM OUT", centerX, centerY));
            }
            return dispatched;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ PINCH ERROR: " + e.getMessage());
            return false;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // 📌 HELPER CLASS
    // ═══════════════════════════════════════════════════════════════
    
    public static class PointF {
        public float x;
        public float y;
        
        public PointF(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}

