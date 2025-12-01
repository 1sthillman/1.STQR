package com.qrmaster.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.JSObject;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.util.UUID;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.getcapacitor.PermissionState;

@CapacitorPlugin(
    name = "OCRScanner",
    permissions = {
        @Permission(
            alias = "camera",
            strings = { Manifest.permission.CAMERA }
        )
    }
)
public class OCRPlugin extends Plugin {

    private static final String TAG = "OCRPlugin";
    private static OCRPlugin instance;
    private static final String CAMERA_PERMISSION_ALIAS = "camera";
    
    private PluginCall pendingCameraPermissionCall;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void load() {
        super.load();
        instance = this;
        Log.i(TAG, "✅ OCRPlugin.load() - GERÇEK METODLAR AKTİF!");
    }

    public static OCRPlugin getInstance() {
        return instance;
    }
    
    public static void notifyTextScanned(final String text, final long timestamp, final float confidence,
                                         final int lineCount, final int charCount, final int wordCount,
                                         final int turkishCharCount, final boolean hasTurkish,
                                         final boolean autoFillAttempted, final boolean autoFillSuccess) {
        if (instance == null) {
            Log.w(TAG, "⚠️ OCRPlugin instance null - event gönderilemedi");
            return;
        }
        
        mainHandler.post(() -> {
            String eventId = UUID.randomUUID().toString();
            JSObject data = new JSObject();
            data.put("text", text);
            data.put("timestamp", timestamp);
            data.put("confidence", confidence);
            data.put("lineCount", lineCount);
            data.put("charCount", charCount);
            data.put("wordCount", wordCount);
            data.put("turkishCharCount", turkishCharCount);
            data.put("hasTurkish", hasTurkish);
            data.put("autoFillAttempted", autoFillAttempted);
            data.put("autoFillSuccess", autoFillSuccess);
            data.put("source", "floating");
            data.put("id", eventId);
            data.put("uuid", eventId);
            
            instance.notifyListeners("textScanned", data);
            Log.d(TAG, "📤 textScanned event gönderildi (autoFillSuccess: " + autoFillSuccess + ")");
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // ✅ GERÇEK İZİN KONTROLÜ VE İSTEME - TAM FONKSİYONEL
    // ═══════════════════════════════════════════════════════════════

    @PluginMethod
    public void startFloatingOCR(PluginCall call) {
        try {
            if (!isCameraPermissionGranted()) {
                Log.w(TAG, "⚠️ Kamera izni yok - OCR başlatma iptal.");
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
            
            // FloatingOCRService'i başlat
            Intent serviceIntent = new Intent(getContext(), FloatingOCRService.class);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getContext().startForegroundService(serviceIntent);
            } else {
                getContext().startService(serviceIntent);
            }
            
            Log.d(TAG, "✅ FloatingOCRService başlatıldı!");
            call.resolve();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ startFloatingOCR hatası: " + e.getMessage());
            call.reject("Servis başlatılamadı: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stopFloatingOCR(PluginCall call) {
        try {
            // FloatingOCRService'i durdur
            Intent serviceIntent = new Intent(getContext(), FloatingOCRService.class);
            getContext().stopService(serviceIntent);
            
            Log.d(TAG, "✅ FloatingOCRService durduruldu!");
            call.resolve();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ stopFloatingOCR hatası: " + e.getMessage());
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
        // OCR için de aynı accessibility service kullanılıyor
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

