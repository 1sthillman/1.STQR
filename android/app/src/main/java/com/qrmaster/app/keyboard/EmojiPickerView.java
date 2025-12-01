package com.qrmaster.app.keyboard;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qrmaster.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Tam emoji picker - tüm emoji kategorileri
 */
public class EmojiPickerView extends LinearLayout {
    
    private RecyclerView emojiRecyclerView;
    private LinearLayout categoryBar;
    private EmojiCallback callback;
    private String currentCategory = "😊";
    
    public interface EmojiCallback {
        void onEmojiSelected(String emoji);
        void onClose();
    }
    
    public EmojiPickerView(Context context, EmojiCallback callback) {
        super(context);
        this.callback = callback;
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            dpToPx(280) // Sabit yükseklik - kompakt
        ));
        setBackgroundColor(0xFF1C1C1E); // Daha koyu modern arka plan

        // Kompakt header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));
        header.setBackgroundColor(0xFF2C2C2E);
        LayoutParams headerParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        TextView title = new TextView(context);
        title.setText("Emoji");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(14);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);

        android.widget.ImageButton closeBtn = new android.widget.ImageButton(context);
        android.graphics.drawable.GradientDrawable closeBg = new android.graphics.drawable.GradientDrawable();
        closeBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        closeBg.setColor(0xFF48484A);
        closeBtn.setBackground(closeBg);
        closeBtn.setImageResource(R.drawable.ic_close);
        closeBtn.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        closeBtn.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        closeBtn.setColorFilter(0xFFFFFFFF);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dpToPx(28), dpToPx(28));
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> { if (callback != null) callback.onClose(); });
        header.addView(closeBtn);
        addView(header, headerParams);

        // Kompakt kategori bar - Horizontal scroll
        android.widget.HorizontalScrollView categoryScroll = new android.widget.HorizontalScrollView(context);
        categoryScroll.setHorizontalScrollBarEnabled(false);
        categoryScroll.setBackgroundColor(0xFF2C2C2E);
        
        categoryBar = new LinearLayout(context);
        categoryBar.setOrientation(HORIZONTAL);
        categoryBar.setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4));
        
        String[] categories = EmojiManager.getAllCategoryIcons();
        for (String category : categories) {
            Button btn = new Button(context);
            btn.setText(category);
            btn.setTextSize(20);
            btn.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
            
            // Modern kompakt buton
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setColor(0x00000000); // Transparan
            shape.setCornerRadius(dpToPx(10));
            btn.setBackground(shape);
            
            btn.setOnClickListener(v -> {
                loadCategory(category);
                // Seçili kategoriyi vurgula
                for (int i = 0; i < categoryBar.getChildCount(); i++) {
                    Button b = (Button) categoryBar.getChildAt(i);
                    android.graphics.drawable.GradientDrawable s = new android.graphics.drawable.GradientDrawable();
                    s.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    s.setColor(b == btn ? 0xFF3A3A3C : 0x00000000);
                    s.setCornerRadius(dpToPx(10));
                    b.setBackground(s);
                }
            });
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(44),
                dpToPx(38)
            );
            params.setMargins(0, 0, dpToPx(4), 0);
            btn.setLayoutParams(params);
            categoryBar.addView(btn);
        }
        categoryScroll.addView(categoryBar);
        
        LayoutParams scrollParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        addView(categoryScroll, scrollParams);
        
        // Emoji grid - kompakt
        emojiRecyclerView = new RecyclerView(context);
        emojiRecyclerView.setLayoutManager(new GridLayoutManager(context, 8));
        emojiRecyclerView.setBackgroundColor(0xFF1C1C1E);
        emojiRecyclerView.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        emojiRecyclerView.setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            0,
            1.0f
        ));
        addView(emojiRecyclerView);
        
        // İlk kategoriyi yükle
        loadCategory(currentCategory);
    }
    
    private void loadCategory(String category) {
        currentCategory = category;
        EmojiManager.Category cat = EmojiManager.getCategoryByIcon(category);
        if (cat != null) {
            List<String> emojis = EmojiManager.getEmojis(cat);
            EmojiAdapter adapter = new EmojiAdapter(emojis, emoji -> {
                if (callback != null) callback.onEmojiSelected(emoji);
            });
            emojiRecyclerView.setAdapter(adapter);
        }
    }
    
    // Emoji Adapter
    private static class EmojiAdapter extends RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder> {
        
        private final List<String> emojis;
        private final OnEmojiClickListener listener;
        
        interface OnEmojiClickListener {
            void onClick(String emoji);
        }
        
        EmojiAdapter(List<String> emojis, OnEmojiClickListener listener) {
            this.emojis = emojis;
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public EmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setTextSize(24); // Daha kompakt boyut
            textView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            textView.setPadding(6, 6, 6, 6);
            textView.setLayoutParams(new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ));
            return new EmojiViewHolder(textView);
        }
        
        @Override
        public void onBindViewHolder(@NonNull EmojiViewHolder holder, int position) {
            String emoji = emojis.get(position);
            holder.textView.setText(emoji);
            holder.textView.setOnClickListener(v -> listener.onClick(emoji));
        }
        
        @Override
        public int getItemCount() {
            return emojis.size();
        }
        
        static class EmojiViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            
            EmojiViewHolder(TextView textView) {
                super(textView);
                this.textView = textView;
            }
        }
    }
    
    private int dpToPx(int dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }
}

