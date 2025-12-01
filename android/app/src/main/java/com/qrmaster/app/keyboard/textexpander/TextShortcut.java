package com.qrmaster.app.keyboard.textexpander;

/**
 * Text Expander - Kısayol modeli
 * /adres → Ev adresi
 * /cv → LinkedIn profil
 */
public class TextShortcut {
    private long id;
    private String trigger;      // "/adres"
    private String expansion;    // "Atatürk Cad. No:123..."
    private String description;  // "Ev Adresi"
    private boolean enabled;
    private int useCount;
    private long lastUsed;

    public TextShortcut(String trigger, String expansion, String description) {
        this.trigger = trigger;
        this.expansion = expansion;
        this.description = description;
        this.enabled = true;
        this.useCount = 0;
        this.lastUsed = 0;
    }

    public TextShortcut(long id, String trigger, String expansion, String description, 
                       boolean enabled, int useCount, long lastUsed) {
        this.id = id;
        this.trigger = trigger;
        this.expansion = expansion;
        this.description = description;
        this.enabled = enabled;
        this.useCount = useCount;
        this.lastUsed = lastUsed;
    }

    // Getters
    public long getId() { return id; }
    public String getTrigger() { return trigger; }
    public String getExpansion() { return expansion; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }
    public int getUseCount() { return useCount; }
    public long getLastUsed() { return lastUsed; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setTrigger(String trigger) { this.trigger = trigger; }
    public void setExpansion(String expansion) { this.expansion = expansion; }
    public void setDescription(String description) { this.description = description; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setUseCount(int useCount) { this.useCount = useCount; }
    public void setLastUsed(long lastUsed) { this.lastUsed = lastUsed; }

    public void incrementUseCount() {
        this.useCount++;
        this.lastUsed = System.currentTimeMillis();
    }
}

