package com.qrmaster.app.keyboard;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Metin düzenleme ve klavye boyutu kontrolleri
 */
public class TextToolsView extends LinearLayout {

    public interface TextToolsCallback {
        void onIncreaseSize();
        void onDecreaseSize();
        void onResetSize();
        void onMoveCursorLeft();
        void onMoveCursorRight();
        void onMoveCursorStart();
        void onMoveCursorEnd();
        void onCopy();
        void onPaste();
        void onCut();
        void onSelectAll();
        void onClose();
    }

    private final TextToolsCallback callback;

    public TextToolsView(Context context, TextToolsCallback callback) {
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
        setPadding(dp(12), dp(12), dp(12), dp(16));
        setBackgroundColor(0xFF1C1C1E);

        TextView title = new TextView(context);
        title.setText("Metin araçları");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        LayoutParams titleParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        titleParams.bottomMargin = dp(8);
        addView(title, titleParams);

        addRow(
            createButton("A-", v -> callback.onDecreaseSize()),
            createButton("A+", v -> callback.onIncreaseSize()),
            createButton("Sıfırla", v -> callback.onResetSize()),
            createButton("Kapat", v -> callback.onClose())
        );

        addRow(
            createButton("←", v -> callback.onMoveCursorLeft()),
            createButton("→", v -> callback.onMoveCursorRight()),
            createButton("⭰", v -> callback.onMoveCursorStart()),
            createButton("⭲", v -> callback.onMoveCursorEnd())
        );

        addRow(
            createButton("Kes", v -> callback.onCut()),
            createButton("Kopyala", v -> callback.onCopy()),
            createButton("Yapıştır", v -> callback.onPaste()),
            createButton("Seç", v -> callback.onSelectAll())
        );
    }

    private void addRow(View... views) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LayoutParams params = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(6);
        row.setLayoutParams(params);
        for (View view : views) {
            LinearLayout.LayoutParams childParams = new LinearLayout.LayoutParams(0, dp(44), 1f);
            childParams.setMargins(dp(4), 0, dp(4), 0);
            row.addView(view, childParams);
        }
        addView(row);
    }

    private Button createButton(String text, OnClickListener listener) {
        Button button = new Button(getContext());
        button.setText(text);
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setBackgroundColor(0xFF2C2C2E);
        button.setOnClickListener(listener);
        return button;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}








