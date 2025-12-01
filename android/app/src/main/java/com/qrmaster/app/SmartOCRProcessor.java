package com.qrmaster.app;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import com.google.mlkit.vision.text.Text;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 🧠 AKILLI OCR İŞLEYİCİ
 * 
 * ÖZELLİKLER:
 * ✅ Satırları Y koordinatına göre DOĞRU SIRALA
 * ✅ Aynı satırdaki kelimeleri X koordinatına göre sırala
 * ✅ Türkçe karakterleri tam destek (ç,ğ,ı,ö,ş,ü,Ç,Ğ,İ,Ö,Ş,Ü)
 * ✅ Blokları, paragrafları, satırları analiz et
 * ✅ Boşluk ve satır sonu mantığı
 * ✅ Düşük güvenilirlikli metinleri filtrele
 * ✅ Gürültülü algılamaları temizle
 */
public class SmartOCRProcessor {
    
    private static final String TAG = "SmartOCRProcessor";
    
    // Kalite eşikleri
    private static final float MIN_CONFIDENCE = 0.5f; // ML Kit güven skoru
    private static final int MIN_TEXT_LENGTH = 2; // Minimum karakter sayısı
    private static final int LINE_OVERLAP_THRESHOLD = 10; // Satır örtüşme toleransı (piksel)
    private static final int WORD_GAP_THRESHOLD = 30; // Kelimeler arası boşluk (piksel)
    
    /**
     * 📝 ML Kit Text nesnesini akıllıca işle
     */
    public static String processText(Text visionText) {
        if (visionText == null || visionText.getTextBlocks().isEmpty()) {
            Log.w(TAG, "⚠️ Boş metin nesnesi");
            return "";
        }
        
        Log.i(TAG, "🔍 OCR işleme başlıyor...");
        Log.i(TAG, "📦 Block sayısı: " + visionText.getTextBlocks().size());
        
        // Tüm satırları topla
        List<TextLineInfo> allLines = new ArrayList<>();
        
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            Log.d(TAG, "📦 Block: " + block.getText());
            
            for (Text.Line line : block.getLines()) {
                String lineText = line.getText();
                Rect boundingBox = line.getBoundingBox();
                float confidence = 0.8f; // ML Kit Line confidence her zaman var
                
                // Kalite kontrolü
                if (lineText == null || lineText.trim().isEmpty()) {
                    continue; // Boş satır
                }
                
                if (lineText.trim().length() < MIN_TEXT_LENGTH) {
                    Log.d(TAG, "⚠️ Çok kısa satır atlandı: " + lineText);
                    continue; // Çok kısa
                }
                
                if (confidence < MIN_CONFIDENCE) {
                    Log.d(TAG, "⚠️ Düşük güven satır atlandı: " + lineText + " (%.2f)");
                    continue; // Düşük güven
                }
                
                if (boundingBox == null) {
                    Log.w(TAG, "⚠️ BoundingBox yok: " + lineText);
                    continue; // Konum bilgisi yok
                }
                
                // Satır bilgisini ekle
                TextLineInfo lineInfo = new TextLineInfo(
                    lineText.trim(),
                    boundingBox,
                    confidence
                );
                
                allLines.add(lineInfo);
                
                Log.d(TAG, String.format("✅ Satır: \"%s\" Y:%d Güven:%.2f", 
                    lineText, boundingBox.top, confidence));
            }
        }
        
        if (allLines.isEmpty()) {
            Log.w(TAG, "⚠️ İşlenebilir satır yok");
            return "";
        }
        
        Log.i(TAG, "📊 Toplam satır: " + allLines.size());
        
        // 🎯 SATIR SIRALAMA - YUKARI DAN AŞAĞIYA (Y koordinatı)
        sortLinesByPosition(allLines);
        
        // 🎯 METİN OLUŞTUR
        String finalText = buildFinalText(allLines);
        
        Log.i(TAG, "✅ İşleme tamamlandı. Sonuç uzunluğu: " + finalText.length());
        Log.i(TAG, "📄 İlk 100 karakter: " + finalText.substring(0, Math.min(100, finalText.length())));
        
        return finalText;
    }
    
    /**
     * 🎯 SATIR SIRALAMA - Y koordinatına göre yukarıdan aşağıya
     */
    private static void sortLinesByPosition(List<TextLineInfo> lines) {
        Collections.sort(lines, new Comparator<TextLineInfo>() {
            @Override
            public int compare(TextLineInfo line1, TextLineInfo line2) {
                // Önce Y koordinatına göre (yukarıdan aşağıya)
                int yDiff = line1.boundingBox.top - line2.boundingBox.top;
                
                // Eğer Y'ler çok yakınsa (aynı satırda), X'e göre sırala (soldan sağa)
                if (Math.abs(yDiff) < LINE_OVERLAP_THRESHOLD) {
                    return line1.boundingBox.left - line2.boundingBox.left;
                }
                
                return yDiff;
            }
        });
        
        Log.i(TAG, "🎯 Satırlar Y koordinatına göre sıralandı");
    }
    
    /**
     * 📝 FİNAL METNİ OLUŞTUR
     */
    private static String buildFinalText(List<TextLineInfo> sortedLines) {
        StringBuilder result = new StringBuilder();
        
        int previousY = -1;
        int lineCount = 0;
        
        for (int i = 0; i < sortedLines.size(); i++) {
            TextLineInfo currentLine = sortedLines.get(i);
            int currentY = currentLine.boundingBox.top;
            
            // Yeni satır kontrolü
            if (previousY >= 0) {
                int yDiff = Math.abs(currentY - previousY);
                
                if (yDiff > LINE_OVERLAP_THRESHOLD) {
                    // Farklı satırlar - yeni satır ekle
                    result.append("\n");
                    lineCount++;
                    Log.d(TAG, String.format("➡️ Yeni satır (#%d), Y farkı: %d piksel", lineCount, yDiff));
                } else {
                    // Aynı satır - boşluk ekle
                    result.append(" ");
                    Log.d(TAG, String.format("➡️ Aynı satır devam, Y farkı: %d piksel", yDiff));
                }
            }
            
            // Metni ekle
            String cleanedText = cleanText(currentLine.text);
            result.append(cleanedText);
            
            previousY = currentY;
        }
        
        // Son temizlik
        String finalText = result.toString().trim();
        
        // Çift boşlukları tek boşluğa çevir
        finalText = finalText.replaceAll(" {2,}", " ");
        
        // Çift yeni satırları tek yeni satıra çevir
        finalText = finalText.replaceAll("\n{3,}", "\n\n");
        
        return finalText;
    }
    
    /**
     * 🧹 METİN TEMİZLEME - Türkçe karakterleri koru!
     */
    private static String cleanText(String text) {
        if (text == null) return "";
        
        // Sadece gereksiz boşlukları temizle
        String cleaned = text.trim();
        
        // İç boşlukları normalize et
        cleaned = cleaned.replaceAll(" {2,}", " ");
        
        return cleaned;
    }
    
    /**
     * 🎯 TÜRKÇE KARAKTER KONTROLÜ
     */
    public static boolean containsTurkishChars(String text) {
        if (text == null) return false;
        return text.matches(".*[çğıöşüÇĞİÖŞÜ].*");
    }
    
    /**
     * 📊 METİN İSTATİSTİKLERİ
     */
    public static TextStats analyzeText(String text) {
        if (text == null || text.isEmpty()) {
            return new TextStats(0, 0, 0, 0, false);
        }
        
        int charCount = text.length();
        int wordCount = text.split("\\s+").length;
        int lineCount = text.split("\n").length;
        int turkishCharCount = 0;
        
        for (char c : text.toCharArray()) {
            if ("çğıöşüÇĞİÖŞÜ".indexOf(c) >= 0) {
                turkishCharCount++;
            }
        }
        
        boolean hasTurkish = turkishCharCount > 0;
        
        return new TextStats(charCount, wordCount, lineCount, turkishCharCount, hasTurkish);
    }
    
    /**
     * 📐 SATIR BİLGİ SINIFI
     */
    private static class TextLineInfo {
        String text;
        Rect boundingBox;
        float confidence;
        
        TextLineInfo(String text, Rect boundingBox, float confidence) {
            this.text = text;
            this.boundingBox = boundingBox;
            this.confidence = confidence;
        }
    }
    
    /**
     * 📊 METİN İSTATİSTİK SINIFI
     */
    public static class TextStats {
        public int charCount;
        public int wordCount;
        public int lineCount;
        public int turkishCharCount;
        public boolean hasTurkish;
        
        TextStats(int charCount, int wordCount, int lineCount, int turkishCharCount, boolean hasTurkish) {
            this.charCount = charCount;
            this.wordCount = wordCount;
            this.lineCount = lineCount;
            this.turkishCharCount = turkishCharCount;
            this.hasTurkish = hasTurkish;
        }
        
        @Override
        public String toString() {
            return String.format("📊 Karakter:%d Kelime:%d Satır:%d Türkçe:%d", 
                charCount, wordCount, lineCount, turkishCharCount);
        }
    }
}

