package com.qrmaster.app;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * QR Accessibility Service
 * QR kod okununca aktif input alanına direkt yazar
 */
public class QRAccessibilityService extends AccessibilityService {
    
    private static final String TAG = "QRAccessibilityService";
    private static QRAccessibilityService instance;
    private static String pendingText = null;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static final int MAX_WRITE_ATTEMPTS = 6;
    private static final Object WRITE_LOCK = new Object();
    private static long lastWriteAttemptTimestamp = 0;
    private static long lastWriteResultTimestamp = 0;
    private static boolean lastWriteSuccess = false;
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "✅ QR Accessibility Service bağlandı!");
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (pendingText != null && instance != null) {
            handler.post(() -> tryWritePendingTextWithRetries(0));
        }
    }
    
    private boolean findAndPasteToEditText(AccessibilityNodeInfo node) {
        if (node == null || pendingText == null) {
            return false;
        }
        
        if (isCandidateInputNode(node)) {
            return writeTextToNode(node);
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                boolean success = findAndPasteToEditText(child);
                child.recycle();
                if (success) {
                    return true;
            }
        }
        }
        return false;
    }
    
    private boolean isCandidateInputNode(AccessibilityNodeInfo node) {
        if (node == null) return false;
        
        if (node.isEditable()) return true;
        
        CharSequence className = node.getClassName();
        if (className != null) {
            String lower = className.toString().toLowerCase();
            if (lower.contains("edittext") || lower.contains("textinput") || lower.contains("searchview") || lower.contains("input")) {
                return true;
            }
        }
        
        for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList()) {
            int id = action.getId();
            if (id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT.getId()
                || id == AccessibilityNodeInfo.AccessibilityAction.ACTION_PASTE.getId()) {
                return true;
            }
        }
        
        return false;
    }
    
    private void ensureClipboardHasText(String text) {
        if (text == null) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("AutoFill", text);
            clipboard.setPrimaryClip(clip);
        }
    }
    
    private void ensureClipboardHasPendingText() {
        ensureClipboardHasText(pendingText);
    }
    
    private boolean writeTextToNode(AccessibilityNodeInfo node) {
        if (pendingText == null) return false;
        
        try {
            boolean focusSuccess = node.isFocused()
                || node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                || node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                || node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            
            if (!focusSuccess) {
                Log.d(TAG, "⚠️ Input alanı odaklanamadı, yine de denenecek");
            }
            
            node.refresh();
            
            CharSequence existingChar = node.getText();
            String existingText = existingChar != null ? existingChar.toString() : "";
            String appendText = pendingText != null ? pendingText : "";
            String newText;
            if (existingText.isEmpty()) {
                newText = appendText;
            } else if (appendText.isEmpty()) {
                newText = existingText;
            } else {
                boolean endsWithNewLine = existingText.endsWith("\n");
                newText = existingText + (endsWithNewLine ? "" : "\n") + appendText;
            }
            
            ensureClipboardHasText(newText);
            
            Bundle arguments = new Bundle();
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText
            );
            
            boolean setText = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            if (setText) {
                Log.i(TAG, "✅ ACTION_SET_TEXT ile yazı başarıyla aktarıldı");
                markWriteCompletion(true);
                pendingText = null;
                return true;
            }
            
            Log.w(TAG, "⚠️ ACTION_SET_TEXT başarısız, PASTE denenecek");
            
            ensureClipboardHasText(newText);
            boolean paste = node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            if (!paste) {
                node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
                ensureClipboardHasText(newText);
                paste = node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            }
            
            if (paste) {
                Log.i(TAG, "✅ Clipboard üzerinden PASTE başarılı");
                markWriteCompletion(true);
                    pendingText = null;
                return true;
            }
            
            Log.e(TAG, "❌ Ne SET_TEXT ne de PASTE başarılı olamadı");
        } catch (Exception e) {
            Log.e(TAG, "❌ writeTextToNode hatası: " + e.getMessage());
        }
        
        return false;
    }
    
    private boolean tryWriteToFocusedNode() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return false;
        
        AccessibilityNodeInfo focused = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused == null) {
            focused = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
        }
        
        if (focused != null) {
            boolean success = writeTextToNode(focused);
            focused.recycle();
            return success;
        }
        
        return false;
    }
    
    private void tryWritePendingTextWithRetries(final int attempt) {
        handler.post(() -> {
            if (pendingText == null) return;
            
            ensureClipboardHasPendingText();
            
            if (attempt > MAX_WRITE_ATTEMPTS) {
                Log.e(TAG, "❌ Otomatik doldurma denemeleri başarısız oldu.");
                markWriteCompletion(false);
                pendingText = null;
                return;
            }
            
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            boolean success = false;
            
            if (tryWriteToFocusedNode()) {
                return;
            }
            
            if (rootNode != null) {
                success = findAndPasteToEditText(rootNode);
                rootNode.recycle();
            } else {
                Log.w(TAG, "⚠️ Root node alınamadı (deneme #" + attempt + ")");
            }
            
            if (!success && pendingText != null) {
                long delay = 160L * Math.max(1, attempt + 1);
                handler.postDelayed(() -> tryWritePendingTextWithRetries(attempt + 1), delay);
            }
        });
    }
    
    private static void markWriteAttempt() {
        synchronized (WRITE_LOCK) {
            lastWriteAttemptTimestamp = System.currentTimeMillis();
            lastWriteResultTimestamp = 0;
            lastWriteSuccess = false;
        }
    }
    
    private static void markWriteCompletion(boolean success) {
        synchronized (WRITE_LOCK) {
            lastWriteSuccess = success;
            lastWriteResultTimestamp = System.currentTimeMillis();
        }
    }
    
    @Override
    public void onInterrupt() {
        // Service kesildiğinde
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }
    
    // Static method - QR okununca dışarıdan çağrılır
    public static void pasteQRCode(Context context, String qrCode) {
        Log.i(TAG, "📋 pasteQRCode çağrıldı: " + qrCode);
        
        // Her durumda clipboard'a kopyala (fallback)
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("QR Code", qrCode);
                clipboard.setPrimaryClip(clip);
                Log.d(TAG, "📋 Clipboard'a kopyalandı (fallback)");
            }
            
        if (instance != null) {
            Log.d(TAG, "✅ Instance var, pending text ayarlanıyor");
            
            markWriteAttempt();
            pendingText = qrCode;
            instance.tryWritePendingTextWithRetries(0);
        } else {
            Log.w(TAG, "⚠️ Accessibility service kapalı - sadece clipboard");
            markWriteCompletion(false);
        }
    }
    
    // 📝 OCR text'i yapıştır (yeni metod - QR ile aynı mantık)
    public static void pasteOCRText(Context context, String text) {
        Log.i(TAG, "📝 pasteOCRText çağrıldı: " + text.substring(0, Math.min(50, text.length())));
        
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("OCR Text", text);
                clipboard.setPrimaryClip(clip);
                Log.d(TAG, "📋 Clipboard'a kopyalandı (fallback)");
            }
            
        if (instance != null) {
            Log.d(TAG, "✅ Instance var, OCR text ayarlanıyor");
            
            markWriteAttempt();
            pendingText = text;
            instance.tryWritePendingTextWithRetries(0);
        } else {
            Log.w(TAG, "⚠️ Accessibility service kapalı - sadece clipboard");
            markWriteCompletion(false);
        }
    }
    
    // Accessibility service aktif mi kontrol et
    public static boolean isServiceEnabled() {
        return instance != null && instance.getRootInActiveWindow() != null;
    }
    
    public static WriteStatus peekWriteStatus(long referenceTimestamp) {
        synchronized (WRITE_LOCK) {
            WriteStatus status = new WriteStatus();
            status.attempted = lastWriteAttemptTimestamp >= referenceTimestamp;
            status.completed = lastWriteResultTimestamp >= referenceTimestamp;
            status.success = status.completed && lastWriteSuccess;
            return status;
        }
    }
    
    public static void clearWriteStatus(long referenceTimestamp) {
        synchronized (WRITE_LOCK) {
            if (lastWriteAttemptTimestamp >= referenceTimestamp) {
                lastWriteAttemptTimestamp = 0;
                lastWriteResultTimestamp = 0;
                lastWriteSuccess = false;
            }
        }
    }
    
    public static class WriteStatus {
        public boolean attempted;
        public boolean completed;
        public boolean success;
    }
}

