package com.qrmaster.app.keyboard;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Inline kamera görünümü - klavyenin içinde QR tarama
 */
public class InlineCameraView extends FrameLayout {
    
    private static final String TAG = "InlineCameraView";
    
    private final Context context;
    private SurfaceView surfaceView;
    private Camera camera;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;
    private ScanCallback callback;
    private boolean isScanning = false;
    
    public interface ScanCallback {
        void onScanned(String barcode);
    }
    
    public InlineCameraView(Context context) {
        super(context);
        this.context = context;
        init();
    }
    
    private void init() {
        // SurfaceView oluştur
        surfaceView = new SurfaceView(context);
        surfaceView.setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            400 // 400dp yükseklik
        ));
        addView(surfaceView);
        
        // Barcode scanner hazırla
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39
            )
            .build();
        barcodeScanner = BarcodeScanning.getClient(options);
        
        cameraExecutor = Executors.newSingleThreadExecutor();
    }
    
    public void startCamera(LifecycleOwner lifecycleOwner) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(context);
        
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider, lifecycleOwner);
            } catch (Exception e) {
                Log.e(TAG, "Kamera başlatma hatası", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }
    
    private void bindPreview(ProcessCameraProvider cameraProvider, LifecycleOwner lifecycleOwner) {
        Preview preview = new Preview.Builder().build();
        // SurfaceView için özel preview surface provider
        preview.setSurfaceProvider(request -> {
            android.view.Surface surface = surfaceView.getHolder().getSurface();
            if (surface != null && surface.isValid()) {
                request.provideSurface(surface, ContextCompat.getMainExecutor(context), result -> {
                    Log.d(TAG, "Surface result: " + result);
                });
            }
        });
        
        // Image analysis için
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();
        
        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            if (isScanning && callback != null) {
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
                                    isScanning = false;
                                    callback.onScanned(value);
                                    break;
                                }
                            }
                        })
                        .addOnCompleteListener(task -> imageProxy.close());
                } else {
                    imageProxy.close();
                }
            } else {
                imageProxy.close();
            }
        });
        
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        
        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            );
        } catch (Exception e) {
            Log.e(TAG, "Kamera bind hatası", e);
        }
    }
    
    public void startScanning(ScanCallback callback) {
        this.callback = callback;
        this.isScanning = true;
    }
    
    public void stopScanning() {
        this.isScanning = false;
        this.callback = null;
    }
    
    public void release() {
        stopScanning();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
    }
}

