package com.qrmaster.app.keyboard;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.List;

public class TranslateView extends LinearLayout {
    private final TranslateCallback callback;
    private final ArrayMap<String, String> languageCodes = new ArrayMap<>();

    private Spinner sourceLangSpinner;
    private Spinner targetLangSpinner;
    private TextView inputPreview;
    private TextView outputText;
    private ProgressBar progressBar;
    private TextView statusText;

    private Translator currentTranslator;
    private String currentSource = "";
    private String currentTarget = "";
    private String currentInput = "";

    public interface TranslateCallback {
        void onTranslated(String text);
        void onClose();
        void onClear();
    }

    public TranslateView(Context context, TranslateCallback callback) {
        super(context);
        this.callback = callback;
        initLanguageMap();
        init(context);
    }

    private void initLanguageMap() {
        // Popüler diller önce
        languageCodes.put("🇹🇷 Türkçe", TranslateLanguage.TURKISH);
        languageCodes.put("🇬🇧 İngilizce", TranslateLanguage.ENGLISH);
        languageCodes.put("🇩🇪 Almanca", TranslateLanguage.GERMAN);
        languageCodes.put("🇫🇷 Fransızca", TranslateLanguage.FRENCH);
        languageCodes.put("🇪🇸 İspanyolca", TranslateLanguage.SPANISH);
        languageCodes.put("🇮🇹 İtalyanca", TranslateLanguage.ITALIAN);
        languageCodes.put("🇷🇺 Rusça", TranslateLanguage.RUSSIAN);
        languageCodes.put("🇸🇦 Arapça", TranslateLanguage.ARABIC);
        languageCodes.put("🇨🇳 Çince", TranslateLanguage.CHINESE);
        languageCodes.put("🇯🇵 Japonca", TranslateLanguage.JAPANESE);
        languageCodes.put("🇰🇷 Korece", TranslateLanguage.KOREAN);
        languageCodes.put("🇵🇹 Portekizce", TranslateLanguage.PORTUGUESE);
        languageCodes.put("🇮🇳 Hintçe", TranslateLanguage.HINDI);
        languageCodes.put("🇮🇷 Farsça", TranslateLanguage.PERSIAN);
        
        // Diğer diller
        languageCodes.put("Afrikanca", TranslateLanguage.AFRIKAANS);
        languageCodes.put("Arnavutça", TranslateLanguage.ALBANIAN);
        languageCodes.put("Bengalce", TranslateLanguage.BENGALI);
        languageCodes.put("Bulgarca", TranslateLanguage.BULGARIAN);
        languageCodes.put("Katalanca", TranslateLanguage.CATALAN);
        languageCodes.put("Hırvatça", TranslateLanguage.CROATIAN);
        languageCodes.put("Çekçe", TranslateLanguage.CZECH);
        languageCodes.put("Danca", TranslateLanguage.DANISH);
        languageCodes.put("Flemenkçe", TranslateLanguage.DUTCH);
        languageCodes.put("Fince", TranslateLanguage.FINNISH);
        languageCodes.put("Yunanca", TranslateLanguage.GREEK);
        languageCodes.put("İbranice", TranslateLanguage.HEBREW);
        languageCodes.put("Macarca", TranslateLanguage.HUNGARIAN);
        languageCodes.put("İzlandaca", TranslateLanguage.ICELANDIC);
        languageCodes.put("Endonezce", TranslateLanguage.INDONESIAN);
        languageCodes.put("Litvanca", TranslateLanguage.LITHUANIAN);
        languageCodes.put("Makedonca", TranslateLanguage.MACEDONIAN);
        languageCodes.put("Malayca", TranslateLanguage.MALAY);
        languageCodes.put("Norveççe", TranslateLanguage.NORWEGIAN);
        languageCodes.put("Lehçe", TranslateLanguage.POLISH);
        languageCodes.put("Rumence", TranslateLanguage.ROMANIAN);
        languageCodes.put("Slovakça", TranslateLanguage.SLOVAK);
        languageCodes.put("Slovence", TranslateLanguage.SLOVENIAN);
        languageCodes.put("İsveççe", TranslateLanguage.SWEDISH);
        languageCodes.put("Tagalogca", TranslateLanguage.TAGALOG);
        languageCodes.put("Tayca", TranslateLanguage.THAI);
        languageCodes.put("Ukraynaca", TranslateLanguage.UKRAINIAN);
        languageCodes.put("Urduca", TranslateLanguage.URDU);
        languageCodes.put("Vietnamca", TranslateLanguage.VIETNAMESE);
        languageCodes.put("Galce", TranslateLanguage.WELSH);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            dpToPx(340) // Daha büyük - rahat görünsün
        ));
        setBackgroundColor(0xFF0A0A0A);

        // Premium Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        
        android.graphics.drawable.GradientDrawable headerBg = new android.graphics.drawable.GradientDrawable();
        headerBg.setColor(0xFF1A1A1A);
        header.setBackground(headerBg);
        
        LayoutParams headerParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        headerParams.bottomMargin = dpToPx(8);
        
        TextView title = new TextView(context);
        title.setText("Çeviri");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setLetterSpacing(0.02f);
        LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);

        ImageButton closeBtn = new ImageButton(context);
        android.graphics.drawable.GradientDrawable closeBg = new android.graphics.drawable.GradientDrawable();
        closeBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        closeBg.setCornerRadius(dpToPx(6));
        closeBg.setColor(0xFF2A2A2A);
        closeBtn.setBackground(closeBg);
        closeBtn.setImageResource(com.qrmaster.app.R.drawable.ic_close);
        closeBtn.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        closeBtn.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        closeBtn.setColorFilter(0xFFFFFFFF);
        LayoutParams closeParams = new LayoutParams(dpToPx(30), dpToPx(30));
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> { if (callback != null) callback.onClose(); });
        header.addView(closeBtn);
        addView(header, headerParams);

        // Modern Language Selector
        addView(createModernLanguageSelector(context));
        
        // Input/Output Container
        addView(createTextContainer(context));
        
        // Action Buttons
        addView(createActionButtons(context));
    }

    private LinearLayout createModernLanguageSelector(Context context) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(HORIZONTAL);
        container.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));
        container.setGravity(Gravity.CENTER_VERTICAL);
        
        LayoutParams containerParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        containerParams.bottomMargin = dpToPx(8);
        container.setLayoutParams(containerParams);

        // Source Language
        sourceLangSpinner = createModernSpinner(context);
        LayoutParams sourceParams = new LayoutParams(0, dpToPx(44), 1f);
        sourceParams.rightMargin = dpToPx(6);
        sourceLangSpinner.setLayoutParams(sourceParams);
        sourceLangSpinner.setSelection(0); // Türkçe default
        container.addView(sourceLangSpinner);
        
        // Swap button
        Button swapBtn = new Button(context);
        swapBtn.setText("⇄");
        swapBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        swapBtn.setTextColor(0xFFFFFFFF);
        swapBtn.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        
        android.graphics.drawable.GradientDrawable swapBg = new android.graphics.drawable.GradientDrawable();
        swapBg.setCornerRadius(dpToPx(8));
        swapBg.setColor(0xFF2A2A2A);
        swapBtn.setBackground(swapBg);
        
        LayoutParams swapParams = new LayoutParams(dpToPx(44), dpToPx(44));
        swapParams.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        swapBtn.setLayoutParams(swapParams);
        swapBtn.setOnClickListener(v -> swapLanguages());
        container.addView(swapBtn);

        // Target Language
        targetLangSpinner = createModernSpinner(context);
        LayoutParams targetParams = new LayoutParams(0, dpToPx(44), 1f);
        targetParams.leftMargin = dpToPx(6);
        targetLangSpinner.setLayoutParams(targetParams);
        targetLangSpinner.setSelection(1); // İngilizce default
        container.addView(targetLangSpinner);

        return container;
    }

    private Spinner createModernSpinner(Context context) {
        Spinner spinner = new Spinner(context);
        
        android.graphics.drawable.GradientDrawable spinnerBg = new android.graphics.drawable.GradientDrawable();
        spinnerBg.setCornerRadius(dpToPx(8));
        spinnerBg.setColor(0xFF1A1A1A);
        spinnerBg.setStroke(dpToPx(1), 0xFF2A2A2A);
        spinner.setBackground(spinnerBg);
        spinner.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));

        List<String> languages = new ArrayList<>(languageCodes.keySet());
        
        // Custom adapter - modern style
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            context,
            android.R.layout.simple_spinner_item,
            languages
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(0xFFFFFFFF);
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                view.setPadding(0, 0, 0, 0);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(0xFFFFFFFF);
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                view.setBackgroundColor(position % 2 == 0 ? 0xFF1A1A1A : 0xFF2A2A2A);
                view.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
                return view;
            }
        };
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        
        return spinner;
    }

    private LinearLayout createTextContainer(Context context) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        
        android.graphics.drawable.GradientDrawable containerBg = new android.graphics.drawable.GradientDrawable();
        containerBg.setCornerRadius(dpToPx(10));
        containerBg.setColor(0xFF1A1A1A);
        container.setBackground(containerBg);

        LayoutParams containerParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            0,
            1f
        );
        containerParams.setMargins(dpToPx(8), 0, dpToPx(8), dpToPx(8));
        container.setLayoutParams(containerParams);

        // Input label
        TextView inputLabel = new TextView(context);
        inputLabel.setText("Metin");
        inputLabel.setTextColor(0xFF666666);
        inputLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        inputLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        LayoutParams labelParams = new LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        );
        labelParams.bottomMargin = dpToPx(4);
        container.addView(inputLabel, labelParams);

        // Input text
        inputPreview = new TextView(context);
        inputPreview.setText("Yapıştır ile metin ekle");
        inputPreview.setTextColor(0xFF8E8E93);
        inputPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        inputPreview.setMaxLines(2);
        inputPreview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LayoutParams inputParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        inputParams.bottomMargin = dpToPx(12);
        container.addView(inputPreview, inputParams);
        
        // Output label
        TextView outputLabel = new TextView(context);
        outputLabel.setText("Çeviri");
        outputLabel.setTextColor(0xFF666666);
        outputLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        outputLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        LayoutParams outLabelParams = new LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        );
        outLabelParams.bottomMargin = dpToPx(4);
        container.addView(outputLabel, outLabelParams);
        
        // Output text
        outputText = new TextView(context);
        outputText.setText("");
        outputText.setTextColor(0xFFFFFFFF);
        outputText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        outputText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        outputText.setMaxLines(3);
        container.addView(outputText, new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ));
        
        // Status area
        LinearLayout statusArea = new LinearLayout(context);
        statusArea.setOrientation(HORIZONTAL);
        statusArea.setGravity(Gravity.CENTER);
        statusArea.setPadding(0, dpToPx(8), 0, 0);
        
        progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(GONE);
        LayoutParams progressParams = new LayoutParams(dpToPx(16), dpToPx(16));
        progressParams.rightMargin = dpToPx(8);
        statusArea.addView(progressBar, progressParams);
        
        statusText = new TextView(context);
        statusText.setTextColor(0xFF666666);
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        statusText.setVisibility(GONE);
        statusArea.addView(statusText);
        
        container.addView(statusArea);

        return container;
    }

    private LinearLayout createActionButtons(Context context) {
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(HORIZONTAL);
        actions.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));
        actions.setGravity(Gravity.CENTER);
        
        LayoutParams actionsParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        actions.setLayoutParams(actionsParams);

        // Paste button
        Button pasteBtn = createModernButton(context, "Yapıştır", 0xFF2A2A2A);
        pasteBtn.setOnClickListener(v -> pasteText());
        actions.addView(pasteBtn, new LayoutParams(0, dpToPx(40), 1f));

        // Translate button
        Button translateBtn = createModernButton(context, "Çevir", 0xFF0A84FF);
        translateBtn.setOnClickListener(v -> translate());
        LayoutParams translateParams = new LayoutParams(0, dpToPx(40), 1f);
        translateParams.setMargins(dpToPx(6), 0, dpToPx(6), 0);
        actions.addView(translateBtn, translateParams);

        // Use button
        Button useBtn = createModernButton(context, "Kullan", 0xFF34C759);
        useBtn.setOnClickListener(v -> useTranslation());
        actions.addView(useBtn, new LayoutParams(0, dpToPx(40), 1f));

        return actions;
            }

    private Button createModernButton(Context context, String text, int color) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        button.setTextColor(0xFFFFFFFF);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setAllCaps(false);
        button.setLetterSpacing(0.02f);
        
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setCornerRadius(dpToPx(8));
        btnBg.setColor(color);
        button.setBackground(btnBg);
        button.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        
        return button;
            }

    private void swapLanguages() {
        int sourcePos = sourceLangSpinner.getSelectedItemPosition();
        int targetPos = targetLangSpinner.getSelectedItemPosition();
        
        sourceLangSpinner.setSelection(targetPos);
        targetLangSpinner.setSelection(sourcePos);
        
        if (!currentInput.isEmpty() && !outputText.getText().toString().isEmpty()) {
            String temp = currentInput;
            currentInput = outputText.getText().toString();
            inputPreview.setText(currentInput);
            inputPreview.setTextColor(0xFFFFFFFF);
            outputText.setText("");
            translate();
        }
    }

    private void pasteText() {
        android.content.ClipboardManager clipboard = 
            (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            android.content.ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            CharSequence text = item.getText();
            if (text != null) {
                currentInput = text.toString();
                inputPreview.setText(currentInput);
                inputPreview.setTextColor(0xFFFFFFFF);
            }
        }
    }

    private void translate() {
        if (currentInput.isEmpty()) {
            Toast.makeText(getContext(), "Önce metin yapıştırın", Toast.LENGTH_SHORT).show();
            return;
        }

        String sourceText = sourceLangSpinner.getSelectedItem().toString();
        String targetText = targetLangSpinner.getSelectedItem().toString();

        String sourceCode = languageCodes.get(sourceText);
        String targetCode = languageCodes.get(targetText);
        
        if (sourceCode == null || targetCode == null) return;

        if (!sourceCode.equals(currentSource) || !targetCode.equals(currentTarget)) {
            if (currentTranslator != null) {
                currentTranslator.close();
            }
            
            TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(targetCode)
                .build();
            currentTranslator = Translation.getClient(options);
            currentSource = sourceCode;
            currentTarget = targetCode;
        }

        progressBar.setVisibility(VISIBLE);
        statusText.setVisibility(VISIBLE);
        statusText.setText("Çevriliyor...");

        DownloadConditions conditions = new DownloadConditions.Builder().build();
        currentTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener(v -> {
                currentTranslator.translate(currentInput)
                    .addOnSuccessListener(translatedText -> {
                        outputText.setText(translatedText);
                        progressBar.setVisibility(GONE);
                        statusText.setVisibility(GONE);
                    })
            .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Çeviri hatası", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(GONE);
                        statusText.setText("Hata");
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Model indirilemedi", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(GONE);
                statusText.setText("Hata");
            });
    }

    private void useTranslation() {
        String translation = outputText.getText().toString();
        if (!translation.isEmpty() && callback != null) {
            callback.onTranslated(translation);
    }
    }

    public void release() {
        if (currentTranslator != null) {
            currentTranslator.close();
            currentTranslator = null;
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
