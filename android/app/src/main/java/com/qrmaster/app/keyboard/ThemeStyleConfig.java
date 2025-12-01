package com.qrmaster.app.keyboard;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 🎨 THEME STYLE CONFIGURATION
 * 
 * Configures how theme colors are applied to keyboard keys
 */
public class ThemeStyleConfig {
    
    // Style modes
    public enum KeyColorMode {
        FILL_ONLY,          // Sadece buton içi renkli
        STROKE_ONLY,        // Sadece kenarlar renkli
        FILL_AND_STROKE,    // Her ikisi de renkli
        GRADIENT            // Gradient fill
    }
    
    public enum TextColorMode {
        AUTO,               // Arka plana göre otomatik (açık→siyah, koyu→beyaz)
        ALWAYS_WHITE,       // Her zaman beyaz
        ALWAYS_BLACK,       // Her zaman siyah
        THEME_ACCENT        // Tema accent rengi
    }
    
    public enum KeyBackgroundMode {
        SOLID,              // Düz renk
        GRADIENT,           // Gradient
        TRANSPARENT,        // Saydam (tema arka planı görünsün)
        SEMI_TRANSPARENT    // Yarı saydam
    }
    
    private SharedPreferences prefs;
    
    private KeyColorMode keyColorMode = KeyColorMode.FILL_AND_STROKE;
    private TextColorMode textColorMode = TextColorMode.AUTO;
    private KeyBackgroundMode keyBackgroundMode = KeyBackgroundMode.GRADIENT;
    
    private int strokeWidth = 2;  // dp
    private int keyElevation = 2;  // dp
    private int keyOpacity = 100;  // 0-100%
    
    public ThemeStyleConfig(Context context) {
        this.prefs = context.getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE);
        load();
    }
    
    private void load() {
        String colorModeStr = prefs.getString("key_color_mode", KeyColorMode.FILL_AND_STROKE.name());
        try {
            keyColorMode = KeyColorMode.valueOf(colorModeStr);
        } catch (Exception e) {
            keyColorMode = KeyColorMode.FILL_AND_STROKE;
        }
        
        String textModeStr = prefs.getString("text_color_mode", TextColorMode.AUTO.name());
        try {
            textColorMode = TextColorMode.valueOf(textModeStr);
        } catch (Exception e) {
            textColorMode = TextColorMode.AUTO;
        }
        
        String bgModeStr = prefs.getString("key_background_mode", KeyBackgroundMode.GRADIENT.name());
        try {
            keyBackgroundMode = KeyBackgroundMode.valueOf(bgModeStr);
        } catch (Exception e) {
            keyBackgroundMode = KeyBackgroundMode.GRADIENT;
        }
        
        strokeWidth = prefs.getInt("stroke_width", 2);
        keyElevation = prefs.getInt("key_elevation", 2);
        keyOpacity = prefs.getInt("key_opacity", 100);
    }
    
    public void save() {
        prefs.edit()
            .putString("key_color_mode", keyColorMode.name())
            .putString("text_color_mode", textColorMode.name())
            .putString("key_background_mode", keyBackgroundMode.name())
            .putInt("stroke_width", strokeWidth)
            .putInt("key_elevation", keyElevation)
            .putInt("key_opacity", keyOpacity)
            .apply();
    }
    
    // Getters
    public KeyColorMode getKeyColorMode() { return keyColorMode; }
    public TextColorMode getTextColorMode() { return textColorMode; }
    public KeyBackgroundMode getKeyBackgroundMode() { return keyBackgroundMode; }
    public int getStrokeWidth() { return strokeWidth; }
    public int getKeyElevation() { return keyElevation; }
    public int getKeyOpacity() { return keyOpacity; }
    
    // Setters
    public void setKeyColorMode(KeyColorMode mode) { 
        this.keyColorMode = mode;
        save();
    }
    
    public void setTextColorMode(TextColorMode mode) { 
        this.textColorMode = mode;
        save();
    }
    
    public void setKeyBackgroundMode(KeyBackgroundMode mode) { 
        this.keyBackgroundMode = mode;
        save();
    }
    
    public void setStrokeWidth(int width) { 
        this.strokeWidth = width;
        save();
    }
    
    public void setKeyElevation(int elevation) { 
        this.keyElevation = elevation;
        save();
    }
    
    public void setKeyOpacity(int opacity) { 
        this.keyOpacity = Math.max(0, Math.min(100, opacity));
        save();
    }
    
    /**
     * Calculate text color based on background
     */
    public int getTextColor(int backgroundColor, int themeAccentColor) {
        switch (textColorMode) {
            case ALWAYS_WHITE:
                return 0xFFFFFFFF;
                
            case ALWAYS_BLACK:
                return 0xFF000000;
                
            case THEME_ACCENT:
                return themeAccentColor;
                
            case AUTO:
            default:
                return isColorDark(backgroundColor) ? 0xFFFFFFFF : 0xFF000000;
        }
    }
    
    /**
     * Apply opacity to color
     */
    public int applyOpacity(int color) {
        if (keyOpacity >= 100) return color;
        
        int alpha = (int) (255 * (keyOpacity / 100f));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
    
    /**
     * Check if color is dark (for auto text color)
     */
    private boolean isColorDark(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        
        // Calculate luminance
        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;
        
        return luminance < 0.5;
    }
    
    /**
     * Get Turkish display name for KeyColorMode
     */
    public static String getKeyColorModeName(KeyColorMode mode) {
        switch (mode) {
            case FILL_ONLY: return "Sadece İç Dolgu";
            case STROKE_ONLY: return "Sadece Kenarlar";
            case FILL_AND_STROKE: return "İç + Kenar";
            case GRADIENT: return "Gradient (Geçişli)";
            default: return "Bilinmeyen";
        }
    }
    
    /**
     * Get Turkish display name for TextColorMode
     */
    public static String getTextColorModeName(TextColorMode mode) {
        switch (mode) {
            case AUTO: return "Otomatik (Akıllı)";
            case ALWAYS_WHITE: return "Her Zaman Beyaz";
            case ALWAYS_BLACK: return "Her Zaman Siyah";
            case THEME_ACCENT: return "Tema Accent Rengi";
            default: return "Bilinmeyen";
        }
    }
    
    /**
     * Get Turkish display name for KeyBackgroundMode
     */
    public static String getKeyBackgroundModeName(KeyBackgroundMode mode) {
        switch (mode) {
            case SOLID: return "Düz Renk";
            case GRADIENT: return "Gradient";
            case TRANSPARENT: return "Saydam";
            case SEMI_TRANSPARENT: return "Yarı Saydam";
            default: return "Bilinmeyen";
        }
    }
}

