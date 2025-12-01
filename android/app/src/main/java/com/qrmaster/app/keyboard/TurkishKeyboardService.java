package com.qrmaster.app.keyboard;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.core.content.FileProvider;

import com.qrmaster.app.keyboard.ModernThemeDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * TAM KAPSAMLI Türkçe Q Klavye
 * ✅ Inline kamera (QR tarama)
 * ✅ Tam emoji picker (tüm kategoriler)
 * ✅ Inline sesli yazma
 * ✅ GIF desteği (yakında)
 * ✅ Çeviri (yakında)
 * ✅ Çıkartma (yakında)
 */
public class TurkishKeyboardService extends InputMethodService 
    implements KeyboardView.OnKeyboardActionListener {

    private static final String TAG = "TurkishKeyboardService";

    // Key codes
    private static final int KEYCODE_SWITCH_SYMBOLS = -100; // 123 butonu
    private static final int KEYCODE_SYMBOLS_SHIFT = -101; // =\< butonu
    private static final int KEYCODE_SWITCH_ALPHA = -102; // ABC butonu
    private static final int KEYCODE_QR_SCAN = -200;
    private static final int KEYCODE_EMOJI = -202;
    private static final int KEYCODE_GIF = -203;
    private static final int KEYCODE_VOICE = -204;
    private static final int KEYCODE_CLIPBOARD = -205;
    private static final int KEYCODE_TRANSLATE = -206;
    private static final int KEYCODE_SETTINGS = -207;

    // Views
    private FrameLayout featureContainer;
    private FrameLayout keyboardContainer;
    private KeyboardView keyboardView;
    private Keyboard mainKeyboard;
    private Keyboard symbolsKeyboard;
    private Keyboard symbolsShiftKeyboard;
    private KeyboardCameraView cameraView;
    private EmojiPickerView emojiView;
    private KeyboardVoiceView voiceView;
    private ClipboardHistoryView clipboardView;
    private GifPickerView gifView;
    private StickerPickerView stickerView;
    private TranslateView translateView;
    private QuickMenuView quickMenuView;
    private TextToolsView textToolsView;
    private CandidateStripView candidateStripView;
    private View rootInputView;
    
    // Mini Apps
    private com.qrmaster.app.keyboard.miniapps.MiniAppsHubView miniAppsHubView;
    private com.qrmaster.app.keyboard.miniapps.CalculatorView calculatorView;
    private com.qrmaster.app.keyboard.miniapps.CurrencyConverterView currencyView;
    private com.qrmaster.app.keyboard.miniapps.MiniCalendarView calendarView;
    
    // Smart Prediction
    private SmartPredictionManager smartPrediction;
    private com.qrmaster.app.keyboard.miniapps.OCRView ocrView;
    private com.qrmaster.app.keyboard.miniapps.MiniMapView mapView;
    private com.qrmaster.app.keyboard.miniapps.SharedTypingView sharedTypingView;
    private SuggestionManager suggestionManager;
    private SmartPhrasePredictor phrasePredictor;
    private LanguageManager languageManager;
    private com.qrmaster.app.keyboard.mouse.MouseManager mouseManager;
    private boolean isMouseKeyboardMode = false; // PC'ye yazma modu
    private boolean isCryptoWriteMode = false; // Crypto mesaj yazma modu
    private com.qrmaster.app.keyboard.views.CryptoView cryptoView;
    
    private boolean isNoteWriteMode = false; // Quick Note yazma modu
    private boolean isNoteTitle = false; // True = Başlık, False = İçerik
    private com.qrmaster.app.keyboard.views.QRDisplayView qrDisplayView;
    private final StringBuilder composingBuffer = new StringBuilder();
    private String pendingSearchQuery = null; // Hızlı arama için bekleyen sorgu
    private boolean isQuickSearchMode = false; // Hızlı arama modu (? yazıldı mı?)
    
    private boolean isSearchWriteMode = false; // Hızlı Arama input yazma modu
    private android.widget.EditText searchInputEditText = null; // Arama input'u referansı
    
    private final StringBuilder gifQueryBuffer = new StringBuilder();
    private final StringBuilder translateBuffer = new StringBuilder();
    
    // Inline voice recording
    private android.speech.SpeechRecognizer inlineSpeechRecognizer;
    private boolean isInlineVoiceRecording = false;
    private View currentVoiceButton;

    // State
    private ViewMode currentMode = ViewMode.KEYBOARD;
    private boolean caps = false;
    private boolean capsLock = false; // Tüm harfler büyük
    private long lastShiftTime = 0; // Çift tıklama için
    private final Locale turkishLocale = new Locale("tr", "TR");
    private final List<String> originalLabels = new ArrayList<>();
    
    // Preferences
    private SharedPreferences prefs;
    private boolean vibrateOnPress = true;
    private boolean soundOnPress = false;
    private String currentTheme = "default";
    private OneHandMode oneHandMode = OneHandMode.CENTER;
    private boolean floatingEnabled = false;
    private float keyboardScale = 1.0f;

    // Backspace repeat
    private final Handler deleteHandler = new Handler();
    private boolean isDeleting = false;
    private final Runnable deleteRunnable = new Runnable() {
        @Override
        public void run() {
            if (isDeleting) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.deleteSurroundingText(1, 0);
                }
                deleteHandler.postDelayed(this, 50);
            }
        }
    };

    // Shift long-press detection
    private final Handler shiftHandler = new Handler(Looper.getMainLooper());
    private boolean isShiftPressed = false;
    private boolean shiftConsumed = false;
    private final Runnable shiftLongPressRunnable = () -> {
        if (isShiftPressed) {
            try {
                // Uzun basma: Caps Lock aç
                capsLock = true;
                caps = true;
                Log.d(TAG, "⬆️⬆️ CAPS LOCK ON - uzun basma");
                
                // Vibration feedback
                try {
                    android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(100);
                        }
                    }
                } catch (Exception ignored) {}
                
                // ULTRA SAFE - her şey try-catch içinde!
                try {
                    if (mainKeyboard != null) {
                        mainKeyboard.setShifted(true);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "mainKeyboard.setShifted error", e);
                }
                
                try {
                    if (symbolsKeyboard != null) {
                        symbolsKeyboard.setShifted(true);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "symbolsKeyboard.setShifted error", e);
                }
                
                // Tuş label'larını güncelle - BÜYÜK HARF
                updateKeyLabels();
                
                // View yenileme - 3 yöntem dene!
                try {
                    if (keyboardView != null) {
                        keyboardView.postInvalidate();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "postInvalidate error (ignored)", e);
                }
                
                // Visual feedback - Toast
                Toast.makeText(TurkishKeyboardService.this, "⬆️ CAPS LOCK", Toast.LENGTH_SHORT).show();
                
                shiftConsumed = true;
                Log.d(TAG, "✅ Caps Lock aktif");
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Caps Lock hatası:", e);
            }
        }
    };

    // Text editing
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private String lastInputState = "";

    // Clipboard
    private ClipboardStore clipboardStore;
    private android.database.ContentObserver screenshotObserver;

    // QR sonucu için broadcast receiver
    private final BroadcastReceiver insertTextReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("text");
            Log.d(TAG, "📩 Broadcast alındı: " + text);
            if (!TextUtils.isEmpty(text)) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.commitText(text, 1);
                    saveInputState();
                    // Kamera view'ı kapat ve klavyeye dön
                    new Handler(getMainLooper()).post(() -> switchToMode(ViewMode.KEYBOARD));
                }
            }
        }
    };

    // OCR picker receiver
    private final BroadcastReceiver ocrPickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && com.qrmaster.app.keyboard.OCRPickActivity.ACTION_OCR_PICK_RESULT.equals(intent.getAction())) {
                android.net.Uri uri = intent.getData();
                if (ocrView != null && uri != null) {
                    ocrView.processImage(uri);
                }
            }
        }
    };

    // Tema değişikliği receiver
    private final BroadcastReceiver themeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "🎨 Tema değişikliği alındı!");
            // Preferences'ı yeniden yükle
            loadPreferences();
            // Temayı uygula
            new Handler(getMainLooper()).post(() -> {
                applyTheme();
                Toast.makeText(TurkishKeyboardService.this, "🎨 Tema güncellendi!", Toast.LENGTH_SHORT).show();
            });
        }
    };

    private enum ViewMode {
        KEYBOARD, CAMERA, EMOJI, VOICE, GIF, CLIPBOARD, TRANSLATE, STICKER, MENU, TEXT_EDIT, 
        MINI_APPS, CALCULATOR, CURRENCY, CALENDAR, OCR, MAP, SHARED_TYPING, TEXT_TEMPLATE, LANGUAGE_SWITCHER, STICKER_PICKER, CRYPTO, MOUSE_MODE, QUICK_SEARCH, QUICK_NOTE, TEXT_EXPANDER
    }

    private enum OneHandMode {
        CENTER, LEFT, RIGHT
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "📱 Klavye servisi oluşturuluyor...");
        
        prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
        loadPreferences();
        clipboardStore = ClipboardStore.getInstance(getApplicationContext());
        keyboardScale = prefs.getFloat("keyboard_scale", 1.0f);
        applyKeyboardScale();
        suggestionManager = new SuggestionManager();
        suggestionManager.load(getApplicationContext(), () -> Log.d(TAG, "✅ Sözlük yüklendi"));
        phrasePredictor = new SmartPhrasePredictor(getApplicationContext());
        smartPrediction = new SmartPredictionManager(getApplicationContext());
        languageManager = new LanguageManager(getApplicationContext());
        com.qrmaster.app.keyboard.textexpander.TextExpanderManager.getInstance(getApplicationContext());
        Log.d(TAG, "✅ Akıllı tahmin sistemi başlatıldı");
        
        
        // Broadcast receiver kaydet
        IntentFilter filter = new IntentFilter("com.qrmaster.app.KEYBOARD_INSERT_TEXT");
        registerReceiver(insertTextReceiver, filter);
        
        // OCR pick receiver
        IntentFilter ocrFilter = new IntentFilter(com.qrmaster.app.keyboard.OCRPickActivity.ACTION_OCR_PICK_RESULT);
        registerReceiver(ocrPickReceiver, ocrFilter);
        
        // Tema değişikliği receiver
        IntentFilter themeFilter = new IntentFilter("com.qrmaster.app.THEME_CHANGED");
        registerReceiver(themeChangeReceiver, themeFilter);
        
        // Clipboard monitor
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.addPrimaryClipChangedListener(() -> {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    if (text != null && text.length() > 0) {
                        clipboardStore.addText(text.toString());
                    }
                }
            });
        }

        registerScreenshotObserver();
        
        Log.d(TAG, "✅ Klavye servisi hazır");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "🔴 Klavye servisi kapatılıyor...");
        try {
            unregisterReceiver(insertTextReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Receiver unregister error", e);
        }
        try {
            unregisterReceiver(themeChangeReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Theme receiver unregister error", e);
        }
        try {
            unregisterReceiver(ocrPickReceiver);
        } catch (Exception e) {
            Log.e(TAG, "OCR receiver unregister error", e);
        }
        if (suggestionManager != null) {
            suggestionManager.shutdown();
        }
        if (phrasePredictor != null) {
            phrasePredictor.shutdown();
        }

        unregisterScreenshotObserver();
        
        // Kaynakları serbest bırak
        releaseAllViews();
        
        super.onDestroy();
    }

    @Override
    public View onCreateInputView() {
        Log.d(TAG, "📐 MODERN Input view oluşturuluyor...");
        
        // Modern layout'u inflate et (toolbar + content)
        View modernView = getLayoutInflater().inflate(
            com.qrmaster.app.R.layout.keyboard_modern_view, null
        );
        
        featureContainer = modernView.findViewById(com.qrmaster.app.R.id.feature_container);
        keyboardContainer = modernView.findViewById(com.qrmaster.app.R.id.keyboard_container);
        candidateStripView = modernView.findViewById(com.qrmaster.app.R.id.candidate_strip);
        if (candidateStripView != null) {
            candidateStripView.setListener(this::applySuggestion);
        }
        
        // Toolbar butonlarını bağla
        setupModernToolbar(modernView);

        rootInputView = modernView;

        // Klavye view'ı oluştur
        createKeyboardView();
        
        Log.d(TAG, "✅ MODERN Input view hazır");
        return modernView;
    }
    
    private volatile long lastToolbarClick = 0;
    
    private View.OnClickListener safeClick(Runnable action) {
        return v -> {
            long now = System.currentTimeMillis();
            if (now - lastToolbarClick < 500) {
                Log.w(TAG, "⚠️ Toolbar: Çok hızlı tıklama");
                return;
            }
            lastToolbarClick = now;
            try {
                action.run();
            } catch (Exception e) {
                Log.e(TAG, "Toolbar action error", e);
            }
        };
    }
    
    private View toolbarView; // Toolbar referansını sakla
    
    private void setupModernToolbar(View view) {
        toolbarView = view; // Toolbar referansını kaydet
        
        // Toggle butonu - Toolbar aç/kapa
        View toggleButton = view.findViewById(com.qrmaster.app.R.id.btn_toggle_toolbar);
        View toolbar = view.findViewById(com.qrmaster.app.R.id.toolbar);
        
        if (toggleButton != null && toolbar != null) {
            toggleButton.setOnClickListener(v -> {
                boolean isVisible = toolbar.getVisibility() == View.VISIBLE;
                
                if (isVisible) {
                    // Kapat - Yukarı kaydır animasyonu
                    toolbar.animate()
                        .translationY(-toolbar.getHeight())
                        .alpha(0f)
                        .setDuration(250)
                        .withEndAction(() -> toolbar.setVisibility(View.GONE))
                        .start();
                    
                    // Toggle button ikonunu değiştir (aşağı ok)
                    if (toggleButton instanceof android.widget.ImageButton) {
                        ((android.widget.ImageButton) toggleButton).setImageResource(com.qrmaster.app.R.drawable.ic_expand_more);
                    }
                } else {
                    // Aç - Aşağı kaydır animasyonu
                    toolbar.setVisibility(View.VISIBLE);
                    toolbar.setTranslationY(-toolbar.getHeight());
                    toolbar.setAlpha(0f);
                    toolbar.animate()
                        .translationY(0)
                        .alpha(1f)
                        .setDuration(250)
                        .start();
                    
                    // Toggle button ikonunu değiştir (yukarı ok)
                    if (toggleButton instanceof android.widget.ImageButton) {
                        ((android.widget.ImageButton) toggleButton).setImageResource(com.qrmaster.app.R.drawable.ic_expand_less);
                    }
                }
            });
        }
        
        // Toggle butonu - Öneriler aç/kapa
        View toggleSuggestions = view.findViewById(com.qrmaster.app.R.id.btn_toggle_suggestions);
        View suggestionsContainer = view.findViewById(com.qrmaster.app.R.id.suggestions_container);
        
        if (toggleSuggestions != null && suggestionsContainer != null) {
            toggleSuggestions.setOnClickListener(v -> {
                boolean isVisible = suggestionsContainer.getVisibility() == View.VISIBLE;
                
                if (isVisible) {
                    // Kapat - Yukarı kaydır animasyonu
                    suggestionsContainer.animate()
                        .translationY(-suggestionsContainer.getHeight())
                        .alpha(0f)
                        .setDuration(250)
                        .withEndAction(() -> suggestionsContainer.setVisibility(View.GONE))
                        .start();
                    
                    // Toggle button opacity değiştir
                    toggleSuggestions.setAlpha(0.5f);
                } else {
                    // Aç - Aşağı kaydır animasyonu
                    suggestionsContainer.setVisibility(View.VISIBLE);
                    suggestionsContainer.setTranslationY(-suggestionsContainer.getHeight());
                    suggestionsContainer.setAlpha(0f);
                    suggestionsContainer.animate()
                        .translationY(0)
                        .alpha(1f)
                        .setDuration(250)
                        .start();
                    
                    // Toggle button opacity değiştir
                    toggleSuggestions.setAlpha(1.0f);
                }
            });
            
            // Başlangıç opacity'si (kapalı)
            toggleSuggestions.setAlpha(0.5f);
        }
        
        view.findViewById(com.qrmaster.app.R.id.btn_quick_menu).setOnClickListener(safeClick(() -> switchToMode(ViewMode.MENU)));
        view.findViewById(com.qrmaster.app.R.id.btn_emoji).setOnClickListener(safeClick(() -> switchToMode(ViewMode.EMOJI)));
        view.findViewById(com.qrmaster.app.R.id.btn_translate).setOnClickListener(safeClick(() -> {
            translateBuffer.setLength(0);
            switchToMode(ViewMode.TRANSLATE);
        }));
        view.findViewById(com.qrmaster.app.R.id.btn_gif).setOnClickListener(safeClick(() -> {
            gifQueryBuffer.setLength(0);
            switchToMode(ViewMode.GIF);
        }));
        view.findViewById(com.qrmaster.app.R.id.btn_camera).setOnClickListener(safeClick(() -> {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "📷 Kamera izni gerekli!\n\nAyarlar → İzinler → Kamera", Toast.LENGTH_LONG).show();
                openAppSettings();
            } else {
                switchToMode(ViewMode.CAMERA);
            }
        }));
        view.findViewById(com.qrmaster.app.R.id.btn_clipboard).setOnClickListener(safeClick(() -> switchToMode(ViewMode.CLIPBOARD)));
        view.findViewById(com.qrmaster.app.R.id.btn_mini_apps).setOnClickListener(safeClick(() -> switchToMode(ViewMode.MINI_APPS)));
        view.findViewById(com.qrmaster.app.R.id.btn_settings).setOnClickListener(safeClick(() -> openKeyboardSettings()));
        
        // Mikrofon - inline çalışır (toolbar içinde)
        View voiceBtn = view.findViewById(com.qrmaster.app.R.id.btn_voice);
        voiceBtn.setOnClickListener(safeClick(() -> {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "🎤 Mikrofon izni gerekli!\n\nAyarlar → İzinler → Mikrofon", Toast.LENGTH_LONG).show();
                openAppSettings();
            } else {
                toggleVoiceRecording(voiceBtn);
            }
        }));
        
        // İlk tema uygulaması
        applyThemeToToolbar();
    }
    
    /**
     * Uygulama ayarlarını aç - İzinler için
     */
    private void openAppSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Ayarlar açılamadı: " + e.getMessage());
            Toast.makeText(this, "Lütfen ayarlardan izin verin", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openKeyboardSettings() {
        try {
            // Ensure we're in a safe state before opening activity
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                try {
                    releaseAllViews();
                    currentMode = ViewMode.KEYBOARD;
                    if (keyboardView != null) {
                        keyboardView.setVisibility(View.VISIBLE);
                    }
                    if (featureContainer != null) {
                        featureContainer.setVisibility(View.GONE);
                    }
                    
                    // Small delay to ensure UI is stable
                    mainHandler.postDelayed(() -> {
                        try {
                            Intent intent = new Intent(TurkishKeyboardService.this, KeyboardSettingsActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.e(TAG, "Ayarlar intent hatası", e);
                            Toast.makeText(TurkishKeyboardService.this, "Ayarlar açılamadı", Toast.LENGTH_SHORT).show();
                        }
                    }, 100);
                } catch (Exception e) {
                    Log.e(TAG, "Ayarlar UI hazırlık hatası", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ayarlar açılamadı", e);
            Toast.makeText(this, "Ayarlar açılamadı", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void createKeyboardView() {
        keyboardView = (KeyboardView) getLayoutInflater()
            .inflate(com.qrmaster.app.R.layout.keyboard_view, null);
        
        // KryptEY STYLE KLAVYELER YÜKLE!
        mainKeyboard = new Keyboard(this, com.qrmaster.app.R.xml.keyboard_kryptey_turkish);
        symbolsKeyboard = new Keyboard(this, com.qrmaster.app.R.xml.keyboard_kryptey_symbols);
        symbolsShiftKeyboard = new Keyboard(this, com.qrmaster.app.R.xml.keyboard_symbols_shift);
        
        keyboardView.setKeyboard(mainKeyboard);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setPreviewEnabled(false);
        
        cacheOriginalLabels();
        applyTheme();
        
        if (keyboardContainer != null) {
            keyboardContainer.removeAllViews();
            keyboardContainer.addView(keyboardView);
            keyboardContainer.post(() -> {
                // Yoğunluk modu uygula (compact/cozy/comfortable)
                applyDensityMode();
                applyKeyboardScale();
                applyTheme();
                applyOneHandMode(false);
                applyFloatingMode(false);
            });
        }
        keyboardView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        Log.d(TAG, "⌨️ Input başlatıldı");
        
        // İlk harf otomatik büyük olsun (cümle başı kontrolü)
        InputConnection ic = getCurrentInputConnection();
        boolean shouldCapitalize = shouldCapitalizeNext(ic);
        
        caps = shouldCapitalize;
        capsLock = false;
        
        if (mainKeyboard != null) {
            mainKeyboard.setShifted(caps);
            // Tuş label'larını güncelle
            updateKeyLabels();
            // SADECE view yenile - %100 SAFE!
            if (keyboardView != null) {
                keyboardView.invalidateAllKeys();
            }
        }
        
        Log.d(TAG, "⌨️ İlk harf otomatik büyük: " + caps);
    }
    
    /**
     * Bir sonraki karakterin büyük harf olup olmayacağını belirle
     * Cümle başı, nokta sonrası vb. durumlarda true döner
     */
    private boolean shouldCapitalizeNext(InputConnection ic) {
        try {
            if (ic == null) {
                return true; // Güvenli default: büyük harf
            }
            
            // Önceki metni al (son 100 karakter)
            CharSequence before = ic.getTextBeforeCursor(100, 0);
            
            if (before == null || before.length() == 0) {
                // Hiç metin yok - ilk karakter
                return true;
            }
            
            String text = before.toString().trim();
            
            if (text.isEmpty()) {
                // Sadece boşluk var - ilk karakter
                return true;
            }
            
            // Son karakteri kontrol et
            char lastChar = text.charAt(text.length() - 1);
            
            // Cümle bitişi: . ! ? sonrası büyük harf
            if (lastChar == '.' || lastChar == '!' || lastChar == '?') {
                return true;
            }
            
            // Yeni satır sonrası büyük harf
            if (lastChar == '\n') {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "shouldCapitalizeNext error", e);
            return true; // Hata durumunda güvenli default
        }
    }
    
    @Override
    public void onFinishInput() {
        super.onFinishInput();
        
        undoStack.clear();
        redoStack.clear();
        lastInputState = "";
        saveInputState();
        composingBuffer.setLength(0);
        clearSuggestions();
        
        // Klavye moduna dön
        if (currentMode != ViewMode.KEYBOARD) {
            switchToMode(ViewMode.KEYBOARD);
        }
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }

        playClick(primaryCode);
        
        // DEBUG: Tüm tuşları logla
        char c = (char) primaryCode;
        Log.d(TAG, "⌨️ onKey: code=" + primaryCode + " char='" + c + "' isQuickSearchMode=" + isQuickSearchMode);

        if (handleModeSpecificKey(primaryCode, ic)) {
            return;
        }

        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                saveInputState();
                
                // SEARCH WRITE MODE - Arama input'tan sil
                if (isSearchWriteMode && searchInputEditText != null) {
                    String current = searchInputEditText.getText().toString();
                    if (current.length() > 0) {
                        searchInputEditText.setText(current.substring(0, current.length() - 1));
                    }
                    Log.d(TAG, "🔍 Search input'tan BACKSPACE");
                } else if (isNoteWriteMode && currentMode == ViewMode.QUICK_NOTE) {
                    // QUICK NOTE YAZMA MODU - QuickNoteView'dan sil
                    View currentView = featureContainer.getChildAt(0);
                    if (currentView instanceof com.qrmaster.app.keyboard.views.QuickNoteView) {
                        ((com.qrmaster.app.keyboard.views.QuickNoteView) currentView).deleteLastChar();
                        Log.d(TAG, "📝 QuickNoteView'dan BACKSPACE");
                    }
                } else if (isCryptoWriteMode && currentMode == ViewMode.CRYPTO && cryptoView != null) {
                    // CRYPTO YAZMA MODU - CryptoView'dan sil
                    cryptoView.deleteLastChar();
                    Log.d(TAG, "🔒 CryptoView'dan BACKSPACE");
                } else if (isMouseKeyboardMode && currentMode == ViewMode.MOUSE_MODE && mouseManager != null) {
                    // MOUSE KEYBOARD MODU - PC'ye backspace gönder
                    mouseManager.sendKeyPress("BACKSPACE");
                    Log.d(TAG, "🖱️ PC'ye BACKSPACE gönderildi");
                } else {
                    // Normal mod - telefonda sil
                    // ÖNCELİKLE seçili metni kontrol et
                    CharSequence selectedText = ic.getSelectedText(0);
                    if (selectedText != null && selectedText.length() > 0) {
                        // Seçili metin varsa, onu sil
                        ic.commitText("", 1);
                        Log.d(TAG, "Seçili metin silindi: " + selectedText.length() + " karakter");
                    } else {
                        // Seçili metin yoksa, normal backspace
                        if (composingBuffer.length() > 0) {
                            composingBuffer.deleteCharAt(composingBuffer.length() - 1);
                            
                            // Eğer buffer boşaldıysa veya "?" kaldıysa, Quick Search modunu iptal et
                            if (composingBuffer.length() <= 1 && isQuickSearchMode) {
                                isQuickSearchMode = false;
                                Log.d(TAG, "🔍 Hızlı arama modu KAPANDI (backspace)");
                            }
                            
                            updateSuggestions();
                        }
                        ic.deleteSurroundingText(1, 0);
                    }
                }
                break;
                
            case Keyboard.KEYCODE_SHIFT:
                // Shift işlemi onPress/onRelease ile yönetiliyor (tek tık/uzun basma)
                break;
                
            case 10: // Enter key (from symbols keyboard)
            case Keyboard.KEYCODE_DONE:
                saveInputState();
                
                // QUICK NOTE YAZMA MODU - QuickNoteView'a yeni satır
                if (isNoteWriteMode && currentMode == ViewMode.QUICK_NOTE) {
                    View currentView = featureContainer.getChildAt(0);
                    if (currentView instanceof com.qrmaster.app.keyboard.views.QuickNoteView) {
                        ((com.qrmaster.app.keyboard.views.QuickNoteView) currentView).appendNewLine();
                        Log.d(TAG, "📝 QuickNoteView'a ENTER (yeni satır)");
                    }
                } else if (isCryptoWriteMode && currentMode == ViewMode.CRYPTO && cryptoView != null) {
                    // CRYPTO YAZMA MODU - CryptoView'a yeni satır
                    cryptoView.appendNewLine();
                    Log.d(TAG, "🔒 CryptoView'a ENTER (yeni satır)");
                } else if (isMouseKeyboardMode && currentMode == ViewMode.MOUSE_MODE && mouseManager != null) {
                    // MOUSE KEYBOARD MODU - PC'ye ENTER gönder
                    mouseManager.sendKeyPress("ENTER");
                    Log.d(TAG, "🖱️ PC'ye ENTER gönderildi: code=" + primaryCode);
                } else {
                    // Normal mod
                    EditorInfo ei = getCurrentInputEditorInfo();
                    if (ei != null && (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
                        int actionId = ei.imeOptions & EditorInfo.IME_MASK_ACTION;
                        if (actionId != EditorInfo.IME_ACTION_NONE
                                && actionId != EditorInfo.IME_ACTION_UNSPECIFIED
                                && (ei.inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0) {
                            sendDefaultEditorAction(true);
                        } else {
                            ic.commitText("\n", 1);
                        }
                    } else {
                        ic.commitText("\n", 1);
                    }
                    // Enter ile cümle bitir
                    if (phrasePredictor != null) {
                        phrasePredictor.finishPhrase();
                    }
                    composingBuffer.setLength(0);
                    clearSuggestions();
                }
                break;
                
            case KEYCODE_SWITCH_SYMBOLS: // 123 butonu
                keyboardView.setKeyboard(symbolsKeyboard);
                keyboardView.invalidateAllKeys();
                break;
                
            case KEYCODE_SYMBOLS_SHIFT: // =\< butonu
                keyboardView.setKeyboard(symbolsShiftKeyboard);
                keyboardView.invalidateAllKeys();
                break;
                
            case KEYCODE_SWITCH_ALPHA: // ABC butonu
                keyboardView.setKeyboard(mainKeyboard);
                keyboardView.invalidateAllKeys();
                break;
                
            case Keyboard.KEYCODE_MODE_CHANGE:
                // Eski mod değiştirme - artık gerekmiyor
                keyboardView.setKeyboard(symbolsKeyboard);
                keyboardView.invalidateAllKeys();
                break;
                
            case KEYCODE_QR_SCAN:
                Log.d(TAG, "📷 QR tarama başlatılıyor - Klavye içi kamera...");
                try {
                    switchToMode(ViewMode.CAMERA);
                } catch (Exception e) {
                    Log.e(TAG, "QR tarama başlatılamadı: " + e.getMessage(), e);
                    Toast.makeText(this, "QR tarayıcı başlatılamadı", Toast.LENGTH_SHORT).show();
                }
                break;
                
            case KEYCODE_EMOJI:
                Log.d(TAG, "😊 Emoji picker açılıyor...");
                switchToMode(ViewMode.EMOJI);
                break;
                
            case KEYCODE_GIF:
                gifQueryBuffer.setLength(0);
                switchToMode(ViewMode.GIF);
                break;
                
            case KEYCODE_VOICE:
                Log.d(TAG, "🎤 Sesli yazma başlatılıyor...");
                try {
                    switchToMode(ViewMode.VOICE);
                } catch (Exception e) {
                    Log.e(TAG, "Sesli yazma hatası: " + e.getMessage(), e);
                    Toast.makeText(this, "Sesli yazma başlatılamadı", Toast.LENGTH_SHORT).show();
                }
                break;
                
            case KEYCODE_CLIPBOARD:
                switchToMode(ViewMode.CLIPBOARD);
                break;
                
            case KEYCODE_TRANSLATE:
                translateBuffer.setLength(0);
                switchToMode(ViewMode.TRANSLATE);
                break;
                
            case KEYCODE_SETTINGS:
                try {
                    Intent intent = new Intent(this, KeyboardSettingsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Ayarlar açılamadı", e);
                }
                break;
                
            default:
                if (handleCharacterKey(primaryCode, keyCodes, ic)) {
                    break;
                }
                break;
        }
    }

    /**
     * Mod değiştir - klavye/kamera/emoji/voice
     */
    private volatile boolean isSwitchingMode = false;
    private volatile long lastModeSwitchTime = 0;
    
    private void switchToMode(ViewMode mode) {
        Log.d(TAG, "🔄 Mod değiştiriliyor: " + currentMode + " -> " + mode);
        
        // CRASH PREVENTION #1: Rapid mode switching
        long now = System.currentTimeMillis();
        if (now - lastModeSwitchTime < 200) {
            Log.w(TAG, "⚠️ Çok hızlı mod değişimi, bekleniyor");
            return;
        }
        
        // CRASH PREVENTION #2: Re-entrancy check
        if (isSwitchingMode) {
            Log.w(TAG, "⚠️ Mod değişimi zaten devam ediyor");
            return;
        }
        
        // CRASH PREVENTION #3: Same mode check
        if (currentMode == mode && mode != ViewMode.KEYBOARD) {
            Log.w(TAG, "⚠️ Aynı mod zaten aktif: " + mode);
            return;
        }
        
        isSwitchingMode = true;
        lastModeSwitchTime = now;
        
        try {
            currentMode = mode;
        
        boolean hideKeyboard = mode == ViewMode.CAMERA
            || mode == ViewMode.VOICE;
        
        try {
            if (keyboardView != null) {
                keyboardView.setVisibility(hideKeyboard ? View.GONE : View.VISIBLE);
                if (!hideKeyboard && keyboardView.getKeyboard() != mainKeyboard
                        && mode == ViewMode.KEYBOARD) {
                    keyboardView.setKeyboard(mainKeyboard);
                }
                if (!hideKeyboard && mode == ViewMode.KEYBOARD) {
                    try {
                        // Klavyeyi görünür yapmayı zorla
                        requestShowSelf(0);
                    } catch (Exception ignored) {}
                }
            }

            if (candidateStripView != null) {
                candidateStripView.setVisibility(mode == ViewMode.KEYBOARD ? View.VISIBLE : View.GONE);
                if (mode != ViewMode.KEYBOARD) {
                    clearSuggestions();
                }
            }
            
            if (featureContainer != null) {
                featureContainer.setVisibility(mode == ViewMode.KEYBOARD ? View.GONE : View.VISIBLE);
            }
            
            releaseAllViews();
        } catch (Exception e) {
            Log.e(TAG, "switchToMode UI hatası", e);
        }
        
        // Yeni mod için view oluştur
        switch (mode) {
            case KEYBOARD:
                if (featureContainer != null) {
                    featureContainer.setVisibility(View.GONE);
                }
                break;
                
            case CAMERA:
                cameraView = new KeyboardCameraView(this, new KeyboardCameraView.ScanCallback() {
                    @Override
                    public void onScanned(String barcode) {
                        Log.d(TAG, "📷 QR tarandı: " + barcode);
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            // Barkodu yaz + ENTER (alt satıra geç)
                            ic.commitText(barcode + "\n", 1);
                            saveInputState();
                        }
                        // KAMERAYI KAPATMA! Sürekli tarama devam etsin
                    }
                    
                    @Override
                    public void onClose() {
                        switchToMode(ViewMode.KEYBOARD);
                    }
                });
                featureContainer.addView(cameraView);
                cameraView.startCamera();
                break;
                
            case EMOJI:
                try {
                    emojiView = new EmojiPickerView(this, new EmojiPickerView.EmojiCallback() {
                        @Override
                        public void onEmojiSelected(String emoji) {
                            Log.d(TAG, "😊 Emoji seçildi: " + emoji);
                            try {
                                InputConnection ic = getCurrentInputConnection();
                                if (ic != null) {
                                    ic.commitText(emoji, 1);
                                    saveInputState();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Emoji commit error", e);
                            }
                            // İstek: Seçimden sonra klavyeye dön
                            switchToMode(ViewMode.KEYBOARD);
                        }
                        
                        @Override
                        public void onClose() {
                            try {
                                switchToMode(ViewMode.KEYBOARD);
                            } catch (Exception e) {
                                Log.e(TAG, "Emoji close error", e);
                            }
                        }
                    });
                    if (featureContainer != null) {
                        featureContainer.addView(emojiView);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "EmojiPickerView oluşturma hatası", e);
                    Toast.makeText(this, "Emoji seçici yüklenemedi", Toast.LENGTH_SHORT).show();
                    try {
                        switchToMode(ViewMode.KEYBOARD);
                    } catch (Exception ex) {
                        Log.e(TAG, "Keyboard switch error", ex);
                    }
                }
                break;
                
            case CLIPBOARD:
                clipboardView = new ClipboardHistoryView(this, new ClipboardHistoryView.ClipboardCallback() {
                    @Override
                    public void onClipSelected(ClipboardEntry entry) {
                        if (entry == null) return;
                        clipboardStore.setSystemClipboard(entry);
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null && entry.getType() == ClipboardEntry.Type.TEXT) {
                            ic.commitText(entry.getContent(), 1);
                            saveInputState();
                        } else if (entry.getType() == ClipboardEntry.Type.SCREENSHOT && ic != null) {
                            // Screenshot'u commitContent ile paylaş
                            android.net.Uri uri = entry.getUri();
                            if (uri != null) {
                                try {
                                    // READ izni ver
                                    grantUriPermission(
                                        getPackageManager().resolveContentProvider("com.android.providers.media.documents", 0).packageName,
                                        uri,
                                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    );
                                    
                                    android.content.ClipDescription description = new android.content.ClipDescription(
                                        "Screenshot",
                                        new String[]{"image/png", "image/jpeg", "image/*"}
                                    );
                                    
                                    android.view.inputmethod.InputContentInfo contentInfo = 
                                        new android.view.inputmethod.InputContentInfo(
                                            uri,
                                            description,
                                            null // linkUri
                                        );
                                    
                                    int flags = android.view.inputmethod.InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION;
                                    boolean success = ic.commitContent(contentInfo, flags, null);
                                    
                                    if (success) {
                                        Toast.makeText(TurkishKeyboardService.this, "Görsel gönderildi", Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Fallback: ClipData ile dene
                                        android.content.ClipData clipData = android.content.ClipData.newUri(
                                            getContentResolver(),
                                            "Screenshot",
                                            uri
                                        );
                                        android.content.ClipboardManager clipboard = 
                                            (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                        if (clipboard != null) {
                                            clipboard.setPrimaryClip(clipData);
                                            Toast.makeText(TurkishKeyboardService.this, "Panoya kopyalandı - Yapıştır ile kullanın", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    
                                    contentInfo.requestPermission();
                                } catch (Exception e) {
                                    Log.e(TAG, "Screenshot paylaşma hatası", e);
                                    Toast.makeText(TurkishKeyboardService.this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        switchToMode(ViewMode.KEYBOARD);
                    }

                    @Override
                    public void onPinToggled(ClipboardEntry entry) {
                        clipboardStore.togglePin(entry.getId());
                    }

                    @Override
                    public void onDelete(ClipboardEntry entry) {
                        clipboardStore.delete(entry.getId());
                    }

                    @Override
                    public void onClose() {
                        switchToMode(ViewMode.KEYBOARD);
                    }
                }, clipboardStore);
                featureContainer.addView(clipboardView);
                break;
                
            case VOICE:
                voiceView = new KeyboardVoiceView(this, new KeyboardVoiceView.VoiceCallback() {
                    @Override
                    public void onResult(String text) {
                        Log.d(TAG, "🎤 Sesli yazma sonucu: " + text);
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            ic.commitText(text + " ", 1);
                            saveInputState();
                        }
                    }
                    
                    @Override
                    public void onClose() {
                        switchToMode(ViewMode.KEYBOARD);
                    }
                });
                featureContainer.addView(voiceView);
                voiceView.startVoice();
                break;
                
            case GIF:
                try {
                    gifView = new GifPickerView(this, new GifPickerView.GifCallback() {
                        @Override
                        public void onGifSelected(String gifUrl, String tinyGifUrl) {
                            Log.d(TAG, "🎬 GIF seçildi: " + gifUrl);
                            // GIF'i asenkron olarak image/gif içerik türünde gönder
                            commitGifFromUrlAsync(gifUrl);
                            // İstek: Seçimden sonra klavyeye dön
                            switchToMode(ViewMode.KEYBOARD);
                        }
                        
                        @Override
                        public void onClose() {
                            try {
                                switchToMode(ViewMode.KEYBOARD);
                            } catch (Exception e) {
                                Log.e(TAG, "GIF close error", e);
                            }
                        }
                    });
                    gifQueryBuffer.setLength(0);
                    if (gifView != null) {
                        gifView.setQuery("");
                        gifView.setStatusText("Trend GIF'ler yükleniyor...");
                        gifView.loadTrending();
                    }
                    if (featureContainer != null) {
                        featureContainer.addView(gifView);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "GifPickerView oluşturma hatası", e);
                    Toast.makeText(this, "GIF seçici yüklenemedi", Toast.LENGTH_SHORT).show();
                    try {
                        switchToMode(ViewMode.KEYBOARD);
                    } catch (Exception ex) {
                        Log.e(TAG, "Keyboard switch error", ex);
                    }
                }
                break;
                
            case STICKER:
                stickerView = new StickerPickerView(this, new StickerPickerView.StickerCallback() {
                    @Override
                    public void onStickerSelected(String sticker) {
                        Log.d(TAG, "🎨 Çıkartma seçildi: " + sticker);
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            ic.commitText(sticker, 1);
                            saveInputState();
                        }
                        switchToMode(ViewMode.KEYBOARD);
                    }
                    
                    @Override
                    public void onClose() {
                        switchToMode(ViewMode.KEYBOARD);
                    }
                });
                featureContainer.addView(stickerView);
                break;
                
            case TRANSLATE:
                translateView = new TranslateView(this, new TranslateView.TranslateCallback() {
                    @Override
                    public void onTranslated(String text) {
                        Log.d(TAG, "🌐 Çeviri yapıldı: " + text);
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            ic.commitText(text, 1);
                            saveInputState();
                        }
                        switchToMode(ViewMode.KEYBOARD);
                    }

                    @Override
                    public void onClose() {
                        switchToMode(ViewMode.KEYBOARD);
                    }

                    @Override
                    public void onClear() {
                        translateBuffer.setLength(0);
                    }
                });
                translateBuffer.setLength(0);
                // translateView.setInputText(""); // Method doesn't exist
                // translateView.setTranslatedText(""); // Method doesn't exist
                // translateView.setStatus("Metin bekleniyor..."); // Method doesn't exist
                featureContainer.addView(translateView);
                break;

            case MENU:
                quickMenuView = new QuickMenuView(this, action -> {
                    switch (action) {
                        case THEME:
                            Log.d(TAG, "🎨 THEME action tetiklendi!");
                            showThemeDialog();
                            break;
                        case ONE_HANDED:
                            // cycleOneHandMode(); // Method doesn't exist
                            break;
                        case TEXT_EDIT:
                            try {
                                switchToMode(ViewMode.TEXT_EDIT);
                            } catch (Exception e) {
                                Log.e(TAG, "Text edit açılamadı", e);
                                Toast.makeText(this, "Metin düzenleme açılamadı", Toast.LENGTH_SHORT).show();
                                switchToMode(ViewMode.KEYBOARD);
                            }
                            break;
                        case FLOATING:
                            // toggleFloatingMode(); // Method doesn't exist
                            break;
                        case RESIZE:
                            switchToMode(ViewMode.KEYBOARD);
                            break;
                        case EMOJI:
                            switchToMode(ViewMode.EMOJI);
                            break;
                        case GIF:
                            gifQueryBuffer.setLength(0);
                            switchToMode(ViewMode.GIF);
                            break;
                        case STICKER:
                            switchToMode(ViewMode.STICKER_PICKER);
                            break;
                        case CRYPTO:
                            switchToMode(ViewMode.CRYPTO);
                            break;
                        case CLIPBOARD:
                            switchToMode(ViewMode.CLIPBOARD);
                            break;
                        case TRANSLATE:
                            translateBuffer.setLength(0);
                            switchToMode(ViewMode.TRANSLATE);
                            break;
                        case CAMERA:
                            switchToMode(ViewMode.CAMERA);
                            break;
                        case VOICE:
                            switchToMode(ViewMode.VOICE);
                            break;
                        case SETTINGS:
                            openKeyboardSettings();
                            switchToMode(ViewMode.KEYBOARD);
                            break;
                        case LANGUAGE:
                            switchToMode(ViewMode.LANGUAGE_SWITCHER);
                            break;
                        case TEXT_TEMPLATE:
                            switchToMode(ViewMode.TEXT_TEMPLATE);
                            break;
                        case MOUSE:
                            switchToMode(ViewMode.MOUSE_MODE);
                            break;
                        case QUICK_SEARCH:
                            // Hızlı arama - Dialog göster
                            showQuickSearchDialog();
                            break;
                        case QUICK_NOTE:
                            // Hızlı not
                            switchToMode(ViewMode.QUICK_NOTE);
                            break;
                        case TEXT_EXPANDER:
                            // Kısayollar
                            switchToMode(ViewMode.TEXT_EXPANDER);
                            break;
                        case CLOSE:
                            switchToMode(ViewMode.KEYBOARD);
                            break;
                    }
                });
                featureContainer.addView(quickMenuView);
                break;

            case TEXT_EDIT:
                Log.d(TAG, "📐 TEXT_EDIT modu başlatılıyor...");
                try {
                    // CRITICAL: Check all prerequisites
                    if (featureContainer == null) {
                        Log.e(TAG, "❌ featureContainer null!");
                        Toast.makeText(this, "Klavye yüklenemedi", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Release old views first
                    try {
                        releaseAllViews();
                    } catch (Exception e) {
                        Log.e(TAG, "releaseAllViews error (non-fatal)", e);
                    }
                    
                    Log.d(TAG, "✓ TextToolsView oluşturuluyor...");
                    textToolsView = new TextToolsView(this, new TextToolsView.TextToolsCallback() {
                        @Override
                        public void onIncreaseSize() {
                            try {
                                Log.d(TAG, "📏 Klavye büyütülüyor");
                                adjustKeyboardScale(0.1f);
                            } catch (Exception e) {
                                Log.e(TAG, "Büyütme hatası", e);
                            }
                        }

                        @Override
                        public void onDecreaseSize() {
                            try {
                                Log.d(TAG, "📏 Klavye küçültülüyor");
                                adjustKeyboardScale(-0.1f);
                            } catch (Exception e) {
                                Log.e(TAG, "Küçültme hatası", e);
                            }
                        }

                        @Override
                        public void onResetSize() {
                            try {
                                Log.d(TAG, "📏 Klavye sıfırlanıyor");
                                resetKeyboardScale();
                            } catch (Exception e) {
                                Log.e(TAG, "Sıfırlama hatası", e);
                            }
                        }

                        @Override
                        public void onMoveCursorLeft() {
                            try { moveCursorBy(-1); } catch (Exception e) { Log.e(TAG, "Cursor left error", e); }
                        }

                        @Override
                        public void onMoveCursorRight() {
                            try { moveCursorBy(1); } catch (Exception e) { Log.e(TAG, "Cursor right error", e); }
                        }

                        @Override
                        public void onMoveCursorStart() {
                            try { moveCursorToEdge(true); } catch (Exception e) { Log.e(TAG, "Cursor start error", e); }
                        }

                        @Override
                        public void onMoveCursorEnd() {
                            try { moveCursorToEdge(false); } catch (Exception e) { Log.e(TAG, "Cursor end error", e); }
                        }

                        @Override
                        public void onCopy() {
                            try { copyText(); } catch (Exception e) { Log.e(TAG, "Copy error", e); }
                        }

                        @Override
                        public void onPaste() {
                            try { pasteText(); } catch (Exception e) { Log.e(TAG, "Paste error", e); }
                        }

                        @Override
                        public void onCut() {
                            try { cutText(); } catch (Exception e) { Log.e(TAG, "Cut error", e); }
                        }

                        @Override
                        public void onSelectAll() {
                            try { selectAllText(); } catch (Exception e) { Log.e(TAG, "Select all error", e); }
                        }

                        @Override
                        public void onClose() {
                            try {
                                Log.d(TAG, "TEXT_EDIT kapatılıyor");
                                switchToMode(ViewMode.KEYBOARD);
                            } catch (Exception e) {
                                Log.e(TAG, "Close error", e);
                            }
                        }
                    });
                    Log.d(TAG, "✓ TextToolsView oluşturuldu, ekleniyor...");
                    featureContainer.addView(textToolsView);
                    featureContainer.setVisibility(View.VISIBLE);
                    currentMode = ViewMode.TEXT_EDIT;
                    Log.d(TAG, "✅ TEXT_EDIT modu aktif");
                } catch (Exception e) {
                    Log.e(TAG, "❌ TextToolsView CRASH: " + e.getMessage(), e);
                    Toast.makeText(this, "Metin araçları hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    try {
                        if (currentMode != ViewMode.KEYBOARD) {
                            switchToMode(ViewMode.KEYBOARD);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Recovery failed", ex);
                    }
                }
                break;

            case MINI_APPS:
                miniAppsHubView = new com.qrmaster.app.keyboard.miniapps.MiniAppsHubView(this, new com.qrmaster.app.keyboard.miniapps.MiniAppsHubView.MiniAppsCallback() {
                    @Override
                    public void onMiniAppSelected(com.qrmaster.app.keyboard.miniapps.MiniAppsHubView.MiniAppType type) {
                        switch (type) {
                            case CALCULATOR: switchToMode(ViewMode.CALCULATOR); break;
                            case CURRENCY: switchToMode(ViewMode.CURRENCY); break;
                            case CALENDAR: switchToMode(ViewMode.CALENDAR); break;
                            case OCR: switchToMode(ViewMode.OCR); break;
                            case MAP: switchToMode(ViewMode.MAP); break;
                            case SHARED_TYPING: switchToMode(ViewMode.SHARED_TYPING); break;
                        }
                    }
                    @Override
                    public void onClose() { switchToMode(ViewMode.KEYBOARD); }
                });
                featureContainer.addView(miniAppsHubView);
                break;

            case CALCULATOR:
                calculatorView = new com.qrmaster.app.keyboard.miniapps.CalculatorView(this, new com.qrmaster.app.keyboard.miniapps.CalculatorView.CalculatorCallback() {
                    @Override
                    public void onResult(String result) {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) { ic.commitText(result, 1); saveInputState(); }
                    }
                    @Override
                    public void onClose() { switchToMode(ViewMode.KEYBOARD); }
                });
                featureContainer.addView(calculatorView);
                break;

            case CURRENCY:
                currencyView = new com.qrmaster.app.keyboard.miniapps.CurrencyConverterView(this, new com.qrmaster.app.keyboard.miniapps.CurrencyConverterView.CurrencyCallback() {
                    @Override
                    public void onResult(String result) {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) { ic.commitText(result, 1); saveInputState(); }
                    }
                    @Override
                    public void onClose() { switchToMode(ViewMode.KEYBOARD); }
                });
                featureContainer.addView(currencyView);
                break;

            case CALENDAR:
                calendarView = new com.qrmaster.app.keyboard.miniapps.MiniCalendarView(this, new com.qrmaster.app.keyboard.miniapps.MiniCalendarView.CalendarCallback() {
                    @Override
                    public void onDateSelected(String date) {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) { ic.commitText(date, 1); saveInputState(); }
                    }
                    @Override
                    public void onClose() { switchToMode(ViewMode.KEYBOARD); }
                });
                featureContainer.addView(calendarView);
                break;

            case OCR:
                ocrView = new com.qrmaster.app.keyboard.miniapps.OCRView(this, new com.qrmaster.app.keyboard.miniapps.OCRView.OCRCallback() {
                    @Override
                    public void onResult(String text) {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) { ic.commitText(text, 1); saveInputState(); }
                    }
                    @Override
                    public void onClose() { switchToMode(ViewMode.KEYBOARD); }
                    @Override
                    public void onSelectImage() {
                        try {
                            Intent intent = new Intent(TurkishKeyboardService.this, OCRPickActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.e(TAG, "OCR gallery start error", e);
                            Toast.makeText(TurkishKeyboardService.this, "Galeri açılamıyor", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onSelectCamera() {
                        try {
                            Intent intent = new Intent(TurkishKeyboardService.this, OCRPickActivity.class);
                            intent.putExtra("camera", true);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.e(TAG, "OCR camera start error", e);
                            Toast.makeText(TurkishKeyboardService.this, "Kamera açılamıyor", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                featureContainer.addView(ocrView);
                break;

            case MAP:
                mapView = new com.qrmaster.app.keyboard.miniapps.MiniMapView(this, new com.qrmaster.app.keyboard.miniapps.MiniMapView.MapCallback() {
                    @Override
                    public void onLocationSelected(String locationUrl) {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) { ic.commitText(locationUrl, 1); saveInputState(); }
                    }
                    @Override
                    public void onClose() { switchToMode(ViewMode.KEYBOARD); }
                });
                featureContainer.addView(mapView);
                break;

            case SHARED_TYPING:
                sharedTypingView = new com.qrmaster.app.keyboard.miniapps.SharedTypingView(this, new com.qrmaster.app.keyboard.miniapps.SharedTypingView.Callback() {
                    @Override
                    public void onTextInsert(String text) {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) { ic.commitText(text, 1); saveInputState(); }
                    }
                    @Override
                    public void onClose() { switchToMode(ViewMode.KEYBOARD); }
                    
                    @Override
                    public void onTextRequest(android.widget.TextView targetView) {
                        // Basit çözüm: Hazır mesajları kullan (dialog yerine)
                        // TextView'e doğrudan yazılmayacak, sadece hazır mesajlar kullanılacak
                    }
                });
                featureContainer.addView(sharedTypingView);
                break;
            
            case TEXT_TEMPLATE:
                com.qrmaster.app.keyboard.views.TextTemplateView templateView = new com.qrmaster.app.keyboard.views.TextTemplateView(this, new com.qrmaster.app.keyboard.views.TextTemplateView.Callback() {
                    @Override
                    public void onTemplateSelected(String text) {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) { 
                            ic.commitText(text, 1); 
                            saveInputState(); 
                        }
                        switchToMode(ViewMode.KEYBOARD);
                    }
                    @Override
                    public void onClose() { switchToMode(ViewMode.KEYBOARD); }
                });
                featureContainer.addView(templateView);
                break;
            
            case LANGUAGE_SWITCHER:
                com.qrmaster.app.keyboard.views.LanguageSwitcherView langView = new com.qrmaster.app.keyboard.views.LanguageSwitcherView(this, languageManager, new com.qrmaster.app.keyboard.views.LanguageSwitcherView.Callback() {
                    @Override
                    public void onLanguageSelected(LanguageManager.Language language) {
                        languageManager.setCurrentLanguage(language);
                        // Klavyeyi yeniden yükle
                        mainKeyboard = languageManager.createKeyboardForCurrentLanguage(TurkishKeyboardService.this);
                        if (keyboardView != null) {
                            keyboardView.setKeyboard(mainKeyboard);
                            cacheOriginalLabels();
                            applyTheme();
                        }
                        Toast.makeText(TurkishKeyboardService.this, language.flag + " " + language.name, Toast.LENGTH_SHORT).show();
                        switchToMode(ViewMode.KEYBOARD);
                    }
                    @Override
                    public void onClose() { switchToMode(ViewMode.KEYBOARD); }
                });
                featureContainer.addView(langView);
                break;
            
            case STICKER_PICKER:
                com.qrmaster.app.keyboard.views.StickerPickerView stickerView = new com.qrmaster.app.keyboard.views.StickerPickerView(this, new com.qrmaster.app.keyboard.views.StickerPickerView.Callback() {
                    @Override
                    public void onStickerSelected(String sticker) {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            ic.commitText(sticker, 1);
                            saveInputState();
                        }
                    }
                    @Override
                    public void onClose() { switchToMode(ViewMode.KEYBOARD); }
                });
                featureContainer.addView(stickerView);
                break;
            
            case CRYPTO:
                Log.d(TAG, "🔒 CRYPTO modu başlatılıyor...");
                cryptoView = new com.qrmaster.app.keyboard.views.CryptoView(this);
                cryptoView.setLegacyCallback(new com.qrmaster.app.keyboard.views.CryptoView.Callback() {
                    @Override
                    public void onClose() {
                        switchToMode(ViewMode.KEYBOARD);
                    }

                    @Override
                    public void onScanQR() {
                        // QR tarama için kamera moduna geç
                        Log.d(TAG, "📷 QR tarama başlatılıyor...");
                        switchToMode(ViewMode.CAMERA);
                        Toast.makeText(TurkishKeyboardService.this,
                            "📷 QR kod okutun, sonuç Crypto'ya kopyalanacak",
                            Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onShowQR(String qrData) {
                        // QR kod göster (overlay)
                        showQRCodeOverlay(qrData);
                    }

                    @Override
                    public void onRequestNFC() {
                        // NFC özelliği (gelecekte implement edilecek)
                        Toast.makeText(TurkishKeyboardService.this,
                            "📡 NFC özelliği yakında eklenecek!",
                            Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCryptoWriteModeChanged(boolean enabled) {
                        // Crypto mesaj yazma modu (Mouse gibi!)
                        isCryptoWriteMode = enabled;
                        Log.d(TAG, "⌨️ Crypto yazma modu: " + (enabled ? "AÇIK" : "KAPALI"));
                    }
                });
                
                // YENİ CryptoViewCallback (Password/Message için)
                cryptoView.setCallback(new com.qrmaster.app.keyboard.views.CryptoView.CryptoViewCallback() {
                    @Override
                    public void onCryptoWriteModeChanged(boolean isPasswordMode, boolean isMessageMode) {
                        isCryptoWriteMode = isPasswordMode || isMessageMode;
                        Log.d(TAG, "⌨️ Crypto yazma: password=" + isPasswordMode + " message=" + isMessageMode);
                        
                        if (isCryptoWriteMode) {
                            Toast.makeText(TurkishKeyboardService.this, 
                                "✅ Klavyeden yazabilirsiniz!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                
                featureContainer.addView(cryptoView);
                break;
            
            case MOUSE_MODE:
                com.qrmaster.app.keyboard.views.MouseModeView mouseModeView = new com.qrmaster.app.keyboard.views.MouseModeView(this, new com.qrmaster.app.keyboard.views.MouseModeView.Callback() {
                    @Override
                    public void onClose() { switchToMode(ViewMode.KEYBOARD); }
                    @Override
                    public void onModeSwitch() { switchToMode(ViewMode.KEYBOARD); }
                    @Override
                    public void onCameraRequest() { 
                        // QR tarama için kamera moduna geç
                        switchToMode(ViewMode.CAMERA);
                    }
                    @Override
                    public void onTextInput(String text) {
                        // Klavyeden yazılan text'i PC'ye gönder
                        if (mouseManager != null && text != null && !text.isEmpty()) {
                            for (char c : text.toCharArray()) {
                                mouseManager.sendKeyPress(String.valueOf(c));
                            }
                        }
                    }
                    @Override
                    public void onKeyboardModeChanged(boolean enabled) {
                        // Yazma modu açık/kapalı
                        isMouseKeyboardMode = enabled;
                        Log.d(TAG, "Mouse keyboard mode: " + enabled);
                    }
                });
                // MouseManager'ı paylaş
                if (mouseManager != null) {
                    mouseModeView.setMouseManager(mouseManager);
                } else {
                    mouseManager = mouseModeView.getMouseManager();
                }
                featureContainer.addView(mouseModeView);
                break;
            
            case QUICK_SEARCH:
                Log.d(TAG, "🔍 QUICK_SEARCH modu başlatılıyor...");
                final com.qrmaster.app.keyboard.views.QuickSearchView quickSearchView = 
                    new com.qrmaster.app.keyboard.views.QuickSearchView(this);
                quickSearchView.setTag("quick_search");
                quickSearchView.setCallback(new com.qrmaster.app.keyboard.views.QuickSearchView.Callback() {
                    @Override
                    public void onClose() {
                        switchToMode(ViewMode.KEYBOARD);
                    }
                    
                    @Override
                    public void onResultSelected(String result) {
                        // Sonucu metin alanına yapıştır
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            ic.commitText(result, 1);
                        }
                        Toast.makeText(TurkishKeyboardService.this, 
                            "✅ Yapıştırıldı!", Toast.LENGTH_SHORT).show();
                    }
                });
                featureContainer.addView(quickSearchView);
                
                // Eğer daha önce bir query varsa, aramayı başlat
                if (pendingSearchQuery != null && !pendingSearchQuery.isEmpty()) {
                    final String query = pendingSearchQuery;
                    pendingSearchQuery = null;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        quickSearchView.search(query);
                    }, 100);
                }
                break;
            
            case TEXT_EXPANDER:
                Log.d(TAG, "⚡ TEXT_EXPANDER modu başlatılıyor...");
                com.qrmaster.app.keyboard.textexpander.TextExpanderView textExpanderView = 
                    new com.qrmaster.app.keyboard.textexpander.TextExpanderView(this, 
                        new com.qrmaster.app.keyboard.textexpander.TextExpanderView.Callback() {
                            @Override
                            public void onClose() {
                                switchToMode(ViewMode.KEYBOARD);
                            }
                        });
                featureContainer.addView(textExpanderView);
                break;
                
            case QUICK_NOTE:
                Log.d(TAG, "📝 QUICK_NOTE modu başlatılıyor...");
                com.qrmaster.app.keyboard.views.QuickNoteView quickNoteView = 
                    new com.qrmaster.app.keyboard.views.QuickNoteView(this);
                quickNoteView.setCallback(new com.qrmaster.app.keyboard.views.QuickNoteView.Callback() {
                    @Override
                    public void onClose() {
                        isNoteWriteMode = false;
                        switchToMode(ViewMode.KEYBOARD);
                    }
                    
                    @Override
                    public void onNoteSaved(String title, String content) {
                        // Not kaydedildi, klavyeye dön
                        Toast.makeText(TurkishKeyboardService.this, 
                            "✅ Not kaydedildi!", Toast.LENGTH_SHORT).show();
                        isNoteWriteMode = false;
                        switchToMode(ViewMode.KEYBOARD);
                    }
                    
                    @Override
                    public void onNoteWriteModeChanged(boolean enabled, boolean isTitle) {
                        isNoteWriteMode = enabled;
                        isNoteTitle = isTitle;
                        Log.d(TAG, "📝 Not yazma modu: " + enabled + " (title=" + isTitle + ")");
                    }
                });
                featureContainer.addView(quickNoteView);
                break;
            

            default:
                if (keyboardView != null) {
                    keyboardView.setKeyboard(mainKeyboard);
                    keyboardView.setVisibility(View.VISIBLE);
                }
                currentMode = ViewMode.KEYBOARD;
                break;
        }
        
        Log.d(TAG, "✅ Mod değiştirildi: " + mode);
        
        } catch (Exception e) {
            Log.e(TAG, "❌ Mod değiştirme CRASH: " + mode, e);
            Toast.makeText(this, "Bir hata oluştu", Toast.LENGTH_SHORT).show();
            try {
                releaseAllViews();
                if (keyboardView != null) keyboardView.setVisibility(View.VISIBLE);
                currentMode = ViewMode.KEYBOARD;
            } catch (Exception ex) {
                Log.e(TAG, "Recovery failed", ex);
            }
        } finally {
            isSwitchingMode = false;
        }
    }
    
    /**
     * Hızlı Arama Dialog'u göster - Direkt QuickSearchView'a geç
     */
    private void showQuickSearchDialog() {
        // Dialog yerine direkt bir input view göster
        Toast.makeText(this, "🔍 Hızlı Arama açılıyor...", Toast.LENGTH_SHORT).show();
        
        // QuickMenu'yü kapat
        switchToMode(ViewMode.KEYBOARD);
        
        // 100ms sonra input dialog göster (UI thread için)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            showSearchInputView();
        }, 100);
    }
    
    /**
     * Arama input view'ı göster (inline) - KLAVYE İLE YAZMA MODU
     */
    private void showSearchInputView() {
        // Basit bir LinearLayout ile input view oluştur
        android.widget.LinearLayout inputContainer = new android.widget.LinearLayout(this);
        inputContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        inputContainer.setBackgroundColor(0xFF1C1C1E);
        inputContainer.setPadding(dp(20), dp(20), dp(20), dp(20));
        
        // Başlık ve Klavye Toggle
        android.widget.LinearLayout titleRow = new android.widget.LinearLayout(this);
        titleRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText("🔍 Hızlı Arama");
        title.setTextColor(0xFF00BFFF);
        title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        android.widget.LinearLayout.LayoutParams titleParams = new android.widget.LinearLayout.LayoutParams(
            0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleRow.addView(title, titleParams);
        
        inputContainer.addView(titleRow);
        
        // Boşluk
        android.view.View space1 = new android.view.View(this);
        android.widget.LinearLayout.LayoutParams spaceParams1 = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(16));
        inputContainer.addView(space1, spaceParams1);
        
        // Input + Klavye Toggle butonu
        android.widget.LinearLayout inputRow = new android.widget.LinearLayout(this);
        inputRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        inputRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("⌨️ butonuna basıp yaz...");
        input.setHintTextColor(0xFF666666);
        input.setTextColor(0xFFFFFFFF);
        input.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        input.setBackgroundColor(0xFF2C2C2E);
        
        // Klavye açılmasın!
        input.setFocusable(false);
        input.setClickable(false);
        input.setCursorVisible(false);
        
        android.widget.LinearLayout.LayoutParams inputParams = new android.widget.LinearLayout.LayoutParams(
            0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        inputRow.addView(input, inputParams);
        
        // ⌨️ Klavye Toggle butonu
        final android.widget.Button keyboardBtn = new android.widget.Button(this);
        keyboardBtn.setText("⌨️");
        keyboardBtn.setTextColor(0xFFFFFFFF);
        keyboardBtn.setBackgroundColor(0xFF2C2C2E);
        keyboardBtn.setPadding(dp(16), dp(12), dp(16), dp(12));
        android.widget.LinearLayout.LayoutParams keyboardBtnParams = new android.widget.LinearLayout.LayoutParams(
            dp(56), android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        keyboardBtnParams.leftMargin = dp(8);
        
        final boolean[] isWriteMode = {false}; // Flag
        
        keyboardBtn.setOnClickListener(v -> {
            isWriteMode[0] = !isWriteMode[0];
            
            if (isWriteMode[0]) {
                keyboardBtn.setText("✅");
                keyboardBtn.setBackgroundColor(0xFF00BFFF);
                isSearchWriteMode = true;
                searchInputEditText = input; // Referansı sakla
                Toast.makeText(this, "✅ Klavye modu AÇIK\nArtık yazabilirsin!", Toast.LENGTH_SHORT).show();
            } else {
                keyboardBtn.setText("⌨️");
                keyboardBtn.setBackgroundColor(0xFF2C2C2E);
                isSearchWriteMode = false;
                searchInputEditText = null;
                Toast.makeText(this, "⌨️ Klavye modu KAPALI", Toast.LENGTH_SHORT).show();
            }
        });
        
        inputRow.addView(keyboardBtn, keyboardBtnParams);
        inputContainer.addView(inputRow);
        
        // Boşluk
        android.view.View space2 = new android.view.View(this);
        android.widget.LinearLayout.LayoutParams spaceParams2 = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(16));
        inputContainer.addView(space2, spaceParams2);
        
        // Butonlar
        android.widget.LinearLayout buttonRow = new android.widget.LinearLayout(this);
        buttonRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        buttonRow.setGravity(android.view.Gravity.CENTER);
        
        // İptal butonu
        android.widget.Button cancelBtn = new android.widget.Button(this);
        cancelBtn.setText("İPTAL");
        cancelBtn.setTextColor(0xFFFF3B30);
        cancelBtn.setBackgroundColor(0xFF2C2C2E);
        cancelBtn.setPadding(dp(24), dp(12), dp(24), dp(12));
        android.widget.LinearLayout.LayoutParams cancelParams = new android.widget.LinearLayout.LayoutParams(
            0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        cancelParams.rightMargin = dp(8);
        cancelBtn.setOnClickListener(v -> {
            isSearchWriteMode = false;
            searchInputEditText = null;
            switchToMode(ViewMode.KEYBOARD);
        });
        buttonRow.addView(cancelBtn, cancelParams);
        
        // Ara butonu
        android.widget.Button searchBtn = new android.widget.Button(this);
        searchBtn.setText("🔍 ARA");
        searchBtn.setTextColor(0xFFFFFFFF);
        searchBtn.setBackgroundColor(0xFF00BFFF);
        searchBtn.setPadding(dp(24), dp(12), dp(24), dp(12));
        android.widget.LinearLayout.LayoutParams searchParams = new android.widget.LinearLayout.LayoutParams(
            0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        searchParams.leftMargin = dp(8);
        searchBtn.setOnClickListener(v -> {
            String query = input.getText().toString().trim();
            if (!query.isEmpty()) {
                isSearchWriteMode = false;
                searchInputEditText = null;
                pendingSearchQuery = query;
                switchToMode(ViewMode.QUICK_SEARCH);
                Toast.makeText(this, "🔍 Arama başlıyor: " + query, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ Arama kelimesi gir!", Toast.LENGTH_SHORT).show();
            }
        });
        buttonRow.addView(searchBtn, searchParams);
        
        inputContainer.addView(buttonRow);
        
        // View'ı featureContainer'a ekle
        featureContainer.removeAllViews();
        featureContainer.addView(inputContainer);
        featureContainer.setVisibility(android.view.View.VISIBLE);
        
        // Klavyeyi GÖSTER (gizleme!)
        if (keyboardView != null) {
            keyboardView.setVisibility(android.view.View.VISIBLE);
        }
    }
    
    /**
     * Tüm özel view'ları release et
     */
    private void releaseAllViews() {
        if (featureContainer == null) {
            return;
        }

        featureContainer.removeAllViews();

        if (cameraView != null) {
            try {
                cameraView.release();
                featureContainer.removeView(cameraView);
            } catch (Exception e) {
                Log.e(TAG, "Camera view release hatası", e);
            }
            cameraView = null;
        }
        
        if (emojiView != null) {
            try {
                featureContainer.removeView(emojiView);
            } catch (Exception e) {
                Log.e(TAG, "Emoji view remove hatası", e);
            }
            emojiView = null;
        }
        
        if (voiceView != null) {
            try {
                voiceView.release();
                featureContainer.removeView(voiceView);
            } catch (Exception e) {
                Log.e(TAG, "Voice view release hatası", e);
            }
            voiceView = null;
        }
        
        if (clipboardView != null) {
            try {
                featureContainer.removeView(clipboardView);
            } catch (Exception e) {
                Log.e(TAG, "Clipboard view remove hatası", e);
            }
            clipboardView = null;
        }
        
        if (gifView != null) {
            try {
                gifView.release();
                featureContainer.removeView(gifView);
            } catch (Exception e) {
                Log.e(TAG, "GIF view remove hatası", e);
            }
            gifView = null;
            gifQueryBuffer.setLength(0);
        }
        
        if (stickerView != null) {
            try {
                featureContainer.removeView(stickerView);
            } catch (Exception e) {
                Log.e(TAG, "Sticker view remove hatası", e);
            }
            stickerView = null;
        }
        
        if (translateView != null) {
            try {
                translateView.release();
                featureContainer.removeView(translateView);
            } catch (Exception e) {
                Log.e(TAG, "Translate view remove hatası", e);
            }
            translateView = null;
            translateBuffer.setLength(0);
        }
        
        if (quickMenuView != null) {
            try {
                featureContainer.removeView(quickMenuView);
            } catch (Exception e) {
                Log.e(TAG, "Quick menu view remove hatası", e);
            }
            quickMenuView = null;
        }
        if (textToolsView != null && featureContainer != null) {
            featureContainer.removeView(textToolsView);
            textToolsView = null;
        }

        // Mini Apps cleanup
        if (miniAppsHubView != null) { miniAppsHubView = null; }
        if (calculatorView != null) { calculatorView = null; }
        if (currencyView != null) { try { currencyView.release(); } catch (Exception e) {} currencyView = null; }
        if (calendarView != null) { calendarView = null; }
        if (ocrView != null) { try { ocrView.release(); } catch (Exception e) {} ocrView = null; }
        if (mapView != null) { try { mapView.release(); } catch (Exception e) {} mapView = null; }
        if (sharedTypingView != null) { try { sharedTypingView.release(); } catch (Exception e) {} sharedTypingView = null; }
        
        // Crypto cleanup
        if (cryptoView != null) { cryptoView = null; }
        if (qrDisplayView != null) { 
            try { featureContainer.removeView(qrDisplayView); } catch (Exception e) {}
            qrDisplayView = null; 
        }
    }
    
    /**
     * QR kod overlay göster (anahtar paylaşımı için)
     */
    private void showQRCodeOverlay(String qrData) {
        try {
            if (featureContainer == null || qrData == null) {
                return;
            }
            
            // Eski QR view varsa kaldır
            if (qrDisplayView != null) {
                featureContainer.removeView(qrDisplayView);
            }
            
            // Yeni QR view oluştur
            qrDisplayView = new com.qrmaster.app.keyboard.views.QRDisplayView(this);
            qrDisplayView.setCallback(() -> {
                // Kapat butonuna basıldı
                if (featureContainer != null && qrDisplayView != null) {
                    featureContainer.removeView(qrDisplayView);
                    qrDisplayView = null;
                }
            });
            
            // QR kodu göster
            qrDisplayView.showQRCode(qrData);
            
            // Overlay olarak ekle (en üstte)
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            );
            featureContainer.addView(qrDisplayView, params);
            
            Log.d(TAG, "✅ QR kod overlay gösterildi");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ QR overlay error", e);
            Toast.makeText(this, "QR kod gösterilemedi", Toast.LENGTH_SHORT).show();
        }
    }

    private void cacheOriginalLabels() {
        try {
            originalLabels.clear();
            if (mainKeyboard == null) {
                Log.w(TAG, "⚠️ mainKeyboard null, cache yapılamıyor!");
                return;
            }
            List<Keyboard.Key> keys = mainKeyboard.getKeys();
            if (keys == null) {
                Log.w(TAG, "⚠️ keys null, cache yapılamıyor!");
                return;
            }
            for (Keyboard.Key key : keys) {
                if (key != null) {
                    originalLabels.add(key.label != null ? key.label.toString() : null);
                } else {
                    originalLabels.add(null);
                }
            }
            Log.d(TAG, "✅ Original labels cache yapıldı: " + originalLabels.size() + " tuş");
        } catch (Exception e) {
            Log.e(TAG, "❌ cacheOriginalLabels crash!", e);
            originalLabels.clear();
        }
    }

    /**
     * Shift tuşu handler - onPress/onRelease'de yönetiliyor
     * Artık bu metoda gerek yok, ancak uyumluluk için bırakıldı
     */
    private void handleShiftKey() {
        // Shift mantığı artık onPress/onRelease'de:
        // - Kısa basma: caps toggle (ilk harf büyük ↔ küçük)
        // - Uzun basma (350ms+): Caps Lock + titreşim
        Log.d(TAG, "handleShiftKey çağrıldı ama mantık onPress/onRelease'de");
    }
    
    /**
     * Shift state'e göre klavye tuş label'larını güncelle
     */
    private void updateKeyLabels() {
        try {
            if (mainKeyboard == null) {
                return;
            }
            
            List<Keyboard.Key> keys = mainKeyboard.getKeys();
            if (keys == null) {
                return;
            }
            
            // Shift state'i al
            boolean isShifted = caps || capsLock;
            
            for (Keyboard.Key key : keys) {
                if (key == null || key.codes == null || key.codes.length == 0) {
                    continue;
                }
                
                // Sadece harf tuşlarını güncelle
                int code = key.codes[0];
                
                // Küçük harf tuşları (a-z, ı, ğ, ü, ş, i, ö, ç)
                if (isShifted) {
                    // Büyük harfe çevir
                    switch (code) {
                        case 97: key.label = "A"; break;   // a -> A
                        case 98: key.label = "B"; break;   // b -> B
                        case 99: key.label = "C"; break;   // c -> C
                        case 100: key.label = "D"; break;  // d -> D
                        case 101: key.label = "E"; break;  // e -> E
                        case 102: key.label = "F"; break;  // f -> F
                        case 103: key.label = "G"; break;  // g -> G
                        case 104: key.label = "H"; break;  // h -> H
                        case 105: key.label = "İ"; break;  // i -> İ (Türkçe)
                        case 106: key.label = "J"; break;  // j -> J
                        case 107: key.label = "K"; break;  // k -> K
                        case 108: key.label = "L"; break;  // l -> L
                        case 109: key.label = "M"; break;  // m -> M
                        case 110: key.label = "N"; break;  // n -> N
                        case 111: key.label = "O"; break;  // o -> O
                        case 112: key.label = "P"; break;  // p -> P
                        case 113: key.label = "Q"; break;  // q -> Q
                        case 114: key.label = "R"; break;  // r -> R
                        case 115: key.label = "S"; break;  // s -> S
                        case 116: key.label = "T"; break;  // t -> T
                        case 117: key.label = "U"; break;  // u -> U
                        case 118: key.label = "V"; break;  // v -> V
                        case 119: key.label = "W"; break;  // w -> W
                        case 120: key.label = "X"; break;  // x -> X
                        case 121: key.label = "Y"; break;  // y -> Y
                        case 122: key.label = "Z"; break;  // z -> Z
                        case 305: key.label = "I"; break;  // ı -> I (Türkçe)
                        case 287: key.label = "Ğ"; break;  // ğ -> Ğ (Türkçe)
                        case 252: key.label = "Ü"; break;  // ü -> Ü (Türkçe)
                        case 351: key.label = "Ş"; break;  // ş -> Ş (Türkçe)
                        case 246: key.label = "Ö"; break;  // ö -> Ö (Türkçe)
                        case 231: key.label = "Ç"; break;  // ç -> Ç (Türkçe)
                    }
                } else {
                    // Küçük harfe çevir
                    switch (code) {
                        case 97: key.label = "a"; break;
                        case 98: key.label = "b"; break;
                        case 99: key.label = "c"; break;
                        case 100: key.label = "d"; break;
                        case 101: key.label = "e"; break;
                        case 102: key.label = "f"; break;
                        case 103: key.label = "g"; break;
                        case 104: key.label = "h"; break;
                        case 105: key.label = "i"; break;
                        case 106: key.label = "j"; break;
                        case 107: key.label = "k"; break;
                        case 108: key.label = "l"; break;
                        case 109: key.label = "m"; break;
                        case 110: key.label = "n"; break;
                        case 111: key.label = "o"; break;
                        case 112: key.label = "p"; break;
                        case 113: key.label = "q"; break;
                        case 114: key.label = "r"; break;
                        case 115: key.label = "s"; break;
                        case 116: key.label = "t"; break;
                        case 117: key.label = "u"; break;
                        case 118: key.label = "v"; break;
                        case 119: key.label = "w"; break;
                        case 120: key.label = "x"; break;
                        case 121: key.label = "y"; break;
                        case 122: key.label = "z"; break;
                        case 305: key.label = "ı"; break;  // Türkçe ı
                        case 287: key.label = "ğ"; break;  // Türkçe ğ
                        case 252: key.label = "ü"; break;  // Türkçe ü
                        case 351: key.label = "ş"; break;  // Türkçe ş
                        case 246: key.label = "ö"; break;  // Türkçe ö
                        case 231: key.label = "ç"; break;  // Türkçe ç
                    }
                }
            }
            
            // View'i yenile
            if (keyboardView != null) {
                keyboardView.invalidateAllKeys();
            }
            
            Log.d(TAG, "✅ Tuş label'ları güncellendi - isShifted: " + isShifted);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ updateKeyLabels hatası:", e);
        }
    }
    
    private boolean isTurkishChar(String s) {
        return s.equals("ı") || s.equals("İ") || s.equals("ğ") || s.equals("Ğ")
            || s.equals("ş") || s.equals("Ş") || s.equals("ç") || s.equals("Ç")
            || s.equals("ö") || s.equals("Ö") || s.equals("ü") || s.equals("Ü");
    }

    private void playClick(int primaryCode) {
        // Vibration
        if (vibrateOnPress) {
            try {
                android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(
                            25, // 25ms - kısa ve keskin
                            android.os.VibrationEffect.DEFAULT_AMPLITUDE
                        ));
                    } else {
                        vibrator.vibrate(25);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Titreşim hatası: " + e.getMessage());
            }
        }
        
        // Sound
        if (soundOnPress) {
            try {
                android.media.AudioManager am = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    // Sistem tuş sesi çal
                    am.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_STANDARD);
                }
            } catch (Exception e) {
                Log.e(TAG, "Ses hatası: " + e.getMessage());
            }
        }
    }
    
    /**
     * Inline voice recording - toolbar içinde çalışır
     */
    private void toggleVoiceRecording(View voiceButton) {
        currentVoiceButton = voiceButton;
        
        if (isInlineVoiceRecording) {
            stopInlineVoiceRecording();
        } else {
            startInlineVoiceRecording();
        }
    }
    
    private void startInlineVoiceRecording() {
        if (inlineSpeechRecognizer != null) {
            try {
                inlineSpeechRecognizer.destroy();
            } catch (Exception e) {
                // ignore
            }
            inlineSpeechRecognizer = null;
        }
        
        try {
            if (!android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
                Toast.makeText(this, "Bu cihazda desteklenmiyor", Toast.LENGTH_SHORT).show();
                return;
            }
            
            inlineSpeechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this);
            if (inlineSpeechRecognizer == null) {
                Toast.makeText(this, "Başlatılamadı", Toast.LENGTH_SHORT).show();
                return;
            }
            
            inlineSpeechRecognizer.setRecognitionListener(new android.speech.RecognitionListener() {
                @Override
                public void onReadyForSpeech(android.os.Bundle params) {
                    isInlineVoiceRecording = true;
                    updateVoiceButtonState(true);
                }
                
                @Override
                public void onBeginningOfSpeech() {}
                
                @Override
                public void onRmsChanged(float rmsdB) {}
                
                @Override
                public void onBufferReceived(byte[] buffer) {}
                
                @Override
                public void onEndOfSpeech() {}
                
                @Override
                public void onError(int error) {
                    stopInlineVoiceRecording();
                    if (error == android.speech.SpeechRecognizer.ERROR_NO_MATCH) {
                        Toast.makeText(TurkishKeyboardService.this, "Ses algılanamadı", Toast.LENGTH_SHORT).show();
                    }
                }
                
                @Override
                public void onResults(android.os.Bundle results) {
                    java.util.ArrayList<String> matches = results.getStringArrayList(
                        android.speech.SpeechRecognizer.RESULTS_RECOGNITION
                    );
                    
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        android.view.inputmethod.InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            ic.commitText(text + " ", 1);
                        }
                    }
                    
                    stopInlineVoiceRecording();
                }
                
                @Override
                public void onPartialResults(android.os.Bundle partialResults) {}
                
                @Override
                public void onEvent(int eventType, android.os.Bundle params) {}
            });
            
            android.content.Intent intent = new android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            
            inlineSpeechRecognizer.startListening(intent);
            
        } catch (Exception e) {
            Log.e(TAG, "Voice recording error: " + e.getMessage());
            Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            stopInlineVoiceRecording();
        }
    }
    
    private void stopInlineVoiceRecording() {
        isInlineVoiceRecording = false;
        updateVoiceButtonState(false);
        
        if (inlineSpeechRecognizer != null) {
            try {
                inlineSpeechRecognizer.stopListening();
                inlineSpeechRecognizer.destroy();
            } catch (Exception e) {
                Log.e(TAG, "Stop recording error: " + e.getMessage());
            }
            inlineSpeechRecognizer = null;
        }
    }
    
    private void updateVoiceButtonState(boolean recording) {
        if (currentVoiceButton == null) return;
        
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            try {
                String theme = prefs.getString("theme", "dark");
                int[] colors;
                
                if (recording) {
                    // Kırmızı - Dinliyor
                    colors = new int[]{0xFFFF3B30, 0xFFFF6B60};
                } else {
                    // Normal tema rengi
                    switch (theme) {
                        case "dynamic":
                            int accent = getDynamicAccentColor();
                            colors = new int[]{blend(accent, 0xFF000000, 0.35f), accent};
                            break;
                        case "light":
                            colors = new int[]{0xFFF0F0F0, 0xFFE0E0E0};
                            break;
                        default:
                            colors = new int[]{0xFF4B5563, 0xFF374151};
                            break;
                    }
                }
                
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                    colors
                );
                bg.setCornerRadius(dp(10));
                bg.setStroke(dp(1), recording ? 0xFFFF3B30 : 0xFF6B7280);
                
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    android.graphics.drawable.RippleDrawable ripple =
                        new android.graphics.drawable.RippleDrawable(
                            android.content.res.ColorStateList.valueOf(0x22000000), bg, null);
                    currentVoiceButton.setBackground(ripple);
                } else {
                    currentVoiceButton.setBackground(bg);
                }
            } catch (Exception e) {
                Log.e(TAG, "Voice button update error: " + e.getMessage());
            }
        });
    }

    private void loadPreferences() {
        vibrateOnPress = prefs.getBoolean("vibrate_on_press", true);
        soundOnPress = prefs.getBoolean("sound_on_press", false);
        currentTheme = prefs.getString("theme", "dark");
        floatingEnabled = prefs.getBoolean("floating_mode", false);
        try {
            oneHandMode = OneHandMode.valueOf(prefs.getString("one_hand_mode", OneHandMode.CENTER.name()));
        } catch (Exception e) {
            oneHandMode = OneHandMode.CENTER;
        }
    }

    private void applyTheme() {
        String theme = prefs.getString("theme", "dark");
        
        // 1. KLAVYE ARKAPLAN - Check for custom photo
        Drawable background;
        if ("custom_photo".equals(theme)) {
            String photoPath = prefs.getString("custom_background_path", "");
            if (!photoPath.isEmpty()) {
                try {
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(photoPath);
                    if (bitmap != null) {
                        android.graphics.drawable.BitmapDrawable photoDrawable = 
                            new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
                        photoDrawable.setAlpha(200);  // Semi-transparent
                        background = photoDrawable;
                        Log.d(TAG, "✅ Özel fotoğraf arka plan uygulandı");
                    } else {
                        background = createThemeBackground(false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Fotoğraf yüklenemedi", e);
                    background = createThemeBackground(false);
                }
            } else {
                background = createThemeBackground(false);
            }
        } else {
            background = createThemeBackground(false);
        }
        
        if (rootInputView != null) {
            rootInputView.setBackground(background);
        }
        if (featureContainer != null) {
            featureContainer.setBackground(background);
        }
        if (keyboardContainer != null) {
            if (floatingEnabled) {
                keyboardContainer.setBackground(createThemeBackground(true));
                keyboardContainer.setElevation(dp(8));
            } else {
                keyboardContainer.setBackground(background);
                keyboardContainer.setElevation(0);
            }
            // Modern blur efekti (Android 12+) - glass/floating için
            try {
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    boolean enableBlur = floatingEnabled || "glassmorphism".equals(theme);
                    if (enableBlur) {
                        keyboardContainer.setRenderEffect(
                            android.graphics.RenderEffect.createBlurEffect(12f, 12f, android.graphics.Shader.TileMode.CLAMP)
                        );
                    } else {
                        keyboardContainer.setRenderEffect(null);
                    }
                }
            } catch (Throwable ignored) { }
        }
        
        // 2. TÜM TUŞLARIN RENKLERİNİ DEĞİŞTİR!!! 🎨🎨🎨
        if (keyboardView != null) {
            try {
                // Her tuş için yeni drawable oluştur
                Drawable keyDrawable = createKeyBackgroundForTheme(theme);
                
                // Ripple ile zenginleştir (API 21+)
                try {
                    if (android.os.Build.VERSION.SDK_INT >= 21) {
                        int ripple = (
                            theme.equals("light") || theme.equals("pastel") || theme.equals("paper") ||
                            theme.equals("gruvbox_light") || theme.equals("nord_light") || theme.equals("solarized_light") ||
                            theme.equals("catppuccin_latte") || theme.equals("everforest_light") || theme.equals("sakura") || theme.equals("mint")
                        ) ? 0x22000000 : 0x22FFFFFF;
                        android.content.res.ColorStateList csl = android.content.res.ColorStateList.valueOf(ripple);
                        android.graphics.drawable.RippleDrawable rippleDrawable =
                            new android.graphics.drawable.RippleDrawable(csl, keyDrawable, null);
                        keyDrawable = rippleDrawable;
                    }
                } catch (Throwable ignored) { }
                
                // Reflection ile KeyboardView'ın mKeyBackground field'ını değiştir
                try {
                    java.lang.reflect.Field keyBgField = KeyboardView.class.getDeclaredField("mKeyBackground");
                    keyBgField.setAccessible(true);
                    keyBgField.set(keyboardView, keyDrawable);
                    Log.d(TAG, "✅ Klavye tuş arka planı reflection ile değiştirildi");
                } catch (Exception reflectionError) {
                    Log.w(TAG, "⚠️ Reflection başarısız, alternatif yöntem deneniyor...");
                }
                
                // TEXT RENKLERİNİ DEĞİŞTİR - ThemeStyleConfig kullan
                ThemeStyleConfig styleConfig = new ThemeStyleConfig(this);
                
                // Get background color from theme for auto mode
                int backgroundColor = getThemeBackgroundColor(theme);
                int accentColor = getThemeAccentColor(theme);
                
                int textColor = styleConfig.getTextColor(backgroundColor, accentColor);
                
                // YÖNTEM 1: mKeyTextColor field
                try {
                    java.lang.reflect.Field textColorField = KeyboardView.class.getDeclaredField("mKeyTextColor");
                    textColorField.setAccessible(true);
                    textColorField.set(keyboardView, textColor);
                    Log.d(TAG, "✅ Method 1: mKeyTextColor değiştirildi");
                } catch (Exception e1) {
                    Log.w(TAG, "⚠️ Method 1 başarısız: " + e1.getMessage());
                }
                
                // YÖNTEM 2: mPaint.setColor() - Daha agresif
                try {
                    java.lang.reflect.Field paintField = KeyboardView.class.getDeclaredField("mPaint");
                    paintField.setAccessible(true);
                    android.graphics.Paint paint = (android.graphics.Paint) paintField.get(keyboardView);
                    if (paint != null) {
                        paint.setColor(textColor);
                        paint.setTextSize(dp(22)); // Text boyutu da ayarla
                        Log.d(TAG, "✅ Method 2: mPaint.setColor() değiştirildi");
                    }
                } catch (Exception e2) {
                    Log.w(TAG, "⚠️ Method 2 başarısız: " + e2.getMessage());
                }
                
                // YÖNTEM 3: setKeyTextColor() metodu varsa çağır
                try {
                    java.lang.reflect.Method setColorMethod = KeyboardView.class.getMethod("setKeyTextColor", int.class);
                    setColorMethod.invoke(keyboardView, textColor);
                    Log.d(TAG, "✅ Method 3: setKeyTextColor() çağrıldı");
                } catch (Exception e3) {
                    // Normal, bu metod yoksa sorun yok
                }
                
                Log.d(TAG, "✅ Klavye text rengi: " + (theme.equals("light") ? "SİYAH (0xFF000000)" : "BEYAZ (0xFFFFFFFF)"));
                
                // KeyboardView'ı yenile
                if (mainKeyboard != null) {
                    keyboardView.setKeyboard(mainKeyboard);
                }
                keyboardView.invalidateAllKeys();
                keyboardView.invalidate();
                
                Log.d(TAG, "✅ Klavye tuşları tema rengine boyandı: " + theme);
            } catch (Exception e) {
                Log.e(TAG, "❌ Klavye tuş rengi hatası", e);
            }
        }
        
        // 3. TOOLBAR BUTONLARINI RENKLENDİR
        applyThemeToToolbar();
        
        // 4. Aday şerit (candidate strip) modern arka plan
        try {
            if (candidateStripView != null) {
                GradientDrawable stripBg = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0x22111111, 0x11000000}
                );
                stripBg.setCornerRadius(dp(12));
                candidateStripView.setBackground(stripBg);
            }
        } catch (Throwable ignored) { }
        
        // 5. 🎨 FORCE REFRESH - Apply all changes
        forceRefreshKeyboard();
        
        Log.d(TAG, "✅ applyTheme() tamamlandı - Theme: " + theme);
    }
    
    /**
     * 🔄 Force refresh keyboard with all theme changes
     * ULTRA AGGRESSIVE - Her şeyi sıfırdan yarat
     */
    private void forceRefreshKeyboard() {
        try {
            Log.d(TAG, "🔄 ULTRA AGGRESSIVE Force refresh başlıyor...");
            
            if (keyboardView != null && mainKeyboard != null) {
                String theme = prefs.getString("theme", "dark");
                
                // 1. FORCE RE-CREATE KEY DRAWABLE
                Drawable keyDrawable = createKeyBackgroundForTheme(theme);
                
                // 2. AGGRESSIVE REFLECTION - TÜM FIELD'LARI GÜNCELLe
                try {
                    // mKeyBackground
                    java.lang.reflect.Field keyBgField = KeyboardView.class.getDeclaredField("mKeyBackground");
                    keyBgField.setAccessible(true);
                    keyBgField.set(keyboardView, keyDrawable);
                    Log.d(TAG, "✅ mKeyBackground set");
                    
                    // mKeyTextColor
                    ThemeStyleConfig styleConfig = new ThemeStyleConfig(this);
                    int backgroundColor = getThemeBackgroundColor(theme);
                    int accentColor = getThemeAccentColor(theme);
                    int textColor = styleConfig.getTextColor(backgroundColor, accentColor);
                    
                    try {
                        java.lang.reflect.Field textColorField = KeyboardView.class.getDeclaredField("mKeyTextColor");
                        textColorField.setAccessible(true);
                        textColorField.set(keyboardView, textColor);
                        Log.d(TAG, "✅ mKeyTextColor set: " + Integer.toHexString(textColor));
                    } catch (Exception e) {
                        Log.w(TAG, "⚠️ mKeyTextColor field not found");
                    }
                    
                    // mPaint - Direct paint update
                    try {
                        java.lang.reflect.Field paintField = KeyboardView.class.getDeclaredField("mPaint");
                        paintField.setAccessible(true);
                        android.graphics.Paint paint = (android.graphics.Paint) paintField.get(keyboardView);
                        if (paint != null) {
                            paint.setColor(textColor);
                            paint.setTextSize(dp(18)); // Ensure text size
                            Log.d(TAG, "✅ mPaint color set");
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "⚠️ mPaint field error");
                    }
                    
                    // mShadowRadius, mShadowColor - Text shadow
                    try {
                        java.lang.reflect.Field shadowRadiusField = KeyboardView.class.getDeclaredField("mShadowRadius");
                        shadowRadiusField.setAccessible(true);
                        shadowRadiusField.set(keyboardView, 0f);
                    } catch (Exception ignored) {}
                    
                } catch (Exception e) {
                    Log.e(TAG, "❌ Reflection error: " + e.getMessage(), e);
                }
                
                // 3. INVALIDATE EVERYTHING (DON'T NULL THE KEYBOARD!)
                keyboardView.invalidateAllKeys();
                keyboardView.invalidate();
                keyboardView.postInvalidate();
                keyboardView.requestLayout();
            }
            
            // 5. REFRESH ALL CONTAINERS
            if (rootInputView != null) {
                rootInputView.setBackground(createThemeBackground(false));
                rootInputView.invalidate();
                rootInputView.postInvalidate();
                rootInputView.requestLayout();
            }
            
            if (keyboardContainer != null) {
                keyboardContainer.setBackground(createThemeBackground(floatingEnabled));
                keyboardContainer.invalidate();
                keyboardContainer.postInvalidate();
                keyboardContainer.requestLayout();
            }
            
            if (featureContainer != null) {
                featureContainer.setBackground(createThemeBackground(false));
                featureContainer.invalidate();
                featureContainer.postInvalidate();
            }
            
            Log.d(TAG, "✅ ULTRA Force refresh tamamlandı");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Force refresh hatası", e);
            Toast.makeText(this, "❌ Tema hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 🎨 Toolbar butonlarını temaya göre renklendir - OTOMATIK & AKILLI
     */
    private void applyThemeToToolbar() {
        if (toolbarView == null) return;
        
        String theme = prefs.getString("theme", "dark");
        
        // ThemeStyleConfig ile text rengi belirleme
        ThemeStyleConfig styleConfig = new ThemeStyleConfig(this);
        int backgroundColor = getThemeBackgroundColor(theme);
        int accentColor = getThemeAccentColor(theme);
        int textColor = styleConfig.getTextColor(backgroundColor, accentColor);
        
        // 🎨 OTOMATIK TEMA RENK UYGULAMASI
        // createKeyBackgroundForTheme kullanarak aynı renkleri toolbar'a uygula
        int[] toolbarColors = new int[]{backgroundColor, blend(backgroundColor, accentColor, 0.3f)};
        int toolbarStroke = accentColor;
        
        // ESKI MANUEL SWITCH SİLİNDİ - ARTIK OTOMATIK!
        // Şimdi her temayı destekliyor otomatik olarak
        
        /* ESKI KOD - ARTIK GEREKSIZ
        switch (theme) {
            case "dynamic":
                int accent = getDynamicAccentColor();
                toolbarColors = new int[]{blend(accent, 0xFF000000, 0.35f), accent};
                toolbarStroke = blend(accent, 0xFFFFFFFF, 0.35f);
                break;
            case "light":
                toolbarColors = new int[]{0xFFF0F0F0, 0xFFE0E0E0};
                toolbarStroke = 0xFFD0D0D0;
                break;
            case "nord":
                toolbarColors = new int[]{0xFF4C566A, 0xFF3B4252};
                toolbarStroke = 0xFF88C0D0;
                break;
            case "dracula":
                toolbarColors = new int[]{0xFF44475A, 0xFF6272A4};
                toolbarStroke = 0xFFBD93F9;
                break;
            case "monokai":
                toolbarColors = new int[]{0xFF3E3D32, 0xFF49483E};
                toolbarStroke = 0xFFFD971F;
                break;
            case "solarized":
                toolbarColors = new int[]{0xFF073642, 0xFF586E75};
                toolbarStroke = 0xFF268BD2;
                break;
            case "gruvbox":
                toolbarColors = new int[]{0xFF3C3836, 0xFF504945};
                toolbarStroke = 0xFFB8BB26;
                break;
            case "cyberpunk":
                toolbarColors = new int[]{0xFF1A1A2E, 0xFF16213E};
                toolbarStroke = 0xFF00FFF0;
                break;
            case "tokyo":
                toolbarColors = new int[]{0xFF24283B, 0xFF414868};
                toolbarStroke = 0xFFBB9AF7;
                break;
            case "atom":
                toolbarColors = new int[]{0xFF2C323C, 0xFF3E4451};
                toolbarStroke = 0xFF61AFEF;
                break;
            case "material":
                toolbarColors = new int[]{0xFF37474F, 0xFF455A64};
                toolbarStroke = 0xFF80CBC4;
                break;
            case "palenight":
                toolbarColors = new int[]{0xFF34324A, 0xFF444267};
                toolbarStroke = 0xFFC792EA;
                break;
            case "owl":
                toolbarColors = new int[]{0xFF0B2942, 0xFF1D3B53};
                toolbarStroke = 0xFF82AAFF;
                break;
            case "espresso":
                toolbarColors = new int[]{0xFF3E2A0F, 0xFF5C4520};
                toolbarStroke = 0xFFD4A574;
                break;
            case "synthwave":
                toolbarColors = new int[]{0xFF2B213A, 0xFF47315B};
                toolbarStroke = 0xFFFF6AD5;
                break;
            case "neon":
                toolbarColors = new int[]{0xFF330066, 0xFF660099};
                toolbarStroke = 0xFFFF00FF;
                break;
            case "aurora":
                toolbarColors = new int[]{0xFF003D5C, 0xFF00667F};
                toolbarStroke = 0xFF00FFAA;
                break;
            case "sunset":
                toolbarColors = new int[]{0xFF8B2635, 0xFFFF6B35};
                toolbarStroke = 0xFFFFAA00;
                break;
            case "ocean":
                toolbarColors = new int[]{0xFF003D5C, 0xFF006994};
                toolbarStroke = 0xFF0099CC;
                break;
            case "forest":
                toolbarColors = new int[]{0xFF1A3D16, 0xFF2D5016};
                toolbarStroke = 0xFF4A7C2E;
                break;
            case "galaxy":
                toolbarColors = new int[]{0xFF1A1A40, 0xFF2D2D5C};
                toolbarStroke = 0xFF6666CC;
                break;
            case "neomorphism":
                toolbarColors = new int[]{0xFFE0E5EC, 0xFFD1D9E6};
                toolbarStroke = 0xFFFFFFFF;
                break;
            case "glassmorphism":
                toolbarColors = new int[]{0x88FFFFFF, 0x66DDDDDD};
                toolbarStroke = 0xCCFFFFFF;
                break;
            case "hacker":
                toolbarColors = new int[]{0xFF001100, 0xFF002200};
                toolbarStroke = 0xFF00FF00;
                break;
            case "rosegold":
                toolbarColors = new int[]{0xFF7D4F59, 0xFFB76E79};
                toolbarStroke = 0xFFE8B4B8;
                break;
            case "midnight":
                toolbarColors = new int[]{0xFF162B4D, 0xFF1E3A5F};
                toolbarStroke = 0xFF4A7FAA;
                break;
            case "lava":
                toolbarColors = new int[]{0xFF4A0000, 0xFF8B0000};
                toolbarStroke = 0xFFFF4400;
                break;
            case "ice":
                toolbarColors = new int[]{0xFF7FB3F4, 0xFF5A9DEB};
                toolbarStroke = 0xFF9FCCFA;
                break;
            case "amoled":
                toolbarColors = new int[]{0xFF000000, 0xFF0A0A0A};
                toolbarStroke = 0xFF222222;
                break;
            case "pastel":
                toolbarColors = new int[]{0xFFFFE3EC, 0xFFF6C1D0};
                toolbarStroke = 0xFFE6A8B8;
                break;
            case "materialyou":
                toolbarColors = new int[]{0xFF7F39FB, 0xFF9E6CFF};
                toolbarStroke = 0xFFBB86FC;
                break;
            case "rainbow":
                toolbarColors = new int[]{0xFFFFCA3A, 0xFF1982C4};
                toolbarStroke = 0xFFFFFFFF;
                break;
            case "metal":
                toolbarColors = new int[]{0xFF606060, 0xFF8E8E8E};
                toolbarStroke = 0xFFCCCCCC;
                break;
            case "paper":
                toolbarColors = new int[]{0xFFF9F7F2, 0xFFF4F1EA};
                toolbarStroke = 0xFFD5D1C6;
                break;
            case "clay":
                toolbarColors = new int[]{0xFFEADBC8, 0xFFD8C3A5};
                toolbarStroke = 0xFFB89B7A;
                break;
            case "catppuccin_latte":
                toolbarColors = new int[]{0xFFF2D5CF, 0xFFEBD0CA};
                toolbarStroke = 0xFFDC8A78;
                break;
            case "catppuccin_frappe":
                toolbarColors = new int[]{0xFF303446, 0xFF414559};
                toolbarStroke = 0xFF8CAAEE;
                break;
            case "catppuccin_macchiato":
                toolbarColors = new int[]{0xFF24273A, 0xFF363A4F};
                toolbarStroke = 0xFF8AADF4;
                break;
            case "catppuccin_mocha":
                toolbarColors = new int[]{0xFF1E1E2E, 0xFF313244};
                toolbarStroke = 0xFF89B4FA;
                break;
            case "everforest_dark":
                toolbarColors = new int[]{0xFF2D353B, 0xFF343F44};
                toolbarStroke = 0xFFA7C080;
                break;
            case "everforest_light":
                toolbarColors = new int[]{0xFFECE3CC, 0xFFE6DDBF};
                toolbarStroke = 0xFF5C6A72;
                break;
            case "kanagawa":
                toolbarColors = new int[]{0xFF1F1F28, 0xFF223249};
                toolbarStroke = 0xFF7E9CD8;
                break;
            case "onedarkpro":
                toolbarColors = new int[]{0xFF282C34, 0xFF30343C};
                toolbarStroke = 0xFF61AFEF;
                break;
            case "gruvbox_light":
                toolbarColors = new int[]{0xFFFBF1C7, 0xFFF2E5BC};
                toolbarStroke = 0xFF928374;
                break;
            case "nord_light":
                toolbarColors = new int[]{0xFFE5E9F0, 0xFFD8DEE9};
                toolbarStroke = 0xFF81A1C1;
                break;
            case "solarized_light":
                toolbarColors = new int[]{0xFFEEE8D5, 0xFFECE3CA};
                toolbarStroke = 0xFF586E75;
                break;
            case "sakura":
                toolbarColors = new int[]{0xFFFFEEF1, 0xFFFFD9E3};
                toolbarStroke = 0xFFFFB7C5;
                break;
            case "high_contrast":
                toolbarColors = new int[]{0xFF000000, 0xFF000000};
                toolbarStroke = 0xFFFFFFFF;
                break;
            case "mint":
                toolbarColors = new int[]{0xFFE8FFF3, 0xFFD9FFF0};
                toolbarStroke = 0xFFA8E6CF;
                break;
            case "mesh":
                toolbarColors = new int[]{0xFF7F00FF, 0xFFFF00FF};
                toolbarStroke = 0x88FFFFFF;
                break;
            case "holo":
                toolbarColors = new int[]{0xFF30D5C8, 0xFF20B2AA};
                toolbarStroke = 0x8820B2AA;
                break;
            case "aurora_plus":
                toolbarColors = new int[]{0xFF1C2541, 0xFF5BC0BE};
                toolbarStroke = 0xFF5BC0BE;
                break;
            case "candy":
                toolbarColors = new int[]{0xFFFF80AB, 0xFFFF4081};
                toolbarStroke = 0xFFFFFFFF;
                break;
            case "gold":
                toolbarColors = new int[]{0xFFFFD700, 0xFFDAA520};
                toolbarStroke = 0xFFEEDC82;
                break;
            case "carbon":
                toolbarColors = new int[]{0xFF2B2B2B, 0xFF3A3A3A};
                toolbarStroke = 0xFF555555;
                break;
            case "slate":
                toolbarColors = new int[]{0xFF2F4F4F, 0xFF708090};
                toolbarStroke = 0xFFB0C4DE;
                break;
            case "midnight_purple":
                toolbarColors = new int[]{0xFF4B0082, 0xFF6A0DAD};
                toolbarStroke = 0xFFBB86FC;
                break;
            case "coral":
                toolbarColors = new int[]{0xFFFF7F50, 0xFFFF6B6B};
                toolbarStroke = 0xFFFFC1A1;
                break;
            case "sunsea":
                toolbarColors = new int[]{0xFFFFA500, 0xFF006994};
                toolbarStroke = 0xFFFFFFFF;
                break;
            ... (all cases removed)
        }
        */ // ESKI KOD SONU
        
        // Toolbar container arka planını değiştir
        View toolbarContainer = toolbarView.findViewById(com.qrmaster.app.R.id.toolbar);
        if (toolbarContainer != null) {
            GradientDrawable toolbarBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                toolbarColors
            );
            toolbarContainer.setBackground(toolbarBg);
        }
        
        // Tüm toolbar butonlarını değiştir (Toggle butonları dahil)
        int[] buttonIds = {
            com.qrmaster.app.R.id.btn_toggle_suggestions,
            com.qrmaster.app.R.id.btn_toggle_toolbar,
            com.qrmaster.app.R.id.btn_quick_menu,
            com.qrmaster.app.R.id.btn_emoji,
            com.qrmaster.app.R.id.btn_camera,
            com.qrmaster.app.R.id.btn_voice,
            com.qrmaster.app.R.id.btn_gif,
            com.qrmaster.app.R.id.btn_translate,
            com.qrmaster.app.R.id.btn_clipboard,
            com.qrmaster.app.R.id.btn_mini_apps,
            com.qrmaster.app.R.id.btn_settings
        };
        
        for (int buttonId : buttonIds) {
            View button = toolbarView.findViewById(buttonId);
            if (button != null) {
                GradientDrawable buttonBg = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    toolbarColors
                );
                buttonBg.setCornerRadius(dp(10));
                buttonBg.setStroke(dp(1), toolbarStroke);
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    android.graphics.drawable.RippleDrawable ripple =
                        new android.graphics.drawable.RippleDrawable(
                            android.content.res.ColorStateList.valueOf(0x22000000), buttonBg, null);
                    button.setBackground(ripple);
                } else {
                    button.setBackground(buttonBg);
                }
                
                // 🎨 TEXT RENGİNİ AYARLA - Otomatik akıllı
                if (button instanceof android.widget.TextView) {
                    ((android.widget.TextView) button).setTextColor(textColor);
                } else if (button instanceof android.widget.ImageView) {
                    // Icon tint
                    ((android.widget.ImageView) button).setColorFilter(textColor);
                }
            }
        }
        
        Log.d(TAG, "✅ Toolbar OTOMATIK tema renklerine boyandı: " + theme + 
            " | Text: " + Integer.toHexString(textColor) + 
            " | BG: " + Integer.toHexString(backgroundColor) + 
            " | Accent: " + Integer.toHexString(accentColor));
    }
    
    /**
     * GERÇEK KLAVYE TEMASI - Her tema için tuş renklerini oluştur
     * ✨ ThemeStyleConfig entegrasyonu ile tam özelleştirilebilir
     */
    private Drawable createKeyBackgroundForTheme(String theme) {
        // Load style config
        ThemeStyleConfig styleConfig = new ThemeStyleConfig(this);
        
        // Her tema için tuş renkleri
        int[] normalColors;
        int[] pressedColors;
        int strokeColor;
        int highlightColor;
        
        Log.d(TAG, "🎨 createKeyBackgroundForTheme() - Theme: " + theme);
        
        switch (theme) {
            case "dynamic": {
                int accent = getDynamicAccentColor();
                int darker = blend(accent, 0xFF000000, 0.4f);
                int lighter = blend(accent, 0xFFFFFFFF, 0.3f);
                normalColors = new int[]{lighter, accent, darker};
                pressedColors = new int[]{blend(accent, 0xFFFFFFFF, 0.15f), accent};
                strokeColor = blend(accent, 0xFF000000, 0.2f);
                highlightColor = 0x40FFFFFF;
                Log.d(TAG, "✅ Dynamic colors: " + Integer.toHexString(accent));
                break;
            }
            case "light":
                normalColors = new int[]{0xFFFFFFFF, 0xFFF0F0F0, 0xFFE5E5E5};
                pressedColors = new int[]{0xFFE0E0E0, 0xFFD0D0D0};
                strokeColor = 0xFFC0C0C0;
                highlightColor = 0x40FFFFFF;
                break;
                
            case "nord":
                normalColors = new int[]{0xFF4C566A, 0xFF434C5E, 0xFF3B4252};
                pressedColors = new int[]{0xFF5E81AC, 0xFF4C566A};
                strokeColor = 0xFF88C0D0;
                highlightColor = 0x4088C0D0;
                break;
                
            case "dracula":
                normalColors = new int[]{0xFF44475A, 0xFF6272A4, 0xFF44475A};
                pressedColors = new int[]{0xFF6272A4, 0xFF44475A};
                strokeColor = 0xFFBD93F9;
                highlightColor = 0x40BD93F9;
                break;
                
            case "monokai":
                normalColors = new int[]{0xFF3E3D32, 0xFF49483E, 0xFF3E3D32};
                pressedColors = new int[]{0xFF75715E, 0xFF3E3D32};
                strokeColor = 0xFFFD971F;
                highlightColor = 0x40FD971F;
                break;
                
            case "solarized":
                normalColors = new int[]{0xFF073642, 0xFF586E75, 0xFF073642};
                pressedColors = new int[]{0xFF586E75, 0xFF073642};
                strokeColor = 0xFF268BD2;
                highlightColor = 0x40268BD2;
                break;
                
            case "gruvbox":
                normalColors = new int[]{0xFF3C3836, 0xFF504945, 0xFF3C3836};
                pressedColors = new int[]{0xFF665C54, 0xFF3C3836};
                strokeColor = 0xFFB8BB26;
                highlightColor = 0x40B8BB26;
                break;
                
            case "cyberpunk":
                normalColors = new int[]{0xFF1A1A2E, 0xFF16213E, 0xFF0F1419};
                pressedColors = new int[]{0xFF00D9FF, 0xFF1A1A2E};
                strokeColor = 0xFF00FFF0;
                highlightColor = 0x4000FFF0;
                break;
                
            case "tokyo":
                normalColors = new int[]{0xFF24283B, 0xFF414868, 0xFF24283B};
                pressedColors = new int[]{0xFF7AA2F7, 0xFF24283B};
                strokeColor = 0xFFBB9AF7;
                highlightColor = 0x40BB9AF7;
                break;
                
            case "atom":
                normalColors = new int[]{0xFF2C323C, 0xFF3E4451, 0xFF2C323C};
                pressedColors = new int[]{0xFF528BFF, 0xFF2C323C};
                strokeColor = 0xFF61AFEF;
                highlightColor = 0x4061AFEF;
                break;
                
            case "material":
                normalColors = new int[]{0xFF37474F, 0xFF455A64, 0xFF37474F};
                pressedColors = new int[]{0xFF546E7A, 0xFF37474F};
                strokeColor = 0xFF80CBC4;
                highlightColor = 0x4080CBC4;
                break;
                
            case "palenight":
                normalColors = new int[]{0xFF34324A, 0xFF444267, 0xFF34324A};
                pressedColors = new int[]{0xFF676E95, 0xFF34324A};
                strokeColor = 0xFFC792EA;
                highlightColor = 0x40C792EA;
                break;
                
            case "owl":
                normalColors = new int[]{0xFF0B2942, 0xFF1D3B53, 0xFF0B2942};
                pressedColors = new int[]{0xFF234D70, 0xFF0B2942};
                strokeColor = 0xFF82AAFF;
                highlightColor = 0x4082AAFF;
                break;
                
            case "espresso":
                normalColors = new int[]{0xFF3E2A0F, 0xFF5C4520, 0xFF3E2A0F};
                pressedColors = new int[]{0xFF7D5A3A, 0xFF3E2A0F};
                strokeColor = 0xFFD4A574;
                highlightColor = 0x40D4A574;
                break;
                
            case "synthwave":
                normalColors = new int[]{0xFF2B213A, 0xFF47315B, 0xFF2B213A};
                pressedColors = new int[]{0xFF72507C, 0xFF2B213A};
                strokeColor = 0xFFFF6AD5;
                highlightColor = 0x40FF6AD5;
                break;
            
            // ✨ YENİ MODERN TEMALAR ✨
            
            case "neon":
                normalColors = new int[]{0xFF330066, 0xFF660099, 0xFF330066};
                pressedColors = new int[]{0xFFFF00FF, 0xFF660099};
                strokeColor = 0xFFFF00FF; // Neon pembe
                highlightColor = 0x60FF00FF;
                break;
            
            case "aurora":
                normalColors = new int[]{0xFF003D5C, 0xFF00667F, 0xFF003D5C};
                pressedColors = new int[]{0xFF00FFAA, 0xFF00667F};
                strokeColor = 0xFF00FFAA;
                highlightColor = 0x4000FFAA;
                break;
            
            case "sunset":
                normalColors = new int[]{0xFF8B2635, 0xFFFF6B35, 0xFF8B2635};
                pressedColors = new int[]{0xFFFFAA00, 0xFF8B2635};
                strokeColor = 0xFFFF6B35;
                highlightColor = 0x40FF6B35;
                break;
            
            case "ocean":
                normalColors = new int[]{0xFF003D5C, 0xFF006994, 0xFF003D5C};
                pressedColors = new int[]{0xFF0099CC, 0xFF006994};
                strokeColor = 0xFF0099CC;
                highlightColor = 0x400099CC;
                break;
            
            case "forest":
                normalColors = new int[]{0xFF1A3D16, 0xFF2D5016, 0xFF1A3D16};
                pressedColors = new int[]{0xFF4A7C2E, 0xFF2D5016};
                strokeColor = 0xFF4A7C2E;
                highlightColor = 0x404A7C2E;
                break;
            
            case "galaxy":
                normalColors = new int[]{0xFF1A1A40, 0xFF2D2D5C, 0xFF1A1A40};
                pressedColors = new int[]{0xFF6666CC, 0xFF2D2D5C};
                strokeColor = 0xFF6666CC;
                highlightColor = 0x406666CC;
                break;
            
            case "neomorphism":
                normalColors = new int[]{0xFFE0E5EC, 0xFFD1D9E6, 0xFFE0E5EC};
                pressedColors = new int[]{0xFFC1C9D6, 0xFFD1D9E6};
                strokeColor = 0xFFFFFFFF;
                highlightColor = 0x60FFFFFF;
                break;
            
            case "glassmorphism":
                normalColors = new int[]{0x88FFFFFF, 0x66DDDDDD, 0x88FFFFFF};
                pressedColors = new int[]{0xAAFFFFFF, 0x88DDDDDD};
                strokeColor = 0xCCFFFFFF;
                highlightColor = 0x80FFFFFF;
                break;
            
            case "hacker":
                normalColors = new int[]{0xFF001100, 0xFF002200, 0xFF001100};
                pressedColors = new int[]{0xFF004400, 0xFF002200};
                strokeColor = 0xFF00FF00; // Matrix yeşil
                highlightColor = 0x6000FF00;
                break;
            
            case "rosegold":
                normalColors = new int[]{0xFF7D4F59, 0xFFB76E79, 0xFF7D4F59};
                pressedColors = new int[]{0xFFE8B4B8, 0xFFB76E79};
                strokeColor = 0xFFE8B4B8;
                highlightColor = 0x40E8B4B8;
                break;
            
            case "midnight":
                normalColors = new int[]{0xFF162B4D, 0xFF1E3A5F, 0xFF162B4D};
                pressedColors = new int[]{0xFF2A4F7D, 0xFF1E3A5F};
                strokeColor = 0xFF4A7FAA;
                highlightColor = 0x404A7FAA;
                break;
            
            case "lava":
                normalColors = new int[]{0xFF4A0000, 0xFF8B0000, 0xFF4A0000};
                pressedColors = new int[]{0xFFFF4400, 0xFF8B0000};
                strokeColor = 0xFFFF4400;
                highlightColor = 0x40FF4400;
                break;
            
            case "ice":
                normalColors = new int[]{0xFF7FB3F4, 0xFF5A9DEB, 0xFF7FB3F4};
                pressedColors = new int[]{0xFF9FCCFA, 0xFF5A9DEB};
                strokeColor = 0xFF9FCCFA;
                highlightColor = 0x409FCCFA;
                break;
            
            case "amoled":
                normalColors = new int[]{0xFF000000, 0xFF0A0A0A, 0xFF000000};
                pressedColors = new int[]{0xFF1A1A1A, 0xFF000000};
                strokeColor = 0xFF222222;
                highlightColor = 0x40FFFFFF;
                break;
            
            case "pastel":
                normalColors = new int[]{0xFFFFE3EC, 0xFFFAD1E6, 0xFFF6C1D0};
                pressedColors = new int[]{0xFFF6C1D0, 0xFFFAD1E6};
                strokeColor = 0xFFE6A8B8;
                highlightColor = 0x90FFFFFF;
                break;
            
            case "materialyou":
                normalColors = new int[]{0xFFBB86FC, 0xFF9E6CFF, 0xFF7F39FB};
                pressedColors = new int[]{0xFFA077FF, 0xFF7F39FB};
                strokeColor = 0xFFBB86FC;
                highlightColor = 0x60FFFFFF;
                break;
            
            case "rainbow":
                normalColors = new int[]{0xFFFF595E, 0xFFFFCA3A, 0xFF8AC926};
                pressedColors = new int[]{0xFF1982C4, 0xFF6A4C93};
                strokeColor = 0xFFFFFFFF;
                highlightColor = 0x60FFFFFF;
                break;
            
            case "metal":
                normalColors = new int[]{0xFF757575, 0xFFB0B0B0, 0xFF8E8E8E};
                pressedColors = new int[]{0xFFA0A0A0, 0xFF6E6E6E};
                strokeColor = 0xFFCCCCCC;
                highlightColor = 0x80FFFFFF;
                break;
            
            case "paper":
                normalColors = new int[]{0xFFF9F7F2, 0xFFF4F1EA, 0xFFEDE9DF};
                pressedColors = new int[]{0xFFEDE9DF, 0xFFF4F1EA};
                strokeColor = 0xFFD5D1C6;
                highlightColor = 0xA0FFFFFF;
                break;
            
            case "clay":
                normalColors = new int[]{0xFFEADBC8, 0xFFD8C3A5, 0xFFCBB399};
                pressedColors = new int[]{0xFFD8C3A5, 0xFFCBB399};
                strokeColor = 0xFFB89B7A;
                highlightColor = 0x80FFFFFF;
                break;
            
            case "catppuccin_latte":
                normalColors = new int[]{0xFFF2D5CF, 0xFFEBD0CA, 0xFFE6C5BE};
                pressedColors = new int[]{0xFFDC8A78, 0xFFEBD0CA};
                strokeColor = 0xFFDC8A78;
                highlightColor = 0x60FFFFFF;
                break;
            case "catppuccin_frappe":
                normalColors = new int[]{0xFF303446, 0xFF414559, 0xFF51576D};
                pressedColors = new int[]{0xFF8CAAEE, 0xFF414559};
                strokeColor = 0xFF8CAAEE;
                highlightColor = 0x408CAAEE;
                break;
            case "catppuccin_macchiato":
                normalColors = new int[]{0xFF24273A, 0xFF363A4F, 0xFF494D64};
                pressedColors = new int[]{0xFF8AADF4, 0xFF363A4F};
                strokeColor = 0xFF8AADF4;
                highlightColor = 0x408AADF4;
                break;
            case "catppuccin_mocha":
                normalColors = new int[]{0xFF1E1E2E, 0xFF313244, 0xFF45475A};
                pressedColors = new int[]{0xFF89B4FA, 0xFF313244};
                strokeColor = 0xFF89B4FA;
                highlightColor = 0x4089B4FA;
                break;
            
            case "everforest_dark":
                normalColors = new int[]{0xFF2D353B, 0xFF343F44, 0xFF3D484D};
                pressedColors = new int[]{0xFFA7C080, 0xFF343F44};
                strokeColor = 0xFFA7C080;
                highlightColor = 0x40A7C080;
                break;
            case "everforest_light":
                normalColors = new int[]{0xFFECE3CC, 0xFFE6DDBF, 0xFFE0D6B5};
                pressedColors = new int[]{0xFF5C6A72, 0xFFE6DDBF};
                strokeColor = 0xFF5C6A72;
                highlightColor = 0x60FFFFFF;
                break;
            
            case "kanagawa":
                normalColors = new int[]{0xFF1F1F28, 0xFF2A2A37, 0xFF223249};
                pressedColors = new int[]{0xFF7E9CD8, 0xFF2A2A37};
                strokeColor = 0xFF7E9CD8;
                highlightColor = 0x407E9CD8;
                break;
            case "onedarkpro":
                normalColors = new int[]{0xFF282C34, 0xFF30343C, 0xFF3A3F48};
                pressedColors = new int[]{0xFF61AFEF, 0xFF30343C};
                strokeColor = 0xFF61AFEF;
                highlightColor = 0x4061AFEF;
                break;
            
            case "gruvbox_light":
                normalColors = new int[]{0xFFFBF1C7, 0xFFF2E5BC, 0xFFEDE0B1};
                pressedColors = new int[]{0xFFEBDBB2, 0xFFF2E5BC};
                strokeColor = 0xFF928374;
                highlightColor = 0x60FFFFFF;
                break;
            case "nord_light":
                normalColors = new int[]{0xFFE5E9F0, 0xFFD8DEE9, 0xFFECEFF4};
                pressedColors = new int[]{0xFF81A1C1, 0xFFD8DEE9};
                strokeColor = 0xFF81A1C1;
                highlightColor = 0x6081A1C1;
                break;
            case "solarized_light":
                normalColors = new int[]{0xFFEEE8D5, 0xFFECE3CA, 0xFFEADFC0};
                pressedColors = new int[]{0xFF268BD2, 0xFFECE3CA};
                strokeColor = 0xFF586E75;
                highlightColor = 0x60268BD2;
                break;
            case "sakura":
                normalColors = new int[]{0xFFFFEEF1, 0xFFFFD9E3, 0xFFFFC6D9};
                pressedColors = new int[]{0xFFFFB7C5, 0xFFFFD9E3};
                strokeColor = 0xFFFFB7C5;
                highlightColor = 0x80FFFFFF;
                break;
            case "high_contrast":
                normalColors = new int[]{0xFF000000, 0xFF000000, 0xFF000000};
                pressedColors = new int[]{0xFFFFFFFF, 0xFFFFFFFF};
                strokeColor = 0xFFFFFFFF;
                highlightColor = 0x80FFFFFF;
                break;
            case "mint":
                normalColors = new int[]{0xFFE8FFF3, 0xFFD9FFF0, 0xFFCFF8E6};
                pressedColors = new int[]{0xFFA8E6CF, 0xFFD9FFF0};
                strokeColor = 0xFFA8E6CF;
                highlightColor = 0x80FFFFFF;
                break;
            
            case "mesh":
                normalColors = new int[]{0xFF7F00FF, 0xFFFF00FF, 0xFF00FFFF};
                pressedColors = new int[]{0xFF9A4DFF, 0xFFFF66FF};
                strokeColor = 0x66FFFFFF;
                highlightColor = 0x60FFFFFF;
                break;
            case "holo":
                normalColors = new int[]{0xFF30D5C8, 0xFF20B2AA, 0xFF008B8B};
                pressedColors = new int[]{0xFF20B2AA, 0xFF30D5C8};
                strokeColor = 0x8820B2AA;
                highlightColor = 0x60FFFFFF;
                break;
            case "aurora_plus":
                normalColors = new int[]{0xFF0B132B, 0xFF1C2541, 0xFF5BC0BE};
                pressedColors = new int[]{0xFF3A506B, 0xFF5BC0BE};
                strokeColor = 0xFF5BC0BE;
                highlightColor = 0x405BC0BE;
                break;
            case "candy":
                normalColors = new int[]{0xFFFF80AB, 0xFFFF4081, 0xFFFF1744};
                pressedColors = new int[]{0xFFFF4081, 0xFFFF80AB};
                strokeColor = 0xFFFFFFFF;
                highlightColor = 0x80FFFFFF;
                break;
            case "gold":
                normalColors = new int[]{0xFFFFD700, 0xFFDAA520, 0xFFB8860B};
                pressedColors = new int[]{0xFFE6BE8A, 0xFFDAA520};
                strokeColor = 0xFFEEDC82;
                highlightColor = 0x90FFFFFF;
                break;
            case "carbon":
                normalColors = new int[]{0xFF1C1C1C, 0xFF2B2B2B, 0xFF3A3A3A};
                pressedColors = new int[]{0xFF444444, 0xFF2B2B2B};
                strokeColor = 0xFF555555;
                highlightColor = 0x40FFFFFF;
                break;
            case "slate":
                normalColors = new int[]{0xFF2F4F4F, 0xFF556B2F, 0xFF708090};
                pressedColors = new int[]{0xFF708090, 0xFF2F4F4F};
                strokeColor = 0xFFB0C4DE;
                highlightColor = 0x60FFFFFF;
                break;
            case "midnight_purple":
                normalColors = new int[]{0xFF2A003E, 0xFF4B0082, 0xFF6A0DAD};
                pressedColors = new int[]{0xFF6A0DAD, 0xFF4B0082};
                strokeColor = 0xFFBB86FC;
                highlightColor = 0x60FFFFFF;
                break;
            case "coral":
                normalColors = new int[]{0xFFFF7F50, 0xFFFF6B6B, 0xFFFF8E72};
                pressedColors = new int[]{0xFFFF6B6B, 0xFFFF7F50};
                strokeColor = 0xFFFFC1A1;
                highlightColor = 0x80FFFFFF;
                break;
            case "sunsea":
                normalColors = new int[]{0xFFFFA500, 0xFFFF6B35, 0xFF006994};
                pressedColors = new int[]{0xFFFF8C00, 0xFF0099CC};
                strokeColor = 0xFFFFFFFF;
                highlightColor = 0x60FFFFFF;
                break;
            
            // ✨ PREMIUM VIP TEMALAR ✨
            case "velvet":
                normalColors = new int[]{0xFF8B008B, 0xFF6A0078, 0xFF4B0052};
                pressedColors = new int[]{0xFFAA00AA, 0xFF8B008B};
                strokeColor = 0xFFDD00DD;
                highlightColor = 0x60FF00FF;
                break;
            case "sapphire":
                normalColors = new int[]{0xFF0F52BA, 0xFF0A3D8A, 0xFF06285A};
                pressedColors = new int[]{0xFF1567D6, 0xFF0F52BA};
                strokeColor = 0xFF1E90FF;
                highlightColor = 0x604169E1;
                break;
            case "emerald":
                normalColors = new int[]{0xFF50C878, 0xFF3DAA62, 0xFF2A8B4C};
                pressedColors = new int[]{0xFF6FE68E, 0xFF50C878};
                strokeColor = 0xFF80FFB5;
                highlightColor = 0x6000FF80;
                break;
            case "ruby":
                normalColors = new int[]{0xFFE0115F, 0xFFB00047, 0xFF80002F};
                pressedColors = new int[]{0xFFFF1777, 0xFFE0115F};
                strokeColor = 0xFFFF4D8D;
                highlightColor = 0x60FF006F;
                break;
            case "diamond":
                normalColors = new int[]{0xFFB9F2FF, 0xFF8FD7F0, 0xFF65BCD1};
                pressedColors = new int[]{0xFFD9FFFF, 0xFFB9F2FF};
                strokeColor = 0xFFFFFFFF;
                highlightColor = 0x80FFFFFF;
                break;
            case "platinum":
                normalColors = new int[]{0xFFE5E4E2, 0xFFD0CFCD, 0xFFBBBAB8};
                pressedColors = new int[]{0xFFF5F4F2, 0xFFE5E4E2};
                strokeColor = 0xFFFFFFFF;
                highlightColor = 0x60FFFFFF;
                break;
            case "obsidian":
                normalColors = new int[]{0xFF0B1215, 0xFF070C0E, 0xFF030607};
                pressedColors = new int[]{0xFF1A252A, 0xFF0B1215};
                strokeColor = 0xFF2A3D44;
                highlightColor = 0x40AAAAAA;
                break;
            case "pearl":
                normalColors = new int[]{0xFFF0EAD6, 0xFFE8DFC5, 0xFFE0D4B4};
                pressedColors = new int[]{0xFFFFF8E6, 0xFFF0EAD6};
                strokeColor = 0xFFF5E9D0;
                highlightColor = 0x80FFFFFF;
                break;
            case "topaz":
                normalColors = new int[]{0xFFFFCC00, 0xFFFFBB00, 0xFFFFAA00};
                pressedColors = new int[]{0xFFFFDD22, 0xFFFFCC00};
                strokeColor = 0xFFFFDD55;
                highlightColor = 0x60FFFF00;
                break;
            case "amber":
                normalColors = new int[]{0xFFFFBF00, 0xFFFF9F00, 0xFFFF7F00};
                pressedColors = new int[]{0xFFFFDF22, 0xFFFFBF00};
                strokeColor = 0xFFFFD966;
                highlightColor = 0x60FFDD00;
                break;
            case "turquoise":
                normalColors = new int[]{0xFF40E0D0, 0xFF30C0B0, 0xFF20A090};
                pressedColors = new int[]{0xFF60FFE8, 0xFF40E0D0};
                strokeColor = 0xFF80FFE8;
                highlightColor = 0x6000FFFF;
                break;
            case "aquamarine":
                normalColors = new int[]{0xFF7FFFD4, 0xFF5FDFB4, 0xFF3FBF94};
                pressedColors = new int[]{0xFF9FFFE4, 0xFF7FFFD4};
                strokeColor = 0xFFBFFFE8;
                highlightColor = 0x6000FFAA;
                break;
            case "amethyst":
                normalColors = new int[]{0xFF9966CC, 0xFF7746AC, 0xFF55268C};
                pressedColors = new int[]{0xFFBB86EC, 0xFF9966CC};
                strokeColor = 0xFFDDA6FF;
                highlightColor = 0x60CC66FF;
                break;
            case "crimson":
                normalColors = new int[]{0xFFDC143C, 0xFFBC0F2C, 0xFF9C0A1C};
                pressedColors = new int[]{0xFFFC1A4C, 0xFFDC143C};
                strokeColor = 0xFFFF3366;
                highlightColor = 0x60FF0033;
                break;
            case "champagne":
                normalColors = new int[]{0xFFF7E7CE, 0xFFEDD7BE, 0xFFE3C7AE};
                pressedColors = new int[]{0xFFFFF7E6, 0xFFF7E7CE};
                strokeColor = 0xFFFFEDD8;
                highlightColor = 0x80FFFFFF;
                break;
            
            // 🌑 MISSING DARK THEMES
            case "charcoal":
                normalColors = new int[]{0xFF1C1C1C, 0xFF2A2A2A, 0xFF3A3A3A};
                pressedColors = new int[]{0xFF4A4A4A, 0xFF1C1C1C};
                strokeColor = 0xFF4A4A4A;
                highlightColor = 0x40FFFFFF;
                break;
            case "navy":
                normalColors = new int[]{0xFF001933, 0xFF002244, 0xFF003366};
                pressedColors = new int[]{0xFF004488, 0xFF001933};
                strokeColor = 0xFF004488;
                highlightColor = 0x40FFFFFF;
                break;
            case "wine":
                normalColors = new int[]{0xFF330011, 0xFF550022, 0xFF770033};
                pressedColors = new int[]{0xFF990044, 0xFF330011};
                strokeColor = 0xFF990044;
                highlightColor = 0x40FFFFFF;
                break;
            
            // ☀️ MISSING LIGHT THEMES
            case "snow":
                normalColors = new int[]{0xFFFFFAFA, 0xFFFFFEFE, 0xFFFFFFFF};
                pressedColors = new int[]{0xFFE8E8E8, 0xFFFFFAFA};
                strokeColor = 0xFFE8E8E8;
                highlightColor = 0x40000000;
                break;
            case "cream":
                normalColors = new int[]{0xFFFFFAF0, 0xFFFFF8DC, 0xFFFFFDF5};
                pressedColors = new int[]{0xFFFFE4B5, 0xFFFFFAF0};
                strokeColor = 0xFFFFE4B5;
                highlightColor = 0x40000000;
                break;
            case "cloud":
                normalColors = new int[]{0xFFECECF0, 0xFFF2F2F6, 0xFFF8F8FC};
                pressedColors = new int[]{0xFFD8D8DC, 0xFFECECF0};
                strokeColor = 0xFFD8D8DC;
                highlightColor = 0x40000000;
                break;
            case "linen":
                normalColors = new int[]{0xFFFAF0E6, 0xFFFFF5EE, 0xFFFFFAF0};
                pressedColors = new int[]{0xFFE8DCC8, 0xFFFAF0E6};
                strokeColor = 0xFFE8DCC8;
                highlightColor = 0x40000000;
                break;
            case "lavender":
                normalColors = new int[]{0xFFF3ECFF, 0xFFF8F0FF, 0xFFFDF5FF};
                pressedColors = new int[]{0xFFE6D5FF, 0xFFF3ECFF};
                strokeColor = 0xFFE6D5FF;
                highlightColor = 0x40000000;
                break;
            
            // 🌈 MISSING GRADIENT THEMES
            case "forest_green":
                normalColors = new int[]{0xFF0D4D2D, 0xFF0F5C35, 0xFF11773F};
                pressedColors = new int[]{0xFF139249, 0xFF0D4D2D};
                strokeColor = 0xFF139249;
                highlightColor = 0x40FFFFFF;
                break;
            case "rose":
                normalColors = new int[]{0xFFD11850, 0xFFE81D5E, 0xFFF4377B};
                pressedColors = new int[]{0xFFFF5090, 0xFFD11850};
                strokeColor = 0xFFFF5090;
                highlightColor = 0x40FFFFFF;
                break;
            case "cyber":
                normalColors = new int[]{0xFFDA2877, 0xFF9B4DE0, 0xFF06B6D4};
                pressedColors = new int[]{0xFF00FFFF, 0xFFDA2877};
                strokeColor = 0xFF00FFFF;
                highlightColor = 0x40FFFFFF;
                break;
            case "fire":
                normalColors = new int[]{0xFFBF1E1E, 0xFFD43F3A, 0xFFEA6C2F};
                pressedColors = new int[]{0xFFFF8844, 0xFFBF1E1E};
                strokeColor = 0xFFFF8844;
                highlightColor = 0x40FFFFFF;
                break;
            case "purple_haze":
                normalColors = new int[]{0xFF5F1BA0, 0xFF832ED6, 0xFFA14EE4};
                pressedColors = new int[]{0xFFBB6EFF, 0xFF5F1BA0};
                strokeColor = 0xFFBB6EFF;
                highlightColor = 0x40FFFFFF;
                break;
            
            // 🎯 MISSING MATERIAL THEMES
            case "material_red":
                normalColors = new int[]{0xFFDC3737, 0xFFEF4444, 0xFFF87171};
                pressedColors = new int[]{0xFFFCA5A5, 0xFFDC3737};
                strokeColor = 0xFFFCA5A5;
                highlightColor = 0x40FFFFFF;
                break;
            case "material_blue":
                normalColors = new int[]{0xFF2563EB, 0xFF3B82F6, 0xFF60A5FA};
                pressedColors = new int[]{0xFF93C5FD, 0xFF2563EB};
                strokeColor = 0xFF93C5FD;
                highlightColor = 0x40FFFFFF;
                break;
            case "material_green":
                normalColors = new int[]{0xFF16A34A, 0xFF22C55E, 0xFF4ADE80};
                pressedColors = new int[]{0xFF86EFAC, 0xFF16A34A};
                strokeColor = 0xFF86EFAC;
                highlightColor = 0x40FFFFFF;
                break;
            case "material_purple":
                normalColors = new int[]{0xFF7C28D8, 0xFF9333EA, 0xFFA855F7};
                pressedColors = new int[]{0xFFC084FC, 0xFF7C28D8};
                strokeColor = 0xFFC084FC;
                highlightColor = 0x40FFFFFF;
                break;
            case "material_orange":
                normalColors = new int[]{0xFFEA570C, 0xFFF97316, 0xFFFB923C};
                pressedColors = new int[]{0xFFFDBA74, 0xFFEA570C};
                strokeColor = 0xFFFDBA74;
                highlightColor = 0x40FFFFFF;
                break;
            case "material_teal":
                normalColors = new int[]{0xFF0D9488, 0xFF14B8A6, 0xFF2DD4BF};
                pressedColors = new int[]{0xFF5EEAD4, 0xFF0D9488};
                strokeColor = 0xFF5EEAD4;
                highlightColor = 0x40FFFFFF;
                break;
            case "material_pink":
                normalColors = new int[]{0xFFDB2777, 0xFFEC4899, 0xFFF472B6};
                pressedColors = new int[]{0xFFF9A8D4, 0xFFDB2777};
                strokeColor = 0xFFF9A8D4;
                highlightColor = 0x40FFFFFF;
                break;
            case "material_indigo":
                normalColors = new int[]{0xFF4F46E5, 0xFF6366F1, 0xFF818CF8};
                pressedColors = new int[]{0xFFA5B4FC, 0xFF4F46E5};
                strokeColor = 0xFFA5B4FC;
                highlightColor = 0x40FFFFFF;
                break;
            
            // 🌸 MISSING PASTEL THEMES
            case "pastel_pink":
                normalColors = new int[]{0xFFFFB5C5, 0xFFFFC0CB, 0xFFFFD9E3};
                pressedColors = new int[]{0xFFFFAABB, 0xFFFFB5C5};
                strokeColor = 0xFFFFAABB;
                highlightColor = 0x40000000;
                break;
            case "pastel_blue":
                normalColors = new int[]{0xFF9BC8E6, 0xFFADD8E6, 0xFFC0E5F0};
                pressedColors = new int[]{0xFF88BBD6, 0xFF9BC8E6};
                strokeColor = 0xFF88BBD6;
                highlightColor = 0x40000000;
                break;
            case "pastel_mint":
                normalColors = new int[]{0xFF8BF098, 0xFF98FF98, 0xFFB0FFB8};
                pressedColors = new int[]{0xFF70E080, 0xFF8BF098};
                strokeColor = 0xFF70E080;
                highlightColor = 0x40000000;
                break;
            case "pastel_peach":
                normalColors = new int[]{0xFFFFCFA9, 0xFFFFDAB9, 0xFFFFE7CC};
                pressedColors = new int[]{0xFFFFBB88, 0xFFFFCFA9};
                strokeColor = 0xFFFFBB88;
                highlightColor = 0x40000000;
                break;
            case "pastel_lavender":
                normalColors = new int[]{0xFFD8D2EA, 0xFFE6E6FA, 0xFFF0EBFF};
                pressedColors = new int[]{0xFFCCC0E0, 0xFFD8D2EA};
                strokeColor = 0xFFCCC0E0;
                highlightColor = 0x40000000;
                break;
            case "pastel_yellow":
                normalColors = new int[]{0xFFFFEEB5, 0xFFFFFACD, 0xFFFFFFE0};
                pressedColors = new int[]{0xFFFFDD88, 0xFFFFEEB5};
                strokeColor = 0xFFFFDD88;
                highlightColor = 0x40000000;
                break;
            case "pastel_coral":
                normalColors = new int[]{0xFFFF6540, 0xFFFF7F50, 0xFFFF9F80};
                pressedColors = new int[]{0xFFFF5522, 0xFFFF6540};
                strokeColor = 0xFFFF5522;
                highlightColor = 0x40000000;
                break;
            case "pastel_aqua":
                normalColors = new int[]{0xFF6AEFD4, 0xFF7FFFD4, 0xFF9FFFE4};
                pressedColors = new int[]{0xFF55DFC0, 0xFF6AEFD4};
                strokeColor = 0xFF55DFC0;
                highlightColor = 0x40000000;
                break;
            
            // 🎮 MISSING GAMING THEMES
            case "rgb":
                normalColors = new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFF00FF};
                pressedColors = new int[]{0xFFFFFFFF, 0xFFFF0000};
                strokeColor = 0xFFFFFFFF;
                highlightColor = 0x40FFFFFF;
                break;
            case "matrix":
                normalColors = new int[]{0xFF001100, 0xFF003300, 0xFF005500};
                pressedColors = new int[]{0xFF00FF00, 0xFF001100};
                strokeColor = 0xFF00FF00;
                highlightColor = 0x40FFFFFF;
                break;
            case "retro":
                normalColors = new int[]{0xFFFF0080, 0xFF00CED1, 0xFFFFD700};
                pressedColors = new int[]{0xFFFFFFFF, 0xFFFF0080};
                strokeColor = 0xFFFFFFFF;
                highlightColor = 0x40FFFFFF;
                break;
            case "gamer":
                normalColors = new int[]{0xFF6F2ED6, 0xFFDB2877, 0xFF06B6D4};
                pressedColors = new int[]{0xFF00FFFF, 0xFF6F2ED6};
                strokeColor = 0xFF00FFFF;
                highlightColor = 0x40FFFFFF;
                break;
            case "console":
                normalColors = new int[]{0xFF172234, 0xFF1E293B, 0xFF334155};
                pressedColors = new int[]{0xFF0EA5E9, 0xFF172234};
                strokeColor = 0xFF0EA5E9;
                highlightColor = 0x40FFFFFF;
                break;
                
            case "dark":
            default:
                normalColors = new int[]{0xFF4B5563, 0xFF374151, 0xFF1F2937};
                pressedColors = new int[]{0xFF6B7280, 0xFF4B5563};
                strokeColor = 0xFF6B7280;
                highlightColor = 0x40FFFFFF;
                break;
        }
        
        // StateListDrawable oluştur (pressed + normal)
        android.graphics.drawable.StateListDrawable stateList = new android.graphics.drawable.StateListDrawable();
        
        // Pressed state
        LayerDrawable pressedLayer = createKeyLayerDrawable(pressedColors, strokeColor, highlightColor, true, styleConfig);
        stateList.addState(new int[]{android.R.attr.state_pressed}, pressedLayer);
        
        // Normal state
        LayerDrawable normalLayer = createKeyLayerDrawable(normalColors, strokeColor, highlightColor, false, styleConfig);
        stateList.addState(new int[]{}, normalLayer);
        
        return stateList;
    }
    
    /**
     * Tuş için layer drawable oluştur (shadow + gradient + highlight)
     * Buton şekli desteği ile!
     */
    private LayerDrawable createKeyLayerDrawable(int[] colors, int strokeColor, int highlightColor, boolean pressed, ThemeStyleConfig styleConfig) {
        // Buton şekli tercihi - ModernThemeDialog'dan gelen "selected_shape" kullan
        String buttonShape = prefs.getString("selected_shape", "rounded");
        Log.d(TAG, "🔷 Creating key with shape: " + buttonShape);
        
        int cornerRadius;
        int shadowSize;
        int strokeWidth;
        boolean isOutline = "outline".equals(buttonShape);
        boolean isGlass = "glass".equals(buttonShape);
        boolean isNeon = "neon".equals(buttonShape);
        boolean isBlob = "blob".equals(buttonShape);
        boolean isCircle = "circle".equals(buttonShape);
        float[] blobRadii = null;
        
        switch (buttonShape) {
            case "square":
                cornerRadius = 0; // Keskin köşeler
                shadowSize = dp(2);
                strokeWidth = dp(1);
                break;
            case "circle":
                cornerRadius = dp(999); // Tam yuvarlak
                shadowSize = dp(3);
                strokeWidth = dp(2);
                break;
            case "pill":
                cornerRadius = dp(999); // Çok yuvarlak (circle)
                shadowSize = dp(3);
                strokeWidth = dp(1);
                break;
            case "minimal":
                cornerRadius = dp(8);
                shadowSize = 0; // Gölge yok
                strokeWidth = dp(1); // İnce çerçeve
                break;
            case "flat":
                cornerRadius = dp(10);
                shadowSize = 0; // Gölge yok
                strokeWidth = dp(2);
                break;
            case "3d":
                cornerRadius = dp(12);
                shadowSize = dp(6); // Çok gölge
                strokeWidth = dp(2);
                break;
            case "outline":
                cornerRadius = dp(12);
                shadowSize = 0;
                strokeWidth = dp(2);
                break;
            case "glass":
                cornerRadius = dp(12);
                shadowSize = dp(3);
                strokeWidth = dp(1);
                break;
            case "neon":
                cornerRadius = dp(12);
                shadowSize = dp(4);
                strokeWidth = dp(3);
                break;
            case "blob":
                cornerRadius = dp(12); // fallback
                shadowSize = dp(3);
                strokeWidth = dp(2);
                blobRadii = new float[] {
                    dp(24), dp(24),   // top-left
                    dp(8), dp(8),     // top-right
                    dp(20), dp(20),   // bottom-right
                    dp(12), dp(12)    // bottom-left
                };
                break;
            case "hexagon":
                cornerRadius = dp(6);
                shadowSize = dp(2);
                strokeWidth = dp(2);
                blobRadii = new float[] {
                    dp(6), dp(6),     // top-left
                    dp(6), dp(6),     // top-right
                    dp(6), dp(6),     // bottom-right
                    dp(6), dp(6)      // bottom-left
                };
                break;
            case "octagon":
                cornerRadius = dp(10);
                shadowSize = dp(2);
                strokeWidth = dp(1);
                blobRadii = new float[] {
                    dp(10), dp(10),
                    dp(10), dp(10),
                    dp(10), dp(10),
                    dp(10), dp(10)
                };
                break;
            case "teardrop":
                cornerRadius = dp(18);
                shadowSize = dp(3);
                strokeWidth = dp(2);
                blobRadii = new float[] {
                    dp(30), dp(30),   // top-left (very rounded)
                    dp(8), dp(8),     // top-right
                    dp(4), dp(4),     // bottom-right (sharp)
                    dp(12), dp(12)    // bottom-left
                };
                break;
            case "diamond":
                cornerRadius = dp(8);
                shadowSize = dp(3);
                strokeWidth = dp(2);
                blobRadii = new float[] {
                    dp(8), dp(8),
                    dp(8), dp(8),
                    dp(8), dp(8),
                    dp(8), dp(8)
                };
                break;
            case "wave":
                cornerRadius = dp(16);
                shadowSize = dp(2);
                strokeWidth = dp(2);
                blobRadii = new float[] {
                    dp(20), dp(20),
                    dp(8), dp(8),
                    dp(20), dp(20),
                    dp(8), dp(8)
                };
                break;
            case "cloud":
                cornerRadius = dp(20);
                shadowSize = dp(4);
                strokeWidth = dp(1);
                blobRadii = new float[] {
                    dp(28), dp(28),
                    dp(22), dp(22),
                    dp(18), dp(18),
                    dp(24), dp(24)
                };
                break;
            case "star":
                cornerRadius = dp(5);
                shadowSize = dp(3);
                strokeWidth = dp(2);
                blobRadii = new float[] {
                    dp(5), dp(5),
                    dp(5), dp(5),
                    dp(5), dp(5),
                    dp(5), dp(5)
                };
                break;
            case "heart":
                cornerRadius = dp(14);
                shadowSize = dp(3);
                strokeWidth = dp(2);
                blobRadii = new float[] {
                    dp(20), dp(20),
                    dp(20), dp(20),
                    dp(4), dp(4),
                    dp(14), dp(14)
                };
                break;
                
            // ✨ NEW 30 SHAPES
            case "soft":
                cornerRadius = dp(8);
                shadowSize = dp(1);
                strokeWidth = dp(1);
                break;
            case "sharp":
                cornerRadius = dp(4);
                shadowSize = dp(1);
                strokeWidth = dp(1);
                break;
            case "ultra_round":
                cornerRadius = dp(24);
                shadowSize = dp(3);
                strokeWidth = dp(1);
                break;
            case "bubble":
                cornerRadius = dp(18);
                shadowSize = dp(4);
                strokeWidth = dp(1);
                break;
            case "neo":
                cornerRadius = dp(14);
                shadowSize = dp(2);
                strokeWidth = dp(2);
                break;
            case "fluid":
                cornerRadius = dp(20);
                shadowSize = dp(3);
                strokeWidth = dp(1);
                break;
            case "smooth":
                cornerRadius = dp(15);
                shadowSize = dp(2);
                strokeWidth = dp(1);
                break;
            case "retro":
                cornerRadius = dp(3);
                shadowSize = 0;
                strokeWidth = dp(2);
                break;
            case "classic":
                cornerRadius = dp(5);
                shadowSize = dp(1);
                strokeWidth = dp(1);
                break;
            case "clean":
                cornerRadius = dp(7);
                shadowSize = dp(1);
                strokeWidth = dp(1);
                break;
            case "bold":
                cornerRadius = dp(10);
                shadowSize = dp(3);
                strokeWidth = dp(3);
                break;
            case "thick":
                cornerRadius = dp(11);
                shadowSize = dp(3);
                strokeWidth = dp(3);
                break;
            case "heavy":
                cornerRadius = dp(9);
                shadowSize = dp(4);
                strokeWidth = dp(3);
                break;
            case "strong":
                cornerRadius = dp(13);
                shadowSize = dp(3);
                strokeWidth = dp(3);
                break;
            case "solid":
                cornerRadius = dp(8);
                shadowSize = dp(2);
                strokeWidth = dp(2);
                break;
            case "cyber":
                cornerRadius = dp(1);
                shadowSize = 0;
                strokeWidth = dp(2);
                isNeon = true;
                break;
            case "crystal":
                cornerRadius = dp(18);
                shadowSize = dp(4);
                strokeWidth = dp(1);
                isGlass = true;
                break;
            case "luxury":
                cornerRadius = dp(22);
                shadowSize = dp(5);
                strokeWidth = dp(2);
                break;
            case "elegant":
                cornerRadius = dp(17);
                shadowSize = dp(3);
                strokeWidth = dp(1);
                break;
            case "premium":
                cornerRadius = dp(19);
                shadowSize = dp(4);
                strokeWidth = dp(2);
                break;
            case "vip":
                cornerRadius = dp(21);
                shadowSize = dp(5);
                strokeWidth = dp(2);
                break;
            case "elite":
                cornerRadius = dp(23);
                shadowSize = dp(5);
                strokeWidth = dp(2);
                break;
                
            case "rounded":
            default:
                cornerRadius = dp(12); // Varsayılan
                shadowSize = dp(2);
                strokeWidth = dp(1);
                break;
        }
        
        // Shadow layer (sadece shadow varsa)
        GradientDrawable shadow = new GradientDrawable();
        shadow.setColor(shadowSize > 0 ? (pressed ? 0x20000000 : 0x30000000) : 0x00000000);
        if (blobRadii != null) {
            shadow.setCornerRadii(blobRadii);
        } else {
            shadow.setCornerRadius(cornerRadius);
        }
        
        // Main gradient layer
        GradientDrawable main = new GradientDrawable(
            pressed ? GradientDrawable.Orientation.BOTTOM_TOP : GradientDrawable.Orientation.TOP_BOTTOM,
            colors
        );
        // Gradient tipi tercihi (linear/radial/sweep)
        try {
            String gType = prefs.getString("key_gradient_type", "linear");
            if ("radial".equals(gType)) {
                main.setGradientType(GradientDrawable.RADIAL_GRADIENT);
                main.setGradientRadius(dp(36));
            } else if ("sweep".equals(gType)) {
                main.setGradientType(GradientDrawable.SWEEP_GRADIENT);
            } else {
                main.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            }
        } catch (Throwable ignored) { }
        if (blobRadii != null) {
            main.setCornerRadii(blobRadii);
        } else {
            main.setCornerRadius(cornerRadius);
        }
        main.setStroke(strokeWidth, strokeColor);
        if (isOutline) {
            // Sadece çerçeve, içi saydam
            main = new GradientDrawable();
            if (blobRadii != null) {
                main.setCornerRadii(blobRadii);
            } else {
                main.setCornerRadius(cornerRadius);
            }
            main.setColor(0x00000000);
            main.setStroke(strokeWidth, strokeColor);
        } else if (isGlass) {
            // Cam efekti: yarı saydam beyaz katman
            main.setColors(new int[]{0x66FFFFFF, 0x44FFFFFF, 0x33FFFFFF});
            main.setStroke(strokeWidth, 0x88FFFFFF);
        } else if (isNeon) {
            // Neon: parlak vurgulu çizgi
            main.setColors(new int[]{0x2200FFFF, 0x2200FFFF, 0x2200FFFF});
            main.setStroke(dp(3), 0xFF00FFFF);
        }
        
        // Highlight layer (inner glow)
        GradientDrawable highlight = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{highlightColor, 0x00FFFFFF, 0x00FFFFFF}
        );
        if (blobRadii != null) {
            float d = dp(1);
            float[] inner = new float[] {
                Math.max(0, blobRadii[0] - d), Math.max(0, blobRadii[1] - d),
                Math.max(0, blobRadii[2] - d), Math.max(0, blobRadii[3] - d),
                Math.max(0, blobRadii[4] - d), Math.max(0, blobRadii[5] - d),
                Math.max(0, blobRadii[6] - d), Math.max(0, blobRadii[7] - d)
            };
            highlight.setCornerRadii(inner);
        } else {
            highlight.setCornerRadius(Math.max(0, cornerRadius - dp(1)));
        }
        if (isOutline) {
            // Outline’da iç parlama minimal
            highlight.setColors(new int[]{0x11FFFFFF, 0x00000000, 0x00000000});
        } else if (isNeon) {
            // Neon iç ışıma
            highlight.setColors(new int[]{0x4000FFFF, 0x1000FFFF, 0x00000000});
        } else if (isGlass) {
            // Cam parlaması
            highlight.setColors(new int[]{0x33FFFFFF, 0x11000000, 0x00000000});
        }
        
        // Combine layers
        Drawable[] layers = new Drawable[3];
        layers[0] = shadow;
        layers[1] = main;
        layers[2] = highlight;
        
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        
        // Shadow offset (şekle göre ayarla) - pressed ise hafif aşağı kaydır
        if (shadowSize > 0) {
            int topInset = pressed ? shadowSize / 4 : shadowSize;
            int bottomInset = pressed ? shadowSize + dp(2) : shadowSize;
            layerDrawable.setLayerInset(0, 0, topInset, 0, 0); // shadow layer
            layerDrawable.setLayerInset(1, 0, 0, 0, bottomInset); // main
            layerDrawable.setLayerInset(2, dp(1), dp(1) + (pressed ? dp(1) : 0), dp(1), bottomInset + dp(1)); // highlight
        } else {
            // Shadow yok - flat/minimal
            int add = pressed ? dp(1) : 0;
            layerDrawable.setLayerInset(0, 0, 0, 0, 0);
            layerDrawable.setLayerInset(1, 0, add, 0, add);
            layerDrawable.setLayerInset(2, dp(1), dp(1) + add, dp(1), dp(2) + add);
        }
        
        // 🎨 APPLY THEME STYLE CONFIG
        layerDrawable = applyStyleConfigToKey(layerDrawable, main, colors, strokeColor, styleConfig);
        
        return layerDrawable;
    }
    
    /**
     * Get theme background color for text color calculation
     */
    private int getThemeBackgroundColor(String theme) {
        // Light themes return light color, dark themes return dark color
        if (theme.equals("light") || theme.equals("snow") || theme.equals("cream") || 
            theme.equals("pearl") || theme.equals("cloud") || theme.equals("linen") || 
            theme.equals("mint") || theme.equals("lavender") || theme.equals("paper") || 
            theme.equals("clay") || theme.equals("sakura") || theme.equals("neomorphism") ||
            theme.equals("catppuccin_latte") || theme.equals("everforest_light") || 
            theme.equals("gruvbox_light") || theme.equals("nord_light") || theme.equals("solarized_light")) {
            return 0xFFF5F5F5; // Light
        }
        return 0xFF1A1A1A; // Dark
    }
    
    /**
     * Get theme accent color
     */
    private int getThemeAccentColor(String theme) {
        switch (theme) {
            case "nord": return 0xFF88C0D0;
            case "dracula": return 0xFFBD93F9;
            case "monokai": return 0xFFFD971F;
            case "solarized": return 0xFF268BD2;
            case "gruvbox": return 0xFFB8BB26;
            case "cyberpunk": return 0xFF00FFF0;
            case "tokyo": return 0xFFBB9AF7;
            case "atom": return 0xFF61AFEF;
            case "material": return 0xFF80CBC4;
            case "dynamic": return getDynamicAccentColor();
            default: return 0xFF0A84FF; // Default blue
        }
    }
    
    /**
     * 🎨 Apply ThemeStyleConfig to key drawable
     */
    private LayerDrawable applyStyleConfigToKey(LayerDrawable layerDrawable, GradientDrawable main, int[] colors, int strokeColor, ThemeStyleConfig styleConfig) {
        try {
            ThemeStyleConfig.KeyColorMode colorMode = styleConfig.getKeyColorMode();
            ThemeStyleConfig.KeyBackgroundMode bgMode = styleConfig.getKeyBackgroundMode();
            int strokeWidth = dp(styleConfig.getStrokeWidth());
            
            // Apply color mode
            switch (colorMode) {
                case FILL_ONLY:
                    // Sadece iç dolgu renkli, kenar saydam
                    if (colors.length > 1) {
                        main.setColors(colors);
                    } else if (colors.length == 1) {
                        main.setColor(colors[0]);
                    }
                    main.setStroke(0, 0x00000000);  // No stroke
                    break;
                    
                case STROKE_ONLY:
                    // Sadece kenarlar renkli, iç saydam/minimal
                    main.setColor(0x11FFFFFF);  // Very transparent fill
                    main.setStroke(strokeWidth, strokeColor);
                    break;
                    
                case FILL_AND_STROKE:
                    // Her ikisi de renkli (default)
                    if (colors.length > 1) {
                        main.setColors(colors);
                    } else if (colors.length == 1) {
                        main.setColor(colors[0]);
                    }
                    main.setStroke(strokeWidth, strokeColor);
                    break;
                    
                case GRADIENT:
                    // Force gradient
                    if (colors.length == 1) {
                        // Single color -> create gradient
                        int lighter = blend(colors[0], 0xFFFFFFFF, 0.2f);
                        int darker = blend(colors[0], 0xFF000000, 0.2f);
                        main.setColors(new int[]{lighter, colors[0], darker});
                    } else {
                        main.setColors(colors);
                    }
                    main.setStroke(strokeWidth, strokeColor);
                    break;
            }
            
            // Apply background mode
            switch (bgMode) {
                case SOLID:
                    // Force solid color (use first color)
                    if (colors.length > 0) {
                        main.setColor(colors[0]);
                    }
                    break;
                    
                case GRADIENT:
                    // Already applied above
                    break;
                    
                case TRANSPARENT:
                    // Fully transparent
                    main.setColor(0x00000000);
                    main.setStroke(strokeWidth, strokeColor);  // Keep stroke
                    break;
                    
                case SEMI_TRANSPARENT:
                    // Semi-transparent
                    if (colors.length > 0) {
                        int semiColor = (colors[0] & 0x00FFFFFF) | 0x44000000;  // 25% opacity
                        main.setColor(semiColor);
                    }
                    break;
            }
            
            // Apply opacity
            main.setAlpha((int) (255 * (styleConfig.getKeyOpacity() / 100f)));
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Style config apply error", e);
        }
        
        return layerDrawable;
    }

    private Drawable createThemeBackground(boolean rounded) {
        int radius = rounded ? dp(18) : 0;
        String theme = prefs.getString("theme", "dark");
        
        // 📷 CUSTOM TEMA - Kullanıcının fotoğrafını kullan
        if (theme.equals("custom")) {
            String customPhotoUri = prefs.getString("custom_photo_uri", null);
            if (customPhotoUri != null && !customPhotoUri.isEmpty()) {
                try {
                    android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                        getContentResolver(),
                        android.net.Uri.parse(customPhotoUri)
                    );
                    android.graphics.drawable.BitmapDrawable bitmapDrawable = new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
                    bitmapDrawable.setTileModeXY(android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP);
                    
                    // Eğer rounded ise, ClipDrawable veya Layer kullan (ama basit tutuyoruz)
                    Log.d(TAG, "✅ Custom fotoğraf yüklendi: " + customPhotoUri);
                    return bitmapDrawable;
                } catch (Exception e) {
                    Log.e(TAG, "❌ Custom fotoğraf yüklenemedi: " + e.getMessage());
                    Toast.makeText(this, "❌ Fotoğraf yüklenemedi, varsayılan tema kullanılıyor", Toast.LENGTH_SHORT).show();
                    // Hata durumunda varsayılan koyu tema'ya geç
                    theme = "dark";
                }
            } else {
                Log.w(TAG, "⚠️ Custom tema seçili ama fotoğraf URI yok!");
                Toast.makeText(this, "⚠️ Önce fotoğraf seçmelisiniz!", Toast.LENGTH_SHORT).show();
                theme = "dark";
            }
        }
        
        switch (theme) {
            case "dynamic": {
                int accent = getDynamicAccentColor();
                int darker = blend(accent, 0xFF000000, 0.45f);
                int lighter = blend(accent, 0xFFFFFFFF, 0.20f);
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{darker, accent, lighter});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), blend(accent, 0xFF000000, 0.25f));
                return gradient;
            }
            case "light": {
                GradientDrawable drawable = new GradientDrawable();
                drawable.setColor(0xFFF5F5F5);
                drawable.setCornerRadius(radius);
                if (rounded) drawable.setStroke(dp(1), 0x33000000);
                return drawable;
            }
            
            case "nord": {
                // Nord Theme - Soğuk mavi tonlar
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF2E3440, 0xFF3B4252, 0xFF434C5E});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF88C0D0);
                return gradient;
            }
            
            case "dracula": {
                // Dracula Theme - Mor tonlar
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.BR_TL,
                        new int[]{0xFF282A36, 0xFF44475A, 0xFF6272A4});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFBD93F9);
                return gradient;
            }
            
            case "monokai": {
                // Monokai Theme - Sıcak tonlar
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFF272822, 0xFF3E3D32, 0xFF49483E});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFD971F);
                return gradient;
            }
            
            case "solarized": {
                // Solarized Dark Theme
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF002B36, 0xFF073642, 0xFF586E75});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF268BD2);
                return gradient;
            }
            
            case "gruvbox": {
                // Gruvbox Theme - Retro renk paleti
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.BL_TR,
                        new int[]{0xFF282828, 0xFF3C3836, 0xFF504945});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFB8BB26);
                return gradient;
            }
            
            case "cyberpunk": {
                // Cyberpunk Theme - Neon renkler
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFF0A0E27, 0xFF1A1A2E, 0xFF16213E});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF00FFF0);
                return gradient;
            }
            
            case "tokyo": {
                // Tokyo Night Theme - Pembe-mor tonlar
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF1A1B26, 0xFF24283B, 0xFF414868});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFBB9AF7);
                return gradient;
            }
            
            case "atom": {
                // Atom One Dark Theme
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.BR_TL,
                        new int[]{0xFF282C34, 0xFF2C323C, 0xFF3E4451});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF61AFEF);
                return gradient;
            }
            
            case "material": {
                // Material Theme - Modern mavi-gri
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF263238, 0xFF37474F, 0xFF455A64});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF80CBC4);
                return gradient;
            }
            
            case "palenight": {
                // Palenight Theme - Yumuşak mor
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFF292D3E, 0xFF34324A, 0xFF444267});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFC792EA);
                return gradient;
            }
            
            case "owl": {
                // Night Owl Theme - Koyu mavi
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.BL_TR,
                        new int[]{0xFF011627, 0xFF0B2942, 0xFF1D3B53});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF82AAFF);
                return gradient;
            }
            
            case "espresso": {
                // Espresso Theme - Kahverengi tonlar
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF2D2006, 0xFF3E2A0F, 0xFF5C4520});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFD4A574);
                return gradient;
            }
            
            case "synthwave": {
                // Synthwave Theme - Neon pembe-mavi
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFF241734, 0xFF2B213A, 0xFF47315B});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFF6AD5);
                return gradient;
            }
            
            // ✨ YENİ MODERN TEMALAR ✨
            
            case "neon": {
                // Neon Theme - Parlak pembe/mor neon
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF1A0033, 0xFF330066, 0xFF660099, 0xFFFF00FF});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(3), 0xFFFF00FF); // Neon pembe çerçeve
                return gradient;
            }
            
            case "aurora": {
                // Aurora Theme - Kuzey ışıkları yeşil-mavi
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF001A33, 0xFF003D5C, 0xFF00667F, 0xFF00FFAA});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF00FFAA);
                return gradient;
            }
            
            case "sunset": {
                // Sunset Theme - Gün batımı turuncu-kırmızı
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                        new int[]{0xFF4A0E0E, 0xFF8B2635, 0xFFFF6B35, 0xFFFFAA00});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFF6B35);
                return gradient;
            }
            
            case "ocean": {
                // Ocean Theme - Derin okyanus mavi
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF001F3F, 0xFF003D5C, 0xFF006994, 0xFF0099CC});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF0099CC);
                return gradient;
            }
            
            case "forest": {
                // Forest Theme - Koyu orman yeşili
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF0D1F0A, 0xFF1A3D16, 0xFF2D5016, 0xFF4A7C2E});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF4A7C2E);
                return gradient;
            }
            
            case "galaxy": {
                // Galaxy Theme - Uzay galaksisi mor-mavi
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF0A0A1F, 0xFF1A1A40, 0xFF2D2D5C, 0xFF4A4A99, 0xFF6666CC});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF6666CC);
                return gradient;
            }
            
            case "neomorphism": {
                // Neomorphism Theme - Yumuşak gölgeli açık gri
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFFE0E5EC, 0xFFD1D9E6, 0xFFCCD5E0});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFFFFFFF); // Beyaz çerçeve
                return gradient;
            }
            
            case "glassmorphism": {
                // Glassmorphism Theme - Saydam cam efekti
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0x66FFFFFF, 0x55DDDDDD, 0x44AAAAAA});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xAAFFFFFF); // Yarı saydam beyaz
                return gradient;
            }
            
            case "hacker": {
                // Hacker Theme - Matrix yeşil terminal
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF000000, 0xFF001100, 0xFF002200});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF00FF00); // Yeşil neon çerçeve
                return gradient;
            }
            
            case "rosegold": {
                // Rose Gold Theme - Pembe altın
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF4A2C2A, 0xFF7D4F59, 0xFFB76E79, 0xFFE8B4B8});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFE8B4B8);
                return gradient;
            }
            
            case "midnight": {
                // Midnight Theme - Gece mavisi
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF0F1A2E, 0xFF162B4D, 0xFF1E3A5F, 0xFF2A4F7D});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF4A7FAA);
                return gradient;
            }
            
            case "lava": {
                // Lava Theme - Kızgın lav kırmızı-turuncu
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                        new int[]{0xFF1A0000, 0xFF4A0000, 0xFF8B0000, 0xFFFF4400});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFF4400);
                return gradient;
            }
            
            case "ice": {
                // Ice Theme - Buz mavisi
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF9FCCFA, 0xFF7FB3F4, 0xFF5A9DEB, 0xFF3B88E0});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF9FCCFA);
                return gradient;
            }
            
            case "mesh": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF7F00FF, 0xFFFF00FF, 0xFF00FFFF});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0x66FFFFFF);
                return gradient;
            }
            case "holo": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFF30D5C8, 0xFF20B2AA, 0xFF008B8B});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0x8820B2AA);
                return gradient;
            }
            case "aurora_plus": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF0B132B, 0xFF1C2541, 0xFF5BC0BE});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF5BC0BE);
                return gradient;
            }
            case "candy": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFFFF80AB, 0xFFFF4081, 0xFFFF1744});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFFFFFF);
                return gradient;
            }
            case "gold": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFFFFD700, 0xFFDAA520, 0xFFB8860B});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFEEDC82);
                return gradient;
            }
            case "carbon": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF1C1C1C, 0xFF2B2B2B, 0xFF3A3A3A});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF555555);
                return gradient;
            }
            case "slate": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF2F4F4F, 0xFF556B2F, 0xFF708090});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFB0C4DE);
                return gradient;
            }
            case "midnight_purple": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF2A003E, 0xFF4B0082, 0xFF6A0DAD});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFBB86FC);
                return gradient;
            }
            case "coral": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFFFF7F50, 0xFFFF6B6B, 0xFFFF8E72});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFFC1A1);
                return gradient;
            }
            case "sunsea": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFFFFA500, 0xFFFF6B35, 0xFF006994});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFFFFFF);
                return gradient;
            }
            
            case "amoled": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF000000, 0xFF0A0A0A, 0xFF000000});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0x33222222);
                return gradient;
            }
            
            case "pastel": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFFFE3EC, 0xFFFAD1E6, 0xFFF6C1D0});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFE6A8B8);
                return gradient;
            }
            
            case "materialyou": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFF7F39FB, 0xFF9E6CFF, 0xFFBB86FC});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFBB86FC);
                return gradient;
            }
            
            case "rainbow": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFFFF595E, 0xFFFFCA3A, 0xFF8AC926, 0xFF1982C4, 0xFF6A4C93});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFFFFFF);
                return gradient;
            }
            
            case "metal": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF606060, 0xFFB0B0B0, 0xFF8E8E8E});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFCCCCCC);
                return gradient;
            }
            
            case "paper": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFF9F7F2, 0xFFF4F1EA, 0xFFEDE9DF});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFD5D1C6);
                return gradient;
            }
            
            case "clay": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFEADBC8, 0xFFD8C3A5, 0xFFCBB399});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFB89B7A);
                return gradient;
            }
            
            case "catppuccin_latte": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFF2D5CF, 0xFFEBD0CA, 0xFFE6C5BE});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFDC8A78);
                return gradient;
            }
            case "catppuccin_frappe": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF303446, 0xFF414559, 0xFF51576D});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF8CAAEE);
                return gradient;
            }
            case "catppuccin_macchiato": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF24273A, 0xFF363A4F, 0xFF494D64});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF8AADF4);
                return gradient;
            }
            case "catppuccin_mocha": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF1E1E2E, 0xFF313244, 0xFF45475A});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF89B4FA);
                return gradient;
            }
            
            case "everforest_dark": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF2D353B, 0xFF343F44, 0xFF3D484D});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFA7C080);
                return gradient;
            }
            case "everforest_light": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFECE3CC, 0xFFE6DDBF, 0xFFE0D6B5});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFF5C6A72);
                return gradient;
            }
            
            case "kanagawa": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF1F1F28, 0xFF2A2A37, 0xFF223249});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF7E9CD8);
                return gradient;
            }
            case "onedarkpro": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF282C34, 0xFF30343C, 0xFF3A3F48});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF61AFEF);
                return gradient;
            }
            
            case "gruvbox_light": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFFBF1C7, 0xFFF2E5BC, 0xFFEBDBB2});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFF928374);
                return gradient;
            }
            case "nord_light": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFE5E9F0, 0xFFD8DEE9, 0xFFECEFF4});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFF81A1C1);
                return gradient;
            }
            case "solarized_light": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFEEE8D5, 0xFFECE3CA, 0xFFEADFC0});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFF586E75);
                return gradient;
            }
            case "sakura": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFFFEEF1, 0xFFFFD9E3, 0xFFFFC6D9});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFFFB7C5);
                return gradient;
            }
            case "high_contrast": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF000000, 0xFF000000, 0xFF000000});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFFFFFF);
                return gradient;
            }
            case "mint": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFE8FFF3, 0xFFD9FFF0, 0xFFCFF8E6});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFA8E6CF);
                return gradient;
            }
            
            // ✨ 15 PREMIUM VIP TEMALAR - ARKAPLAN ✨
            case "velvet": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.BR_TL,
                        new int[]{0xFF4B0052, 0xFF6A0078, 0xFF8B008B, 0xFFAA00AA});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFDD00DD);
                return gradient;
            }
            case "sapphire": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF06285A, 0xFF0A3D8A, 0xFF0F52BA, 0xFF1567D6});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF1E90FF);
                return gradient;
            }
            case "emerald": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.BL_TR,
                        new int[]{0xFF2A8B4C, 0xFF3DAA62, 0xFF50C878, 0xFF6FE68E});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF80FFB5);
                return gradient;
            }
            case "ruby": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFF80002F, 0xFFB00047, 0xFFE0115F, 0xFFFF1777});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFF4D8D);
                return gradient;
            }
            case "diamond": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF65BCD1, 0xFF8FD7F0, 0xFFB9F2FF, 0xFFD9FFFF});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFFFFFF);
                return gradient;
            }
            case "platinum": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFFBBBAB8, 0xFFD0CFCD, 0xFFE5E4E2, 0xFFF5F4F2});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFFFFFF);
                return gradient;
            }
            case "obsidian": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF030607, 0xFF070C0E, 0xFF0B1215, 0xFF1A252A});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF2A3D44);
                return gradient;
            }
            case "pearl": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFE0D4B4, 0xFFE8DFC5, 0xFFF0EAD6, 0xFFFFF8E6});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFF5E9D0);
                return gradient;
            }
            case "topaz": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.BL_TR,
                        new int[]{0xFFFFAA00, 0xFFFFBB00, 0xFFFFCC00, 0xFFFFDD22});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFFDD55);
                return gradient;
            }
            case "amber": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFFFF7F00, 0xFFFF9F00, 0xFFFFBF00, 0xFFFFDF22});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFFD966);
                return gradient;
            }
            case "turquoise": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF20A090, 0xFF30C0B0, 0xFF40E0D0, 0xFF60FFE8});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF80FFE8);
                return gradient;
            }
            case "aquamarine": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.BR_TL,
                        new int[]{0xFF3FBF94, 0xFF5FDFB4, 0xFF7FFFD4, 0xFF9FFFE4});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFBFFFE8);
                return gradient;
            }
            case "amethyst": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF55268C, 0xFF7746AC, 0xFF9966CC, 0xFFBB86EC});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFDDA6FF);
                return gradient;
            }
            case "crimson": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFF9C0A1C, 0xFFBC0F2C, 0xFFDC143C, 0xFFFC1A4C});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFF3366);
                return gradient;
            }
            case "champagne": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFE3C7AE, 0xFFEDD7BE, 0xFFF7E7CE, 0xFFFFF7E6});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFFEDD8);
                return gradient;
            }
            
            // 🌑 MISSING DARK THEMES
            case "charcoal": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF1C1C1C, 0xFF2A2A2A, 0xFF3A3A3A});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFF4A4A4A);
                return gradient;
            }
            case "navy": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF001933, 0xFF002244, 0xFF003366});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF004488);
                return gradient;
            }
            case "wine": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF330011, 0xFF550022, 0xFF770033});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF990044);
                return gradient;
            }
            
            // ☀️ MISSING LIGHT THEMES
            case "snow": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFFFFAFA, 0xFFFFFEFE, 0xFFFFFFFF});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFE8E8E8);
                return gradient;
            }
            case "cream": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFFFFAF0, 0xFFFFF8DC, 0xFFFFFDF5});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFFFE4B5);
                return gradient;
            }
            case "cloud": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFECECF0, 0xFFF2F2F6, 0xFFF8F8FC});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFD8D8DC);
                return gradient;
            }
            case "linen": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFFAF0E6, 0xFFFFF5EE, 0xFFFFFAF0});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFE8DCC8);
                return gradient;
            }
            case "lavender": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFF3ECFF, 0xFFF8F0FF, 0xFFFDF5FF});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFE6D5FF);
                return gradient;
            }
            
            // 🌈 MISSING GRADIENT THEMES
            case "forest_green": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF0D4D2D, 0xFF0F5C35, 0xFF11773F});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF139249);
                return gradient;
            }
            case "rose": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFFD11850, 0xFFE81D5E, 0xFFF4377B});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFF5090);
                return gradient;
            }
            case "cyber": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFFDA2877, 0xFF9B4DE0, 0xFF06B6D4});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF00FFFF);
                return gradient;
            }
            case "fire": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                        new int[]{0xFFBF1E1E, 0xFFD43F3A, 0xFFEA6C2F});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFF8844);
                return gradient;
            }
            case "purple_haze": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF5F1BA0, 0xFF832ED6, 0xFFA14EE4});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFBB6EFF);
                return gradient;
            }
            
            // 🎯 MISSING MATERIAL THEMES
            case "material_red": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFDC3737, 0xFFEF4444, 0xFFF87171});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFCA5A5);
                return gradient;
            }
            case "material_blue": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF2563EB, 0xFF3B82F6, 0xFF60A5FA});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF93C5FD);
                return gradient;
            }
            case "material_green": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF16A34A, 0xFF22C55E, 0xFF4ADE80});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF86EFAC);
                return gradient;
            }
            case "material_purple": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF7C28D8, 0xFF9333EA, 0xFFA855F7});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFC084FC);
                return gradient;
            }
            case "material_orange": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFEA570C, 0xFFF97316, 0xFFFB923C});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFDBA74);
                return gradient;
            }
            case "material_teal": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF0D9488, 0xFF14B8A6, 0xFF2DD4BF});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF5EEAD4);
                return gradient;
            }
            case "material_pink": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFDB2777, 0xFFEC4899, 0xFFF472B6});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFF9A8D4);
                return gradient;
            }
            case "material_indigo": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF4F46E5, 0xFF6366F1, 0xFF818CF8});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFA5B4FC);
                return gradient;
            }
            
            // 🌸 MISSING PASTEL THEMES
            case "pastel_pink": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFFFB5C5, 0xFFFFC0CB, 0xFFFFD9E3});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFFFAABB);
                return gradient;
            }
            case "pastel_blue": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF9BC8E6, 0xFFADD8E6, 0xFFC0E5F0});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFF88BBD6);
                return gradient;
            }
            case "pastel_mint": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF8BF098, 0xFF98FF98, 0xFFB0FFB8});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFF70E080);
                return gradient;
            }
            case "pastel_peach": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFFFCFA9, 0xFFFFDAB9, 0xFFFFE7CC});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFFFBB88);
                return gradient;
            }
            case "pastel_lavender": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFD8D2EA, 0xFFE6E6FA, 0xFFF0EBFF});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFCCC0E0);
                return gradient;
            }
            case "pastel_yellow": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFFFEEB5, 0xFFFFFACD, 0xFFFFFFE0});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFFFDD88);
                return gradient;
            }
            case "pastel_coral": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFFF6540, 0xFFFF7F50, 0xFFFF9F80});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFFFF5522);
                return gradient;
            }
            case "pastel_aqua": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF6AEFD4, 0xFF7FFFD4, 0xFF9FFFE4});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0xFF55DFC0);
                return gradient;
            }
            
            // 🎮 MISSING GAMING THEMES
            case "rgb": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFF00FF});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFFFFFF);
                return gradient;
            }
            case "matrix": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF001100, 0xFF003300, 0xFF005500});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF00FF00);
                return gradient;
            }
            case "retro": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFFFF0080, 0xFF00CED1, 0xFFFFD700});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFFFFFFFF);
                return gradient;
            }
            case "gamer": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF6F2ED6, 0xFFDB2877, 0xFF06B6D4});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF00FFFF);
                return gradient;
            }
            case "console": {
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF172234, 0xFF1E293B, 0xFF334155});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(2), 0xFF0EA5E9);
                return gradient;
            }
            
            case "dark":
            default: {
                // Default Dark Theme - Modern gradient
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFF0A0A0A, 0xFF1A1A1A, 0xFF2A2A2A});
                gradient.setCornerRadius(radius);
                if (rounded) gradient.setStroke(dp(1), 0x33FFFFFF);
                return gradient;
            }
        }
    }

    private void updateSuggestions() {
        if (candidateStripView == null || suggestionManager == null || phrasePredictor == null || smartPrediction == null) {
            return;
        }
        if (composingBuffer.length() == 0) {
            // 🧠 AKILLI TAHMİN: Boşluktan sonra sıradaki kelimeyi öner
            List<String> smartPreds = smartPrediction.getPredictions();
            Log.d(TAG, "🔍 updateSuggestions called with empty buffer");
            Log.d(TAG, "🔍 Recent words: " + smartPrediction.getRecentWords());
            if (!smartPreds.isEmpty()) {
                Log.d(TAG, "💡 Sıradaki kelime tahminleri: " + smartPreds);
                candidateStripView.setSuggestions(smartPreds);
                return;
            } else {
                Log.d(TAG, "⚠️ Akıllı tahmin boş döndü");
            }
            
            // Fallback: Phrase predictor
            List<String> contextSuggestions = phrasePredictor.getSuggestions("", Collections.emptyList());
            if (contextSuggestions.isEmpty()) {
                candidateStripView.hideAll();
            } else {
                candidateStripView.setSuggestions(contextSuggestions);
            }
            return;
        }
        
        // Sözlükten base suggestions al (5 kelime)
        List<String> dictSuggestions = suggestionManager.getSuggestions(composingBuffer.toString(), 5);
        
        // 🧠 AKILLI TAHMİN: Prefix ile başlayan öğrenilen kelimeler + sözlük
        List<String> combinedSuggestions = smartPrediction.getPredictionsWithPrefix(
            composingBuffer.toString(), 
            dictSuggestions
        );
        
        // Phrase predictor ile birleştir (max 5)
        List<String> smartSuggestions = phrasePredictor.getSuggestions(
            composingBuffer.toString(), 
            combinedSuggestions.isEmpty() ? dictSuggestions : combinedSuggestions
        );
        
        // Maksimum 5 kelime göster
        if (smartSuggestions.size() > 5) {
            smartSuggestions = smartSuggestions.subList(0, 5);
        }
        
        if (smartSuggestions.isEmpty()) {
            candidateStripView.hideAll();
        } else {
            candidateStripView.setSuggestions(smartSuggestions);
        }
    }

    private void clearSuggestions() {
        if (candidateStripView != null) {
            candidateStripView.hideAll();
        }
    }

    private boolean handleCharacterKey(int primaryCode, int[] keyCodes, InputConnection ic) {
        saveInputState();
        
        // 🔍 HIZLI ARAMA - "?" ile başlayan metin algılama
        if (primaryCode == ' ' && isQuickSearchMode && composingBuffer.length() > 1) {
            String bufferStr = composingBuffer.toString();
            Log.d(TAG, "🔍 Space basıldı (Hızlı Arama modunda), buffer: '" + bufferStr + "'");
            Toast.makeText(this, "🔍 Buffer: " + bufferStr, Toast.LENGTH_SHORT).show();
            
            if (bufferStr.startsWith("?")) {
                String query = bufferStr.substring(1).trim(); // "?" karakterini çıkar
                if (!query.isEmpty()) {
                    Log.d(TAG, "🔍 Hızlı arama tetiklendi: " + query);
                    Toast.makeText(this, "🔍 Arama başlıyor: " + query, Toast.LENGTH_LONG).show();
                    
                    // Yazdığı metni sil ("?kelime")
                    if (ic != null) {
                        // "?kelime" toplamda composingBuffer.length() karakter
                        ic.deleteSurroundingText(composingBuffer.length(), 0);
                    }
                    composingBuffer.setLength(0);
                    isQuickSearchMode = false; // Flag'i sıfırla
                    
                    // Query'yi kaydet ve Quick Search moduna geç
                    pendingSearchQuery = query;
                    switchToMode(ViewMode.QUICK_SEARCH);
                    
                    return true;
                } else {
                    Log.d(TAG, "🔍 Query boş, arama iptal edildi");
                    Toast.makeText(this, "❌ Query boş!", Toast.LENGTH_SHORT).show();
                    isQuickSearchMode = false;
                }
            }
        }
        
        if (primaryCode == ' ') {
            Log.d(TAG, "⌨️ Space tuşu basıldı, composingBuffer: '" + composingBuffer + "'");
            
            // SEARCH WRITE MODE - Arama input'a space ekle
            if (isSearchWriteMode && searchInputEditText != null) {
                String current = searchInputEditText.getText().toString();
                searchInputEditText.setText(current + " ");
                composingBuffer.setLength(0);
                Log.d(TAG, "🔍 Search input'a SPACE eklendi");
                return true;
            }
            
            // QUICK NOTE YAZMA MODU - QuickNoteView'a space ekle
            if (isNoteWriteMode && currentMode == ViewMode.QUICK_NOTE) {
                View currentView = featureContainer.getChildAt(0);
                if (currentView instanceof com.qrmaster.app.keyboard.views.QuickNoteView) {
                    ((com.qrmaster.app.keyboard.views.QuickNoteView) currentView).appendSpace();
                    Log.d(TAG, "📝 QuickNoteView'a SPACE eklendi");
                }
                composingBuffer.setLength(0);
                return true;
            }
            
            // CRYPTO YAZMA MODU - CryptoView'a space ekle
            if (isCryptoWriteMode && currentMode == ViewMode.CRYPTO && cryptoView != null) {
                cryptoView.appendSpace();
                Log.d(TAG, "🔒 CryptoView'a SPACE eklendi");
                composingBuffer.setLength(0);
                return true;
            }
            
            // MOUSE KEYBOARD MODU - PC'ye space gönder
            if (isMouseKeyboardMode && currentMode == ViewMode.MOUSE_MODE && mouseManager != null) {
                mouseManager.sendKeyPress("SPACE");
                Log.d(TAG, "🖱️ PC'ye SPACE gönderildi");
                // Buffer'ı temizle - yoksa sonraki kelime yazılmaz!
                composingBuffer.setLength(0);
                return true;
            }
            
            // Normal mod
            // Kelimeyi öğren (sözlük, phrase predictor, smart prediction)
            if (composingBuffer.length() > 0) {
                String completedWord = composingBuffer.toString();
                
                Log.d(TAG, "📚 Kelime tamamlandı: '" + completedWord + "'");
                
                // TEXT EXPANDER kontrolü (/ ile başlıyorsa)
                if (completedWord.startsWith("/")) {
                    try {
                        Log.d(TAG, "⚡ TEXT EXPANDER: Kısayol tespit edildi: '" + completedWord + "'");
                        
                        com.qrmaster.app.keyboard.textexpander.TextExpanderManager textExpander = 
                            com.qrmaster.app.keyboard.textexpander.TextExpanderManager.getInstance(this);
                        
                        java.util.List<com.qrmaster.app.keyboard.textexpander.TextShortcut> shortcuts = textExpander.getAllShortcuts();
                        Log.d(TAG, "⚡ TEXT EXPANDER: Toplam " + shortcuts.size() + " kısayol var");
                        
                        com.qrmaster.app.keyboard.textexpander.TextShortcut shortcut = null;
                        for (com.qrmaster.app.keyboard.textexpander.TextShortcut s : shortcuts) {
                            Log.d(TAG, "⚡ TEXT EXPANDER: Kontrol ediliyor: '" + s.getTrigger() + "' vs '" + completedWord + "'");
                            if (s.getTrigger().equals(completedWord)) {
                                shortcut = s;
                                Log.d(TAG, "⚡ TEXT EXPANDER: ✅ EŞLEŞME BULUNDU!");
                                break;
                            }
                        }
                        
                        if (shortcut != null) {
                            // Kısayolu genişlet
                            Log.d(TAG, "⚡ TEXT EXPANDER: Genişletiliyor: " + shortcut.getExpansion());
                            ic.deleteSurroundingText(completedWord.length(), 0);
                            ic.commitText(shortcut.getExpansion() + " ", 1);
                            shortcut.incrementUseCount();
                            textExpander.updateShortcut(shortcut);
                            composingBuffer.setLength(0);
                            saveInputState();
                            android.widget.Toast.makeText(this, "🔄 " + shortcut.getDescription(), android.widget.Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "🔄 Text Expander: " + completedWord + " → " + shortcut.getExpansion());
                            return true;
                        } else {
                            Log.d(TAG, "⚡ TEXT EXPANDER: ❌ Eşleşme bulunamadı!");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Text Expander error", e);
                    }
                }
                
                try {
                if (suggestionManager != null) {
                    suggestionManager.learnWord(completedWord);
                }
                } catch (Exception e) {
                    Log.e(TAG, "suggestionManager.learnWord error", e);
                }
                
                try {
                if (phrasePredictor != null) {
                    phrasePredictor.learnWord(completedWord);
                }
                } catch (Exception e) {
                    Log.e(TAG, "phrasePredictor.learnWord error", e);
                }
                
                // 🧠 AKILLI TAHMİN: Kelime tamamlandı, ilişki kur
                try {
                if (smartPrediction != null) {
                    boolean learned = smartPrediction.onSpacePressed(completedWord);
                    Log.d(TAG, "🧠 SmartPrediction.onSpacePressed('" + completedWord + "') → learned: " + learned);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ smartPrediction.onSpacePressed error", e);
                }
            } else {
                Log.d(TAG, "⚠️ composingBuffer boş, sadece space yazılıyor");
            }
            
            ic.commitText(" ", 1);
            composingBuffer.setLength(0);
            
            Log.d(TAG, "🎯 updateSuggestions() çağrılıyor (Space sonrası)");
            // Boşluktan sonra context-based öneriler göster
            updateSuggestions();
            return true;
        }
        
        // Noktalama işaretleri - cümle bitişi
        if (primaryCode == '.' || primaryCode == '!' || primaryCode == '?' || primaryCode == ';') {
            // ÖZEL: "?" karakteri - Hızlı arama modunu başlat!
            if (primaryCode == '?' && !isQuickSearchMode) {
                isQuickSearchMode = true;
                composingBuffer.setLength(0);
                composingBuffer.append('?'); // "?" karakterini buffer'a ekle
                ic.commitText("?", 1); // Ekrana "?" yaz
                Log.d(TAG, "🔍 Hızlı arama modu AÇILDI - '?' algılandı");
                Toast.makeText(this, "🔍 Hızlı Arama AÇILDI!\nKelime yaz + SPACE bas", Toast.LENGTH_LONG).show();
                return true;
            }
            
            // MOUSE KEYBOARD MODU
            if (isMouseKeyboardMode && currentMode == ViewMode.MOUSE_MODE && mouseManager != null) {
                char punctuation = (char) primaryCode;
                mouseManager.sendKeyPress(String.valueOf(punctuation));
                composingBuffer.setLength(0);
                isQuickSearchMode = false; // Flag'i sıfırla
                Log.d(TAG, "🖱️ PC'ye noktalama gönderildi: " + punctuation);
                return true;
            }
            
            // Normal mod
            // Önce kelimeyi commit et
            if (composingBuffer.length() > 0) {
                String completedWord = composingBuffer.toString();
                ic.commitText(completedWord, 1);
                try {
                if (smartPrediction != null) {
                    smartPrediction.onSpacePressed(completedWord);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ smartPrediction.onSpacePressed error (punctuation)", e);
                }
                composingBuffer.setLength(0);
            }
            
            // Noktalama işaretini ekle
            char punctuation = (char) primaryCode;
            ic.commitText(String.valueOf(punctuation), 1);
            
            // 🧠 Cümle bitti, context'i temizle
            if (smartPrediction != null) {
                smartPrediction.clearRecentWords();
                Log.d(TAG, "🔚 Cümle bitti, context temizlendi");
            }
            
            // Cümle bitişi sonrası otomatik büyük harf
            if (primaryCode == '.' || primaryCode == '!' || primaryCode == '?') {
                caps = true;
                if (mainKeyboard != null) {
                    mainKeyboard.setShifted(true);
                }
                updateKeyLabels();
                if (keyboardView != null) {
                    keyboardView.postInvalidate();
                }
                Log.d(TAG, "⬆️ Cümle bitti - sonraki harf büyük");
            }
            
            clearSuggestions();
            return true;
        }

        if (isWordSeparator(primaryCode)) {
            // MOUSE KEYBOARD MODU
            if (isMouseKeyboardMode && currentMode == ViewMode.MOUSE_MODE && mouseManager != null) {
                String sep = new String(Character.toChars(primaryCode));
                mouseManager.sendKeyPress(sep);
                composingBuffer.setLength(0);
                Log.d(TAG, "🖱️ PC'ye separator gönderildi: " + sep);
                return true;
            }
            
            // Normal mod
            // Kelimeyi öğren ve cümleyi bitir
            if (composingBuffer.length() > 0) {
                String completedWord = composingBuffer.toString();
                if (suggestionManager != null) {
                    suggestionManager.learnWord(completedWord);
                }
                if (phrasePredictor != null) {
                    phrasePredictor.learnWord(completedWord);
                }
            }
            
            String sep = new String(Character.toChars(primaryCode));
            ic.commitText(sep, 1);
            composingBuffer.setLength(0);
            
            // Cümle bitirme işaretlerinde phrase'i sonlandır
            if (primaryCode == '.' || primaryCode == '!' || primaryCode == '?') {
                if (phrasePredictor != null) {
                    phrasePredictor.finishPhrase();
                }
            }
            
            clearSuggestions();
            return true;
        }

        String text = null;
        if (primaryCode != 0) {
            text = new String(Character.toChars(primaryCode));
        } else if (keyCodes != null && keyCodes.length > 0) {
            text = new String(Character.toChars(keyCodes[0]));
        }

        if (TextUtils.isEmpty(text)) {
            return false;
        }

        boolean isLetter = Character.isLetter(text.codePointAt(0));
        
        // Shift/Caps Lock mantığı - BASİT!
        if (caps) {
            text = text.toUpperCase(turkishLocale);
            
            // Eğer Caps Lock DEĞİLSE, ilk harfi yazdıktan sonra otomatik küçük harfe dön
            if (!capsLock) {
                caps = false;
                if (mainKeyboard != null) {
                    mainKeyboard.setShifted(false);
                }
                try {
                    if (symbolsKeyboard != null) {
                        symbolsKeyboard.setShifted(false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "symbolsKeyboard.setShifted error", e);
                }
                
                // Tuş label'larını güncelle - küçük harfe dön
                updateKeyLabels();
                
                // View yenileme - SAFE!
                try {
                    if (keyboardView != null) {
                        keyboardView.postInvalidate();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "postInvalidate error (ignored)", e);
                }
                
                Log.d(TAG, "⬇️ İlk harf büyük yazıldı, shift OFF");
            }
        }
        
        // SEARCH WRITE MODE - Arama input'a karakter ekle
        if (isSearchWriteMode && searchInputEditText != null) {
            String current = searchInputEditText.getText().toString();
            searchInputEditText.setText(current + text);
            Log.d(TAG, "🔍 Search input'a eklendi: " + text);
            if (isLetter) {
                composingBuffer.append(text.toLowerCase(turkishLocale));
            } else {
                composingBuffer.setLength(0);
            }
            return true;
        }
        
        // QUICK NOTE YAZMA MODU - QuickNoteView'a karakter ekle
        if (isNoteWriteMode && currentMode == ViewMode.QUICK_NOTE) {
            View currentView = featureContainer.getChildAt(0);
            if (currentView instanceof com.qrmaster.app.keyboard.views.QuickNoteView) {
                ((com.qrmaster.app.keyboard.views.QuickNoteView) currentView).appendToNote(text);
                Log.d(TAG, "📝 QuickNoteView'a eklendi: " + text);
            }
            if (isLetter) {
                composingBuffer.append(text.toLowerCase(turkishLocale));
            } else {
                composingBuffer.setLength(0);
            }
            return true;
        }
        
        // CRYPTO YAZMA MODU - CryptoView'a karakter ekle
        if (isCryptoWriteMode && currentMode == ViewMode.CRYPTO && cryptoView != null) {
            cryptoView.appendToMessage(text);
            Log.d(TAG, "🔒 CryptoView'a eklendi: " + text);
            if (isLetter) {
                composingBuffer.append(text.toLowerCase(turkishLocale));
            } else {
                composingBuffer.setLength(0);
            }
            return true;
        }
        
        // MOUSE KEYBOARD MODU - PC'ye karakter gönder
        if (isMouseKeyboardMode && currentMode == ViewMode.MOUSE_MODE && mouseManager != null) {
            mouseManager.sendKeyPress(text);
            Log.d(TAG, "🖱️ PC'ye gönderildi: " + text);
            // Buffer'ı yönet - sonraki tuşlar için temiz tut
            if (isLetter) {
                composingBuffer.append(text.toLowerCase(turkishLocale));
            } else {
                composingBuffer.setLength(0);
            }
            return true;
        }
        
        // Normal mod
        ic.commitText(text, 1);
        if (isLetter) {
            composingBuffer.append(text.toLowerCase(turkishLocale));
            
            // Hızlı arama modundaysa öneriler gösterme
            if (!isQuickSearchMode) {
                updateSuggestions();
            } else {
                Log.d(TAG, "🔍 Hızlı arama modunda - buffer: '" + composingBuffer + "'");
            }
        } else if (text.equals("/")) {
            // TEXT EXPANDER için / karakterini buffer'a ekle
            composingBuffer.append(text);
            clearSuggestions();
            Log.d(TAG, "⚡ TEXT EXPANDER: / karakteri eklendi, buffer: '" + composingBuffer + "'");
        } else {
            composingBuffer.setLength(0);
            clearSuggestions();
            isQuickSearchMode = false; // Özel karakter geldi, flag'i sıfırla
        }
        return true;
    }

    private boolean isWordSeparator(int primaryCode) {
        char c = (char) primaryCode;
        return ",.!?:;".indexOf(c) >= 0;
    }

    private void applySuggestion(String suggestion) {
        Log.d(TAG, "📝 applySuggestion çağrıldı: '" + suggestion + "', composingBuffer: '" + composingBuffer + "'");
        
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            Log.e(TAG, "❌ InputConnection null!");
            return;
        }
        
        int length = composingBuffer.length();
        if (length > 0) {
            Log.d(TAG, "🔙 Composing buffer temizleniyor: " + length + " karakter");
            ic.deleteSurroundingText(length, 0);
        }
        
        Log.d(TAG, "✍️ Öneri yazılıyor: '" + suggestion + " '");
        ic.commitText(suggestion + " ", 1);
        
        // 🧠 AKILLI TAHMİN: Seçilen kelimeyi öğren ve sıradaki kelimeleri öner
        if (smartPrediction != null) {
            boolean learned = smartPrediction.onSpacePressed(suggestion);
            Log.d(TAG, "🧠 SmartPrediction.onSpacePressed('" + suggestion + "') → learned: " + learned);
            Log.d(TAG, "📋 Recent words: " + smartPrediction.getRecentWords());
        } else {
            Log.e(TAG, "❌ smartPrediction null!");
        }
        
        if (suggestionManager != null) {
            suggestionManager.learnWord(suggestion);
        }
        
        if (phrasePredictor != null) {
            phrasePredictor.learnWord(suggestion);
        }
        
        composingBuffer.setLength(0);
        Log.d(TAG, "🧹 composingBuffer temizlendi");
        
        // 🎯 Hemen sıradaki kelimeyi öner!
        Log.d(TAG, "🎯 updateSuggestions() çağrılıyor (applySuggestion sonrası)");
        updateSuggestions();
        
        saveInputState();
        Log.d(TAG, "✅ applySuggestion tamamlandı");
    }


    private void showInputMethodPicker() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showInputMethodPicker();
        }
    }

    private void saveInputState() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            CharSequence currentText = ic.getTextBeforeCursor(1000, 0);
            if (currentText != null && !currentText.toString().equals(lastInputState)) {
                undoStack.push(lastInputState);
                lastInputState = currentText.toString();
                redoStack.clear();
            }
        }
    }

    private void registerScreenshotObserver() {
        if (screenshotObserver != null) return;
        if (!hasImageReadPermission()) {
            Log.w(TAG, "Screenshot observer için izin yok");
            return;
        }
        screenshotObserver = new android.database.ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                handleScreenshotCaptured();
            }
        };
        try {
            getContentResolver().registerContentObserver(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    screenshotObserver);
            Log.d(TAG, "✅ Screenshot gözlemcisi aktif");
        } catch (Exception e) {
            Log.e(TAG, "Screenshot observer kaydedilemedi", e);
        }
    }

    private void unregisterScreenshotObserver() {
        if (screenshotObserver != null) {
            try {
                getContentResolver().unregisterContentObserver(screenshotObserver);
            } catch (Exception e) {
                Log.e(TAG, "Screenshot observer kaldırılamadı", e);
            }
            screenshotObserver = null;
        }
    }

    private boolean hasImageReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void handleScreenshotCaptured() {
        if (!hasImageReadPermission()) {
            Log.w(TAG, "Screenshot için izin yok");
            return;
        }
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                android.net.Uri collection = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                String[] projection;
                String sortOrder = android.provider.MediaStore.Images.Media.DATE_ADDED + " DESC";
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    projection = new String[]{
                        android.provider.MediaStore.Images.Media._ID,
                        android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                        android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                        android.provider.MediaStore.Images.Media.DATE_ADDED
                    };
                } else {
                    projection = new String[]{
                        android.provider.MediaStore.Images.Media._ID,
                        android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                        android.provider.MediaStore.Images.Media.DATA,
                        android.provider.MediaStore.Images.Media.DATE_ADDED
                    };
                }
                
                // Son 3 saniyede eklenen resimleri kontrol et
                long now = System.currentTimeMillis() / 1000;
                String selection = android.provider.MediaStore.Images.Media.DATE_ADDED + " > ?";
                String[] selectionArgs = new String[]{String.valueOf(now - 3)};
                
                try (android.database.Cursor cursor = getContentResolver().query(collection, projection, selection, selectionArgs, sortOrder)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        long id = cursor.getLong(0);
                        String name = cursor.getString(1);
                        String pathOrRelative = cursor.getString(2);
                        
                        // Screenshot tespiti: dosya adı veya path'te "screenshot" geçiyor mu?
                        boolean isScreenshot = (name != null && name.toLowerCase(Locale.getDefault()).contains("screenshot")) ||
                                             (pathOrRelative != null && pathOrRelative.toLowerCase(Locale.getDefault()).contains("screenshot"));
                        
                        if (isScreenshot) {
                            android.net.Uri uri = android.content.ContentUris.withAppendedId(collection, id);
                            ClipboardEntry entry = clipboardStore.addScreenshot(uri, name != null ? name : "Screenshot");
                            if (entry != null) {
                                clipboardStore.setSystemClipboard(entry);
                                Log.d(TAG, "✅ Screenshot panoya eklendi: " + name);
                                Toast.makeText(this, "📷 Ekran görüntüsü panoya eklendi", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d(TAG, "Yeni resim screenshot değil: " + name);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Screenshot işlenemedi", e);
            }
        }, 500); // 500ms gecikme - dosyanın tam kaydedilmesini bekle
    }

    private boolean handleModeSpecificKey(int primaryCode, InputConnection ic) {
        if (currentMode == ViewMode.MENU) {
            if (primaryCode == Keyboard.KEYCODE_CANCEL || primaryCode == Keyboard.KEYCODE_DONE) {
                switchToMode(ViewMode.KEYBOARD);
                return true;
            }
            return false;
        }

        if (currentMode == ViewMode.GIF && gifView != null) {
            if (primaryCode == Keyboard.KEYCODE_CANCEL) {
                switchToMode(ViewMode.KEYBOARD);
                return true;
            }
            if (primaryCode == Keyboard.KEYCODE_DELETE) {
                if (gifQueryBuffer.length() > 0) {
                    gifQueryBuffer.deleteCharAt(gifQueryBuffer.length() - 1);
                }
                gifView.setQuery(gifQueryBuffer.toString());
                gifView.setStatusText(gifQueryBuffer.length() == 0 ? "Aramak için yazın..." : "");
                return true;
            }
            if (primaryCode == Keyboard.KEYCODE_DONE) {
                gifView.submitSearch();
                return true;
            }
            if (primaryCode == 32) {
                gifQueryBuffer.append(' ');
                gifView.setQuery(gifQueryBuffer.toString());
                gifView.setStatusText("");
                return true;
            }
            if (primaryCode >= 32 && primaryCode != KEYCODE_SWITCH_SYMBOLS
                    && primaryCode != KEYCODE_SYMBOLS_SHIFT
                    && primaryCode != KEYCODE_SWITCH_ALPHA) {
                gifQueryBuffer.append(Character.toChars(primaryCode));
                gifView.setQuery(gifQueryBuffer.toString());
                gifView.setStatusText("");
                return true;
            }
            return false;
        }

        if (currentMode == ViewMode.TRANSLATE && translateView != null) {
            if (primaryCode == Keyboard.KEYCODE_CANCEL) {
                switchToMode(ViewMode.KEYBOARD);
                return true;
            }
            if (primaryCode == Keyboard.KEYCODE_DELETE) {
                if (translateBuffer.length() > 0) {
                    translateBuffer.deleteCharAt(translateBuffer.length() - 1);
                }
                // translateView.setInputText(translateBuffer.toString()); // Method doesn't exist
                // translateView.setTranslatedText(""); // Method doesn't exist
                // translateView.setStatus("Metin düzenleniyor..."); // Method doesn't exist
                return true;
            }
            if (primaryCode == Keyboard.KEYCODE_DONE) {
                // translateView.requestTranslation(); // Method doesn't exist
                return true;
            }
            if (primaryCode == 32) {
                translateBuffer.append(' ');
                // translateView.setInputText(translateBuffer.toString()); // Method doesn't exist
                // translateView.setTranslatedText(""); // Method doesn't exist
                // translateView.setStatus("Metin hazırlanıyor..."); // Method doesn't exist
                return true;
            }
            if (primaryCode >= 32 && primaryCode != KEYCODE_SWITCH_SYMBOLS
                    && primaryCode != KEYCODE_SYMBOLS_SHIFT
                    && primaryCode != KEYCODE_SWITCH_ALPHA) {
                translateBuffer.append(Character.toChars(primaryCode));
                // translateView.setInputText(translateBuffer.toString()); // Method doesn't exist
                // translateView.setTranslatedText(""); // Method doesn't exist
                // translateView.setStatus("Metin hazırlanıyor..."); // Method doesn't exist
                return true;
            }
        }

        // Mini-app input routing
        if (currentMode == ViewMode.CURRENCY && currencyView != null) {
            if (primaryCode == Keyboard.KEYCODE_DELETE) {
                currencyView.backspaceAmount();
                return true;
            }
            if (primaryCode == Keyboard.KEYCODE_DONE) {
                currencyView.submitInsert();
                return true;
            }
            if (primaryCode == 32) {
                // ignore space
                return true;
            }
            if (primaryCode >= 32 && primaryCode < 127) {
                char ch = (char) primaryCode;
                if ((ch >= '0' && ch <= '9') || ch == '.' || ch == ',') {
                    currencyView.appendAmountText(String.valueOf(ch));
                    return true;
                }
            }
        }

        if (currentMode == ViewMode.CALCULATOR && calculatorView != null) {
            if (primaryCode == Keyboard.KEYCODE_DELETE) {
                calculatorView.inputFromKeyboard("⌫");
                return true;
            }
            if (primaryCode == Keyboard.KEYCODE_DONE) {
                calculatorView.inputFromKeyboard("=");
                return true;
            }
            if (primaryCode >= 32 && primaryCode < 127) {
                char ch = (char) primaryCode;
                String s = String.valueOf(ch);
                // Map operator chars
                if (s.equals("/")) s = "÷";
                if (s.equals("*")) s = "×";
                calculatorView.inputFromKeyboard(s);
                return true;
            }
        }

        return false;
    }

    /**
     * GIF'i asenkron olarak indirip FileProvider ile content URI olarak commit eder.
     * WhatsApp gibi uygulamalara görüntü olarak gönderir, URL değil.
     */
    private void commitGifFromUrlAsync(String urlString) {
        EditorInfo editorInfo = getCurrentInputEditorInfo();
        if (editorInfo == null) {
            Log.w(TAG, "EditorInfo null, GIF gönderilemedi");
            return;
        }

        // 🔧 WhatsApp, Telegram gibi uygulamalar için özel kontrol
        String packageName = editorInfo.packageName != null ? editorInfo.packageName : "";
        boolean isWhatsApp = packageName.contains("whatsapp") || packageName.contains("com.whatsapp");
        boolean isTelegram = packageName.contains("telegram");
        boolean isMessenger = packageName.contains("messenger");
        boolean isInstagram = packageName.contains("instagram");
        boolean forceGifSupport = isWhatsApp || isTelegram || isMessenger || isInstagram;

        // Editör GIF/image kabul ediyor mu?
        String[] mimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo);
        boolean gifSupported = forceGifSupport; // Popüler uygulamalar için her zaman true
        
        if (!forceGifSupport) {
        for (String mt : mimeTypes) {
            if ("image/gif".equalsIgnoreCase(mt) || "image/*".equalsIgnoreCase(mt)) {
                gifSupported = true;
                break;
            }
        }
        }
        
        if (!gifSupported) {
            Log.w(TAG, "Editör GIF desteklemiyor, URL olarak gönderiliyor");
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.commitText(urlString, 1);
                saveInputState();
            }
            return;
        }

        Log.d(TAG, "📱 GIF gönderiliyor: " + packageName + " (forceSupport=" + forceGifSupport + ")");

        // Asenkron indirme ve commit
        new Thread(() -> {
            try {
                File cacheDir = new File(getCacheDir(), "gifs");
                if (!cacheDir.exists()) cacheDir.mkdirs();
                String fileName = "gif_" + System.currentTimeMillis() + ".gif";
                File outFile = new File(cacheDir, fileName);

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(15000);
                connection.connect();
                try (InputStream in = connection.getInputStream();
                     FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                } finally {
                    connection.disconnect();
                }

                // FileProvider URI
                android.net.Uri contentUri = FileProvider.getUriForFile(
                        TurkishKeyboardService.this,
                        getPackageName() + ".fileprovider",
                        outFile
                );

                // UI thread'de commit
                new Handler(getMainLooper()).post(() -> {
                    try {
                        EditorInfo ed = getCurrentInputEditorInfo();
                        InputConnection ic = getCurrentInputConnection();
                        if (ed == null || ic == null) {
                            Log.w(TAG, "EditorInfo veya InputConnection null, commit edilemedi");
                            return;
                        }

                        // Grant perms to editor target package
                        if (ed.packageName != null) {
                            try {
                                grantUriPermission(ed.packageName, contentUri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (Exception ignored) {}
                        }

                        // Hedef editörün desteklediği MIME tipini seç
                        String[] supported = EditorInfoCompat.getContentMimeTypes(ed);
                        String preferredMime = "image/gif";
                        boolean hasGif = false, hasImageWildcard = false;
                        for (String mt : supported) {
                            if ("image/gif".equalsIgnoreCase(mt)) { hasGif = true; }
                            if ("image/*".equalsIgnoreCase(mt)) { hasImageWildcard = true; }
                        }
                        if (!hasGif && hasImageWildcard) preferredMime = "image/*";

                        InputContentInfoCompat contentInfo = new InputContentInfoCompat(
                                contentUri,
                                new android.content.ClipDescription("GIF", new String[]{preferredMime}),
                                null
                        );

                        int flags = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION;
                        boolean success = InputConnectionCompat.commitContent(
                                ic, ed, contentInfo, flags, null);
                        if (success) {
                            Log.d(TAG, "✅ GIF görüntü olarak gönderildi: " + preferredMime);
                            Toast.makeText(TurkishKeyboardService.this, "🎬 GIF gönderildi", Toast.LENGTH_SHORT).show();
                            saveInputState();
                        } else {
                            Log.w(TAG, "⚠️ GIF image commit başarısız, URL fallback");
                            ic.commitText(urlString, 1);
                            Toast.makeText(TurkishKeyboardService.this, "⚠️ GIF URL olarak gönderildi", Toast.LENGTH_SHORT).show();
                            saveInputState();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "GIF commit error", e);
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            ic.commitText(urlString, 1);
                            saveInputState();
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "GIF download failed", e);
                new Handler(getMainLooper()).post(() -> {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        ic.commitText(urlString, 1);
                        saveInputState();
                    }
                });
            }
        }).start();
    }

    // KeyboardView.OnKeyboardActionListener implementation
    // Uzun basma için handler
    private Handler longPressHandler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;
    private int lastPressedKey = 0;
    private boolean longPressTriggered = false;

    @Override
    public void onPress(int primaryCode) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            isDeleting = true;
            deleteHandler.postDelayed(deleteRunnable, 500);
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            isShiftPressed = true;
            shiftConsumed = false;
            // 350ms üzeri uzun basma ile Caps Lock
            shiftHandler.postDelayed(shiftLongPressRunnable, 350);
        }
    }

    @Override
    public void onRelease(int primaryCode) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            isDeleting = false;
            deleteHandler.removeCallbacks(deleteRunnable);
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            isShiftPressed = false;
            shiftHandler.removeCallbacks(shiftLongPressRunnable);
            if (!shiftConsumed) {
                try {
                    // BASİT MANTIK - SADECE STATE TOGGLE!
                    if (capsLock || caps) {
                        caps = false;
                        capsLock = false;
                        Log.d(TAG, "⬇️ Shift OFF - küçük harf");
                    } else {
                        caps = true;
                        capsLock = false;
                        Log.d(TAG, "⬆️ Shift ON - BÜYÜK HARF");
                    }
                    
                    // ULTRA SAFE - her şey try-catch içinde!
                    try {
                        if (mainKeyboard != null) {
                            mainKeyboard.setShifted(caps);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "mainKeyboard.setShifted error", e);
                    }
                    
                    try {
                        if (symbolsKeyboard != null) {
                            symbolsKeyboard.setShifted(caps);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "symbolsKeyboard.setShifted error", e);
                    }
                    
                    // Tuş label'larını güncelle
                    updateKeyLabels();
                    
                    // View yenileme - SAFE!
                    try {
                        if (keyboardView != null) {
                            keyboardView.postInvalidate();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "postInvalidate error (ignored)", e);
                    }
                    
                    Log.d(TAG, "✅ Shift tamamlandı - caps=" + caps);
                    
                } catch (Exception e) {
                    Log.e(TAG, "❌ Shift hatası:", e);
                    // En kötü durumda - sadece toggle
                    caps = !caps;
                }
            }
        }
        // Global klavye animasyonu yok - yalnızca tuş pressed state ile tepkisel
    }

    @Override
    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
            saveInputState();
        }
    }

    @Override
    public void swipeLeft() {}

    @Override
    public void swipeRight() {}

    @Override
    public void swipeDown() {}

    @Override
    public void swipeUp() {}
    
    /**
     * Component ID döndür - KeyboardManagerPlugin için
     */
    public static String getComponentId(Context context) {
        ComponentName componentName = new ComponentName(context, TurkishKeyboardService.class);
        return componentName.flattenToShortString();
    }

    private void adjustKeyboardScale(float delta) {
        keyboardScale = Math.max(0.8f, Math.min(1.4f, keyboardScale + delta));
        prefs.edit().putFloat("keyboard_scale", keyboardScale).apply();
        applyKeyboardScale();
        Toast.makeText(this, String.format(Locale.getDefault(), "Klavye boyutu: %d%%", (int) (keyboardScale * 100)), Toast.LENGTH_SHORT).show();
    }

    private void resetKeyboardScale() {
        keyboardScale = 1.0f;
        prefs.edit().putFloat("keyboard_scale", keyboardScale).apply();
        applyKeyboardScale();
        Toast.makeText(this, "Klavye boyutu sıfırlandı", Toast.LENGTH_SHORT).show();
    }

    private void applyKeyboardScale() {
        if (keyboardContainer == null) return;
        keyboardContainer.post(() -> {
            if (keyboardContainer == null) return;
            keyboardContainer.setPivotX(keyboardContainer.getWidth() / 2f);
            keyboardContainer.setPivotY(0);
            keyboardContainer.setScaleX(keyboardScale);
            keyboardContainer.setScaleY(keyboardScale);
        });
    }
    
    /**
     * Yoğunluk modu uygula:
     * - compact: daha sıkı, küçük boşluklar
     * - cozy: varsayılan
     * - comfortable: biraz daha ferah
     */
    private void applyDensityMode() {
        try {
            String mode = prefs.getString("density_mode", "cozy");
            float targetScale;
            int padTop;
            int padBottom;
            switch (mode) {
                case "compact":
                    targetScale = Math.max(0.9f, prefs.getFloat("keyboard_scale", 1.0f) - 0.08f);
                    padTop = dp(2);
                    padBottom = dp(4);
                    break;
                case "comfortable":
                    targetScale = Math.min(1.2f, prefs.getFloat("keyboard_scale", 1.0f) + 0.06f);
                    padTop = dp(6);
                    padBottom = dp(8);
                    break;
                case "cozy":
                default:
                    targetScale = prefs.getFloat("keyboard_scale", 1.0f);
                    padTop = dp(4);
                    padBottom = dp(6);
                    break;
            }
            keyboardScale = targetScale;
            if (keyboardContainer != null) {
                int l = keyboardContainer.getPaddingLeft();
                int r = keyboardContainer.getPaddingRight();
                int pLeftRight = dp(6);
                keyboardContainer.setPadding(pLeftRight, padTop, pLeftRight, padBottom);
            }
        } catch (Exception e) {
            Log.w(TAG, "Density mode apply failed", e);
        }
    }

    /**
     * Sistemden/temadan dinamik accent rengi al (Material You benzeri).
     * Çalışmazsa güvenli bir teal (#00ACC1) döner.
     */
    private int getDynamicAccentColor() {
        try {
            // Android 12+ sistem dinamik palet isimleri
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                int[] candidates = new int[] {
                    getColorResIdByName("system_accent1_500"),
                    getColorResIdByName("system_accent1_600"),
                    getColorResIdByName("system_accent1_400"),
                    getColorResIdByName("system_accent2_500"),
                    getColorResIdByName("system_accent3_500")
                };
                for (int resId : candidates) {
                    if (resId != 0) {
                        return getColorCompat(resId);
                    }
                }
            }
            // Tema attribute: colorAccent (legacy) veya material secondary
            int accentAttr = android.R.attr.colorAccent;
            android.util.TypedValue tv = new android.util.TypedValue();
            if (getTheme() != null && getTheme().resolveAttribute(accentAttr, tv, true)) {
                return tv.data;
            }
            int matSecondary = getResources().getIdentifier("colorSecondary", "attr", "com.google.android.material");
            if (matSecondary != 0 && getTheme() != null && getTheme().resolveAttribute(matSecondary, tv, true)) {
                return tv.data;
            }
        } catch (Throwable ignored) { }
        // Güvenli varsayılan teal
        return 0xFF00ACC1;
    }

    private int getColorResIdByName(String name) {
        try {
            return getResources().getIdentifier(name, "color", "android");
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private int getColorCompat(int resId) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                return getResources().getColor(resId, getTheme());
            } else {
                return getResources().getColor(resId);
            }
        } catch (Throwable ignored) {
            return 0xFF00ACC1;
        }
    }

    /**
     * İki rengi oranla karıştır (0..1)
     */
    private int blend(int colorA, int colorB, float ratio) {
        float r = Math.max(0f, Math.min(1f, ratio));
        int aA = (colorA >> 24) & 0xFF;
        int rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8) & 0xFF;
        int bA = (colorA) & 0xFF;
        int aB = (colorB >> 24) & 0xFF;
        int rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8) & 0xFF;
        int bB = (colorB) & 0xFF;
        int a = (int) (aA * (1 - r) + aB * r);
        int rr = (int) (rA * (1 - r) + rB * r);
        int gg = (int) (gA * (1 - r) + gB * r);
        int bb = (int) (bA * (1 - r) + bB * r);
        return (a & 0xFF) << 24 | (rr & 0xFF) << 16 | (gg & 0xFF) << 8 | (bb & 0xFF);
    }

    private void applyOneHandMode(boolean animate) {
        if (keyboardContainer == null) return;
        
        try {
            final float offset;
            switch (oneHandMode) {
                case LEFT:
                    offset = -dp(60); // Sol tarafa kaydır
                    break;
                case RIGHT:
                    offset = dp(60); // Sağ tarafa kaydır
                    break;
                case CENTER:
                default:
                    offset = 0f; // Ortalı
            }
            
            if (animate) {
                final OneHandMode mode = oneHandMode;
                keyboardContainer.animate()
                    .translationX(offset)
                    .setDuration(200)
                    .withEndAction(() -> {
                        Log.d(TAG, "✅ Tek elle mod uygulandı: " + mode + " offset=" + offset);
                    })
                    .start();
                if (featureContainer != null) {
                    featureContainer.animate().translationX(offset).setDuration(200).start();
                }
            } else {
                keyboardContainer.setTranslationX(offset);
                if (featureContainer != null) {
                    featureContainer.setTranslationX(offset);
                }
                Log.d(TAG, "✅ Tek elle mod uygulandı (animasyon yok): " + oneHandMode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Tek elle mod hatası", e);
        }
    }

    private void applyFloatingMode(boolean animate) {
        if (keyboardContainer == null) return;
        
        try {
            float offsetY = floatingEnabled ? -dp(100) : 0f; // Yüksekliği azalttık
            
            if (animate) {
                keyboardContainer.animate()
                    .translationY(offsetY)
                    .setDuration(250)
                    .withStartAction(() -> {
                        if (floatingEnabled) {
                            // Floating mod aktifken rounded background
                            keyboardContainer.setBackground(createThemeBackground(true));
                            keyboardContainer.setElevation(dp(12));
                        }
                    })
                    .withEndAction(() -> {
                        if (!floatingEnabled) {
                            // Normal moda dönerken flat background
                            keyboardContainer.setBackground(createThemeBackground(false));
                            keyboardContainer.setElevation(0);
                        }
                        Log.d(TAG, "✅ Kayan mod uygulandı: " + floatingEnabled);
                    })
                    .start();
                if (featureContainer != null) {
                    featureContainer.animate().translationY(offsetY).setDuration(250).start();
                }
            } else {
                keyboardContainer.setTranslationY(offsetY);
                if (featureContainer != null) {
                    featureContainer.setTranslationY(offsetY);
                }
                applyTheme();
                Log.d(TAG, "✅ Kayan mod uygulandı (animasyon yok): " + floatingEnabled);
            }
        } catch (Exception e) {
            Log.e(TAG, "Kayan mod hatası", e);
        }
    }

    private void cycleTheme() {
        try {
            Log.d(TAG, "🎨🎨🎨 cycleTheme() BAŞLADI!");
            Log.d(TAG, "🎨 Mevcut tema: " + currentTheme);
            
            // Tema döngüsü
            String eskiTema = currentTheme;
            switch (currentTheme) {
                case "default":
                    currentTheme = "dark";
                    break;
                case "dark":
                    currentTheme = "light";
                    break;
                case "light":
                    currentTheme = "colorful";
                    break;
                case "colorful":
                    currentTheme = "default";
                    break;
                default:
                    currentTheme = "default";
                    break;
            }
            
            Log.d(TAG, "🎨 Tema değişti: " + eskiTema + " → " + currentTheme);
            
            // Kaydet
            prefs.edit().putString("keyboard_theme", currentTheme).apply();
            Log.d(TAG, "✅ SharedPreferences'a kaydedildi");
            
            // Uygula - FORCE refresh
            Log.d(TAG, "🎨 applyTheme() çağrılıyor...");
            applyTheme();
            
            // Tüm view'ları yenile
            if (rootInputView != null) {
                rootInputView.postInvalidate();
                Log.d(TAG, "✅ rootInputView yenilendi");
            }
            if (keyboardContainer != null) {
                keyboardContainer.postInvalidate();
                Log.d(TAG, "✅ keyboardContainer yenilendi");
            }
            if (featureContainer != null) {
                featureContainer.postInvalidate();
                Log.d(TAG, "✅ featureContainer yenilendi");
            }
            if (keyboardView != null) {
                keyboardView.invalidateAllKeys();
                Log.d(TAG, "✅ keyboardView.invalidateAllKeys() çağrıldı");
            }
            
            // Feedback - BÜYÜK TOAST
            String temaAdi = getTemaAdi(currentTheme);
            Toast.makeText(this, "🎨 TEMA DEĞİŞTİ: " + temaAdi, Toast.LENGTH_LONG).show();
            
            Log.d(TAG, "🎨🎨🎨 cycleTheme() TAMAMLANDI! Yeni tema: " + currentTheme);
            
        } catch (Exception e) {
            Log.e(TAG, "❌❌❌ Tema değiştirme HATA!", e);
            e.printStackTrace();
            Toast.makeText(this, "❌ Tema hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void showThemeDialog() {
        try {
            Log.d(TAG, "🎨 Modern Tema Dialog açılıyor...");
            
            ModernThemeDialog dialog = new ModernThemeDialog(this, new ModernThemeDialog.ThemeCallback() {
                @Override
                public void onThemeSelected(String theme, String shape) {
                    Log.d(TAG, "✅ Tema seçildi: " + theme + ", Şekil: " + shape);
                    
                    // Save selections - BOTH keys for compatibility
                    SharedPreferences prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
                    prefs.edit()
                        .putString("selected_theme", theme)
                        .putString("selected_shape", shape)
                        .putString("theme", theme)  // applyTheme() uses this key
                        .putString("key_shape", shape)  // For future shape implementation
                        .apply();
                    
                    // Update current theme
                    currentTheme = theme;
                    
                    // Apply theme
                    applyTheme();
                    
                    // Force refresh all views
                    if (rootInputView != null) {
                        rootInputView.postInvalidate();
                    }
                    if (keyboardView != null) {
                        keyboardView.invalidateAllKeys();
                    }
                    
                    Toast.makeText(TurkishKeyboardService.this, 
                        "✨ " + getTemaAdi(theme) + " + " + shape + " uygulandı", Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onGalleryPhotoSelected() {
                    Log.d(TAG, "📷 Galeri fotoğraf seçimi başlatılıyor...");
                    openGalleryForBackground();
                }
            });
            
            dialog.show();
            
            Log.d(TAG, "✅ Modern Tema Dialog gösterildi");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Tema dialog hatası", e);
            Toast.makeText(this, "Tema seçici açılamadı: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    // ====================================================================
    // TEXT EDITING HELPER METHODS
    // ====================================================================
    
    private void moveCursorBy(int offset) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        int currentPos = getCursorPosition(ic);
        int newPos = Math.max(0, Math.min(currentPos + offset, getTotalLength(ic)));
        ic.setSelection(newPos, newPos);
    }
    
    private void moveCursorToEdge(boolean start) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        int target = start ? 0 : getTotalLength(ic);
        ic.setSelection(target, target);
    }

    private void selectAllText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.performContextMenuAction(android.R.id.selectAll);
        Toast.makeText(this, "Seçildi", Toast.LENGTH_SHORT).show();
    }
    
    private void performEditorAction(int actionId, String toastText) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.performContextMenuAction(actionId);
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
    }
    
    private int getCursorPosition(InputConnection ic) {
        CharSequence before = ic.getTextBeforeCursor(1000, 0);
        return before != null ? before.length() : 0;
    }
    
    private int getTotalLength(InputConnection ic) {
        CharSequence before = ic.getTextBeforeCursor(1000, 0);
        CharSequence after = ic.getTextAfterCursor(1000, 0);
        int total = 0;
        if (before != null) total += before.length();
        if (after != null) total += after.length();
        return total;
    }
    
    public void commitTyped(InputConnection inputConnection, String text) {
        if (inputConnection != null) {
            inputConnection.commitText(text, 1);
            saveInputState();
        }
        if (clipboardStore != null) {
            clipboardStore.addText(text);
        }
    }
    
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
    
    private void copyText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.performContextMenuAction(android.R.id.copy);
        Toast.makeText(this, "Kopyalandı", Toast.LENGTH_SHORT).show();
    }
    
    private void openGalleryForBackground() {
        try {
            Log.d(TAG, "📷 Photo Background Selector açılıyor");
            
            // Release old views
            releaseAllViews();
            
            // Show photo selector with preview/crop
            PhotoBackgroundSelector photoSelector = new PhotoBackgroundSelector(this, new PhotoBackgroundSelector.PhotoCallback() {
                @Override
                public void onPhotoApplied(String croppedPhotoPath) {
                    Log.d(TAG, "✅ Fotoğraf uygulandı: " + croppedPhotoPath);
                    
                    // Save cropped photo path for background
                    SharedPreferences prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
                    prefs.edit()
                        .putString("custom_background_path", croppedPhotoPath)
                        .putString("theme", "custom_photo")  // Custom theme
                        .putString("selected_theme", "custom_photo")
                        .apply();
                    
                    // Update theme
                    currentTheme = "custom_photo";
                    
                    // Apply theme with photo
                    applyTheme();
                    
                    // Force refresh
                    if (rootInputView != null) {
                        rootInputView.postInvalidate();
                    }
                    if (keyboardView != null) {
                        keyboardView.invalidateAllKeys();
                    }
                    
                    // Close gallery
                    switchToMode(ViewMode.KEYBOARD);
                    
                    Toast.makeText(TurkishKeyboardService.this, 
                        "✨ Özel arka plan uygulandı", Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onClose() {
                    Log.d(TAG, "📷 Galeri kapatıldı");
                    switchToMode(ViewMode.KEYBOARD);
                }
            });
            
            featureContainer.addView(photoSelector);
            featureContainer.setVisibility(View.VISIBLE);
            currentMode = ViewMode.MENU;  // Use MENU mode for gallery
            
            Log.d(TAG, "✅ Photo Background Selector gösterildi");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Galeri açma hatası", e);
            Toast.makeText(this, "Galeri açılamadı: " + e.getMessage(), Toast.LENGTH_LONG).show();
            switchToMode(ViewMode.KEYBOARD);
        }
    }
    
    private void pasteText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.performContextMenuAction(android.R.id.paste);
        Toast.makeText(this, "Yapıştırıldı", Toast.LENGTH_SHORT).show();
    }
    
    private void cutText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.performContextMenuAction(android.R.id.cut);
        Toast.makeText(this, "Kesildi", Toast.LENGTH_SHORT).show();
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    private void cleanupAllViews() {
        try {
            if (emojiView != null) {
                // emojiView.cleanup(); // Method doesn't exist
                emojiView = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "emojiView cleanup error", e);
        }
        
        try {
            if (gifView != null) {
                // gifView.cleanup(); // Method doesn't exist
                gifView = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "gifView cleanup error", e);
        }
        
        try {
            if (translateView != null) {
                // translateView.cleanup(); // Method doesn't exist
                translateView = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "translateView cleanup error", e);
        }
        
        try {
            // clipboardHistoryView variable doesn't exist
            // if (clipboardHistoryView != null) {
            //     clipboardHistoryView = null;
            // }
        } catch (Exception e) {
            Log.e(TAG, "clipboardHistoryView cleanup error", e);
        }
        
        try {
            if (calculatorView != null) {
                calculatorView = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "calculatorView cleanup error", e);
        }
        
        try {
            if (sharedTypingView != null) {
                sharedTypingView.release();
                sharedTypingView = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "sharedTypingView cleanup error", e);
        }
    }
    
    private String getTemaAdi(String theme) {
        // Tema adını Türkçe döndür
        if (theme == null) return "Koyu";
        switch (theme) {
            case "dark": return "Koyu";
            case "light": return "Açık";
            case "dynamic": return "Dinamik";
            case "midnight": return "Gece Mavisi";
            case "amoled": return "AMOLED";
            case "sunset": return "Gün Batımı";
            case "ocean": return "Okyanus";
            case "aurora": return "Aurora";
            case "fire": return "Ateş";
            case "ice": return "Buz";
            case "gold": return "Altın";
            case "emerald": return "Zümrüt";
            case "neon": return "Neon Pembe";
            default: return theme.substring(0, 1).toUpperCase() + theme.substring(1);
        }
    }
    
    // ====================================================================
    // END OF CLASS
    // ====================================================================
}
