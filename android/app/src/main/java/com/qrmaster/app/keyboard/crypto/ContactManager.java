package com.qrmaster.app.keyboard.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 👥 Contact Manager for E2EE Contacts
 * 
 * Features:
 * - Add/remove contacts
 * - Store contact fingerprints
 * - Verify contacts
 * - Persist to SharedPreferences
 */
public class ContactManager {
    private static final String TAG = "ContactManager";
    private static final String PREFS_NAME = "e2ee_contacts";
    private static final String KEY_CONTACTS = "contacts_json";
    
    private Context context;
    private Map<String, Contact> contacts;
    private Gson gson;
    
    public ContactManager(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.contacts = new HashMap<>();
        loadContacts();
    }
    
    /**
     * 📥 Load contacts from storage
     */
    private void loadContacts() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CONTACTS, null);
        
        if (json != null) {
            try {
                Type type = new TypeToken<Map<String, Contact>>(){}.getType();
                contacts = gson.fromJson(json, type);
                Log.d(TAG, "📥 Loaded " + contacts.size() + " contacts");
            } catch (Exception e) {
                Log.e(TAG, "Failed to load contacts", e);
                contacts = new HashMap<>();
            }
        }
    }
    
    /**
     * 💾 Save contacts to storage
     */
    private void saveContacts() {
        try {
            String json = gson.toJson(contacts);
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_CONTACTS, json).apply();
            Log.d(TAG, "💾 Saved " + contacts.size() + " contacts");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save contacts", e);
        }
    }
    
    /**
     * ➕ Add new contact
     */
    public boolean addContact(String id, String displayName) {
        if (contacts.containsKey(id)) {
            Log.w(TAG, "Contact already exists: " + id);
            return false;
        }
        
        Contact contact = new Contact(id, displayName);
        contacts.put(id, contact);
        saveContacts();
        
        Log.d(TAG, "➕ Added contact: " + id);
        return true;
    }
    
    /**
     * 🗑️ Remove contact
     */
    public boolean removeContact(String id) {
        Contact removed = contacts.remove(id);
        if (removed != null) {
            saveContacts();
            Log.d(TAG, "🗑️ Removed contact: " + id);
            return true;
        }
        return false;
    }
    
    /**
     * 📝 Update contact
     */
    public boolean updateContact(Contact contact) {
        if (!contacts.containsKey(contact.getId())) {
            return false;
        }
        
        contacts.put(contact.getId(), contact);
        saveContacts();
        Log.d(TAG, "📝 Updated contact: " + contact.getId());
        return true;
    }
    
    /**
     * 🔍 Get contact by ID
     */
    public Contact getContact(String id) {
        return contacts.get(id);
    }
    
    /**
     * 📋 Get all contacts
     */
    public List<Contact> getAllContacts() {
        return new ArrayList<>(contacts.values());
    }
    
    /**
     * ✅ Mark contact as verified
     */
    public void setContactVerified(String id, boolean verified) {
        Contact contact = contacts.get(id);
        if (contact != null) {
            contact.setVerified(verified);
            saveContacts();
            Log.d(TAG, "✅ Contact " + id + " verified: " + verified);
        }
    }
    
    /**
     * 🔑 Update contact fingerprint
     */
    public void setContactFingerprint(String id, String fingerprint) {
        Contact contact = contacts.get(id);
        if (contact != null) {
            contact.setFingerprint(fingerprint);
            saveContacts();
            Log.d(TAG, "🔑 Fingerprint updated for " + id);
        }
    }
    
    /**
     * 💬 Update last message
     */
    public void updateLastMessage(String id, String message) {
        Contact contact = contacts.get(id);
        if (contact != null) {
            contact.setLastMessage(message);
            contact.setLastMessageTime(System.currentTimeMillis());
            saveContacts();
        }
    }
    
    /**
     * 🔢 Get contact count
     */
    public int getContactCount() {
        return contacts.size();
    }
    
    /**
     * ✅ Check if contact exists
     */
    public boolean hasContact(String id) {
        return contacts.containsKey(id);
    }
}





