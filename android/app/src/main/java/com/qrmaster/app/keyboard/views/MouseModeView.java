package com.qrmaster.app.keyboard.views;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.qrmaster.app.R;
import com.qrmaster.app.keyboard.mouse.*;

/**
 * WiFi Mouse Modu - Premium Windows 11 Tasarım
 * Trackpad + Gesture + Kontrol Paneli
 */
public class MouseModeView extends LinearLayout {
    
    public interface Callback {
        void onClose();
        void onModeSwitch(); // Klavye moduna geç
        void onCameraRequest(); // Kamera için
        void onTextInput(String text); // PC'ye text gönder
        void onKeyboardModeChanged(boolean enabled); // Yazma modu değişti
    }
    
    private final Callback callback;
    private MouseManager mouseManager;
    private com.qrmaster.app.keyboard.mouse.GestureDetector gestureDetector;
    
    public MouseManager getMouseManager() {
        return mouseManager;
    }
    
    public void setMouseManager(MouseManager manager) {
        this.mouseManager = manager;
    }
    
    private TextView statusText;
    private TextView latencyText;
    private View trackpadArea;
    private LinearLayout controlPanel;
    private SeekBar sensitivityBar;
    private ImageView screenPreview; // Ekran önizlemesi
    private Button screenToggleBtn; // Ekran aç/kapa
    
    private float sensitivity = 1.5f;
    private boolean isConnected = false;
    private boolean screenStreamingEnabled = false;
    private ScreenStreamThread screenThread;
    
    public MouseModeView(Context context, Callback callback) {
        super(context);
        this.callback = callback;
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        
        // Premium gradient background (Windows 11 inspired)
        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF0F0F0F, 0xFF000000}
        );
        setBackground(bg);
        
        // Mouse Manager
        mouseManager = new MouseManager(context);
        mouseManager.setCallback(new MouseManager.ConnectionCallback() {
            @Override
            public void onConnected(String pcName, String ipAddress) {
                isConnected = true;
                updateStatus("🟢 Bağlı: " + pcName, 0xFF34C759);
            }
            
            @Override
            public void onDisconnected() {
                isConnected = false;
                updateStatus("⚪ Bağlantı kesildi", 0xFFFF3B30);
            }
            
            @Override
            public void onLatencyUpdate(int latencyMs) {
                updateLatency(latencyMs);
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(context, "❌ " + error, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Header
        addView(createHeader(context));
        
        // SCREEN PREVIEW - Üstte küçük önizleme
        addView(createScreenPreview(context));
        
        // Trackpad Area (60% ekran - ekran önizlemesi için yer açtık)
        trackpadArea = createTrackpadArea(context);
        LinearLayout.LayoutParams trackpadParams = new LayoutParams(
            LayoutParams.MATCH_PARENT, 0, 0.60f
        );
        addView(trackpadArea, trackpadParams);
        
        // Control Panel (35% ekran) - Daha büyük text input için
        controlPanel = createControlPanel(context);
        LinearLayout.LayoutParams controlParams = new LayoutParams(
            LayoutParams.MATCH_PARENT, 0, 0.35f
        );
        addView(controlPanel, controlParams);
        
        // Gesture Detector
        gestureDetector = new com.qrmaster.app.keyboard.mouse.GestureDetector(new com.qrmaster.app.keyboard.mouse.GestureDetector.GestureCallback() {
            @Override
            public void onMouseMove(float deltaX, float deltaY) {
                if (isConnected) {
                    mouseManager.sendMouseMove(deltaX * sensitivity, deltaY * sensitivity);
                }
            }
            
            @Override
            public void onSingleTap() {
                vibrate(10);
                if (isConnected) mouseManager.sendMouseClick("LEFT");
            }
            
            @Override
            public void onDoubleTap() {
                vibrate(10, 10);
                if (isConnected) mouseManager.sendMouseClick("DOUBLE");
            }
            
            @Override
            public void onTwoFingerTap() {
                vibrate(10, 5, 10);
                if (isConnected) mouseManager.sendMouseClick("RIGHT");
            }
            
            @Override
            public void onScroll(float deltaY) {
                if (isConnected) {
                    // ULTRA HIZLI SCROLL - 5x artırdık
                    int scrollAmount = (int) (deltaY / 2); // 2'ye böl = 5x hızlı
                    if (Math.abs(scrollAmount) > 0) {
                        mouseManager.sendMouseScroll(scrollAmount);
                        vibrate(1); // Minimal feedback
                    }
                }
            }
            
            @Override
            public void onThreeFingerSwipeUp() {
                vibrate(30);
                if (isConnected) mouseManager.sendGesture("THREE_FINGER_UP");
            }
            
            @Override
            public void onThreeFingerSwipeDown() {
                vibrate(30);
                if (isConnected) mouseManager.sendGesture("THREE_FINGER_DOWN");
            }
            
            @Override
            public void onThreeFingerSwipeLeft() {
                vibrate(20);
                if (isConnected) mouseManager.sendGesture("THREE_FINGER_LEFT");
            }
            
            @Override
            public void onThreeFingerSwipeRight() {
                vibrate(20);
                if (isConnected) mouseManager.sendGesture("THREE_FINGER_RIGHT");
            }
            
            @Override
            public void onThreeFingerTap() {
                vibrate(15, 5, 15);
                if (isConnected) mouseManager.sendGesture("THREE_FINGER_TAP");
            }
            
            @Override
            public void onFourFingerTap() {
                vibrate(40);
                if (isConnected) mouseManager.sendGesture("FOUR_FINGER_TAP");
            }
            
            @Override
            public void onFourFingerSwipeLeft() {
                vibrate(25);
                if (isConnected) mouseManager.sendGesture("FOUR_FINGER_LEFT");
            }
            
            @Override
            public void onFourFingerSwipeRight() {
                vibrate(25);
                if (isConnected) mouseManager.sendGesture("FOUR_FINGER_RIGHT");
            }
            
            @Override
            public void onPinchZoom(float scale) {
                if (isConnected) {
                    if (scale > 1.1f) {
                        mouseManager.sendGesture("ZOOM_IN");
                    } else if (scale < 0.9f) {
                        mouseManager.sendGesture("ZOOM_OUT");
                    }
                }
            }
        });
        
        trackpadArea.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }
    
    /**
     * Screen Preview - Bilgisayar ekranını göster
     */
    private LinearLayout createScreenPreview(Context context) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setPadding(dp(8), dp(4), dp(8), dp(4));
        container.setBackgroundColor(0xFF1C1C1E);
        
        // Toggle button + ImageView container
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView label = new TextView(context);
        label.setText("📺 Ekran Görüntüsü:");
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(label, labelParams);
        
        // Toggle button
        screenToggleBtn = new Button(context);
        screenToggleBtn.setText("AÇIK");
        screenToggleBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        screenToggleBtn.setTextColor(Color.WHITE);
        screenToggleBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
        GradientDrawable toggleBg = new GradientDrawable();
        toggleBg.setColor(0xFF34C759);
        toggleBg.setCornerRadius(dp(6));
        screenToggleBtn.setBackground(toggleBg);
        
        screenToggleBtn.setOnClickListener(v -> toggleScreenStreaming());
        
        header.addView(screenToggleBtn);
        container.addView(header);
        
        // ImageView for screen preview
        screenPreview = new ImageView(context);
        screenPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        screenPreview.setBackgroundColor(0xFF000000);
        LinearLayout.LayoutParams previewParams = new LayoutParams(
            LayoutParams.MATCH_PARENT, dp(120) // 120dp height
        );
        previewParams.topMargin = dp(4);
        screenPreview.setLayoutParams(previewParams);
        container.addView(screenPreview);
        
        return container;
    }
    
    private void toggleScreenStreaming() {
        if (!isConnected || mouseManager == null || mouseManager.getCurrentDevice() == null) {
            Toast.makeText(getContext(), "❌ Önce PC'ye bağlanın!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        android.util.Log.d("MouseModeView", "🔘 Toggle butonu tıklandı! Mevcut durum: " + screenStreamingEnabled);
        
        screenStreamingEnabled = !screenStreamingEnabled;
        
        if (screenStreamingEnabled) {
            // Start streaming
            screenToggleBtn.setText("KAPALI");
            GradientDrawable offBg = new GradientDrawable();
            offBg.setColor(0xFFFF3B30);
            offBg.setCornerRadius(dp(6));
            screenToggleBtn.setBackground(offBg);
            
            android.util.Log.d("MouseModeView", "📺 Streaming başlatılıyor...");
            Toast.makeText(getContext(), "📺 Server'a enable isteği gönderiliyor...", Toast.LENGTH_SHORT).show();
            
            // Enable on server - ONCE
            enableScreenOnServer();
            
            // WAIT 1 second for server to start capture, THEN start stream
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                String serverIp = mouseManager.getCurrentDevice().ipAddress;
                android.util.Log.d("MouseModeView", "📺 Stream thread başlatılıyor: " + serverIp + ":58081");
                screenThread = new ScreenStreamThread(serverIp, 58081, screenPreview);
                screenThread.start();
                Toast.makeText(getContext(), "📺 Ekran streaming başladı", Toast.LENGTH_SHORT).show();
            }, 1000);
        } else {
            // Stop streaming
            screenToggleBtn.setText("AÇIK");
            GradientDrawable onBg = new GradientDrawable();
            onBg.setColor(0xFF34C759);
            onBg.setCornerRadius(dp(6));
            screenToggleBtn.setBackground(onBg);
            
            // Stop thread
            if (screenThread != null) {
                screenThread.stopStream();
                screenThread = null;
            }
            
            // Disable on server
            disableScreenOnServer();
            
            // Clear preview
            screenPreview.setImageBitmap(null);
            
            Toast.makeText(getContext(), "📺 Ekran streaming durdu", Toast.LENGTH_SHORT).show();
        }
        
        vibrate(10);
    }
    
    private void enableScreenOnServer() {
        if (mouseManager == null || mouseManager.getCurrentDevice() == null) return;
        
        new Thread(() -> {
            try {
                String url = "http://" + mouseManager.getCurrentDevice().ipAddress + ":58081/screen/enable";
                android.util.Log.d("MouseModeView", "📡 POST isteği gönderiliyor: " + url);
                
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.connect();
                
                int responseCode = conn.getResponseCode();
                String responseMsg = conn.getResponseMessage();
                
                android.util.Log.d("MouseModeView", "📡 Enable response: " + responseCode + " - " + responseMsg);
                
                if (responseCode == 200) {
                    android.util.Log.d("MouseModeView", "✅ Screen capture server'da başlatıldı!");
                } else {
                    android.util.Log.e("MouseModeView", "❌ Enable failed: " + responseCode);
                }
                
                conn.disconnect();
            } catch (Exception e) {
                android.util.Log.e("MouseModeView", "❌ Enable screen error", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "❌ Server'a ulaşılamadı: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private void disableScreenOnServer() {
        if (mouseManager == null || mouseManager.getCurrentDevice() == null) return;
        
        new Thread(() -> {
            try {
                String url = "http://" + mouseManager.getCurrentDevice().ipAddress + ":58081/screen/disable";
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(3000);
                conn.connect();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                android.util.Log.e("MouseModeView", "Disable screen error", e);
            }
        }).start();
    }
    
    private LinearLayout createHeader(Context context) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setPadding(dp(16), dp(12), dp(16), dp(12));
        header.setGravity(Gravity.CENTER_VERTICAL);
        
        // Glassmorphism background
        GradientDrawable headerBg = new GradientDrawable();
        headerBg.setColor(0x80202020);
        headerBg.setCornerRadii(new float[]{0,0,0,0,dp(16),dp(16),dp(16),dp(16)});
        header.setBackground(headerBg);
        
        // Status
        LinearLayout statusContainer = new LinearLayout(context);
        statusContainer.setOrientation(VERTICAL);
        
        statusText = new TextView(context);
        statusText.setText("⚪ Bağlı değil");
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        statusText.setTextColor(0xFFAAAAAA);
        statusText.setTypeface(null, Typeface.BOLD);
        statusContainer.addView(statusText);
        
        latencyText = new TextView(context);
        latencyText.setText("Gecikme: --");
        latencyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        latencyText.setTextColor(0xFF8E8E93);
        statusContainer.addView(latencyText);
        
        LinearLayout.LayoutParams statusParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(statusContainer, statusParams);
        
        // Connect button
        Button connectBtn = createPremiumButton(context, "🔗", 0xFF007AFF);
        connectBtn.setOnClickListener(v -> {
            android.util.Log.d("MouseModeView", "BAĞLAN TIKLANDI!");
            Toast.makeText(context, "BUTON ÇALIŞIYOR!", Toast.LENGTH_SHORT).show();
            vibrate(10);
            showConnectionDialog();
        });
        header.addView(connectBtn);
        
        // Manuel IP giriş butonu
        Button ipBtn = createPremiumButton(context, "🔢", 0xFF34C759);
        ipBtn.setOnClickListener(v -> {
            vibrate(10);
            showManualIPDialog();
        });
        LinearLayout.LayoutParams ipParams = new LayoutParams(dp(44), dp(44));
        ipParams.leftMargin = dp(8);
        header.addView(ipBtn, ipParams);
        
        // QR info butonu - QR nasıl kullanılır göster
        Button qrBtn = createPremiumButton(context, "❓", 0xFFFF9500);
        qrBtn.setOnClickListener(v -> {
            vibrate(10);
            String qrInfo = "📱 QR Kod ile Bağlantı:\n\n" +
                "1. PC'de START_MOUSE_SERVER.bat çalıştır\n" +
                "2. qkeyboard_qr.png açılacak\n" +
                "3. Telefonda herhangi bir QR okuyucu uygulama kullan\n" +
                "4. Çıkan bilgileri buraya gir:\n" +
                "   - 🔢 butonuna bas → IP gir\n" +
                "   - PIN'i gir\n\n" +
                "NOT: Klavye QR okuyamaz! Ayrı QR app kullan.";
            Toast.makeText(context, qrInfo, Toast.LENGTH_LONG).show();
        });
        LinearLayout.LayoutParams qrParams = new LayoutParams(dp(44), dp(44));
        qrParams.leftMargin = dp(8);
        header.addView(qrBtn, qrParams);
        
        // Keyboard mode button
        Button keyboardBtn = createPremiumButton(context, "⌨️", 0xFF5856D6);
        keyboardBtn.setOnClickListener(v -> callback.onModeSwitch());
        LinearLayout.LayoutParams kbParams = new LayoutParams(dp(44), dp(44));
        kbParams.leftMargin = dp(8);
        header.addView(keyboardBtn, kbParams);
        
        // Close button
        Button closeBtn = createPremiumButton(context, "✕", 0xFFFF3B30);
        closeBtn.setOnClickListener(v -> callback.onClose());
        LinearLayout.LayoutParams closeParams = new LayoutParams(dp(44), dp(44));
        closeParams.leftMargin = dp(8);
        header.addView(closeBtn, closeParams);
        
        return header;
    }
    
    private View createTrackpadArea(Context context) {
        FrameLayout trackpad = new FrameLayout(context);
        trackpad.setPadding(dp(8), dp(8), dp(8), dp(8));
        
        // Trackpad background (subtle pattern)
        View trackpadBg = new View(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1C1C1E);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), 0x40FFFFFF);
        trackpadBg.setBackground(bg);
        trackpad.addView(trackpadBg);
        
        // Hint text
        TextView hint = new TextView(context);
        hint.setText("🖱️ Touchpad Alanı\n\n1 Parmak: Hareket\n2 Parmak: Sağ Tık / Kaydır\n3 Parmak: Windows 11 Hareketleri");
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        hint.setTextColor(0x80FFFFFF);
        hint.setGravity(Gravity.CENTER);
        hint.setAlpha(isConnected ? 0.3f : 1.0f);
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER
        );
        trackpad.addView(hint, hintParams);
        
        return trackpad;
    }
    
    private LinearLayout createControlPanel(Context context) {
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(VERTICAL);
        panel.setPadding(dp(12), dp(8), dp(12), dp(12));
        panel.setBackgroundColor(0xFF1C1C1E);
        
        // KLAVYE MODU SWITCH - Açık/Kapalı
        LinearLayout keyboardModeContainer = new LinearLayout(context);
        keyboardModeContainer.setOrientation(HORIZONTAL);
        keyboardModeContainer.setPadding(dp(8), dp(8), dp(8), dp(8));
        keyboardModeContainer.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable modeBg = new GradientDrawable();
        modeBg.setColor(0xFF2C2C2E);
        modeBg.setCornerRadius(dp(8));
        keyboardModeContainer.setBackground(modeBg);
        
        TextView modeLabel = new TextView(context);
        modeLabel.setText("⌨️ PC'ye Yazma Modu:");
        modeLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        modeLabel.setTextColor(Color.WHITE);
        modeLabel.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        keyboardModeContainer.addView(modeLabel, labelParams);
        
        // Switch button
        final boolean[] keyboardModeEnabled = {false};
        Button modeToggle = new Button(context);
        modeToggle.setText("KAPALI");
        modeToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        modeToggle.setTextColor(Color.WHITE);
        modeToggle.setPadding(dp(16), dp(6), dp(16), dp(6));
        modeToggle.setAllCaps(true);
        modeToggle.setTypeface(null, Typeface.BOLD);
        GradientDrawable toggleBg = new GradientDrawable();
        toggleBg.setColor(0xFFFF3B30);
        toggleBg.setCornerRadius(dp(6));
        modeToggle.setBackground(toggleBg);
        
        modeToggle.setOnClickListener(v -> {
            keyboardModeEnabled[0] = !keyboardModeEnabled[0];
            if (keyboardModeEnabled[0]) {
                modeToggle.setText("AÇIK ✓");
                GradientDrawable onBg = new GradientDrawable();
                onBg.setColor(0xFF34C759);
                onBg.setCornerRadius(dp(6));
                modeToggle.setBackground(onBg);
                Toast.makeText(context, "✅ Yazma modu AÇIK\nKlavyeden yazdıklarınız PC'ye gidecek!", Toast.LENGTH_LONG).show();
            } else {
                modeToggle.setText("KAPALI");
                GradientDrawable offBg = new GradientDrawable();
                offBg.setColor(0xFFFF3B30);
                offBg.setCornerRadius(dp(6));
                modeToggle.setBackground(offBg);
                Toast.makeText(context, "❌ Yazma modu KAPALI", Toast.LENGTH_SHORT).show();
            }
            vibrate(10);
            // Callback
            callback.onKeyboardModeChanged(keyboardModeEnabled[0]);
        });
        
        keyboardModeContainer.addView(modeToggle);
        panel.addView(keyboardModeContainer);
        
        // Store reference for callback
        callback.onTextInput(""); // Initialize
        
        // COMPACT Quick buttons - 2 satır yeterli
        LinearLayout row1 = createButtonRow(context);
        row1.addView(createControlButton(context, "Sol", () -> {
            vibrate(5);
            if (isConnected) mouseManager.sendMouseClick("LEFT");
        }));
        row1.addView(createControlButton(context, "Sağ", () -> {
            vibrate(5);
            if (isConnected) mouseManager.sendMouseClick("RIGHT");
        }));
        row1.addView(createControlButton(context, "Orta", () -> {
            vibrate(5);
            if (isConnected) mouseManager.sendMouseClick("MIDDLE");
        }));
        row1.addView(createControlButton(context, "Win", () -> {
            vibrate(5);
            if (isConnected) mouseManager.sendKeyPress("LWIN");
        }));
        panel.addView(row1);
        
        LinearLayout row2 = createButtonRow(context);
        row2.addView(createControlButton(context, "↑", () -> {
            if (isConnected) mouseManager.sendMouseScroll(5);
        }));
        row2.addView(createControlButton(context, "↓", () -> {
            if (isConnected) mouseManager.sendMouseScroll(-5);
        }));
        row2.addView(createControlButton(context, "Alt+Tab", () -> {
            vibrate(5);
            if (isConnected) mouseManager.sendGesture("ALT_TAB");
        }));
        row2.addView(createControlButton(context, "Task", () -> {
            vibrate(5);
            if (isConnected) mouseManager.sendGesture("THREE_FINGER_UP");
        }));
        panel.addView(row2);
        
        return panel;
    }
    
    private LinearLayout createButtonRow(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        row.setLayoutParams(params);
        return row;
    }
    
    private Button createControlButton(Context context, String text, Runnable action) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btn.setTextColor(Color.WHITE);
        btn.setPadding(dp(8), dp(8), dp(8), dp(8));
        btn.setMinHeight(0);
        btn.setMinWidth(0);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF2C2C2E);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), 0x40FFFFFF);
        btn.setBackground(bg);
        
        btn.setOnClickListener(v -> action.run());
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        btn.setLayoutParams(params);
        
        return btn;
    }
    
    private Button createPremiumButton(Context context, String text, int color) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        btn.setTextColor(Color.WHITE);
        btn.setPadding(0, 0, 0, 0);
        btn.setMinWidth(0);
        btn.setMinHeight(0);
        btn.setGravity(Gravity.CENTER);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(10));
        btn.setBackground(bg);
        
        LinearLayout.LayoutParams params = new LayoutParams(dp(44), dp(44));
        btn.setLayoutParams(params);
        
        return btn;
    }
    
    private void showConnectionDialog() {
        // TEST: Butona basılınca kesin toast göster
        Toast.makeText(getContext(), "🔘 BAĞLAN BUTONUNA BASILDI!", Toast.LENGTH_LONG).show();
        vibrate(15);
        startWiFiDiscovery();
    }
    
    private void startWiFiDiscovery() {
        // Hemen Toast göster - kullanıcı bilsin ne oluyor
        post(() -> {
            Toast.makeText(getContext(), "🔍 PC aranıyor... (2 saniye)", Toast.LENGTH_SHORT).show();
        });
        
        // PC keşfini başlat
        mouseManager.discoverDevices(devices -> {
            post(() -> {
                if (devices.isEmpty()) {
                    // Detaylı hata mesajı
                    String errorMsg = "❌ PC BULUNAMADI!\n\n" +
                        "✅ Yapılması gerekenler:\n\n" +
                        "1. PC'de komutu çalıştır:\n" +
                        "   python qkeyboard_server.py\n\n" +
                        "2. Telefon ve PC AYNI WiFi'de olmalı\n\n" +
                        "3. Windows Firewall:\n" +
                        "   Port 58080 ve 59090 açık olmalı\n\n" +
                        "Tekrar dene!";
                    
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    
                    // Status güncelle
                    updateStatus("⚪ PC bulunamadı", 0xFFFF9500);
                } else {
                    // PC bulundu!
                    updateStatus("✅ " + devices.size() + " PC bulundu", 0xFF34C759);
                    showDeviceListDialog(devices);
                }
            });
        });
    }
    
    private void showBluetoothInfo() {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                getContext(),
                android.R.style.Theme_DeviceDefault_Dialog_Alert
            );
            builder.setTitle("🔵 Bluetooth HID");
            builder.setMessage(
                "⚠️ Android Kısıtlaması:\n\n" +
                "Android cihazları varsayılan olarak Bluetooth Mouse/Keyboard olarak ÇALIŞAMAZ.\n\n" +
                "🔓 Gereksinimler:\n" +
                "• Root erişimi\n" +
                "• Özel ROM (LineageOS vb.)\n" +
                "• Bluetooth HID Device profili\n\n" +
                "✅ Önerilen Alternatifler:\n" +
                "1. USB Kablo (Root gerektirmez)\n" +
                "2. WiFi Server (Python)\n" +
                "3. WiFi Direct (Geliştirilecek)"
            );
            builder.setPositiveButton("Anladım", null);
            
            android.app.AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            }
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ Dialog hatası", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showUSBInfo() {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                getContext(),
                android.R.style.Theme_DeviceDefault_Dialog_Alert
            );
            builder.setTitle("🔌 USB Bağlantısı");
            builder.setMessage(
                "✅ En Kolay Yöntem (Root gerektirmez):\n\n" +
                "1. USB kabloyu telefondan PC'ye tak\n" +
                "2. Telefonda 'USB için kullanım' açılır\n" +
                "3. 'Dosya aktarımı' yerine 'USB tethering' seç\n" +
                "4. PC'de Windows ayarlardan 'USB cihazlar' → 'HID klavye/mouse' etkinleştir\n\n" +
                "⚠️ NOT:\n" +
                "Bu özellik Android USB Gadget desteği gerektirir.\n" +
                "Bazı telefonlarda (Samsung, Xiaomi) desteklenmeyebilir.\n\n" +
                "📱 Alternatif:\n" +
                "WiFi Server (Python) kullanın - kablosuz ve evrensel!"
            );
            builder.setPositiveButton("Anladım", null);
            builder.setNegativeButton("WiFi Server Kullan", (d, w) -> startWiFiDiscovery());
            
            android.app.AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            }
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ Dialog hatası", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showInfoDialog() {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                getContext(),
                android.R.style.Theme_DeviceDefault_Dialog_Alert
            );
            builder.setTitle("ℹ️ Mouse Modu Hakkında");
            builder.setMessage(
                "🖱️ Telefonunuzu PC mouse'u olarak kullanın!\n\n" +
                "🔗 Bağlantı Seçenekleri:\n\n" +
                "1️⃣ WiFi Server (Önerilen):\n" +
                "   • Kablosuz\n" +
                "   • Python server çalıştırın\n" +
                "   • <15ms gecikme\n" +
                "   • Windows 11 gesture'ları\n\n" +
                "2️⃣ USB Kablo:\n" +
                "   • Root gerektirmez\n" +
                "   • 0ms gecikme\n" +
                "   • Telefona bağlı\n\n" +
                "3️⃣ Bluetooth HID:\n" +
                "   • Root gerektirir\n" +
                "   • Özel ROM\n\n" +
                "🎮 Desteklenen Hareketler:\n" +
                "• 1 Parmak: Mouse hareket\n" +
                "• 2 Parmak: Sağ tık, scroll\n" +
                "• 3 Parmak: Win+Tab, Show Desktop\n" +
                "• 4 Parmak: Action Center, Virtual Desktop"
            );
            builder.setPositiveButton("Kapat", null);
            
            android.app.AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            }
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ Dialog hatası", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showDeviceListDialog(java.util.List<MouseManager.PCDevice> devices) {
        try {
            // Eğer tek PC varsa direkt seç
            if (devices.size() == 1) {
                MouseManager.PCDevice device = devices.get(0);
                Toast.makeText(getContext(), "🖥️ PC bulundu: " + device.name, Toast.LENGTH_SHORT).show();
                showPinDialog(device);
                return;
            }
            
            // Çoklu PC varsa liste göster
            String[] deviceNames = new String[devices.size()];
            for (int i = 0; i < devices.size(); i++) {
                MouseManager.PCDevice device = devices.get(i);
                deviceNames[i] = device.name + "\n" + device.ipAddress + (device.isPaired ? " ✓" : "");
            }
            
            // PopupWindow kullan - AlertDialog yerine
            showCustomListPopup(deviceNames, devices);
            
        } catch (Exception e) {
            android.util.Log.e("MouseModeView", "Device list hatası", e);
            // Fallback: İlk cihazı seç
            if (!devices.isEmpty()) {
                showPinDialog(devices.get(0));
            }
        }
    }
    
    private void showCustomListPopup(String[] deviceNames, java.util.List<MouseManager.PCDevice> devices) {
        // Basit liste göster - PopupMenu ile
        android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), this);
        
        for (int i = 0; i < deviceNames.length; i++) {
            final int index = i;
            popup.getMenu().add(deviceNames[i]).setOnMenuItemClickListener(item -> {
                showPinDialog(devices.get(index));
                return true;
            });
        }
        
        popup.show();
    }
    
    private void showPinDialog(MouseManager.PCDevice device) {
        try {
            // BASİT YOL: Inline PIN input
            Toast.makeText(getContext(), 
                "📌 " + device.name + "\n" +
                "IP: " + device.ipAddress + "\n\n" +
                "PC'de gösterilen PIN'i gireceksiniz...", 
                Toast.LENGTH_LONG).show();
            
            // Inline PIN input area ekle
            createInlinePinInput(device);
            
        } catch (Exception e) {
            android.util.Log.e("MouseModeView", "PIN input hatası", e);
            Toast.makeText(getContext(), "❌ Bağlantı hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showManualIPDialog() {
        // Manuel IP:PORT girişi - SCROLL ile düzgün layout
        post(() -> {
            if (trackpadArea instanceof FrameLayout) {
                FrameLayout trackpad = (FrameLayout) trackpadArea;
                trackpad.removeAllViews();
                
                // ScrollView ekle
                ScrollView scrollView = new ScrollView(getContext());
                scrollView.setFillViewport(true);
                
                LinearLayout ipContainer = new LinearLayout(getContext());
                ipContainer.setOrientation(LinearLayout.VERTICAL);
                ipContainer.setGravity(Gravity.CENTER_HORIZONTAL);
                ipContainer.setPadding(dp(16), dp(16), dp(16), dp(16));
                
                TextView title = new TextView(getContext());
                title.setText("🔢 PC IP Adresi");
                title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                title.setTextColor(Color.WHITE);
                title.setTypeface(null, Typeface.BOLD);
                title.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
                );
                titleParams.bottomMargin = dp(12);
                ipContainer.addView(title, titleParams);
                
                // IP gösterim text
                TextView ipDisplay = new TextView(getContext());
                ipDisplay.setText("");
                ipDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                ipDisplay.setTextColor(Color.WHITE);
                ipDisplay.setGravity(Gravity.CENTER);
                ipDisplay.setPadding(dp(16), dp(12), dp(16), dp(12));
                ipDisplay.setTypeface(null, Typeface.BOLD);
                ipDisplay.setMinHeight(dp(48));
                ipDisplay.setHint("IP giriniz");
                ipDisplay.setHintTextColor(0xFF8E8E93);
                
                GradientDrawable ipBg = new GradientDrawable();
                ipBg.setColor(0xFF2C2C2E);
                ipBg.setCornerRadius(dp(8));
                ipBg.setStroke(dp(1), 0xFF007AFF);
                ipDisplay.setBackground(ipBg);
                
                LinearLayout.LayoutParams ipDisplayParams = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
                );
                ipDisplayParams.bottomMargin = dp(12);
                ipContainer.addView(ipDisplay, ipDisplayParams);
                
                // Compact numpad - SAFE IMPLEMENTATION
                final StringBuilder ipBuilder = new StringBuilder();
                GridLayout numpad = new GridLayout(getContext());
                numpad.setColumnCount(3);
                numpad.setRowCount(4);
                numpad.setPadding(dp(4), dp(4), dp(4), dp(4));
                
                String[] keys = {"1","2","3","4","5","6","7","8","9",".","0","⌫"};
                for (String key : keys) {
                    Button numBtn = new Button(getContext());
                    numBtn.setText(key);
                    numBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                    numBtn.setTextColor(Color.WHITE);
                    numBtn.setPadding(0, 0, 0, 0);
                    GradientDrawable numBg = new GradientDrawable();
                    numBg.setColor(0xFF3A3A3C);
                    numBg.setCornerRadius(dp(6));
                    numBtn.setBackground(numBg);
                    
                    GridLayout.LayoutParams numParams = new GridLayout.LayoutParams();
                    numParams.width = dp(60);
                    numParams.height = dp(50);
                    numParams.setMargins(dp(3), dp(3), dp(3), dp(3));
                    numBtn.setLayoutParams(numParams);
                    
                    numBtn.setOnClickListener(v -> {
                        try {
                            if (key.equals("⌫")) {
                                if (ipBuilder != null && ipBuilder.length() > 0) {
                                    ipBuilder.deleteCharAt(ipBuilder.length() - 1);
                                }
                            } else {
                                if (ipBuilder != null) {
                                    ipBuilder.append(key);
                                }
                            }
                            if (ipDisplay != null && ipBuilder != null) {
                                ipDisplay.setText(ipBuilder.toString());
                            }
                            vibrate(5);
                        } catch (Exception e) {
                            android.util.Log.e("MouseModeView", "Numpad error", e);
                        }
                    });
                    
                    numpad.addView(numBtn);
                }
                
                LinearLayout.LayoutParams numpadParams = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
                );
                numpadParams.bottomMargin = dp(12);
                ipContainer.addView(numpad, numpadParams);
                
                LinearLayout buttonRow = new LinearLayout(getContext());
                buttonRow.setOrientation(LinearLayout.HORIZONTAL);
                buttonRow.setGravity(Gravity.CENTER);
                
                Button cancelBtn = new Button(getContext());
                cancelBtn.setText("✕");
                cancelBtn.setTextColor(Color.WHITE);
                cancelBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                cancelBtn.setPadding(dp(16), dp(10), dp(16), dp(10));
                GradientDrawable cancelBg = new GradientDrawable();
                cancelBg.setColor(0xFFFF3B30);
                cancelBg.setCornerRadius(dp(6));
                cancelBtn.setBackground(cancelBg);
                cancelBtn.setOnClickListener(v -> init(getContext()));
                
                Button connectBtn = new Button(getContext());
                connectBtn.setText("İleri →");
                connectBtn.setTextColor(Color.WHITE);
                connectBtn.setTypeface(null, Typeface.BOLD);
                connectBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                connectBtn.setPadding(dp(20), dp(10), dp(20), dp(10));
                GradientDrawable connectBg = new GradientDrawable();
                connectBg.setColor(0xFF34C759);
                connectBg.setCornerRadius(dp(6));
                connectBtn.setBackground(connectBg);
                connectBtn.setOnClickListener(v -> {
                    try {
                        String ip = ipBuilder != null ? ipBuilder.toString().trim() : "";
                        if (ip == null || ip.isEmpty()) {
                            vibrate(30);
                            Toast.makeText(getContext(), "⚠️ IP adresi boş!", Toast.LENGTH_SHORT).show();
                        } else {
                            vibrate(10);
                            // IP ile direkt PIN ekranına geç
                            MouseManager.PCDevice device = new MouseManager.PCDevice("Manuel PC", ip, "manual_" + ip);
                            createInlinePinInput(device);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("MouseModeView", "Connect error", e);
                        Toast.makeText(getContext(), "❌ Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
                );
                btnParams.setMargins(dp(6), 0, dp(6), 0);
                buttonRow.addView(cancelBtn, btnParams);
                buttonRow.addView(connectBtn, btnParams);
                
                ipContainer.addView(buttonRow);
                
                // ScrollView'e ekle
                scrollView.addView(ipContainer);
                
                trackpad.addView(scrollView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ));
            }
        });
    }
    
    private void createInlinePinInput(MouseManager.PCDevice device) {
        // Trackpad alanının üstüne PIN input overlay ekle - SCROLL ile
        post(() -> {
            if (trackpadArea instanceof FrameLayout) {
                FrameLayout trackpad = (FrameLayout) trackpadArea;
                trackpad.removeAllViews();
                
                // ScrollView ekle
                ScrollView scrollView = new ScrollView(getContext());
                scrollView.setFillViewport(true);
                
                // PIN input container
                LinearLayout pinContainer = new LinearLayout(getContext());
                pinContainer.setOrientation(LinearLayout.VERTICAL);
                pinContainer.setGravity(Gravity.CENTER_HORIZONTAL);
                pinContainer.setPadding(dp(16), dp(16), dp(16), dp(16));
                
                // Title
                TextView title = new TextView(getContext());
                title.setText("🔐 4 Haneli PIN");
                title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                title.setTextColor(Color.WHITE);
                title.setTypeface(null, Typeface.BOLD);
                title.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
                );
                titleParams.bottomMargin = dp(8);
                pinContainer.addView(title, titleParams);
                
                // Device info
                TextView deviceInfo = new TextView(getContext());
                deviceInfo.setText(device.ipAddress);
                deviceInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                deviceInfo.setTextColor(0xFFAAAAAA);
                deviceInfo.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
                );
                infoParams.bottomMargin = dp(12);
                pinContainer.addView(deviceInfo, infoParams);
                
                // PIN display
                TextView pinDisplay = new TextView(getContext());
                pinDisplay.setText("");
                pinDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
                pinDisplay.setTextColor(Color.WHITE);
                pinDisplay.setGravity(Gravity.CENTER);
                pinDisplay.setPadding(dp(16), dp(12), dp(16), dp(12));
                pinDisplay.setTypeface(null, Typeface.BOLD);
                pinDisplay.setLetterSpacing(0.5f);
                pinDisplay.setMinHeight(dp(48));
                pinDisplay.setHint("- - - -");
                pinDisplay.setHintTextColor(0xFF8E8E93);
                
                GradientDrawable pinBg = new GradientDrawable();
                pinBg.setColor(0xFF2C2C2E);
                pinBg.setCornerRadius(dp(8));
                pinBg.setStroke(dp(1), 0xFF007AFF);
                pinDisplay.setBackground(pinBg);
                
                LinearLayout.LayoutParams pinDisplayParams = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
                );
                pinDisplayParams.bottomMargin = dp(12);
                pinContainer.addView(pinDisplay, pinDisplayParams);
                
                // PIN numpad (compact)
                final StringBuilder pinBuilder = new StringBuilder();
                GridLayout pinNumpad = new GridLayout(getContext());
                pinNumpad.setColumnCount(3);
                pinNumpad.setRowCount(4);
                pinNumpad.setPadding(dp(4), dp(4), dp(4), dp(4));
                
                String[] pinKeys = {"1","2","3","4","5","6","7","8","9","","0","⌫"};
                for (String key : pinKeys) {
                    if (key.isEmpty()) {
                        // Empty space
                        View space = new View(getContext());
                        GridLayout.LayoutParams spaceParams = new GridLayout.LayoutParams();
                        spaceParams.width = dp(60);
                        spaceParams.height = dp(50);
                        spaceParams.setMargins(dp(3), dp(3), dp(3), dp(3));
                        space.setLayoutParams(spaceParams);
                        pinNumpad.addView(space);
                        continue;
                    }
                    
                    Button pinBtn = new Button(getContext());
                    pinBtn.setText(key);
                    pinBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                    pinBtn.setTextColor(Color.WHITE);
                    pinBtn.setPadding(0, 0, 0, 0);
                    GradientDrawable pinBtnBg = new GradientDrawable();
                    pinBtnBg.setColor(0xFF3A3A3C);
                    pinBtnBg.setCornerRadius(dp(6));
                    pinBtn.setBackground(pinBtnBg);
                    
                    GridLayout.LayoutParams pinBtnParams = new GridLayout.LayoutParams();
                    pinBtnParams.width = dp(60);
                    pinBtnParams.height = dp(50);
                    pinBtnParams.setMargins(dp(3), dp(3), dp(3), dp(3));
                    pinBtn.setLayoutParams(pinBtnParams);
                    
                    pinBtn.setOnClickListener(v -> {
                        if (key.equals("⌫")) {
                            if (pinBuilder.length() > 0) {
                                pinBuilder.deleteCharAt(pinBuilder.length() - 1);
                            }
                        } else {
                            if (pinBuilder.length() < 4) {
                                pinBuilder.append(key);
                            }
                        }
                        pinDisplay.setText(pinBuilder.toString());
                        vibrate(5);
                    });
                    
                    pinNumpad.addView(pinBtn);
                }
                
                LinearLayout.LayoutParams pinNumpadParams = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
                );
                pinNumpadParams.bottomMargin = dp(12);
                pinContainer.addView(pinNumpad, pinNumpadParams);
                
                // Buttons
                LinearLayout buttonRow = new LinearLayout(getContext());
                buttonRow.setOrientation(LinearLayout.HORIZONTAL);
                buttonRow.setGravity(Gravity.CENTER);
                
                Button cancelBtn = new Button(getContext());
                cancelBtn.setText("✕");
                cancelBtn.setTextColor(Color.WHITE);
                cancelBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                cancelBtn.setPadding(dp(16), dp(10), dp(16), dp(10));
                GradientDrawable cancelBg = new GradientDrawable();
                cancelBg.setColor(0xFFFF3B30);
                cancelBg.setCornerRadius(dp(6));
                cancelBtn.setBackground(cancelBg);
                cancelBtn.setOnClickListener(v -> {
                    // PIN input'u kapat, normal trackpad'e dön
                    init(getContext());
                });
                
                Button connectBtn = new Button(getContext());
                connectBtn.setText("Bağlan ✓");
                connectBtn.setTextColor(Color.WHITE);
                connectBtn.setTypeface(null, Typeface.BOLD);
                connectBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                connectBtn.setPadding(dp(20), dp(10), dp(20), dp(10));
                GradientDrawable connectBg = new GradientDrawable();
                connectBg.setColor(0xFF34C759);
                connectBg.setCornerRadius(dp(6));
                connectBtn.setBackground(connectBg);
                connectBtn.setOnClickListener(v -> {
                    String pin = pinBuilder.toString().trim();
                    if (pin.length() != 4) {
                        vibrate(30);
                        Toast.makeText(getContext(), "⚠️ PIN 4 haneli olmalı!", Toast.LENGTH_SHORT).show();
                    } else {
                        vibrate(10);
                        updateStatus("🔗 Bağlanıyor...", 0xFF007AFF);
                        mouseManager.connect(device, pin);
                        // Trackpad'e geri dön - BAĞLANTI SONRASI
                        // Klavye kapanmaması için init'i çağırmıyoruz hemen
                        Toast.makeText(getContext(), "✅ Bağlandı! Trackpad aktif.", Toast.LENGTH_SHORT).show();
                        android.os.Handler handler = new android.os.Handler();
                        handler.postDelayed(() -> {
                            // Sadece PIN ekranını kapat, trackpad göster
                            if (trackpadArea instanceof FrameLayout) {
                                FrameLayout trackpadFrame = (FrameLayout) trackpadArea;
                                trackpadFrame.removeAllViews();
                                
                                // Trackpad hint
                                TextView hint = new TextView(getContext());
                                hint.setText("🖱️ Trackpad Aktif\n\nParmağınızı hareket ettirin");
                                hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                                hint.setTextColor(0xFF8E8E93);
                                hint.setGravity(Gravity.CENTER);
                                hint.setTypeface(null, Typeface.BOLD);
                                trackpadFrame.addView(hint, new FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                    Gravity.CENTER
                                ));
                            }
                        }, 500);
                    }
                });
                
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
                );
                btnParams.setMargins(dp(6), 0, dp(6), 0);
                buttonRow.addView(cancelBtn, btnParams);
                buttonRow.addView(connectBtn, btnParams);
                
                pinContainer.addView(buttonRow);
                
                // ScrollView'e ekle
                scrollView.addView(pinContainer);
                
                trackpad.addView(scrollView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ));
            }
        });
    }
    
    private void updateStatus(String text, int color) {
        post(() -> {
            if (statusText != null) {
                statusText.setText(text);
                statusText.setTextColor(color);
            }
        });
    }
    
    private void updateLatency(int latencyMs) {
        post(() -> {
            if (latencyText != null) {
                String quality;
                int color;
                
                if (latencyMs < 20) {
                    quality = "⚡⚡⚡⚡⚡";
                    color = 0xFF34C759;
                } else if (latencyMs < 50) {
                    quality = "⚡⚡⚡⚡";
                    color = 0xFF34C759;
                } else if (latencyMs < 100) {
                    quality = "⚡⚡⚡";
                    color = 0xFFFFCC00;
                } else {
                    quality = "⚡⚡";
                    color = 0xFFFF9500;
                }
                
                latencyText.setText(String.format("Gecikme: %dms %s", latencyMs, quality));
                latencyText.setTextColor(color);
            }
        });
    }
    
    private void vibrate(int... durations) {
        android.os.Vibrator vibrator = (android.os.Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                long[] pattern = new long[durations.length * 2];
                for (int i = 0; i < durations.length; i++) {
                    pattern[i * 2] = (i == 0) ? 0 : 5;
                    pattern[i * 2 + 1] = durations[i];
                }
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(durations[0]);
            }
        }
    }
    
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
    
    public void cleanup() {
        if (mouseManager != null) {
            mouseManager.cleanup();
        }
    }
}

