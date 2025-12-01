package com.qrmaster.app;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * 🔧 ACCESSİBİLİTY KONTROL VE DÜZELTME
 */
public class AccessibilityChecker {
    
    private static final String TAG = "AccessibilityChecker";
    
    /**
     * Accessibility servisinin durumunu kontrol et ve kullanıcıyı bilgilendir
     */
    public static boolean checkAndPrompt(Context context) {
        boolean isEnabled = AutoClickerAccessibilityService.isServiceEnabled(context);
        boolean instanceExists = AutoClickerAccessibilityService.getInstance() != null;
        
        Log.d(TAG, "====================================");
        Log.d(TAG, "🔍 ACCESSIBILITY SERVICE STATUS:");
        Log.d(TAG, "   Settings Enabled: " + isEnabled);
        Log.d(TAG, "   Instance Exists: " + instanceExists);
        Log.d(TAG, "====================================");
        
        if (!isEnabled) {
            Log.e(TAG, "❌ Accessibility Service NOT ENABLED in Settings!");
            
            Toast.makeText(context,
                "⚠️ ERİŞİLEBİLİRLİK SERVİSİ KAPALI!\n\n" +
                "Ayarlar → Erişilebilirlik → 1STQR → Aç",
                Toast.LENGTH_LONG).show();
            
            // Kullanıcıyı ayarlara yönlendir
            try {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Could not open accessibility settings: " + e.getMessage());
            }
            
            return false;
        }
        
        if (!instanceExists) {
            Log.w(TAG, "⚠️ Service enabled but instance is NULL!");
            Log.w(TAG, "   This might be temporary, waiting for service to start...");
            
            Toast.makeText(context,
                "⏳ Erişilebilirlik servisi başlatılıyor...\n5 saniye bekleyin",
                Toast.LENGTH_SHORT).show();
            
            return false;
        }
        
        Log.d(TAG, "✅ Accessibility Service is READY!");
        return true;
    }
    
    /**
     * Kullanıcıya detaylı talimat göster
     */
    public static void showDetailedInstructions(Context context) {
        String instructions = 
            "📱 ERİŞİLEBİLİRLİK SERVİSİ NASIL AKTİF EDİLİR?\n\n" +
            "1️⃣ Telefon Ayarları → Erişilebilirlik\n" +
            "2️⃣ İndirilen Uygulamalar → 1STQR\n" +
            "3️⃣ Servis kapalı olarak gösterilecek\n" +
            "4️⃣ Aç/Kapat düğmesine basın\n" +
            "5️⃣ İzinleri onaylayın\n" +
            "6️⃣ Uygulamayı yeniden açın\n\n" +
            "⚠️ Bu izin olmadan Smart Booker çalışamaz!";
        
        Toast.makeText(context, instructions, Toast.LENGTH_LONG).show();
        
        Log.d(TAG, "ℹ️ Detailed instructions shown to user");
    }
}



























