package com.qrmaster.app;

import android.util.Log;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * 👁️ EYE TRACKER STUB PLUGIN - Java Version for Testing
 * Simple stub to test plugin registration and basic functionality
 */
@CapacitorPlugin(name = "EyeTracker")
public class EyeTrackerPluginStub extends Plugin {
    
    static {
        System.out.println("🔥 EyeTrackerPluginStub class loaded!");
    }
    
    private static final String TAG = "EyeTrackerStub";
    
    @Override
    public void load() {
        Log.i(TAG, "✅ EyeTrackerPluginStub loaded successfully!");
    }
    
    @PluginMethod
    public void start(PluginCall call) {
        Log.i(TAG, "🚀 EyeTracker start() called");
        
        JSObject result = new JSObject();
        result.put("hasOverlay", false);
        result.put("hasAccessibility", false);
        result.put("hasCamera", false);
        result.put("deviceSupported", false);
        
        call.resolve(result);
        Log.i(TAG, "⚠️ EyeTracker STUB - Not implemented yet");
    }
    
    @PluginMethod
    public void stop(PluginCall call) {
        Log.i(TAG, "🛑 EyeTracker stop() called");
        call.resolve();
    }
    
    @PluginMethod
    public void calibrate(PluginCall call) {
        Log.i(TAG, "🎯 EyeTracker calibrate() called");
        call.reject("Not implemented in stub");
    }
    
    @PluginMethod
    public void setActionMap(PluginCall call) {
        Log.i(TAG, "🎮 EyeTracker setActionMap() called");
        call.resolve();
    }
    
    @PluginMethod
    public void enablePointer(PluginCall call) {
        Log.i(TAG, "👆 EyeTracker enablePointer() called");
        call.resolve();
    }
}
