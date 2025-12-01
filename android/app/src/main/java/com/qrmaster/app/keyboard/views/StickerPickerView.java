package com.qrmaster.app.keyboard.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.qrmaster.app.keyboard.StickerManager;
import com.qrmaster.app.keyboard.StickerManager.StickerPack;
import java.util.List;

public class StickerPickerView extends LinearLayout {
    
    public interface Callback {
        void onStickerSelected(String sticker);
        void onClose();
    }
    
    private final Callback callback;
    private GridLayout stickerGrid;
    private String currentPackId = "happy";
    
    public StickerPickerView(Context context, Callback callback) {
        super(context);
        this.callback = callback;
        init(context);
    }
    
    private void init(Context context) {
        StickerManager.init(context);
        
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp(280)));
        setBackgroundColor(0xFF000000);
        setPadding(0, 0, 0, 0);
        
        // Header
        LinearLayout header = createHeader(context);
        addView(header);
        
        // Category tabs
        HorizontalScrollView categoryScroll = new HorizontalScrollView(context);
        categoryScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout categoryBar = createCategoryBar(context);
        categoryScroll.addView(categoryBar);
        addView(categoryScroll);
        
        // Sticker grid
        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);
        stickerGrid = new GridLayout(context);
        stickerGrid.setColumnCount(6);
        stickerGrid.setPadding(dp(8), dp(8), dp(8), dp(8));
        scrollView.addView(stickerGrid);
        
        LinearLayout.LayoutParams scrollParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        addView(scrollView, scrollParams);
        
        // Load first pack
        loadPack(currentPackId);
    }
    
    private LinearLayout createHeader(Context context) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(8), dp(12), dp(8));
        header.setBackgroundColor(0xFF1C1C1E);
        
        TextView title = new TextView(context);
        title.setText("🎨 Sticker Seç");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);
        
        Button closeBtn = new Button(context);
        closeBtn.setText("✕");
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
        closeBtn.setMinWidth(0);
        closeBtn.setMinHeight(0);
        
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setColor(0xFFFF3B30);
        closeBg.setCornerRadius(dp(8));
        closeBtn.setBackground(closeBg);
        closeBtn.setOnClickListener(v -> callback.onClose());
        header.addView(closeBtn);
        
        return header;
    }
    
    private LinearLayout createCategoryBar(Context context) {
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(HORIZONTAL);
        bar.setPadding(dp(8), dp(8), dp(8), dp(8));
        bar.setBackgroundColor(0xFF2C2C2E);
        
        List<StickerPack> packs = StickerManager.getAllPacks();
        for (StickerPack pack : packs) {
            Button catBtn = createCategoryButton(context, pack);
            LinearLayout.LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.rightMargin = dp(8);
            bar.addView(catBtn, params);
        }
        
        return bar;
    }
    
    private Button createCategoryButton(Context context, StickerPack pack) {
        Button btn = new Button(context);
        btn.setText(pack.icon + "\n" + pack.name);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setTextColor(Color.WHITE);
        btn.setPadding(dp(16), dp(8), dp(16), dp(8));
        btn.setMinWidth(0);
        btn.setMinHeight(0);
        btn.setGravity(Gravity.CENTER);
        
        GradientDrawable bg = new GradientDrawable();
        if (pack.id.equals(currentPackId)) {
            bg.setColor(0xFF007AFF);
        } else {
            bg.setColor(0xFF3A3A3C);
        }
        bg.setCornerRadius(dp(12));
        btn.setBackground(bg);
        
        btn.setOnClickListener(v -> {
            currentPackId = pack.id;
            loadPack(pack.id);
            // Refresh category bar
            ViewGroup parent = (ViewGroup) getChildAt(1); // HorizontalScrollView
            LinearLayout categoryBar = (LinearLayout) ((HorizontalScrollView) parent).getChildAt(0);
            categoryBar.removeAllViews();
            
            List<StickerPack> packs = StickerManager.getAllPacks();
            for (StickerPack p : packs) {
                Button catBtn = createCategoryButton(context, p);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                params.rightMargin = dp(8);
                categoryBar.addView(catBtn, params);
            }
        });
        
        return btn;
    }
    
    private void loadPack(String packId) {
        stickerGrid.removeAllViews();
        
        StickerPack pack = StickerManager.getPack(packId);
        if (pack == null) return;
        
        for (String sticker : pack.stickers) {
            Button stickerBtn = new Button(getContext());
            stickerBtn.setText(sticker);
            stickerBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
            stickerBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
            stickerBtn.setMinWidth(0);
            stickerBtn.setMinHeight(0);
            
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFF2C2C2E);
            bg.setCornerRadius(dp(8));
            stickerBtn.setBackground(bg);
            
            stickerBtn.setOnClickListener(v -> callback.onStickerSelected(sticker));
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dp(56);
            params.height = dp(56);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            stickerGrid.addView(stickerBtn, params);
        }
    }
    
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}








