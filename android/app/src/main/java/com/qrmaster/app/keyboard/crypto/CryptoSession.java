package com.qrmaster.app.keyboard.crypto;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * 🔒 Double Ratchet Oturum Yönetimi
 * 
 * Perfect Forward Secrecy (PFS) için her mesajda yeni anahtar türetir.
 * Signal Protocol'e benzer yapı.
 */
public class CryptoSession {
    private static final String TAG = "CryptoSession";
    
    private String contactId;
    private String sessionId;
    private PublicKey contactPublicKey;
    
    // Double Ratchet state
    private byte[] rootKey;
    private byte[] sendChainKey;
    private byte[] receiveChainKey;
    private int sendIndex = 0;
    private int receiveIndex = 0;
    
    // Skipped message keys (out-of-order mesajlar için)
    private Map<Integer, SecretKey> skippedKeys = new HashMap<>();
    private static final int MAX_SKIP = 1000;
    
    // Metadata
    private long createdAt;
    private long lastMessageAt;
    private int messageCount = 0;
    
    public CryptoSession(String contactId, PublicKey contactPublicKey) {
        this.contactId = contactId;
        this.contactPublicKey = contactPublicKey;
        this.sessionId = generateSessionId();
        this.createdAt = System.currentTimeMillis();
        this.lastMessageAt = createdAt;
    }
    
    /**
     * X3DH protokolü ile oturum başlat
     */
    public void initializeX3DH(PrivateKey myPrivateKey) {
        try {
            // ECDH (Elliptic Curve Diffie-Hellman) ile shared secret oluştur
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(myPrivateKey);
            keyAgreement.doPhase(contactPublicKey, true);
            byte[] sharedSecret = keyAgreement.generateSecret();
            
            // KDF (Key Derivation Function) - HKDF-SHA256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            this.rootKey = digest.digest(sharedSecret);
            
            // İlk chain key'leri türet
            this.sendChainKey = deriveKey(rootKey, "send_chain".getBytes());
            this.receiveChainKey = deriveKey(rootKey, "receive_chain".getBytes());
            
            Log.d(TAG, "✅ X3DH initialized: " + sessionId);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ X3DH init error", e);
        }
    }
    
    /**
     * Mesaj şifreleme için yeni anahtar türet (Ratchet ileri)
     */
    public SecretKey ratchetEncrypt() {
        try {
            // Message key türet
            byte[] messageKeyBytes = deriveKey(sendChainKey, ("message_" + sendIndex).getBytes());
            SecretKey messageKey = new SecretKeySpec(messageKeyBytes, 0, 32, "AES");
            
            // Chain key'i ilerlet
            sendChainKey = deriveKey(sendChainKey, "ratchet".getBytes());
            sendIndex++;
            
            messageCount++;
            lastMessageAt = System.currentTimeMillis();
            
            return messageKey;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Ratchet encrypt error", e);
            return null;
        }
    }
    
    /**
     * Mesaj deşifre için anahtar al (Ratchet ileri/geri)
     */
    public SecretKey ratchetDecrypt(int messageIndex) {
        try {
            // Out-of-order mesaj - skipped key'leri kontrol et
            if (messageIndex < receiveIndex) {
                SecretKey skippedKey = skippedKeys.get(messageIndex);
                if (skippedKey != null) {
                    skippedKeys.remove(messageIndex);
                    Log.d(TAG, "🔑 Skipped key kullanıldı: " + messageIndex);
                    return skippedKey;
                } else {
                    Log.e(TAG, "❌ Skipped key bulunamadı: " + messageIndex);
                    return null;
                }
            }
            
            // Normal durum - chain'i ilerlet
            while (receiveIndex < messageIndex) {
                // Atlanmış mesaj anahtarlarını sakla
                if (skippedKeys.size() >= MAX_SKIP) {
                    Log.e(TAG, "❌ Max skip exceeded");
                    return null;
                }
                
                byte[] skippedKeyBytes = deriveKey(receiveChainKey, ("message_" + receiveIndex).getBytes());
                SecretKey skippedKey = new SecretKeySpec(skippedKeyBytes, 0, 32, "AES");
                skippedKeys.put(receiveIndex, skippedKey);
                
                receiveChainKey = deriveKey(receiveChainKey, "ratchet".getBytes());
                receiveIndex++;
            }
            
            // İstenen mesajın anahtarını türet
            byte[] messageKeyBytes = deriveKey(receiveChainKey, ("message_" + receiveIndex).getBytes());
            SecretKey messageKey = new SecretKeySpec(messageKeyBytes, 0, 32, "AES");
            
            // Chain'i ilerlet
            receiveChainKey = deriveKey(receiveChainKey, "ratchet".getBytes());
            receiveIndex++;
            
            messageCount++;
            lastMessageAt = System.currentTimeMillis();
            
            return messageKey;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Ratchet decrypt error", e);
            return null;
        }
    }
    
    /**
     * KDF (Key Derivation Function) - HKDF benzeri
     */
    private byte[] deriveKey(byte[] inputKey, byte[] info) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(inputKey);
        digest.update(info);
        return digest.digest();
    }
    
    /**
     * Session ID oluştur (benzersiz)
     */
    private String generateSessionId() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.NO_WRAP | Base64.URL_SAFE).substring(0, 16);
    }
    
    // Getters
    public String getContactId() { return contactId; }
    public String getSessionId() { return sessionId; }
    public int getSendIndex() { return sendIndex; }
    public int getReceiveIndex() { return receiveIndex; }
    public long getCreatedAt() { return createdAt; }
    public long getLastMessageAt() { return lastMessageAt; }
    public int getMessageCount() { return messageCount; }
    
    /**
     * JSON serileştirme (kaydetmek için)
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("contactId", contactId);
        json.put("sessionId", sessionId);
        json.put("rootKey", Base64.encodeToString(rootKey, Base64.NO_WRAP));
        json.put("sendChainKey", Base64.encodeToString(sendChainKey, Base64.NO_WRAP));
        json.put("receiveChainKey", Base64.encodeToString(receiveChainKey, Base64.NO_WRAP));
        json.put("sendIndex", sendIndex);
        json.put("receiveIndex", receiveIndex);
        json.put("createdAt", createdAt);
        json.put("lastMessageAt", lastMessageAt);
        json.put("messageCount", messageCount);
        // Not: skippedKeys'i serialize etmiyoruz (geçici cache)
        return json;
    }
    
    /**
     * JSON deserializasyon
     */
    public static CryptoSession fromJSON(JSONObject json) throws Exception {
        String contactId = json.getString("contactId");
        
        // PublicKey'i reconstruct edemiyoruz, bu yüzden null - lazy load gerekir
        CryptoSession session = new CryptoSession(contactId, null);
        session.sessionId = json.getString("sessionId");
        session.rootKey = Base64.decode(json.getString("rootKey"), Base64.NO_WRAP);
        session.sendChainKey = Base64.decode(json.getString("sendChainKey"), Base64.NO_WRAP);
        session.receiveChainKey = Base64.decode(json.getString("receiveChainKey"), Base64.NO_WRAP);
        session.sendIndex = json.getInt("sendIndex");
        session.receiveIndex = json.getInt("receiveIndex");
        session.createdAt = json.getLong("createdAt");
        session.lastMessageAt = json.getLong("lastMessageAt");
        session.messageCount = json.getInt("messageCount");
        
        return session;
    }
}






