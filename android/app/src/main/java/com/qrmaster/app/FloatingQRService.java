package com.qrmaster.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.qrmaster.app.R;
// MainActivity import'una gerek yok - context üzerinden çalışacağız
import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FloatingQRService extends LifecycleService {
    
    private static final String CHANNEL_ID = "FloatingQRChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    private WindowManager windowManager;
    private View floatingView;
    private PreviewView previewView;
    private ImageView closeButton;
    private ImageView flashButton;
    private ImageView resizeHandle;
    private ImageView dragHandle;
    private TextView lastResultText;
    private TextView lastResultMeta;
    
    private WindowManager.LayoutParams params;
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    
    // Resize için
    private int initialWidth;
    private int initialHeight;
    private boolean isResizing = false;
    private static final int MIN_WIDTH = 200;
    private static final int MIN_HEIGHT = 300;
    private static final int MAX_WIDTH = 500;
    private static final int MAX_HEIGHT = 800;
    
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;
    
    private boolean flashOn = false;
    private long lastScanTime = 0;
    private static final long SCAN_COOLDOWN = 1000; // 1 saniye cooldown
    private long lastResultTimestamp = 0;
    
    private MediaPlayer scanSoundPlayer;
    private Handler mainHandler;
    
    @Override
    public void onCreate() {
        super.onCreate();
        android.util.Log.i("FloatingQRService", "🚀 Service onCreate başladı");
        
        try {
            // Notification channel oluştur
            createNotificationChannel();
            android.util.Log.i("FloatingQRService", "✅ Notification channel oluşturuldu");
            
            // Foreground service başlat
            startForeground(NOTIFICATION_ID, createNotification());
            android.util.Log.i("FloatingQRService", "✅ Foreground service başlatıldı");
            
            // Camera executor
            cameraExecutor = Executors.newSingleThreadExecutor();
            android.util.Log.i("FloatingQRService", "✅ Camera executor hazır");
            
            // Main handler
            mainHandler = new Handler(Looper.getMainLooper());
            
            // Scan ses efekti yükle
            try {
                scanSoundPlayer = MediaPlayer.create(this, R.raw.casual_click_pop_ui_2_262119);
                if (scanSoundPlayer != null) {
                    scanSoundPlayer.setVolume(1.0f, 1.0f);
                    android.util.Log.i("FloatingQRService", "✅ Scan sesi yüklendi");
                } else {
                    android.util.Log.w("FloatingQRService", "⚠️ Scan sesi yüklenemedi");
                }
            } catch (Exception e) {
                android.util.Log.e("FloatingQRService", "❌ Scan sesi hatası: " + e.getMessage());
            }
            
            // Barcode scanner
            BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39
                )
                .build();
            
            barcodeScanner = BarcodeScanning.getClient(options);
            android.util.Log.i("FloatingQRService", "✅ Barcode scanner hazır");
            
            // Floating view oluştur
            createFloatingView();
            android.util.Log.i("FloatingQRService", "✅ Floating view oluşturuldu");
            
            // Kamera başlat
            startCamera();
            android.util.Log.i("FloatingQRService", "✅ Kamera başlatıldı");
            
            android.util.Log.i("FloatingQRService", "🎉 Service başarıyla başlatıldı!");
            
        } catch (Exception e) {
            android.util.Log.e("FloatingQRService", "❌ Service başlatma hatası: " + e.getMessage());
            e.printStackTrace();
            
            // Hata durumunda Toast göster
            Toast.makeText(this, "Floating Scanner başlatılamadı: " + e.getMessage(), Toast.LENGTH_LONG).show();
            
            // Service'i durdur
            stopSelf();
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Floating QR Scanner",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("QR kod tarama servisi çalışıyor");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        // MainActivity.class yerine package manager'dan launch intent al
        Intent notificationIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (notificationIntent == null) {
            notificationIntent = new Intent();
        }
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎯 Floating QR Scanner")
            .setContentText("QR kod tarama aktif - Dokunarak konumlandırın")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }
    
    private void createFloatingView() {
        try {
            android.util.Log.i("FloatingQRService", "🔧 createFloatingView başladı");
            
            // Layout inflate
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_qr_layout, null);
            android.util.Log.i("FloatingQRService", "✅ Layout inflate edildi");
            
            // View'ları bul
            previewView = floatingView.findViewById(R.id.preview_view);
            closeButton = floatingView.findViewById(R.id.close_button);
            flashButton = floatingView.findViewById(R.id.flash_button);
            resizeHandle = floatingView.findViewById(R.id.resize_handle);
            dragHandle = floatingView.findViewById(R.id.drag_handle);
            lastResultText = floatingView.findViewById(R.id.last_result_text);
            lastResultMeta = floatingView.findViewById(R.id.last_result_meta);
            android.util.Log.i("FloatingQRService", "✅ View'lar bulundu");
            
            if (lastResultMeta != null) {
                lastResultMeta.setText("⏳ Bekleniyor");
            }
            
            // Close button
            closeButton.setOnClickListener(v -> stopSelf());
            
            // Flash button
            flashButton.setOnClickListener(v -> toggleFlash());
            
            // Resize handle
            resizeHandle.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            isResizing = true;
                            initialWidth = params.width;
                            initialHeight = params.height;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                            
                        case MotionEvent.ACTION_MOVE:
                            if (isResizing) {
                                float deltaX = event.getRawX() - initialTouchX;
                                float deltaY = event.getRawY() - initialTouchY;
                                
                                int newWidth = (int) (initialWidth + deltaX);
                                int newHeight = (int) (initialHeight + deltaY);
                                
                                // Min/Max sınırları
                                int density = (int) getResources().getDisplayMetrics().density;
                                newWidth = Math.max(MIN_WIDTH * density, Math.min(MAX_WIDTH * density, newWidth));
                                newHeight = Math.max(MIN_HEIGHT * density, Math.min(MAX_HEIGHT * density, newHeight));
                                
                                params.width = newWidth;
                                params.height = newHeight;
                                
                                if (windowManager != null) {
                                    windowManager.updateViewLayout(floatingView, params);
                                }
                                return true;
                            }
                            break;
                            
                        case MotionEvent.ACTION_UP:
                            isResizing = false;
                            return true;
                    }
                    return false;
                }
            });
            
            // Drag handle - Sadece taşıma için
            dragHandle.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            // Resize modunda değilse taşımaya izin ver
                            if (isResizing) return false;
                            
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            
                            // Görsel feedback
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
                            // Görsel feedback geri al
                            dragHandle.setAlpha(1.0f);
                            return true;
                    }
                    return false;
                }
            });
            
            android.util.Log.i("FloatingQRService", "✅ Button listener'lar eklendi");
            
        } catch (Exception e) {
            android.util.Log.e("FloatingQRService", "❌ createFloatingView hatası: " + e.getMessage());
            e.printStackTrace();
            throw e; // Üst katmana fırlat
        }
        
        // WindowManager parametreleri
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        params = new WindowManager.LayoutParams(
            (int) (300 * getResources().getDisplayMetrics().density), // 300dp width
            (int) (400 * getResources().getDisplayMetrics().density), // 400dp height
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 50;
        params.y = 100;
        
        // Touch listener - sürüklenebilir
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private GestureDetector gestureDetector = new GestureDetector(
                FloatingQRService.this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }
                }
            );
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Resize modundayken move etme
                        if (isResizing) return false;
                        
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        // Resize modundayken move etme
                        if (isResizing) return false;
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        
                        if (windowManager != null && floatingView != null) {
                            windowManager.updateViewLayout(floatingView, params);
                        }
                        return true;
                }
                return false;
            }
        });
        
        // WindowManager'a ekle
        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (windowManager != null) {
                android.util.Log.i("FloatingQRService", "🪟 WindowManager'a view ekleniyor...");
                windowManager.addView(floatingView, params);
                android.util.Log.i("FloatingQRService", "✅ Floating view eklendi!");
            } else {
                android.util.Log.e("FloatingQRService", "❌ WindowManager null!");
                throw new RuntimeException("WindowManager null");
            }
        } catch (Exception e) {
            android.util.Log.e("FloatingQRService", "❌ WindowManager.addView hatası: " + e.getMessage());
            e.printStackTrace();
            throw e; // Üst katmana fırlat
        }
    }
    
    private void startCamera() {
        try {
            android.util.Log.i("FloatingQRService", "📸 startCamera başladı");
            
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
            
            cameraProviderFuture.addListener(() -> {
                try {
                    android.util.Log.i("FloatingQRService", "🔄 Camera provider alınıyor...");
                    cameraProvider = cameraProviderFuture.get();
                    android.util.Log.i("FloatingQRService", "✅ Camera provider alındı");
                    
                    bindCameraUseCases();
                    android.util.Log.i("FloatingQRService", "✅ Camera use cases bağlandı");
                    
                } catch (ExecutionException | InterruptedException e) {
                    android.util.Log.e("FloatingQRService", "❌ Kamera başlatma hatası: " + e.getMessage());
                    e.printStackTrace();
                    Toast.makeText(this, "Kamera başlatılamadı: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            }, ContextCompat.getMainExecutor(this));
            
        } catch (Exception e) {
            android.util.Log.e("FloatingQRService", "❌ startCamera genel hatası: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    private void bindCameraUseCases() {
        if (cameraProvider == null) return;
        
        // Eski bağlantıları kaldır
        cameraProvider.unbindAll();
        
        // Camera selector - arka kamera
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        
        // Preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        
        // Image analysis - QR tarama
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();
        
        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            @androidx.camera.core.ExperimentalGetImage
            android.media.Image mediaImage = imageProxy.getImage();
            
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees()
                );
                
                // Barcode scan
                barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            handleBarcodeDetected(barcode);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Hata - sessizce devam et
                    })
                    .addOnCompleteListener(task -> {
                        imageProxy.close();
                    });
            } else {
                imageProxy.close();
            }
        });
        
        // Camera bind
        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis
            );
        } catch (Exception e) {
            Toast.makeText(this, "Kamera bağlanamadı: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
        }
    }
    
    private void handleBarcodeDetected(Barcode barcode) {
        long currentTime = System.currentTimeMillis();
        
        // Cooldown kontrolü
        if (currentTime - lastScanTime < SCAN_COOLDOWN) {
            return;
        }
        
        lastScanTime = currentTime;
        
        String barcodeValue = barcode.getRawValue();
        if (barcodeValue != null && !barcodeValue.isEmpty()) {
            // 🔊 SES EFEKTI ÇAL
            playScanSound();
            
            // ⚡ FLAŞ EFEKTİ (hızlı aç-kapa)
            playFlashEffect();
            
            // Clipboard'a kopyala
            copyToClipboard(barcodeValue);
            
        final long eventTimestamp = System.currentTimeMillis();
        lastResultTimestamp = eventTimestamp;
        
        boolean accessibilityEnabled = QRAccessibilityService.isServiceEnabled();
        updateLastResultPreview(barcodeValue, eventTimestamp, accessibilityEnabled);
        
        if (accessibilityEnabled) {
            QRAccessibilityService.pasteQRCode(this, barcodeValue);
            evaluateAutoFillResult(barcodeValue, eventTimestamp, 0);
        } else {
            // Accessibility kapalı - sadece clipboard
            finalizeResultAndNotify(barcodeValue, eventTimestamp, false, false);
        }
        }
    }
    
    private void playScanSound() {
        try {
            if (scanSoundPlayer != null) {
                if (scanSoundPlayer.isPlaying()) {
                    scanSoundPlayer.seekTo(0); // Başa sar
                }
                scanSoundPlayer.start();
                android.util.Log.i("FloatingQRService", "🔊 Scan sesi çalıyor");
            }
        } catch (Exception e) {
            android.util.Log.e("FloatingQRService", "❌ Ses çalma hatası: " + e.getMessage());
        }
    }
    
    private void playFlashEffect() {
        if (camera == null || !camera.getCameraInfo().hasFlashUnit()) {
            android.util.Log.w("FloatingQRService", "⚠️ Flaş yok");
            return;
        }
        
        try {
            // Flaşı aç
            camera.getCameraControl().enableTorch(true);
            android.util.Log.i("FloatingQRService", "⚡ Flaş açıldı");
            
            // 150ms sonra kapat
            mainHandler.postDelayed(() -> {
                try {
                    if (camera != null && !flashOn) { // Kullanıcı manuel açmadıysa kapat
                        camera.getCameraControl().enableTorch(false);
                        android.util.Log.i("FloatingQRService", "⚡ Flaş kapatıldı");
                    }
                } catch (Exception e) {
                    android.util.Log.e("FloatingQRService", "❌ Flaş kapatma hatası: " + e.getMessage());
                }
            }, 150); // 150ms = hızlı flaş efekti
            
        } catch (Exception e) {
            android.util.Log.e("FloatingQRService", "❌ Flaş efekti hatası: " + e.getMessage());
        }
    }
    
    private void updateLastResultPreview(String qrCode, long timestamp, boolean accessibilityEnabled) {
        final String shortened = shortenMultiline(qrCode, 160);
        final String statusText;
        final int statusColor;
        String timePart = formatTime(timestamp);
        
        if (accessibilityEnabled) {
            statusText = "🧠 Otomatik doldurma deneniyor...";
            statusColor = 0xFF60A5FA;
        } else {
            statusText = "📋 Panoya kopyalandı (Servis kapalı)";
            statusColor = 0xFFFBBF24;
        }
        
        mainHandler.post(() -> {
            if (timestamp != lastResultTimestamp) return;
            if (lastResultText != null) {
                lastResultText.setText(shortened);
            }
            if (lastResultMeta != null) {
                lastResultMeta.setText("⏰ " + timePart + "  •  " + statusText);
                lastResultMeta.setTextColor(statusColor);
            }
        });
    }
    
    private void finalizeResultAndNotify(String qrCode, long timestamp, boolean autoFillAttempted, boolean autoFillSuccess) {
        updateLastResultFinalUI(qrCode, timestamp, autoFillAttempted, autoFillSuccess);
        showScanSuccessToast(qrCode, autoFillAttempted, autoFillSuccess);
        FloatingQRPlugin.notifyQRScanned(qrCode, timestamp, autoFillAttempted, autoFillSuccess);
    }
    
    private void updateLastResultFinalUI(String qrCode, long timestamp, boolean autoFillAttempted, boolean autoFillSuccess) {
        final String shortened = shortenMultiline(qrCode, 180);
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
                lastResultMeta.setText("⏰ " + timePart + "  •  " + statusText);
                lastResultMeta.setTextColor(statusColor);
            }
        });
    }
    
    private void evaluateAutoFillResult(String qrCode, long timestamp, int attempt) {
        if (timestamp != lastResultTimestamp) {
            QRAccessibilityService.clearWriteStatus(timestamp);
            return;
        }
        
        QRAccessibilityService.WriteStatus status = QRAccessibilityService.peekWriteStatus(timestamp);
        
        if (!status.attempted) {
            if (attempt < 6) {
                mainHandler.postDelayed(() -> evaluateAutoFillResult(qrCode, timestamp, attempt + 1), 150L * (attempt + 1));
            } else {
                finalizeResultAndNotify(qrCode, timestamp, false, false);
            }
            return;
        }
        
        if (!status.completed) {
            if (attempt < 6) {
                mainHandler.postDelayed(() -> evaluateAutoFillResult(qrCode, timestamp, attempt + 1), 150L * (attempt + 1));
            } else {
                QRAccessibilityService.clearWriteStatus(timestamp);
                finalizeResultAndNotify(qrCode, timestamp, true, false);
            }
            return;
        }
        
        QRAccessibilityService.clearWriteStatus(timestamp);
        finalizeResultAndNotify(qrCode, timestamp, true, status.success);
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
    
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("QR Code", text);
            clipboard.setPrimaryClip(clip);
        }
    }
    
    private void showScanSuccessToast(String qrCode, boolean autoFillAttempted, boolean autoFillSuccess) {
        String displayText = qrCode.length() > 50 
            ? qrCode.substring(0, 50) + "..." 
            : qrCode;
        
        String message;
        if (!autoFillAttempted) {
            message = "✅ QR Okundu! Panoya kopyalandı:\n" + displayText;
        } else if (autoFillSuccess) {
            message = "✅ QR Okundu! Otomatik dolduruldu:\n" + displayText;
        } else {
            message = "⚠️ QR Okundu ancak otomatik yazılamadı.\nPanoya kopyalandı:\n" + displayText;
        }
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    private void toggleFlash() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            flashOn = !flashOn;
            camera.getCameraControl().enableTorch(flashOn);
            
            // Flash button icon güncelle (Android 5.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                flashButton.setImageResource(
                    flashOn ? android.R.drawable.ic_lock_idle_lock : android.R.drawable.ic_menu_camera
                );
            }
        } else {
            Toast.makeText(this, "Cihazınızda flaş bulunmuyor", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
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
        
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        
        // Floating view kaldır
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }
}

