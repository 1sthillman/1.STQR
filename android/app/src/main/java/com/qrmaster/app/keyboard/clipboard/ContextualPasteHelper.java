package com.qrmaster.app.keyboard.clipboard;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Contextual Paste - Akıllı yapıştır
 * Pano içeriğine göre aksiyon önerileri
 */
public class ContextualPasteHelper {

    public enum ContentType {
        URL,
        EMAIL,
        PHONE,
        IBAN,
        ADDRESS,
        PLAIN_TEXT,
        UNKNOWN
    }

    public static class Action {
        public String label;
        public String icon;
        public Intent intent;
        public Runnable customAction;

        public Action(String label, String icon, Intent intent) {
            this.label = label;
            this.icon = icon;
            this.intent = intent;
        }

        public Action(String label, String icon, Runnable customAction) {
            this.label = label;
            this.icon = icon;
            this.customAction = customAction;
        }
    }

    // IBAN regex (TR ile başlar, 26 karakter)
    private static final Pattern IBAN_PATTERN = Pattern.compile("^TR\\d{24}$");
    
    // Telefon regex (Türkiye formatı)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(\\+90|0)?\\s?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{2}[\\s.-]?\\d{2}$"
    );

    /**
     * İçerik tipini tespit et
     */
    public static ContentType detectType(String content) {
        if (TextUtils.isEmpty(content)) {
            return ContentType.UNKNOWN;
        }

        content = content.trim();

        // URL kontrolü
        if (Patterns.WEB_URL.matcher(content).matches() || 
            content.startsWith("http://") || content.startsWith("https://")) {
            return ContentType.URL;
        }

        // Email kontrolü
        if (Patterns.EMAIL_ADDRESS.matcher(content).matches()) {
            return ContentType.EMAIL;
        }

        // IBAN kontrolü
        String cleanIban = content.replace(" ", "").toUpperCase();
        if (IBAN_PATTERN.matcher(cleanIban).matches()) {
            return ContentType.IBAN;
        }

        // Telefon kontrolü
        if (PHONE_PATTERN.matcher(content).matches()) {
            return ContentType.PHONE;
        }

        // Adres kontrolü (basit: 2+ satır içerir veya şehir adı var)
        if (content.contains("\n") || containsCityName(content)) {
            return ContentType.ADDRESS;
        }

        return ContentType.PLAIN_TEXT;
    }

    /**
     * İçerik tipine göre aksiyon önerileri oluştur
     */
    public static List<Action> getActions(Context context, String content, ContentType type) {
        List<Action> actions = new ArrayList<>();

        switch (type) {
            case URL:
                // Tarayıcıda aç
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(content));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actions.add(new Action("Tarayıcıda Aç", "🌐", browserIntent));

                // Paylaş
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, content);
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actions.add(new Action("Paylaş", "📤", Intent.createChooser(shareIntent, "Paylaş")));
                break;

            case EMAIL:
                // Mail gönder
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:" + content));
                emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actions.add(new Action("Mail Gönder", "📧", emailIntent));

                // Rehbere ekle
                Intent contactIntent = new Intent(Intent.ACTION_INSERT);
                contactIntent.setType(android.provider.ContactsContract.Contacts.CONTENT_TYPE);
                contactIntent.putExtra(android.provider.ContactsContract.Intents.Insert.EMAIL, content);
                contactIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actions.add(new Action("Rehbere Ekle", "👤", contactIntent));
                break;

            case PHONE:
                // Ara
                String cleanPhone = content.replaceAll("[^+\\d]", "");
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + cleanPhone));
                dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actions.add(new Action("Ara", "📞", dialIntent));

                // SMS gönder
                Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                smsIntent.setData(Uri.parse("sms:" + cleanPhone));
                smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actions.add(new Action("SMS Gönder", "💬", smsIntent));

                // Rehbere ekle
                Intent addContactIntent = new Intent(Intent.ACTION_INSERT);
                addContactIntent.setType(android.provider.ContactsContract.Contacts.CONTENT_TYPE);
                addContactIntent.putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, cleanPhone);
                addContactIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actions.add(new Action("Rehbere Ekle", "👤", addContactIntent));
                break;

            case IBAN:
                // 4'lü gruplara böl
                String formattedIban = formatIban(content);
                actions.add(new Action("Düzenli Kopyala", "📋", () -> {
                    android.content.ClipboardManager clipboard = 
                        (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        android.content.ClipData clip = android.content.ClipData.newPlainText("IBAN", formattedIban);
                        clipboard.setPrimaryClip(clip);
                        android.widget.Toast.makeText(context, "IBAN kopyalandı:\n" + formattedIban, 
                            android.widget.Toast.LENGTH_SHORT).show();
                    }
                }));
                break;

            case ADDRESS:
                // Haritada göster
                try {
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW);
                    mapIntent.setData(Uri.parse("geo:0,0?q=" + Uri.encode(content)));
                    mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    actions.add(new Action("Haritada Göster", "📍", mapIntent));
                } catch (Exception e) {
                    // Geo URI desteklenmiyorsa
                }

                // Paylaş
                Intent shareAddressIntent = new Intent(Intent.ACTION_SEND);
                shareAddressIntent.setType("text/plain");
                shareAddressIntent.putExtra(Intent.EXTRA_TEXT, content);
                shareAddressIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actions.add(new Action("Paylaş", "📤", Intent.createChooser(shareAddressIntent, "Paylaş")));
                break;

            case PLAIN_TEXT:
                // Google'da ara
                Intent searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
                searchIntent.putExtra(android.app.SearchManager.QUERY, content);
                searchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actions.add(new Action("Google'da Ara", "🔍", searchIntent));

                // Paylaş
                Intent shareTextIntent = new Intent(Intent.ACTION_SEND);
                shareTextIntent.setType("text/plain");
                shareTextIntent.putExtra(Intent.EXTRA_TEXT, content);
                shareTextIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actions.add(new Action("Paylaş", "📤", Intent.createChooser(shareTextIntent, "Paylaş")));
                break;
        }

        return actions;
    }

    /**
     * IBAN'ı 4'lü gruplara böl
     */
    public static String formatIban(String iban) {
        String clean = iban.replace(" ", "").toUpperCase();
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < clean.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(" ");
            }
            formatted.append(clean.charAt(i));
        }
        return formatted.toString();
    }

    /**
     * Türkiye şehir adı içeriyor mu?
     */
    private static boolean containsCityName(String text) {
        String[] cities = {
            "istanbul", "ankara", "izmir", "bursa", "antalya", "adana", "konya",
            "gaziantep", "mersin", "diyarbakır", "kayseri", "eskişehir"
        };
        String lowerText = text.toLowerCase();
        for (String city : cities) {
            if (lowerText.contains(city)) {
                return true;
            }
        }
        return false;
    }
}

