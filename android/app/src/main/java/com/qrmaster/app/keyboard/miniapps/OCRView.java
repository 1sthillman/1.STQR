package com.qrmaster.app.keyboard.miniapps;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.qrmaster.app.R;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OCR - Görselden Metin Çıkarma (ML Kit Text Recognition)
 */
public class OCRView extends LinearLayout {
    private static final String TAG = "OCRView";
    private static final int MAX_IMAGE_DIMENSION = 1024;
    
    private TextView resultText;
    private ProgressBar progressBar;
    private TextView statusText;
    private Button selectImageBtn;
    private OCRLiveCameraView liveCameraView;
    
    private final TextRecognizer recognizer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OCRCallback {
        void onResult(String text);
        void onClose();
        void onSelectImage(); // Trigger image picker from activity
        void onSelectCamera(); // Trigger camera capture
    }

    private final OCRCallback callback;

    public OCRView(Context context, OCRCallback callback) {
        super(context);
        this.callback = callback;
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setBackgroundColor(0xFF1C1C1E);
        setPadding(dp(12), dp(12), dp(12), dp(12));

        // Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(12));
        
        TextView title = new TextView(context);
        title.setText("📷 OCR - Metin Tanıma");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);

        ImageButton closeBtn = new ImageButton(context);
        closeBtn.setBackground(ContextCompat.getDrawable(context, R.drawable.toolbar_button_bg));
        closeBtn.setImageResource(R.drawable.ic_close);
        closeBtn.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        closeBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> { if (callback != null) callback.onClose(); });
        header.addView(closeBtn);
        addView(header);

        // Info
        TextView infoText = new TextView(context);
        infoText.setText("Canlı kamera ile gördüğünü metne dönüştürür.");
        infoText.setTextColor(0xFF8E8E93);
        infoText.setTextSize(13);
        LayoutParams infoParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        infoParams.bottomMargin = dp(12);
        addView(infoText, infoParams);

        // Live camera (IME içinde, alt bant)
        liveCameraView = new OCRLiveCameraView(context, text -> {});
        addView(liveCameraView);

        // Tek “Yakala” butonu (yuvarlak)
        Button captureBtn = createButton(context, "●", 0xFF34C759);
        android.graphics.drawable.GradientDrawable capBg = new android.graphics.drawable.GradientDrawable();
        capBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        capBg.setColor(0xFF34C759);
        captureBtn.setBackground(capBg);
        captureBtn.setTextColor(0xFFFFFFFF);
        captureBtn.setTextSize(18);
        LayoutParams capParams = new LayoutParams(dp(64), dp(64));
        capParams.gravity = Gravity.CENTER_HORIZONTAL;
        capParams.bottomMargin = dp(12);
        captureBtn.setLayoutParams(capParams);
        captureBtn.setOnClickListener(v -> {
            String text = liveCameraView != null ? liveCameraView.getLastText() : "";
            if (text != null && !text.trim().isEmpty()) {
                statusText.setText("✓ Yakalandı");
                resultText.setText(text);
                // Metni yazalım, klavye AÇIK KALSIN
                if (callback != null) {
                    callback.onResult(text);
                    // onClose() ÇAĞIRMAYIN - Klavye açık kalmalı
                }
            } else {
                Toast.makeText(getContext(), "Metin algılanamadı", Toast.LENGTH_SHORT).show();
            }
        });
        addView(captureBtn);

        // Progress
        progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(GONE);
        LayoutParams pbParams = new LayoutParams(dp(40), dp(40));
        pbParams.gravity = Gravity.CENTER;
        pbParams.topMargin = dp(8);
        addView(progressBar, pbParams);

        // Status
        statusText = new TextView(context);
        statusText.setTextColor(0xFF8E8E93);
        statusText.setTextSize(12);
        statusText.setGravity(Gravity.CENTER);
        statusText.setText("Görsel bekleniyor...");
        LayoutParams statusParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(4);
        statusParams.bottomMargin = dp(12);
        addView(statusText, statusParams);

        // Result
        ScrollView scrollView = new ScrollView(context);
        resultText = new TextView(context);
        resultText.setText("");
        resultText.setTextColor(0xFFFFFFFF);
        resultText.setTextSize(14);
        resultText.setBackgroundColor(0xFF2C2C2E);
        resultText.setPadding(dp(12), dp(12), dp(12), dp(12));
        android.graphics.drawable.GradientDrawable resultBg = new android.graphics.drawable.GradientDrawable();
        resultBg.setCornerRadius(dp(8));
        resultBg.setColor(0xFF2C2C2E);
        resultText.setBackground(resultBg);
        scrollView.addView(resultText);
        LayoutParams scrollParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.bottomMargin = dp(12);
        addView(scrollView, scrollParams);

        // Ek yapıştır butonu kaldırıldı; yakala anında yazar
    }

    private Button createButton(Context context, String text, int color) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(14);
        btn.setTypeface(null, Typeface.BOLD);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(color);
        btn.setBackground(bg);
        return btn;
    }

    private void extractFromClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clipData = clipboard.getPrimaryClip();
                if (clipData != null && clipData.getItemCount() > 0) {
                    ClipData.Item item = clipData.getItemAt(0);
                    Uri uri = item.getUri();
                    if (uri != null) {
                        processImage(uri);
                        return;
                    }
                }
            }
            Toast.makeText(getContext(), "Panoda görsel bulunamadı", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Clipboard error", e);
            Toast.makeText(getContext(), "Pano hatası", Toast.LENGTH_SHORT).show();
        }
    }

    public void processImage(Uri imageUri) {
        if (imageUri == null) return;
        
        progressBar.setVisibility(VISIBLE);
        statusText.setText("Görsel işleniyor...");
        resultText.setText("");
        
        executor.execute(() -> {
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
                if (inputStream == null) throw new FileNotFoundException();
                
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                
                if (bitmap == null) throw new Exception("Bitmap decode failed");
                
                // Resize if too large
                bitmap = resizeBitmap(bitmap);
                
                InputImage image = InputImage.fromBitmap(bitmap, 0);
                
                recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String extractedText = visionText.getText();
                        mainHandler.post(() -> {
                            progressBar.setVisibility(GONE);
                            if (extractedText.isEmpty()) {
                                statusText.setText("❌ Metin bulunamadı");
                                resultText.setText("");
                            } else {
                                statusText.setText("✓ Metin çıkarıldı (" + extractedText.length() + " karakter)");
                                resultText.setText(extractedText);
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "OCR failed", e);
                        mainHandler.post(() -> {
                            progressBar.setVisibility(GONE);
                            statusText.setText("❌ OCR hatası");
                            resultText.setText("");
                        });
                    });
                
            } catch (Exception e) {
                Log.e(TAG, "Image processing failed", e);
                mainHandler.post(() -> {
                    progressBar.setVisibility(GONE);
                    statusText.setText("❌ Görsel işlenemedi");
                    Toast.makeText(getContext(), "Görsel yüklenemedi", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private Bitmap resizeBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            return bitmap;
        }
        
        float scale = Math.min(
            (float) MAX_IMAGE_DIMENSION / width,
            (float) MAX_IMAGE_DIMENSION / height
        );
        
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    public void release() {
        executor.shutdown();
        recognizer.close();
        if (liveCameraView != null) {
            liveCameraView.release();
        }
    }
}


