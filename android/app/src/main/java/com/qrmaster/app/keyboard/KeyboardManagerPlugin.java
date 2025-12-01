package com.qrmaster.app.keyboard;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "KeyboardManager")
public class KeyboardManagerPlugin extends Plugin {

    @PluginMethod
    public void openInputMethodSettings(PluginCall call) {
        try {
            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            call.resolve();
        } catch (Exception e) {
            call.reject("Klavye ayarları açılamadı: " + e.getMessage(), e);
        }
    }

    @PluginMethod
    public void showKeyboardPicker(PluginCall call) {
        try {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showInputMethodPicker();
            }
            call.resolve();
        } catch (Exception e) {
            call.reject("Klavye seçici açılamadı: " + e.getMessage(), e);
        }
    }

    @PluginMethod
    public void getStatus(PluginCall call) {
        JSObject ret = new JSObject();
        try {
            String keyboardId = TurkishKeyboardService.getComponentId(getContext());
            String enabled = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ENABLED_INPUT_METHODS);
            String current = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);

            boolean isEnabled = false;
            if (!TextUtils.isEmpty(enabled)) {
                String[] parts = enabled.split(":");
                for (String part : parts) {
                    if (keyboardId.equals(part)) {
                        isEnabled = true;
                        break;
                    }
                }
            }

            boolean isSelected = keyboardId.equals(current);

            ret.put("enabled", isEnabled);
            ret.put("selected", isSelected);
            ret.put("keyboardId", keyboardId);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Klavye durumu alınamadı: " + e.getMessage(), e);
        }
    }
}

