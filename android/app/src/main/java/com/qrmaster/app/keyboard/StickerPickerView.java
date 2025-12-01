package com.qrmaster.app.keyboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StickerPickerView extends LinearLayout {
    private static final String TAG = "StickerPickerView";

    private final StickerCallback callback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView statusText;
    private StickerAdapter adapter;

    private final List<String> stickerUrls = Arrays.asList(
        "https://i.imgur.com/8pQ9N3E.png",
        "https://i.imgur.com/9LYbF1D.png",
        "https://i.imgur.com/4skxUTM.png",
        "https://i.imgur.com/u8VwJYX.png",
        "https://i.imgur.com/ZQz7QwF.png",
        "https://i.imgur.com/t5Q1aMM.png",
        "https://i.imgur.com/HXyssNa.png",
        "https://i.imgur.com/cCj1oOW.png",
        "https://i.imgur.com/L0sdWqq.png",
        "https://i.imgur.com/b7gHlos.png",
        "https://i.imgur.com/fcbyf92.png",
        "https://i.imgur.com/0xq11W8.png"
    );

    public interface StickerCallback {
        void onStickerSelected(String stickerUrl);
        void onClose();
    }

    public StickerPickerView(Context context, StickerCallback callback) {
        super(context);
        this.callback = callback;
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            dpToPx(300)
        ));
        setBackgroundColor(0xFF1C1C1E);
        setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        TextView title = new TextView(context);
        title.setText("🎨 Çıkartmalar");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, dpToPx(8));
        addView(title);

        progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        LayoutParams pbParams = new LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        );
        pbParams.gravity = Gravity.CENTER_HORIZONTAL;
        pbParams.bottomMargin = dpToPx(6);
        progressBar.setLayoutParams(pbParams);
        addView(progressBar);
        progressBar.setVisibility(GONE);

        statusText = new TextView(context);
        statusText.setTextColor(0xFF8E8E93);
        statusText.setTextSize(12);
        statusText.setGravity(Gravity.CENTER);
        LayoutParams statusParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        statusText.setLayoutParams(statusParams);
        addView(statusText);

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 4));
        recyclerView.addItemDecoration(new SpacesItemDecoration(dpToPx(6)));

        adapter = new StickerAdapter();
        recyclerView.setAdapter(adapter);

        LayoutParams recyclerParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            0,
            1f
        );
        recyclerView.setLayoutParams(recyclerParams);
        addView(recyclerView);

        LinearLayout buttonLayout = new LinearLayout(context);
        buttonLayout.setOrientation(HORIZONTAL);
        LayoutParams buttonParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        buttonParams.topMargin = dpToPx(8);
        buttonLayout.setLayoutParams(buttonParams);

        Button closeBtn = new Button(context);
        closeBtn.setText("Kapat");
        closeBtn.setTextColor(0xFFFFFFFF);
        closeBtn.setBackgroundColor(0xFF007AFF);
        LayoutParams closeParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            dpToPx(44)
        );
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> {
            if (callback != null) callback.onClose();
        });
        buttonLayout.addView(closeBtn);
        addView(buttonLayout);

        loadStickers();
    }

    private void loadStickers() {
        progressBar.setVisibility(VISIBLE);
        statusText.setText("Çıkartmalar yükleniyor...");

        mainHandler.postDelayed(() -> {
            progressBar.setVisibility(GONE);
            statusText.setText("");
            adapter.setData(stickerUrls);
        }, 300); // kısa bir gecikme animasyon için
    }

    private void copyToClipboard(String url) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Sticker", url);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Çıkartma linki panoya kopyalandı", Toast.LENGTH_SHORT).show();
        }
    }

    private class StickerAdapter extends RecyclerView.Adapter<StickerViewHolder> {
        private final List<String> items = new ArrayList<>();

        void setData(List<String> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public StickerViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.widget.ImageView imageView = new android.widget.ImageView(parent.getContext());
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                dpToPx(80)
            );
            params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            imageView.setLayoutParams(params);
            imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundColor(0xFF2C2C2E);
            imageView.setClipToOutline(true);
            return new StickerViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(StickerViewHolder holder, int position) {
            String url = items.get(position);
            Glide.with(holder.imageView.getContext())
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imageView);

            holder.imageView.setOnClickListener(v -> {
                if (callback != null) {
                    copyToClipboard(url);
                    callback.onStickerSelected(url);
                    Toast.makeText(getContext(), "Çıkartma linki yapıştırıldı", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class StickerViewHolder extends RecyclerView.ViewHolder {
        android.widget.ImageView imageView;

        StickerViewHolder(android.widget.ImageView view) {
            super(view);
            this.imageView = view;
        }
    }

    private static class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(android.graphics.Rect outRect, android.view.View view,
                                   RecyclerView parent, RecyclerView.State state) {
            outRect.left = space;
            outRect.right = space;
            outRect.top = space;
            outRect.bottom = space;
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }
}

