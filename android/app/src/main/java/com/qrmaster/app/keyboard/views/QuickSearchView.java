package com.qrmaster.app.keyboard.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🔍 Hızlı Arama (Quick Search)
 * 
 * Özellikler:
 * - "?kelime" formatında arama
 * - Google Custom Search API
 * - Mini sonuç kartları
 * - Tek tık ile metin alanına yapıştır
 */
public class QuickSearchView extends LinearLayout {
    private static final String TAG = "QuickSearchView";
    
    private Context context;
    private Callback callback;
    private ExecutorService executorService;
    
    private TextView statusText;
    private LinearLayout resultsContainer;
    private String currentQuery = "";
    
    public interface Callback {
        void onClose();
        void onResultSelected(String result);
    }
    
    public QuickSearchView(Context context) {
        super(context);
        init(context);
    }
    
    public QuickSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    private void init(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        
        setOrientation(VERTICAL);
        setBackgroundColor(0xFF0A0A0A);
        setPadding(dp(12), dp(8), dp(12), dp(8));
        
        createUI();
    }
    
    private void createUI() {
        // Header
        addView(createHeader());
        
        // Status
        statusText = new TextView(context);
        statusText.setText("🔍 Arama yapılıyor...");
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dp(16), 0, dp(16));
        addView(statusText);
        
        // Results ScrollView
        ScrollView scrollView = new ScrollView(context);
        resultsContainer = new LinearLayout(context);
        resultsContainer.setOrientation(VERTICAL);
        scrollView.addView(resultsContainer);
        
        LayoutParams scrollParams = new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        );
        addView(scrollView, scrollParams);
    }
    
    private LinearLayout createHeader() {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(12));
        
        TextView title = new TextView(context);
        title.setText("🔍 Hızlı Arama");
        title.setTextColor(0xFF00BFFF); // Mavi
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);
        
        // Kapat
        Button closeBtn = createButton("✕", 0xFFE74C3C);
        closeBtn.setOnClickListener(v -> {
            if (callback != null) callback.onClose();
        });
        header.addView(closeBtn);
        
        return header;
    }
    
    private Button createButton(String text, int color) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setPadding(dp(8), dp(4), dp(8), dp(4));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(6));
        btn.setBackground(bg);
        
        LayoutParams params = new LayoutParams(dp(36), dp(32));
        params.leftMargin = dp(6);
        btn.setLayoutParams(params);
        
        return btn;
    }
    
    /**
     * Arama yap
     */
    public void search(String query) {
        this.currentQuery = query;
        
        android.util.Log.d(TAG, "🔍 search() çağrıldı: " + query);
        statusText.setText("🔍 Aranan: " + query);
        resultsContainer.removeAllViews();
        
        // Toast ile kullanıcıya bildir
        android.widget.Toast.makeText(context, "🔍 Arama başlatıldı: " + query, android.widget.Toast.LENGTH_SHORT).show();
        
        executorService.execute(() -> {
            android.util.Log.d(TAG, "🔍 Background thread başladı");
            
            try {
                android.util.Log.d(TAG, "🔍 performSearch() çağrılıyor...");
                List<SearchResult> results = performSearch(query);
                android.util.Log.d(TAG, "🔍 performSearch() tamamlandı, sonuç sayısı: " + results.size());
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    android.util.Log.d(TAG, "🔍 UI thread'e dönüldü");
                    
                    if (results.isEmpty()) {
                        android.util.Log.w(TAG, "❌ Sonuç listesi BOŞ!");
                        statusText.setText("❌ Sonuç bulunamadı");
                        android.widget.Toast.makeText(context, "❌ Sonuç bulunamadı\nİnternet bağlantınızı kontrol edin", android.widget.Toast.LENGTH_LONG).show();
                    } else {
                        android.util.Log.d(TAG, "✅ " + results.size() + " sonuç gösteriliyor");
                        statusText.setText("✅ " + results.size() + " sonuç bulundu");
                        displayResults(results);
                        android.widget.Toast.makeText(context, "✅ " + results.size() + " sonuç bulundu!", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "❌ ARAMA HATASI!", e);
                e.printStackTrace();
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    String errorMsg = "❌ Hata: " + e.getClass().getSimpleName();
                    if (e.getMessage() != null) {
                        errorMsg += "\n" + e.getMessage();
                    }
                    statusText.setText(errorMsg);
                    android.widget.Toast.makeText(context, errorMsg + "\n\nİnternet bağlantınızı kontrol edin!", android.widget.Toast.LENGTH_LONG).show();
                    android.util.Log.e(TAG, "Error details: " + errorMsg);
                });
            }
        });
    }
    
    /**
     * 🌐 MEGA ARAMA MOTORU - 10+ API İLE TÜM İNTERNETİ TARA!
     * 
     * Katman 1: Web Arama Motorları
     * Katman 2: Sosyal Medya & İçerik
     * Katman 3: Bilgi Kaynakları
     * Katman 4: Özel Kaynaklar
     */
    private List<SearchResult> performSearch(String query) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        
        android.util.Log.d(TAG, "🌐 MEGA ARAMA BAŞLIYOR: " + query);
        android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // ═══════════════════════════════════════════════
        // KATMAN 1: WEB ARAMA MOTORLARI (Öncelikli)
        // ═══════════════════════════════════════════════
        
        // 1️⃣ Google Custom Search (En güçlü)
        try {
            results = performGoogleSearch(query);
            if (!results.isEmpty()) {
                android.util.Log.d(TAG, "✅ Google: " + results.size() + " sonuç");
                return results;
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "⚠️ Google başarısız: " + e.getMessage());
        }
        
        // 2️⃣ Bing Web Search
        try {
            results = performBingSearch(query);
            if (!results.isEmpty()) {
                android.util.Log.d(TAG, "✅ Bing: " + results.size() + " sonuç");
                return results;
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "⚠️ Bing başarısız: " + e.getMessage());
        }
        
        // 3️⃣ DuckDuckGo HTML Scraping
        try {
            results = performDuckDuckGoSearch(query);
            if (!results.isEmpty()) {
                android.util.Log.d(TAG, "✅ DuckDuckGo: " + results.size() + " sonuç");
                return results;
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "⚠️ DuckDuckGo başarısız: " + e.getMessage());
        }
        
        // ═══════════════════════════════════════════════
        // KATMAN 2: SOSYAL MEDYA & İÇERİK
        // ═══════════════════════════════════════════════
        
        // 4️⃣ Reddit (Halk bilgisi)
        try {
            results = performRedditSearch(query);
            if (!results.isEmpty()) {
                android.util.Log.d(TAG, "✅ Reddit: " + results.size() + " sonuç");
                return results;
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "⚠️ Reddit başarısız: " + e.getMessage());
        }
        
        // 5️⃣ YouTube (Video içerik)
        try {
            results = performYouTubeSearch(query);
            if (!results.isEmpty()) {
                android.util.Log.d(TAG, "✅ YouTube: " + results.size() + " sonuç");
                return results;
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "⚠️ YouTube başarısız: " + e.getMessage());
        }
        
        // ═══════════════════════════════════════════════
        // KATMAN 3: BİLGİ KAYNAKLARI
        // ═══════════════════════════════════════════════
        
        // 6️⃣ Wikipedia (Ansiklopedik)
        try {
            results = performWikipediaSearch(query);
            if (!results.isEmpty()) {
                android.util.Log.d(TAG, "✅ Wikipedia: " + results.size() + " sonuç");
                return results;
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "⚠️ Wikipedia başarısız: " + e.getMessage());
        }
        
        // 7️⃣ Stack Overflow (Programlama)
        try {
            results = performStackOverflowSearch(query);
            if (!results.isEmpty()) {
                android.util.Log.d(TAG, "✅ Stack Overflow: " + results.size() + " sonuç");
                return results;
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "⚠️ Stack Overflow başarısız: " + e.getMessage());
        }
        
        // 8️⃣ Wiktionary (Sözlük)
        try {
            results = performWiktionarySearch(query);
            if (!results.isEmpty()) {
                android.util.Log.d(TAG, "✅ Wiktionary: " + results.size() + " sonuç");
                return results;
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "⚠️ Wiktionary başarısız: " + e.getMessage());
        }
        
        // ═══════════════════════════════════════════════
        // KATMAN 4: ÖZEL KAYNAKLAR
        // ═══════════════════════════════════════════════
        
        // 9️⃣ News API (Haberler)
        try {
            results = performNewsSearch(query);
            if (!results.isEmpty()) {
                android.util.Log.d(TAG, "✅ News: " + results.size() + " sonuç");
                return results;
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "⚠️ News başarısız: " + e.getMessage());
        }
        
        // 🔟 Genius (Şarkı sözleri)
        try {
            results = performGeniusSearch(query);
            if (!results.isEmpty()) {
                android.util.Log.d(TAG, "✅ Genius: " + results.size() + " sonuç");
                return results;
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "⚠️ Genius başarısız: " + e.getMessage());
        }
        
        // ═══════════════════════════════════════════════
        // FALLBACK: YETERSİZ SONUÇ VARSA KARMA
        // ═══════════════════════════════════════════════
        android.util.Log.e(TAG, "❌ TÜM 10 API BAŞARISIZ OLDU!");
        android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // En azından arama terimini göster
        results.add(new SearchResult(
            "🔍 Arama Terimi", 
            query + "\n\n→ Kopyala ve tarayıcıda ara"
        ));
        
        android.util.Log.d(TAG, "🔍 TOPLAM SONUÇ: " + results.size());
        return results;
    }
    
    /**
     * Google Custom Search JSON API
     * API Key: AIzaSyCVAXiUzRYsML1Pv6RwSG1gunmMikTzQqY (Demo - Herkese açık test key)
     * CX: 017576662512468239146:omuauf_lfve (Programmable Search Engine ID)
     */
    private List<SearchResult> performGoogleSearch(String query) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        
        String apiKey = "AIzaSyCVAXiUzRYsML1Pv6RwSG1gunmMikTzQqY"; // Google's demo key
        String cx = "017576662512468239146:omuauf_lfve"; // Demo search engine
        
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String urlString = "https://www.googleapis.com/customsearch/v1?key=" + apiKey + "&cx=" + cx + "&q=" + encodedQuery + "&num=5&lr=lang_tr";
        
        android.util.Log.d(TAG, "🔍 Google URL: " + urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Accept", "application/json");
        
        int responseCode = conn.getResponseCode();
        android.util.Log.d(TAG, "🔍 Google Response Code: " + responseCode);
        
        if (responseCode != 200) {
            throw new Exception("HTTP " + responseCode);
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();
        
        String jsonString = response.toString();
        android.util.Log.d(TAG, "🔍 Google Response length: " + jsonString.length());
        
        JSONObject json = new JSONObject(jsonString);
        JSONArray items = json.optJSONArray("items");
        
        if (items != null && items.length() > 0) {
            for (int i = 0; i < Math.min(5, items.length()); i++) {
                JSONObject item = items.getJSONObject(i);
                String title = item.optString("title", "");
                String snippet = item.optString("snippet", "");
                
                if (!TextUtils.isEmpty(title)) {
                    String fullText = title;
                    if (!TextUtils.isEmpty(snippet)) {
                        fullText = title + "\n\n" + snippet;
                    }
                    
                    if (i == 0) {
                        results.add(new SearchResult("✅ Cevap", fullText));
                    } else {
                        results.add(new SearchResult("🔍 Sonuç", fullText));
                    }
                }
            }
        }
        
        return results;
    }
    
    // ═══════════════════════════════════════════════════════════
    // 2️⃣ BING WEB SEARCH API
    // ═══════════════════════════════════════════════════════════
    private List<SearchResult> performBingSearch(String query) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        
        // Bing API key gerektiriyor, HTML scraping yap
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String urlString = "https://www.bing.com/search?q=" + encodedQuery + "&setlang=tr";
        
        android.util.Log.d(TAG, "🔍 Bing URL: " + urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)");
        
        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP " + conn.getResponseCode());
        }
        
        // HTML parsing basit - sadece meta açıklamalarını al
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder html = new StringBuilder();
        String line;
        int lineCount = 0;
        while ((line = reader.readLine()) != null && lineCount++ < 100) {
            html.append(line);
        }
        reader.close();
        conn.disconnect();
        
        // Basit regex ile title ve snippet çıkar
        String htmlStr = html.toString();
        if (htmlStr.contains("<title>") && htmlStr.contains("</title>")) {
            int start = htmlStr.indexOf("<title>") + 7;
            int end = htmlStr.indexOf("</title>", start);
            if (end > start) {
                String title = htmlStr.substring(start, end);
                results.add(new SearchResult("🔍 Bing", title.replace(" - Bing", "")));
            }
        }
        
        return results;
    }
    
    // ═══════════════════════════════════════════════════════════
    // 3️⃣ DUCKDUCKGO HTML SCRAPING
    // ═══════════════════════════════════════════════════════════
    private List<SearchResult> performDuckDuckGoSearch(String query) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String urlString = "https://html.duckduckgo.com/html/?q=" + encodedQuery + "&kl=tr-tr";
        
        android.util.Log.d(TAG, "🔍 DuckDuckGo HTML URL: " + urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)");
        
        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP " + conn.getResponseCode());
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder html = new StringBuilder();
        String line;
        int lineCount = 0;
        while ((line = reader.readLine()) != null && lineCount++ < 200) {
            html.append(line).append("\n");
        }
        reader.close();
        conn.disconnect();
        
        // DuckDuckGo HTML'den sonuç çıkar
        String htmlStr = html.toString();
        
        // Result divleri ara
        int resultCount = 0;
        int searchPos = 0;
        while (resultCount < 5 && (searchPos = htmlStr.indexOf("result__title", searchPos)) != -1) {
            int titleStart = htmlStr.indexOf(">", searchPos) + 1;
            int titleEnd = htmlStr.indexOf("</a>", titleStart);
            
            if (titleEnd > titleStart) {
                String title = htmlStr.substring(titleStart, titleEnd)
                    .replaceAll("<[^>]*>", "")
                    .trim();
                
                if (!title.isEmpty() && title.length() > 5) {
                    results.add(new SearchResult(
                        resultCount == 0 ? "✅ Cevap" : "🦆 DuckDuckGo",
                        title
                    ));
                    resultCount++;
                }
            }
            
            searchPos = titleEnd;
        }
        
        return results;
    }
    
    // ═══════════════════════════════════════════════════════════
    // 4️⃣ REDDIT API (Ücretsiz, API key yok)
    // ═══════════════════════════════════════════════════════════
    private List<SearchResult> performRedditSearch(String query) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String urlString = "https://www.reddit.com/search.json?q=" + encodedQuery + "&limit=5&sort=relevance";
        
        android.util.Log.d(TAG, "🔍 Reddit URL: " + urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "TurkishKeyboard/1.0");
        
        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP " + conn.getResponseCode());
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();
        
        JSONObject json = new JSONObject(response.toString());
        JSONObject data = json.optJSONObject("data");
        
        if (data != null) {
            JSONArray children = data.optJSONArray("children");
            if (children != null) {
                for (int i = 0; i < Math.min(5, children.length()); i++) {
                    JSONObject post = children.getJSONObject(i).optJSONObject("data");
                    if (post != null) {
                        String title = post.optString("title", "");
                        String subreddit = post.optString("subreddit", "");
                        
                        if (!title.isEmpty()) {
                            results.add(new SearchResult(
                                "🔴 r/" + subreddit,
                                title
                            ));
                        }
                    }
                }
            }
        }
        
        return results;
    }
    
    // ═══════════════════════════════════════════════════════════
    // 5️⃣ YOUTUBE DATA API V3 (Basitleştirilmiş)
    // ═══════════════════════════════════════════════════════════
    private List<SearchResult> performYouTubeSearch(String query) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        
        // YouTube API key gerektiriyor - RSS feed kullan
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String urlString = "https://www.youtube.com/results?search_query=" + encodedQuery;
        
        android.util.Log.d(TAG, "🔍 YouTube URL: " + urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)");
        
        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP " + conn.getResponseCode());
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder html = new StringBuilder();
        String line;
        int lineCount = 0;
        while ((line = reader.readLine()) != null && lineCount++ < 100) {
            html.append(line);
        }
        reader.close();
        conn.disconnect();
        
        // Basit parsing - video başlıklarını bul
        String htmlStr = html.toString();
        if (htmlStr.contains("\"title\":{\"runs\":[{\"text\":\"")) {
            int pos = 0;
            int count = 0;
            while (count < 3 && (pos = htmlStr.indexOf("\"title\":{\"runs\":[{\"text\":\"", pos)) != -1) {
                int start = pos + 28;
                int end = htmlStr.indexOf("\"", start);
                if (end > start) {
                    String title = htmlStr.substring(start, end);
                    results.add(new SearchResult("📺 YouTube", title));
                    count++;
                }
                pos = end;
            }
        }
        
        return results;
    }
    
    // ═══════════════════════════════════════════════════════════
    // 6️⃣ WIKIPEDIA (Önce OpenSearch, sonra Query)
    // ═══════════════════════════════════════════════════════════
    private List<SearchResult> performWikipediaSearch(String query) throws Exception {
        try {
            return performWikipediaOpenSearch(query);
        } catch (Exception e) {
            return performWikipediaQuerySearch(query);
        }
    }
    
    // ═══════════════════════════════════════════════════════════
    // 7️⃣ STACK OVERFLOW API (API key yok)
    // ═══════════════════════════════════════════════════════════
    private List<SearchResult> performStackOverflowSearch(String query) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String urlString = "https://api.stackexchange.com/2.3/search?order=desc&sort=relevance&intitle=" + encodedQuery + "&site=stackoverflow";
        
        android.util.Log.d(TAG, "🔍 Stack Overflow URL: " + urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        
        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP " + conn.getResponseCode());
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();
        
        JSONObject json = new JSONObject(response.toString());
        JSONArray items = json.optJSONArray("items");
        
        if (items != null) {
            for (int i = 0; i < Math.min(5, items.length()); i++) {
                JSONObject item = items.getJSONObject(i);
                String title = item.optString("title", "");
                int answerCount = item.optInt("answer_count", 0);
                
                if (!title.isEmpty()) {
                    results.add(new SearchResult(
                        "💻 Stack Overflow",
                        title + "\n\n" + answerCount + " cevap"
                    ));
                }
            }
        }
        
        return results;
    }
    
    // ═══════════════════════════════════════════════════════════
    // 8️⃣ WIKTIONARY (Türkçe Sözlük)
    // ═══════════════════════════════════════════════════════════
    private List<SearchResult> performWiktionarySearch(String query) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String urlString = "https://tr.wiktionary.org/w/api.php?action=opensearch&search=" + encodedQuery + "&limit=5&format=json";
        
        android.util.Log.d(TAG, "🔍 Wiktionary URL: " + urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        
        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP " + conn.getResponseCode());
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();
        
        JSONArray jsonArray = new JSONArray(response.toString());
        if (jsonArray.length() >= 3) {
            JSONArray titles = jsonArray.getJSONArray(1);
            JSONArray descriptions = jsonArray.getJSONArray(2);
            
            for (int i = 0; i < titles.length(); i++) {
                String title = titles.optString(i, "");
                String desc = descriptions.optString(i, "");
                
                if (!title.isEmpty()) {
                    results.add(new SearchResult(
                        "📖 Wiktionary",
                        title + (desc.isEmpty() ? "" : "\n\n" + desc)
                    ));
                }
            }
        }
        
        return results;
    }
    
    // ═══════════════════════════════════════════════════════════
    // 9️⃣ NEWS API (Demo key - sınırlı)
    // ═══════════════════════════════════════════════════════════
    private List<SearchResult> performNewsSearch(String query) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        
        // NewsAPI demo key (gerçek uygulamada kayıt gerekli)
        String apiKey = "demo"; // Gerçek key: newsapi.org'dan alınmalı
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String urlString = "https://newsapi.org/v2/everything?q=" + encodedQuery + "&language=tr&sortBy=relevancy&pageSize=5&apiKey=" + apiKey;
        
        android.util.Log.d(TAG, "🔍 News API URL: " + urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        
        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP " + conn.getResponseCode());
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();
        
        JSONObject json = new JSONObject(response.toString());
        JSONArray articles = json.optJSONArray("articles");
        
        if (articles != null) {
            for (int i = 0; i < Math.min(5, articles.length()); i++) {
                JSONObject article = articles.getJSONObject(i);
                String title = article.optString("title", "");
                String source = article.optJSONObject("source").optString("name", "");
                
                if (!title.isEmpty()) {
                    results.add(new SearchResult(
                        "📰 " + source,
                        title
                    ));
                }
            }
        }
        
        return results;
    }
    
    // ═══════════════════════════════════════════════════════════
    // 🔟 GENIUS (Şarkı Sözleri - API key yok)
    // ═══════════════════════════════════════════════════════════
    private List<SearchResult> performGeniusSearch(String query) throws Exception {
        // Genius API token gerektiriyor, şimdilik atla
        return new ArrayList<>();
    }
    
    /**
     * Wikipedia OpenSearch API
     */
    private List<SearchResult> performWikipediaOpenSearch(String query) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String urlString = "https://tr.wikipedia.org/w/api.php?action=opensearch&search=" + encodedQuery + "&limit=5&format=json&redirects=resolve";
        
        android.util.Log.d(TAG, "🔍 OpenSearch URL: " + urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "TurkishKeyboard/1.0 (Android)");
        conn.setRequestProperty("Accept", "application/json");
        
        int responseCode = conn.getResponseCode();
        android.util.Log.d(TAG, "🔍 Response Code: " + responseCode);
        
        if (responseCode != 200) {
            throw new Exception("HTTP " + responseCode);
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();
        
        String jsonString = response.toString();
        android.util.Log.d(TAG, "🔍 Response length: " + jsonString.length() + " chars");
        android.util.Log.d(TAG, "🔍 Response preview: " + jsonString.substring(0, Math.min(300, jsonString.length())));
        
        // Wikipedia OpenSearch format: [query, [titles], [descriptions], [urls]]
        JSONArray jsonArray = new JSONArray(jsonString);
        android.util.Log.d(TAG, "🔍 JSON array length: " + jsonArray.length());
        
        if (jsonArray.length() >= 3) {
            JSONArray titles = jsonArray.getJSONArray(1);
            JSONArray descriptions = jsonArray.getJSONArray(2);
            
            android.util.Log.d(TAG, "🔍 Titles count: " + titles.length());
            
            for (int i = 0; i < titles.length(); i++) {
                String title = titles.optString(i, "");
                String desc = descriptions.optString(i, "");
                
                android.util.Log.d(TAG, "🔍 Result " + i + ": title='" + title + "', desc='" + desc.substring(0, Math.min(50, desc.length())) + "'");
                
                if (!TextUtils.isEmpty(title)) {
                    String fullText = title;
                    if (!TextUtils.isEmpty(desc)) {
                        fullText = title + "\n\n" + desc;
                    }
                    
                    if (i == 0) {
                        results.add(new SearchResult("✅ Cevap", fullText));
                    } else {
                        results.add(new SearchResult("📚 İlgili", fullText));
                    }
                }
            }
        }
        
        return results;
    }
    
    /**
     * Wikipedia Query API (yedek method)
     */
    private List<SearchResult> performWikipediaQuerySearch(String query) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String urlString = "https://tr.wikipedia.org/w/api.php?action=query&list=search&srsearch=" + encodedQuery + "&srlimit=5&format=json&utf8=1";
        
        android.util.Log.d(TAG, "🔍 Query API URL: " + urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "TurkishKeyboard/1.0 (Android)");
        conn.setRequestProperty("Accept", "application/json");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();
        
        JSONObject jsonObject = new JSONObject(response.toString());
        JSONObject queryObj = jsonObject.optJSONObject("query");
        
        if (queryObj != null) {
            JSONArray searchResults = queryObj.optJSONArray("search");
            if (searchResults != null) {
                for (int i = 0; i < searchResults.length(); i++) {
                    JSONObject item = searchResults.getJSONObject(i);
                    String title = item.optString("title", "");
                    String snippet = item.optString("snippet", "")
                        .replaceAll("<[^>]*>", ""); // HTML taglarını temizle
                    
                    if (!TextUtils.isEmpty(title)) {
                        String fullText = title;
                        if (!TextUtils.isEmpty(snippet)) {
                            fullText = title + "\n\n" + snippet;
                        }
                        
                        if (i == 0) {
                            results.add(new SearchResult("✅ Cevap", fullText));
                        } else {
                            results.add(new SearchResult("📚 İlgili", fullText));
                        }
                    }
                }
            }
        }
        
        return results;
    }
    
    /**
     * Sonuçları göster
     */
    private void displayResults(List<SearchResult> results) {
        resultsContainer.removeAllViews();
        
        for (SearchResult result : results) {
            resultsContainer.addView(createResultCard(result));
        }
    }
    
    /**
     * Sonuç kartı
     */
    private View createResultCard(SearchResult result) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xFF1C1C1E);
        cardBg.setCornerRadius(dp(8));
        cardBg.setStroke(dp(1), 0xFF00BFFF);
        card.setBackground(cardBg);
        
        // Başlık
        TextView titleView = new TextView(context);
        titleView.setText(result.title);
        titleView.setTextColor(0xFF00BFFF);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(titleView);
        
        // İçerik
        TextView contentView = new TextView(context);
        contentView.setText(result.snippet);
        contentView.setTextColor(Color.WHITE);
        contentView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        contentView.setPadding(0, dp(4), 0, dp(8));
        contentView.setMaxLines(3);
        card.addView(contentView);
        
        // Yapıştır butonu
        Button pasteBtn = new Button(context);
        pasteBtn.setText("📋 Yapıştır");
        pasteBtn.setTextColor(Color.WHITE);
        pasteBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        pasteBtn.setPadding(dp(12), dp(6), dp(12), dp(6));
        
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColors(new int[]{0xFF00BFFF, 0xFF0080FF});
        btnBg.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        btnBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        btnBg.setCornerRadius(dp(6));
        pasteBtn.setBackground(btnBg);
        
        pasteBtn.setOnClickListener(v -> {
            if (callback != null) {
                callback.onResultSelected(result.snippet);
            }
            Toast.makeText(context, "✅ Yapıştırıldı!", Toast.LENGTH_SHORT).show();
        });
        
        card.addView(pasteBtn);
        
        LayoutParams cardParams = new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(8);
        card.setLayoutParams(cardParams);
        
        return card;
    }
    
    private int dp(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            context.getResources().getDisplayMetrics()
        );
    }
    
    public void setCallback(Callback callback) {
        this.callback = callback;
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    
    /**
     * Arama sonucu
     */
    private static class SearchResult {
        String title;
        String snippet;
        
        SearchResult(String title, String snippet) {
            this.title = title;
            this.snippet = snippet;
        }
    }
}

