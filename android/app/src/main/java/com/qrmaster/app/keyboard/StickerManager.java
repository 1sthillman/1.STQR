package com.qrmaster.app.keyboard;

import android.content.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sticker Yöneticisi
 * EweSticker'dan esinlenildi
 */
public class StickerManager {
    
    public static class StickerPack {
        public final String id;
        public final String name;
        public final String icon;
        public final List<String> stickers;
        
        public StickerPack(String id, String name, String icon, List<String> stickers) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.stickers = stickers;
        }
    }
    
    private static Map<String, StickerPack> stickerPacks;
    
    public static void init(Context context) {
        if (stickerPacks != null) return;
        
        stickerPacks = new HashMap<>();
        
        // Pack 1: Mutlu Yüzler
        List<String> happy = new ArrayList<>();
        happy.add("😀");
        happy.add("😃");
        happy.add("😄");
        happy.add("😁");
        happy.add("😆");
        happy.add("😂");
        happy.add("🤣");
        happy.add("😊");
        happy.add("😇");
        happy.add("🥰");
        happy.add("😍");
        happy.add("🤩");
        happy.add("😘");
        happy.add("😗");
        happy.add("😙");
        happy.add("😚");
        happy.add("☺️");
        happy.add("😌");
        stickerPacks.put("happy", new StickerPack("happy", "Mutlu", "😄", happy));
        
        // Pack 2: Üzgün Yüzler
        List<String> sad = new ArrayList<>();
        sad.add("😔");
        sad.add("😞");
        sad.add("😟");
        sad.add("😢");
        sad.add("😭");
        sad.add("😩");
        sad.add("😫");
        sad.add("😣");
        sad.add("😖");
        sad.add("😰");
        sad.add("😨");
        sad.add("😱");
        sad.add("😓");
        sad.add("🥺");
        sad.add("😪");
        sad.add("😥");
        stickerPacks.put("sad", new StickerPack("sad", "Üzgün", "😢", sad));
        
        // Pack 3: Aşk
        List<String> love = new ArrayList<>();
        love.add("❤️");
        love.add("🧡");
        love.add("💛");
        love.add("💚");
        love.add("💙");
        love.add("💜");
        love.add("🖤");
        love.add("🤍");
        love.add("🤎");
        love.add("💔");
        love.add("💕");
        love.add("💞");
        love.add("💓");
        love.add("💗");
        love.add("💖");
        love.add("💘");
        love.add("💝");
        love.add("💟");
        love.add("💌");
        love.add("💋");
        stickerPacks.put("love", new StickerPack("love", "Aşk", "❤️", love));
        
        // Pack 4: Jestler
        List<String> gestures = new ArrayList<>();
        gestures.add("👍");
        gestures.add("👎");
        gestures.add("👌");
        gestures.add("✌️");
        gestures.add("🤞");
        gestures.add("🤟");
        gestures.add("🤘");
        gestures.add("🤙");
        gestures.add("👏");
        gestures.add("🙌");
        gestures.add("👐");
        gestures.add("🤲");
        gestures.add("🙏");
        gestures.add("💪");
        gestures.add("👋");
        gestures.add("🤚");
        gestures.add("✋");
        gestures.add("🖐️");
        gestures.add("👊");
        gestures.add("✊");
        stickerPacks.put("gestures", new StickerPack("gestures", "Jestler", "👍", gestures));
        
        // Pack 5: Hayvanlar
        List<String> animals = new ArrayList<>();
        animals.add("🐶");
        animals.add("🐱");
        animals.add("🐭");
        animals.add("🐹");
        animals.add("🐰");
        animals.add("🦊");
        animals.add("🐻");
        animals.add("🐼");
        animals.add("🐨");
        animals.add("🐯");
        animals.add("🦁");
        animals.add("🐮");
        animals.add("🐷");
        animals.add("🐸");
        animals.add("🐵");
        animals.add("🐔");
        animals.add("🐧");
        animals.add("🐦");
        animals.add("🐤");
        animals.add("🦆");
        stickerPacks.put("animals", new StickerPack("animals", "Hayvanlar", "🐶", animals));
        
        // Pack 6: Yemek
        List<String> food = new ArrayList<>();
        food.add("🍕");
        food.add("🍔");
        food.add("🍟");
        food.add("🌭");
        food.add("🍿");
        food.add("🧂");
        food.add("🥓");
        food.add("🥚");
        food.add("🍳");
        food.add("🧇");
        food.add("🥞");
        food.add("🧈");
        food.add("🍞");
        food.add("🥐");
        food.add("🥨");
        food.add("🥯");
        food.add("🍖");
        food.add("🍗");
        food.add("🥩");
        food.add("🍤");
        stickerPacks.put("food", new StickerPack("food", "Yemek", "🍕", food));
        
        // Pack 7: Doğa
        List<String> nature = new ArrayList<>();
        nature.add("🌸");
        nature.add("💐");
        nature.add("🌹");
        nature.add("🥀");
        nature.add("🌺");
        nature.add("🌻");
        nature.add("🌼");
        nature.add("🌷");
        nature.add("🌲");
        nature.add("🌳");
        nature.add("🌴");
        nature.add("🌵");
        nature.add("🌾");
        nature.add("🌿");
        nature.add("☘️");
        nature.add("🍀");
        nature.add("🍁");
        nature.add("🍂");
        nature.add("🍃");
        nature.add("🌱");
        stickerPacks.put("nature", new StickerPack("nature", "Doğa", "🌸", nature));
        
        // Pack 8: Aktivite
        List<String> activity = new ArrayList<>();
        activity.add("⚽");
        activity.add("🏀");
        activity.add("🏈");
        activity.add("⚾");
        activity.add("🥎");
        activity.add("🎾");
        activity.add("🏐");
        activity.add("🏉");
        activity.add("🥏");
        activity.add("🎱");
        activity.add("🪀");
        activity.add("🏓");
        activity.add("🏸");
        activity.add("🏒");
        activity.add("🏑");
        activity.add("🥍");
        activity.add("🏏");
        activity.add("🪃");
        activity.add("🥅");
        activity.add("⛳");
        stickerPacks.put("activity", new StickerPack("activity", "Aktivite", "⚽", activity));
    }
    
    public static List<StickerPack> getAllPacks() {
        List<StickerPack> packs = new ArrayList<>();
        if (stickerPacks != null) {
            packs.addAll(stickerPacks.values());
        }
        return packs;
    }
    
    public static StickerPack getPack(String id) {
        return stickerPacks != null ? stickerPacks.get(id) : null;
    }
}








