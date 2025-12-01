package com.qrmaster.app.keyboard.textexpander;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

/**
 * Text Expander Manager
 * Yazılan metni izler ve kısayolları genişletir
 */
public class TextExpanderManager {
    private static final String TAG = "TextExpander";
    private static TextExpanderManager instance;
    
    private TextExpanderDatabase database;
    private boolean enabled = true;

    public static synchronized TextExpanderManager getInstance(Context context) {
        if (instance == null) {
            instance = new TextExpanderManager(context);
        }
        return instance;
    }

    private TextExpanderManager(Context context) {
        this.database = TextExpanderDatabase.getInstance(context);
        Log.d(TAG, "✅ TextExpanderManager başlatıldı");
    }

    /**
     * Yazılan metni kontrol et ve kısayol varsa genişlet
     * @param currentText Şu anki yazılan metin
     * @return Genişletilmiş metin veya null (değişmedi)
     */
    public String checkAndExpand(String currentText) {
        if (!enabled || TextUtils.isEmpty(currentText)) {
            return null;
        }

        // Son kelimeyi al (boşluk veya satır sonu ile biter)
        String[] words = currentText.split("\\s+");
        if (words.length == 0) return null;

        String lastWord = words[words.length - 1];
        
        // Kısayol mu kontrol et (/ ile başlar)
        if (!lastWord.startsWith("/")) {
            return null;
        }

        // Veritabanından kısayolu bul
        TextShortcut shortcut = database.findByTrigger(lastWord);
        if (shortcut != null) {
            database.incrementUsage(shortcut.getId());
            Log.d(TAG, "🔄 Kısayol genişletildi: " + lastWord + " → " + shortcut.getExpansion());
            
            // Son kelimeyi genişletilmiş metinle değiştir
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < words.length - 1; i++) {
                result.append(words[i]).append(" ");
            }
            result.append(shortcut.getExpansion());
            
            return result.toString();
        }

        return null;
    }

    /**
     * Yeni kısayol ekle
     */
    public long addShortcut(String trigger, String expansion, String description) {
        if (!trigger.startsWith("/")) {
            trigger = "/" + trigger;
        }
        TextShortcut shortcut = new TextShortcut(trigger, expansion, description);
        return database.addShortcut(shortcut);
    }

    /**
     * Kısayol güncelle
     */
    public int updateShortcut(TextShortcut shortcut) {
        return database.updateShortcut(shortcut);
    }

    /**
     * Kısayol sil
     */
    public int deleteShortcut(long id) {
        return database.deleteShortcut(id);
    }

    /**
     * Tüm kısayolları getir
     */
    public List<TextShortcut> getAllShortcuts() {
        return database.getAllShortcuts();
    }

    /**
     * Aktif/Pasif
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        Log.d(TAG, enabled ? "✅ Text Expander aktif" : "❌ Text Expander pasif");
    }
}

