package com.qrmaster.app.keyboard.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 📨 MESSAGE LOG - Mesaj Geçmişi Yönetimi
 * 
 * Gönderilen/alınan şifreli mesajları logla
 * Son 50 mesaj, tarih/saat, contact, deşifre edilmiş metin
 */
public class MessageLog {
    private static final String TAG = "MessageLog";
    private static final String PREFS_NAME = "CryptoMessageLog";
    private static final String KEY_MESSAGES = "messages";
    private static final int MAX_MESSAGES = 50;
    
    private Context context;
    private SharedPreferences prefs;
    private Gson gson;
    
    public static class Message {
        public String id;
        public long timestamp;
        public String contact;
        public String text;
        public String direction; // "sent" or "received"
        public String mode; // "raw" or "fairytale"
        
        public Message(String contact, String text, String direction, String mode) {
            this.id = java.util.UUID.randomUUID().toString();
            this.timestamp = System.currentTimeMillis();
            this.contact = contact;
            this.text = text;
            this.direction = direction;
            this.mode = mode;
        }
        
        public String getFormattedTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
        
        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
        
        public String getDirectionIcon() {
            return "sent".equals(direction) ? "↗️" : "↙️";
        }
    }
    
    public MessageLog(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }
    
    /**
     * Mesaj ekle
     */
    public void addMessage(String contact, String text, String direction, String mode) {
        try {
            List<Message> messages = getMessages();
            
            // Yeni mesaj ekle
            Message newMessage = new Message(contact, text, direction, mode);
            messages.add(0, newMessage); // Başa ekle (en yeni üstte)
            
            // Max 50 mesaj tut
            if (messages.size() > MAX_MESSAGES) {
                messages = messages.subList(0, MAX_MESSAGES);
            }
            
            // Kaydet
            saveMessages(messages);
            
            Log.d(TAG, "✅ Mesaj eklendi: " + contact + " (" + direction + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Mesaj ekleme hatası", e);
        }
    }
    
    /**
     * Gönderilen mesaj logla
     */
    public void logSent(String contact, String text, String mode) {
        addMessage(contact, text, "sent", mode);
    }
    
    /**
     * Alınan mesaj logla
     */
    public void logReceived(String contact, String text, String mode) {
        addMessage(contact, text, "received", mode);
    }
    
    /**
     * Tüm mesajları al
     */
    public List<Message> getMessages() {
        try {
            String json = prefs.getString(KEY_MESSAGES, "[]");
            Type listType = new TypeToken<ArrayList<Message>>(){}.getType();
            List<Message> messages = gson.fromJson(json, listType);
            
            if (messages == null) {
                messages = new ArrayList<>();
            }
            
            Log.d(TAG, "📨 " + messages.size() + " mesaj yüklendi");
            return messages;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Mesaj yükleme hatası", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Belirli contact için mesajları al
     */
    public List<Message> getMessagesForContact(String contact) {
        List<Message> allMessages = getMessages();
        List<Message> filtered = new ArrayList<>();
        
        for (Message msg : allMessages) {
            if (contact.equals(msg.contact)) {
                filtered.add(msg);
            }
        }
        
        Log.d(TAG, "📨 " + contact + " için " + filtered.size() + " mesaj bulundu");
        return filtered;
    }
    
    /**
     * Mesajları kaydet
     */
    private void saveMessages(List<Message> messages) {
        try {
            String json = gson.toJson(messages);
            prefs.edit().putString(KEY_MESSAGES, json).apply();
            Log.d(TAG, "💾 " + messages.size() + " mesaj kaydedildi");
        } catch (Exception e) {
            Log.e(TAG, "❌ Mesaj kaydetme hatası", e);
        }
    }
    
    /**
     * Tüm mesajları temizle
     */
    public void clearAll() {
        prefs.edit().clear().apply();
        Log.d(TAG, "🗑️ Tüm mesajlar silindi");
    }
    
    /**
     * Belirli contact için mesajları sil
     */
    public void clearContact(String contact) {
        List<Message> messages = getMessages();
        messages.removeIf(msg -> contact.equals(msg.contact));
        saveMessages(messages);
        Log.d(TAG, "🗑️ " + contact + " mesajları silindi");
    }
    
    /**
     * Mesaj sayısı
     */
    public int getMessageCount() {
        return getMessages().size();
    }
    
    /**
     * Son mesaj
     */
    public Message getLastMessage() {
        List<Message> messages = getMessages();
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(0);
    }
}





