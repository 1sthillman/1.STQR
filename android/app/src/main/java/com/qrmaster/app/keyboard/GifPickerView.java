package com.qrmaster.app.keyboard;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import androidx.core.content.ContextCompat;

import com.qrmaster.app.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GifPickerView extends LinearLayout {
    private static final String TAG = "GifPickerView";
    private static final String TENOR_API_KEY = "LIVDSRZULELA";
    private static final String TENOR_BASE_URL = "https://g.tenor.com/v1/";

    private final GifCallback callback;
    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private android.widget.EditText queryEditText;
    private ProgressBar progressBar;
    private TextView statusText;
    private RecyclerView recyclerView;
    private GifAdapter adapter;
    private Call runningCall;
    private String currentQuery = "";

    public interface GifCallback {
        void onGifSelected(String gifUrl, String tinyGifUrl);
        void onClose();
    }

    public GifPickerView(Context context, GifCallback callback) {
        super(context);
        this.callback = callback;
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            dpToPx(240) // Daha kompakt
        ));
        setBackgroundColor(0xFF1C1C1E);
        setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

        // Mini header - sadece X
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        header.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
        LayoutParams headerParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        headerParams.bottomMargin = dpToPx(2);

        ImageButton closeBtn = new ImageButton(context);
        android.graphics.drawable.GradientDrawable closeBg = new android.graphics.drawable.GradientDrawable();
        closeBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        closeBg.setColor(0xFF48484A);
        closeBtn.setBackground(closeBg);
        closeBtn.setImageResource(R.drawable.ic_close);
        closeBtn.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        closeBtn.setPadding(dpToPx(5), dpToPx(5), dpToPx(5), dpToPx(5));
        closeBtn.setColorFilter(0xFFFFFFFF);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24));
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> {
            cancelRunningCall();
            if (callback != null) callback.onClose();
        });
        header.addView(closeBtn);
        addView(header, headerParams);

        // Modern arama - kompakt
        LinearLayout searchContainer = new LinearLayout(context);
        searchContainer.setOrientation(HORIZONTAL);
        searchContainer.setGravity(Gravity.CENTER_VERTICAL);
        searchContainer.setPadding(dpToPx(4), 0, dpToPx(4), 0);
        
        // EditText görünür ve modern
        queryEditText = new android.widget.EditText(context);
        queryEditText.setTextColor(0xFFFFFFFF); // Beyaz yazı
        queryEditText.setHintTextColor(0xFF8E8E93);
        queryEditText.setTextSize(12);
        queryEditText.setHint("GIF ara...");
        queryEditText.setSingleLine(true);
        queryEditText.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        android.graphics.drawable.GradientDrawable editBg = new android.graphics.drawable.GradientDrawable();
        editBg.setCornerRadius(dpToPx(8));
        editBg.setColor(0xFF2C2C2E); // Koyu gri arkaplan
        editBg.setStroke(dpToPx(1), 0xFF48484A); // İnce çerçeve
        queryEditText.setBackground(editBg);
        queryEditText.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        queryEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                submitSearch();
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
            0,
            dpToPx(32),
            1f
        );
        editParams.setMargins(0, 0, dpToPx(4), 0);
        queryEditText.setLayoutParams(editParams);
        searchContainer.addView(queryEditText);
        
        // Ara butonu - gri ton
        Button searchButton = new Button(context);
        searchButton.setText("Ara");
        searchButton.setTextSize(11);
        searchButton.setTextColor(0xFFFFFFFF);
        searchButton.setTypeface(null, android.graphics.Typeface.BOLD);
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setCornerRadius(dpToPx(8));
        btnBg.setColor(0xFF48484A); // Gri - renkli değil
        searchButton.setBackground(btnBg);
        searchButton.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        searchButton.setOnClickListener(v -> submitSearch());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT,
            dpToPx(32)
        );
        searchContainer.addView(searchButton, btnParams);
        
        LayoutParams searchParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        searchParams.bottomMargin = dpToPx(4);
        searchContainer.setLayoutParams(searchParams);
        addView(searchContainer);

        // Mini kategori butonları - emoji yok, sadece metin
        android.widget.HorizontalScrollView categoryScroll = new android.widget.HorizontalScrollView(context);
        categoryScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout categories = new LinearLayout(context);
        categories.setOrientation(HORIZONTAL);
        categories.setPadding(dpToPx(4), 0, dpToPx(4), dpToPx(2));
        
        String[] catNames = {"Trend", "Komik", "Sevgi", "Kutlama", "Üzgün", "Dans"};
        for (String name : catNames) {
            Button catBtn = new Button(context);
            catBtn.setText(name);
            catBtn.setTextSize(11);
            catBtn.setTextColor(0xFFFFFFFF);
            catBtn.setTypeface(null, android.graphics.Typeface.NORMAL);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dpToPx(8));
            bg.setColor(0xFF3A3A3C);
            catBtn.setBackground(bg);
            catBtn.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
            catBtn.setOnClickListener(v -> {
                setQuery(name);
                submitSearch();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                dpToPx(28)
            );
            params.setMargins(0, 0, dpToPx(4), 0);
            catBtn.setLayoutParams(params);
            categories.addView(catBtn);
        }
        
        categoryScroll.addView(categories);
        LayoutParams catScrollParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        catScrollParams.bottomMargin = dpToPx(4);
        categoryScroll.setLayoutParams(catScrollParams);
        addView(categoryScroll);

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 3)); // 3 sütun daha kompakt
        recyclerView.addItemDecoration(new SpacesItemDecoration(dpToPx(4)));
        recyclerView.setBackgroundColor(0xFF1C1C1E);
        adapter = new GifAdapter();
        recyclerView.setAdapter(adapter);
        LayoutParams rvParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            0,
            1f
        );
        recyclerView.setLayoutParams(rvParams);
        addView(recyclerView);

        progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        LayoutParams pbParams = new LayoutParams(
            dpToPx(24),
            dpToPx(24)
        );
        pbParams.gravity = Gravity.CENTER_HORIZONTAL;
        pbParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
        progressBar.setLayoutParams(pbParams);
        progressBar.setVisibility(GONE);
        addView(progressBar);

        statusText = new TextView(context);
        statusText.setTextColor(0xFF8E8E93);
        statusText.setTextSize(10);
        statusText.setGravity(Gravity.CENTER);
        LayoutParams statusParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        statusText.setLayoutParams(statusParams);
        addView(statusText);

        loadTrending();
    }

    private Button createActionButton(Context context, String text) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(0xFFFFFFFF);
        btn.setBackgroundColor(0xFF3A3A3C);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        LayoutParams params = new LayoutParams(0, dpToPx(42), 1f);
        params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        btn.setLayoutParams(params);
        return btn;
    }
    
    private Button createCategoryButton(Context context, String text) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(12);
        btn.setBackgroundColor(0xFF3A3A3C);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dpToPx(16));
        bg.setColor(0xFF3A3A3C);
        btn.setBackground(bg);
        btn.setPadding(dpToPx(14), dpToPx(6), dpToPx(14), dpToPx(6));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT,
            dpToPx(32)
        );
        params.setMargins(0, 0, dpToPx(8), 0);
        btn.setLayoutParams(params);
        return btn;
    }

    public void setQuery(String query) {
        currentQuery = query;
        if (queryEditText != null) {
            queryEditText.setText(query);
        }
    }

    public String getCurrentQuery() {
        if (queryEditText != null && queryEditText.getText() != null) {
            return queryEditText.getText().toString();
        }
        return currentQuery;
    }

    public void submitSearch() {
        String query = getCurrentQuery();
        if (query == null || query.trim().isEmpty()) {
            loadTrending();
        } else {
            fetchGifs("search", query.trim());
        }
    }

    public void loadTrending() {
        fetchGifs("trending", "");
    }

    private void fetchGifs(String type, String query) {
        cancelRunningCall();
        progressBar.setVisibility(VISIBLE);
        updateStatus("Yükleniyor...");

        String url;
        if ("search".equals(type)) {
            url = TENOR_BASE_URL + "search?key=" + TENOR_API_KEY + "&q=" + query + "&limit=24&media_filter=minimal";
        } else {
            url = TENOR_BASE_URL + "trending?key=" + TENOR_API_KEY + "&limit=24&media_filter=minimal";
        }

        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        runningCall = client.newCall(request);
        runningCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(GONE);
                    updateStatus("GIF'ler yüklenemedi: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    onFailure(call, new IOException("Unexpected code " + response));
                    return;
                }
                String body = response.body().string();
                List<GifItem> gifs = parseGifResponse(body);
                mainHandler.post(() -> {
                    progressBar.setVisibility(GONE);
                    if (gifs.isEmpty()) {
                        updateStatus("Sonuç bulunamadı");
                    } else {
                        updateStatus("");
                    }
                    adapter.setData(gifs);
                });
            }
        });
    }

    private List<GifItem> parseGifResponse(String json) {
        List<GifItem> list = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(json);
            JSONArray results = root.optJSONArray("results");
            if (results == null) return list;
            for (int i = 0; i < results.length(); i++) {
                JSONObject obj = results.getJSONObject(i);
                JSONArray media = obj.optJSONArray("media");
                if (media == null || media.length() == 0) continue;
                JSONObject formats = media.getJSONObject(0);
                JSONObject gif = formats.optJSONObject("gif");
                JSONObject tiny = formats.optJSONObject("tinygif");
                if (gif == null) continue;
                String url = gif.optString("url");
                String preview = tiny != null ? tiny.optString("url") : url;
                if (!url.isEmpty()) {
                    list.add(new GifItem(url, preview));
                }
            }
        } catch (JSONException e) {
            updateStatus("JSON parse hatası");
        }
        return list;
    }

    private void cancelRunningCall() {
        if (runningCall != null && !runningCall.isCanceled()) {
            runningCall.cancel();
        }
    }

    public void release() {
        cancelRunningCall();
    }

    private void updateStatus(String text) {
        statusText.setText(text);
    }

    public void setStatusText(String text) {
        updateStatus(text);
    }

    private void copyToClipboard(String url) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            android.content.ClipData clip = android.content.ClipData.newPlainText("GIF", url);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "GIF linki panoya kopyalandı", Toast.LENGTH_SHORT).show();
        }
    }

    private class GifAdapter extends RecyclerView.Adapter<GifViewHolder> {
        private final List<GifItem> items = new ArrayList<>();

        void setData(List<GifItem> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public GifViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.widget.ImageView imageView = new android.widget.ImageView(parent.getContext());
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                dpToPx(75) // Daha kompakt
            );
            imageView.setLayoutParams(params);
            imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundColor(0xFF2C2C2E);
            
            // Köşeleri yuvarla
            android.graphics.drawable.GradientDrawable roundBg = new android.graphics.drawable.GradientDrawable();
            roundBg.setCornerRadius(dpToPx(8));
            roundBg.setColor(0xFF2C2C2E);
            imageView.setBackground(roundBg);
            imageView.setClipToOutline(true);
            return new GifViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(GifViewHolder holder, int position) {
            GifItem item = items.get(position);
            Glide.with(holder.imageView.getContext())
                .asGif()
                .load(item.previewUrl)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(holder.imageView);

            holder.imageView.setOnClickListener(v -> {
                if (callback != null) {
                    callback.onGifSelected(item.url, item.previewUrl);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class GifItem {
        final String url;
        final String previewUrl;

        GifItem(String url, String previewUrl) {
            this.url = url;
            this.previewUrl = previewUrl;
        }
    }

    private static class GifViewHolder extends RecyclerView.ViewHolder {
        android.widget.ImageView imageView;

        GifViewHolder(android.widget.ImageView view) {
            super(view);
            this.imageView = view;
        }
    }

    private static class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        SpacesItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(android.graphics.Rect outRect, android.view.View view,
                                   RecyclerView parent, RecyclerView.State state) {
            outRect.left = spacing;
            outRect.right = spacing;
            outRect.top = spacing;
            outRect.bottom = spacing;
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }
}

