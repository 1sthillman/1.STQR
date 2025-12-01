package com.qrmaster.app.keyboard.crypto;

import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * BASİT ŞİFRELEME - Karmaşık değil, ÇALIŞIYOR!
 */
public class SimpleCrypto {
    private static final String TAG = "SimpleCrypto";
    private static final String ALGORITHM = "AES";
    
    /**
     * Şifrele (şifre ile)
     */
    public static String encrypt(String plaintext, String password) {
        try {
            SecretKey key = generateKey(password);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            String result = "ENC:" + Base64.encodeToString(encrypted, Base64.NO_WRAP);
            Log.d(TAG, "✅ Şifrelendi: " + plaintext.length() + " → " + result.length() + " bytes");
            return result;
        } catch (Exception e) {
            Log.e(TAG, "❌ Şifreleme hatası", e);
            return null;
        }
    }
    
    /**
     * Deşifre et (şifre ile)
     */
    public static String decrypt(String encrypted, String password) {
        try {
            if (!encrypted.startsWith("ENC:")) {
                Log.e(TAG, "❌ Geçersiz format");
                return null;
            }
            
            String base64 = encrypted.substring(4);
            byte[] encryptedBytes = Base64.decode(base64, Base64.NO_WRAP);
            
            SecretKey key = generateKey(password);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(encryptedBytes);
            String result = new String(decrypted, StandardCharsets.UTF_8);
            Log.d(TAG, "✅ Deşifre edildi: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "❌ Deşifre hatası", e);
            return null;
        }
    }
    
    /**
     * Şifreden anahtar üret
     */
    private static SecretKey generateKey(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        byte[] key = new byte[16]; // AES-128 (16 byte)
        System.arraycopy(hash, 0, key, 0, 16);
        return new SecretKeySpec(key, ALGORITHM);
    }
    
    /**
     * Şifreli mi kontrol et
     */
    public static boolean isEncrypted(String text) {
        return text != null && text.startsWith("ENC:");
    }
}






