package com.qrmaster.app.keyboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.qrmaster.app.R;

/**
 * Klavye ayarları ekranı - Gboard tarzı
 * Tüm klavye özellikleri buradan yönetiliyor
 */
public class KeyboardSettingsActivity extends AppCompatActivity {
    
    private static final int REQUEST_CODE_PICK_IMAGE = 1001;
    
    private SharedPreferences prefs;
    
    // Switches
    private Switch switchVibrate;
    private Switch switchSound;
    private Switch switchPredictive;
    private Switch switchAutocorrect;
    private Switch switchEmoji;
    private Switch switchSwipe;
    
    // TextViews
    private TextView tvCurrentLang;
    private TextView tvCurrentTheme;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyboard_settings);
        
        prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
        
        initViews();
        loadSettings();
        setupListeners();
        
        // 📷 Custom tema için galeri aç (TurkishKeyboardService'den intent ile geldiyse)
        if (getIntent().getBooleanExtra("open_custom_theme", false)) {
            openGalleryForCustomTheme();
        }
    }
    
    /**
     * Galeri aç - Custom tema için fotoğraf seç
     */
    private void openGalleryForCustomTheme() {
        // Galeri izni kontrolü
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "🖼️ Galeri izni gerekli!\n\nAyarlar → İzinler → Fotoğraflar", Toast.LENGTH_LONG).show();
                requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 999);
                return;
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "🖼️ Galeri izni gerekli!\n\nAyarlar → İzinler → Depolama", Toast.LENGTH_LONG).show();
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 999);
                return;
            }
        }
        
        // Galeri intent
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        Toast.makeText(this, "📷 Klavye arka planı için fotoğraf seçin...", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            android.net.Uri imageUri = data.getData();
            if (imageUri != null) {
                // URI'yi SharedPreferences'a kaydet
                prefs.edit()
                    .putString("custom_photo_uri", imageUri.toString())
                    .putString("theme", "custom") // Temayı custom yap
                    .apply();
                
                Toast.makeText(this, "✅ Custom tema kaydedildi!", Toast.LENGTH_SHORT).show();
                
                // Klavye servisine broadcast gönder
                Intent broadcastIntent = new Intent("com.qrmaster.app.THEME_CHANGED");
                sendBroadcast(broadcastIntent);
                
                // Activity'yi kapat - klavyeye dön
                finish();
            }
        }
    }
    
    private void initViews() {
        // Header
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
        
        // Switches
        switchVibrate = findViewById(R.id.switch_vibrate);
        switchSound = findViewById(R.id.switch_sound);
        switchPredictive = findViewById(R.id.switch_predictive);
        switchAutocorrect = findViewById(R.id.switch_autocorrect);
        switchEmoji = findViewById(R.id.switch_emoji);
        switchSwipe = findViewById(R.id.switch_swipe);
        
        // TextViews
        tvCurrentLang = findViewById(R.id.tv_current_lang);
        tvCurrentTheme = findViewById(R.id.tv_current_theme);
    }
    
    private void loadSettings() {
        switchVibrate.setChecked(prefs.getBoolean("vibrate", true));
        switchSound.setChecked(prefs.getBoolean("sound", false));
        switchPredictive.setChecked(prefs.getBoolean("predictive", true));
        switchAutocorrect.setChecked(prefs.getBoolean("auto_correct", true));
        switchEmoji.setChecked(prefs.getBoolean("emoji_suggestions", true));
        switchSwipe.setChecked(prefs.getBoolean("swipe_typing", false));
        
        updateThemeText();
    }
    
    private void setupListeners() {
        // Auto-save switches
        switchVibrate.setOnCheckedChangeListener((v, checked) -> {
            prefs.edit().putBoolean("vibrate", checked).apply();
            showToast("Titreşim " + (checked ? "açık" : "kapalı"));
        });
        
        switchSound.setOnCheckedChangeListener((v, checked) -> {
            prefs.edit().putBoolean("sound", checked).apply();
            showToast("Ses " + (checked ? "açık" : "kapalı"));
        });
        
        switchPredictive.setOnCheckedChangeListener((v, checked) -> {
            prefs.edit().putBoolean("predictive", checked).apply();
            showToast("Tahminli metin " + (checked ? "açık" : "kapalı"));
        });
        
        switchAutocorrect.setOnCheckedChangeListener((v, checked) -> {
            prefs.edit().putBoolean("auto_correct", checked).apply();
            showToast("Otomatik düzeltme " + (checked ? "açık" : "kapalı"));
        });
        
        switchEmoji.setOnCheckedChangeListener((v, checked) -> {
            prefs.edit().putBoolean("emoji_suggestions", checked).apply();
            showToast("Emoji önerileri " + (checked ? "açık" : "kapalı"));
        });
        
        switchSwipe.setOnCheckedChangeListener((v, checked) -> {
            prefs.edit().putBoolean("swipe_typing", checked).apply();
            showToast("Kaydırarak yazma " + (checked ? "açık" : "kapalı"));
        });
        
        // Clickable sections
        findViewById(R.id.section_languages).setOnClickListener(v -> {
            showToast("🌐 Dil ayarları - Yakında!");
        });
        
        findViewById(R.id.section_theme).setOnClickListener(v -> {
            showThemeDialog();
        });
        
        findViewById(R.id.section_voice).setOnClickListener(v -> {
            showToast("🎤 Sesle yazma ayarları");
        });
        
        findViewById(R.id.section_clipboard).setOnClickListener(v -> {
            showToast("📋 Pano geçmişi ayarları");
        });
        
        findViewById(R.id.section_dictionary).setOnClickListener(v -> {
            showToast("📖 Kişisel sözlük - Yakında!");
        });
        
        findViewById(R.id.section_emoji_gif).setOnClickListener(v -> {
            showToast("😊 Emoji, çıkartma ve GIF ayarları");
        });
        
        findViewById(R.id.section_share).setOnClickListener(v -> {
            shareKeyboard();
        });
        
        findViewById(R.id.section_privacy).setOnClickListener(v -> {
            showToast("🔒 Gizlilik ayarları");
        });
    }
    
    private void updateThemeText() {
        String theme = prefs.getString("theme", "dark");
        tvCurrentTheme.setText(getThemeName(theme));
    }
    
    private String getThemeName(String theme) {
        switch (theme) {
            case "dark": return "🌙 Koyu";
            case "light": return "☀️ Açık";
            case "nord": return "❄️ Nord";
            case "dracula": return "🧛 Dracula";
            case "monokai": return "🎨 Monokai";
            case "solarized": return "🌊 Solarized";
            case "gruvbox": return "🍂 Gruvbox";
            case "cyberpunk": return "🌃 Cyberpunk";
            case "tokyo": return "🌸 Tokyo Night";
            case "atom": return "🔥 Atom One";
            case "material": return "🌌 Material";
            case "palenight": return "💜 Palenight";
            case "owl": return "🎭 Night Owl";
            case "espresso": return "☕ Espresso";
            case "synthwave": return "🌈 Synthwave";
            default: return "🌙 Koyu";
        }
    }
    
    private void showThemeDialog() {
        String currentTheme = prefs.getString("theme", "dark");
        
        // 15 Modern Tema!
        String[] themes = {
            "🌙 Koyu", 
            "☀️ Açık", 
            "❄️ Nord", 
            "🧛 Dracula", 
            "🎨 Monokai",
            "🌊 Solarized",
            "🍂 Gruvbox",
            "🌃 Cyberpunk",
            "🌸 Tokyo Night",
            "🔥 Atom One Dark",
            "🌌 Material",
            "💜 Palenight",
            "🎭 Night Owl",
            "☕ Espresso",
            "🌈 Synthwave"
        };
        
        String[] themeKeys = {
            "dark", "light", "nord", "dracula", "monokai",
            "solarized", "gruvbox", "cyberpunk", "tokyo",
            "atom", "material", "palenight", "owl", "espresso", "synthwave"
        };
        
        // Mevcut tema index'ini bul
        int currentIndex = 0;
        for (int i = 0; i < themeKeys.length; i++) {
            if (themeKeys[i].equals(currentTheme)) {
                currentIndex = i;
                break;
            }
        }
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("🎨 Tema Seç (15 Tema)");
        builder.setSingleChoiceItems(themes, currentIndex, (dialog, which) -> {
            String selectedTheme = themeKeys[which];
            prefs.edit().putString("theme", selectedTheme).apply();
            
            // Klavyeyi yeniden başlat (broadcast gönder)
            Intent intent = new Intent("com.qrmaster.app.THEME_CHANGED");
            intent.putExtra("theme", selectedTheme);
            sendBroadcast(intent);
            
            updateThemeText();
            showToast("✅ Tema: " + themes[which]);
            dialog.dismiss();
            
            // Activity'yi kapat - klavye otomatik yenilenecek
            finish();
        });
        builder.setNegativeButton("İptal", null);
        builder.show();
    }
    
    private void shareKeyboard() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "1STQR Türkçe Q Klavye");
        shareIntent.putExtra(Intent.EXTRA_TEXT, 
            "1STQR Türkçe Q Klavye'yi kullanıyorum!\n\n" +
            "✨ QR Tarama\n" +
            "😊 Emoji & GIF\n" +
            "🌐 Çeviri\n" +
            "🎤 Sesle Yazma\n" +
            "ve daha fazlası!");
        startActivity(Intent.createChooser(shareIntent, "Klavyeyi Paylaş"));
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
