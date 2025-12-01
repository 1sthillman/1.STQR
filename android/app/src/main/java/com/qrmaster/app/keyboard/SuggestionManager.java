package com.qrmaster.app.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Akıllı kelime öneri sistemi
 * - Sözlük tabanlı
 * - Önceden yazılan kelimeleri hatırlayıp önceliklendirir
 * - Frekans bazlı öneri
 */
public class SuggestionManager {

    public interface LoadCallback {
        void onLoaded();
    }

    private final List<String> dictionary = new ArrayList<>();
    private final Map<String, Integer> userHistory = new LinkedHashMap<>(); // Kullanıcı geçmişi
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean loaded = false;
    private Context context;

    public void load(Context context, LoadCallback callback) {
        this.context = context;
        if (loaded) {
            if (callback != null) callback.onLoaded();
            return;
        }

        executor.execute(() -> {
            // Sözlük yükle
            AssetManager manager = context.getAssets();
            try (InputStream inputStream = manager.open("dictionary_tr.txt");
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        dictionary.add(line.toLowerCase(Locale.getDefault()));
                    }
                }
                Collections.sort(dictionary);
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            // Kullanıcı geçmişini yükle
            loadUserHistory();
            loaded = true;
            
            if (callback != null) {
                mainHandler.post(callback::onLoaded);
            }
        });
    }
    
    private void loadUserHistory() {
        try {
            SharedPreferences prefs = context.getSharedPreferences("keyboard_suggestions", Context.MODE_PRIVATE);
            Map<String, ?> allEntries = prefs.getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                if (entry.getValue() instanceof Integer) {
                    userHistory.put(entry.getKey(), (Integer) entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void learnWord(String word) {
        if (TextUtils.isEmpty(word) || word.length() < 2) return;
        word = word.toLowerCase(Locale.getDefault());
        
        userHistory.put(word, userHistory.getOrDefault(word, 0) + 1);
        
        // Asenkron kaydet
        final String finalWord = word;
        final int finalCount = userHistory.get(word);
        executor.execute(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences("keyboard_suggestions", Context.MODE_PRIVATE);
                prefs.edit().putInt(finalWord, finalCount).apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public List<String> getSuggestions(String prefix, int maxCount) {
        if (!loaded || TextUtils.isEmpty(prefix)) {
            return Collections.emptyList();
        }
        prefix = prefix.toLowerCase(Locale.getDefault());
        
        // Önce kullanıcı geçmişinden öneriler al (öncelikli)
        List<WordScore> scoredWords = new ArrayList<>();
        
        // Kullanıcı geçmişinden
        for (Map.Entry<String, Integer> entry : userHistory.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                scoredWords.add(new WordScore(entry.getKey(), entry.getValue() * 100)); // Yüksek öncelik
            }
        }
        
        // Sözlükten
        for (String word : dictionary) {
            if (word.startsWith(prefix)) {
                int score = userHistory.getOrDefault(word, 0);
                scoredWords.add(new WordScore(word, score));
            }
            if (scoredWords.size() >= maxCount * 3) break; // Fazla olsun, sonra sırala
        }
        
        // Skora göre sırala (en yüksek önce)
        Collections.sort(scoredWords, (a, b) -> Integer.compare(b.score, a.score));
        
        // İlk maxCount kelimeyi al
        List<String> results = new ArrayList<>(maxCount);
        for (int i = 0; i < Math.min(maxCount, scoredWords.size()); i++) {
            results.add(scoredWords.get(i).word);
        }
        
        return results;
    }
    
    private static class WordScore {
        String word;
        int score;
        
        WordScore(String word, int score) {
            this.word = word;
            this.score = score;
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
