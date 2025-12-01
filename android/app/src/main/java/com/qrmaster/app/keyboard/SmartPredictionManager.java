package com.qrmaster.app.keyboard;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🧠 Akıllı Kelime Tahmini Yöneticisi
 * 
 * Yazdıkça öğrenir ve tahmin eder:
 * 1. Her yazılan cümleyi analiz eder
 * 2. Kelime ilişkilerini DB'ye kaydeder
 * 3. Sıradaki kelimeyi tahmin eder
 * 
 * Örnek öğrenme:
 * "merhaba nasılsın nasıl gidiyor hayat"
 * 
 * Öğrenilen bigram'lar:
 * merhaba → nasılsın
 * nasılsın → nasıl
 * nasıl → gidiyor
 * gidiyor → hayat
 * 
 * Öğrenilen trigram'lar:
 * merhaba + nasılsın → nasıl
 * nasılsın + nasıl → gidiyor
 * nasıl + gidiyor → hayat
 * 
 * Kullanım:
 * M tuşuna basınca → "merhaba" öner (mevcut sözlük)
 * "merhaba " yazdıktan sonra → "nasılsın" öner (bigram)
 * "merhaba nasılsın " yazdıktan sonra → "nasıl" öner (trigram)
 */
public class SmartPredictionManager {
    private static final String TAG = "SmartPrediction";
    
    private final SmartPredictionDB db;
    private final LinkedList<String> recentWords = new LinkedList<>();
    private static final int MAX_RECENT_WORDS = 3; // Son 3 kelimeyi hatırla
    
    // Kelime ayırıcılar
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}\\p{N}']+");
    
    public SmartPredictionManager(Context context) {
        this.db = SmartPredictionDB.getInstance(context);
        Log.d(TAG, "✅ SmartPredictionManager initialized");
    }
    
    /**
     * Yeni metin yazıldığında çağrılır - öğrenme mekanizması
     * 
     * @param text Yazılan metin
     */
    public void onTextCommitted(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        // Kelimeleri ayır
        List<String> words = extractWords(text);
        
        if (words.isEmpty()) {
            return;
        }
        
        // Son kelimeyi recent words'e ekle
        String lastWord = words.get(words.size() - 1);
        addToRecentWords(lastWord);
        
        // Eğer cümle bittiyse (noktalama işareti varsa), tüm cümleyi öğren
        if (text.matches(".*[.!?;]\\s*$")) {
            learnFromSentence(text);
        }
    }
    
    /**
     * Space tuşuna basıldığında çağrılır - kelime tamamlandı
     * Önceki kelimelerle ilişki kur
     * 
     * @param currentWord Tamamlanan kelime
     * @return true if learned (had previous context), false otherwise
     */
    public boolean onSpacePressed(String currentWord) {
        if (currentWord == null || currentWord.trim().isEmpty()) {
            return false;
        }
        
        currentWord = currentWord.toLowerCase().trim();
        
        boolean learned = false;
        
        // Önceki kelimelerle ilişki kur
        if (recentWords.size() >= 1) {
            String prevWord = recentWords.get(recentWords.size() - 1);
            db.saveBigram(prevWord, currentWord);
            Log.d(TAG, "📚 Learned bigram: " + prevWord + " → " + currentWord);
            learned = true;
        }
        
        if (recentWords.size() >= 2) {
            String prevWord1 = recentWords.get(recentWords.size() - 2);
            String prevWord2 = recentWords.get(recentWords.size() - 1);
            db.saveTrigram(prevWord1, prevWord2, currentWord);
            Log.d(TAG, "📚 Learned trigram: " + prevWord1 + " + " + prevWord2 + " → " + currentWord);
        }
        
        addToRecentWords(currentWord);
        
        return learned;
    }
    
    /**
     * Cümleden öğren - tüm kelime ilişkilerini kaydet
     */
    private void learnFromSentence(String sentence) {
        List<String> words = extractWords(sentence);
        
        if (words.size() < 2) {
            return;
        }
        
        Log.d(TAG, "📖 Learning from sentence: " + sentence);
        Log.d(TAG, "📖 Words: " + words);
        
        // Bigram'ları kaydet
        for (int i = 0; i < words.size() - 1; i++) {
            db.saveBigram(words.get(i), words.get(i + 1));
        }
        
        // Trigram'ları kaydet
        for (int i = 0; i < words.size() - 2; i++) {
            db.saveTrigram(words.get(i), words.get(i + 1), words.get(i + 2));
        }
        
        Log.d(TAG, "✅ Sentence learned: " + (words.size() - 1) + " bigrams, " + (words.size() - 2) + " trigrams");
    }
    
    /**
     * Sıradaki kelimeyi tahmin et (context-aware)
     * 
     * @return Tahmin edilen kelimeler (öncelik sırasına göre)
     */
    public List<String> getPredictions() {
        List<String> predictions = new ArrayList<>();
        
        if (recentWords.isEmpty()) {
            return predictions;
        }
        
        String prevWord2 = recentWords.size() >= 1 ? recentWords.get(recentWords.size() - 1) : null;
        String prevWord1 = recentWords.size() >= 2 ? recentWords.get(recentWords.size() - 2) : null;
        
        // Akıllı tahmin: Önce trigram, sonra bigram
        predictions = db.getSmartPredictions(prevWord1, prevWord2, 5);
        
        if (!predictions.isEmpty()) {
            Log.d(TAG, "💡 Smart predictions: " + predictions);
        }
        
        return predictions;
    }
    
    /**
     * Belirli bir prefix ile başlayan tahminleri al
     * Hem sözlük hem de öğrenilen kelimeler
     */
    public List<String> getPredictionsWithPrefix(String prefix, List<String> dictionarySuggestions) {
        List<String> combined = new ArrayList<>();
        
        // Önce smart predictions ekle
        List<String> smartPreds = getPredictions();
        for (String pred : smartPreds) {
            if (pred.toLowerCase().startsWith(prefix.toLowerCase())) {
                combined.add(pred);
            }
        }
        
        // Sonra dictionary suggestions ekle (duplicate check)
        if (dictionarySuggestions != null) {
            for (String dict : dictionarySuggestions) {
                if (!combined.contains(dict.toLowerCase())) {
                    combined.add(dict);
                }
            }
        }
        
        return combined;
    }
    
    /**
     * Metni kelimelere ayır
     */
    private List<String> extractWords(String text) {
        List<String> words = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return words;
        }
        
        Matcher matcher = WORD_PATTERN.matcher(text.toLowerCase());
        while (matcher.find()) {
            String word = matcher.group().trim();
            if (!word.isEmpty() && word.length() > 1) { // Min 2 karakter
                words.add(word);
            }
        }
        
        return words;
    }
    
    /**
     * Recent words listesine ekle (FIFO)
     */
    private void addToRecentWords(String word) {
        if (word == null || word.isEmpty()) {
            return;
        }
        
        word = word.toLowerCase().trim();
        
        recentWords.add(word);
        
        if (recentWords.size() > MAX_RECENT_WORDS) {
            recentWords.removeFirst();
        }
        
        Log.d(TAG, "Recent words: " + recentWords);
    }
    
    /**
     * Recent words'ü temizle (yeni cümle başlarken)
     */
    public void clearRecentWords() {
        recentWords.clear();
        Log.d(TAG, "Recent words cleared");
    }
    
    /**
     * Recent words'ü al (debug için)
     */
    public List<String> getRecentWords() {
        return new ArrayList<>(recentWords);
    }
    
    /**
     * Backspace basıldığında - son kelimeyi çıkar
     */
    public void onBackspacePressed() {
        if (!recentWords.isEmpty()) {
            recentWords.removeLast();
        }
    }
    
    /**
     * İstatistikler
     */
    public void printStats() {
        var stats = db.getStats();
        Log.d(TAG, "📊 Stats: " + stats.get("bigrams") + " bigrams, " + stats.get("trigrams") + " trigrams");
    }
    
    /**
     * Eski verileri temizle (30 gün)
     */
    public void cleanOldData() {
        db.cleanOldData(30);
    }
}

