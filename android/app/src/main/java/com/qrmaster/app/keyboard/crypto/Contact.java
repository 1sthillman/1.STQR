package com.qrmaster.app.keyboard.crypto;

/**
 * 👤 Contact Model for E2EE
 */
public class Contact {
    private String id;              // Unique ID (username)
    private String displayName;     // Display name
    private String fingerprint;     // Public key fingerprint
    private boolean verified;       // Is fingerprint verified?
    private long lastMessageTime;   // Last message timestamp
    private String lastMessage;     // Last message preview
    
    public Contact(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.verified = false;
        this.lastMessageTime = System.currentTimeMillis();
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getFingerprint() {
        return fingerprint;
    }
    
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
    
    public boolean isVerified() {
        return verified;
    }
    
    public void setVerified(boolean verified) {
        this.verified = verified;
    }
    
    public long getLastMessageTime() {
        return lastMessageTime;
    }
    
    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
    
    public String getLastMessage() {
        return lastMessage;
    }
    
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}





