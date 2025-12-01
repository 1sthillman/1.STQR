package com.qrmaster.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.JSObject;
import android.util.Log;

/**
 * ✅ AUTO CLICKER - Capacitor Plugin
 */
@CapacitorPlugin(name = "AutoClicker")
public class AutoClickerPlugin extends Plugin {
    
    private static final String TAG = "AutoClickerPlugin";
    private static AutoClickerPlugin instance;
    
    @Override
    public void load() {
        super.load();
        instance = this;
        Log.i(TAG, "✅ AutoClickerPlugin.load() - GERÇEK METODLAR AKTİF!");
    }
    
    public static AutoClickerPlugin getInstance() {
        return instance;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ✅ GERÇEK İZİN KONTROLÜ VE İSTEME - TAM FONKSİYONEL
    // ═══════════════════════════════════════════════════════════════
    
    @PluginMethod
    public void startService(PluginCall call) {
        try {
            // Overlay iznini kontrol et
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(getContext())) {
                    call.reject("Overlay izni gerekli! Lütfen önce izin verin.");
                    return;
                }
            }
            
            // Accessibility Service kontrolü
            if (!isAccessibilityServiceEnabled()) {
                call.reject("Accessibility Service izni gerekli! Lütfen önce izin verin.");
                return;
            }
            
            // AutoClickerService'i başlat
            Intent serviceIntent = new Intent(getContext(), AutoClickerService.class);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getContext().startForegroundService(serviceIntent);
            } else {
                getContext().startService(serviceIntent);
            }
            
            Log.d(TAG, "✅ AutoClickerService başlatıldı!");
            call.resolve();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ startService hatası: " + e.getMessage());
            call.reject("Servis başlatılamadı: " + e.getMessage());
        }
    }
    
    @PluginMethod
    public void stopService(PluginCall call) {
        try {
            // AutoClickerService'i durdur
            Intent serviceIntent = new Intent(getContext(), AutoClickerService.class);
            getContext().stopService(serviceIntent);
            
            Log.d(TAG, "✅ AutoClickerService durduruldu!");
            call.resolve();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ stopService hatası: " + e.getMessage());
            call.reject("Servis durdurulamadı: " + e.getMessage());
    }
    }
    
    /**
     * ✅ OVERLAY İZNİNİ KONTROL ET
     */
    @PluginMethod
    public void checkOverlayPermission(PluginCall call) {
        try {
            boolean hasPermission = false;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hasPermission = Settings.canDrawOverlays(getContext());
            } else {
                // Android 6.0 altında overlay izni gerekmez
                hasPermission = true;
            }
            
            Log.d(TAG, "✅ checkOverlayPermission: " + hasPermission);
            
            JSObject ret = new JSObject();
            ret.put("hasPermission", hasPermission);
            ret.put("granted", hasPermission);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "❌ checkOverlayPermission hatası: " + e.getMessage());
            call.reject("İzin kontrolü başarısız: " + e.getMessage());
    }
    }
    
    /**
     * ✅ OVERLAY İZNİNİ İSTE - Settings'e yönlendir
     */
    @PluginMethod
    public void requestOverlayPermission(PluginCall call) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(getContext())) {
                    Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getContext().getPackageName())
                    );
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(intent);
                    
                    Log.d(TAG, "✅ Overlay izin sayfası açıldı");
                    call.resolve();
                } else {
                    Log.d(TAG, "✅ Overlay izni zaten var");
                    call.resolve();
                }
            } else {
                Log.d(TAG, "✅ Android 6.0 altı - izin gerekmiyor");
                call.resolve();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ requestOverlayPermission hatası: " + e.getMessage());
            call.reject("İzin isteği başarısız: " + e.getMessage());
        }
    }
    
    /**
     * ✅ ACCESSIBILITY İZNİNİ KONTROL ET
     */
    @PluginMethod
    public void checkAccessibilityPermission(PluginCall call) {
        try {
            boolean hasPermission = isAccessibilityServiceEnabled();
            
            Log.d(TAG, "✅ checkAccessibilityPermission: " + hasPermission);
            
            JSObject ret = new JSObject();
            ret.put("hasPermission", hasPermission);
            ret.put("granted", hasPermission);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "❌ checkAccessibilityPermission hatası: " + e.getMessage());
            call.reject("İzin kontrolü başarısız: " + e.getMessage());
    }
    }
    
    /**
     * ✅ ACCESSIBILITY İZNİNİ İSTE - Accessibility Settings'e yönlendir
     */
    @PluginMethod
    public void requestAccessibilityPermission(PluginCall call) {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            
            Log.d(TAG, "✅ Accessibility ayarları açıldı");
            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "❌ requestAccessibilityPermission hatası: " + e.getMessage());
            call.reject("İzin isteği başarısız: " + e.getMessage());
        }
    }
    
    /**
     * Accessibility Service'in aktif olup olmadığını kontrol eder
     */
    private boolean isAccessibilityServiceEnabled() {
        Context context = getContext();
        // AutoClicker için AutoClickerAccessibilityService kontrol ediliyor
        String service = context.getPackageName() + "/com.qrmaster.app.AutoClickerAccessibilityService";
        
        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED
            );
            
            if (accessibilityEnabled == 1) {
                String settingValue = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                );
                
                if (settingValue != null) {
                    return settingValue.contains(service);
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Accessibility ayarları bulunamadı: " + e.getMessage());
        }
        
        return false;
    }
}

