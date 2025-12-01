package com.qrmaster.app.keyboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🧠 Akıllı Kelime Tahmini Veritabanı
 * 
 * N-gram tabanlı kelime ilişkileri öğrenir:
 * - "merhaba" → "nasılsın" (80%)
 * - "nasılsın" → "nasıl" (60%)
 * - "nasıl" → "gidiyor" (70%)
 * 
 * Özellikler:
 * - Bigram (2 kelime) ilişkileri
 * - Trigram (3 kelime) ilişkileri
 * - Frekans sayımı
 * - Otomatik öğrenme
 * - Akıllı sıralama
 */
public class SmartPredictionDB extends SQLiteOpenHelper {
    private static final String TAG = "SmartPredictionDB";
    
    private static final String DATABASE_NAME = "smart_prediction.db";
    private static final int DATABASE_VERSION = 1;
    
    // Bigram table (2 kelime ilişkisi)
    private static final String TABLE_BIGRAM = "bigram";
    private static final String COL_WORD1 = "word1";
    private static final String COL_WORD2 = "word2";
    private static final String COL_FREQUENCY = "frequency";
    private static final String COL_LAST_USED = "last_used";
    
    // Trigram table (3 kelime ilişkisi) - daha akıllı
    private static final String TABLE_TRIGRAM = "trigram";
    private static final String COL_WORD1_TRI = "word1";
    private static final String COL_WORD2_TRI = "word2";
    private static final String COL_WORD3_TRI = "word3";
    private static final String COL_FREQUENCY_TRI = "frequency";
    private static final String COL_LAST_USED_TRI = "last_used";
    
    private static SmartPredictionDB instance;
    
    public static synchronized SmartPredictionDB getInstance(Context context) {
        if (instance == null) {
            instance = new SmartPredictionDB(context.getApplicationContext());
        }
        return instance;
    }
    
    private SmartPredictionDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Bigram table
        String createBigram = "CREATE TABLE " + TABLE_BIGRAM + " (" +
                COL_WORD1 + " TEXT NOT NULL, " +
                COL_WORD2 + " TEXT NOT NULL, " +
                COL_FREQUENCY + " INTEGER DEFAULT 1, " +
                COL_LAST_USED + " INTEGER DEFAULT 0, " +
                "PRIMARY KEY (" + COL_WORD1 + ", " + COL_WORD2 + ")" +
                ")";
        db.execSQL(createBigram);
        
        // Trigram table
        String createTrigram = "CREATE TABLE " + TABLE_TRIGRAM + " (" +
                COL_WORD1_TRI + " TEXT NOT NULL, " +
                COL_WORD2_TRI + " TEXT NOT NULL, " +
                COL_WORD3_TRI + " TEXT NOT NULL, " +
                COL_FREQUENCY_TRI + " INTEGER DEFAULT 1, " +
                COL_LAST_USED_TRI + " INTEGER DEFAULT 0, " +
                "PRIMARY KEY (" + COL_WORD1_TRI + ", " + COL_WORD2_TRI + ", " + COL_WORD3_TRI + ")" +
                ")";
        db.execSQL(createTrigram);
        
        // Indexes for fast lookup
        db.execSQL("CREATE INDEX idx_bigram_word1 ON " + TABLE_BIGRAM + "(" + COL_WORD1 + ")");
        db.execSQL("CREATE INDEX idx_trigram_words ON " + TABLE_TRIGRAM + "(" + COL_WORD1_TRI + ", " + COL_WORD2_TRI + ")");
        
        Log.d(TAG, "✅ Database created");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BIGRAM);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIGRAM);
        onCreate(db);
    }
    
    /**
     * Bigram ilişkisi kaydet veya güncelle
     * Örnek: "merhaba" → "nasılsın"
     */
    public void saveBigram(String word1, String word2) {
        if (word1 == null || word2 == null || word1.isEmpty() || word2.isEmpty()) {
            return;
        }
        
        word1 = word1.toLowerCase().trim();
        word2 = word2.toLowerCase().trim();
        
        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COL_WORD1, word1);
            values.put(COL_WORD2, word2);
            values.put(COL_FREQUENCY, 1);
            values.put(COL_LAST_USED, System.currentTimeMillis());
            
            long result = db.insertWithOnConflict(TABLE_BIGRAM, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            
            if (result == -1) {
                // Already exists, increment frequency
                db.execSQL("UPDATE " + TABLE_BIGRAM + 
                        " SET " + COL_FREQUENCY + " = " + COL_FREQUENCY + " + 1, " +
                        COL_LAST_USED + " = ? " +
                        " WHERE " + COL_WORD1 + " = ? AND " + COL_WORD2 + " = ?",
                        new Object[]{System.currentTimeMillis(), word1, word2});
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving bigram", e);
        }
    }
    
    /**
     * Trigram ilişkisi kaydet veya güncelle
     * Örnek: "merhaba" + "nasılsın" → "nasıl"
     */
    public void saveTrigram(String word1, String word2, String word3) {
        if (word1 == null || word2 == null || word3 == null || 
            word1.isEmpty() || word2.isEmpty() || word3.isEmpty()) {
            return;
        }
        
        word1 = word1.toLowerCase().trim();
        word2 = word2.toLowerCase().trim();
        word3 = word3.toLowerCase().trim();
        
        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COL_WORD1_TRI, word1);
            values.put(COL_WORD2_TRI, word2);
            values.put(COL_WORD3_TRI, word3);
            values.put(COL_FREQUENCY_TRI, 1);
            values.put(COL_LAST_USED_TRI, System.currentTimeMillis());
            
            long result = db.insertWithOnConflict(TABLE_TRIGRAM, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            
            if (result == -1) {
                // Already exists, increment frequency
                db.execSQL("UPDATE " + TABLE_TRIGRAM + 
                        " SET " + COL_FREQUENCY_TRI + " = " + COL_FREQUENCY_TRI + " + 1, " +
                        COL_LAST_USED_TRI + " = ? " +
                        " WHERE " + COL_WORD1_TRI + " = ? AND " + COL_WORD2_TRI + " = ? AND " + COL_WORD3_TRI + " = ?",
                        new Object[]{System.currentTimeMillis(), word1, word2, word3});
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving trigram", e);
        }
    }
    
    /**
     * Bigram tahminleri al (tek kelime context)
     * Örnek: "merhaba" → ["nasılsın", "dostum", "arkadaşım"]
     */
    public List<String> getBigramPredictions(String word1, int limit) {
        List<String> predictions = new ArrayList<>();
        
        if (word1 == null || word1.isEmpty()) {
            return predictions;
        }
        
        word1 = word1.toLowerCase().trim();
        
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        
        try {
            cursor = db.query(
                    TABLE_BIGRAM,
                    new String[]{COL_WORD2, COL_FREQUENCY, COL_LAST_USED},
                    COL_WORD1 + " = ?",
                    new String[]{word1},
                    null, null,
                    COL_FREQUENCY + " DESC, " + COL_LAST_USED + " DESC",
                    String.valueOf(limit)
            );
            
            while (cursor.moveToNext()) {
                String word2 = cursor.getString(0);
                predictions.add(word2);
            }
            
            Log.d(TAG, "Bigram predictions for '" + word1 + "': " + predictions);
        } catch (Exception e) {
            Log.e(TAG, "Error getting bigram predictions", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        
        return predictions;
    }
    
    /**
     * Trigram tahminleri al (iki kelime context) - DAHA AKILLI
     * Örnek: "merhaba" + "nasılsın" → ["nasıl", "iyi", "iyiyim"]
     */
    public List<String> getTrigramPredictions(String word1, String word2, int limit) {
        List<String> predictions = new ArrayList<>();
        
        if (word1 == null || word2 == null || word1.isEmpty() || word2.isEmpty()) {
            return predictions;
        }
        
        word1 = word1.toLowerCase().trim();
        word2 = word2.toLowerCase().trim();
        
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        
        try {
            cursor = db.query(
                    TABLE_TRIGRAM,
                    new String[]{COL_WORD3_TRI, COL_FREQUENCY_TRI, COL_LAST_USED_TRI},
                    COL_WORD1_TRI + " = ? AND " + COL_WORD2_TRI + " = ?",
                    new String[]{word1, word2},
                    null, null,
                    COL_FREQUENCY_TRI + " DESC, " + COL_LAST_USED_TRI + " DESC",
                    String.valueOf(limit)
            );
            
            while (cursor.moveToNext()) {
                String word3 = cursor.getString(0);
                predictions.add(word3);
            }
            
            Log.d(TAG, "Trigram predictions for '" + word1 + " " + word2 + "': " + predictions);
        } catch (Exception e) {
            Log.e(TAG, "Error getting trigram predictions", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        
        return predictions;
    }
    
    /**
     * Akıllı tahmin - Trigram önce, bulamazsa bigram
     */
    public List<String> getSmartPredictions(String prevWord1, String prevWord2, int limit) {
        List<String> predictions = new ArrayList<>();
        
        // 1. Önce trigram dene (2 kelime context)
        if (prevWord1 != null && prevWord2 != null) {
            predictions.addAll(getTrigramPredictions(prevWord1, prevWord2, limit));
        }
        
        // 2. Yeterli değilse bigram ekle (1 kelime context)
        if (predictions.size() < limit && prevWord2 != null) {
            List<String> bigramPreds = getBigramPredictions(prevWord2, limit - predictions.size());
            for (String pred : bigramPreds) {
                if (!predictions.contains(pred)) {
                    predictions.add(pred);
                }
            }
        }
        
        return predictions;
    }
    
    /**
     * Veritabanı istatistikleri
     */
    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor c1 = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_BIGRAM, null);
        if (c1.moveToFirst()) {
            stats.put("bigrams", c1.getInt(0));
        }
        c1.close();
        
        Cursor c2 = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TRIGRAM, null);
        if (c2.moveToFirst()) {
            stats.put("trigrams", c2.getInt(0));
        }
        c2.close();
        
        return stats;
    }
    
    /**
     * Eski verileri temizle (performans için)
     */
    public void cleanOldData(long daysOld) {
        SQLiteDatabase db = getWritableDatabase();
        long cutoff = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000);
        
        db.delete(TABLE_BIGRAM, COL_FREQUENCY + " < 2 AND " + COL_LAST_USED + " < ?", 
                new String[]{String.valueOf(cutoff)});
        db.delete(TABLE_TRIGRAM, COL_FREQUENCY_TRI + " < 2 AND " + COL_LAST_USED_TRI + " < ?", 
                new String[]{String.valueOf(cutoff)});
        
        Log.d(TAG, "🧹 Old data cleaned");
    }
}








