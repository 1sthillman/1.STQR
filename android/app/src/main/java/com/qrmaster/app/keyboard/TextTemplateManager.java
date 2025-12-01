package com.qrmaster.app.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Hızlı Metin Şablonları Yöneticisi
 * Hashtag-Keyboard'dan esinlenildi
 */
public class TextTemplateManager {
    private static final String PREFS_NAME = "text_templates";
    private static final String KEY_TEMPLATES = "templates";
    
    private final SharedPreferences prefs;
    private List<TextTemplate> templates;
    
    public static class TextTemplate {
        public String id;
        public String name;
        public String content;
        public String category; // hashtag, signature, address, other
        public long createdAt;
        
        public TextTemplate(String id, String name, String content, String category) {
            this.id = id;
            this.name = name;
            this.content = content;
            this.category = category;
            this.createdAt = System.currentTimeMillis();
        }
        
        public JSONObject toJSON() throws Exception {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("name", name);
            obj.put("content", content);
            obj.put("category", category);
            obj.put("createdAt", createdAt);
            return obj;
        }
        
        public static TextTemplate fromJSON(JSONObject obj) throws Exception {
            TextTemplate t = new TextTemplate(
                obj.getString("id"),
                obj.getString("name"),
                obj.getString("content"),
                obj.optString("category", "other")
            );
            t.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
            return t;
        }
    }
    
    public TextTemplateManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadTemplates();
        
        // İlk kullanım için örnek şablonlar
        if (templates.isEmpty()) {
            addDefaultTemplates();
        }
    }
    
    private void loadTemplates() {
        templates = new ArrayList<>();
        try {
            String json = prefs.getString(KEY_TEMPLATES, "[]");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                templates.add(TextTemplate.fromJSON(array.getJSONObject(i)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void saveTemplates() {
        try {
            JSONArray array = new JSONArray();
            for (TextTemplate t : templates) {
                array.put(t.toJSON());
            }
            prefs.edit().putString(KEY_TEMPLATES, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void addDefaultTemplates() {
        // Hashtag örnekleri
        addTemplate("Instagram Hashtags", "#instagram #instagood #photooftheday #love #instalike", "hashtag");
        addTemplate("Travel Hashtags", "#travel #wanderlust #adventure #explore #vacation", "hashtag");
        
        // İmza örnekleri
        addTemplate("Resmi İmza", "Saygılarımla,\n[Adınız]\n[Şirket]", "signature");
        addTemplate("Dostane İmza", "Sevgiler,\n[Adınız] ❤️", "signature");
        
        // Adres örneği
        addTemplate("İş Adresi", "[Şirket Adı]\n[Adres]\n[Telefon]", "address");
        
        saveTemplates();
    }
    
    public void addTemplate(String name, String content, String category) {
        String id = "template_" + System.currentTimeMillis();
        templates.add(new TextTemplate(id, name, content, category));
        saveTemplates();
    }
    
    public void updateTemplate(String id, String name, String content, String category) {
        for (TextTemplate t : templates) {
            if (t.id.equals(id)) {
                t.name = name;
                t.content = content;
                t.category = category;
                saveTemplates();
                break;
            }
        }
    }
    
    public void deleteTemplate(String id) {
        templates.removeIf(t -> t.id.equals(id));
        saveTemplates();
    }
    
    public List<TextTemplate> getAllTemplates() {
        return new ArrayList<>(templates);
    }
    
    public List<TextTemplate> getTemplatesByCategory(String category) {
        List<TextTemplate> result = new ArrayList<>();
        for (TextTemplate t : templates) {
            if (t.category.equals(category)) {
                result.add(t);
            }
        }
        return result;
    }
    
    public TextTemplate getTemplate(String id) {
        for (TextTemplate t : templates) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }
}








