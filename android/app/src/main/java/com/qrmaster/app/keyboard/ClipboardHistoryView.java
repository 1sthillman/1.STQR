package com.qrmaster.app.keyboard;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.qrmaster.app.R;

import java.util.List;

public class ClipboardHistoryView extends LinearLayout implements ClipboardStore.OnChangeListener {
    private static final String TAG = "ClipboardHistoryView";

    public interface ClipboardCallback {
        void onClipSelected(ClipboardEntry entry);
        void onPinToggled(ClipboardEntry entry);
        void onDelete(ClipboardEntry entry);
        void onClose();
    }

    private final ClipboardCallback callback;
    private final ClipboardStore store;
    private LinearLayout historyContainer;

    public ClipboardHistoryView(Context context, ClipboardCallback callback, ClipboardStore store) {
        super(context);
        this.callback = callback;
        this.store = store;
        init(context);
        store.register(this);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            dp(280)
        ));
        setBackgroundColor(0xFF0A0A0A);

        // Premium Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(8), dp(12), dp(8));
        
        android.graphics.drawable.GradientDrawable headerBg = new android.graphics.drawable.GradientDrawable();
        headerBg.setColor(0xFF1A1A1A);
        header.setBackground(headerBg);
        
        LayoutParams headerParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        
        TextView title = new TextView(context);
        title.setText("Pano Geçmişi");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setLetterSpacing(0.02f);
        LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleParams);
        header.addView(title);
        
        ImageButton closeBtn = new ImageButton(context);
        android.graphics.drawable.GradientDrawable closeBg = new android.graphics.drawable.GradientDrawable();
        closeBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        closeBg.setCornerRadius(dp(6));
        closeBg.setColor(0xFF2A2A2A);
        closeBtn.setBackground(closeBg);
        closeBtn.setImageResource(R.drawable.ic_close);
        closeBtn.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        closeBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        closeBtn.setColorFilter(0xFFFFFFFF);
        LayoutParams closeParams = new LayoutParams(dp(30), dp(30));
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> { if (callback != null) callback.onClose(); });
        header.addView(closeBtn);
        addView(header, headerParams);

        ScrollView scrollView = new ScrollView(context);
        LayoutParams scrollParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            0,
            1f
        );
        scrollView.setLayoutParams(scrollParams);
        scrollView.setBackgroundColor(0xFF000000);
        
        historyContainer = new LinearLayout(context);
        historyContainer.setOrientation(VERTICAL);
        historyContainer.setPadding(dp(8), dp(6), dp(8), dp(6));
        scrollView.addView(historyContainer, new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ));
        addView(scrollView);
    }

    private void renderEntries(List<ClipboardEntry> entries) {
        historyContainer.removeAllViews();
        if (entries == null || entries.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("Henüz pano öğesi yok");
            empty.setTextColor(0xFF666666);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(30), 0, dp(30));
            historyContainer.addView(empty);
            return;
        }

        for (ClipboardEntry entry : entries) {
            historyContainer.addView(createProEntryView(entry));
        }
    }

    private View createProEntryView(ClipboardEntry entry) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        
        android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
        cardBg.setCornerRadius(dp(10));
        cardBg.setColor(0xFF1A1A1A);
        cardBg.setStroke(dp(1), 0xFF2A2A2A);
        card.setBackground(cardBg);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        
        LayoutParams params = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(6);
        card.setLayoutParams(params);

        // Top row - Type badge + timestamp
        LinearLayout topRow = new LinearLayout(getContext());
        topRow.setOrientation(HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams topParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        topParams.bottomMargin = dp(6);
        topRow.setLayoutParams(topParams);
        
        // Type badge
        TextView typeBadge = new TextView(getContext());
        boolean isScreenshot = entry.getType() == ClipboardEntry.Type.SCREENSHOT;
        typeBadge.setText(isScreenshot ? "GÖRSEL" : "METİN");
        typeBadge.setTextColor(isScreenshot ? 0xFF00C7BE : 0xFF0A84FF);
        typeBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        typeBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        typeBadge.setAllCaps(true);
        typeBadge.setLetterSpacing(0.05f);
        typeBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        
        android.graphics.drawable.GradientDrawable badgeBg = new android.graphics.drawable.GradientDrawable();
        badgeBg.setCornerRadius(dp(4));
        badgeBg.setColor(isScreenshot ? 0x2000C7BE : 0x200A84FF);
        typeBadge.setBackground(badgeBg);
        
        topRow.addView(typeBadge);
        card.addView(topRow);

        // Content row - Thumbnail + Text
        LinearLayout contentRow = new LinearLayout(getContext());
        contentRow.setOrientation(HORIZONTAL);
        contentRow.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams contentParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        contentParams.bottomMargin = dp(8);
        contentRow.setLayoutParams(contentParams);
        
        // Thumbnail (if screenshot)
        if (isScreenshot && entry.getUri() != null) {
            android.widget.ImageView thumbnail = new android.widget.ImageView(getContext());
            thumbnail.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            
            android.graphics.drawable.GradientDrawable thumbBg = new android.graphics.drawable.GradientDrawable();
            thumbBg.setCornerRadius(dp(8));
            thumbBg.setColor(0xFF2A2A2A);
            thumbnail.setBackground(thumbBg);
            thumbnail.setClipToOutline(true);
            
            LayoutParams thumbParams = new LayoutParams(dp(70), dp(70));
            thumbParams.rightMargin = dp(10);
            thumbnail.setLayoutParams(thumbParams);
            
            try {
                android.net.Uri uri = entry.getUri();
                java.io.InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                if (bitmap != null) {
                    android.graphics.Bitmap thumb = android.graphics.Bitmap.createScaledBitmap(
                        bitmap, dp(70), dp(70), true
                    );
                    thumbnail.setImageBitmap(thumb);
                    bitmap.recycle();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Görsel yüklenemedi", e);
            }
            
            contentRow.addView(thumbnail);
        }
        
        // Text content
        TextView content = new TextView(getContext());
        content.setTextColor(0xFFCCCCCC);
        content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        content.setMaxLines(isScreenshot ? 1 : 2);
        content.setEllipsize(TextUtils.TruncateAt.END);
        content.setLineSpacing(dp(2), 1.0f);
        content.setText(entry.getType() == ClipboardEntry.Type.TEXT ? entry.getContent() : "Ekran görüntüsü");
        LayoutParams contentTextParams = new LayoutParams(
            0,
            LayoutParams.WRAP_CONTENT,
            1f
        );
        content.setLayoutParams(contentTextParams);
        contentRow.addView(content);
        
        card.addView(contentRow);

        // Action buttons - MODERN & CLEAN
        LinearLayout actions = new LinearLayout(getContext());
        actions.setOrientation(HORIZONTAL);
        actions.setGravity(Gravity.START);

        // Primary action - Kullan
        Button useBtn = createModernButton("Kullan", 0xFF0A84FF, true);
        useBtn.setOnClickListener(v -> {
            if (callback != null) callback.onClipSelected(entry);
        });
        actions.addView(useBtn);

        // Contextual actions (TEXT için)
        if (entry.getType() == ClipboardEntry.Type.TEXT) {
            com.qrmaster.app.keyboard.clipboard.ContextualPasteHelper.ContentType type = 
                com.qrmaster.app.keyboard.clipboard.ContextualPasteHelper.detectType(entry.getContent());
            
            java.util.List<com.qrmaster.app.keyboard.clipboard.ContextualPasteHelper.Action> contextActions = 
                com.qrmaster.app.keyboard.clipboard.ContextualPasteHelper.getActions(getContext(), entry.getContent(), type);
            
            // İlk 1 aksiyonu ekle (daha kompakt)
            if (!contextActions.isEmpty()) {
                com.qrmaster.app.keyboard.clipboard.ContextualPasteHelper.Action action = contextActions.get(0);
                Button actionBtn = createModernButton(action.label, 0xFF666666, false);
                actionBtn.setOnClickListener(v -> {
                    if (action.intent != null) {
                        try {
                            getContext().startActivity(action.intent);
                        } catch (Exception e) {
                            android.widget.Toast.makeText(getContext(), "İşlem desteklenmiyor", 
                                android.widget.Toast.LENGTH_SHORT).show();
                        }
                    } else if (action.customAction != null) {
                        action.customAction.run();
                    }
                });
                actions.addView(actionBtn);
            }
        }

        // Delete button
        Button deleteBtn = createModernButton("Sil", 0xFF3A3A3A, false);
        deleteBtn.setTextColor(0xFFFF453A);
        deleteBtn.setOnClickListener(v -> {
            if (callback != null) callback.onDelete(entry);
        });
        actions.addView(deleteBtn);

        card.addView(actions);
        return card;
    }

    private Button createModernButton(String text, int color, boolean isPrimary) {
        Button button = new Button(getContext());
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        button.setTextColor(isPrimary ? 0xFFFFFFFF : 0xFFCCCCCC);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setAllCaps(false);
        button.setLetterSpacing(0.02f);
        
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setCornerRadius(dp(6));
        btnBg.setColor(color);
        button.setBackground(btnBg);
        button.setPadding(dp(14), dp(6), dp(14), dp(6));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT,
            dp(30)
        );
        params.rightMargin = dp(6);
        button.setLayoutParams(params);
        return button;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onClipboardChanged(List<ClipboardEntry> entries) {
        new Handler(Looper.getMainLooper()).post(() -> renderEntries(entries));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try {
            store.unregister(this);
        } catch (Exception e) {
            Log.e(TAG, "Listener kaldırılamadı", e);
        }
    }
}
