package com.qrmaster.app.keyboard.miniapps;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qrmaster.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Mini Uygulamalar Hub - Premium özellikler
 * - Hesap Makinesi
 * - Döviz Çevirici
 * - Takvim
 * - OCR (Görselden Metin)
 * - Harita (OSM)
 * - Ortak Yazı Alanı
 */
public class MiniAppsHubView extends LinearLayout {
    private static final String TAG = "MiniAppsHubView";

    public enum MiniAppType {
        CALCULATOR, CURRENCY, CALENDAR, OCR, MAP, SHARED_TYPING
    }

    public interface MiniAppsCallback {
        void onMiniAppSelected(MiniAppType type);
        void onClose();
    }

    private final MiniAppsCallback callback;

    public MiniAppsHubView(Context context, MiniAppsCallback callback) {
        super(context);
        this.callback = callback;
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        ));
        // Premium gradient background
        android.graphics.drawable.GradientDrawable bgGradient = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF1C1C1E, 0xFF0A0A0A}
        );
        setBackground(bgGradient);
        setPadding(dp(16), dp(16), dp(16), dp(16));

        // Header with premium styling
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(4), dp(8), dp(4), dp(16));
        
        TextView title = new TextView(context);
        title.setText("✨ Mini Uygulamalar");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setShadowLayer(4, 0, 2, 0x80000000); // Text gölge
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);

        ImageButton closeBtn = new ImageButton(context);
        closeBtn.setBackground(ContextCompat.getDrawable(context, R.drawable.toolbar_button_bg));
        closeBtn.setImageResource(R.drawable.ic_close);
        closeBtn.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        closeBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> {
            if (callback != null) callback.onClose();
        });
        header.addView(closeBtn);
        addView(header);

        // Grid of mini apps
        RecyclerView gridView = new RecyclerView(context);
        gridView.setLayoutManager(new GridLayoutManager(context, 2));
        gridView.setAdapter(new MiniAppsAdapter(createMiniAppsList(), app -> {
            if (callback != null) callback.onMiniAppSelected(app.type);
        }));
        LayoutParams gridParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            0,
            1f
        );
        addView(gridView, gridParams);
    }

    private List<MiniAppItem> createMiniAppsList() {
        List<MiniAppItem> apps = new ArrayList<>();
        apps.add(new MiniAppItem(R.drawable.ic_calculator, "Hesap\nMakinesi", MiniAppType.CALCULATOR));
        apps.add(new MiniAppItem(R.drawable.ic_currency, "Döviz\nÇevirici", MiniAppType.CURRENCY));
        apps.add(new MiniAppItem(R.drawable.ic_calendar_mini, "Takvim", MiniAppType.CALENDAR));
        apps.add(new MiniAppItem(R.drawable.ic_ocr, "OCR\nMetin Çek", MiniAppType.OCR));
        apps.add(new MiniAppItem(R.drawable.ic_map_mini, "Harita", MiniAppType.MAP));
        apps.add(new MiniAppItem(R.drawable.ic_shared_typing, "Ortak\nYazı", MiniAppType.SHARED_TYPING));
        return apps;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    static class MiniAppItem {
        final int iconRes;
        final String name;
        final MiniAppType type;

        MiniAppItem(int iconRes, String name, MiniAppType type) {
            this.iconRes = iconRes;
            this.name = name;
            this.type = type;
        }
    }

    static class MiniAppsAdapter extends RecyclerView.Adapter<MiniAppsAdapter.ViewHolder> {
        private final List<MiniAppItem> items;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(MiniAppItem item);
        }

        MiniAppsAdapter(List<MiniAppItem> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(parent.getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(parent.getContext(), 20), dp(parent.getContext(), 24), 
                           dp(parent.getContext(), 20), dp(parent.getContext(), 24));
            
            // Premium gradient card
            android.graphics.drawable.GradientDrawable cardGradient = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF2C2C2E, 0xFF1A1A1C}
            );
            cardGradient.setCornerRadius(dp(parent.getContext(), 20));
            cardGradient.setStroke(dp(parent.getContext(), 1), 0x30FFFFFF); // Subtle border
            card.setBackground(cardGradient);
            card.setElevation(dp(parent.getContext(), 4)); // Shadow

            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                dp(parent.getContext(), 145) // Increased for better text visibility
            );
            params.setMargins(dp(parent.getContext(), 8), dp(parent.getContext(), 8), 
                             dp(parent.getContext(), 8), dp(parent.getContext(), 8));
            card.setLayoutParams(params);

            return new ViewHolder(card);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            MiniAppItem item = items.get(position);
            
            LinearLayout card = (LinearLayout) holder.itemView;
            card.removeAllViews();
            
            // SVG Icon (ImageView)
            ImageView icon = new ImageView(card.getContext());
            icon.setImageResource(item.iconRes);
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dp(card.getContext(), 56),
                dp(card.getContext(), 56)
            );
            iconParams.gravity = Gravity.CENTER_HORIZONTAL;
            icon.setLayoutParams(iconParams);
            icon.setElevation(dp(card.getContext(), 2)); // Icon elevation
            card.addView(icon);

            // Name with proper wrapping
            TextView name = new TextView(card.getContext());
            name.setText(item.name);
            name.setTextColor(0xFFFFFFFF);
            name.setTextSize(12);
            name.setGravity(Gravity.CENTER);
            name.setTypeface(null, Typeface.BOLD);
            name.setShadowLayer(2, 0, 1, 0x80000000);
            name.setMaxLines(2);
            name.setLineSpacing(dp(card.getContext(), 2), 1.0f);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            nameParams.topMargin = dp(card.getContext(), 8);
            name.setLayoutParams(nameParams);
            card.addView(name);

            // Premium click effect
            card.setOnClickListener(v -> {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                }).start();
                listener.onItemClick(item);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private static int dp(Context context, int value) {
            return (int) (value * context.getResources().getDisplayMetrics().density);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(LinearLayout itemView) {
                super(itemView);
            }
        }
    }
}

