package com.qrmaster.app.keyboard;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qrmaster.app.R;

import java.util.ArrayList;
import java.util.List;

public class QuickMenuView extends LinearLayout {

    public enum QuickAction {
        THEME,
        ONE_HANDED,
        TEXT_EDIT,
        FLOATING,
        RESIZE,
        EMOJI,
        GIF,
        STICKER,
        CLIPBOARD,
        TRANSLATE,
        CRYPTO,
        MOUSE,
        CAMERA,
        VOICE,
        SETTINGS,
        LANGUAGE,
        TEXT_TEMPLATE,
        QUICK_SEARCH,
        QUICK_NOTE,
        TEXT_EXPANDER,
        CLOSE
    }

    public interface QuickMenuCallback {
        void onQuickActionSelected(QuickAction action);
    }

    private final QuickMenuCallback callback;

    public QuickMenuView(Context context, QuickMenuCallback callback) {
        super(context);
        this.callback = callback;
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ));
        
        // Ultra Modern Glassmorphism Background
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            new int[]{0xE01C1C1E, 0xE0000000, 0xE01A1A1A}
        );
        bg.setCornerRadius(dpToPx(24));
        bg.setStroke(dpToPx(1), 0x40FFFFFF);
        setBackground(bg);
        setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(16));
        setElevation(dpToPx(8));

        // Header with close button
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams headerParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        headerParams.bottomMargin = dpToPx(12);
        
        TextView title = new TextView(context);
        title.setText("Hızlı Menü");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setLetterSpacing(0.03f);
        LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);
        
        // Close button
        TextView closeBtn = new TextView(context);
        closeBtn.setText("✕");
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        closeBtn.setTextColor(0xFFFF3B30);
        closeBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        closeBtn.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable closeBg = new android.graphics.drawable.GradientDrawable();
        closeBg.setColor(0xFF2C2C2E);
        closeBg.setCornerRadius(dpToPx(8));
        closeBtn.setBackground(closeBg);
        closeBtn.setOnClickListener(v -> {
            if (callback != null) {
                callback.onQuickActionSelected(QuickAction.CLOSE);
            }
        });
        header.addView(closeBtn);
        
        addView(header, headerParams);
        
        // SCROLLVIEW ekle - KOMPAKT!
        android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
        LayoutParams scrollParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            dpToPx(240) // KOMPAKT: 240dp max yükseklik
        );
        
        LinearLayout contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(VERTICAL);

        // KOMPAKT 3-SÜTUN LAYOUT - TEMİZ, EMOJİSİZ
        // Row 1: Featured Items
        LinearLayout row1 = createRow(context);
        row1.addView(createProItem(context, R.drawable.ic_palette, "Tema", QuickAction.THEME, 0xFF666666));
        row1.addView(createProItem(context, R.drawable.ic_ocr, "Arama", QuickAction.QUICK_SEARCH, 0xFF666666));
        row1.addView(createProItem(context, R.drawable.ic_edit, "Not", QuickAction.QUICK_NOTE, 0xFF666666));
        contentContainer.addView(row1);
        
        // Row 2: Text Tools
        LinearLayout row2 = createRow(context);
        row2.addView(createProItem(context, R.drawable.ic_text_template, "Kısayol", QuickAction.TEXT_EXPANDER, 0xFF666666));
        row2.addView(createProItem(context, R.drawable.ic_crypto, "Şifre", QuickAction.CRYPTO, 0xFF666666));
        row2.addView(createProItem(context, R.drawable.ic_emoji, "Emoji", QuickAction.EMOJI, 0xFF666666));
        contentContainer.addView(row2);

        // Row 3: Media
        LinearLayout row3 = createRow(context);
        row3.addView(createProItem(context, R.drawable.ic_gif, "GIF", QuickAction.GIF, 0xFF666666));
        row3.addView(createProItem(context, R.drawable.ic_clipboard, "Pano", QuickAction.CLIPBOARD, 0xFF666666));
        row3.addView(createProItem(context, R.drawable.ic_translate, "Çeviri", QuickAction.TRANSLATE, 0xFF666666));
        contentContainer.addView(row3);
        
        // Row 4: More Tools
        LinearLayout row4 = createRow(context);
        row4.addView(createProItem(context, R.drawable.ic_camera, "QR", QuickAction.CAMERA, 0xFF666666));
        row4.addView(createProItem(context, R.drawable.ic_mic, "Ses", QuickAction.VOICE, 0xFF666666));
        row4.addView(createProItem(context, R.drawable.ic_settings, "Ayarlar", QuickAction.SETTINGS, 0xFF666666));
        contentContainer.addView(row4);
        
        scrollView.addView(contentContainer);
        addView(scrollView, scrollParams);
    }

    private LinearLayout createRow(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LayoutParams params = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dpToPx(8);
        row.setLayoutParams(params);
        return row;
    }
    
    /**
     * PRO ITEM - Tek renk, minimal, profesyonel (EMOJİSİZ)
     */
    private View createProItem(Context context, int iconRes, String label, QuickAction action, int color) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setGravity(Gravity.CENTER);
        LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        container.setLayoutParams(params);
        container.setPadding(dpToPx(10), dpToPx(14), dpToPx(10), dpToPx(14));
        
        // Minimal dark background
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFF2A2A2A);
        bg.setCornerRadius(dpToPx(10));
        bg.setStroke(dpToPx(1), 0xFF3A3A3A);
        container.setBackground(bg);
        
        // Icon
        android.widget.ImageView icon = new android.widget.ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(0xFFCCCCCC);
        LayoutParams iconParams = new LayoutParams(dpToPx(28), dpToPx(28));
        iconParams.bottomMargin = dpToPx(6);
        container.addView(icon, iconParams);
        
        // Label
        TextView text = new TextView(context);
        text.setText(label);
        text.setTextColor(0xFFCCCCCC);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        text.setGravity(Gravity.CENTER);
        text.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        container.addView(text);
        
        container.setOnClickListener(v -> {
            if (callback != null) {
                callback.onQuickActionSelected(action);
            }
        });
        
        return container;
    }

    private View createSafeItem(Context context, int iconRes, String label, QuickAction action) {
        try {
            return createItem(context, iconRes, label, action);
        } catch (Exception e) {
            android.util.Log.e("QuickMenuView", "Error creating item: " + label, e);
            return new View(context);
        }
    }
    
    /**
     * Sadece text ile item oluştur (icon olmadan - yeni özellikler için)
     */
    private View createTextOnlyItem(Context context, String label, QuickAction action) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setGravity(Gravity.CENTER);
        LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        container.setLayoutParams(params);
        container.setPadding(dpToPx(12), dpToPx(16), dpToPx(12), dpToPx(16));
        
        // Premium gradient background
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF2C2C2E, 0xFF1C1C1E}
        );
        bg.setCornerRadius(dpToPx(12));
        bg.setStroke(dpToPx(2), 0x60FFFFFF);
        container.setBackground(bg);
        
        TextView text = new TextView(context);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        text.setGravity(Gravity.CENTER);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(text);
        
        container.setOnClickListener(v -> {
            if (callback != null) {
                callback.onQuickActionSelected(action);
            }
        });
        
        return container;
    }
    
    /**
     * KOMPAKT GLOW ITEM - Emoji + Text, parlak border
     */
    private View createCompactGlowItem(Context context, String emoji, String label, QuickAction action, int glowColor) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setGravity(Gravity.CENTER);
        LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dpToPx(3), 0, dpToPx(3), 0);
        container.setLayoutParams(params);
        container.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));
        
        // Modern gradient dengan glow
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            new int[]{(glowColor & 0x40FFFFFF), 0xFF1A1A1A}
        );
        bg.setCornerRadius(dpToPx(14));
        bg.setStroke(dpToPx(2), glowColor);
        container.setBackground(bg);
        
        // Emoji
        TextView emojiText = new TextView(context);
        emojiText.setText(emoji);
        emojiText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        emojiText.setGravity(Gravity.CENTER);
        container.addView(emojiText);
        
        // Label
        TextView text = new TextView(context);
        text.setText(label);
        text.setTextColor(glowColor);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        text.setGravity(Gravity.CENTER);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        text.setShadowLayer(6, 0, 0, glowColor);
        LayoutParams textParams = new LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        );
        textParams.topMargin = dpToPx(4);
        container.addView(text, textParams);
        
        container.setOnClickListener(v -> {
            if (callback != null) {
                callback.onQuickActionSelected(action);
            }
        });
        
        return container;
    }
    
    /**
     * KOMPAKT NORMAL ITEM - Icon + Text
     */
    private View createCompactItem(Context context, int iconRes, String label, QuickAction action) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setGravity(Gravity.CENTER);
        LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dpToPx(3), 0, dpToPx(3), 0);
        container.setLayoutParams(params);
        container.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));
        
        // Modern subtle gradient
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF2C2C2E, 0xFF1C1C1E}
        );
        bg.setCornerRadius(dpToPx(14));
        bg.setStroke(dpToPx(1), 0x30FFFFFF);
        container.setBackground(bg);
        
        // Icon
        try {
            CompactIconView icon = new CompactIconView(context);
            icon.setImageResource(iconRes);
            container.addView(icon, new LayoutParams(dpToPx(36), dpToPx(36)));
        } catch (Exception e) {
            android.util.Log.e("QuickMenuView", "Error loading icon", e);
        }
        
        // Label
        TextView text = new TextView(context);
        text.setText(label);
        text.setTextColor(0xFFFFFFFF);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        text.setGravity(Gravity.CENTER);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        LayoutParams textParams = new LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        );
        textParams.topMargin = dpToPx(4);
        container.addView(text, textParams);
        
        container.setOnClickListener(v -> {
            if (callback != null) {
                callback.onQuickActionSelected(action);
            }
        });
        
        return container;
    }
    
    /**
     * BÜYÜK VE PARLAK TEXT BUTON (Yeni özellikler için)
     */
    private View createBigTextItem(Context context, String label, QuickAction action, int glowColor) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setGravity(Gravity.CENTER);
        LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        container.setLayoutParams(params);
        container.setPadding(dpToPx(16), dpToPx(20), dpToPx(16), dpToPx(20));
        
        // PARLAK gradient background
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            new int[]{glowColor & 0x60FFFFFF, 0xFF1C1C1E} // Yarı saydam renkten koyuya
        );
        bg.setCornerRadius(dpToPx(16));
        bg.setStroke(dpToPx(3), glowColor); // Kalın parlak border
        container.setBackground(bg);
        
        TextView text = new TextView(context);
        text.setText(label);
        text.setTextColor(glowColor); // Parlak renk
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16); // BÜYÜK
        text.setGravity(Gravity.CENTER);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        text.setShadowLayer(10, 0, 0, glowColor); // GLOW efekti!
        container.addView(text);
        
        container.setOnClickListener(v -> {
            android.util.Log.d("QuickMenuView", "🎨 BigTextItem TIKLANDI! Label: " + label + " Action: " + action);
            if (callback != null) {
                android.util.Log.d("QuickMenuView", "✅ Callback çağrılıyor: " + action);
                callback.onQuickActionSelected(action);
            } else {
                android.util.Log.e("QuickMenuView", "❌ Callback NULL!");
            }
        });
        
        return container;
    }

    private View createItem(Context context, int iconRes, String label, QuickAction action) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setGravity(Gravity.CENTER);
        LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        container.setLayoutParams(params);
        container.setPadding(0, dpToPx(6), 0, dpToPx(6));

        try {
            RippleImageView icon = new RippleImageView(context);
            icon.setImageResource(iconRes);
            // Daha büyük icon - 48dp
            container.addView(icon, new LayoutParams(dpToPx(52), dpToPx(52)));
        } catch (Exception e) {
            android.util.Log.e("QuickMenuView", "Error loading icon", e);
        }

        TextView text = new TextView(context);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        text.setGravity(Gravity.CENTER);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        LayoutParams textParams = new LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        );
        textParams.topMargin = dpToPx(6);
        container.addView(text, textParams);

        container.setOnClickListener(v -> {
            if (callback != null) {
                try {
                    callback.onQuickActionSelected(action);
                } catch (Exception e) {
                    android.util.Log.e("QuickMenuView", "Error in callback", e);
                }
            }
        });

        return container;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }

    /** Kompakt icon view */
    private static class CompactIconView extends androidx.appcompat.widget.AppCompatImageView {
        public CompactIconView(Context context) {
            super(context);
            int padding = (int) (context.getResources().getDisplayMetrics().density * 6);
            setPadding(padding, padding, padding, padding);
            setScaleType(ScaleType.CENTER_INSIDE);
            
            // Compact circular background
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            bg.setColor(0xFF3A3A3C);
            bg.setStroke((int)(context.getResources().getDisplayMetrics().density * 1), 0x20FFFFFF);
            setBackground(bg);
            
            // Icon tint
            setColorFilter(0xFFFFFFFF);
        }
    }
    
    /** Premium circular icon view with gradient */
    private static class RippleImageView extends androidx.appcompat.widget.AppCompatImageView {
        public RippleImageView(Context context) {
            super(context);
            int padding = (int) (context.getResources().getDisplayMetrics().density * 10);
            setPadding(padding, padding, padding, padding);
            setScaleType(ScaleType.CENTER_INSIDE);
            
            // Premium gradient background with shadow
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF2C2C2E, 0xFF1C1C1E}
            );
            bg.setCornerRadius(999);
            bg.setStroke((int)(context.getResources().getDisplayMetrics().density * 1), 0x40FFFFFF);
            
            // Shadow layer
            android.graphics.drawable.GradientDrawable shadow = new android.graphics.drawable.GradientDrawable();
            shadow.setColor(0x60000000);
            shadow.setCornerRadius(999);
            
            android.graphics.drawable.LayerDrawable layerBg = new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[]{shadow, bg}
            );
            int offset = (int)(context.getResources().getDisplayMetrics().density * 2);
            layerBg.setLayerInset(0, 0, offset, 0, 0); // Shadow offset
            layerBg.setLayerInset(1, offset, 0, offset, offset); // Main
            
            setBackground(layerBg);
            
            // Icon tint
            setColorFilter(0xFFFFFFFF);
        }
    }
}
