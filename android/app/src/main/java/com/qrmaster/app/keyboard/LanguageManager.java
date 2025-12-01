package com.qrmaster.app.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.Keyboard;
import com.qrmaster.app.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Çoklu Dil Desteği Yöneticisi
 * fcitx5'ten esinlenildi
 */
public class LanguageManager {
    private static final String PREFS_NAME = "keyboard_language";
    private static final String KEY_CURRENT_LANG = "current_language";
    
    public enum Language {
        TURKISH("tr", "Türkçe Q", R.xml.keyboard_turkish_q_modern, "🇹🇷"),
        ENGLISH("en", "English", R.xml.keyboard_english_qwerty, "🇬🇧"),
        GERMAN("de", "Deutsch", R.xml.keyboard_german_qwertz, "🇩🇪");
        
        public final String code;
        public final String name;
        public final int layoutResId;
        public final String flag;
        
        Language(String code, String name, int layoutResId, String flag) {
            this.code = code;
            this.name = name;
            this.layoutResId = layoutResId;
            this.flag = flag;
        }
        
        public static Language fromCode(String code) {
            for (Language lang : values()) {
                if (lang.code.equals(code)) {
                    return lang;
                }
            }
            return TURKISH; // Default
        }
    }
    
    private final SharedPreferences prefs;
    private Language currentLanguage;
    
    public LanguageManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedLang = prefs.getString(KEY_CURRENT_LANG, Language.TURKISH.code);
        currentLanguage = Language.fromCode(savedLang);
    }
    
    public Language getCurrentLanguage() {
        return currentLanguage;
    }
    
    public void setCurrentLanguage(Language language) {
        currentLanguage = language;
        prefs.edit().putString(KEY_CURRENT_LANG, language.code).apply();
    }
    
    public Language getNextLanguage() {
        Language[] languages = Language.values();
        int currentIndex = currentLanguage.ordinal();
        int nextIndex = (currentIndex + 1) % languages.length;
        return languages[nextIndex];
    }
    
    public void switchToNextLanguage() {
        setCurrentLanguage(getNextLanguage());
    }
    
    public List<Language> getAllLanguages() {
        List<Language> list = new ArrayList<>();
        for (Language lang : Language.values()) {
            list.add(lang);
        }
        return list;
    }
    
    public Keyboard createKeyboardForCurrentLanguage(Context context) {
        return new Keyboard(context, currentLanguage.layoutResId);
    }
}








