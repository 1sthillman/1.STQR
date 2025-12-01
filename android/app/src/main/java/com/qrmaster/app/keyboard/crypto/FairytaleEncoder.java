package com.qrmaster.app.keyboard.crypto;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * 📖 FAIRYTALE MODE - Gizli Mesaj Steganografi
 * 
 * Şifreli mesajı normal görünen bir metin içine gizler.
 * Zero-width characters kullanarak invisible embedding yapar.
 * 
 * Örnek:
 * Gerçek: "Yarın saat 3"
 * Fairytale: "Bugün hava çok güzel, sanırım yağmur yağacak"
 *            (içinde gizli: şifreli mesaj)
 */
public class FairytaleEncoder {
    private static final String TAG = "FairytaleEncoder";
    
    // Zero-width characters (görünmez karakterler)
    private static final char ZERO_WIDTH_SPACE = '\u200B';      // ZWSP
    private static final char ZERO_WIDTH_NON_JOINER = '\u200C'; // ZWNJ
    private static final char ZERO_WIDTH_JOINER = '\u200D';     // ZWJ
    
    // Fairytale template'leri (Türkçe)
    private static final String[] TEMPLATES = {
        "Bugün hava çok güzel. %s Sanırım yağmur yağacak.",
        "Dün markete gittim. %s Çok kalabalıktı.",
        "Film izledim akşam. %s Çok güzeldi.",
        "Kahvaltıda yumurta yedim. %s Çok lezzetliydi.",
        "Kitap okuyorum şu sıralar. %s Çok heyecanlı.",
        "Spor salonuna gittim bugün. %s Yoruldum ama keyifliydi.",
        "Müzik dinledim sabah sabah. %s Harika başladı güne.",
        "Arkadaşımla buluştum. %s Çok eğlenceliydi.",
        "Yeni bir oyun aldım. %s Çok bağımlılık yapıyor.",
        "Pasta yaptım evde. %s Çok güzel oldu.",
        "Bahçeye çıktım biraz. %s Hava çok temizdi.",
        "İnternette gezindim. %s İlginç şeyler buldum.",
        "Telefonda konuştum annemle. %s Her şey yolunda.",
        "Çay içiyorum şu an. %s Çok sıcak ve güzel.",
        "Fotoğraf çektim dışarıda. %s Manzara muhteşemdi.",
    };
    
    /**
     * ŞİFRELİ MESAJI FAİRYTALE'E GİZLE
     * 
     * @param encryptedMessage Şifreli mesaj (ENC:...)
     * @return Normal görünen ama içinde gizli mesaj olan metin
     */
    public static String encode(String encryptedMessage) {
        try {
            Log.d(TAG, "📖 Fairytale encoding başladı");
            Log.d(TAG, "Encrypted: " + encryptedMessage);
            
            // 1. Şifreli mesajı Base64'le compact hale getir
            String compactData = Base64.encodeToString(
                encryptedMessage.getBytes(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            );
            Log.d(TAG, "Compact (Base64): " + compactData.length() + " chars");
            
            // 2. Zero-width karakterlerle binary encoding yap
            String hiddenMarker = encodeToZeroWidth(compactData);
            Log.d(TAG, "Hidden marker: " + hiddenMarker.length() + " chars (invisible)");
            
            // 3. Random template seç
            Random random = new Random(encryptedMessage.hashCode()); // Deterministic
            String template = TEMPLATES[random.nextInt(TEMPLATES.length)];
            
            // 4. Template'e gizli marker'ı yerleştir
            String fairytale = String.format(template, hiddenMarker);
            
            Log.d(TAG, "✅ Fairytale oluşturuldu: " + fairytale);
            Log.d(TAG, "Görünen uzunluk: " + fairytale.replaceAll("[\\u200B\\u200C\\u200D]", "").length() + " chars");
            Log.d(TAG, "Gerçek uzunluk: " + fairytale.length() + " chars");
            
            return fairytale;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Fairytale encoding hatası", e);
            return encryptedMessage; // Fallback: gizleyemezse direk döndür
        }
    }
    
    /**
     * FAİRYTALE'DEN ŞİFRELİ MESAJI ÇIKAR
     * 
     * @param fairytaleText Gizli mesaj içeren metin
     * @return Şifreli mesaj (ENC:...) veya null
     */
    public static String decode(String fairytaleText) {
        try {
            Log.d(TAG, "📖 Fairytale decoding başladı");
            Log.d(TAG, "Input: " + fairytaleText);
            
            // 1. Zero-width karakterleri çıkar
            String hiddenMarker = extractZeroWidth(fairytaleText);
            
            if (hiddenMarker == null || hiddenMarker.isEmpty()) {
                Log.w(TAG, "❌ Zero-width karakterler bulunamadı");
                return null;
            }
            
            Log.d(TAG, "Hidden marker bulundu: " + hiddenMarker.length() + " chars");
            
            // 2. Binary'den Base64'e decode et
            String compactData = decodeFromZeroWidth(hiddenMarker);
            
            if (compactData == null || compactData.isEmpty()) {
                Log.w(TAG, "❌ Binary decode başarısız");
                return null;
            }
            
            Log.d(TAG, "Compact data: " + compactData.length() + " chars");
            
            // 3. Base64'den şifreli mesajı geri al
            byte[] decodedBytes = Base64.decode(compactData, Base64.NO_WRAP);
            String encryptedMessage = new String(decodedBytes, StandardCharsets.UTF_8);
            
            Log.d(TAG, "✅ Encrypted message geri alındı: " + encryptedMessage);
            
            return encryptedMessage;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Fairytale decoding hatası", e);
            return null;
        }
    }
    
    /**
     * String'i zero-width karakterlere encode et
     * Her karakter → 8-bit binary → ZWSP/ZWNJ ile temsil
     */
    private static String encodeToZeroWidth(String text) {
        StringBuilder result = new StringBuilder();
        
        for (char c : text.toCharArray()) {
            // Karakteri 8-bit binary'ye çevir
            String binary = String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0');
            
            // Her bit'i zero-width karaktere map et
            for (char bit : binary.toCharArray()) {
                if (bit == '0') {
                    result.append(ZERO_WIDTH_SPACE);      // 0 → ZWSP
                } else {
                    result.append(ZERO_WIDTH_NON_JOINER); // 1 → ZWNJ
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Zero-width karakterlerden String'e decode et
     */
    private static String decodeFromZeroWidth(String encoded) {
        StringBuilder result = new StringBuilder();
        StringBuilder binary = new StringBuilder();
        
        for (char c : encoded.toCharArray()) {
            if (c == ZERO_WIDTH_SPACE) {
                binary.append('0');
            } else if (c == ZERO_WIDTH_NON_JOINER) {
                binary.append('1');
            } else if (c == ZERO_WIDTH_JOINER) {
                // Separator (kullanılmıyor şu an)
                continue;
            }
            
            // Her 8 bit'te bir karakter oluştur
            if (binary.length() == 8) {
                int charCode = Integer.parseInt(binary.toString(), 2);
                result.append((char) charCode);
                binary.setLength(0);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Metinden zero-width karakterleri çıkar
     */
    private static String extractZeroWidth(String text) {
        StringBuilder result = new StringBuilder();
        
        for (char c : text.toCharArray()) {
            if (c == ZERO_WIDTH_SPACE || 
                c == ZERO_WIDTH_NON_JOINER || 
                c == ZERO_WIDTH_JOINER) {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Metinde gizli mesaj var mı kontrol et
     */
    public static boolean hasFairytale(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // Zero-width karakterlerin varlığını kontrol et
        int zwCount = 0;
        for (char c : text.toCharArray()) {
            if (c == ZERO_WIDTH_SPACE || 
                c == ZERO_WIDTH_NON_JOINER || 
                c == ZERO_WIDTH_JOINER) {
                zwCount++;
            }
        }
        
        // En az 80 zero-width karakter varsa muhtemelen fairytale
        // (10 karakter = 80 bit minimum)
        return zwCount >= 80;
    }
    
    /**
     * Fairytale metni test et
     */
    public static void test() {
        Log.d(TAG, "========== FAIRYTALE TEST ==========");
        
        // Test mesajı
        String original = "ENC:MTIzNDU2Nzg5MA==";
        Log.d(TAG, "Original: " + original);
        
        // Encode
        String fairytale = encode(original);
        Log.d(TAG, "Fairytale: " + fairytale);
        Log.d(TAG, "Has fairytale: " + hasFairytale(fairytale));
        
        // Decode
        String decoded = decode(fairytale);
        Log.d(TAG, "Decoded: " + decoded);
        
        // Verify
        boolean success = original.equals(decoded);
        Log.d(TAG, success ? "✅ TEST BAŞARILI!" : "❌ TEST BAŞARISIZ!");
        
        Log.d(TAG, "====================================");
    }
}





