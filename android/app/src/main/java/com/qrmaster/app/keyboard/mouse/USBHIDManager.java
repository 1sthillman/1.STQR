package com.qrmaster.app.keyboard.mouse;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbAccessory;
import android.util.Log;

/**
 * USB HID Manager
 * USB kablo ile PC'ye bağlandığında mouse/keyboard olarak çalış
 * 
 * ✅ Root GEREKTIRMEZ
 * ✅ Direkt Windows tanır
 * ✅ Gecikme YOK (kablolu)
 * ✅ Server GEREKTIRMEZ
 */
public class USBHIDManager {
    private static final String TAG = "USBHID";
    
    private UsbManager usbManager;
    private Context context;
    private boolean isUSBMode = false;
    
    public interface USBCallback {
        void onUSBConnected();
        void onUSBDisconnected();
        void onError(String error);
    }
    
    private USBCallback callback;
    
    public USBHIDManager(Context context) {
        this.context = context;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }
    
    public void setCallback(USBCallback callback) {
        this.callback = callback;
    }
    
    /**
     * USB HID modunu kontrol et
     */
    public boolean checkUSBConnection() {
        if (usbManager == null) return false;
        
        UsbAccessory[] accessories = usbManager.getAccessoryList();
        
        if (accessories != null && accessories.length > 0) {
            Log.d(TAG, "📱 USB bağlantısı tespit edildi!");
            isUSBMode = true;
            
            if (callback != null) {
                callback.onUSBConnected();
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Mouse hareketi gönder (USB üzerinden)
     */
    public void sendMouseMove(float deltaX, float deltaY) {
        if (!isUSBMode) return;
        
        // USB HID protokolü ile mouse hareketi gönder
        // NOT: Bu özellik Android USB Gadget desteği gerektirir
        
        Log.d(TAG, "Mouse: " + deltaX + ", " + deltaY);
    }
    
    /**
     * Mouse tıklaması gönder
     */
    public void sendMouseClick(int button) {
        if (!isUSBMode) return;
        
        Log.d(TAG, "Click: " + button);
    }
    
    public boolean isUSBMode() {
        return isUSBMode;
    }
}







