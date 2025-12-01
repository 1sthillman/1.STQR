package com.qrmaster.app.keyboard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Inline sesli yazma - SpeechRecognizer kullanarak
 */
public class VoiceInputView extends LinearLayout {
    
    private TextView statusText;
    private Button startButton;
    private Button stopButton;
    private SpeechRecognizer speechRecognizer;
    private VoiceCallback callback;
    private boolean isListening = false;
    
    public interface VoiceCallback {
        void onResult(String text);
        void onClose();
    }
    
    public VoiceInputView(Context context, VoiceCallback callback) {
        super(context);
        this.callback = callback;
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            300
        ));
        setBackgroundColor(0xFF1C1C1E);
        setPadding(20, 20, 20, 20);
        
        // Durum metni
        statusText = new TextView(context);
        statusText.setText("🎤 Konuşmaya hazır");
        statusText.setTextColor(0xFFFFFFFF);
        statusText.setTextSize(18);
        statusText.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        statusText.setPadding(0, 20, 0, 20);
        addView(statusText);
        
        // Butonlar
        LinearLayout buttonLayout = new LinearLayout(context);
        buttonLayout.setOrientation(HORIZONTAL);
        buttonLayout.setGravity(android.view.Gravity.CENTER);
        
        startButton = new Button(context);
        startButton.setText("🎤 Başlat");
        startButton.setTextColor(0xFFFFFFFF);
        startButton.setBackgroundColor(0xFF34C759);
        startButton.setPadding(40, 20, 40, 20);
        startButton.setOnClickListener(v -> startListening());
        
        stopButton = new Button(context);
        stopButton.setText("⏹ Durdur");
        stopButton.setTextColor(0xFFFFFFFF);
        stopButton.setBackgroundColor(0xFFFF3B30);
        stopButton.setPadding(40, 20, 40, 20);
        stopButton.setEnabled(false);
        stopButton.setOnClickListener(v -> stopListening());
        
        buttonLayout.addView(startButton);
        buttonLayout.addView(stopButton);
        addView(buttonLayout);
        
        // Kapat butonu
        Button closeBtn = new Button(context);
        closeBtn.setText("✕ Kapat");
        closeBtn.setTextColor(0xFFFFFFFF);
        closeBtn.setBackgroundColor(0xFF007AFF);
        closeBtn.setOnClickListener(v -> {
            stopListening();
            if (callback != null) callback.onClose();
        });
        addView(closeBtn);
        
        // SpeechRecognizer hazırla
        setupSpeechRecognizer(context);
    }
    
    private void setupSpeechRecognizer(Context context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    post(() -> statusText.setText("🎤 Dinliyorum..."));
                }
                
                @Override
                public void onBeginningOfSpeech() {
                    post(() -> statusText.setText("🗣️ Konuşuyorsunuz..."));
                }
                
                @Override
                public void onRmsChanged(float rmsdB) {
                    // Ses seviyesi değişimi
                }
                
                @Override
                public void onBufferReceived(byte[] buffer) {}
                
                @Override
                public void onEndOfSpeech() {
                    post(() -> statusText.setText("✅ İşleniyor..."));
                }
                
                @Override
                public void onError(int error) {
                    String errorMsg = getErrorText(error);
                    post(() -> {
                        statusText.setText("❌ " + errorMsg);
                        isListening = false;
                        startButton.setEnabled(true);
                        stopButton.setEnabled(false);
                    });
                }
                
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    );
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        post(() -> {
                            statusText.setText("✅ " + text);
                            if (callback != null) {
                                callback.onResult(text);
                            }
                            isListening = false;
                            startButton.setEnabled(true);
                            stopButton.setEnabled(false);
                        });
                    } else {
                        post(() -> {
                            isListening = false;
                            startButton.setEnabled(true);
                            stopButton.setEnabled(false);
                        });
                    }
                }
                
                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    );
                    if (matches != null && !matches.isEmpty()) {
                        String partial = matches.get(0);
                        post(() -> statusText.setText("💬 " + partial));
                    }
                }
                
                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        }
    }
    
    private void startListening() {
        if (speechRecognizer == null) {
            statusText.setText("❌ Sesli yazma desteklenmiyor");
            return;
        }
        
        if (!isListening) {
            try {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getContext().getPackageName());
                
                speechRecognizer.startListening(intent);
                isListening = true;
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                statusText.setText("🎤 Dinlemeye hazırlanıyor...");
            } catch (Exception e) {
                statusText.setText("❌ Başlatılamadı: " + e.getMessage());
                isListening = false;
            }
        }
    }
    
    private void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusText.setText("🎤 Konuşmaya hazır");
        }
    }
    
    public void release() {
        stopListening();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
    
    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Ses kaydı hatası";
            case SpeechRecognizer.ERROR_CLIENT:
                return "İstemci hatası";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Mikrofon izni gerekli";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Ağ hatası";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Ağ zaman aşımı";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "Eşleşme bulunamadı";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Tanıyıcı meşgul";
            case SpeechRecognizer.ERROR_SERVER:
                return "Sunucu hatası";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Konuşma zaman aşımı";
            default:
                return "Bilinmeyen hata";
        }
    }
}

