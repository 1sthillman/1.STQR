package com.qrmaster.app.keyboard.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.qrmaster.app.keyboard.TextTemplateManager;
import com.qrmaster.app.keyboard.TextTemplateManager.TextTemplate;
import java.util.List;

public class TextTemplateView extends LinearLayout {
    
    public interface Callback {
        void onTemplateSelected(String text);
        void onClose();
    }
    
    private final Callback callback;
    private final TextTemplateManager templateManager;
    private LinearLayout templateList;
    
    public TextTemplateView(Context context, Callback callback) {
        super(context);
        this.callback = callback;
        this.templateManager = new TextTemplateManager(context);
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp(300)));
        setBackgroundColor(0xFF1C1C1E);
        setPadding(dp(12), dp(8), dp(12), dp(8));
        
        // Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView title = new TextView(context);
        title.setText("📝 Hızlı Metinler");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);
        
        // Add button
        Button addBtn = createButton(context, "➕");
        addBtn.setOnClickListener(v -> showAddDialog());
        header.addView(addBtn);
        
        // Close button
        Button closeBtn = createButton(context, "❌");
        closeBtn.setOnClickListener(v -> callback.onClose());
        LinearLayout.LayoutParams closeParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        closeParams.leftMargin = dp(8);
        header.addView(closeBtn, closeParams);
        
        addView(header);
        
        // Category tabs
        LinearLayout tabs = new LinearLayout(context);
        tabs.setOrientation(HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tabsParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tabsParams.topMargin = dp(8);
        
        String[] categories = {"Tümü", "Hashtag", "İmza", "Adres", "Diğer"};
        String[] categoryKeys = {"all", "hashtag", "signature", "address", "other"};
        
        for (int i = 0; i < categories.length; i++) {
            final String cat = categoryKeys[i];
            Button tab = createTabButton(context, categories[i]);
            tab.setOnClickListener(v -> loadTemplates(cat));
            LinearLayout.LayoutParams tabParam = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            tabParam.setMargins(dp(2), 0, dp(2), 0);
            tabs.addView(tab, tabParam);
        }
        
        addView(tabs, tabsParams);
        
        // Template list
        ScrollView scrollView = new ScrollView(context);
        templateList = new LinearLayout(context);
        templateList.setOrientation(VERTICAL);
        scrollView.addView(templateList);
        
        LayoutParams scrollParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.topMargin = dp(8);
        addView(scrollView, scrollParams);
        
        // Load all templates
        loadTemplates("all");
    }
    
    private void loadTemplates(String category) {
        templateList.removeAllViews();
        
        List<TextTemplate> templates = category.equals("all") 
            ? templateManager.getAllTemplates()
            : templateManager.getTemplatesByCategory(category);
        
        if (templates.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("Henüz şablon yok. ➕ ile ekleyin!");
            empty.setTextColor(0xFF8E8E93);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(40), dp(20), dp(40));
            templateList.addView(empty);
            return;
        }
        
        for (TextTemplate template : templates) {
            templateList.addView(createTemplateCard(template));
        }
    }
    
    private View createTemplateCard(TextTemplate template) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF2C2C2E);
        bg.setCornerRadius(dp(10));
        card.setBackground(bg);
        
        LinearLayout.LayoutParams cardParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, dp(4), 0, dp(4));
        card.setLayoutParams(cardParams);
        
        // Title row
        LinearLayout titleRow = new LinearLayout(getContext());
        titleRow.setOrientation(HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView name = new TextView(getContext());
        name.setText(template.name);
        name.setTextColor(Color.WHITE);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams nameParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        titleRow.addView(name, nameParams);
        
        // Delete button
        Button delBtn = createSmallButton(getContext(), "🗑️");
        delBtn.setOnClickListener(v -> {
            templateManager.deleteTemplate(template.id);
            loadTemplates("all");
        });
        titleRow.addView(delBtn);
        
        card.addView(titleRow);
        
        // Content preview
        TextView content = new TextView(getContext());
        content.setText(template.content);
        content.setTextColor(0xFFAAAAAA);
        content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        content.setMaxLines(2);
        content.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams contentParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        contentParams.topMargin = dp(4);
        card.addView(content, contentParams);
        
        // Click to use
        card.setOnClickListener(v -> {
            callback.onTemplateSelected(template.content);
        });
        
        return card;
    }
    
    private void showAddDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Yeni Şablon");
        
        LinearLayout dialogLayout = new LinearLayout(getContext());
        dialogLayout.setOrientation(VERTICAL);
        dialogLayout.setPadding(dp(20), dp(10), dp(20), dp(10));
        
        EditText nameInput = new EditText(getContext());
        nameInput.setHint("Şablon adı");
        dialogLayout.addView(nameInput);
        
        EditText contentInput = new EditText(getContext());
        contentInput.setHint("Metin içeriği");
        contentInput.setMinLines(3);
        LinearLayout.LayoutParams contentParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        contentParams.topMargin = dp(8);
        dialogLayout.addView(contentInput, contentParams);
        
        builder.setView(dialogLayout);
        builder.setPositiveButton("Kaydet", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String content = contentInput.getText().toString().trim();
            if (!name.isEmpty() && !content.isEmpty()) {
                templateManager.addTemplate(name, content, "other");
                loadTemplates("all");
            }
        });
        builder.setNegativeButton("İptal", null);
        builder.show();
    }
    
    private Button createButton(Context context, String text) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setPadding(dp(12), dp(6), dp(12), dp(6));
        btn.setMinWidth(0);
        btn.setMinHeight(0);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF007AFF);
        bg.setCornerRadius(dp(8));
        btn.setBackground(bg);
        
        return btn;
    }
    
    private Button createTabButton(Context context, String text) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btn.setPadding(dp(8), dp(6), dp(8), dp(6));
        btn.setMinWidth(0);
        btn.setMinHeight(0);
        btn.setAllCaps(false);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF3A3A3C);
        bg.setCornerRadius(dp(8));
        btn.setBackground(bg);
        
        return btn;
    }
    
    private Button createSmallButton(Context context, String text) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setPadding(dp(6), dp(4), dp(6), dp(4));
        btn.setMinWidth(0);
        btn.setMinHeight(0);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFF3B30);
        bg.setCornerRadius(dp(6));
        btn.setBackground(bg);
        
        return btn;
    }
    
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}








