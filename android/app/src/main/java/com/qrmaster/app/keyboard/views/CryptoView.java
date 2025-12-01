package com.qrmaster.app.keyboard.views;

import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.qrmaster.app.keyboard.crypto.CryptoManager;
import com.qrmaster.app.keyboard.crypto.ContactManager;
import com.qrmaster.app.keyboard.crypto.Contact;
import com.qrmaster.app.keyboard.crypto.FairytaleEncoder;
import com.qrmaster.app.keyboard.crypto.MessageLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 🔒 Şifreli Görüşme Modu UI
 * 
 * Özellikler:
 * - Şifreleme/Deşifre arayüzü
 * - Holografik animasyonlar
 * - QR/NFC anahtar paylaşımı
 * - Kişi yönetimi
 * - Güvenlik kontrol paneli
 */
public class CryptoView extends LinearLayout {
    private static final String TAG = "CryptoView";
    
    private Context context;
    private Callback legacyCallback; // ESKİ callback (onClose, onScanQR, vs)
    
    // 🔒 KryptEY Components (Simplified!)
    private ContactManager contactManager;
    private MessageLog messageLog;
    private String myFingerprint = "";
    
    // UI Components
    private EditText messageInput;
    private EditText passwordInput;
    private Button encryptBtn, decryptBtn;
    private TextView contactNameView;
    private String currentContactId = null;
    
    // Mode: "raw" or "fairytale"
    private String encryptionMode = "raw";
    
    public interface Callback {
        void onClose();
        void onScanQR(); // QR kod tarama
        void onShowQR(String qrData); // QR kod gösterme
        void onRequestNFC(); // NFC aktif etme
        void onCryptoWriteModeChanged(boolean enabled); // Mesaj yazma modu (Mouse gibi!)
    }
    
    public CryptoView(Context context) {
        super(context);
        init(context);
    }
    
    public CryptoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    private void init(Context context) {
        this.context = context;
        
        // 🔒 Initialize KryptEY components (Simplified!)
        this.contactManager = new ContactManager(context);
        this.messageLog = new MessageLog(context);
        this.myFingerprint = generateSimpleFingerprint();
        
        setOrientation(VERTICAL);
        setBackgroundColor(0xFF0A0A0A); // SİYAH ARKA PLAN!
        setPadding(dp(12), dp(8), dp(12), dp(8));
        
        createUI();
    }
    
    /**
     * 🔑 Generate SHA-512 fingerprint (KryptEY Style!)
     * 
     * Format: 8 blok x 4 hex = 32 karakter
     * Örnek: 3F7A-9B2C-4D8E-1A6F-8C3D-5E9A-2F4B-7D1C
     */
    private String generateSimpleFingerprint() {
        try {
            // Device ID + Build info (unique!)
            String androidId = android.provider.Settings.Secure.getString(
                context.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID
            );
            
            if (androidId == null) androidId = "DEFAULT_DEVICE";
            
            // Unique string
            String uniqueData = androidId + 
                               android.os.Build.MANUFACTURER + 
                               android.os.Build.MODEL;
            
            // SHA-512 hash
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(uniqueData.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // İlk 16 byte → 32 hex karakter
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                String h = Integer.toHexString(0xff & hash[i]);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            
            // 4'lü gruplara ayır
            String raw = hex.toString().toUpperCase();
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < 32; i += 4) {
                if (i > 0) formatted.append("-");
                formatted.append(raw.substring(i, i + 4));
            }
            
            return formatted.toString();
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Fingerprint error", e);
            return "XXXX-XXXX-XXXX-XXXX";
        }
    }
    
    private void createUI() {
        // Modern tasarım - Temiz ve basit
        
        // Şifre input (yazılabilir)
        addView(createModernPasswordInput());
        
        // Mesaj input
        addView(createModernMessageInput());
        
        // Modern butonlar
        addView(createModernButtons());
    }
    
    /**
     * 🎨 MODERN ŞİFRE INPUT - KLAVYE BUTONLU!
     */
    private LinearLayout createModernPasswordInput() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setPadding(dp(16), dp(12), dp(16), dp(8));
        
        // Label + Keyboard button row
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView label = new TextView(context);
        label.setText("Şifre");
        label.setTextColor(0xFF94A3B8);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        label.setPadding(dp(4), 0, 0, 0);
        
        LinearLayout.LayoutParams labelParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        headerRow.addView(label, labelParams);
        
        // YAPIŞTIR BUTONU (ŞİFRE)
        Button pastePwdBtn = new Button(context);
        pastePwdBtn.setText("📋");
        pastePwdBtn.setTextColor(0xFFEAB308);
        pastePwdBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        pastePwdBtn.setAllCaps(false);
        pastePwdBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        pastePwdBtn.setBackgroundColor(0x00000000);
        pastePwdBtn.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    if (text != null) {
                        passwordInput.setText(text.toString());
                        Toast.makeText(context, "✅ Yapıştırıldı!", Toast.LENGTH_SHORT).show();
                        vibrate(30);
                    }
                }
            } else {
                Toast.makeText(context, "❌ Panoda veri yok!", Toast.LENGTH_SHORT).show();
            }
        });
        headerRow.addView(pastePwdBtn);
        
        // KLAVYE BUTONU
        Button keyboardBtn = new Button(context);
        keyboardBtn.setText("⌨ YAZ");
        keyboardBtn.setTextColor(0xFF3B82F6);
        keyboardBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        keyboardBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        keyboardBtn.setAllCaps(false);
        keyboardBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
        keyboardBtn.setBackgroundColor(0x00000000);
        
        keyboardBtn.setOnClickListener(v -> {
            isPasswordWriteMode = !isPasswordWriteMode;
            
            if (isPasswordWriteMode) {
                // Diğer modu kapat
                isMessageWriteMode = false;
                
                keyboardBtn.setText("✓ TAMAM");
                keyboardBtn.setTextColor(0xFF10B981);
                Toast.makeText(context, "✅ Şifre yazma modu AÇIK\nKlavyeden yaz!", Toast.LENGTH_SHORT).show();
                
                // Background'ı yeşil yap
                GradientDrawable activeBg = new GradientDrawable();
                activeBg.setColor(0xFF1E4D3B);
                activeBg.setCornerRadius(dp(12));
                activeBg.setStroke(dp(2), 0xFF10B981);
                passwordInput.setBackground(activeBg);
            } else {
                keyboardBtn.setText("⌨ YAZ");
                keyboardBtn.setTextColor(0xFF3B82F6);
                Toast.makeText(context, "Şifre yazma modu KAPALI", Toast.LENGTH_SHORT).show();
                
                // Normal background
                GradientDrawable normalBg = new GradientDrawable();
                normalBg.setColor(0xFF1E293B);
                normalBg.setCornerRadius(dp(12));
                normalBg.setStroke(dp(1), 0xFF334155);
                passwordInput.setBackground(normalBg);
            }
            
            // Callback
            if (callback != null) {
                callback.onCryptoWriteModeChanged(isPasswordWriteMode, isMessageWriteMode);
            }
            
            vibrate(30);
        });
        
        headerRow.addView(keyboardBtn);
        container.addView(headerRow);
        
        // Spacer
        View spacer = new View(context);
        LinearLayout.LayoutParams spacerParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(6));
        container.addView(spacer, spacerParams);
        
        // Input (EditText ama yazılamaz, sadece gösterim)
        passwordInput = new EditText(context);
        passwordInput.setText("1234");
        passwordInput.setHint("⌨ YAZ butonuna bas");
        passwordInput.setTextColor(0xFFFFFFFF);
        passwordInput.setHintTextColor(0xFF64748B);
        passwordInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        passwordInput.setPadding(dp(16), dp(14), dp(16), dp(14));
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        
        // Modern gradient background
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(0xFF1E293B);
        inputBg.setCornerRadius(dp(12));
        inputBg.setStroke(dp(1), 0xFF334155);
        passwordInput.setBackground(inputBg);
        
        // YAZILMAZ! Sadece görüntüleme
        passwordInput.setFocusable(false);
        passwordInput.setClickable(false);
        passwordInput.setCursorVisible(false);
        
        container.addView(passwordInput);
        
        LinearLayout.LayoutParams containerParams = new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        containerParams.bottomMargin = dp(16);
        container.setLayoutParams(containerParams);
        
        return container;
    }
    
    private boolean isPasswordWriteMode = false;
    private boolean isMessageWriteMode = false;
    
    /**
     * Callback interface
     */
    public interface CryptoViewCallback {
        void onCryptoWriteModeChanged(boolean isPasswordMode, boolean isMessageMode);
    }
    
    private CryptoViewCallback callback;
    
    public void setCallback(CryptoViewCallback callback) {
        this.callback = callback;
    }
    
    /**
     * ⌨️ KLAVYEDEN YAZMA FONKSİYONLARI
     */
    public void appendToMessage(String text) {
        if (isMessageWriteMode && messageInput != null) {
            String current = messageInput.getText().toString();
            messageInput.setText(current + text);
            android.util.Log.d(TAG, "✍️ Mesaja eklendi: " + text);
        } else if (isPasswordWriteMode && passwordInput != null) {
            String current = passwordInput.getText().toString();
            passwordInput.setText(current + text);
            android.util.Log.d(TAG, "✍️ Şifreye eklendi: " + text);
        }
    }
    
    public void appendSpace() {
        if (isMessageWriteMode && messageInput != null) {
            String current = messageInput.getText().toString();
            messageInput.setText(current + " ");
        } else if (isPasswordWriteMode && passwordInput != null) {
            String current = passwordInput.getText().toString();
            passwordInput.setText(current + " ");
        }
    }
    
    public void appendNewLine() {
        if (isMessageWriteMode && messageInput != null) {
            String current = messageInput.getText().toString();
            messageInput.setText(current + "\n");
        }
    }
    
    public void deleteLastChar() {
        if (isMessageWriteMode && messageInput != null) {
            String current = messageInput.getText().toString();
            if (current.length() > 0) {
                messageInput.setText(current.substring(0, current.length() - 1));
            }
        } else if (isPasswordWriteMode && passwordInput != null) {
            String current = passwordInput.getText().toString();
            if (current.length() > 0) {
                passwordInput.setText(current.substring(0, current.length() - 1));
            }
        }
    }
    
    /**
     * 🎨 MODERN MESAJ INPUT - KLAVYE BUTONLU!
     */
    private LinearLayout createModernMessageInput() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setPadding(dp(16), dp(8), dp(16), dp(8));
        
        // Label + Keyboard button row
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView label = new TextView(context);
        label.setText("Mesaj");
        label.setTextColor(0xFF94A3B8);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        label.setPadding(dp(4), 0, 0, 0);
        
        LinearLayout.LayoutParams labelParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        headerRow.addView(label, labelParams);
        
        // KOPYALA BUTONU (MESAJ)
        Button copyMsgBtn = new Button(context);
        copyMsgBtn.setText("📤");
        copyMsgBtn.setTextColor(0xFF10B981);
        copyMsgBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        copyMsgBtn.setAllCaps(false);
        copyMsgBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        copyMsgBtn.setBackgroundColor(0x00000000);
        copyMsgBtn.setOnClickListener(v -> {
            String text = messageInput.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                copyToClipboard(text);
                Toast.makeText(context, "✅ Kopyalandı!", Toast.LENGTH_SHORT).show();
                vibrate(30);
            } else {
                Toast.makeText(context, "❌ Mesaj boş!", Toast.LENGTH_SHORT).show();
            }
        });
        headerRow.addView(copyMsgBtn);
        
        // YAPIŞTIR BUTONU (MESAJ)
        Button pasteMsgBtn = new Button(context);
        pasteMsgBtn.setText("📋");
        pasteMsgBtn.setTextColor(0xFFEAB308);
        pasteMsgBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        pasteMsgBtn.setAllCaps(false);
        pasteMsgBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        pasteMsgBtn.setBackgroundColor(0x00000000);
        pasteMsgBtn.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    if (text != null) {
                        messageInput.setText(text.toString());
                        Toast.makeText(context, "✅ Yapıştırıldı!", Toast.LENGTH_SHORT).show();
                        vibrate(30);
                    }
                }
            } else {
                Toast.makeText(context, "❌ Panoda veri yok!", Toast.LENGTH_SHORT).show();
            }
        });
        headerRow.addView(pasteMsgBtn);
        
        // KLAVYE BUTONU
        Button keyboardBtn = new Button(context);
        keyboardBtn.setText("⌨ YAZ");
        keyboardBtn.setTextColor(0xFF3B82F6);
        keyboardBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        keyboardBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        keyboardBtn.setAllCaps(false);
        keyboardBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
        keyboardBtn.setBackgroundColor(0x00000000);
        
        keyboardBtn.setOnClickListener(v -> {
            isMessageWriteMode = !isMessageWriteMode;
            
            if (isMessageWriteMode) {
                // Diğer modu kapat
                isPasswordWriteMode = false;
                
                keyboardBtn.setText("✓ TAMAM");
                keyboardBtn.setTextColor(0xFF10B981);
                Toast.makeText(context, "✅ Mesaj yazma modu AÇIK\nKlavyeden yaz!", Toast.LENGTH_SHORT).show();
                
                // Background'ı yeşil yap
                GradientDrawable activeBg = new GradientDrawable();
                activeBg.setColor(0xFF1E4D3B);
                activeBg.setCornerRadius(dp(12));
                activeBg.setStroke(dp(2), 0xFF10B981);
                messageInput.setBackground(activeBg);
            } else {
                keyboardBtn.setText("⌨ YAZ");
                keyboardBtn.setTextColor(0xFF3B82F6);
                Toast.makeText(context, "Mesaj yazma modu KAPALI", Toast.LENGTH_SHORT).show();
                
                // Normal background
                GradientDrawable normalBg = new GradientDrawable();
                normalBg.setColor(0xFF1E293B);
                normalBg.setCornerRadius(dp(12));
                normalBg.setStroke(dp(1), 0xFF334155);
                messageInput.setBackground(normalBg);
            }
            
            // Callback
            if (callback != null) {
                callback.onCryptoWriteModeChanged(isPasswordWriteMode, isMessageWriteMode);
            }
            
            vibrate(30);
        });
        
        headerRow.addView(keyboardBtn);
        container.addView(headerRow);
        
        // Spacer
        View spacer = new View(context);
        LinearLayout.LayoutParams spacerParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(6));
        container.addView(spacer, spacerParams);
        
        // Input (EditText ama yazılamaz, sadece gösterim)
        messageInput = new EditText(context);
        messageInput.setHint("⌨ YAZ butonuna bas");
        messageInput.setTextColor(0xFFFFFFFF);
        messageInput.setHintTextColor(0xFF64748B);
        messageInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        messageInput.setPadding(dp(16), dp(14), dp(16), dp(14));
        messageInput.setMinHeight(dp(100));
        messageInput.setGravity(Gravity.TOP | Gravity.START);
        messageInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        
        // Modern gradient background
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(0xFF1E293B);
        inputBg.setCornerRadius(dp(12));
        inputBg.setStroke(dp(1), 0xFF334155);
        messageInput.setBackground(inputBg);
        
        // YAZILMAZ! Sadece görüntüleme
        messageInput.setFocusable(false);
        messageInput.setClickable(false);
        messageInput.setCursorVisible(false);
        
        container.addView(messageInput);
        
        LinearLayout.LayoutParams containerParams = new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        containerParams.bottomMargin = dp(16);
        container.setLayoutParams(containerParams);
        
        return container;
    }
    
    /**
     * 🎨 MODERN BUTONLAR
     */
    private LinearLayout createModernButtons() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(HORIZONTAL);
        container.setPadding(dp(16), dp(8), dp(16), dp(16));
        container.setGravity(Gravity.CENTER);
        
        // ŞİFRELE Butonu
        encryptBtn = new Button(context);
        encryptBtn.setText("ŞİFRELE");
        encryptBtn.setTextColor(0xFFFFFFFF);
        encryptBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        encryptBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        encryptBtn.setAllCaps(false);
        encryptBtn.setPadding(0, dp(16), 0, dp(16));
        
        // Gradient mavi-mor
        GradientDrawable encryptBg = new GradientDrawable();
        encryptBg.setColors(new int[]{0xFF3B82F6, 0xFF8B5CF6});
        encryptBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        encryptBg.setCornerRadius(dp(12));
        encryptBtn.setBackground(encryptBg);
        
        encryptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.util.Log.d(TAG, "🔐 ŞİFRELE butonuna basıldı");
                vibrate(50);
                
                try {
                    encryptMessage();
                } catch (Exception e) {
                    android.util.Log.e(TAG, "❌ HATA!", e);
                    Toast.makeText(context, "❌ Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        LinearLayout.LayoutParams encryptParams = new LayoutParams(0, dp(54), 1f);
        encryptParams.rightMargin = dp(8);
        container.addView(encryptBtn, encryptParams);
        
        // DEŞİFRE ET Butonu
        decryptBtn = new Button(context);
        decryptBtn.setText("DEŞİFRE ET");
        decryptBtn.setTextColor(0xFFFFFFFF);
        decryptBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        decryptBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        decryptBtn.setAllCaps(false);
        decryptBtn.setPadding(0, dp(16), 0, dp(16));
        
        // Gradient yeşil-cyan
        GradientDrawable decryptBg = new GradientDrawable();
        decryptBg.setColors(new int[]{0xFF10B981, 0xFF06B6D4});
        decryptBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        decryptBg.setCornerRadius(dp(12));
        decryptBtn.setBackground(decryptBg);
        
        decryptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.util.Log.d(TAG, "🔓 DEŞİFRE ET butonuna basıldı");
                vibrate(50);
                
                try {
                    decryptMessage();
                } catch (Exception e) {
                    android.util.Log.e(TAG, "❌ HATA!", e);
                    Toast.makeText(context, "❌ Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        LinearLayout.LayoutParams decryptParams = new LayoutParams(0, dp(54), 1f);
        decryptParams.leftMargin = dp(8);
        container.addView(decryptBtn, decryptParams);
        
        return container;
    }
    
    /**
     * 📦 COMPACT HEADER - Ultra minimal!
     */
    private LinearLayout createCompactHeader() {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(4), dp(4), dp(4), dp(6));
        
        // Contact name (clickable to select)
        contactNameView = new TextView(context);
        contactNameView.setText(currentContactId != null ? currentContactId : "Select →");
        contactNameView.setTextColor(0xFF2C6BED);
        contactNameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        contactNameView.setTypeface(null, android.graphics.Typeface.BOLD);
        contactNameView.setPadding(dp(8), dp(6), dp(8), dp(6));
        contactNameView.setOnClickListener(v -> {
            showContactsDialog();
            vibrate(30);
        });
        
        LinearLayout.LayoutParams nameParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(contactNameView, nameParams);
        
        // Mode toggle mini
        Button modeBtn = createMiniButton(encryptionMode.equals("raw") ? "RAW" : "HID");
        modeBtn.setOnClickListener(v -> {
            encryptionMode = encryptionMode.equals("raw") ? "fairytale" : "raw";
            modeBtn.setText(encryptionMode.equals("raw") ? "RAW" : "HID");
            vibrate(20);
        });
        header.addView(modeBtn);
        
        // Copy ID button
        Button idBtn = createMiniButton("ID");
        idBtn.setOnClickListener(v -> {
            showFingerprintDialog();
            vibrate(30);
        });
        header.addView(idBtn);
        
        // Close button
        Button closeBtn = createMiniButton("✕");
        closeBtn.setOnClickListener(v -> {
            if (legacyCallback != null) legacyCallback.onClose();
        });
        header.addView(closeBtn);
        
        return header;
    }
    
    /**
     * 🔘 Mini Button (Compact!)
     */
    private Button createMiniButton(String text) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.setPadding(0, 0, 0, 0);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF2C2C2E);
        bg.setCornerRadius(dp(6));
        btn.setBackground(bg);
        
        LayoutParams params = new LayoutParams(dp(36), dp(28));
        params.leftMargin = dp(4);
        btn.setLayoutParams(params);
        
        return btn;
    }
    
    /**
     * 🎨 Create Icon Button (Modern, no emoji!)
     */
    private Button createIconButton(String text, int color) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.setPadding(0, 0, 0, 0);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(8));
        btn.setBackground(bg);
        
        LayoutParams params = new LayoutParams(dp(42), dp(36));
        params.leftMargin = dp(6);
        btn.setLayoutParams(params);
        
        return btn;
    }
    
    /**
     * 👤 CONTACT SELECTOR (MODERN!)
     */
    private LinearLayout createContactSelector() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(HORIZONTAL);
        container.setPadding(dp(14), dp(10), dp(14), dp(10));
        container.setGravity(Gravity.CENTER_VERTICAL);
        
        // Modern card
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1C1C1E);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(2), 0xFF2C6BED);
        container.setBackground(bg);
        
        // Icon circle
        TextView iconCircle = new TextView(context);
        iconCircle.setText("●");
        iconCircle.setTextColor(0xFF00C853);
        iconCircle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        LinearLayout.LayoutParams iconParams = new LayoutParams(dp(24), LayoutParams.WRAP_CONTENT);
        iconParams.rightMargin = dp(10);
        container.addView(iconCircle, iconParams);
        
        // Contact name
        contactNameView = new TextView(context);
        contactNameView.setText(currentContactId != null ? currentContactId : "Select Contact");
        contactNameView.setTextColor(Color.WHITE);
        contactNameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        contactNameView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams nameParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        container.addView(contactNameView, nameParams);
        
        // Select button
        Button selectBtn = createIconButton("▼", 0xFF2C6BED);
        selectBtn.setOnClickListener(v -> {
            showContactsDialog();
            vibrate(30);
        });
        container.addView(selectBtn);
        
        LayoutParams containerParams = new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        containerParams.bottomMargin = dp(12);
        container.setLayoutParams(containerParams);
        
        return container;
    }
    
    /**
     * 🔄 MODE SELECTOR (Modern Toggle!)
     */
    private LinearLayout createModeSelector() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(HORIZONTAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(0, dp(8), 0, dp(8));
        
        // Raw Mode Button
        Button rawBtn = createToggleButton("RAW", true);
        rawBtn.setOnClickListener(v -> {
            encryptionMode = "raw";
            updateToggleButtons(rawBtn, (Button)container.getChildAt(1));
            Toast.makeText(context, "RAW Mode", Toast.LENGTH_SHORT).show();
            vibrate(30);
        });
        LinearLayout.LayoutParams rawParams = new LayoutParams(0, dp(44), 1f);
        rawParams.rightMargin = dp(6);
        container.addView(rawBtn, rawParams);
        
        // Fairytale Mode Button
        Button fairytaleBtn = createToggleButton("HIDDEN", false);
        fairytaleBtn.setOnClickListener(v -> {
            encryptionMode = "fairytale";
            updateToggleButtons(fairytaleBtn, (Button)container.getChildAt(0));
            Toast.makeText(context, "HIDDEN Mode", Toast.LENGTH_SHORT).show();
            vibrate(30);
        });
        LinearLayout.LayoutParams fairytaleParams = new LayoutParams(0, dp(44), 1f);
        fairytaleParams.leftMargin = dp(6);
        container.addView(fairytaleBtn, fairytaleParams);
        
        return container;
    }
    
    /**
     * 🔘 Create Toggle Button (Modern!)
     */
    private Button createToggleButton(String text, boolean active) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.setPadding(0, 0, 0, 0);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(active ? 0xFF2C6BED : 0xFF2C2C2E);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(2), active ? 0xFF2C6BED : 0xFF444444);
        btn.setBackground(bg);
        
        return btn;
    }
    
    /**
     * 🔄 Update Toggle Buttons
     */
    private void updateToggleButtons(Button activeBtn, Button inactiveBtn) {
        GradientDrawable activeBg = new GradientDrawable();
        activeBg.setColor(0xFF2C6BED);
        activeBg.setCornerRadius(dp(10));
        activeBg.setStroke(dp(2), 0xFF2C6BED);
        activeBtn.setBackground(activeBg);
        
        GradientDrawable inactiveBg = new GradientDrawable();
        inactiveBg.setColor(0xFF2C2C2E);
        inactiveBg.setCornerRadius(dp(10));
        inactiveBg.setStroke(dp(2), 0xFF444444);
        inactiveBtn.setBackground(inactiveBg);
    }
    
    /**
     * 📊 STATUS BAR (Modern!)
     */
    private LinearLayout createStatusBar() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(HORIZONTAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dp(12), dp(10), dp(12), dp(4));
        
        // Status icon
        TextView icon = new TextView(context);
        icon.setText("●");
        icon.setTextColor(0xFF00C853);
        icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        LinearLayout.LayoutParams iconParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        iconParams.rightMargin = dp(6);
        container.addView(icon, iconParams);
        
        // Fingerprint text
        TextView status = new TextView(context);
        status.setText("Device ID: " + myFingerprint);
        status.setTextColor(0xFF666666);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        status.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(status);
        
        return container;
    }
    
    /**
     * 🔑 Şifre Input (Matrix border)
     */
    private LinearLayout createPasswordInput() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(HORIZONTAL);
        container.setPadding(dp(12), dp(8), dp(12), dp(8));
        container.setGravity(Gravity.CENTER_VERTICAL);
        
        // Koyu arka plan + Matrix border
        GradientDrawable containerBg = new GradientDrawable();
        containerBg.setColor(0xFF1C1C1E);
        containerBg.setCornerRadius(dp(10));
        containerBg.setStroke(dp(2), 0xFF00FF00); // Matrix yeşil border
        container.setBackground(containerBg);
        
        TextView label = new TextView(context);
        label.setText("🔑");
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        LinearLayout.LayoutParams labelParams = new LayoutParams(dp(32), LayoutParams.WRAP_CONTENT);
        labelParams.rightMargin = dp(8);
        container.addView(label, labelParams);
        
        passwordInput = new EditText(context);
        passwordInput.setHint("Şifre (tıkla yaz)");
        passwordInput.setText("1234"); // Varsayılan şifre
        passwordInput.setTextColor(Color.WHITE);
        passwordInput.setHintTextColor(0xFF666666);
        passwordInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        passwordInput.setPadding(dp(8), dp(8), dp(8), dp(8));
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        passwordInput.setBackgroundColor(Color.TRANSPARENT);
        passwordInput.setFocusable(true);
        passwordInput.setFocusableInTouchMode(true);
        passwordInput.setClickable(true);
        
        LinearLayout.LayoutParams passParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        container.addView(passwordInput, passParams);
        
        LayoutParams containerParams = new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        containerParams.bottomMargin = dp(12);
        container.setLayoutParams(containerParams);
        
        return container;
    }
    
    // ESKİ createMessageDisplay() SİLİNDİ - YENİSİ ÜSTTE!
    
    // ESKİ DUPLICATE KODLAR SİLİNDİ - YENİLERİ ÜSTTE!
    
    /**
     * ⚡ COMPACT ACTION BAR - Tek satır!
     */
    private LinearLayout createCompactActionBar() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(HORIZONTAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(0, dp(6), 0, 0);
        
        // Şifreleme (BÜYÜK VE BASİT!)
        encryptBtn = new Button(context);
        encryptBtn.setText("🔒 LOCK\nŞİFRELE");
        encryptBtn.setTextColor(Color.WHITE);
        encryptBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        encryptBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        encryptBtn.setPadding(dp(20), dp(20), dp(20), dp(20));
        encryptBtn.setBackgroundColor(0xFFE53935);
        encryptBtn.setAllCaps(false);
        
        encryptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.util.Log.d(TAG, "========================================");
                android.util.Log.d(TAG, "🔴🔴🔴 LOCK BUTONUNA BASILDI! 🔴🔴🔴");
                android.util.Log.d(TAG, "========================================");
                
                Toast.makeText(context, "🔴 LOCK BASILDI!", Toast.LENGTH_LONG).show();
                vibrate(200);
                
                try {
                    encryptMessage();
                } catch (Exception e) {
                    android.util.Log.e(TAG, "❌ HATA!", e);
                    Toast.makeText(context, "❌ HATA: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        
        LinearLayout.LayoutParams encryptParams = new LayoutParams(0, dp(80), 1f);
        encryptParams.rightMargin = dp(8);
        container.addView(encryptBtn, encryptParams);
        
        // Deşifreleme (BÜYÜK VE BASİT!)
        decryptBtn = new Button(context);
        decryptBtn.setText("🔓 UNLOCK\nAÇ");
        decryptBtn.setTextColor(Color.WHITE);
        decryptBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        decryptBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        decryptBtn.setPadding(dp(20), dp(20), dp(20), dp(20));
        decryptBtn.setBackgroundColor(0xFF00C853);
        decryptBtn.setAllCaps(false);
        
        decryptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.util.Log.d(TAG, "========================================");
                android.util.Log.d(TAG, "🟢🟢🟢 UNLOCK BUTONUNA BASILDI! 🟢🟢🟢");
                android.util.Log.d(TAG, "========================================");
                
                Toast.makeText(context, "🟢 UNLOCK BASILDI!", Toast.LENGTH_LONG).show();
                vibrate(200);
                
                try {
                    decryptMessage();
                } catch (Exception e) {
                    android.util.Log.e(TAG, "❌ HATA!", e);
                    Toast.makeText(context, "❌ HATA: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        
        LinearLayout.LayoutParams decryptParams = new LayoutParams(0, dp(80), 1f);
        decryptParams.leftMargin = dp(8);
        container.addView(decryptBtn, decryptParams);
        
        return container;
    }
    
    
    /**
     * Küçük buton oluştur
     */
    private Button createSmallButton(String text) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setPadding(dp(8), dp(4), dp(8), dp(4));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF2C2C2E);
        bg.setCornerRadius(dp(6));
        btn.setBackground(bg);
        
        LayoutParams params = new LayoutParams(dp(36), dp(32));
        params.leftMargin = dp(6);
        btn.setLayoutParams(params);
        
        return btn;
    }
    
    
    /**
     * BASİT ŞİFRELEME - Çalışıyor!
     */
    private void encryptMessage() {
        try {
            android.util.Log.d(TAG, "🔒 encryptMessage() ÇAĞRILDI!");
            
            if (messageInput == null) {
                android.util.Log.e(TAG, "❌ messageInput NULL!");
                Toast.makeText(context, "❌ Mesaj input hatası!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (passwordInput == null) {
                android.util.Log.e(TAG, "❌ passwordInput NULL!");
                Toast.makeText(context, "❌ Şifre input hatası!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String message = messageInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            
            android.util.Log.d(TAG, "🔒 Mesaj: '" + message + "' (uzunluk: " + message.length() + ")");
            android.util.Log.d(TAG, "🔒 Şifre: '" + password + "' (uzunluk: " + password.length() + ")");
            
            if (TextUtils.isEmpty(message)) {
                Toast.makeText(context, "❌ Mesaj boş!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(context, "❌ Şifre boş!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            vibrate(50);
            
            // 1. Normal şifreleme
            String encrypted = com.qrmaster.app.keyboard.crypto.SimpleCrypto.encrypt(message, password);
            
            if (encrypted != null) {
                // 2. Fairytale mode ise gizle
                if ("fairytale".equals(encryptionMode)) {
                    android.util.Log.d(TAG, "📖 Fairytale mode aktif - gizleniyor...");
                    String fairytale = FairytaleEncoder.encode(encrypted);
                    messageInput.setText(fairytale);
                    copyToClipboard(fairytale);
                    Toast.makeText(context, "✅ ŞİFRELENDİ ve GİZLENDİ! 📖", Toast.LENGTH_SHORT).show();
                    android.util.Log.d(TAG, "✅ Fairytale: " + fairytale);
                } else {
                    // RAW mode
                    messageInput.setText(encrypted);
                    copyToClipboard(encrypted);
                    Toast.makeText(context, "✅ ŞİFRELENDİ ve kopyalandı!", Toast.LENGTH_SHORT).show();
                    android.util.Log.d(TAG, "✅ RAW: " + encrypted);
                }
                
                // 3. Mesajı logla
                if (messageLog != null) {
                    String contactName = currentContactId != null ? currentContactId : "General";
                    messageLog.logSent(contactName, message, encryptionMode);
                    android.util.Log.d(TAG, "📨 Mesaj loglandı");
                }
                
                vibrate(new long[]{0, 50, 100, 50});
            } else {
                Toast.makeText(context, "❌ Şifreleme başarısız!", Toast.LENGTH_SHORT).show();
                vibrate(200);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "❌ Encrypt error", e);
            Toast.makeText(context, "❌ Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * BASİT DEŞİFRE - Çalışıyor!
     */
    private void decryptMessage() {
        try {
            String message = messageInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            
            android.util.Log.d(TAG, "🔓 Deşifre başladı: '" + message + "' password='" + password + "'");
            
            if (TextUtils.isEmpty(message)) {
                Toast.makeText(context, "❌ Mesaj boş!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(context, "❌ Şifre boş!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            vibrate(30);
            
            String encryptedMessage = message;
            
            // 1. Fairytale olup olmadığını kontrol et
            if (FairytaleEncoder.hasFairytale(message)) {
                android.util.Log.d(TAG, "📖 Fairytale algılandı - çıkarılıyor...");
                encryptedMessage = FairytaleEncoder.decode(message);
                if (encryptedMessage == null) {
                    Toast.makeText(context, "❌ Fairytale decode hatası!", Toast.LENGTH_SHORT).show();
                    return;
                }
                android.util.Log.d(TAG, "✅ Şifreli mesaj çıkarıldı: " + encryptedMessage);
            } else if (!com.qrmaster.app.keyboard.crypto.SimpleCrypto.isEncrypted(message)) {
                Toast.makeText(context, "❌ Bu şifreli mesaj değil!", Toast.LENGTH_LONG).show();
                return;
            }
            
            // 2. Normal deşifre
            String decrypted = com.qrmaster.app.keyboard.crypto.SimpleCrypto.decrypt(encryptedMessage, password);
            
            if (decrypted != null) {
                messageInput.setText(decrypted);
                Toast.makeText(context, "✅ Açıldı: " + decrypted, Toast.LENGTH_SHORT).show();
                vibrate(new long[]{0, 30, 50, 30, 50, 30});
                android.util.Log.d(TAG, "✅ Başarılı!");
                
                // Mesajı logla
                if (messageLog != null) {
                    String contactName = currentContactId != null ? currentContactId : "General";
                    String mode = FairytaleEncoder.hasFairytale(message) ? "fairytale" : "raw";
                    messageLog.logReceived(contactName, decrypted, mode);
                    android.util.Log.d(TAG, "📨 Alınan mesaj loglandı");
                }
            } else {
                Toast.makeText(context, "❌ Açılamadı! Yanlış şifre?", Toast.LENGTH_SHORT).show();
                vibrate(200);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "❌ Decrypt error", e);
            Toast.makeText(context, "❌ Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Otomatik panodan deşifre
     */
    private void autoDetectAndDecrypt() {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                String text = clip.getItemAt(0).getText().toString();
                
                if (com.qrmaster.app.keyboard.crypto.SimpleCrypto.isEncrypted(text)) {
                    messageInput.setText(text);
                    decryptMessage();
                } else {
                    Toast.makeText(context, "⚠️ Panoda şifreli mesaj bulunamadı (ENC: ile başlamalı)", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    // Utility methods
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("encrypted", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
    }
    
    private void vibrate(long duration) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(duration);
            }
        }
    }
    
    private void vibrate(long[] pattern) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }
    
    /**
     * 🔑 Show Fingerprint Dialog
     */
    private void showFingerprintDialog() {
        // Direct copy to clipboard - NO DIALOG CRASH!
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("fingerprint", myFingerprint);
        clipboard.setPrimaryClip(clip);
        
        Toast.makeText(context, 
            "🔑 Fingerprint kopyalandı!\n" + myFingerprint, 
            Toast.LENGTH_LONG).show();
        vibrate(50);
    }
    
    /**
     * 👥 Show Contacts Dialog - INLINE VIEW (NO CRASH!)
     */
    private void showContactsDialog() {
        List<Contact> contacts = contactManager.getAllContacts();
        
        // Create inline contact list view
        ScrollView scrollView = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setPadding(dp(16), dp(12), dp(16), dp(12));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1C1C1E);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(2), 0xFF2C6BED);
        container.setBackground(bg);
        
        // Header
        TextView header = new TextView(context);
        header.setText("Kişiler (" + contacts.size() + ")");
        header.setTextColor(0xFF2C6BED);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 0, 0, dp(12));
        container.addView(header);
        
        // Add new contact button
        Button addBtn = createContactButton("+ Yeni Kişi Ekle", 0xFF00C853);
        addBtn.setOnClickListener(v -> {
            showAddContactInlineView();
            vibrate(30);
        });
        container.addView(addBtn);
        
        // Contact list
        if (!contacts.isEmpty()) {
            for (Contact contact : contacts) {
                Button contactBtn = createContactButton(
                    contact.getDisplayName() + (contact.isVerified() ? " ✓" : ""),
                    0xFF2C6BED
                );
                contactBtn.setOnClickListener(v -> {
                    selectContact(contact);
                    removeView(scrollView);
                });
                container.addView(contactBtn);
            }
        }
        
        // Close button
        Button closeBtn = createContactButton("✕ Kapat", 0xFFE53935);
        closeBtn.setOnClickListener(v -> {
            removeView(scrollView);
            vibrate(30);
        });
        container.addView(closeBtn);
        
        scrollView.addView(container);
        
        LayoutParams params = new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(300)
        );
        params.topMargin = dp(8);
        addView(scrollView, getChildCount() - 1, params);
    }
    
    /**
     * ➕ Show Add Contact Inline View (NO CRASH!)
     */
    private void showAddContactInlineView() {
        // Remove contacts list first
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child instanceof ScrollView) {
                removeView(child);
            }
        }
        
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setPadding(dp(16), dp(12), dp(16), dp(12));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1C1C1E);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(2), 0xFF00C853);
        container.setBackground(bg);
        
        // Header
        TextView header = new TextView(context);
        header.setText("Yeni Kişi Ekle");
        header.setTextColor(0xFF00C853);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 0, 0, dp(12));
        container.addView(header);
        
        // UYARI mesajı
        TextView warning = new TextView(context);
        warning.setText("⚠️ Klavye servisi içinde input açılamaz!\n\n📝 Kişi ekleme:\n1. Ana uygulamayı aç\n2. Ayarlar → Kişiler\n3. Oradan ekle\n\nYA DA:\n\nKişi olmadan kullan:\n- Sadece ŞİFRE gir\n- LOCK/UNLOCK kullan");
        warning.setTextColor(0xFFFFAA00);
        warning.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        warning.setPadding(dp(12), dp(12), dp(12), dp(12));
        warning.setGravity(Gravity.CENTER);
        
        GradientDrawable warnBg = new GradientDrawable();
        warnBg.setColor(0xFF332200);
        warnBg.setCornerRadius(dp(8));
        warning.setBackground(warnBg);
        
        LinearLayout.LayoutParams warnParams = new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        warnParams.bottomMargin = dp(12);
        container.addView(warning, warnParams);
        
        // Kapat button
        Button closeBtn = createContactButton("✕ Kapat", 0xFFE53935);
        closeBtn.setOnClickListener(v -> {
            removeView(container);
            vibrate(30);
        });
        container.addView(closeBtn);
        
        LayoutParams params = new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(8);
        addView(container, getChildCount() - 1, params);
        
        vibrate(30);
    }
    
    /**
     * 🔘 Create Contact Button (Modern!)
     */
    private Button createContactButton(String text, int color) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.setPadding(dp(16), dp(12), dp(16), dp(12));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(10));
        btn.setBackground(bg);
        
        LayoutParams params = new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48)
        );
        params.bottomMargin = dp(8);
        btn.setLayoutParams(params);
        
        return btn;
    }
    
    /**
     * 👤 Select Contact
     */
    private void selectContact(Contact contact) {
        currentContactId = contact.getId();
        contactNameView.setText(contact.getDisplayName());
        
        Toast.makeText(context, 
            "✅ " + contact.getDisplayName() + " seçildi!",
            Toast.LENGTH_SHORT).show();
        
        vibrate(30);
    }
    
    /**
     * 🧚 Fairytale Mode: Hide encrypted message in a fairy tale
     */
    private String wrapInFairytale(String encrypted) {
        String[] fairytales = {
            "Bir varmış bir yokmuş, evvel zaman içinde...",
            "Vaktiyle bir padişahın üç oğlu varmış...",
            "Çok eski zamanlarda, uzak diyarlarda...",
            "Bir zamanlar ormanda yaşayan...",
            "Günlerden bir gün, bir köyde..."
        };
        
        Random random = new Random();
        String prefix = fairytales[random.nextInt(fairytales.length)];
        String suffix = " ...ve sonsuza dek mutlu yaşadılar.";
        
        return prefix + " [" + encrypted + "] " + suffix;
    }
    
    /**
     * 🔍 Extract encrypted message from fairytale
     */
    private String extractFromFairytale(String fairytaleText) {
        int start = fairytaleText.indexOf("[");
        int end = fairytaleText.indexOf("]");
        
        if (start != -1 && end != -1 && end > start) {
            return fairytaleText.substring(start + 1, end);
        }
        
        // Eğer [ ] yok ise, direkt text'i döndür (raw mode olabilir)
        return fairytaleText;
    }
    
    private int dp(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            context.getResources().getDisplayMetrics()
        );
    }
    
    public void setLegacyCallback(Callback callback) {
        this.legacyCallback = callback;
    }
}
