package com.qrmaster.app.keyboard.textexpander;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;

/**
 * Mini Klavye - Dialog içinde kullanım için
 */
public class MiniKeyboardView extends LinearLayout {
    
    private EditText targetEditText;
    
    public MiniKeyboardView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setPadding(dp(4), dp(4), dp(4), dp(4));
        setBackgroundColor(0xFFE0E0E0);
        initKeyboard();
    }
    
    public void setTargetEditText(EditText editText) {
        this.targetEditText = editText;
    }
    
    private void initKeyboard() {
        // Row 1: Numbers + special
        addRow(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"});
        
        // Row 2: QWERTY
        addRow(new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"});
        
        // Row 3: ASDFGH
        addRow(new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l"});
        
        // Row 4: ZXCVBN
        addRow(new String[]{"z", "x", "c", "v", "b", "n", "m"});
        
        // Row 5: Special + Space + Paste + Delete
        LinearLayout row5 = new LinearLayout(getContext());
        row5.setOrientation(HORIZONTAL);
        row5.setGravity(Gravity.CENTER);
        
        addKeyToRow(row5, "/", 1);
        addKeyToRow(row5, "@", 1);
        addKeyToRow(row5, ".", 1);
        
        Button spaceBtn = createKey(" ", 2);
        spaceBtn.setText("Boşluk");
        row5.addView(spaceBtn);
        
        // Yapıştır butonu
        Button pasteBtn = createKey("", 1);
        pasteBtn.setText("Yapıştır");
        pasteBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        pasteBtn.setOnClickListener(v -> {
            if (targetEditText != null) {
                android.content.ClipboardManager clipboard = 
                    (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip()) {
                    android.content.ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                    CharSequence text = item.getText();
                    if (text != null) {
                        int start = targetEditText.getSelectionStart();
                        targetEditText.getText().insert(start, text);
                    }
                }
            }
        });
        row5.addView(pasteBtn);
        
        Button delBtn = createKey("⌫", 1);
        delBtn.setOnClickListener(v -> {
            if (targetEditText != null) {
                int start = targetEditText.getSelectionStart();
                if (start > 0) {
                    targetEditText.getText().delete(start - 1, start);
                }
            }
        });
        row5.addView(delBtn);
        
        addView(row5);
    }
    
    private void addRow(String[] keys) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        
        for (String key : keys) {
            addKeyToRow(row, key, 1);
        }
        
        addView(row);
    }
    
    private void addKeyToRow(LinearLayout row, String key, int weight) {
        Button btn = createKey(key, weight);
        row.addView(btn);
    }
    
    private Button createKey(String key, int weight) {
        Button btn = new Button(getContext());
        btn.setText(key);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setTextColor(0xFF000000);
        btn.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        btn.setAllCaps(false);
        btn.setPadding(0, 0, 0, 0);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(4));
        bg.setColor(0xFFFFFFFF);
        bg.setStroke(dp(1), 0xFFCCCCCC);
        btn.setBackground(bg);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(32), weight);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        btn.setLayoutParams(params);
        
        btn.setOnClickListener(v -> {
            if (targetEditText != null) {
                int start = targetEditText.getSelectionStart();
                targetEditText.getText().insert(start, key);
            }
        });
        
        return btn;
    }
    
    private int dp(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}

