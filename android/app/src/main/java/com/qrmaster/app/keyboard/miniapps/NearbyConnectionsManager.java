package com.qrmaster.app.keyboard.miniapps;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 🔗 Nearby Connections Manager
 * 
 * Google Nearby Connections API kullanarak WiFi Direct ve Bluetooth 
 * üzerinden peer-to-peer bağlantı yönetimi.
 * 
 * Özellikler:
 * - Otomatik cihaz keşfi (Discovery)
 * - Reklam yayını (Advertising)
 * - Güvenli bağlantı (Connection)
 * - İki yönlü mesajlaşma (Messaging)
 * - Otomatik yeniden bağlanma
 */
public class NearbyConnectionsManager {
    private static final String TAG = "NearbyConnections";
    
    // Service ID - unique identifier for the app
    private static final String SERVICE_ID = "com.qrmaster.sharedtyping";
    
    // Strategy: P2P_CLUSTER allows many-to-many connections
    // P2P_STAR allows one-to-many (host-client)
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    
    private final ConnectionsClient connectionsClient;
    private final Context context;
    private final Callback callback;
    
    private String localEndpointName;
    private String connectedEndpointId;
    private ConnectionState state = ConnectionState.IDLE;
    
    // Discovered endpoints
    private final List<DiscoveredDevice> discoveredDevices = new ArrayList<>();
    
    public enum ConnectionState {
        IDLE,
        DISCOVERING,
        ADVERTISING,
        CONNECTING,
        CONNECTED
    }
    
    public static class DiscoveredDevice {
        public final String endpointId;
        public final String endpointName;
        
        public DiscoveredDevice(String endpointId, String endpointName) {
            this.endpointId = endpointId;
            this.endpointName = endpointName;
        }
    }
    
    public interface Callback {
        void onStateChanged(ConnectionState state);
        void onDeviceDiscovered(DiscoveredDevice device);
        void onDeviceLost(String endpointId);
        void onMessageReceived(String message);
        void onError(String error);
        
        // Optional: Override for audio payload handling
        default void onPayloadReceived(String endpointId, Payload payload) {}
    }
    
    public NearbyConnectionsManager(Context context, String deviceName, Callback callback) {
        this.context = context.getApplicationContext();
        this.localEndpointName = deviceName;
        this.callback = callback;
        this.connectionsClient = Nearby.getConnectionsClient(context);
        
        Log.d(TAG, "✅ NearbyConnectionsManager initialized: " + deviceName);
    }
    
    /**
     * Start advertising - Host modu
     * Diğer cihazların bu cihazı bulmasını sağlar
     */
    public void startAdvertising() {
        if (state != ConnectionState.IDLE) {
            Log.w(TAG, "Already in state: " + state);
            return;
        }
        
        Log.d(TAG, "📡 Starting advertising as: " + localEndpointName);
        setState(ConnectionState.ADVERTISING);
        
        AdvertisingOptions options = new AdvertisingOptions.Builder()
                .setStrategy(STRATEGY)
                .build();
        
        connectionsClient
                .startAdvertising(
                        localEndpointName,
                        SERVICE_ID,
                        connectionLifecycleCallback,
                        options)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✅ Advertising started successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Advertising failed", e);
                    setState(ConnectionState.IDLE);
                    callback.onError("Reklam başlatılamadı: " + e.getMessage());
                });
    }
    
    /**
     * Start discovery - Client modu
     * Yakındaki cihazları tarar
     */
    public void startDiscovery() {
        if (state != ConnectionState.IDLE) {
            Log.w(TAG, "Already in state: " + state);
            return;
        }
        
        Log.d(TAG, "🔍 Starting discovery...");
        setState(ConnectionState.DISCOVERING);
        discoveredDevices.clear();
        
        DiscoveryOptions options = new DiscoveryOptions.Builder()
                .setStrategy(STRATEGY)
                .build();
        
        connectionsClient
                .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✅ Discovery started successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Discovery failed", e);
                    setState(ConnectionState.IDLE);
                    callback.onError("Tarama başlatılamadı: " + e.getMessage());
                });
    }
    
    /**
     * Stop advertising
     */
    public void stopAdvertising() {
        Log.d(TAG, "⏹️ Stopping advertising");
        connectionsClient.stopAdvertising();
        if (state == ConnectionState.ADVERTISING) {
            setState(ConnectionState.IDLE);
        }
    }
    
    /**
     * Stop discovery
     */
    public void stopDiscovery() {
        Log.d(TAG, "⏹️ Stopping discovery");
        connectionsClient.stopDiscovery();
        if (state == ConnectionState.DISCOVERING) {
            setState(ConnectionState.IDLE);
        }
    }
    
    /**
     * Connect to a discovered device
     */
    public void connectToDevice(String endpointId, String endpointName) {
        Log.d(TAG, "🤝 Connecting to: " + endpointName + " (" + endpointId + ")");
        setState(ConnectionState.CONNECTING);
        
        connectionsClient
                .requestConnection(localEndpointName, endpointId, connectionLifecycleCallback)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✅ Connection request sent");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Connection request failed", e);
                    setState(ConnectionState.IDLE);
                    callback.onError("Bağlantı isteği başarısız: " + e.getMessage());
                });
    }
    
    /**
     * Send a text message to connected device
     */
    public void sendMessage(String message) {
        if (connectedEndpointId == null) {
            Log.w(TAG, "No connected endpoint");
            return;
        }
        
        if (message == null || message.isEmpty()) {
            return;
        }
        
        try {
            Payload payload = Payload.fromBytes(message.getBytes(StandardCharsets.UTF_8));
            connectionsClient.sendPayload(connectedEndpointId, payload);
            Log.d(TAG, "📤 Sent message: " + message.substring(0, Math.min(50, message.length())));
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to send message", e);
            callback.onError("Mesaj gönderilemedi: " + e.getMessage());
        }
    }
    
    /**
     * Send audio data for voice chat
     */
    public void sendAudioData(byte[] audioData, int length) {
        if (connectedEndpointId == null || audioData == null || length <= 0) {
            return;
        }
        
        try {
            byte[] trimmed = new byte[length];
            System.arraycopy(audioData, 0, trimmed, 0, length);
            Payload payload = Payload.fromBytes(trimmed);
            connectionsClient.sendPayload(connectedEndpointId, payload);
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to send audio", e);
        }
    }
    
    /**
     * Disconnect from current endpoint
     */
    public void disconnect() {
        if (connectedEndpointId != null) {
            Log.d(TAG, "🔌 Disconnecting from: " + connectedEndpointId);
            connectionsClient.disconnectFromEndpoint(connectedEndpointId);
            connectedEndpointId = null;
        }
        stopAdvertising();
        stopDiscovery();
        setState(ConnectionState.IDLE);
    }
    
    /**
     * Clean up all connections
     */
    public void release() {
        Log.d(TAG, "🧹 Releasing NearbyConnectionsManager");
        disconnect();
        connectionsClient.stopAllEndpoints();
        discoveredDevices.clear();
    }
    
    public List<DiscoveredDevice> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices);
    }
    
    public ConnectionState getState() {
        return state;
    }
    
    public boolean isConnected() {
        return state == ConnectionState.CONNECTED && connectedEndpointId != null;
    }
    
    private void setState(ConnectionState newState) {
        if (this.state != newState) {
            Log.d(TAG, "State: " + this.state + " → " + newState);
            this.state = newState;
            callback.onStateChanged(newState);
        }
    }
    
    // ===== CALLBACKS =====
    
    /**
     * Endpoint Discovery Callback - Cihaz keşfi
     */
    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
            Log.d(TAG, "🔍 Device found: " + info.getEndpointName() + " (" + endpointId + ")");
            DiscoveredDevice device = new DiscoveredDevice(endpointId, info.getEndpointName());
            
            // Aynı cihazı tekrar ekleme
            boolean exists = false;
            for (DiscoveredDevice d : discoveredDevices) {
                if (d.endpointId.equals(endpointId)) {
                    exists = true;
                    break;
                }
            }
            
            if (!exists) {
                discoveredDevices.add(device);
                callback.onDeviceDiscovered(device);
            }
        }
        
        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            Log.d(TAG, "📡 Device lost: " + endpointId);
            discoveredDevices.removeIf(d -> d.endpointId.equals(endpointId));
            callback.onDeviceLost(endpointId);
        }
    };
    
    /**
     * Connection Lifecycle Callback - Bağlantı yönetimi
     */
    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo info) {
            Log.d(TAG, "🤝 Connection initiated with: " + info.getEndpointName());
            
            // Otomatik kabul et (güvenli ortam için)
            // Gerçek uygulamada kullanıcıya onay sorulabilir
            connectionsClient.acceptConnection(endpointId, payloadCallback);
            Log.d(TAG, "✅ Connection auto-accepted");
        }
        
        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
            if (result.getStatus().isSuccess()) {
                Log.d(TAG, "✅ Connected to: " + endpointId);
                connectedEndpointId = endpointId;
                setState(ConnectionState.CONNECTED);
                
                // Bağlantı kurulunca tarama/reklamı durdur
                stopAdvertising();
                stopDiscovery();
            } else {
                Log.e(TAG, "❌ Connection failed: " + result.getStatus());
                setState(ConnectionState.IDLE);
                callback.onError("Bağlantı başarısız: " + result.getStatus().getStatusMessage());
            }
        }
        
        @Override
        public void onDisconnected(@NonNull String endpointId) {
            Log.d(TAG, "🔌 Disconnected from: " + endpointId);
            if (endpointId.equals(connectedEndpointId)) {
                connectedEndpointId = null;
                setState(ConnectionState.IDLE);
                callback.onError("Bağlantı kesildi");
            }
        }
    };
    
    /**
     * Payload Callback - Mesaj ve audio alımı
     */
    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            if (payload.getType() == Payload.Type.BYTES) {
                byte[] bytes = payload.asBytes();
                if (bytes != null) {
                    // Try to decode as text first
                    try {
                        String message = new String(bytes, StandardCharsets.UTF_8);
                        // If it starts with known prefixes, it's a text message
                        if (message.startsWith("TEXT:") || message.startsWith("VOICE:") || message.equals("VOICE_END")) {
                            Log.d(TAG, "📥 Received message: " + message.substring(0, Math.min(50, message.length())));
                            callback.onMessageReceived(message);
                        } else {
                            // Might be audio data, delegate to callback
                            callback.onPayloadReceived(endpointId, payload);
                        }
                    } catch (Exception e) {
                        // Binary data (audio), delegate to callback
                        callback.onPayloadReceived(endpointId, payload);
                    }
                }
            } else {
                // Stream or file, delegate to callback
                callback.onPayloadReceived(endpointId, payload);
            }
        }
        
        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
            // Progress tracking için
        }
    };
}

