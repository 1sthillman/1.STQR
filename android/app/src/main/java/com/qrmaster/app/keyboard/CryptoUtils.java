package com.qrmaster.app.keyboard;

import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Metin Şifreleme Yardımcısı
 * KryptEY'den esinlenildi
 */
public class CryptoUtils {
    
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int IV_LENGTH = 16;
    
    /**
     * Metni şifreler (AES-256)
     */
    public static String encrypt(String plainText, String password) {
        try {
            // Password'den key oluştur
            byte[] key = generateKey(password);
            
            // IV oluştur
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // Şifreleme
            SecretKeySpec secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            // IV + Encrypted data birleştir ve Base64'e çevir
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);
            
            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Şifrelenmiş metni çözer
     */
    public static String decrypt(String encryptedText, String password) {
        try {
            // Base64'den byte array'e çevir
            byte[] combined = Base64.decode(encryptedText, Base64.NO_WRAP);
            
            if (combined.length < IV_LENGTH) {
                return null;
            }
            
            // IV'yi ayır
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // Encrypted data'yı ayır
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);
            
            // Password'den key oluştur
            byte[] key = generateKey(password);
            
            // Şifre çözme
            SecretKeySpec secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Base64 encode (basit)
     */
    public static String encodeBase64(String plainText) {
        try {
            byte[] data = plainText.getBytes(StandardCharsets.UTF_8);
            return Base64.encodeToString(data, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Base64 decode (basit)
     */
    public static String decodeBase64(String encodedText) {
        try {
            byte[] data = Base64.decode(encodedText, Base64.NO_WRAP);
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Password'den 256-bit key oluştur (SHA-256)
     */
    private static byte[] generateKey(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(password.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Hızlı ROT13 şifreleme (basit Caesar cipher)
     */
    public static String rot13(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                result.append((char) ('a' + (c - 'a' + 13) % 26));
            } else if (c >= 'A' && c <= 'Z') {
                result.append((char) ('A' + (c - 'A' + 13) % 26));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}








