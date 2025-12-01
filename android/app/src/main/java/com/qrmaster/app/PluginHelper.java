package com.qrmaster.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;

/**
 * ✅ İzin isteme işlemleri için helper class
 * Tüm plugin'ler bu class'ı kullanabilir
 */
public class PluginHelper {
    
    private static final String TAG = "PluginHelper";
    
    /**
     * Overlay iznini iste - Settings sayfasını aç
     */
    public static void requestOverlayPermission(Plugin plugin, PluginCall call) {
        try {
            Log.d(TAG, "🔍 requestOverlayPermission çağrıldı");
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Activity activity = plugin.getActivity();
                Context context = activity != null ? activity : plugin.getContext();
                
                if (!Settings.canDrawOverlays(context)) {
                    Log.d(TAG, "🚀 Settings sayfasına yönlendiriliyor...");
                    
                    Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName())
                    );
                    
                    // Activity context kullan (daha güvenilir)
                    if (activity != null) {
                        activity.startActivity(intent);
                        Log.d(TAG, "✅ Activity context ile açıldı");
                    } else {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        Log.d(TAG, "✅ Application context ile açıldı");
                    }
                    
                    JSObject ret = new JSObject();
                    ret.put("success", true);
                    ret.put("opened", true);
                    call.resolve(ret);
                } else {
                    Log.d(TAG, "✅ İzin zaten var");
                    
                    JSObject ret = new JSObject();
                    ret.put("success", true);
                    ret.put("alreadyGranted", true);
                    call.resolve(ret);
                }
            } else {
                Log.d(TAG, "✅ Android 6.0 altı - izin gerekmiyor");
                
                JSObject ret = new JSObject();
                ret.put("success", true);
                ret.put("notRequired", true);
                call.resolve(ret);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ requestOverlayPermission HATA: " + e.getMessage());
            e.printStackTrace();
            call.reject("İzin isteği başarısız: " + e.getMessage(), e);
        }
    }
    
    /**
     * Accessibility iznini iste - Settings sayfasını aç
     */
    public static void requestAccessibilityPermission(Plugin plugin, PluginCall call) {
        try {
            Log.d(TAG, "🔍 requestAccessibilityPermission çağrıldı");
            
            Activity activity = plugin.getActivity();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            
            // Activity context kullan (daha güvenilir)
            if (activity != null) {
                activity.startActivity(intent);
                Log.d(TAG, "✅ Activity context ile Accessibility ayarları açıldı");
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                plugin.getContext().startActivity(intent);
                Log.d(TAG, "✅ Application context ile Accessibility ayarları açıldı");
            }
            
            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("opened", true);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "❌ requestAccessibilityPermission HATA: " + e.getMessage());
            e.printStackTrace();
            call.reject("İzin isteği başarısız: " + e.getMessage(), e);
        }
    }
}

