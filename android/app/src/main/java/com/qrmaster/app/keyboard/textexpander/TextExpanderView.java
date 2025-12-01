package com.qrmaster.app.keyboard.textexpander;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.qrmaster.app.R;

import java.util.List;

/**
 * Text Expander Yönetim UI
 * Kısayolları göster, ekle, düzenle, sil
 */
public class TextExpanderView extends LinearLayout {
    private Context context;
    private Callback callback;
    private TextExpanderManager manager;
    private LinearLayout shortcutsContainer;

    public interface Callback {
        void onClose();
    }

    public TextExpanderView(Context context, Callback callback) {
        super(context);
        this.context = context;
        this.callback = callback;
        this.manager = TextExpanderManager.getInstance(context);
        initUI();
    }

    private void initUI() {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp(280)));
        setBackgroundColor(0xFF0A0A0A);

        addView(createHeader());
        addView(createShortcutsList());
        addView(createBottomButtons());

        refreshList();
    }

    private LinearLayout createHeader() {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(6), dp(12), dp(6));
        header.setBackgroundColor(0xFF1A1A1A);

        TextView title = new TextView(context);
        title.setText("Kısayollar");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);

        ImageButton closeBtn = new ImageButton(context);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(GradientDrawable.RECTANGLE);
        closeBg.setCornerRadius(dp(5));
        closeBg.setColor(0xFF2A2A2A);
        closeBtn.setBackground(closeBg);
        closeBtn.setImageResource(R.drawable.ic_close);
        closeBtn.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        closeBtn.setPadding(dp(5), dp(5), dp(5), dp(5));
        closeBtn.setColorFilter(0xFFFFFFFF);
        LayoutParams closeParams = new LayoutParams(dp(24), dp(24));
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> {
            if (callback != null) callback.onClose();
        });
        header.addView(closeBtn);

        return header;
    }

    private LinearLayout createShortcutsList() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setBackgroundColor(0xFF000000);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        container.setLayoutParams(params);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        shortcutsContainer = new LinearLayout(context);
        shortcutsContainer.setOrientation(VERTICAL);
        shortcutsContainer.setPadding(dp(8), dp(6), dp(8), dp(6));
        scrollView.addView(shortcutsContainer);

        container.addView(scrollView);
        return container;
    }

    private LinearLayout createBottomButtons() {
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(HORIZONTAL);
        actions.setPadding(dp(8), dp(6), dp(8), dp(6));
        actions.setBackgroundColor(0xFF141414);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        actions.setLayoutParams(params);

        Button addBtn = createButton("Yeni", 0xFF007AFF);
        addBtn.setOnClickListener(v -> showAddDialog());
        actions.addView(addBtn);

        Button examplesBtn = createButton("Örnekler", 0xFF2A2A2A);
        examplesBtn.setOnClickListener(v -> showExamplesDialog());
        actions.addView(examplesBtn);

        return actions;
    }

    private Button createButton(String text, int color) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setAllCaps(false);
        button.setLetterSpacing(0.02f);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(7));
        bg.setColor(color);
        button.setBackground(bg);

        LayoutParams params = new LayoutParams(0, dp(36), 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        button.setLayoutParams(params);
        return button;
    }

    private void refreshList() {
        shortcutsContainer.removeAllViews();
        List<TextShortcut> shortcuts = manager.getAllShortcuts();

        if (shortcuts.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText("Henüz kısayol yok\n'Yeni' ile başla");
            empty.setTextColor(0xFF666666);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(8), dp(20), dp(8), dp(20));
            shortcutsContainer.addView(empty);
        } else {
            for (TextShortcut shortcut : shortcuts) {
                shortcutsContainer.addView(createShortcutCard(shortcut));
            }
        }
    }

    private LinearLayout createShortcutCard(TextShortcut shortcut) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(VERTICAL);
        card.setPadding(dp(8), dp(6), dp(8), dp(6));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(6));
        cardBg.setColor(0xFF1A1A1A);
        card.setBackground(cardBg);

        LayoutParams cardParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(4);
        card.setLayoutParams(cardParams);

        // Header row
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView trigger = new TextView(context);
        trigger.setText(shortcut.getTrigger());
        trigger.setTextColor(0xFF007AFF);
        trigger.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        trigger.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LayoutParams triggerParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        triggerParams.rightMargin = dp(6);
        trigger.setLayoutParams(triggerParams);
        headerRow.addView(trigger);

        TextView description = new TextView(context);
        description.setText(shortcut.getDescription());
        description.setTextColor(0xFF999999);
        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        LayoutParams descParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        description.setLayoutParams(descParams);
        headerRow.addView(description);

        // Delete button
        TextView deleteBtn = new TextView(context);
        deleteBtn.setText("✕");
        deleteBtn.setTextColor(0xFFFF3B30);
        deleteBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        deleteBtn.setPadding(dp(6), 0, 0, 0);
        deleteBtn.setOnClickListener(v -> {
            manager.deleteShortcut(shortcut.getId());
            refreshList();
            Toast.makeText(context, "Kısayol silindi", Toast.LENGTH_SHORT).show();
        });
        headerRow.addView(deleteBtn);

        card.addView(headerRow);

        // Expansion preview
        TextView expansion = new TextView(context);
        expansion.setText(shortcut.getExpansion());
        expansion.setTextColor(0xFFCCCCCC);
        expansion.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        expansion.setMaxLines(2);
        expansion.setEllipsize(android.text.TextUtils.TruncateAt.END);
        expansion.setPadding(0, dp(4), 0, 0);
        card.addView(expansion);

        // Stats
        if (shortcut.getUseCount() > 0) {
            TextView stats = new TextView(context);
            stats.setText(shortcut.getUseCount() + " kez kullanıldı");
            stats.setTextColor(0xFF666666);
            stats.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            stats.setPadding(0, dp(3), 0, 0);
            card.addView(stats);
        }

        card.setOnClickListener(v -> showEditDialog(shortcut));

        return card;
    }

    private void showAddDialog() {
        // InputMethodService için özel dialog context
        Context dialogContext = context;
        AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("Yeni Kısayol");

        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp(400)));
        
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(VERTICAL);
        layout.setPadding(dp(12), dp(8), dp(12), dp(4));

        EditText triggerInput = new EditText(context);
        triggerInput.setHint("Tetikleyici (örn: /mail)");
        triggerInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        triggerInput.setTextColor(0xFF000000);
        triggerInput.setHintTextColor(0xFF999999);
        triggerInput.setBackgroundColor(0xFFF5F5F5);
        triggerInput.setPadding(dp(10), dp(8), dp(10), dp(8));
        triggerInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        triggerInput.setFocusable(true);
        triggerInput.setFocusableInTouchMode(true);
        LayoutParams triggerParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(38));
        triggerParams.topMargin = dp(4);
        triggerInput.setLayoutParams(triggerParams);
        layout.addView(triggerInput);

        EditText expansionInput = new EditText(context);
        expansionInput.setHint("Genişletme (örn: ornek@email.com)");
        expansionInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        expansionInput.setTextColor(0xFF000000);
        expansionInput.setHintTextColor(0xFF999999);
        expansionInput.setBackgroundColor(0xFFF5F5F5);
        expansionInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        expansionInput.setMinLines(2);
        expansionInput.setMaxLines(4);
        expansionInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        LayoutParams expParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        expParams.topMargin = dp(8);
        expansionInput.setLayoutParams(expParams);
        layout.addView(expansionInput);

        EditText descInput = new EditText(context);
        descInput.setHint("Açıklama (isteğe bağlı)");
        descInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        descInput.setTextColor(0xFF000000);
        descInput.setHintTextColor(0xFF999999);
        descInput.setBackgroundColor(0xFFF5F5F5);
        descInput.setPadding(dp(10), dp(8), dp(10), dp(8));
        descInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        LayoutParams descParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(38));
        descParams.topMargin = dp(6);
        descInput.setLayoutParams(descParams);
        layout.addView(descInput);
        
        // Mini klavye ekle
        MiniKeyboardView miniKeyboard = new MiniKeyboardView(context);
        miniKeyboard.setTargetEditText(triggerInput);
        LayoutParams keyboardParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        keyboardParams.topMargin = dp(8);
        miniKeyboard.setLayoutParams(keyboardParams);
        layout.addView(miniKeyboard);
        
        // EditText focus değişince klavye hedefini değiştir
        triggerInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) miniKeyboard.setTargetEditText(triggerInput);
        });
        expansionInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) miniKeyboard.setTargetEditText(expansionInput);
        });
        descInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) miniKeyboard.setTargetEditText(descInput);
        });
        
        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("Ekle", (dialog, which) -> {
            String trigger = triggerInput.getText().toString().trim();
            String expansion = expansionInput.getText().toString().trim();
            String desc = descInput.getText().toString().trim();

            if (trigger.isEmpty() || expansion.isEmpty()) {
                Toast.makeText(context, "Tetikleyici ve genişletme gerekli", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!trigger.startsWith("/")) {
                trigger = "/" + trigger;
            }

            manager.addShortcut(trigger, expansion, desc.isEmpty() ? trigger : desc);
            refreshList();
            Toast.makeText(context, "Kısayol eklendi", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("İptal", null);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.type = android.view.WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
            lp.token = this.getWindowToken();
            dialog.getWindow().setAttributes(lp);
            
            // Sistem klavyesini KAPATILI tut - mini klavye kullanacağız
            dialog.getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            );
        }
        dialog.show();
        
        // İlk EditText'e focus ver
        triggerInput.post(() -> {
            triggerInput.requestFocus();
        });
    }

    private void showEditDialog(TextShortcut shortcut) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("Düzenle");

        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp(400)));
        
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(VERTICAL);
        layout.setPadding(dp(12), dp(8), dp(12), dp(4));

        EditText triggerInput = new EditText(context);
        triggerInput.setText(shortcut.getTrigger());
        triggerInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        triggerInput.setTextColor(0xFF000000);
        triggerInput.setHintTextColor(0xFF999999);
        triggerInput.setBackgroundColor(0xFFF5F5F5);
        triggerInput.setPadding(dp(10), dp(8), dp(10), dp(8));
        triggerInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        triggerInput.setFocusable(true);
        triggerInput.setFocusableInTouchMode(true);
        LayoutParams triggerEditParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(38));
        triggerEditParams.topMargin = dp(4);
        triggerInput.setLayoutParams(triggerEditParams);
        layout.addView(triggerInput);

        EditText expansionInput = new EditText(context);
        expansionInput.setText(shortcut.getExpansion());
        expansionInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        expansionInput.setTextColor(0xFF000000);
        expansionInput.setHintTextColor(0xFF999999);
        expansionInput.setBackgroundColor(0xFFF5F5F5);
        expansionInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        expansionInput.setMinLines(2);
        expansionInput.setMaxLines(3);
        expansionInput.setPadding(dp(10), dp(8), dp(10), dp(8));
        LayoutParams expEditParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        expEditParams.topMargin = dp(6);
        expansionInput.setLayoutParams(expEditParams);
        layout.addView(expansionInput);

        EditText descInput = new EditText(context);
        descInput.setText(shortcut.getDescription());
        descInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        descInput.setTextColor(0xFF000000);
        descInput.setHintTextColor(0xFF999999);
        descInput.setBackgroundColor(0xFFF5F5F5);
        descInput.setPadding(dp(10), dp(8), dp(10), dp(8));
        descInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        LayoutParams descEditParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(38));
        descEditParams.topMargin = dp(6);
        descInput.setLayoutParams(descEditParams);
        layout.addView(descInput);
        
        // Mini klavye ekle
        MiniKeyboardView miniKeyboard = new MiniKeyboardView(context);
        miniKeyboard.setTargetEditText(triggerInput);
        LayoutParams keyboardParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        keyboardParams.topMargin = dp(8);
        miniKeyboard.setLayoutParams(keyboardParams);
        layout.addView(miniKeyboard);
        
        // EditText focus değişince klavye hedefini değiştir
        triggerInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) miniKeyboard.setTargetEditText(triggerInput);
        });
        expansionInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) miniKeyboard.setTargetEditText(expansionInput);
        });
        descInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) miniKeyboard.setTargetEditText(descInput);
        });
        
        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("Kaydet", (dialog, which) -> {
            shortcut.setTrigger(triggerInput.getText().toString().trim());
            shortcut.setExpansion(expansionInput.getText().toString().trim());
            shortcut.setDescription(descInput.getText().toString().trim());
            manager.updateShortcut(shortcut);
            refreshList();
            Toast.makeText(context, "Kısayol güncellendi", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("İptal", null);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.type = android.view.WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
            lp.token = this.getWindowToken();
            dialog.getWindow().setAttributes(lp);
            
            // Sistem klavyesini KAPATILI tut - mini klavye kullanacağız
            dialog.getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            );
        }
        dialog.show();
        
        // İlk EditText'e focus ver
        triggerInput.post(() -> {
            triggerInput.requestFocus();
        });
    }

    private void showExamplesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("Örnek Kısayollar");
        builder.setMessage(
            "/mail → Email adresiniz\n" +
            "/tel → Telefon numaranız\n" +
            "/adres → Ev/iş adresiniz\n" +
            "/iban → Banka IBAN'ınız\n" +
            "/imza → İmzanız\n\n" +
            "Kullanım: Tetikleyiciyi yazıp SPACE'e basın!"
        );
        builder.setPositiveButton("Anladım", null);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.type = android.view.WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
            lp.token = this.getWindowToken();
            dialog.getWindow().setAttributes(lp);
        }
        dialog.show();
    }

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}

