package com.qrmaster.app.keyboard.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.qrmaster.app.keyboard.LanguageManager;
import com.qrmaster.app.keyboard.LanguageManager.Language;

public class LanguageSwitcherView extends LinearLayout {
    
    public interface Callback {
        void onLanguageSelected(Language language);
        void onClose();
    }
    
    private final Callback callback;
    private final LanguageManager languageManager;
    
    public LanguageSwitcherView(Context context, LanguageManager languageManager, Callback callback) {
        super(context);
        this.callback = callback;
        this.languageManager = languageManager;
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp(200)));
        setBackgroundColor(0xFF1C1C1E);
        setPadding(dp(16), dp(12), dp(16), dp(12));
        
        // Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView title = new TextView(context);
        title.setText("🌍 Dil Seçin");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);
        
        // Close button
        Button closeBtn = createSmallButton(context, "❌");
        closeBtn.setOnClickListener(v -> callback.onClose());
        header.addView(closeBtn);
        
        addView(header);
        
        // Language buttons
        LinearLayout langGrid = new LinearLayout(context);
        langGrid.setOrientation(HORIZONTAL);
        langGrid.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams gridParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        gridParams.topMargin = dp(12);
        
        Language currentLang = languageManager.getCurrentLanguage();
        
        for (Language lang : languageManager.getAllLanguages()) {
            LinearLayout langCard = createLanguageCard(context, lang, lang == currentLang);
            langCard.setOnClickListener(v -> {
                callback.onLanguageSelected(lang);
            });
            
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            cardParams.setMargins(dp(4), 0, dp(4), 0);
            langGrid.addView(langCard, cardParams);
        }
        
        addView(langGrid, gridParams);
    }
    
    private LinearLayout createLanguageCard(Context context, Language lang, boolean isActive) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(12), dp(16), dp(12), dp(16));
        
        GradientDrawable bg = new GradientDrawable();
        if (isActive) {
            bg.setColor(0xFF007AFF); // Active - Blue
        } else {
            bg.setColor(0xFF2C2C2E); // Inactive - Dark
        }
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(2), isActive ? 0xFF0055CC : 0xFF3A3A3C);
        card.setBackground(bg);
        
        // Flag
        TextView flag = new TextView(context);
        flag.setText(lang.flag);
        flag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        flag.setGravity(Gravity.CENTER);
        card.addView(flag);
        
        // Language name
        TextView name = new TextView(context);
        name.setText(lang.name);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        name.setTextColor(Color.WHITE);
        name.setTypeface(null, Typeface.BOLD);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nameParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = dp(8);
        card.addView(name, nameParams);
        
        // Code
        TextView code = new TextView(context);
        code.setText(lang.code.toUpperCase());
        code.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        code.setTextColor(0xFFAAAAAA);
        code.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams codeParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        codeParams.topMargin = dp(4);
        card.addView(code, codeParams);
        
        if (isActive) {
            TextView active = new TextView(context);
            active.setText("✓ Aktif");
            active.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            active.setTextColor(0xFF34C759);
            active.setTypeface(null, Typeface.BOLD);
            active.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams activeParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            activeParams.topMargin = dp(6);
            card.addView(active, activeParams);
        }
        
        return card;
    }
    
    private Button createSmallButton(Context context, String text) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setPadding(dp(12), dp(6), dp(12), dp(6));
        btn.setMinWidth(0);
        btn.setMinHeight(0);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFF3B30);
        bg.setCornerRadius(dp(8));
        btn.setBackground(bg);
        
        return btn;
    }
    
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}








