package com.qrmaster.app.keyboard;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import androidx.camera.view.PreviewView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.Lifecycle;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * KLAVYE İÇİ KAMERA - TextureView kullanarak
 */
public class KeyboardCameraView extends FrameLayout implements LifecycleOwner {
    
    private static final String TAG = "KeyboardCameraView";
    
    private final LifecycleRegistry lifecycleRegistry;
    private PreviewView previewView;
    private TextView statusText;
    private Button closeButton;
    private Camera camera;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;
    private ScanCallback callback;
    private boolean isScanning = false;
    private long lastScanTime = 0;
    private String lastScannedCode = "";
    
    private MediaPlayer beepPlayer;
    private Handler mainHandler;
    
    public interface ScanCallback {
        void onScanned(String barcode);
        void onClose();
    }
    
    public KeyboardCameraView(Context context, ScanCallback callback) {
        super(context);
        this.callback = callback;
        this.lifecycleRegistry = new LifecycleRegistry(this);
        this.lifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
        this.mainHandler = new Handler(Looper.getMainLooper());
        initBeepSound(context);
        init(context);
    }
    
    private void init(Context context) {
        setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            dpToPx(280) // 280dp yükseklik - kompakt
        ));
        setBackgroundColor(0xFF000000); // Pure black
        
        // PreviewView (kamera preview için)
        previewView = new PreviewView(context);
        LayoutParams previewParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        );
        previewView.setLayoutParams(previewParams);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        addView(previewView);
        
        // Durum metni (üstte) - Modern gradient overlay
        statusText = new TextView(context);
        statusText.setText("📷 QR veya Barkod okutun");
        statusText.setTextColor(0xFFFFFFFF);
        statusText.setTextSize(15);
        statusText.setBackgroundColor(0xCC000000); // Daha koyu overlay
        statusText.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        statusText.setGravity(Gravity.CENTER);
        statusText.setTypeface(null, android.graphics.Typeface.BOLD);
        LayoutParams statusParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        statusParams.gravity = Gravity.TOP;
        statusText.setLayoutParams(statusParams);
        addView(statusText);
        
        // Kapat butonu (altta, modern yuvarlak)
        closeButton = new Button(context);
        closeButton.setText("✕");
        closeButton.setTextColor(0xFFFFFFFF);
        closeButton.setBackgroundColor(0xFFFF3B30);
        closeButton.setTextSize(20);
        closeButton.setTypeface(null, android.graphics.Typeface.BOLD);
        
        // Yuvarlak buton
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        shape.setColor(0xFFFF3B30);
        shape.setStroke(dpToPx(2), 0xFFFFFFFF);
        closeButton.setBackground(shape);
        
        LayoutParams closeParams = new LayoutParams(
            dpToPx(50),
            dpToPx(50)
        );
        closeParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        closeParams.bottomMargin = dpToPx(16);
        closeButton.setLayoutParams(closeParams);
        closeButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.onClose();
            }
        });
        addView(closeButton);
        
        // ML Kit barcode scanner
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_ITF
            )
            .build();
        barcodeScanner = BarcodeScanning.getClient(options);
        
        cameraExecutor = Executors.newSingleThreadExecutor();
        
        Log.d(TAG, "✅ KeyboardCameraView oluşturuldu");
    }
    
    public void startCamera() {
        Log.d(TAG, "📷 Kamera başlatılıyor...");
        lifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
        lifecycleRegistry.setCurrentState(Lifecycle.State.RESUMED);
        
        if (!hasCameraPermission()) {
            statusText.setText("❌ Kamera izni gerekli");
            Log.e(TAG, "Camera permission missing");
            return;
        }

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(getContext());
        
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                previewView.post(() -> bindCamera(cameraProvider));
            } catch (Exception e) {
                Log.e(TAG, "❌ Kamera başlatma hatası", e);
                post(() -> statusText.setText("❌ Kamera başlatılamadı: " + e.getMessage()));
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }
    
    private void bindCamera(ProcessCameraProvider cameraProvider) {
        Log.d(TAG, "📷 Kamera bind ediliyor...");
        
        // Preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        
        // Image analysis (barcode scanning için)
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();
        
        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            if (!isScanning) {
                imageProxy.close();
                return;
            }
            
            @androidx.camera.core.ExperimentalGetImage
            android.media.Image mediaImage = imageProxy.getImage();
            
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees()
                );
                
                barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String value = barcode.getRawValue();
                            if (value != null && !value.isEmpty()) {
                                long now = System.currentTimeMillis();
                                // Aynı kodu 1 saniye içinde tekrar okutma (ama kapat YAPMA!)
                                if (!value.equals(lastScannedCode) || (now - lastScanTime) > 1000) {
                                    lastScannedCode = value;
                                    lastScanTime = now;
                                    
                                    Log.d(TAG, "✅ Barkod tarandı: " + value);
                                    
                                    // SES + FLAŞ EFEKT!
                                    playBeep();
                                    flashEffect();
                                    
                                    post(() -> {
                                        statusText.setText("✅ Tarandı: " + value);
                                        if (callback != null) {
                                            callback.onScanned(value);
                                        }
                                    });
                                    // KAMERAYI KAPATMA - sürekli tarama devam etsin!
                                }
                                break;
                            }
                        }
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
            } else {
                imageProxy.close();
            }
        });
        
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        
        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(
                this, // LifecycleOwner
                cameraSelector,
                preview,
                imageAnalysis
            );
            
            isScanning = true;
            Log.d(TAG, "✅ Kamera başarıyla başlatıldı");
            post(() -> statusText.setText("📷 QR Kod veya Barkod okutun"));
        } catch (Exception e) {
            Log.e(TAG, "❌ Kamera bind hatası", e);
            post(() -> statusText.setText("❌ Kamera hatası: " + e.getMessage()));
        }
    }
    
    public void stopCamera() {
        Log.d(TAG, "🔴 Kamera durduruluyor...");
        isScanning = false;
        lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
    }
    
    public void release() {
        Log.d(TAG, "🗑️ Kaynaklar temizleniyor...");
        stopCamera();
        
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        
        if (beepPlayer != null) {
            beepPlayer.release();
            beepPlayer = null;
        }
    }
    
    private void initBeepSound(Context context) {
        try {
            beepPlayer = new MediaPlayer();
            AssetFileDescriptor afd = context.getAssets().openFd("scan_beep.mp3");
            beepPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            beepPlayer.prepare();
            Log.d(TAG, "✅ Ses efekti yüklendi");
        } catch (IOException e) {
            Log.e(TAG, "❌ Ses efekti yüklenemedi: " + e.getMessage());
        }
    }
    
    private void playBeep() {
        if (beepPlayer != null) {
            try {
                if (beepPlayer.isPlaying()) {
                    beepPlayer.seekTo(0);
                } else {
                    beepPlayer.start();
                }
            } catch (Exception e) {
                Log.e(TAG, "Ses çalma hatası: " + e.getMessage());
            }
        }
    }
    
    private void flashEffect() {
        if (camera != null) {
            mainHandler.post(() -> {
                try {
                    camera.getCameraControl().enableTorch(true);
                    mainHandler.postDelayed(() -> {
                        try {
                            camera.getCameraControl().enableTorch(false);
                        } catch (Exception e) {
                            Log.e(TAG, "Flaş kapatma hatası: " + e.getMessage());
                        }
                    }, 150); // 150ms flaş
                } catch (Exception e) {
                    Log.e(TAG, "Flaş açma hatası: " + e.getMessage());
                }
            });
        }
    }
    
    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }
    
    private int dpToPx(int dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED;
    }
}

