package com.qrmaster.app.keyboard.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Calendar;

/**
 * Dinamik Tema Motoru
 * - Zaman bazlı (Sabah/Öğle/Akşam/Gece)
 * - Müzik bazlı (Ses seviyesi / Media state)
 * - Özel renkler
 */
public class DynamicThemeEngine {
    private static final String TAG = "DynamicTheme";
    private static final String PREFS_NAME = "keyboard_theme";
    private static final String KEY_THEME_MODE = "theme_mode";
    
    public enum ThemeMode {
        LIGHT, DARK, AUTO_TIME, AUTO_MUSIC, CUSTOM
    }
    
    public static class ThemeColors {
        public int background;
        public int keyBackground;
        public int keyText;
        public int accentColor;
        public int toolbarBg;
        
        public ThemeColors(int bg, int keyBg, int keyText, int accent, int toolbar) {
            this.background = bg;
            this.keyBackground = keyBg;
            this.keyText = keyText;
            this.accentColor = accent;
            this.toolbarBg = toolbar;
        }
    }
    
    private final Context context;
    private final SharedPreferences prefs;
    private final Handler handler;
    private ThemeMode currentMode;
    private ThemeColors currentTheme;
    
    public interface ThemeChangeListener {
        void onThemeChanged(ThemeColors colors);
    }
    
    private ThemeChangeListener listener;

    public DynamicThemeEngine(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.handler = new Handler(Looper.getMainLooper());
        loadThemeMode();
        updateTheme();
    }

    public void setThemeChangeListener(ThemeChangeListener listener) {
        this.listener = listener;
    }

    private void loadThemeMode() {
        String modeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.AUTO_TIME.name());
        try {
            currentMode = ThemeMode.valueOf(modeStr);
        } catch (Exception e) {
            currentMode = ThemeMode.AUTO_TIME;
        }
    }

    public void setThemeMode(ThemeMode mode) {
        currentMode = mode;
        prefs.edit().putString(KEY_THEME_MODE, mode.name()).apply();
        updateTheme();
    }

    public ThemeMode getThemeMode() {
        return currentMode;
    }

    public ThemeColors getCurrentTheme() {
        return currentTheme;
    }

    public void updateTheme() {
        switch (currentMode) {
            case LIGHT:
                currentTheme = getLightTheme();
                break;
            case DARK:
                currentTheme = getDarkTheme();
                break;
            case AUTO_TIME:
                currentTheme = getTimeBasedTheme();
                break;
            case AUTO_MUSIC:
                currentTheme = getMusicBasedTheme();
                break;
            case CUSTOM:
                currentTheme = getCustomTheme();
                break;
            default:
                currentTheme = getDarkTheme();
        }
        
        if (listener != null) {
            listener.onThemeChanged(currentTheme);
        }
    }

    private ThemeColors getLightTheme() {
        return new ThemeColors(
            0xFFFFFFFF, // background
            0xFFE5E5EA, // key background
            0xFF000000, // key text
            0xFF007AFF, // accent
            0xFFF2F2F7  // toolbar
        );
    }

    private ThemeColors getDarkTheme() {
        return new ThemeColors(
            0xFF1C1C1E, // background
            0xFF33363D, // key background
            0xFFFFFFFF, // key text
            0xFF0A84FF, // accent
            0xFF1C1C1E  // toolbar
        );
    }

    private ThemeColors getTimeBasedTheme() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        
        if (hour >= 6 && hour < 12) {
            // Morning - Light blue
            return new ThemeColors(
                0xFFE3F2FD, // Light blue bg
                0xFFBBDEFB,
                0xFF0D47A1,
                0xFF2196F3,
                0xFFBBDEFB
            );
        } else if (hour >= 12 && hour < 18) {
            // Afternoon - Warm
            return new ThemeColors(
                0xFFFFF3E0,
                0xFFFFE0B2,
                0xFFE65100,
                0xFFFF9800,
                0xFFFFE0B2
            );
        } else if (hour >= 18 && hour < 22) {
            // Evening - Purple
            return new ThemeColors(
                0xFFF3E5F5,
                0xFFE1BEE7,
                0xFF4A148C,
                0xFF9C27B0,
                0xFFE1BEE7
            );
        } else {
            // Night - Dark
            return getDarkTheme();
        }
    }

    private ThemeColors getMusicBasedTheme() {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                float volumeRatio = (float) volume / maxVolume;
                
                // Color shifts based on volume
                if (volumeRatio > 0.7f) {
                    // High volume - Energetic red
                    return new ThemeColors(
                        0xFF1A0A0A,
                        0xFF3D1F1F,
                        0xFFFFFFFF,
                        0xFFFF3B30,
                        0xFF1A0A0A
                    );
                } else if (volumeRatio > 0.4f) {
                    // Mid volume - Cool blue
                    return new ThemeColors(
                        0xFF0A0A1A,
                        0xFF1F1F3D,
                        0xFFFFFFFF,
                        0xFF0A84FF,
                        0xFF0A0A1A
                    );
                } else {
                    // Low/no music - Calm green
                    return new ThemeColors(
                        0xFF0A1A0A,
                        0xFF1F3D1F,
                        0xFFFFFFFF,
                        0xFF34C759,
                        0xFF0A1A0A
                    );
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Music theme error", e);
        }
        return getDarkTheme();
    }

    private ThemeColors getCustomTheme() {
        // Load from prefs
        int bg = prefs.getInt("custom_bg", 0xFF1C1C1E);
        int keyBg = prefs.getInt("custom_key_bg", 0xFF33363D);
        int keyText = prefs.getInt("custom_key_text", 0xFFFFFFFF);
        int accent = prefs.getInt("custom_accent", 0xFF0A84FF);
        int toolbar = prefs.getInt("custom_toolbar", 0xFF1C1C1E);
        
        return new ThemeColors(bg, keyBg, keyText, accent, toolbar);
    }

    public void setCustomTheme(int bg, int keyBg, int keyText, int accent, int toolbar) {
        prefs.edit()
            .putInt("custom_bg", bg)
            .putInt("custom_key_bg", keyBg)
            .putInt("custom_key_text", keyText)
            .putInt("custom_accent", accent)
            .putInt("custom_toolbar", toolbar)
            .apply();
        
        if (currentMode == ThemeMode.CUSTOM) {
            updateTheme();
        }
    }

    public void startAutoUpdate(long intervalMs) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentMode == ThemeMode.AUTO_TIME || currentMode == ThemeMode.AUTO_MUSIC) {
                    updateTheme();
                }
                handler.postDelayed(this, intervalMs);
            }
        }, intervalMs);
    }

    public void stopAutoUpdate() {
        handler.removeCallbacksAndMessages(null);
    }
}









