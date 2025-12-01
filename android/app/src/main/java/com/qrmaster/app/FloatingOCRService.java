package com.qrmaster.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FloatingOCRService extends LifecycleService {
    
    private static final String CHANNEL_ID = "FloatingOCRChannel";
    private static final int NOTIFICATION_ID = 2002;
    private static final long SCAN_COOLDOWN = 2000; // 2 saniye cooldown
    
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    
    // CameraX
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ExecutorService cameraExecutor;
    
    // ML Kit Text Recognition
    private TextRecognizer textRecognizer;
    
    // UI Elements
    private PreviewView previewView;
    private ImageView closeButton;
    private ImageView flashButton;
    private ImageView dragHandle;
    private ImageView resizeHandle;
    private ImageView captureButton; // 📸 Fotoğraf çekme butonu
    private ImageView modeToggleButton; // 🔄 Mod değiştirme butonu
    private TextView titleText;
    private TextView instructionText;
    private TextView lastResultText;
    private TextView lastResultMeta;
    
    // State
    private boolean flashOn = false;
    private long lastScanTime = 0;
    private long lastResultTimestamp = 0;
    
    // 🎯 SCAN MODE
    private enum ScanMode {
        CONTINUOUS,  // Sürekli tarama (canlı)
        PHOTO        // Fotoğraf modu (butona bas)
    }
    private ScanMode currentMode = ScanMode.CONTINUOUS; // Varsayılan: Sürekli
    
    // Touch handling
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isResizing = false;
    private int initialWidth, initialHeight;
    
    // Resize limits
    private static final int MIN_WIDTH = 400;
    private static final int MIN_HEIGHT = 500;
    private static final int MAX_WIDTH = 1200;
    private static final int MAX_HEIGHT = 1800;
    
    // Ses efekti
    private MediaPlayer scanSoundPlayer;
    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            android.util.Log.i("FloatingOCRService", "🚀 Service onCreate başlatılıyor...");
            
            // 🇹🇷 ML Kit Text Recognizer başlat - Latin script (Türkçe tam destek!)
            // Latin alfabesi: a-z, A-Z + Türkçe özel karakterler (ç,ğ,ı,ö,ş,ü,Ç,Ğ,İ,Ö,Ş,Ü)
            textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            android.util.Log.i("FloatingOCRService", "✅ ML Kit Text Recognizer hazır (Latin + Türkçe)");
            
            // Kamera executor
            cameraExecutor = Executors.newSingleThreadExecutor();
            
            // Ses efekti yükle
            try {
                scanSoundPlayer = MediaPlayer.create(this, R.raw.casual_click_pop_ui_2_262119);
                if (scanSoundPlayer != null) {
                    scanSoundPlayer.setVolume(1.0f, 1.0f);
                    android.util.Log.i("FloatingOCRService", "✅ Scan sesi yüklendi");
                } else {
                    android.util.Log.w("FloatingOCRService", "⚠️ Scan sesi yüklenemedi");
                }
            } catch (Exception e) {
                android.util.Log.e("FloatingOCRService", "❌ Scan sesi hatası: " + e.getMessage());
            }
            
            mainHandler = new Handler();
            
            android.util.Log.i("FloatingOCRService", "✅ Service onCreate tamamlandı");
            
        } catch (Exception e) {
            android.util.Log.e("FloatingOCRService", "❌ onCreate hatası: " + e.getMessage());
            e.printStackTrace();
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        
        try {
            android.util.Log.i("FloatingOCRService", "▶️ onStartCommand başlatılıyor...");
            
            // Foreground notification başlat
            startForeground(NOTIFICATION_ID, createNotification());
            
            // Floating view oluştur
            createFloatingView();
            
            android.util.Log.i("FloatingOCRService", "✅ Service başarıyla başlatıldı");
            
        } catch (Exception e) {
            android.util.Log.e("FloatingOCRService", "❌ onStartCommand hatası: " + e.getMessage());
            e.printStackTrace();
            
            Toast.makeText(this, "❌ OCR başlatma hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
        }
        
        return START_STICKY;
    }

    private void createFloatingView() {
        try {
            android.util.Log.i("FloatingOCRService", "🎨 Floating view oluşturuluyor...");
            
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            floatingView = inflater.inflate(R.layout.floating_ocr_layout, null);
            
            // UI elementlerini bağla
            previewView = floatingView.findViewById(R.id.ocr_preview);
            closeButton = floatingView.findViewById(R.id.ocr_close_button);
            flashButton = floatingView.findViewById(R.id.ocr_flash_button);
            dragHandle = floatingView.findViewById(R.id.ocr_drag_handle);
            resizeHandle = floatingView.findViewById(R.id.ocr_resize_handle);
            captureButton = floatingView.findViewById(R.id.ocr_capture_button);
            modeToggleButton = floatingView.findViewById(R.id.ocr_mode_toggle_button);
            titleText = floatingView.findViewById(R.id.ocr_title);
            instructionText = floatingView.findViewById(R.id.ocr_instruction);
            lastResultText = floatingView.findViewById(R.id.ocr_last_result_text);
            lastResultMeta = floatingView.findViewById(R.id.ocr_last_result_meta);
            if (lastResultMeta != null) {
                lastResultMeta.setText("⏳ Bekleniyor");
            }
            
            // Layout parametreleri
            int layoutType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutType = WindowManager.LayoutParams.TYPE_PHONE;
            }
            
            params = new WindowManager.LayoutParams(
                900, // width
                1100, // height
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 50;
            params.y = 100;
            
            // Kapat butonu
            closeButton.setOnClickListener(v -> {
                android.util.Log.i("FloatingOCRService", "🛑 Kapat butonuna tıklandı");
                stopSelf();
            });
            
            // Flaş butonu
            flashButton.setOnClickListener(v -> toggleFlash());
            
            // 🔄 Mod değiştirme butonu
            modeToggleButton.setOnClickListener(v -> toggleScanMode());
            
            // 📸 Fotoğraf çekme butonu
            captureButton.setOnClickListener(v -> captureAndScan());
            
            // Drag handle - Sadece taşıma için
            dragHandle.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            if (isResizing) return false;
                            
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            dragHandle.setAlpha(0.7f);
                            return true;
                            
                        case MotionEvent.ACTION_MOVE:
                            if (isResizing) return false;
                            
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            
                            if (windowManager != null && floatingView != null) {
                                windowManager.updateViewLayout(floatingView, params);
                            }
                            return true;
                            
                        case MotionEvent.ACTION_UP:
                            dragHandle.setAlpha(1.0f);
                            return true;
                    }
                    return false;
                }
            });
            
            // Resize handle
            resizeHandle.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            isResizing = true;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            initialWidth = params.width;
                            initialHeight = params.height;
                            resizeHandle.setAlpha(0.7f);
                            return true;
                            
                        case MotionEvent.ACTION_MOVE:
                            int newWidth = initialWidth + (int) (event.getRawX() - initialTouchX);
                            int newHeight = initialHeight + (int) (event.getRawY() - initialTouchY);
                            
                            params.width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, newWidth));
                            params.height = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, newHeight));
                            
                            if (windowManager != null && floatingView != null) {
                                windowManager.updateViewLayout(floatingView, params);
                            }
                            return true;
                            
                        case MotionEvent.ACTION_UP:
                            isResizing = false;
                            resizeHandle.setAlpha(1.0f);
                            return true;
                    }
                    return false;
                }
            });
            
            // Floating view'i ekle
            windowManager.addView(floatingView, params);
            android.util.Log.i("FloatingOCRService", "✅ Floating view eklendi");
            
            // Kamerayı başlat
            startCamera();
            
        } catch (Exception e) {
            android.util.Log.e("FloatingOCRService", "❌ createFloatingView hatası: " + e.getMessage());
            e.printStackTrace();
            
            Toast.makeText(this, "❌ OCR görünüm hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private void startCamera() {
        try {
            android.util.Log.i("FloatingOCRService", "📷 Kamera başlatılıyor...");
            
            ProcessCameraProvider.getInstance(this).addListener(() -> {
                try {
                    cameraProvider = ProcessCameraProvider.getInstance(this).get();
                    
                    // Preview
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());
                    
                    // Image Analysis - ML Kit Text Recognition (YÜK SEK ÇÖZÜNÜRLÜK)
                    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new android.util.Size(1280, 720)) // 720p kalite
                        .build();
                    
                    imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                        // 🎯 SADECE SÜREKLI MODDA TAR
                        if (currentMode != ScanMode.CONTINUOUS) {
                            imageProxy.close();
                            return; // Fotoğraf modundaysa otomatik tarama yapma
                        }
                        
                        @androidx.camera.core.ExperimentalGetImage
                        android.media.Image mediaImage = imageProxy.getImage();
                        
                        if (mediaImage != null) {
                            InputImage image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.getImageInfo().getRotationDegrees()
                            );
                            
                            // ML Kit Text Recognition
                            textRecognizer.process(image)
                                .addOnSuccessListener(visionText -> {
                                    if (visionText.getText().length() > 0) {
                                        handleTextDetected(visionText);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("FloatingOCRService", "❌ OCR hatası: " + e.getMessage());
                                })
                                .addOnCompleteListener(task -> {
                                    imageProxy.close();
                                });
                        } else {
                            imageProxy.close();
                        }
                    });
                    
                    // Kamera selector (arka kamera)
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                    
                    // Unbind tüm use case'ler
                    cameraProvider.unbindAll();
                    
                    // Bind camera
                    camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    );
                    
                    android.util.Log.i("FloatingOCRService", "✅ Kamera başarıyla başlatıldı");
                    
                } catch (Exception e) {
                    android.util.Log.e("FloatingOCRService", "❌ Kamera başlatma hatası: " + e.getMessage());
                    e.printStackTrace();
                }
            }, getMainExecutor());
            
        } catch (Exception e) {
            android.util.Log.e("FloatingOCRService", "❌ startCamera hatası: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 🔄 Mod değiştirme: Sürekli ↔ Fotoğraf
     */
    private void toggleScanMode() {
        if (currentMode == ScanMode.CONTINUOUS) {
            // Fotoğraf moduna geç
            currentMode = ScanMode.PHOTO;
            captureButton.setVisibility(View.VISIBLE); // Fotoğraf butonunu göster
            titleText.setText("📸 FOTO MODU");
            instructionText.setText("📸 Butona basıp fotoğraf çekin");
            modeToggleButton.setAlpha(0.5f); // Mod butonunu sönükleştir
            Toast.makeText(this, "📸 Fotoğraf modu: Butona basın", Toast.LENGTH_SHORT).show();
            android.util.Log.i("FloatingOCRService", "📸 Fotoğraf moduna geçildi");
        } else {
            // Sürekli tarama moduna geç
            currentMode = ScanMode.CONTINUOUS;
            captureButton.setVisibility(View.GONE); // Fotoğraf butonunu gizle
            titleText.setText("📝 YAZI TANIMA");
            instructionText.setText("📝 Kamerayı yazıya tutun");
            modeToggleButton.setAlpha(1.0f); // Mod butonunu parlat
            Toast.makeText(this, "🔄 Sürekli tarama modu", Toast.LENGTH_SHORT).show();
            android.util.Log.i("FloatingOCRService", "🔄 Sürekli tarama moduna geçildi");
        }
    }
    
    /**
     * 📸 Fotoğraf çek ve tara
     */
    private void captureAndScan() {
        if (currentMode != ScanMode.PHOTO) {
            return; // Sadece fotoğraf modundayken çalış
        }
        
        android.util.Log.i("FloatingOCRService", "📸 Fotoğraf çekiliyor...");
        
        // UI feedback
        captureButton.setAlpha(0.5f);
        instructionText.setText("📸 Fotoğraf çekiliyor...");
        
        // Flaş efekti (varsa)
        if (flashOn && camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(true);
            mainHandler.postDelayed(() -> {
                if (camera != null) {
                    camera.getCameraControl().enableTorch(false);
                }
            }, 100);
        }
        
        // Ses efekti çal
        playScanSound();
        
        // ImageCapture kullanmadan, mevcut preview'dan bitmap al
        mainHandler.postDelayed(() -> {
            try {
                // PreviewView'dan bitmap al
                android.graphics.Bitmap bitmap = previewView.getBitmap();
                
                if (bitmap != null) {
                    // ML Kit ile tara
                    InputImage image = InputImage.fromBitmap(bitmap, 0);
                    
                    textRecognizer.process(image)
                        .addOnSuccessListener(visionText -> {
                            if (visionText.getText().length() > 0) {
                                handleTextDetected(visionText);
                                instructionText.setText("✅ Yazı algılandı!");
                            } else {
                                instructionText.setText("❌ Yazı bulunamadı");
                                Toast.makeText(this, "Yazı bulunamadı, tekrar deneyin", Toast.LENGTH_SHORT).show();
                            }
                            captureButton.setAlpha(1.0f);
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("FloatingOCRService", "❌ OCR hatası: " + e.getMessage());
                            instructionText.setText("❌ Hata!");
                            Toast.makeText(this, "OCR hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            captureButton.setAlpha(1.0f);
                        });
                } else {
                    android.util.Log.e("FloatingOCRService", "❌ Bitmap alınamadı");
                    instructionText.setText("❌ Kamera hatası");
                    captureButton.setAlpha(1.0f);
                }
                
            } catch (Exception e) {
                android.util.Log.e("FloatingOCRService", "❌ Fotoğraf çekme hatası: " + e.getMessage());
                instructionText.setText("❌ Hata!");
                captureButton.setAlpha(1.0f);
            }
        }, 100); // Küçük gecikme
    }
    
    private void handleTextDetected(Text visionText) {
        // 🎯 MOD KONTROLÜ - Sadece sürekli modda otomatik tara
        long currentTime = System.currentTimeMillis();
        
        if (currentMode == ScanMode.PHOTO) {
            // Fotoğraf modunda, sadece butona basıldığında buraya gelir
            // Cooldown yok, direkt işle
        } else {
            // Sürekli modda cooldown kontrolü
            if (currentTime - lastScanTime < SCAN_COOLDOWN) {
                return;
            }
        }
        
        // 🧠 AKILLI OCR İŞLEME - SmartOCRProcessor kullan
        String detectedText = SmartOCRProcessor.processText(visionText);
        
        // 🎯 KALİTE FİLTRELERİ
        if (detectedText == null || detectedText.trim().isEmpty()) {
            android.util.Log.d("FloatingOCRService", "⚠️ Boş metin, atlandı");
            return; // Boş metin
        }
        
        // Minimum 3 karakter olmalı (tek harf/rakam algılamayı engelle)
        if (detectedText.trim().length() < 3) {
            android.util.Log.d("FloatingOCRService", "⚠️ Çok kısa metin, atlandı: " + detectedText);
            return;
        }
        
        // Cooldown'u geçtik - şimdi işle
        lastScanTime = currentTime;
        
        // 📊 Metin istatistikleri
        SmartOCRProcessor.TextStats stats = SmartOCRProcessor.analyzeText(detectedText);
        android.util.Log.i("FloatingOCRService", "📝 Yazı algılandı: " + stats.toString());
        android.util.Log.i("FloatingOCRService", "📄 İlk 100 karakter: " + detectedText.substring(0, Math.min(100, detectedText.length())));
        
        // Türkçe karakter kontrolü
        if (stats.hasTurkish) {
            android.util.Log.i("FloatingOCRService", "🇹🇷 Türkçe karakterler tespit edildi!");
        }
        
        // Confidence hesapla (satır sayısı + karakter sayısı + Türkçe bonusu)
        float lineConfidence = Math.min(0.4f, stats.lineCount * 0.1f);
        float charConfidence = Math.min(0.4f, stats.charCount / 200.0f);
        float turkishBonus = stats.hasTurkish ? 0.2f : 0.0f;
        float confidence = Math.min(1.0f, lineConfidence + charConfidence + turkishBonus);
        
        android.util.Log.i("FloatingOCRService", "🎯 Güven skoru: " + String.format("%.2f", confidence));
        
        // Ses efekti ve flaş
        playScanSound();
        playFlashEffect();
        
        final long eventTimestamp = System.currentTimeMillis();
        lastResultTimestamp = eventTimestamp;
        
        boolean accessibilityEnabled = QRAccessibilityService.isServiceEnabled();
        updateLastResultPreview(detectedText, stats, confidence, eventTimestamp, accessibilityEnabled);
        
        // Accessibility servisine ilet (servis kapalıysa sadece clipboard)
        QRAccessibilityService.pasteOCRText(this, detectedText);
        
        if (accessibilityEnabled) {
            evaluateAutoFillResult(detectedText, stats, confidence, eventTimestamp, 0);
        } else {
            finalizeResultAndNotify(detectedText, stats, confidence, eventTimestamp, false, false);
        }
    }

    private void toggleFlash() {
        if (camera == null || !camera.getCameraInfo().hasFlashUnit()) {
            Toast.makeText(this, "⚠️ Flaş mevcut değil", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            flashOn = !flashOn;
            camera.getCameraControl().enableTorch(flashOn);
            
            flashButton.setAlpha(flashOn ? 1.0f : 0.6f);
            
            android.util.Log.i("FloatingOCRService", "💡 Flaş: " + (flashOn ? "AÇIK" : "KAPALI"));
            
        } catch (Exception e) {
            android.util.Log.e("FloatingOCRService", "❌ Flaş hatası: " + e.getMessage());
        }
    }

    private void playScanSound() {
        try {
            if (scanSoundPlayer != null) {
                if (scanSoundPlayer.isPlaying()) {
                    scanSoundPlayer.seekTo(0);
                }
                scanSoundPlayer.start();
                android.util.Log.i("FloatingOCRService", "🔊 Scan sesi çalıyor");
            }
        } catch (Exception e) {
            android.util.Log.e("FloatingOCRService", "❌ Ses çalma hatası: " + e.getMessage());
        }
    }

    private void playFlashEffect() {
        if (camera == null || !camera.getCameraInfo().hasFlashUnit()) {
            android.util.Log.w("FloatingOCRService", "⚠️ Flaş yok");
            return;
        }
        
        try {
            camera.getCameraControl().enableTorch(true);
            android.util.Log.i("FloatingOCRService", "⚡ Flaş açıldı");
            
            mainHandler.postDelayed(() -> {
                try {
                    if (camera != null && !flashOn) {
                        camera.getCameraControl().enableTorch(false);
                        android.util.Log.i("FloatingOCRService", "⚡ Flaş kapatıldı");
                    }
                } catch (Exception e) {
                    android.util.Log.e("FloatingOCRService", "❌ Flaş kapatma hatası: " + e.getMessage());
                }
            }, 150);
            
        } catch (Exception e) {
            android.util.Log.e("FloatingOCRService", "❌ Flaş efekti hatası: " + e.getMessage());
        }
    }

    private void updateLastResultPreview(String text, SmartOCRProcessor.TextStats stats, float confidence, long timestamp, boolean accessibilityEnabled) {
        final String shortened = shortenMultiline(text, 240);
        final String statsLine = buildStatsLine(stats, confidence);
        final String timePart = formatTime(timestamp);
        final String statusText = accessibilityEnabled
            ? "🧠 Otomatik doldurma deneniyor..."
            : "📋 Panoya kopyalandı (Servis kapalı)";
        final int statusColor = accessibilityEnabled ? 0xFF60A5FA : 0xFFFBBF24;
        
        mainHandler.post(() -> {
            if (timestamp != lastResultTimestamp) return;
            if (lastResultText != null) {
                lastResultText.setText(shortened);
            }
            if (lastResultMeta != null) {
                lastResultMeta.setText("⏰ " + timePart + "  •  " + statusText + "\n" + statsLine);
                lastResultMeta.setTextColor(statusColor);
            }
        });
    }
    
    private void finalizeResultAndNotify(String text, SmartOCRProcessor.TextStats stats, float confidence,
                                         long timestamp, boolean autoFillAttempted, boolean autoFillSuccess) {
        updateLastResultFinalUI(text, stats, confidence, timestamp, autoFillAttempted, autoFillSuccess);
        showOcrSuccessToast(text, autoFillAttempted, autoFillSuccess);
        OCRPlugin.notifyTextScanned(
            text,
            timestamp,
            confidence,
            stats.lineCount,
            stats.charCount,
            stats.wordCount,
            stats.turkishCharCount,
            stats.hasTurkish,
            autoFillAttempted,
            autoFillSuccess
        );
    }
    
    private void updateLastResultFinalUI(String text, SmartOCRProcessor.TextStats stats, float confidence,
                                         long timestamp, boolean autoFillAttempted, boolean autoFillSuccess) {
        final String shortened = shortenMultiline(text, 260);
        final String statsLine = buildStatsLine(stats, confidence);
        final String timePart = formatTime(timestamp);
        final String statusText;
        final int statusColor;
        
        if (!autoFillAttempted) {
            statusText = "📋 Panoya kopyalandı (Servis kapalı)";
            statusColor = 0xFFFBBF24;
        } else if (autoFillSuccess) {
            statusText = "✅ Otomatik dolduruldu";
            statusColor = 0xFF10B981;
        } else {
            statusText = "⚠️ Otomatik doldurulamadı (Panoya kopyalandı)";
            statusColor = 0xFFF87171;
        }
        
        mainHandler.post(() -> {
            if (timestamp != lastResultTimestamp) return;
            if (lastResultText != null) {
                lastResultText.setText(shortened);
            }
            if (lastResultMeta != null) {
                lastResultMeta.setText("⏰ " + timePart + "  •  " + statusText + "\n" + statsLine);
                lastResultMeta.setTextColor(statusColor);
            }
        });
    }
    
    private void evaluateAutoFillResult(String text, SmartOCRProcessor.TextStats stats, float confidence,
                                        long timestamp, int attempt) {
        if (timestamp != lastResultTimestamp) {
            QRAccessibilityService.clearWriteStatus(timestamp);
            return;
        }
        
        QRAccessibilityService.WriteStatus status = QRAccessibilityService.peekWriteStatus(timestamp);
        
        if (!status.attempted) {
            if (attempt < 6) {
                mainHandler.postDelayed(() -> evaluateAutoFillResult(text, stats, confidence, timestamp, attempt + 1),
                    150L * (attempt + 1));
            } else {
                finalizeResultAndNotify(text, stats, confidence, timestamp, false, false);
            }
            return;
        }
        
        if (!status.completed) {
            if (attempt < 6) {
                mainHandler.postDelayed(() -> evaluateAutoFillResult(text, stats, confidence, timestamp, attempt + 1),
                    150L * (attempt + 1));
            } else {
                QRAccessibilityService.clearWriteStatus(timestamp);
                finalizeResultAndNotify(text, stats, confidence, timestamp, true, false);
            }
            return;
        }
        
        QRAccessibilityService.clearWriteStatus(timestamp);
        finalizeResultAndNotify(text, stats, confidence, timestamp, true, status.success);
    }
    
    private String buildStatsLine(SmartOCRProcessor.TextStats stats, float confidence) {
        int confidencePercent = Math.round(confidence * 100f);
        StringBuilder builder = new StringBuilder();
        builder.append("📏 Satır: ").append(stats.lineCount);
        builder.append("  •  Kelime: ").append(stats.wordCount);
        builder.append("  •  Karakter: ").append(stats.charCount);
        builder.append("  •  Güven: ").append(confidencePercent).append("%");
        if (stats.hasTurkish) {
            builder.append("  •  🇹🇷 ").append(stats.turkishCharCount).append(" Türkçe harf");
        }
        return builder.toString();
    }
    
    private void showOcrSuccessToast(String text, boolean autoFillAttempted, boolean autoFillSuccess) {
        mainHandler.post(() -> {
            String preview = shortenMultiline(text, 120);
            String message;
            if (!autoFillAttempted) {
                message = "📋 Yazı panoya kopyalandı:\n" + preview;
            } else if (autoFillSuccess) {
                message = "✅ Yazı otomatik dolduruldu:\n" + preview;
            } else {
                message = "⚠️ Yazı otomatik doldurulamadı.\nPanoya kopyalandı:\n" + preview;
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            
            if (instructionText != null) {
                if (!autoFillAttempted) {
                    instructionText.setText("📋 Panoya kopyalandı");
                    instructionText.setTextColor(0xFFFBBF24);
                } else if (autoFillSuccess) {
                    instructionText.setText("✅ Yazı aktarıldı");
                    instructionText.setTextColor(0xFF10B981);
                } else {
                    instructionText.setText("⚠️ Aktarılamadı (Panoya kopyalandı)");
                    instructionText.setTextColor(0xFFF87171);
                }
                
                mainHandler.postDelayed(() -> {
                    if (instructionText != null) {
                        instructionText.setText("📝 Kamerayı yazıya tutun");
                        instructionText.setTextColor(0xFFFFFFFF);
                    }
                }, 2500);
            }
        });
    }
    
    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    private String shortenMultiline(String text, int limit) {
        if (text == null) return "";
        if (text.length() <= limit) return text;
        return text.substring(0, Math.max(0, limit - 1)) + "…";
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Floating OCR Scanner",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Floating OCR text scanner is running");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        
        Intent notificationIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (notificationIntent != null) {
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📝 OCR Tarayıcı Aktif")
            .setContentText("Yazıları taramak için kamerayı kullanın")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        android.util.Log.i("FloatingOCRService", "🛑 Service onDestroy...");
        
        // MediaPlayer temizle
        if (scanSoundPlayer != null) {
            if (scanSoundPlayer.isPlaying()) {
                scanSoundPlayer.stop();
            }
            scanSoundPlayer.release();
            scanSoundPlayer = null;
        }
        
        // Handler callback'lerini temizle
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        
        // Kamera kapat
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        
        if (textRecognizer != null) {
            textRecognizer.close();
        }
        
        // Floating view kaldır
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
        
        android.util.Log.i("FloatingOCRService", "✅ Service temizlendi");
    }

    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }
}

