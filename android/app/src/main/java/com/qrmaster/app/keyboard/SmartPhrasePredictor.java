package com.qrmaster.app.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Akıllı kelime ve cümle tahmini sistemi
 * - Kullanıcının yazdığı kelimeleri ve cümleleri öğrenir
 * - N-gram modeli ile sonraki kelimeleri tahmin eder
 * - dictionary_tr.txt ile birlikte çalışır
 */
public class SmartPhrasePredictor {
    private static final String TAG = "SmartPhrasePredictor";
    private static final String PREF_NAME = "keyboard_phrase_history";
    private static final String PREF_PHRASES = "phrases";
    private static final String PREF_WORDS = "words";
    private static final int MAX_PHRASES = 10000;
    private static final int MAX_SUGGESTIONS = 3;

    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;

    // Kelime geçmişi: kelime -> frekans
    private final ConcurrentHashMap<String, Integer> wordFrequency = new ConcurrentHashMap<>();

    // N-gram modeli: "kelime1 kelime2" -> [sonraki kelimeler]
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> bigramModel = new ConcurrentHashMap<>();

    // Trigram modeli: "kelime1 kelime2" -> kelime3 -> frekans
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> trigramModel = new ConcurrentHashMap<>();

    // Son yazılan kelimeler (context için)
    private final LinkedList<String> recentWords = new LinkedList<>();
    private static final int RECENT_WORDS_SIZE = 5;

    public SmartPhrasePredictor(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        loadFromPrefs();
    }

    /**
     * Kullanıcı bir kelime yazdığında çağır
     */
    public void learnWord(String word) {
        if (word == null || word.trim().isEmpty() || word.length() < 2) {
            return;
        }

        String normalized = normalize(word);
        
        // Kelime frekansını artır
        wordFrequency.merge(normalized, 1, Integer::sum);

        // N-gram modeline ekle
        if (!recentWords.isEmpty()) {
            String prev = recentWords.getLast();
            
            // Bigram: prev -> current
            bigramModel.computeIfAbsent(prev, k -> new ConcurrentHashMap<>())
                .merge(normalized, 1, Integer::sum);

            // Trigram: prev2 prev1 -> current
            if (recentWords.size() >= 2) {
                String prev2 = recentWords.get(recentWords.size() - 2);
                String bigramKey = prev2 + " " + prev;
                trigramModel.computeIfAbsent(bigramKey, k -> new ConcurrentHashMap<>())
                    .merge(normalized, 1, Integer::sum);
            }
        }

        // Son kelimelere ekle
        recentWords.add(normalized);
        if (recentWords.size() > RECENT_WORDS_SIZE) {
            recentWords.removeFirst();
        }

        // Periyodik kayıt (her 10 kelimede bir)
        if (wordFrequency.size() % 10 == 0) {
            saveToPrefs();
        }
    }

    /**
     * Kullanıcı bir cümleyi bitirdiğinde çağır (nokta, enter, vs.)
     */
    public void finishPhrase() {
        recentWords.clear();
        saveToPrefs();
    }

    /**
     * Akıllı öneri getir
     * @param currentWord Şu an yazılan kelime
     * @param dictSuggestions Sözlükten gelen öneriler
     * @return Birleştirilmiş ve sıralanmış öneriler
     */
    public List<String> getSuggestions(String currentWord, List<String> dictSuggestions) {
        if (currentWord == null || currentWord.trim().isEmpty()) {
            // Context-based öneriler (önceki kelimeye göre)
            return getContextBasedSuggestions();
        }

        String normalized = normalize(currentWord);
        Map<String, Integer> scores = new HashMap<>();

        // 1. Sözlük önerilerini ekle
        for (String suggestion : dictSuggestions) {
            scores.put(suggestion, 10); // Base score
        }

        // 2. Kullanıcı geçmişinden eşleşenleri ekle
        for (Map.Entry<String, Integer> entry : wordFrequency.entrySet()) {
            String word = entry.getKey();
            if (word.startsWith(normalized)) {
                int freq = entry.getValue();
                // Kullanıcı kelimelerine ekstra ağırlık
                scores.merge(word, 50 + freq * 10, Integer::sum);
            }
        }

        // 3. Context-aware skorlar (önceki kelimeye göre)
        if (!recentWords.isEmpty()) {
            String prev = recentWords.getLast();
            ConcurrentHashMap<String, Integer> nextWords = bigramModel.get(prev);
            if (nextWords != null) {
                for (Map.Entry<String, Integer> entry : nextWords.entrySet()) {
                    String word = entry.getKey();
                    if (word.startsWith(normalized)) {
                        int freq = entry.getValue();
                        scores.merge(word, 100 + freq * 20, Integer::sum);
                    }
                }
            }

            // Trigram model
            if (recentWords.size() >= 2) {
                String prev2 = recentWords.get(recentWords.size() - 2);
                String bigramKey = prev2 + " " + prev;
                ConcurrentHashMap<String, Integer> triWords = trigramModel.get(bigramKey);
                if (triWords != null) {
                    for (Map.Entry<String, Integer> entry : triWords.entrySet()) {
                        String word = entry.getKey();
                        if (word.startsWith(normalized)) {
                            int freq = entry.getValue();
                            scores.merge(word, 200 + freq * 30, Integer::sum);
                        }
                    }
                }
            }
        }

        // 4. Sırala ve döndür
        return scores.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(MAX_SUGGESTIONS)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Boşluktan sonra context-based öneriler
     */
    private List<String> getContextBasedSuggestions() {
        if (recentWords.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Integer> scores = new HashMap<>();

        // Bigram model
        String prev = recentWords.getLast();
        ConcurrentHashMap<String, Integer> nextWords = bigramModel.get(prev);
        if (nextWords != null) {
            for (Map.Entry<String, Integer> entry : nextWords.entrySet()) {
                scores.put(entry.getKey(), entry.getValue() * 10);
            }
        }

        // Trigram model (daha güçlü)
        if (recentWords.size() >= 2) {
            String prev2 = recentWords.get(recentWords.size() - 2);
            String bigramKey = prev2 + " " + prev;
            ConcurrentHashMap<String, Integer> triWords = trigramModel.get(bigramKey);
            if (triWords != null) {
                for (Map.Entry<String, Integer> entry : triWords.entrySet()) {
                    scores.merge(entry.getKey(), entry.getValue() * 20, Integer::sum);
                }
            }
        }

        return scores.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(MAX_SUGGESTIONS)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private String normalize(String word) {
        return word.toLowerCase(new Locale("tr", "TR")).trim();
    }

    private void loadFromPrefs() {
        try {
            // Kelime frekansları
            String wordsJson = prefs.getString(PREF_WORDS, null);
            if (wordsJson != null) {
                Type type = new TypeToken<ConcurrentHashMap<String, Integer>>(){}.getType();
                ConcurrentHashMap<String, Integer> saved = gson.fromJson(wordsJson, type);
                if (saved != null) {
                    wordFrequency.clear();
                    wordFrequency.putAll(saved);
                }
            }

            // Bigram modeli
            String phrasesJson = prefs.getString(PREF_PHRASES, null);
            if (phrasesJson != null) {
                Type type = new TypeToken<ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>>>(){}.getType();
                ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> saved = gson.fromJson(phrasesJson, type);
                if (saved != null) {
                    bigramModel.clear();
                    bigramModel.putAll(saved);
                }
            }

            Log.d(TAG, "✅ Phrase model yüklendi: " + wordFrequency.size() + " kelime, " 
                + bigramModel.size() + " bigram");
        } catch (Exception e) {
            Log.e(TAG, "Phrase model yüklenemedi", e);
        }
    }

    private void saveToPrefs() {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            
            // Kelime frekansları
            String wordsJson = gson.toJson(wordFrequency);
            editor.putString(PREF_WORDS, wordsJson);

            // Bigram modeli (trigram çok büyük olabilir, sadece bigram kaydediyoruz)
            String phrasesJson = gson.toJson(bigramModel);
            editor.putString(PREF_PHRASES, phrasesJson);

            editor.apply();
            Log.d(TAG, "✅ Phrase model kaydedildi");
        } catch (Exception e) {
            Log.e(TAG, "Phrase model kaydedilemedi", e);
        }
    }

    public void clear() {
        wordFrequency.clear();
        bigramModel.clear();
        trigramModel.clear();
        recentWords.clear();
        prefs.edit().clear().apply();
    }

    public void shutdown() {
        saveToPrefs();
    }
}









