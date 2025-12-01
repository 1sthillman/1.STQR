package com.qrmaster.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.os.Handler;
import android.os.Looper;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.getcapacitor.PermissionState;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.util.UUID;

@CapacitorPlugin(
    name = "FloatingQRScanner",
    permissions = {
        @Permission(
            alias = "camera",
            strings = { Manifest.permission.CAMERA }
        )
    }
)
public class FloatingQRPlugin extends Plugin {
    
    private static final String TAG = "FloatingQRPlugin";
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final String CAMERA_PERMISSION_ALIAS = "camera";
    
    private PluginCall pendingCameraPermissionCall;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static FloatingQRPlugin instance;
    
    @Override
    public void load() {
        super.load();
        instance = this;
        Log.i(TAG, "✅ FloatingQRPlugin.load() - GERÇEK METODLAR AKTİF!");
    }
    
    public static FloatingQRPlugin getInstance() {
        return instance;
    }
    
    public static void notifyQRScanned(final String qrCode, final long timestamp, final boolean autoFillAttempted, final boolean autoFillSuccess) {
        if (instance == null) {
            Log.w(TAG, "⚠️ FloatingQRPlugin instance null - event gönderilemedi");
            return;
        }
        
        mainHandler.post(() -> {
            String eventId = UUID.randomUUID().toString();
            JSObject data = new JSObject();
            data.put("qrCode", qrCode);
            data.put("timestamp", timestamp);
            data.put("source", "floating");
            data.put("autoFillAttempted", autoFillAttempted);
            data.put("autoFillSuccess", autoFillSuccess);
            data.put("id", eventId);
            data.put("uuid", eventId);
            
            instance.notifyListeners("qrScanned", data);
            Log.d(TAG, "📤 qrScanned event gönderildi (autoFillSuccess: " + autoFillSuccess + ")");
        });
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ✅ GERÇEK İZİN KONTROLÜ VE İSTEME - TAM FONKSİYONEL
    // ═══════════════════════════════════════════════════════════════

    @PluginMethod
    public void startFloatingScanner(PluginCall call) {
        try {
            if (!isCameraPermissionGranted()) {
                Log.w(TAG, "⚠️ Kamera izni yok - başlatma iptal.");
                call.reject("Kamera izni gerekli! Lütfen önce izin verin.");
                return;
            }
            
            // Overlay iznini kontrol et
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(getContext())) {
                    call.reject("Overlay izni gerekli! Lütfen önce izin verin.");
                    return;
                }
            }
            
            // FloatingQRService'i başlat
            Intent serviceIntent = new Intent(getContext(), FloatingQRService.class);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getContext().startForegroundService(serviceIntent);
            } else {
                getContext().startService(serviceIntent);
            }
            
            Log.d(TAG, "✅ FloatingQRService başlatıldı!");
            call.resolve();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ startFloatingScanner hatası: " + e.getMessage());
            call.reject("Servis başlatılamadı: " + e.getMessage());
        }
    }
    
    @PluginMethod
    public void stopFloatingScanner(PluginCall call) {
        try {
            // FloatingQRService'i durdur
            Intent serviceIntent = new Intent(getContext(), FloatingQRService.class);
            getContext().stopService(serviceIntent);
            
            Log.d(TAG, "✅ FloatingQRService durduruldu!");
            call.resolve();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ stopFloatingScanner hatası: " + e.getMessage());
            call.reject("Servis durdurulamadı: " + e.getMessage());
        }
    }
    
    @PluginMethod
    public void checkCameraPermission(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("granted", isCameraPermissionGranted());
        call.resolve(ret);
    }
    
    @PluginMethod
    public void requestCameraPermission(PluginCall call) {
        if (isCameraPermissionGranted()) {
            JSObject ret = new JSObject();
            ret.put("granted", true);
            call.resolve(ret);
            return;
        }
        
        pendingCameraPermissionCall = call;
        requestPermissionForAlias(CAMERA_PERMISSION_ALIAS, call, "cameraPermissionCallback");
    }
    
    @PermissionCallback
    private void cameraPermissionCallback(PluginCall call) {
        boolean granted = isCameraPermissionGranted();
        if (call == null) {
            call = pendingCameraPermissionCall;
        }
        
        if (call == null) {
            Log.w(TAG, "⚠️ cameraPermissionCallback çağrıldı ancak PluginCall null");
            return;
        }
        
        if (granted) {
            JSObject ret = new JSObject();
            ret.put("granted", true);
            call.resolve(ret);
        } else {
            call.reject("Kamera izni verilmedi.");
        }
        
        pendingCameraPermissionCall = null;
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
            Log.d(TAG, "🔍 requestOverlayPermission çağrıldı");
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Context context = getActivity() != null ? getActivity() : getContext();
                
                if (!Settings.canDrawOverlays(context)) {
                    Log.d(TAG, "🚀 Settings sayfasına yönlendiriliyor...");
                    
                    Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName())
                    );
                    
                    // Activity context kullan
                    if (getActivity() != null) {
                        getActivity().startActivity(intent);
                    } else {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                    
                    Log.d(TAG, "✅ Overlay izin sayfası açıldı!");
                    
                    JSObject ret = new JSObject();
                    ret.put("success", true);
                    ret.put("message", "İzin ayarları sayfası açıldı");
                    call.resolve(ret);
                } else {
                    Log.d(TAG, "✅ Overlay izni zaten var");
                    
                    JSObject ret = new JSObject();
                    ret.put("success", true);
                    ret.put("message", "İzin zaten verilmiş");
                    call.resolve(ret);
                }
            } else {
                Log.d(TAG, "✅ Android 6.0 altı - izin gerekmiyor");
                
                JSObject ret = new JSObject();
                ret.put("success", true);
                ret.put("message", "Bu Android sürümünde izin gerekmiyor");
                call.resolve(ret);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ requestOverlayPermission HATA: " + e.getMessage());
            e.printStackTrace();
            call.reject("İzin isteği başarısız: " + e.getMessage(), e);
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
            Log.d(TAG, "🔍 requestAccessibilityPermission çağrıldı");
            
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            
            // Activity context kullan
            if (getActivity() != null) {
                getActivity().startActivity(intent);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            }
            
            Log.d(TAG, "✅ Accessibility ayarları açıldı!");
            
            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("message", "Accessibility ayarları açıldı");
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "❌ requestAccessibilityPermission HATA: " + e.getMessage());
            e.printStackTrace();
            call.reject("İzin isteği başarısız: " + e.getMessage(), e);
        }
    }
    
    /**
     * Accessibility Service'in aktif olup olmadığını kontrol eder
     */
    private boolean isAccessibilityServiceEnabled() {
        Context context = getContext();
        String service = context.getPackageName() + "/com.qrmaster.app.QRAccessibilityService";
        
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
    
    private boolean isCameraPermissionGranted() {
        PermissionState state = getPermissionState(CAMERA_PERMISSION_ALIAS);
        if (state == PermissionState.GRANTED) {
            return true;
        }
        
        return ContextCompat.checkSelfPermission(
            getContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }
}

