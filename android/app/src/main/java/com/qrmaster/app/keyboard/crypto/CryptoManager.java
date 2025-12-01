package com.qrmaster.app.keyboard.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 🔒 Şifreli Görüşme Modu - Kriptografi Motoru
 * 
 * Özellikler:
 * - RSA-4096 anahtar üretimi (AndroidKeyStore)
 * - X3DH protokolü (Extended Triple Diffie-Hellman)
 * - Double Ratchet (Perfect Forward Secrecy)
 * - AES-256-GCM şifreleme
 * - Kişi yönetimi ve oturum takibi
 */
public class CryptoManager {
    private static final String TAG = "CryptoManager";
    
    // Keystore & Preferences
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String PREFS_NAME = "crypto_prefs";
    private static final String KEY_ALIAS = "qkeyboard_master_key";
    
    // Encryption algorithms
    private static final String RSA_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    
    // Double Ratchet
    private static final int MAX_SKIP = 1000; // Max skipped message keys
    
    private final Context context;
    private final SharedPreferences prefs;
    private KeyStore keyStore;
    
    // Session management
    private Map<String, CryptoSession> activeSessions = new HashMap<>();
    
    // Premium limits
    private boolean isPremium = false;
    private static final int FREE_MAX_CONTACTS = 5;
    private static final int FREE_MAX_MESSAGES = 100;
    
    // Coercion Mode (Sahte Deşifre)
    private boolean coercionModeEnabled = false;
    private String coercionSecretPattern = ""; // Gizli kombinasyon (örn: "🔒🔒🔓🔒")
    private Map<String, String> coercionFakeMessages = new HashMap<>();
    
    public CryptoManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        try {
            keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            
            // İlk kurulum - anahtar çifti oluştur
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateMasterKeyPair();
                Log.d(TAG, "✅ Master key pair oluşturuldu");
            }
            
            // Kayıtlı oturumları yükle
            loadSessions();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Keystore init error", e);
        }
    }
    
    /**
     * Master RSA-4096 anahtar çifti oluştur
     */
    private void generateMasterKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA, 
            KEYSTORE_PROVIDER
        );
        
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT | 
            KeyProperties.PURPOSE_DECRYPT | 
            KeyProperties.PURPOSE_SIGN | 
            KeyProperties.PURPOSE_VERIFY
        )
        .setKeySize(4096) // RSA-4096
        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PSS)
        .setUserAuthenticationRequired(false) // Kolay kullanım için
        .build();
        
        kpg.initialize(spec);
        KeyPair keyPair = kpg.generateKeyPair();
        
        // Public key'i kaydet (QR/NFC için)
        String publicKeyBase64 = Base64.encodeToString(
            keyPair.getPublic().getEncoded(), 
            Base64.NO_WRAP
        );
        prefs.edit().putString("public_key", publicKeyBase64).apply();
        
        // Görsel kurtarma anahtarı oluştur
        generateRecoveryKey();
    }
    
    /**
     * Görsel kurtarma anahtarı (12 emoji)
     */
    private void generateRecoveryKey() {
        String[] emojis = {
            "🐱", "🐶", "🐼", "🦊", "🐻", "🐨", "🐯", "🦁",
            "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑",
            "🌟", "⭐", "✨", "💫", "🌙", "☀️", "⚡", "🔥",
            "🎸", "🎹", "🎺", "🎻", "🥁", "🎤", "🎧", "🎮"
        };
        
        SecureRandom random = new SecureRandom();
        StringBuilder recoveryKey = new StringBuilder();
        
        for (int i = 0; i < 12; i++) {
            recoveryKey.append(emojis[random.nextInt(emojis.length)]);
            if (i < 11) recoveryKey.append("-");
        }
        
        prefs.edit().putString("recovery_key", recoveryKey.toString()).apply();
        Log.d(TAG, "🔑 Kurtarma anahtarı: " + recoveryKey);
    }
    
    /**
     * Public key'i Base64 olarak al (QR/NFC için)
     */
    public String getPublicKeyBase64() {
        return prefs.getString("public_key", null);
    }
    
    /**
     * Public key'i QR kod için JSON formatında al
     */
    public String getPublicKeyQRPayload() {
        try {
            JSONObject payload = new JSONObject();
            payload.put("type", "QKEYBOARD_PUBLIC_KEY");
            payload.put("version", "1.0");
            payload.put("key", getPublicKeyBase64());
            payload.put("device_id", getDeviceId());
            payload.put("timestamp", System.currentTimeMillis());
            return payload.toString();
        } catch (Exception e) {
            Log.e(TAG, "QR payload error", e);
            return null;
        }
    }
    
    /**
     * Karşı tarafın public key'ini kaydet ve oturum başlat
     */
    public boolean addContact(String contactId, String publicKeyBase64) {
        try {
            // Premium kontrolü
            if (!isPremium && getContactCount() >= FREE_MAX_CONTACTS) {
                Log.w(TAG, "⚠️ Free limit: max " + FREE_MAX_CONTACTS + " contacts");
                return false;
            }
            
            // Public key'i doğrula
            byte[] publicKeyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            PublicKey contactPublicKey = kf.generatePublic(spec);
            
            // X3DH protokolü ile oturum başlat
            CryptoSession session = new CryptoSession(contactId, contactPublicKey);
            session.initializeX3DH(getPrivateKey());
            
            activeSessions.put(contactId, session);
            saveSession(session);
            
            Log.d(TAG, "✅ Kişi eklendi: " + contactId);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Add contact error", e);
            return false;
        }
    }
    
    /**
     * Metni şifrele (Double Ratchet ile) - temel versiyon
     */
    public String encrypt(String plaintext, String contactId) {
        return encrypt(plaintext, contactId, null, null, null);
    }
    
    /**
     * Metni şifrele (Double Ratchet ile) - gelişmiş versiyon
     * 
     * @param plaintext şifrelenecek metin
     * @param contactId kişi ID
     * @param unlockTime mesajın açılabileceği zaman (ms, null=hemen)
     * @param geoLat konum kilidi - enlem (null=yok)
     * @param geoLon konum kilidi - boylam (null=yok)
     */
    public String encrypt(String plaintext, String contactId, Long unlockTime, Double geoLat, Double geoLon) {
        try {
            // Premium kontrolü - mesaj limiti
            if (!isPremium && getMessageCount() >= FREE_MAX_MESSAGES) {
                Log.w(TAG, "⚠️ Free limit: max " + FREE_MAX_MESSAGES + " messages/month");
                return null;
            }
            
            // Time-Lock & Geofence - PREMIUM özelliği
            if ((unlockTime != null || geoLat != null) && !isPremium) {
                Log.w(TAG, "⚠️ Time-Lock/Geofence requires Premium");
                return null;
            }
            
            CryptoSession session = activeSessions.get(contactId);
            if (session == null) {
                Log.e(TAG, "❌ Session not found: " + contactId);
                return null;
            }
            
            // Double Ratchet - yeni mesaj anahtarı oluştur
            SecretKey messageKey = session.ratchetEncrypt();
            
            // AES-256-GCM ile şifrele
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, messageKey, gcmSpec);
            
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
            
            // Format: VERSION|SESSION_ID|MESSAGE_INDEX|IV|CIPHERTEXT|TIME_LOCK|GEOFENCE
            JSONObject encrypted = new JSONObject();
            encrypted.put("v", 1); // version
            encrypted.put("sid", session.getSessionId());
            encrypted.put("idx", session.getSendIndex());
            encrypted.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP));
            encrypted.put("ct", Base64.encodeToString(ciphertext, Base64.NO_WRAP));
            
            // Time-Lock metadata
            if (unlockTime != null) {
                encrypted.put("unlock_time", unlockTime);
            }
            
            // Geofence metadata
            if (geoLat != null && geoLon != null) {
                JSONObject geo = new JSONObject();
                geo.put("lat", geoLat);
                geo.put("lon", geoLon);
                geo.put("radius", 100); // 100 metre içinde
                encrypted.put("geo", geo);
            }
            
            String encryptedPayload = encrypted.toString();
            
            // Base64 encode - kopyala/yapıştır için
            String finalEncrypted = "QKC1" + Base64.encodeToString(
                encryptedPayload.getBytes("UTF-8"), 
                Base64.NO_WRAP | Base64.URL_SAFE
            );
            
            // Mesaj sayısını artır
            incrementMessageCount();
            
            Log.d(TAG, "🔒 Encrypted: " + plaintext.length() + " → " + finalEncrypted.length() + " bytes");
            return finalEncrypted;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Encrypt error", e);
            return null;
        }
    }
    
    /**
     * Şifreli metni çöz (Double Ratchet ile)
     * 
     * @param encryptedText şifreli mesaj
     * @param contactId kişi ID
     * @param isCoercionMode zorla çözme modu (sahte mesaj döndürür)
     */
    public String decrypt(String encryptedText, String contactId, boolean isCoercionMode) {
        try {
            // COERCION MODE: Sahte mesaj döndür
            if (isCoercionMode && coercionModeEnabled) {
                String fakeMessage = coercionFakeMessages.get(encryptedText);
                if (fakeMessage != null) {
                    Log.d(TAG, "🎭 Coercion mode: fake message returned");
                    return fakeMessage;
                } else {
                    // Varsayılan sahte mesaj
                    return "Bugün hava çok güzel, dışarı çıkmak lazım.";
                }
            }
            
            // Format kontrolü
            if (!encryptedText.startsWith("QKC1")) {
                Log.e(TAG, "❌ Invalid format");
                return null;
            }
            
            CryptoSession session = activeSessions.get(contactId);
            if (session == null) {
                Log.e(TAG, "❌ Session not found: " + contactId);
                return null;
            }
            
            // Base64 decode
            String payload = new String(
                Base64.decode(encryptedText.substring(4), Base64.NO_WRAP | Base64.URL_SAFE),
                "UTF-8"
            );
            
            JSONObject encrypted = new JSONObject(payload);
            int version = encrypted.getInt("v");
            String sessionId = encrypted.getString("sid");
            int messageIndex = encrypted.getInt("idx");
            byte[] iv = Base64.decode(encrypted.getString("iv"), Base64.NO_WRAP);
            byte[] ciphertext = Base64.decode(encrypted.getString("ct"), Base64.NO_WRAP);
            
            // Session ID kontrolü
            if (!sessionId.equals(session.getSessionId())) {
                Log.e(TAG, "❌ Session ID mismatch");
                return null;
            }
            
            // TIME-LOCK kontrolü
            if (encrypted.has("unlock_time")) {
                long unlockTime = encrypted.getLong("unlock_time");
                long now = System.currentTimeMillis();
                if (now < unlockTime) {
                    long remainingMs = unlockTime - now;
                    long remainingMinutes = remainingMs / (1000 * 60);
                    Log.w(TAG, "⏰ Time-locked: " + remainingMinutes + " dakika kaldı");
                    throw new TimeLockException("Bu mesaj henüz açılamaz. " + 
                        remainingMinutes + " dakika sonra tekrar deneyin.");
                }
            }
            
            // GEOFENCE kontrolü
            if (encrypted.has("geo")) {
                JSONObject geo = encrypted.getJSONObject("geo");
                double targetLat = geo.getDouble("lat");
                double targetLon = geo.getDouble("lon");
                int radius = geo.getInt("radius");
                
                // Cihazın mevcut konumunu al
                android.location.Location currentLocation = getCurrentLocation();
                if (currentLocation == null) {
                    Log.w(TAG, "📍 Konum alınamadı - geofence kontrolü atlandı");
                } else {
                    double distance = calculateDistance(
                        currentLocation.getLatitude(), 
                        currentLocation.getLongitude(), 
                        targetLat, 
                        targetLon
                    );
                    
                    if (distance > radius) {
                        Log.w(TAG, "📍 Geofence: Mesaj " + (int)distance + "m uzakta");
                        throw new GeofenceException("Bu mesaj sadece belirtilen konumda açılabilir. " +
                            "Hedefe " + (int)distance + " metre mesafedesiniz.");
                    }
                }
            }
            
            // Double Ratchet - mesaj anahtarını al
            SecretKey messageKey = session.ratchetDecrypt(messageIndex);
            
            // AES-256-GCM ile deşifre
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, messageKey, gcmSpec);
            
            byte[] plaintext = cipher.doFinal(ciphertext);
            String decrypted = new String(plaintext, "UTF-8");
            
            Log.d(TAG, "🔓 Decrypted: " + encryptedText.length() + " → " + decrypted.length() + " bytes");
            return decrypted;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Decrypt error", e);
            return null;
        }
    }
    
    /**
     * Otomatik şifreli metin algılama
     */
    public boolean isEncryptedText(String text) {
        return text != null && text.startsWith("QKC1") && text.length() > 20;
    }
    
    /**
     * Mesaj için en uygun contact'ı bul (otomatik deşifre için)
     */
    public String detectContact(String encryptedText) {
        try {
            if (!isEncryptedText(encryptedText)) return null;
            
            String payload = new String(
                Base64.decode(encryptedText.substring(4), Base64.NO_WRAP | Base64.URL_SAFE),
                "UTF-8"
            );
            JSONObject encrypted = new JSONObject(payload);
            String sessionId = encrypted.getString("sid");
            
            // Session ID'ye göre contact bul
            for (Map.Entry<String, CryptoSession> entry : activeSessions.entrySet()) {
                if (entry.getValue().getSessionId().equals(sessionId)) {
                    return entry.getKey();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Detect contact error", e);
        }
        return null;
    }
    
    /**
     * Private key'i al (keystore'dan)
     */
    private PrivateKey getPrivateKey() throws Exception {
        return (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
    }
    
    /**
     * Device ID (benzersiz tanımlayıcı)
     */
    private String getDeviceId() {
        String deviceId = prefs.getString("device_id", null);
        if (deviceId == null) {
            deviceId = java.util.UUID.randomUUID().toString();
            prefs.edit().putString("device_id", deviceId).apply();
        }
        return deviceId;
    }
    
    /**
     * Kayıtlı kişi sayısı
     */
    public int getContactCount() {
        return activeSessions.size();
    }
    
    /**
     * Bu ayki mesaj sayısı
     */
    private int getMessageCount() {
        long now = System.currentTimeMillis();
        long lastReset = prefs.getLong("message_count_reset", 0);
        
        // Ay başında sıfırla
        if (now - lastReset > 30L * 24 * 60 * 60 * 1000) {
            prefs.edit()
                .putInt("message_count", 0)
                .putLong("message_count_reset", now)
                .apply();
            return 0;
        }
        
        return prefs.getInt("message_count", 0);
    }
    
    private void incrementMessageCount() {
        prefs.edit().putInt("message_count", getMessageCount() + 1).apply();
    }
    
    /**
     * Premium durumunu ayarla
     */
    public void setPremium(boolean premium) {
        this.isPremium = premium;
        prefs.edit().putBoolean("is_premium", premium).apply();
    }
    
    public boolean isPremium() {
        return prefs.getBoolean("is_premium", false);
    }
    
    /**
     * Tüm kişileri al
     */
    public List<ContactInfo> getContacts() {
        List<ContactInfo> contacts = new ArrayList<>();
        for (Map.Entry<String, CryptoSession> entry : activeSessions.entrySet()) {
            CryptoSession session = entry.getValue();
            ContactInfo info = new ContactInfo();
            info.contactId = entry.getKey();
            info.sessionId = session.getSessionId();
            info.createdAt = session.getCreatedAt();
            info.lastMessageAt = session.getLastMessageAt();
            info.messageCount = session.getMessageCount();
            contacts.add(info);
        }
        return contacts;
    }
    
    /**
     * Kişiyi sil
     */
    public boolean removeContact(String contactId) {
        activeSessions.remove(contactId);
        deleteSession(contactId);
        Log.d(TAG, "🗑️ Kişi silindi: " + contactId);
        return true;
    }
    
    /**
     * Şifreli metni çöz (backward compatibility - normal mode)
     */
    public String decrypt(String encryptedText, String contactId) {
        return decrypt(encryptedText, contactId, false);
    }
    
    /**
     * Coercion Mode'u aktif et
     * 
     * @param secretPattern gizli kombinasyon (örn: "🔒🔒🔓🔒")
     */
    public void enableCoercionMode(String secretPattern) {
        this.coercionModeEnabled = true;
        this.coercionSecretPattern = secretPattern;
        prefs.edit()
            .putBoolean("coercion_enabled", true)
            .putString("coercion_pattern", secretPattern)
            .apply();
        Log.d(TAG, "🎭 Coercion mode enabled with pattern: " + secretPattern);
    }
    
    /**
     * Coercion Mode'u kapat
     */
    public void disableCoercionMode() {
        this.coercionModeEnabled = false;
        this.coercionSecretPattern = "";
        coercionFakeMessages.clear();
        prefs.edit()
            .putBoolean("coercion_enabled", false)
            .putString("coercion_pattern", "")
            .apply();
        Log.d(TAG, "🎭 Coercion mode disabled");
    }
    
    /**
     * Sahte mesaj ekle (şifreli mesaj için masum alternatif)
     */
    public void addFakeMessage(String encryptedText, String fakeMessage) {
        coercionFakeMessages.put(encryptedText, fakeMessage);
        Log.d(TAG, "🎭 Fake message added for encrypted text");
    }
    
    /**
     * Gizli kombinasyonu kontrol et (UI'da kullanılacak)
     */
    public boolean checkCoercionPattern(String inputPattern) {
        if (!coercionModeEnabled) return false;
        boolean match = coercionSecretPattern.equals(inputPattern);
        Log.d(TAG, "🎭 Pattern check: " + (match ? "MATCH" : "NO MATCH"));
        return match;
    }
    
    /**
     * Coercion Mode aktif mi?
     */
    public boolean isCoercionModeEnabled() {
        return coercionModeEnabled;
    }
    
    /**
     * Kurtarma anahtarını göster
     */
    public String getRecoveryKey() {
        return prefs.getString("recovery_key", "");
    }
    
    /**
     * Oturumları kaydet/yükle
     */
    private void saveSession(CryptoSession session) {
        try {
            String json = session.toJSON().toString();
            prefs.edit().putString("session_" + session.getContactId(), json).apply();
        } catch (Exception e) {
            Log.e(TAG, "Save session error", e);
        }
    }
    
    private void loadSessions() {
        Map<String, ?> allPrefs = prefs.getAll();
        for (String key : allPrefs.keySet()) {
            if (key.startsWith("session_")) {
                try {
                    String json = (String) allPrefs.get(key);
                    CryptoSession session = CryptoSession.fromJSON(new JSONObject(json));
                    activeSessions.put(session.getContactId(), session);
                } catch (Exception e) {
                    Log.e(TAG, "Load session error: " + key, e);
                }
            }
        }
        Log.d(TAG, "✅ " + activeSessions.size() + " oturum yüklendi");
    }
    
    private void deleteSession(String contactId) {
        prefs.edit().remove("session_" + contactId).apply();
    }
    
    /**
     * Mevcut konumu al (konum izni gerekli)
     */
    private android.location.Location getCurrentLocation() {
        try {
            android.location.LocationManager locationManager = 
                (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            
            if (locationManager == null) return null;
            
            // Son bilinen konumu al (daha hızlı)
            android.location.Location location = locationManager.getLastKnownLocation(
                android.location.LocationManager.GPS_PROVIDER
            );
            
            if (location == null) {
                location = locationManager.getLastKnownLocation(
                    android.location.LocationManager.NETWORK_PROVIDER
                );
            }
            
            return location;
            
        } catch (SecurityException e) {
            Log.e(TAG, "Konum izni yok", e);
            return null;
        }
    }
    
    /**
     * İki GPS koordinatı arasındaki mesafeyi hesapla (Haversine formülü)
     * @return metre cinsinden mesafe
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Dünya yarıçapı (metre)
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Metre
    }
    
    /**
     * Contact bilgileri (UI için)
     */
    public static class ContactInfo {
        public String contactId;
        public String sessionId;
        public long createdAt;
        public long lastMessageAt;
        public int messageCount;
    }
    
    /**
     * Time-Lock exception
     */
    public static class TimeLockException extends Exception {
        public TimeLockException(String message) {
            super(message);
        }
    }
    
    /**
     * Geofence exception
     */
    public static class GeofenceException extends Exception {
        public GeofenceException(String message) {
            super(message);
        }
    }
}
