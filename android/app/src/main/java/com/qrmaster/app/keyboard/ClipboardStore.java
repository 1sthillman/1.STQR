package com.qrmaster.app.keyboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Kalıcı pano geçmişi yöneticisi
 */
public class ClipboardStore {

    public interface OnChangeListener {
        void onClipboardChanged(List<ClipboardEntry> entries);
    }

    private static final String PREF_NAME = "keyboard_clipboard_store";
    private static final String PREF_ENTRIES = "entries";
    private static final int MAX_ITEMS = 20;
    private static final ClipboardStore INSTANCE = new ClipboardStore();

    private final List<ClipboardEntry> entries = new ArrayList<>();
    private final Set<OnChangeListener> listeners = new HashSet<>();
    private Context appContext;
    private Gson gson;

    private ClipboardStore() {}

    public static ClipboardStore getInstance(Context context) {
        INSTANCE.ensureInit(context.getApplicationContext());
        return INSTANCE;
    }

    private void ensureInit(Context context) {
        if (appContext != null) return;
        appContext = context;
        gson = new Gson();
        loadFromPrefs();
    }

    private void loadFromPrefs() {
        String json = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(PREF_ENTRIES, null);
        if (json != null) {
            try {
                Type type = new TypeToken<List<ClipboardEntry>>(){}.getType();
                List<ClipboardEntry> saved = gson.fromJson(json, type);
                if (saved != null) {
                    entries.clear();
                    entries.addAll(saved);
                }
            } catch (Exception e) {
                Log.e("ClipboardStore", "Clipboard entries yüklenemedi", e);
            }
        }
    }

    private void persist() {
        try {
            String json = gson.toJson(entries);
            appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_ENTRIES, json)
                .apply();
        } catch (Exception e) {
            Log.e("ClipboardStore", "Clipboard entries kaydedilemedi", e);
        }
    }

    public synchronized List<ClipboardEntry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public synchronized ClipboardEntry addText(String text) {
        if (text == null || text.isEmpty()) return null;
        // Duplicate check (ignore case)
        for (ClipboardEntry entry : entries) {
            if (entry.getType() == ClipboardEntry.Type.TEXT && text.equals(entry.getContent())) {
                // move to top
                entries.remove(entry);
                entries.add(0, entry);
                notifyChange();
                persist();
                return entry;
            }
        }
        ClipboardEntry newEntry = new ClipboardEntry(ClipboardEntry.Type.TEXT, text, text, null, false);
        insertEntry(newEntry);
        return newEntry;
    }

    public synchronized ClipboardEntry addScreenshot(Uri uri, String previewText) {
        if (uri == null) return null;
        ClipboardEntry newEntry = new ClipboardEntry(ClipboardEntry.Type.SCREENSHOT,
                uri.toString(), previewText != null ? previewText : "Screenshot",
                uri, false);
        // Remove duplicates (same URI)
        entries.removeIf(entry -> entry.getType() == ClipboardEntry.Type.SCREENSHOT
                && uri.toString().equals(entry.getContent()));
        insertEntry(newEntry);
        return newEntry;
    }

    private void insertEntry(ClipboardEntry entry) {
        entries.add(0, entry);
        trim();
        notifyChange();
        persist();
    }

    private void trim() {
        // Pinned items should stay, non pinned limited to MAX_ITEMS
        int count = 0;
        List<ClipboardEntry> toRemove = new ArrayList<>();
        for (ClipboardEntry entry : entries) {
            if (entry.isPinned()) continue;
            count++;
            if (count > MAX_ITEMS) {
                toRemove.add(entry);
            }
        }
        entries.removeAll(toRemove);
    }

    public synchronized void togglePin(String id) {
        for (ClipboardEntry entry : entries) {
            if (entry.getId().equals(id)) {
                entry.setPinned(!entry.isPinned());
                sortEntries();
                notifyChange();
                persist();
                break;
            }
        }
    }

    public synchronized void delete(String id) {
        entries.removeIf(entry -> entry.getId().equals(id));
        notifyChange();
        persist();
    }

    private void sortEntries() {
        entries.sort(Comparator.comparing(ClipboardEntry::isPinned).reversed()
            .thenComparing(ClipboardEntry::getTimestamp, Comparator.reverseOrder()));
    }

    public synchronized void register(OnChangeListener listener) {
        listeners.add(listener);
        listener.onClipboardChanged(getEntries());
    }

    public synchronized void unregister(OnChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyChange() {
        sortEntries();
        List<ClipboardEntry> snapshot = getEntries();
        for (OnChangeListener listener : listeners) {
            listener.onClipboardChanged(snapshot);
        }
    }

    public void setSystemClipboard(ClipboardEntry entry) {
        ClipboardManager clipboard = (ClipboardManager) appContext.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        if (entry.getType() == ClipboardEntry.Type.TEXT) {
            clipboard.setPrimaryClip(ClipData.newPlainText("text", entry.getContent()));
        } else if (entry.getType() == ClipboardEntry.Type.SCREENSHOT) {
            Uri uri = entry.getUri();
            if (uri != null) {
                clipboard.setPrimaryClip(ClipData.newUri(appContext.getContentResolver(), "screenshot", uri));
            }
        }
    }
}
