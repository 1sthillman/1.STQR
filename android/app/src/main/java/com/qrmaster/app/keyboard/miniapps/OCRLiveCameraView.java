package com.qrmaster.app.keyboard.miniapps;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OCR canlı kamera görünümü - ML Kit Text Recognition (Latin)
 */
public class OCRLiveCameraView extends FrameLayout implements LifecycleOwner {
    private static final String TAG = "OCRLiveCameraView";
    private final LifecycleRegistry lifecycleRegistry;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final com.google.mlkit.vision.text.TextRecognizer recognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    private PreviewView previewView;
    private TextView overlayText;
    private long lastAnalyze = 0;
    private Callback callback;
    private volatile String lastText = "";

    public interface Callback {
        void onTextRecognized(String text);
    }

    public OCRLiveCameraView(Context context, Callback callback) {
        super(context);
        this.callback = callback;
        this.lifecycleRegistry = new LifecycleRegistry(this);
        this.lifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
        init(context);
    }

    private void init(Context context) {
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp(260)));
        previewView = new PreviewView(context);
        LayoutParams pv = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        previewView.setLayoutParams(pv);
        addView(previewView);

        overlayText = new TextView(context);
        overlayText.setText("");
        overlayText.setTextColor(0xFFFFFFFF);
        overlayText.setTextSize(12);
        overlayText.setBackgroundColor(0x66000000);
        overlayText.setPadding(dp(8), dp(8), dp(8), dp(8));
        LayoutParams ov = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        ov.gravity = Gravity.BOTTOM;
        overlayText.setLayoutParams(ov);
        addView(overlayText);

        startCamera();
    }

    private void startCamera() {
        try {
            final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                    ProcessCameraProvider.getInstance(getContext());
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindUseCases(cameraProvider);
                } catch (Exception e) {
                    Log.e(TAG, "Camera provider get failed", e);
                }
            }, ContextCompat.getMainExecutor(getContext()));
        } catch (Exception e) {
            Log.e(TAG, "startCamera failed", e);
        }
    }

    private void bindUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        try {
            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            ImageAnalysis analysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

            analysis.setAnalyzer(cameraExecutor, this::analyzeImage);

            CameraSelector selector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();

            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, selector, preview, analysis);
            lifecycleRegistry.setCurrentState(Lifecycle.State.RESUMED);
        } catch (Exception e) {
            Log.e(TAG, "bindUseCases failed", e);
        }
    }

    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        try {
            long now = System.currentTimeMillis();
            if (now - lastAnalyze < 500) {
                imageProxy.close();
                return;
            }
            lastAnalyze = now;
            ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
            if (planes == null || planes.length == 0) {
                imageProxy.close();
                return;
            }
            InputImage img = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
            recognizer.process(img)
                    .addOnSuccessListener(result -> {
                        String text = result.getText();
                        lastText = text != null ? text : "";
                        overlayText.setText(lastText.length() > 120 ? lastText.substring(0, 120) + "…" : lastText);
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> {
                        imageProxy.close();
                    });
        } catch (Exception e) {
            Log.e(TAG, "analyzeImage error", e);
            imageProxy.close();
        }
    }

    public void release() {
        try {
            lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
        } catch (Exception ignored) {}
        try {
            recognizer.close();
        } catch (Exception ignored) {}
        cameraExecutor.shutdown();
    }

    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    public String getLastText() {
        return lastText;
    }
}


