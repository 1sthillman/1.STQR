package com.qrmaster.app.keyboard;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GIF ve Sticker sağlayıcı
 * Gerçek uygulamada Giphy, Tenor gibi API'ler kullanılabilir
 */
public class GifStickerProvider {
    
    private static final String TAG = "GifStickerProvider";
    
    // Trending GIF kategorileri
    private static final List<String> GIF_CATEGORIES = Arrays.asList(
        "Trending", "Funny", "Love", "Happy", "Sad", "Angry",
        "Surprised", "Dance", "Cats", "Dogs", "Food", "Sports"
    );
    
    // Sticker paketleri
    private static final List<String> STICKER_PACKS = Arrays.asList(
        "Komik Yüzler", "Hayvanlar", "Yemekler", "Aktiviteler",
        "Emoji Seti 1", "Emoji Seti 2", "Tatil", "İş"
    );
    
    /**
     * GIF kategorilerini döndürür
     */
    public static List<String> getGifCategories() {
        return new ArrayList<>(GIF_CATEGORIES);
    }
    
    /**
     * Belirli kategorideki GIF'leri döndürür
     * Gerçek uygulamada API çağrısı yapılır
     */
    public static List<GifItem> getGifsForCategory(Context context, String category) {
        // Demo: Boş liste döndür, gerçekte API'den gelecek
        List<GifItem> gifs = new ArrayList<>();
        
        // Giphy API entegrasyonu için:
        // https://developers.giphy.com/docs/api
        // API Key gerekir
        
        Log.d(TAG, "GIF'ler yükleniyor: " + category);
        return gifs;
    }
    
    /**
     * GIF arama yapar
     */
    public static List<GifItem> searchGifs(Context context, String query) {
        List<GifItem> results = new ArrayList<>();
        
        // Gerçek uygulamada Giphy/Tenor API kullanılır
        Log.d(TAG, "GIF arama: " + query);
        
        return results;
    }
    
    /**
     * Sticker paketlerini döndürür
     */
    public static List<String> getStickerPacks() {
        return new ArrayList<>(STICKER_PACKS);
    }
    
    /**
     * Belirli paketteki sticker'ları döndürür
     */
    public static List<StickerItem> getStickersForPack(Context context, String packName) {
        List<StickerItem> stickers = new ArrayList<>();
        
        // Demo: Varsayılan sticker'lar eklenebilir
        Log.d(TAG, "Sticker'lar yükleniyor: " + packName);
        
        return stickers;
    }
    
    /**
     * GIF item modeli
     */
    public static class GifItem {
        public String id;
        public String url;
        public String previewUrl;
        public int width;
        public int height;
        public String title;
        
        public GifItem(String id, String url, String previewUrl, int width, int height, String title) {
            this.id = id;
            this.url = url;
            this.previewUrl = previewUrl;
            this.width = width;
            this.height = height;
            this.title = title;
        }
    }
    
    /**
     * Sticker item modeli
     */
    public static class StickerItem {
        public String id;
        public String imageUrl;
        public String packName;
        public int width;
        public int height;
        
        public StickerItem(String id, String imageUrl, String packName, int width, int height) {
            this.id = id;
            this.imageUrl = imageUrl;
            this.packName = packName;
            this.width = width;
            this.height = height;
        }
    }
}










