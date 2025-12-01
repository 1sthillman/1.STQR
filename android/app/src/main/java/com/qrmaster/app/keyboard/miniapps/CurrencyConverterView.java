package com.qrmaster.app.keyboard.miniapps;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.core.content.ContextCompat;

import com.qrmaster.app.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Döviz Çevirici - Gerçek zamanlı kurlar
 * API: exchangerate-api.com (ücretsiz, günlük 1500 istek)
 */
public class CurrencyConverterView extends LinearLayout {
    private static final String TAG = "CurrencyConverter";
    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/";
    
    private EditText amountInput;
    private Spinner fromCurrency, toCurrency;
    private LinearLayout fromRow, toRow;
    private String selectedFrom = "USD";
    private String selectedTo = "TRY";
    private TextView resultText;
    private ProgressBar progressBar;
    private TextView statusText;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Double> rates = new HashMap<>();
    private String baseCurrency = "USD";

    public interface CurrencyCallback {
        void onResult(String result);
        void onClose();
    }

    private final CurrencyCallback callback;

    public CurrencyConverterView(Context context, CurrencyCallback callback) {
        super(context);
        this.callback = callback;
        init(context);
        loadRates();
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setBackgroundColor(0xFF1C1C1E);
        setPadding(dp(12), dp(12), dp(12), dp(12));

        // Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(12));
        
        TextView title = new TextView(context);
        title.setText("💱 Döviz Çevirici");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);

        ImageButton closeBtn = new ImageButton(context);
        closeBtn.setBackground(ContextCompat.getDrawable(context, R.drawable.toolbar_button_bg));
        closeBtn.setImageResource(R.drawable.ic_close);
        closeBtn.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        closeBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> { if (callback != null) callback.onClose(); });
        header.addView(closeBtn);
        addView(header);

        // Amount input
        TextView amountLabel = new TextView(context);
        amountLabel.setText("Miktar:");
        amountLabel.setTextColor(0xFF8E8E93);
        amountLabel.setTextSize(12);
        addView(amountLabel);

        amountInput = new EditText(context);
        amountInput.setHint("100");
        amountInput.setTextColor(0xFFFFFFFF);
        amountInput.setHintTextColor(0xFF8E8E93);
        amountInput.setTextSize(18);
        amountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        try {
            // IME içinde ikinci bir IME açılmasını önle
            amountInput.setShowSoftInputOnFocus(false);
        } catch (Throwable ignored) {}
        amountInput.setBackgroundColor(0xFF2C2C2E);
        amountInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        LayoutParams amountParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(50));
        amountParams.topMargin = dp(4);
        amountParams.bottomMargin = dp(12);
        android.graphics.drawable.GradientDrawable amountBg = new android.graphics.drawable.GradientDrawable();
        amountBg.setCornerRadius(dp(8));
        amountBg.setColor(0xFF2C2C2E);
        amountInput.setBackground(amountBg);
        addView(amountInput, amountParams);

        // From currency
        TextView fromLabel = new TextView(context);
        fromLabel.setText("Çevir:");
        fromLabel.setTextColor(0xFF8E8E93);
        fromLabel.setTextSize(12);
        addView(fromLabel);

        String[] currencies = {"USD", "EUR", "GBP", "TRY", "JPY", "CNY", "RUB", "AUD", "CAD", "CHF"};

        // Inline chips (from)
        fromRow = new LinearLayout(context);
        fromRow.setOrientation(HORIZONTAL);
        LayoutParams fromParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        fromParams.topMargin = dp(4);
        fromParams.bottomMargin = dp(12);
        addView(fromRow, fromParams);
        buildCurrencyChips(fromRow, currencies, true);

        // To currency
        TextView toLabel = new TextView(context);
        toLabel.setText("Hedef:");
        toLabel.setTextColor(0xFF8E8E93);
        toLabel.setTextSize(12);
        addView(toLabel);

        // Inline chips (to)
        toRow = new LinearLayout(context);
        toRow.setOrientation(HORIZONTAL);
        LayoutParams toParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        toParams.topMargin = dp(4);
        toParams.bottomMargin = dp(12);
        addView(toRow, toParams);
        buildCurrencyChips(toRow, currencies, false);

        // Progress
        progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(GONE);
        LayoutParams pbParams = new LayoutParams(dp(40), dp(40));
        pbParams.gravity = Gravity.CENTER;
        pbParams.topMargin = dp(8);
        addView(progressBar, pbParams);

        // Status
        statusText = new TextView(context);
        statusText.setTextColor(0xFF8E8E93);
        statusText.setTextSize(12);
        statusText.setGravity(Gravity.CENTER);
        statusText.setText("Kurlar yükleniyor...");
        LayoutParams statusParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(4);
        addView(statusText, statusParams);

        // Result
        resultText = new TextView(context);
        resultText.setText("0.00");
        resultText.setTextColor(0xFF34C759);
        resultText.setTextSize(32);
        resultText.setTypeface(null, Typeface.BOLD);
        resultText.setGravity(Gravity.CENTER);
        resultText.setBackgroundColor(0xFF2C2C2E);
        resultText.setPadding(dp(16), dp(20), dp(16), dp(20));
        LayoutParams resultParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        resultParams.topMargin = dp(16);
        android.graphics.drawable.GradientDrawable resultBg = new android.graphics.drawable.GradientDrawable();
        resultBg.setCornerRadius(dp(12));
        resultBg.setColor(0xFF2C2C2E);
        resultText.setBackground(resultBg);
        addView(resultText, resultParams);

        // Spacer
        addView(new TextView(context), new LayoutParams(0, 0, 1f));

        // Insert button
        Button insertBtn = new Button(context);
        insertBtn.setText("✓ Yapıştır");
        insertBtn.setTextColor(0xFFFFFFFF);
        insertBtn.setTextSize(16);
        insertBtn.setTypeface(null, Typeface.BOLD);
        android.graphics.drawable.GradientDrawable insertBg = new android.graphics.drawable.GradientDrawable();
        insertBg.setCornerRadius(dp(12));
        insertBg.setColor(0xFF34C759);
        insertBtn.setBackground(insertBg);
        LayoutParams insertParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(50));
        insertParams.topMargin = dp(12);
        insertBtn.setLayoutParams(insertParams);
        insertBtn.setOnClickListener(v -> {
            if (callback != null) {
                String result = resultText.getText().toString() + " " + toCurrency.getSelectedItem().toString();
                callback.onResult(result);
                callback.onClose();
            }
        });
        addView(insertBtn);

        // Auto-calculate on input
        amountInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) { calculate(); }
        });

        // Placeholder spinners (ekranda kullanılmıyor)
        fromCurrency = new Spinner(context);
        toCurrency = new Spinner(context);
    }

    // IME'den yönlendirilen girişler
    public void appendAmountText(String s) {
        try {
            int start = amountInput.getSelectionStart();
            int end = amountInput.getSelectionEnd();
            if (start < 0) start = amountInput.getText().length();
            if (end < 0) end = start;
            amountInput.getText().replace(Math.min(start, end), Math.max(start, end), s, 0, s.length());
        } catch (Exception ignored) {}
    }

    public void backspaceAmount() {
        try {
            int start = amountInput.getSelectionStart();
            int end = amountInput.getSelectionEnd();
            if (start == end && start > 0) {
                amountInput.getText().delete(start - 1, start);
            } else if (start != end) {
                amountInput.getText().delete(Math.min(start, end), Math.max(start, end));
            }
        } catch (Exception ignored) {}
    }

    public void submitInsert() {
        if (callback != null) {
            String result = resultText.getText().toString() + " " + selectedTo;
            callback.onResult(result);
        }
    }

    private void loadRates() {
        progressBar.setVisibility(VISIBLE);
        statusText.setText("Kurlar yükleniyor...");
        
        executor.execute(() -> {
            try {
                URL url = new URL(API_URL + "USD");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject json = new JSONObject(response.toString());
                    JSONObject ratesJson = json.getJSONObject("rates");
                    
                    rates.clear();
                    java.util.Iterator<String> keys = ratesJson.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        rates.put(key, ratesJson.getDouble(key));
                    }
                    
                    mainHandler.post(() -> {
                        progressBar.setVisibility(GONE);
                        statusText.setText("✓ Kurlar güncellendi");
                        calculate();
                    });
                } else {
                    throw new Exception("HTTP " + conn.getResponseCode());
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Kur yükleme hatası", e);
                mainHandler.post(() -> {
                    progressBar.setVisibility(GONE);
                    statusText.setText("❌ Kurlar yüklenemedi (offline mod)");
                    loadOfflineRates();
                });
            }
        });
    }

    private void loadOfflineRates() {
        // Fallback offline rates
        rates.put("USD", 1.0);
        rates.put("EUR", 0.92);
        rates.put("GBP", 0.79);
        rates.put("TRY", 32.5);
        rates.put("JPY", 149.8);
        rates.put("CNY", 7.24);
        rates.put("RUB", 92.5);
        rates.put("AUD", 1.53);
        rates.put("CAD", 1.36);
        rates.put("CHF", 0.88);
        calculate();
    }

    private void calculate() {
        try {
            String amountStr = amountInput.getText().toString();
            if (amountStr.isEmpty() || rates.isEmpty()) {
                resultText.setText("0.00");
                return;
            }
            
            double amount = Double.parseDouble(amountStr);
            String from = selectedFrom;
            String to = selectedTo;
            
            Double fromRate = rates.get(from);
            Double toRate = rates.get(to);
            
            if (fromRate == null || toRate == null) {
                resultText.setText("N/A");
                return;
            }
            
            double result = (amount / fromRate) * toRate;
            DecimalFormat df = new DecimalFormat("#,##0.00");
            resultText.setText(df.format(result));
        } catch (Exception e) {
            Log.e(TAG, "Hesaplama hatası", e);
            resultText.setText("Hata");
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    public void release() {
        executor.shutdown();
    }

    private void buildCurrencyChips(LinearLayout row, String[] codes, boolean isFrom) {
        row.removeAllViews();
        for (String code : codes) {
            Button chip = new Button(getContext());
            chip.setText(code);
            chip.setTextSize(12);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setTextColor(0xFFFFFFFF);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(16));
            boolean selected = (isFrom ? selectedFrom : selectedTo).equals(code);
            bg.setColor(selected ? 0xFF007AFF : 0xFF3A3A3C);
            chip.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp(36));
            lp.rightMargin = dp(6);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                if (isFrom) selectedFrom = code; else selectedTo = code;
                buildCurrencyChips(row, codes, isFrom);
                calculate();
                try {
                    if (getContext() instanceof android.inputmethodservice.InputMethodService) {
                        ((android.inputmethodservice.InputMethodService)getContext()).requestShowSelf(0);
                    }
                } catch (Exception ignored) {}
            });
            row.addView(chip);
        }
    }
}


