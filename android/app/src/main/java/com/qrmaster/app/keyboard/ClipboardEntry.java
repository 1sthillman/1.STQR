package com.qrmaster.app.keyboard;

import android.net.Uri;

import java.util.UUID;

public class ClipboardEntry {
    public enum Type { TEXT, SCREENSHOT }

    private final String id;
    private final Type type;
    private final String content;
    private final String preview;
    private final long timestamp;
    private boolean pinned;
    private final String uriString;

    public ClipboardEntry(Type type, String content, String preview, Uri uri, boolean pinned) {
        this(UUID.randomUUID().toString(), type, content, preview, uri, pinned, System.currentTimeMillis());
    }

    public ClipboardEntry(String id, Type type, String content, String preview, Uri uri, boolean pinned, long timestamp) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.preview = preview;
        this.uriString = uri != null ? uri.toString() : null;
        this.pinned = pinned;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String getPreview() {
        return preview;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public Uri getUri() {
        return uriString != null ? Uri.parse(uriString) : null;
    }
}








