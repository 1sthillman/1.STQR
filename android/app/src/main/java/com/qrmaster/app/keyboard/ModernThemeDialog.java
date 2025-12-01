package com.qrmaster.app.keyboard;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🎨 PREMIUM THEME DIALOG
 * 
 * Modern, scroll, gradient preview, SVG-like keyboard preview
 * 50+ themes, 15+ shapes, custom gallery support
 */
public class ModernThemeDialog extends Dialog {
    private Context context;
    private ThemeCallback callback;
    private SharedPreferences prefs;
    
    private String selectedTheme = "dark";
    private String selectedShape = "rounded";
    private int currentTab = 0; // 0=Themes, 1=Shapes, 2=Gallery, 3=Style
    
    // UI References
    private LinearLayout contentContainer;
    private Button themesTab, shapesTab, galleryTab, styleTab;
    
    private ThemeStyleConfig styleConfig;
    
    // Modern tema listesi - Gradient destekli
    private final Map<String, ThemeData> themes = new LinkedHashMap<>();
    private final Map<String, ShapeData> shapes = new LinkedHashMap<>();
    
    public interface ThemeCallback {
        void onThemeSelected(String theme, String shape);
        void onGalleryPhotoSelected();
    }
    
    static class ThemeData {
        String name;
        int[] colors; // Gradient için 2-3 renk
        String icon;
        
        ThemeData(String name, int[] colors, String icon) {
            this.name = name;
            this.colors = colors;
            this.icon = icon;
        }
    }
    
    static class ShapeData {
        String name;
        String displayName;
        float cornerRadius;
        String preview;
        
        ShapeData(String name, String displayName, float cornerRadius, String preview) {
            this.name = name;
            this.displayName = displayName;
            this.cornerRadius = cornerRadius;
            this.preview = preview;
        }
    }
    
    public ModernThemeDialog(Context context, ThemeCallback callback) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.context = context;
        this.callback = callback;
        this.prefs = context.getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE);
        
        initThemes();
        initShapes();
        loadSelections();
        
        styleConfig = new ThemeStyleConfig(context);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(createModernLayout());
        
        // CRITICAL: Set window type for InputMethodService context
        Window window = getWindow();
        if (window != null) {
            try {
                window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
                
                // Get window token from InputMethodService
                if (context instanceof android.inputmethodservice.InputMethodService) {
                    android.inputmethodservice.InputMethodService ims = (android.inputmethodservice.InputMethodService) context;
                    android.view.View rootView = ims.getWindow().getWindow().getDecorView();
                    if (rootView != null) {
                        WindowManager.LayoutParams lp = window.getAttributes();
                        lp.token = rootView.getWindowToken();
                        window.setAttributes(lp);
                    }
                }
            } catch (Exception e) {
                // Fallback to TYPE_SYSTEM_ALERT if token issue
                window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            }
        }
    }
    
    private void initThemes() {
        // 🎨 DARK THEMES (10)
        themes.put("dark", new ThemeData("Dark", new int[]{0xFF0A0A0A, 0xFF1A1A1A}, "🌑"));
        themes.put("midnight", new ThemeData("Midnight Blue", new int[]{0xFF0D1117, 0xFF161B22, 0xFF21262D}, "🌌"));
        themes.put("amoled", new ThemeData("AMOLED", new int[]{0xFF000000, 0xFF050505}, "⚫"));
        themes.put("slate", new ThemeData("Slate Gray", new int[]{0xFF1E293B, 0xFF334155}, "🪨"));
        themes.put("charcoal", new ThemeData("Charcoal", new int[]{0xFF2A2A2A, 0xFF3A3A3A}, "🖤"));
        themes.put("navy", new ThemeData("Deep Navy", new int[]{0xFF001F3F, 0xFF002855}, "🌊"));
        themes.put("forest", new ThemeData("Forest Night", new int[]{0xFF0F1A0F, 0xFF1A2A1A}, "🌲"));
        themes.put("wine", new ThemeData("Wine Red", new int[]{0xFF2A0A0A, 0xFF3A1A1A}, "🍷"));
        themes.put("carbon", new ThemeData("Carbon Fiber", new int[]{0xFF181818, 0xFF282828, 0xFF1A1A1A}, "💎"));
        themes.put("obsidian", new ThemeData("Obsidian", new int[]{0xFF0C0C0C, 0xFF1C1C1C}, "🖤"));
        
        // ☀️ LIGHT THEMES (8)
        themes.put("light", new ThemeData("Light", new int[]{0xFFF5F5F5, 0xFFFFFFFF}, "☀️"));
        themes.put("snow", new ThemeData("Snow White", new int[]{0xFFFAFAFA, 0xFFFFFFFF}, "❄️"));
        themes.put("cream", new ThemeData("Cream", new int[]{0xFFFFF8DC, 0xFFFFFAF0}, "🥛"));
        themes.put("pearl", new ThemeData("Pearl", new int[]{0xFFF0F0F0, 0xFFFFFFFE}, "🤍"));
        themes.put("cloud", new ThemeData("Cloud", new int[]{0xFFE8E8E8, 0xFFF5F5F5}, "☁️"));
        themes.put("linen", new ThemeData("Linen", new int[]{0xFFFAF0E6, 0xFFFFF5EE}, "🏳️"));
        themes.put("mint", new ThemeData("Mint Light", new int[]{0xFFF0FFF4, 0xFFF5FFFA}, "🌿"));
        themes.put("lavender", new ThemeData("Lavender", new int[]{0xFFF5F0FF, 0xFFFAF5FF}, "💜"));
        
        // 🌈 GRADIENT THEMES (12)
        themes.put("sunset", new ThemeData("Sunset", new int[]{0xFFFF6B6B, 0xFFFFAA00, 0xFFFF8800}, "🌅"));
        themes.put("ocean", new ThemeData("Ocean", new int[]{0xFF0EA5E9, 0xFF0284C7, 0xFF0369A1}, "🌊"));
        themes.put("aurora", new ThemeData("Aurora", new int[]{0xFF4C1D95, 0xFF7C3AED, 0xFFA855F7}, "🌌"));
        themes.put("forest_green", new ThemeData("Forest Green", new int[]{0xFF047857, 0xFF059669, 0xFF10B981}, "🌲"));
        themes.put("rose", new ThemeData("Rose Gold", new int[]{0xFFE11D48, 0xFFF43F5E, 0xFFFB7185}, "🌹"));
        themes.put("cyber", new ThemeData("Cyberpunk", new int[]{0xFFEC4899, 0xFF8B5CF6, 0xFF06B6D4}, "🤖"));
        themes.put("fire", new ThemeData("Fire", new int[]{0xFFDC2626, 0xFFEA580C, 0xFFF97316}, "🔥"));
        themes.put("ice", new ThemeData("Ice Blue", new int[]{0xFF3B82F6, 0xFF60A5FA, 0xFF93C5FD}, "❄️"));
        themes.put("gold", new ThemeData("Golden", new int[]{0xFFCA8A04, 0xFFEAB308, 0xFFFBBF24}, "✨"));
        themes.put("emerald", new ThemeData("Emerald", new int[]{0xFF047857, 0xFF10B981, 0xFF34D399}, "💚"));
        themes.put("purple_haze", new ThemeData("Purple Haze", new int[]{0xFF6B21A8, 0xFF9333EA, 0xFFA855F7}, "💜"));
        themes.put("neon", new ThemeData("Neon Pink", new int[]{0xFFEC4899, 0xFFF472B6, 0xFFFBBF24}, "💖"));
        
        // 🎯 MATERIAL THEMES (8)
        themes.put("material_red", new ThemeData("Material Red", new int[]{0xFFEF4444, 0xFFF87171}, "🔴"));
        themes.put("material_blue", new ThemeData("Material Blue", new int[]{0xFF3B82F6, 0xFF60A5FA}, "🔵"));
        themes.put("material_green", new ThemeData("Material Green", new int[]{0xFF22C55E, 0xFF4ADE80}, "🟢"));
        themes.put("material_purple", new ThemeData("Material Purple", new int[]{0xFF9333EA, 0xFFA855F7}, "🟣"));
        themes.put("material_orange", new ThemeData("Material Orange", new int[]{0xFFF97316, 0xFFFB923C}, "🟠"));
        themes.put("material_teal", new ThemeData("Material Teal", new int[]{0xFF14B8A6, 0xFF2DD4BF}, "🔷"));
        themes.put("material_pink", new ThemeData("Material Pink", new int[]{0xFFEC4899, 0xFFF472B6}, "🩷"));
        themes.put("material_indigo", new ThemeData("Material Indigo", new int[]{0xFF6366F1, 0xFF818CF8}, "🔵"));
        
        // 🌸 PASTEL THEMES (8)
        themes.put("pastel_pink", new ThemeData("Pastel Pink", new int[]{0xFFFFC0CB, 0xFFFFD9E3}, "🌸"));
        themes.put("pastel_blue", new ThemeData("Pastel Blue", new int[]{0xFFADD8E6, 0xFFB0E0E6}, "💙"));
        themes.put("pastel_mint", new ThemeData("Pastel Mint", new int[]{0xFF98FF98, 0xFFB2FFD8}, "🍃"));
        themes.put("pastel_peach", new ThemeData("Pastel Peach", new int[]{0xFFFFDAB9, 0xFFFFE4C4}, "🍑"));
        themes.put("pastel_lavender", new ThemeData("Pastel Lavender", new int[]{0xFFE6E6FA, 0xFFF0E6FF}, "💜"));
        themes.put("pastel_yellow", new ThemeData("Pastel Yellow", new int[]{0xFFFFFACD, 0xFFFFFFE0}, "💛"));
        themes.put("pastel_coral", new ThemeData("Pastel Coral", new int[]{0xFFFF7F50, 0xFFFF9F80}, "🪸"));
        themes.put("pastel_aqua", new ThemeData("Pastel Aqua", new int[]{0xFF7FFFD4, 0xFF9FFFE4}, "🧊"));
        
        // 🎮 GAMING THEMES (6)
        themes.put("rgb", new ThemeData("RGB Gaming", new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF}, "🎮"));
        themes.put("matrix", new ThemeData("Matrix", new int[]{0xFF003300, 0xFF00FF00}, "💻"));
        themes.put("synthwave", new ThemeData("Synthwave", new int[]{0xFFFF00FF, 0xFF00FFFF, 0xFFFFFF00}, "🌆"));
        themes.put("retro", new ThemeData("Retro Wave", new int[]{0xFFFF1493, 0xFF00CED1, 0xFFFFD700}, "📼"));
        themes.put("gamer", new ThemeData("Gamer", new int[]{0xFF7C3AED, 0xFFEC4899, 0xFF06B6D4}, "👾"));
        themes.put("console", new ThemeData("Console", new int[]{0xFF1E293B, 0xFF334155, 0xFF0EA5E9}, "🎮"));
    }
    
    private void initShapes() {
        // Basic Shapes (5)
        shapes.put("square", new ShapeData("square", "Square", 0f, "▢"));
        shapes.put("rounded", new ShapeData("rounded", "Rounded", 12f, "⬜"));
        shapes.put("soft", new ShapeData("soft", "Soft", 8f, "◻"));
        shapes.put("sharp", new ShapeData("sharp", "Sharp", 4f, "▫"));
        shapes.put("pill", new ShapeData("pill", "Pill", 50f, "⬯"));
        
        // Modern Shapes (5)
        shapes.put("ultra_round", new ShapeData("ultra_round", "Ultra Round", 24f, "●"));
        shapes.put("bubble", new ShapeData("bubble", "Bubble", 18f, "◉"));
        shapes.put("neo", new ShapeData("neo", "Neo", 14f, "◾"));
        shapes.put("fluid", new ShapeData("fluid", "Fluid", 20f, "⬬"));
        shapes.put("smooth", new ShapeData("smooth", "Smooth", 15f, "⚫"));
        
        // Minimal Shapes (5)
        shapes.put("minimal", new ShapeData("minimal", "Minimal", 6f, "□"));
        shapes.put("flat", new ShapeData("flat", "Flat", 2f, "▪"));
        shapes.put("retro", new ShapeData("retro", "Retro", 3f, "▫"));
        shapes.put("classic", new ShapeData("classic", "Classic", 5f, "◻"));
        shapes.put("clean", new ShapeData("clean", "Clean", 7f, "⬜"));
        
        // Bold Shapes (5)
        shapes.put("bold", new ShapeData("bold", "Bold", 10f, "◼"));
        shapes.put("thick", new ShapeData("thick", "Thick", 11f, "◼"));
        shapes.put("heavy", new ShapeData("heavy", "Heavy", 9f, "◼"));
        shapes.put("strong", new ShapeData("strong", "Strong", 13f, "◼"));
        shapes.put("solid", new ShapeData("solid", "Solid", 8f, "◼"));
        
        // Special Shapes (5)
        shapes.put("cyber", new ShapeData("cyber", "Cyber", 1f, "▮"));
        shapes.put("neon", new ShapeData("neon", "Neon", 16f, "◉"));
        shapes.put("glass", new ShapeData("glass", "Glass", 14f, "◎"));
        shapes.put("crystal", new ShapeData("crystal", "Crystal", 18f, "◈"));
        shapes.put("diamond", new ShapeData("diamond", "Diamond", 6f, "◆"));
        
        // Extra Premium (5)
        shapes.put("luxury", new ShapeData("luxury", "Luxury", 22f, "◉"));
        shapes.put("elegant", new ShapeData("elegant", "Elegant", 17f, "◇"));
        shapes.put("premium", new ShapeData("premium", "Premium", 19f, "◆"));
        shapes.put("vip", new ShapeData("vip", "VIP", 21f, "◈"));
        shapes.put("elite", new ShapeData("elite", "Elite", 23f, "◉"));
    }
    
    private void loadSelections() {
        selectedTheme = prefs.getString("selected_theme", "dark");
        selectedShape = prefs.getString("selected_shape", "rounded");
    }
    
    private View createModernLayout() {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        root.setBackgroundColor(0xFF0A0A0A);
        
        // Header
        root.addView(createPremiumHeader());
        
        // Tabs (Themes / Shapes / Gallery)
        root.addView(createTabBar());
        
        // Content (Scrollable)
        root.addView(createScrollContent());
        
        // Apply Button
        root.addView(createApplyButton());
        
        return root;
    }
    
    private LinearLayout createPremiumHeader() {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(12), dp(16), dp(12));
        
        GradientDrawable headerBg = new GradientDrawable();
        headerBg.setColor(0xFF1A1A1A);
        header.setBackground(headerBg);
        
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        header.setLayoutParams(headerParams);
        
        TextView title = new TextView(context);
        title.setText("Tema & Şekil");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setLetterSpacing(0.02f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        header.addView(title, titleParams);
        
        ImageButton closeBtn = new ImageButton(context);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(GradientDrawable.RECTANGLE);
        closeBg.setCornerRadius(dp(8));
        closeBg.setColor(0xFF2A2A2A);
        closeBtn.setBackground(closeBg);
        closeBtn.setImageResource(com.qrmaster.app.R.drawable.ic_close);
        closeBtn.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        closeBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
        closeBtn.setColorFilter(0xFFFFFFFF);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> dismiss());
        header.addView(closeBtn);
        
        return header;
    }
    
    private LinearLayout createTabBar() {
        LinearLayout tabBar = new LinearLayout(context);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setPadding(dp(12), dp(8), dp(12), dp(8));
        tabBar.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams tabBarParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tabBar.setLayoutParams(tabBarParams);
        
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
            0,
            dp(42),
            1f
        );
        tabParams.setMargins(dp(4), 0, dp(4), 0);
        
        // Themes tab
        themesTab = createTabButton("Temalar (54)", true);
        themesTab.setOnClickListener(v -> switchTab(0));
        tabBar.addView(themesTab, tabParams);
        
        // Shapes tab
        shapesTab = createTabButton("Şekiller (30)", false);
        shapesTab.setOnClickListener(v -> switchTab(1));
        tabBar.addView(shapesTab, tabParams);
        
        // Gallery tab
        galleryTab = createTabButton("Galeri", false);
        galleryTab.setOnClickListener(v -> switchTab(2));
        tabBar.addView(galleryTab, tabParams);
        
        // Style tab
        styleTab = createTabButton("Stil", false);
        styleTab.setOnClickListener(v -> switchTab(3));
        tabBar.addView(styleTab, tabParams);
        
        return tabBar;
    }
    
    private void switchTab(int tab) {
        currentTab = tab;
        
        // Update tab buttons
        updateTabButton(themesTab, tab == 0);
        updateTabButton(shapesTab, tab == 1);
        updateTabButton(galleryTab, tab == 2);
        updateTabButton(styleTab, tab == 3);
        
        // Update content
        updateContent();
    }
    
    private void updateTabButton(Button button, boolean selected) {
        button.setTextColor(selected ? 0xFFFFFFFF : 0xFF666666);
        button.setTypeface(Typeface.create("sans-serif-medium", selected ? Typeface.BOLD : Typeface.NORMAL));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(selected ? 0xFF0A84FF : 0xFF2A2A2A);
        button.setBackground(bg);
    }
    
    private Button createTabButton(String text, boolean selected) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        button.setTextColor(selected ? 0xFFFFFFFF : 0xFF666666);
        button.setTypeface(Typeface.create("sans-serif-medium", selected ? Typeface.BOLD : Typeface.NORMAL));
        button.setAllCaps(false);
        button.setLetterSpacing(0.02f);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(selected ? 0xFF0A84FF : 0xFF2A2A2A);
        button.setBackground(bg);
        
        return button;
    }
    
    private ScrollView createScrollContent() {
        ScrollView scroll = new ScrollView(context);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        );
        scroll.setLayoutParams(scrollParams);
        
        contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(dp(12), dp(8), dp(12), dp(8));
        
        // Initial content (Themes)
        updateContent();
        
        scroll.addView(contentContainer);
        return scroll;
    }
    
    private void updateContent() {
        contentContainer.removeAllViews();
        
        switch (currentTab) {
            case 0: // Themes
                contentContainer.addView(createThemeCategory("DARK THEMES", getDarkThemes()));
                contentContainer.addView(createThemeCategory("LIGHT THEMES", getLightThemes()));
                contentContainer.addView(createThemeCategory("GRADIENT THEMES", getGradientThemes()));
                contentContainer.addView(createThemeCategory("MATERIAL THEMES", getMaterialThemes()));
                contentContainer.addView(createThemeCategory("PASTEL THEMES", getPastelThemes()));
                contentContainer.addView(createThemeCategory("GAMING THEMES", getGamingThemes()));
                break;
                
            case 1: // Shapes
                contentContainer.addView(createShapesContent());
                break;
                
            case 2: // Gallery
                contentContainer.addView(createGalleryContent());
                break;
                
            case 3: // Style
                contentContainer.addView(createStyleContent());
                break;
        }
    }
    
    private LinearLayout createShapesContent() {
        LinearLayout shapesLayout = new LinearLayout(context);
        shapesLayout.setOrientation(LinearLayout.VERTICAL);
        
        // Keyboard preview
        shapesLayout.addView(createKeyboardPreview());
        
        // Shape grid (3 columns)
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        gridParams.topMargin = dp(16);
        grid.setLayoutParams(gridParams);
        
        LinearLayout row = null;
        int count = 0;
        
        for (Map.Entry<String, ShapeData> entry : shapes.entrySet()) {
            if (count % 3 == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row);
            }
            
            row.addView(createShapeCard(entry.getKey(), entry.getValue()));
            count++;
        }
        
        shapesLayout.addView(grid);
        return shapesLayout;
    }
    
    private LinearLayout createKeyboardPreview() {
        LinearLayout preview = new LinearLayout(context);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setGravity(Gravity.CENTER);
        preview.setPadding(dp(16), dp(12), dp(16), dp(12));
        
        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setCornerRadius(dp(12));
        previewBg.setColor(0xFF1A1A1A);
        preview.setBackground(previewBg);
        
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        preview.setLayoutParams(previewParams);
        
        TextView label = new TextView(context);
        label.setText("ÖNİZLEME");
        label.setTextColor(0xFF666666);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.bottomMargin = dp(12);
        preview.addView(label, labelParams);
        
        // Mini keyboard (3 rows)
        String[][] keys = {
            {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"},
            {"A", "S", "D", "F", "G", "H", "J", "K", "L"},
            {"Z", "X", "C", "V", "B", "N", "M"}
        };
        
        for (String[] keyRow : keys) {
            LinearLayout keyboardRow = new LinearLayout(context);
            keyboardRow.setOrientation(LinearLayout.HORIZONTAL);
            keyboardRow.setGravity(Gravity.CENTER);
            
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.bottomMargin = dp(4);
            keyboardRow.setLayoutParams(rowParams);
            
            for (String key : keyRow) {
                TextView keyView = new TextView(context);
                keyView.setText(key);
                keyView.setTextColor(0xFFFFFFFF);
                keyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                keyView.setGravity(Gravity.CENTER);
                
                ShapeData currentShape = shapes.get(selectedShape);
                float radius = currentShape != null ? currentShape.cornerRadius : 12f;
                
                GradientDrawable keyBg = new GradientDrawable();
                keyBg.setCornerRadius(dp((int)radius));
                keyBg.setColor(0xFF2A2A2A);
                keyView.setBackground(keyBg);
                
                LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(
                    dp(28),
                    dp(32)
                );
                keyParams.setMargins(dp(2), 0, dp(2), 0);
                keyView.setLayoutParams(keyParams);
                
                keyboardRow.addView(keyView);
            }
            
            preview.addView(keyboardRow);
        }
        
        return preview;
    }
    
    private View createShapeCard(String key, ShapeData shape) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(12), dp(14), dp(12), dp(14));
        
        boolean isSelected = key.equals(selectedShape);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(12));
        cardBg.setColor(0xFF1A1A1A);
        cardBg.setStroke(dp(2), isSelected ? 0xFF0A84FF : 0xFF2A2A2A);
        card.setBackground(cardBg);
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        cardParams.setMargins(dp(4), dp(4), dp(4), dp(4));
        card.setLayoutParams(cardParams);
        
        // Shape preview (3 mini keys)
        LinearLayout previewRow = new LinearLayout(context);
        previewRow.setOrientation(LinearLayout.HORIZONTAL);
        previewRow.setGravity(Gravity.CENTER);
        
        for (int i = 0; i < 3; i++) {
            View miniKey = new View(context);
            GradientDrawable keyBg = new GradientDrawable();
            keyBg.setCornerRadius(dp((int)shape.cornerRadius));
            keyBg.setColor(0xFF2A2A2A);
            miniKey.setBackground(keyBg);
            
            LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(
                dp(24),
                dp(28)
            );
            keyParams.setMargins(dp(2), 0, dp(2), 0);
            previewRow.addView(miniKey, keyParams);
        }
        
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        previewParams.bottomMargin = dp(10);
        card.addView(previewRow, previewParams);
        
        // Shape name
        TextView name = new TextView(context);
        name.setText(shape.displayName);
        name.setTextColor(0xFFFFFFFF);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        name.setGravity(Gravity.CENTER);
        card.addView(name);
        
        // Corner radius info
        TextView info = new TextView(context);
        info.setText((int)shape.cornerRadius + "dp");
        info.setTextColor(0xFF666666);
        info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        info.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.topMargin = dp(4);
        card.addView(info, infoParams);
        
        card.setOnClickListener(v -> {
            selectedShape = key;
            updateContent(); // Refresh to show selection
        });
        
        return card;
    }
    
    private LinearLayout createGalleryContent() {
        LinearLayout gallery = new LinearLayout(context);
        gallery.setOrientation(LinearLayout.VERTICAL);
        gallery.setGravity(Gravity.CENTER);
        gallery.setPadding(dp(24), dp(32), dp(24), dp(32));
        
        // Icon
        TextView icon = new TextView(context);
        icon.setText("🖼️");
        icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 64);
        icon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        iconParams.bottomMargin = dp(24);
        gallery.addView(icon, iconParams);
        
        // Title
        TextView title = new TextView(context);
        title.setText("Galeriden Arka Plan");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.bottomMargin = dp(12);
        gallery.addView(title, titleParams);
        
        // Description
        TextView desc = new TextView(context);
        desc.setText("Kendi fotoğrafınızı klavye arka planı olarak kullanın");
        desc.setTextColor(0xFF666666);
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        desc.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.bottomMargin = dp(32);
        gallery.addView(desc, descParams);
        
        // Select photo button
        Button selectBtn = new Button(context);
        selectBtn.setText("Fotoğraf Seç");
        selectBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        selectBtn.setTextColor(0xFFFFFFFF);
        selectBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        selectBtn.setAllCaps(false);
        
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setCornerRadius(dp(12));
        btnBg.setColor(0xFF0A84FF);
        selectBtn.setBackground(btnBg);
        selectBtn.setPadding(dp(32), dp(14), dp(32), dp(14));
        
        selectBtn.setOnClickListener(v -> {
            if (callback != null) {
                callback.onGalleryPhotoSelected();
                dismiss();
            }
        });
        
        gallery.addView(selectBtn);
        
        return gallery;
    }
    
    private LinearLayout createThemeCategory(String title, List<Map.Entry<String, ThemeData>> themeList) {
        LinearLayout category = new LinearLayout(context);
        category.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        categoryParams.bottomMargin = dp(16);
        category.setLayoutParams(categoryParams);
        
        // Category title
        TextView categoryTitle = new TextView(context);
        categoryTitle.setText(title);
        categoryTitle.setTextColor(0xFF666666);
        categoryTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        categoryTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        categoryTitle.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(dp(4), 0, 0, dp(8));
        category.addView(categoryTitle, titleParams);
        
        // Theme grid (3 columns)
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout row = null;
        int count = 0;
        
        for (Map.Entry<String, ThemeData> entry : themeList) {
            if (count % 3 == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row);
            }
            
            row.addView(createThemeCard(entry.getKey(), entry.getValue()));
            count++;
        }
        
        category.addView(grid);
        return category;
    }
    
    private View createThemeCard(String key, ThemeData theme) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(8), dp(10), dp(8), dp(10));
        
        boolean isSelected = key.equals(selectedTheme);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(12));
        cardBg.setColor(0xFF1A1A1A);
        cardBg.setStroke(dp(2), isSelected ? 0xFF0A84FF : 0xFF2A2A2A);
        card.setBackground(cardBg);
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        cardParams.setMargins(dp(4), dp(4), dp(4), dp(4));
        card.setLayoutParams(cardParams);
        
        // Gradient preview
        View preview = new View(context);
        GradientDrawable previewBg = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            theme.colors
        );
        previewBg.setCornerRadius(dp(8));
        preview.setBackground(previewBg);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(50)
        );
        previewParams.bottomMargin = dp(8);
        card.addView(preview, previewParams);
        
        // Theme name
        TextView name = new TextView(context);
        name.setText(theme.icon + " " + theme.name);
        name.setTextColor(0xFFFFFFFF);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        name.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        name.setMaxLines(2);
        name.setGravity(Gravity.CENTER);
        card.addView(name);
        
        card.setOnClickListener(v -> {
            selectedTheme = key;
            updateContent(); // Refresh to show selection
        });
        
        return card;
    }
    
    private Button createApplyButton() {
        Button apply = new Button(context);
        apply.setText("Uygula");
        apply.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        apply.setTextColor(0xFFFFFFFF);
        apply.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        apply.setAllCaps(false);
        apply.setLetterSpacing(0.02f);
        
        GradientDrawable applyBg = new GradientDrawable();
        applyBg.setCornerRadius(dp(12));
        applyBg.setColor(0xFF0A84FF);
        apply.setBackground(applyBg);
        
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(56)
        );
        applyParams.setMargins(dp(16), dp(12), dp(16), dp(16));
        apply.setLayoutParams(applyParams);
        
        apply.setOnClickListener(v -> {
            prefs.edit()
                .putString("selected_theme", selectedTheme)
                .putString("selected_shape", selectedShape)
                .apply();
            
            if (callback != null) {
                callback.onThemeSelected(selectedTheme, selectedShape);
            }
            dismiss();
        });
        
        return apply;
    }
    
    // Helper methods to categorize themes
    private List<Map.Entry<String, ThemeData>> getDarkThemes() {
        List<Map.Entry<String, ThemeData>> list = new ArrayList<>();
        for (Map.Entry<String, ThemeData> entry : themes.entrySet()) {
            if (entry.getKey().contains("dark") || entry.getKey().contains("midnight") || 
                entry.getKey().contains("amoled") || entry.getKey().contains("slate") ||
                entry.getKey().contains("charcoal") || entry.getKey().contains("navy") ||
                entry.getKey().contains("forest") && !entry.getKey().contains("_green") ||
                entry.getKey().contains("wine") || entry.getKey().contains("carbon") ||
                entry.getKey().contains("obsidian")) {
                list.add(entry);
            }
        }
        return list;
    }
    
    private List<Map.Entry<String, ThemeData>> getLightThemes() {
        List<Map.Entry<String, ThemeData>> list = new ArrayList<>();
        for (Map.Entry<String, ThemeData> entry : themes.entrySet()) {
            if (entry.getKey().contains("light") || entry.getKey().contains("snow") ||
                entry.getKey().contains("cream") || entry.getKey().contains("pearl") ||
                entry.getKey().contains("cloud") || entry.getKey().contains("linen") ||
                entry.getKey().contains("mint") && !entry.getKey().contains("pastel") ||
                entry.getKey().contains("lavender") && !entry.getKey().contains("pastel")) {
                list.add(entry);
            }
        }
        return list;
    }
    
    private List<Map.Entry<String, ThemeData>> getGradientThemes() {
        List<Map.Entry<String, ThemeData>> list = new ArrayList<>();
        for (Map.Entry<String, ThemeData> entry : themes.entrySet()) {
            if (entry.getKey().contains("sunset") || entry.getKey().contains("ocean") ||
                entry.getKey().contains("aurora") || entry.getKey().contains("forest_green") ||
                entry.getKey().contains("rose") || entry.getKey().contains("cyber") ||
                entry.getKey().contains("fire") || entry.getKey().contains("ice") ||
                entry.getKey().contains("gold") || entry.getKey().contains("emerald") ||
                entry.getKey().contains("purple_haze") || entry.getKey().contains("neon")) {
                list.add(entry);
            }
        }
        return list;
    }
    
    private List<Map.Entry<String, ThemeData>> getMaterialThemes() {
        List<Map.Entry<String, ThemeData>> list = new ArrayList<>();
        for (Map.Entry<String, ThemeData> entry : themes.entrySet()) {
            if (entry.getKey().startsWith("material_")) {
                list.add(entry);
            }
        }
        return list;
    }
    
    private List<Map.Entry<String, ThemeData>> getPastelThemes() {
        List<Map.Entry<String, ThemeData>> list = new ArrayList<>();
        for (Map.Entry<String, ThemeData> entry : themes.entrySet()) {
            if (entry.getKey().startsWith("pastel_")) {
                list.add(entry);
            }
        }
        return list;
    }
    
    private List<Map.Entry<String, ThemeData>> getGamingThemes() {
        List<Map.Entry<String, ThemeData>> list = new ArrayList<>();
        for (Map.Entry<String, ThemeData> entry : themes.entrySet()) {
            if (entry.getKey().equals("rgb") || entry.getKey().equals("matrix") ||
                entry.getKey().equals("synthwave") || entry.getKey().equals("retro") ||
                entry.getKey().equals("gamer") || entry.getKey().equals("console")) {
                list.add(entry);
            }
        }
        return list;
    }
    
    /**
     * 🎨 CREATE STYLE SETTINGS CONTENT
     */
    private LinearLayout createStyleContent() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        
        // Section: Key Color Mode
        layout.addView(createSectionTitle("Tema Rengi Nerede Görünsün?"));
        layout.addView(createStyleOptionGroup(
            new String[]{
                ThemeStyleConfig.getKeyColorModeName(ThemeStyleConfig.KeyColorMode.FILL_ONLY),
                ThemeStyleConfig.getKeyColorModeName(ThemeStyleConfig.KeyColorMode.STROKE_ONLY),
                ThemeStyleConfig.getKeyColorModeName(ThemeStyleConfig.KeyColorMode.FILL_AND_STROKE),
                ThemeStyleConfig.getKeyColorModeName(ThemeStyleConfig.KeyColorMode.GRADIENT)
            },
            styleConfig.getKeyColorMode().ordinal(),
            (index) -> {
                styleConfig.setKeyColorMode(ThemeStyleConfig.KeyColorMode.values()[index]);
            }
        ));
        
        // Section: Text Color Mode
        layout.addView(createSectionTitle("Yazı Rengi"));
        layout.addView(createStyleOptionGroup(
            new String[]{
                ThemeStyleConfig.getTextColorModeName(ThemeStyleConfig.TextColorMode.AUTO),
                ThemeStyleConfig.getTextColorModeName(ThemeStyleConfig.TextColorMode.ALWAYS_WHITE),
                ThemeStyleConfig.getTextColorModeName(ThemeStyleConfig.TextColorMode.ALWAYS_BLACK),
                ThemeStyleConfig.getTextColorModeName(ThemeStyleConfig.TextColorMode.THEME_ACCENT)
            },
            styleConfig.getTextColorMode().ordinal(),
            (index) -> {
                styleConfig.setTextColorMode(ThemeStyleConfig.TextColorMode.values()[index]);
            }
        ));
        
        // Section: Key Background Mode
        layout.addView(createSectionTitle("Buton Arka Plan Stili"));
        layout.addView(createStyleOptionGroup(
            new String[]{
                ThemeStyleConfig.getKeyBackgroundModeName(ThemeStyleConfig.KeyBackgroundMode.SOLID),
                ThemeStyleConfig.getKeyBackgroundModeName(ThemeStyleConfig.KeyBackgroundMode.GRADIENT),
                ThemeStyleConfig.getKeyBackgroundModeName(ThemeStyleConfig.KeyBackgroundMode.TRANSPARENT),
                ThemeStyleConfig.getKeyBackgroundModeName(ThemeStyleConfig.KeyBackgroundMode.SEMI_TRANSPARENT)
            },
            styleConfig.getKeyBackgroundMode().ordinal(),
            (index) -> {
                styleConfig.setKeyBackgroundMode(ThemeStyleConfig.KeyBackgroundMode.values()[index]);
            }
        ));
        
        // Section: Stroke Width Slider
        layout.addView(createSectionTitle("Kenar Kalınlığı: " + styleConfig.getStrokeWidth() + "dp"));
        layout.addView(createSlider(styleConfig.getStrokeWidth(), 0, 6, (value) -> {
            styleConfig.setStrokeWidth(value);
        }));
        
        // Section: Key Opacity Slider
        layout.addView(createSectionTitle("Buton Saydamlığı: %" + styleConfig.getKeyOpacity()));
        layout.addView(createSlider(styleConfig.getKeyOpacity(), 50, 100, (value) -> {
            styleConfig.setKeyOpacity(value);
        }));
        
        return layout;
    }
    
    private TextView createSectionTitle(String title) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(20);
        params.bottomMargin = dp(8);
        tv.setLayoutParams(params);
        
        return tv;
    }
    
    private LinearLayout createStyleOptionGroup(String[] options, int selected, StyleOptionCallback callback) {
        LinearLayout group = new LinearLayout(context);
        group.setOrientation(LinearLayout.VERTICAL);
        
        for (int i = 0; i < options.length; i++) {
            final int index = i;
            Button button = createStyleOptionButton(options[i], i == selected);
            button.setOnClickListener(v -> {
                // Update all buttons
                for (int j = 0; j < group.getChildCount(); j++) {
                    Button btn = (Button) group.getChildAt(j);
                    updateStyleButton(btn, j == index);
                }
                callback.onSelected(index);
            });
            group.addView(button);
        }
        
        return group;
    }
    
    private Button createStyleOptionButton(String text, boolean selected) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        
        updateStyleButton(button, selected);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(48)
        );
        params.topMargin = dp(6);
        button.setLayoutParams(params);
        
        return button;
    }
    
    private void updateStyleButton(Button button, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        
        if (selected) {
            bg.setColor(0xFF0A84FF);
            button.setTextColor(0xFFFFFFFF);
        } else {
            bg.setColor(0xFF2A2A2A);
            button.setTextColor(0xFF999999);
        }
        
        button.setBackground(bg);
    }
    
    private LinearLayout createSlider(int currentValue, int min, int max, SliderCallback callback) {
        LinearLayout sliderLayout = new LinearLayout(context);
        sliderLayout.setOrientation(LinearLayout.HORIZONTAL);
        sliderLayout.setPadding(dp(8), dp(8), dp(8), dp(8));
        sliderLayout.setGravity(Gravity.CENTER_VERTICAL);
        
        // Minus button
        Button minusBtn = createSliderButton("-");
        minusBtn.setOnClickListener(v -> {
            int newValue = Math.max(min, currentValue - 1);
            callback.onValueChanged(newValue);
            updateContent(); // Refresh
        });
        sliderLayout.addView(minusBtn);
        
        // Value display
        TextView valueText = new TextView(context);
        valueText.setText(String.valueOf(currentValue));
        valueText.setTextColor(0xFFFFFFFF);
        valueText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        valueText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        valueText.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        sliderLayout.addView(valueText, valueParams);
        
        // Plus button
        Button plusBtn = createSliderButton("+");
        plusBtn.setOnClickListener(v -> {
            int newValue = Math.min(max, currentValue + 1);
            callback.onValueChanged(newValue);
            updateContent(); // Refresh
        });
        sliderLayout.addView(plusBtn);
        
        return sliderLayout;
    }
    
    private Button createSliderButton(String text) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        button.setTextColor(0xFFFFFFFF);
        button.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        bg.setColor(0xFF0A84FF);
        button.setBackground(bg);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(60), dp(60));
        params.setMargins(dp(4), 0, dp(4), 0);
        button.setLayoutParams(params);
        
        return button;
    }
    
    interface StyleOptionCallback {
        void onSelected(int index);
    }
    
    interface SliderCallback {
        void onValueChanged(int value);
    }
    
    private int dp(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}

