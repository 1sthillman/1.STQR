package com.qrmaster.app.keyboard;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Gravity;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.qrmaster.app.R;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Crash-safe, minimal ses yazma görünümü
 */
public class KeyboardVoiceView extends LinearLayout {
    private static final String TAG = "KeyboardVoiceView";
    
    private TextView statusText;
    private ImageView micCircle;
    private ValueAnimator pulseAnimator;
    
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private Handler mainHandler;
    
    private VoiceCallback callback;
    
    public interface VoiceCallback {
        void onResult(String text);
        void onClose();
    }
    
    public KeyboardVoiceView(Context context, VoiceCallback callback) {
        super(context);
        this.callback = callback;
        this.mainHandler = new Handler(Looper.getMainLooper());
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            dpToPx(160)
        ));
        setBackgroundColor(0xFF1C1C1E);
        setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(context);
        title.setText("Sesle yazma");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);

        ImageButton close = new ImageButton(context);
        close.setBackground(ContextCompat.getDrawable(context, R.drawable.toolbar_button_bg));
        close.setImageResource(R.drawable.ic_close);
        close.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        close.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
        close.setLayoutParams(closeParams);
        close.setOnClickListener(v -> {
            stopListening();
            if (callback != null) callback.onClose();
        });
        header.addView(close);
        addView(header);

        LinearLayout center = new LinearLayout(context);
        center.setOrientation(VERTICAL);
        center.setGravity(Gravity.CENTER);
        LayoutParams centerParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        centerParams.topMargin = dpToPx(12);
        centerParams.bottomMargin = dpToPx(12);
        addView(center, centerParams);

        micCircle = new ImageView(context);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0xFF2C2C2E);
        circle.setStroke(dpToPx(2), 0xFF34C759);
        micCircle.setBackground(circle);
        micCircle.setImageResource(R.drawable.ic_mic);
        micCircle.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int size = dpToPx(72);
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(size, size);
        center.addView(micCircle, circleParams);

        statusText = new TextView(context);
        statusText.setText("Dinliyorum...");
        statusText.setTextColor(0xFFFFFFFF);
        statusText.setTextSize(14);
        statusText.setTypeface(null, android.graphics.Typeface.BOLD);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dpToPx(12), 0, dpToPx(4));
        center.addView(statusText);

        TextView hint = new TextView(context);
        hint.setText("Konuşmanız bittiğinde otomatik kapanır");
        hint.setTextColor(0xFF8E8E93);
        hint.setTextSize(11);
        hint.setGravity(Gravity.CENTER);
        center.addView(hint);

        Log.d(TAG, "✅ KeyboardVoiceView (Gboard style) oluşturuldu");
    }
    
    private void toggleListening() {
        if (isListening) {
            stopListening();
        } else {
            startListening();
        }
    }

    public void startVoice() {
        if (!isListening) {
            startListening();
        }
    }

    private void startListening() {
        Context context = getContext();
        
        // İzin kontrolü
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            updateStatus("❌ Mikrofon izni gerekli");
            Toast.makeText(context, "Mikrofon izni verin", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Önceki recognizer'ı temizle
        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception e) {
                // ignore
            }
            speechRecognizer = null;
        }
        
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                updateStatus("❌ Bu cihazda desteklenmiyor");
                return;
            }
            
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            if (speechRecognizer == null) {
                updateStatus("❌ Başlatılamadı");
                return;
            }
            
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    mainHandler.post(() -> {
                        isListening = true;
                        updateStatus("Dinliyorum...");
                        updateMicAnimation(true);
                    });
                }
                
                @Override
                public void onBeginningOfSpeech() {
                    mainHandler.post(() -> updateStatus("Konuşuyorsunuz..."));
                }
                
                @Override
                public void onRmsChanged(float rmsdB) {}
                
                @Override
                public void onBufferReceived(byte[] buffer) {}
                
                @Override
                public void onEndOfSpeech() {
                    mainHandler.post(() -> updateStatus("İşleniyor..."));
                }
                
                @Override
                public void onError(int error) {
                    mainHandler.post(() -> {
                        String msg = "Hata: " + error;
                        if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                            msg = "Ses algılanamadı";
                        } else if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            msg = "Zaman aşımı";
                        }
                        updateStatus(msg);
                        stopListening();
                    });
                }
                
                @Override
                public void onResults(Bundle results) {
                    mainHandler.post(() -> {
                        ArrayList<String> matches = results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        );
                        
                        if (matches != null && !matches.isEmpty()) {
                            String text = matches.get(0);
                            updateStatus(text);
                            
                            if (callback != null) {
                                callback.onResult(text);
                            }
                        }
                        
                        stopListening();
                        // Otomatik kapat
                        mainHandler.postDelayed(() -> {
                            if (callback != null) {
                                callback.onClose();
                            }
                        }, 500);
                    });
                }
                
                @Override
                public void onPartialResults(Bundle partialResults) {
                    mainHandler.post(() -> {
                        ArrayList<String> matches = partialResults.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        );
                        if (matches != null && !matches.isEmpty()) {
                            updateStatus(matches.get(0));
                        }
                    });
                }
                
                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
            
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            
            speechRecognizer.startListening(intent);
            Log.d(TAG, "✅ Dinleme başladı");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Dinleme hatası: " + e.getMessage());
            updateStatus("❌ Hata: " + e.getMessage());
            stopListening();
        }
    }
    
    private void stopListening() {
        isListening = false;
        updateMicAnimation(false);
        
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
                speechRecognizer.destroy();
            } catch (Exception e) {
                Log.e(TAG, "Durdurma hatası: " + e.getMessage());
            }
            speechRecognizer = null;
        }
        
        updateStatus("Hazır");
    }
    
    private void updateStatus(String text) {
        mainHandler.post(() -> {
            if (statusText != null) {
                statusText.setText(text);
            }
        });
    }
    
    private void updateMicAnimation(boolean listening) {
        mainHandler.post(() -> {
            if (micCircle == null) return;
            if (listening) {
                if (pulseAnimator == null) {
                    pulseAnimator = ValueAnimator.ofFloat(1f, 1.15f);
                    pulseAnimator.setDuration(450);
                    pulseAnimator.setInterpolator(new LinearInterpolator());
                    pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
                    pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
                    pulseAnimator.addUpdateListener(anim -> {
                        float scale = (float) anim.getAnimatedValue();
                        micCircle.setScaleX(scale);
                        micCircle.setScaleY(scale);
                    });
                }
                if (!pulseAnimator.isStarted()) {
                    pulseAnimator.start();
                }
            } else {
                if (pulseAnimator != null && pulseAnimator.isRunning()) {
                    pulseAnimator.cancel();
                }
                micCircle.setScaleX(1f);
                micCircle.setScaleY(1f);
            }
        });
    }
 
    public void release() {
        Log.d(TAG, "🗑️ KeyboardVoiceView temizleniyor");
        stopListening();
    }
 
    private int dpToPx(int dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }
}
