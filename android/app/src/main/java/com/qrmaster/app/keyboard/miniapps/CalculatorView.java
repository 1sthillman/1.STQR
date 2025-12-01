package com.qrmaster.app.keyboard.miniapps;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.gridlayout.widget.GridLayout;

import com.qrmaster.app.R;

/**
 * Premium Hesap Makinesi - Modern iOS Style
 */
public class CalculatorView extends LinearLayout {
    private TextView display;
    private TextView expressionText;
    private StringBuilder currentInput = new StringBuilder();
    private double result = 0;
    private String operator = "";
    private boolean startNewNumber = true;

    public interface CalculatorCallback {
        void onResult(String result);
        void onClose();
    }

    private final CalculatorCallback callback;

    public CalculatorView(Context context, CalculatorCallback callback) {
        super(context);
        this.callback = callback;
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp(380))); // Daha yüksek
        setBackgroundColor(0xFF0A0A0A);

        // Premium Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(8), dp(12), dp(8));
        
        android.graphics.drawable.GradientDrawable headerBg = new android.graphics.drawable.GradientDrawable();
        headerBg.setColor(0xFF1A1A1A);
        header.setBackground(headerBg);
        
        LayoutParams headerParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        headerParams.bottomMargin = dp(6);
        
        TextView title = new TextView(context);
        title.setText("Hesap Makinesi");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setLetterSpacing(0.02f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);

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
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> { if (callback != null) callback.onClose(); });
        header.addView(closeBtn);
        addView(header, headerParams);

        // Display Container
        LinearLayout displayContainer = new LinearLayout(context);
        displayContainer.setOrientation(VERTICAL);
        displayContainer.setPadding(dp(16), dp(12), dp(16), dp(8));
        
        android.graphics.drawable.GradientDrawable displayBg = new android.graphics.drawable.GradientDrawable();
        displayBg.setCornerRadius(dp(12));
        displayBg.setColor(0xFF1A1A1A);
        displayContainer.setBackground(displayBg);
        
        LayoutParams displayContainerParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        displayContainerParams.setMargins(dp(8), 0, dp(8), dp(8));
        
        // Expression text
        expressionText = new TextView(context);
        expressionText.setText("");
        expressionText.setTextColor(0xFF666666);
        expressionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        expressionText.setGravity(Gravity.END);
        expressionText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        LayoutParams exprParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        exprParams.bottomMargin = dp(4);
        displayContainer.addView(expressionText, exprParams);

        // Main display
        display = new TextView(context);
        display.setText("0");
        display.setTextColor(0xFFFFFFFF);
        display.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
        display.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        display.setGravity(Gravity.END);
        LayoutParams displayParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        displayContainer.addView(display, displayParams);
        
        addView(displayContainer, displayContainerParams);

        // Buttons Grid
        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(4);
        grid.setRowCount(5);
        grid.setPadding(dp(4), 0, dp(4), dp(4));
        
        String[][] buttons = {
            {"C", "⌫", "%", "÷"},
            {"7", "8", "9", "×"},
            {"4", "5", "6", "-"},
            {"1", "2", "3", "+"},
            {"0", ".", "=", "✓"}
        };

        for (String[] row : buttons) {
            for (String btn : row) {
                grid.addView(createPremiumButton(context, btn));
            }
        }
        
        addView(grid);
    }

    private Button createPremiumButton(Context context, String text) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        btn.setTextColor(0xFFFFFFFF);
        btn.setAllCaps(false);
        
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(10));
        
        // iOS-style colors
        if (text.equals("=") || text.equals("✓")) {
            bg.setColor(0xFF0A84FF); // Apple Blue
            btn.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        } else if (text.matches("[+\\-×÷%]")) {
            bg.setColor(0xFFFF9F0A); // Apple Orange
        } else if (text.equals("C") || text.equals("⌫")) {
            bg.setColor(0xFF505050); // Dark Gray
        } else {
            bg.setColor(0xFF2A2A2A); // Dark
        }
        
        btn.setBackground(bg);
        
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(50);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        btn.setLayoutParams(params);
        
        btn.setOnClickListener(v -> handleButton(text));
        
        return btn;
    }

    private void handleButton(String btn) {
        switch (btn) {
            case "C":
                clear();
                break;
            case "⌫":
                backspace();
                break;
            case "=":
                calculate();
                break;
            case "✓":
                if (callback != null) {
                    callback.onResult(display.getText().toString());
                    callback.onClose();
                }
                break;
            case "+":
            case "-":
            case "×":
            case "÷":
            case "%":
                setOperator(btn);
                break;
            default:
                appendNumber(btn);
                break;
        }
    }

    public void inputFromKeyboard(String text) {
        if (text == null || text.isEmpty()) return;
        switch (text) {
            case "C":
            case "⌫":
            case "=":
            case "+":
            case "-":
            case "×":
            case "÷":
            case "%":
            case ".":
                handleButton(text);
                break;
            default:
                if (text.matches("[0-9]")) {
                    handleButton(text);
        }
                break;
        }
    }

    private void appendNumber(String num) {
        if (startNewNumber) {
            currentInput.setLength(0);
            startNewNumber = false;
        }
        currentInput.append(num);
        display.setText(currentInput.toString());
    }

    private void setOperator(String op) {
        if (currentInput.length() > 0) {
            if (!operator.isEmpty()) {
                calculate();
            } else {
            result = Double.parseDouble(currentInput.toString());
            }
        }
        operator = op;
        expressionText.setText(result + " " + op);
        startNewNumber = true;
    }

    private void calculate() {
        if (currentInput.length() == 0 || operator.isEmpty()) return;
        
        double currentValue = Double.parseDouble(currentInput.toString());
        
        switch (operator) {
            case "+":
                result += currentValue;
                break;
            case "-":
                result -= currentValue;
                break;
            case "×":
                result *= currentValue;
                break;
            case "÷":
                if (currentValue != 0) {
                    result /= currentValue;
                }
                break;
            case "%":
                result = result * currentValue / 100;
                break;
        }
        
        display.setText(formatResult(result));
        expressionText.setText("");
        currentInput.setLength(0);
        currentInput.append(formatResult(result));
        operator = "";
        startNewNumber = true;
    }

    private void clear() {
        currentInput.setLength(0);
        result = 0;
        operator = "";
        startNewNumber = true;
        display.setText("0");
        expressionText.setText("");
    }

    private void backspace() {
        if (currentInput.length() > 0) {
            currentInput.deleteCharAt(currentInput.length() - 1);
            display.setText(currentInput.length() > 0 ? currentInput.toString() : "0");
        }
    }

    private String formatResult(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            return String.format("%.2f", value);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
