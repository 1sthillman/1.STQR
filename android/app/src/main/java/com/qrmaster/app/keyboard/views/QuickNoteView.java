package com.qrmaster.app.keyboard.views;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 📝 Hızlı Not (Quick Note)
 * 
 * Özellikler:
 * - Klavyeden uzun basma ile açılır
 * - Hızlı not alma
 * - Google Keep, Notion, Obsidian entegrasyonu
 * - Otomatik tarih/saat
 */
public class QuickNoteView extends LinearLayout {
    private static final String TAG = "QuickNoteView";
    
    private Context context;
    private Callback callback;
    
    private EditText titleInput;
    private EditText contentInput;
    
    private boolean isNoteWriteMode = false;
    private EditText activeInput = null; // Hangi alana yazılacak (title veya content)
    
    public interface Callback {
        void onClose();
        void onNoteSaved(String title, String content);
        void onNoteWriteModeChanged(boolean enabled, boolean isTitle);
    }
    
    public QuickNoteView(Context context) {
        super(context);
        init(context);
    }
    
    public QuickNoteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    private void init(Context context) {
        this.context = context;
        
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp(280))); // Kompakt - tutarlı
        setBackgroundColor(0xFF0A0A0A);
        setPadding(dp(8), dp(6), dp(8), dp(6));
        
        createUI();
    }
    
    private void createUI() {
        // Header
        addView(createHeader());
        
        // Title input
        addView(createTitleInput());
        
        // Content input
        addView(createContentInputSection());
        
        // Actions
        addView(createActions());
    }
    
    private LinearLayout createHeader() {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(4), dp(8), dp(4));
        header.setBackgroundColor(0xFF2C2C2E);
        LayoutParams headerParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        headerParams.bottomMargin = dp(6);
        header.setLayoutParams(headerParams);
        
        TextView title = new TextView(context);
        title.setText("Hızlı Not");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);
        
        // Tarih/Saat - daha küçük
        TextView dateView = new TextView(context);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM HH:mm", Locale.getDefault());
        dateView.setText(sdf.format(new Date()));
        dateView.setTextColor(0xFF888888);
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        LayoutParams dateParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        dateParams.rightMargin = dp(8);
        header.addView(dateView, dateParams);
        
        // Kapat - modern
        Button closeBtn = new Button(context);
        closeBtn.setText("✕");
        closeBtn.setTextColor(0xFFFFFFFF);
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(GradientDrawable.OVAL);
        closeBg.setColor(0xFF48484A);
        closeBtn.setBackground(closeBg);
        LayoutParams closeParams = new LayoutParams(dp(28), dp(28));
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> {
            if (callback != null) callback.onClose();
        });
        header.addView(closeBtn);
        
        return header;
    }
    
    private LinearLayout createTitleInput() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setPadding(0, 0, 0, dp(8));
        
        LinearLayout topBar = new LinearLayout(context);
        topBar.setOrientation(HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView label = new TextView(context);
        label.setText("📌 Başlık:");
        label.setTextColor(0xFFFFD700);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        LayoutParams labelParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        topBar.addView(label, labelParams);
        
        // ⌨️ BAŞLIK YAZ butonu
        Button writeBtn = createSmallButton("⌨️");
        writeBtn.setOnClickListener(v -> {
            boolean newState = !isNoteWriteMode || activeInput != titleInput;
            setNoteWriteMode(newState, true); // true = title
            
            if (newState) {
                writeBtn.setText("✅");
                GradientDrawable activeBg = new GradientDrawable();
                activeBg.setColor(0xFF332200);
                activeBg.setCornerRadius(dp(6));
                activeBg.setStroke(dp(2), 0xFFFFD700);
                writeBtn.setBackground(activeBg);
                Toast.makeText(context, "✅ Başlık yazma modu AÇIK", Toast.LENGTH_SHORT).show();
            } else {
                writeBtn.setText("⌨️");
                GradientDrawable normalBg = new GradientDrawable();
                normalBg.setColor(0xFF2C2C2E);
                normalBg.setCornerRadius(dp(6));
                writeBtn.setBackground(normalBg);
                Toast.makeText(context, "⌨️ Başlık yazma modu KAPALI", Toast.LENGTH_SHORT).show();
            }
        });
        topBar.addView(writeBtn);
        
        container.addView(topBar);
        
        titleInput = new EditText(context);
        titleInput.setHint("⌨️ butonuna basıp yazın...");
        titleInput.setTextColor(Color.WHITE);
        titleInput.setHintTextColor(0xFF666666);
        titleInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12); // Daha küçük
        titleInput.setPadding(dp(8), dp(6), dp(8), dp(6)); // Daha kompakt
        titleInput.setSingleLine(true);
        
        // Klavye açılmasın!
        titleInput.setFocusable(false);
        titleInput.setClickable(false);
        titleInput.setCursorVisible(false);
        
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(0xFF1C1C1E);
        inputBg.setCornerRadius(dp(8));
        inputBg.setStroke(dp(1), 0xFFFFD700);
        titleInput.setBackground(inputBg);
        
        LayoutParams inputParams = new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        inputParams.topMargin = dp(4);
        container.addView(titleInput, inputParams);
        
        return container;
    }
    
    private LinearLayout createContentInputSection() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setPadding(0, 0, 0, dp(8));
        
        LinearLayout topBar = new LinearLayout(context);
        topBar.setOrientation(HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView label = new TextView(context);
        label.setText("📝 Not:");
        label.setTextColor(0xFFFFD700);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        LayoutParams labelParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        topBar.addView(label, labelParams);
        
        // ⌨️ NOT YAZ butonu
        Button writeBtn = createSmallButton("⌨️");
        writeBtn.setOnClickListener(v -> {
            boolean newState = !isNoteWriteMode || activeInput != contentInput;
            setNoteWriteMode(newState, false); // false = content
            
            if (newState) {
                writeBtn.setText("✅");
                GradientDrawable activeBg = new GradientDrawable();
                activeBg.setColor(0xFF332200);
                activeBg.setCornerRadius(dp(6));
                activeBg.setStroke(dp(2), 0xFFFFD700);
                writeBtn.setBackground(activeBg);
                Toast.makeText(context, "✅ Not yazma modu AÇIK", Toast.LENGTH_SHORT).show();
            } else {
                writeBtn.setText("⌨️");
                GradientDrawable normalBg = new GradientDrawable();
                normalBg.setColor(0xFF2C2C2E);
                normalBg.setCornerRadius(dp(6));
                writeBtn.setBackground(normalBg);
                Toast.makeText(context, "⌨️ Not yazma modu KAPALI", Toast.LENGTH_SHORT).show();
            }
        });
        topBar.addView(writeBtn);
        
        container.addView(topBar);
        
        // ScrollView için content input - kompakt
        ScrollView scrollView = new ScrollView(context);
        LayoutParams scrollParams = new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(110) // Daha kompakt - 280dp'ye sığdırmak için
        );
        scrollParams.topMargin = dp(4);
        
        contentInput = new EditText(context);
        contentInput.setHint("⌨️ butonuna basıp yazın...");
        contentInput.setTextColor(Color.WHITE);
        contentInput.setHintTextColor(0xFF666666);
        contentInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12); // Daha küçük
        contentInput.setPadding(dp(8), dp(8), dp(8), dp(8)); // Daha kompakt
        contentInput.setGravity(Gravity.TOP | Gravity.START);
        contentInput.setMinLines(5); // Daha az satır
        
        // Klavye açılmasın!
        contentInput.setFocusable(false);
        contentInput.setClickable(false);
        contentInput.setCursorVisible(false);
        
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(0xFF1C1C1E);
        inputBg.setCornerRadius(dp(8));
        inputBg.setStroke(dp(1), 0xFFFFD700);
        contentInput.setBackground(inputBg);
        
        scrollView.addView(contentInput);
        container.addView(scrollView, scrollParams);
        
        return container;
    }
    
    private LinearLayout createActions() {
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, dp(12), 0, 0);
        
        // Google Keep
        Button keepBtn = createActionButton("Keep", 0xFFFBC02D);
        keepBtn.setOnClickListener(v -> saveToKeep());
        actions.addView(keepBtn);
        
        // Notion (Intent)
        Button notionBtn = createActionButton("Notion", 0xFF000000);
        notionBtn.setOnClickListener(v -> saveToNotion());
        actions.addView(notionBtn);
        
        // Pano
        Button clipboardBtn = createActionButton("📋 Kopyala", 0xFF2196F3);
        clipboardBtn.setOnClickListener(v -> saveToClipboard());
        actions.addView(clipboardBtn);
        
        return actions;
    }
    
    private Button createButton(String text, int color) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setPadding(dp(8), dp(4), dp(8), dp(4));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(6));
        btn.setBackground(bg);
        
        LayoutParams params = new LayoutParams(dp(36), dp(32));
        params.leftMargin = dp(6);
        btn.setLayoutParams(params);
        
        return btn;
    }
    
    private Button createActionButton(String text, int color) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setPadding(dp(16), dp(10), dp(16), dp(10));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(2), 0xFFFFD700);
        btn.setBackground(bg);
        
        LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = dp(4);
        params.rightMargin = dp(4);
        btn.setLayoutParams(params);
        
        return btn;
    }
    
    /**
     * Küçük buton oluştur (⌨️, ✕ gibi)
     */
    private Button createSmallButton(String text) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setPadding(dp(8), dp(4), dp(8), dp(4));
        btn.setMinWidth(0);
        btn.setMinimumWidth(0);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF2C2C2E);
        bg.setCornerRadius(dp(6));
        btn.setBackground(bg);
        
        LayoutParams params = new LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = dp(6);
        btn.setLayoutParams(params);
        
        return btn;
    }
    
    /**
     * Google Keep'e kaydet
     */
    private void saveToKeep() {
        String title = titleInput.getText().toString().trim();
        String content = contentInput.getText().toString().trim();
        
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(context, "❌ Not içeriği boş!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Google Keep intent
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, title);
            intent.putExtra(Intent.EXTRA_TEXT, content);
            intent.setPackage("com.google.android.keep");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(intent);
            Toast.makeText(context, "✅ Google Keep açıldı!", Toast.LENGTH_SHORT).show();
            
            if (callback != null) {
                callback.onNoteSaved(title, content);
            }
        } catch (Exception e) {
            Toast.makeText(context, "❌ Google Keep bulunamadı!\nYüklü mü?", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Notion'a kaydet (web link)
     */
    private void saveToNotion() {
        String title = titleInput.getText().toString().trim();
        String content = contentInput.getText().toString().trim();
        
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(context, "❌ Not içeriği boş!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Notion web quick add
            String encodedContent = Uri.encode(title + "\n\n" + content);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.notion.so/"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(intent);
            
            // Panoya da kopyala
            android.content.ClipboardManager clipboard = 
                (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("note", title + "\n\n" + content);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
            }
            
            Toast.makeText(context, "✅ Notion açıldı!\nNot panoda, yapıştırın!", Toast.LENGTH_LONG).show();
            
            if (callback != null) {
                callback.onNoteSaved(title, content);
            }
        } catch (Exception e) {
            Toast.makeText(context, "❌ Notion açılamadı!", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Panoya kopyala
     */
    private void saveToClipboard() {
        String title = titleInput.getText().toString().trim();
        String content = contentInput.getText().toString().trim();
        
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(context, "❌ Not içeriği boş!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String fullNote = "📝 " + (!TextUtils.isEmpty(title) ? title : "Hızlı Not") + 
                         "\n⏰ " + sdf.format(new Date()) + 
                         "\n\n" + content;
        
        android.content.ClipboardManager clipboard = 
            (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("note", fullNote);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "✅ Not panoya kopyalandı!", Toast.LENGTH_SHORT).show();
            
            if (callback != null) {
                callback.onNoteSaved(title, content);
            }
        } else {
            Toast.makeText(context, "❌ Panoya erişilemiyor!", Toast.LENGTH_SHORT).show();
        }
    }
    
    private int dp(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            context.getResources().getDisplayMetrics()
        );
    }
    
    public void setCallback(Callback callback) {
        this.callback = callback;
    }
    
    // ============================================================
    // KLAVYE YAZMA MODU (CryptoView benzeri)
    // ============================================================
    
    /**
     * Not yazma modunu aç/kapat
     */
    public void setNoteWriteMode(boolean enabled, boolean isTitle) {
        this.isNoteWriteMode = enabled;
        this.activeInput = enabled ? (isTitle ? titleInput : contentInput) : null;
        
        if (callback != null) {
            callback.onNoteWriteModeChanged(enabled, isTitle);
        }
    }
    
    /**
     * Aktif input'a text ekle (TurkishKeyboardService'den çağrılır)
     */
    public void appendToNote(String text) {
        if (activeInput != null) {
            String current = activeInput.getText().toString();
            activeInput.setText(current + text);
        }
    }
    
    /**
     * Aktif input'tan son karakteri sil (BACKSPACE)
     */
    public void deleteLastChar() {
        if (activeInput != null) {
            String current = activeInput.getText().toString();
            if (current.length() > 0) {
                activeInput.setText(current.substring(0, current.length() - 1));
            }
        }
    }
    
    /**
     * Aktif input'a space ekle
     */
    public void appendSpace() {
        if (activeInput != null) {
            String current = activeInput.getText().toString();
            activeInput.setText(current + " ");
        }
    }
    
    /**
     * Aktif input'a yeni satır ekle (ENTER)
     */
    public void appendNewLine() {
        if (activeInput != null && activeInput == contentInput) {
            String current = activeInput.getText().toString();
            activeInput.setText(current + "\n");
        }
    }
}

