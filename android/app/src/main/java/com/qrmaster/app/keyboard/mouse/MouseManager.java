package com.qrmaster.app.keyboard.mouse;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * WiFi Mouse Yönetimi - Bilgisayar Keşfi ve Bağlantı
 * Ultra düşük gecikme hedefi: <15ms
 */
public class MouseManager {
    private static final String TAG = "MouseManager";
    private static final int TCP_PORT = 58080;
    private static final int UDP_PORT = 59090;
    private static final String PREFS_NAME = "mouse_connections";
    
    private Socket tcpSocket;
    private DatagramSocket udpSocket;
    private PrintWriter tcpWriter;
    private BufferedReader tcpReader;
    private PCDevice currentDevice; // Mevcut bağlı cihaz
    private ExecutorService executorService;
    private boolean isConnected = false;
    private long lastPingTime = 0;
    private int latency = 0;
    
    private final Context context;
    private ConnectionCallback callback;
    
    public interface ConnectionCallback {
        void onConnected(String pcName, String ipAddress);
        void onDisconnected();
        void onLatencyUpdate(int latencyMs);
        void onError(String error);
    }
    
    public static class PCDevice {
        public String name;
        public String ipAddress;
        public String id;
        public long lastSeen;
        public boolean isPaired;
        
        public PCDevice(String name, String ipAddress, String id) {
            this.name = name;
            this.ipAddress = ipAddress;
            this.id = id;
            this.lastSeen = System.currentTimeMillis();
            this.isPaired = false;
        }
    }
    
    public MouseManager(Context context) {
        this.context = context;
        this.executorService = Executors.newFixedThreadPool(3);
    }
    
    public void setCallback(ConnectionCallback callback) {
        this.callback = callback;
    }
    
    public PCDevice getCurrentDevice() {
        return currentDevice;
    }
    
    /**
     * Ağdaki PC'leri keşfet (mDNS/Broadcast)
     */
    public void discoverDevices(DeviceDiscoveryCallback discoveryCallback) {
        if (discoveryCallback == null) {
            Log.e(TAG, "Discovery callback is NULL!");
            return;
        }
        
        Log.d(TAG, "Discovery başlatılıyor...");
        
        executorService.execute(() -> {
            List<PCDevice> devices = new ArrayList<>();
            
            try {
                Log.d(TAG, "Broadcast mesajı gönderiliyor...");
                
                // Broadcast mesajı gönder
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);
                socket.setReuseAddress(true);
                
                String discoveryMsg = "QKEYBOARD_DISCOVERY";
                byte[] buffer = discoveryMsg.getBytes();
                
                // Local subnet'teki tüm cihazlara gönder
                InetAddress broadcastAddr = InetAddress.getByName("255.255.255.255");
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddr, UDP_PORT);
                socket.send(packet);
                
                Log.d(TAG, "Broadcast gönderildi: " + broadcastAddr + ":" + UDP_PORT);
                
                // Yanıtları dinle (2 saniye)
                socket.setSoTimeout(2000);
                byte[] receiveBuffer = new byte[1024];
                
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < 2000) {
                    try {
                        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        socket.receive(receivePacket);
                        
                        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        Log.d(TAG, "Yanıt alındı: " + response);
                        
                        JSONObject json = new JSONObject(response);
                        
                        if (json.has("type") && "QKEYBOARD_SERVER".equals(json.getString("type"))) {
                            PCDevice device = new PCDevice(
                                json.getString("name"),
                                receivePacket.getAddress().getHostAddress(),
                                json.getString("id")
                            );
                            
                            // Daha önce eşleştirilmiş mi?
                            device.isPaired = isPaired(device.id);
                            devices.add(device);
                            
                            Log.d(TAG, "✅ PC bulundu: " + device.name + " (" + device.ipAddress + ")");
                        }
                    } catch (SocketTimeoutException e) {
                        // Timeout - normal
                        break;
                    }
                }
                
                socket.close();
                
                Log.d(TAG, "Discovery tamamlandı. " + devices.size() + " cihaz bulundu.");
                
                if (discoveryCallback != null) {
                    discoveryCallback.onDevicesFound(devices);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Keşif hatası", e);
                if (discoveryCallback != null) {
                    discoveryCallback.onDevicesFound(devices); // Boş liste döndür
                }
                if (callback != null) {
                    callback.onError("Cihaz keşfi başarısız: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * PC'ye bağlan (TCP + UDP)
     */
    public void connect(PCDevice device, String pin) {
        if (device == null || pin == null || pin.isEmpty()) {
            Log.e(TAG, "❌ Geçersiz device veya PIN!");
            if (callback != null) {
                callback.onError("Geçersiz bağlantı bilgisi");
            }
            return;
        }
        
        Log.d(TAG, "Bağlantı başlatılıyor: " + device.ipAddress + " PIN: " + pin);
        
        executorService.execute(() -> {
            try {
                Log.d(TAG, "TCP bağlantısı kuruluyor...");
                
                // TCP bağlantısı (kontrol kanalı) - OPTIMIZED
                tcpSocket = new Socket();
                tcpSocket.setKeepAlive(true); // TCP KEEPALIVE - bağlantı canlı tutar
                tcpSocket.setTcpNoDelay(true); // Nagle algoritmasını devre dışı bırak = düşük latency
                tcpSocket.setSoTimeout(15000); // 15 saniye read timeout
                tcpSocket.setReceiveBufferSize(65536); // 64KB buffer
                tcpSocket.setSendBufferSize(65536); // 64KB send buffer
                
                tcpSocket.connect(new InetSocketAddress(device.ipAddress, TCP_PORT), 5000);
                
                Log.d(TAG, "TCP bağlandı, kimlik doğrulanıyor...");
                
                // BUFFERED writer - daha hızlı
                tcpWriter = new PrintWriter(new java.io.BufferedWriter(
                    new java.io.OutputStreamWriter(tcpSocket.getOutputStream())), true);
                tcpReader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
                
                // Kimlik doğrulama
                JSONObject authMsg = new JSONObject();
                authMsg.put("type", "AUTH");
                authMsg.put("pin", pin);
                authMsg.put("device_name", android.os.Build.MODEL);
                tcpWriter.println(authMsg.toString());
                
                // Yanıtı bekle
                String response = tcpReader.readLine();
                JSONObject authResponse = new JSONObject(response);
                
                if ("AUTH_OK".equals(authResponse.getString("status"))) {
                    // UDP bağlantısı (mouse pozisyonu için) - OPTIMIZED
                    udpSocket = new DatagramSocket();
                    udpSocket.setSendBufferSize(65536); // 64KB send buffer
                    udpSocket.connect(InetAddress.getByName(device.ipAddress), UDP_PORT);
                    
                    isConnected = true;
                    currentDevice = device;
                    
                    // Cihazı kaydet
                    savePairedDevice(device);
                    
                    // Ping thread başlat
                    startPingThread();
                    
                    if (callback != null) {
                        callback.onConnected(device.name, device.ipAddress);
                    }
                    
                    Log.d(TAG, "✅ Bağlantı başarılı: " + device.name);
                } else {
                    throw new Exception("Kimlik doğrulama başarısız: " + authResponse.optString("error"));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Bağlantı hatası", e);
                disconnect();
                if (callback != null) {
                    callback.onError("Bağlantı başarısız: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Bağlantıyı kes
     */
    public void disconnect() {
        isConnected = false;
        
        try {
            if (tcpWriter != null) {
                JSONObject msg = new JSONObject();
                msg.put("type", "DISCONNECT");
                tcpWriter.println(msg.toString());
            }
            if (tcpSocket != null) tcpSocket.close();
            if (udpSocket != null) udpSocket.close();
        } catch (Exception e) {
            Log.e(TAG, "Disconnect hatası", e);
        }
        
        tcpSocket = null;
        udpSocket = null;
        tcpWriter = null;
        tcpReader = null;
        
        if (callback != null) {
            callback.onDisconnected();
        }
    }
    
    /**
     * Mouse hareketi gönder (UDP - ULTRA LOW LATENCY)
     */
    public void sendMouseMove(float deltaX, float deltaY) {
        if (!isConnected || udpSocket == null) return;
        
        // DIREKT GÖNDER - ExecutorService kullanma, çok yavaş!
        try {
            // Kompakt binary format: [0x01][deltaX:short][deltaY:short]
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(0x01); // MOUSE_MOVE
            dos.writeShort((short) deltaX);
            dos.writeShort((short) deltaY);
            
            byte[] data = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(data, data.length);
            udpSocket.send(packet);
        } catch (Exception e) {
            // Silent fail - gecikme olmasın
        }
    }
    
    /**
     * Mouse tıklaması gönder (TCP - güvenilir)
     */
    public void sendMouseClick(String button) {
        if (!isConnected || tcpWriter == null) return;
        
        executorService.execute(() -> {
            try {
                JSONObject msg = new JSONObject();
                msg.put("type", "MOUSE_CLICK");
                msg.put("button", button); // "LEFT", "RIGHT", "MIDDLE"
                tcpWriter.println(msg.toString());
            } catch (Exception e) {
                Log.e(TAG, "Mouse click hatası", e);
            }
        });
    }
    
    /**
     * Mouse scroll gönder
     */
    public void sendMouseScroll(int delta) {
        if (!isConnected || tcpWriter == null) return;
        
        executorService.execute(() -> {
            try {
                JSONObject msg = new JSONObject();
                msg.put("type", "MOUSE_SCROLL");
                msg.put("delta", delta);
                tcpWriter.println(msg.toString());
            } catch (Exception e) {
                Log.e(TAG, "Mouse scroll hatası", e);
            }
        });
    }
    
    /**
     * Windows 11 gesture gönder
     */
    public void sendGesture(String gestureName) {
        if (!isConnected || tcpWriter == null) return;
        
        executorService.execute(() -> {
            try {
                JSONObject msg = new JSONObject();
                msg.put("type", "GESTURE");
                msg.put("name", gestureName);
                tcpWriter.println(msg.toString());
                
                Log.d(TAG, "Gesture gönderildi: " + gestureName);
            } catch (Exception e) {
                Log.e(TAG, "Gesture hatası", e);
            }
        });
    }
    
    /**
     * Keyboard tuşu gönder
     */
    public void sendKeyPress(String key) {
        if (!isConnected || tcpWriter == null) {
            Log.w(TAG, "⚠️ sendKeyPress çağrıldı ama bağlantı yok: " + key);
            return;
        }
        
        executorService.execute(() -> {
            try {
                JSONObject msg = new JSONObject();
                msg.put("type", "KEY_PRESS");
                msg.put("key", key);
                
                // NULL check
                if (tcpWriter != null) {
                    tcpWriter.println(msg.toString());
                    tcpWriter.flush(); // FORCE SEND
                    // Log.d(TAG, "✓ Key gönderildi: " + key);
                } else {
                    Log.e(TAG, "❌ tcpWriter NULL!");
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Key press hatası: " + key, e);
                // Bağlantı koptu - reconnect dene
                isConnected = false;
                if (callback != null) {
                    callback.onDisconnected();
                }
            }
        });
    }
    
    /**
     * Ping thread - bağlantı kontrolü ve latency ölçümü
     */
    private void startPingThread() {
        executorService.execute(() -> {
            int failedPings = 0;
            final int MAX_FAILED_PINGS = 3;
            
            while (isConnected) {
                try {
                    long pingStart = System.currentTimeMillis();
                    
                    JSONObject ping = new JSONObject();
                    ping.put("type", "PING");
                    ping.put("timestamp", pingStart);
                    
                    if (tcpWriter != null) {
                        tcpWriter.println(ping.toString());
                        tcpWriter.flush();
                    } else {
                        Log.e(TAG, "❌ Ping: tcpWriter NULL!");
                        break;
                    }
                    
                    // Pong bekle (timeout ile)
                    tcpSocket.setSoTimeout(2000); // 2 saniye timeout
                    String response = tcpReader.readLine();
                    
                    if (response != null) {
                        JSONObject pong = new JSONObject(response);
                        if ("PONG".equals(pong.getString("type"))) {
                            latency = (int) (System.currentTimeMillis() - pingStart);
                            lastPingTime = System.currentTimeMillis();
                            failedPings = 0; // Reset
                            
                            if (callback != null) {
                                callback.onLatencyUpdate(latency);
                            }
                        }
                    } else {
                        failedPings++;
                        Log.w(TAG, "⚠️ Pong alınamadı (" + failedPings + "/" + MAX_FAILED_PINGS + ")");
                    }
                    
                    // Çok fazla başarısız ping - bağlantı koptu
                    if (failedPings >= MAX_FAILED_PINGS) {
                        Log.e(TAG, "❌ Bağlantı kayboldu (ping timeout)");
                        disconnect();
                        if (callback != null) {
                            callback.onDisconnected();
                        }
                        break;
                    }
                    
                    Thread.sleep(1000); // Her saniye ping
                    
                } catch (java.net.SocketTimeoutException e) {
                    failedPings++;
                    Log.w(TAG, "⚠️ Ping timeout (" + failedPings + "/" + MAX_FAILED_PINGS + ")");
                    
                    if (failedPings >= MAX_FAILED_PINGS) {
                        Log.e(TAG, "❌ Bağlantı koptu (timeout)");
                        disconnect();
                        if (callback != null) {
                            callback.onDisconnected();
                        }
                        break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Ping hatası", e);
                    disconnect();
                    if (callback != null) {
                        callback.onDisconnected();
                    }
                    break;
                }
            }
            Log.d(TAG, "Ping thread sonlandı");
        });
    }
    
    // SharedPreferences helpers
    private void savePairedDevice(PCDevice device) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putString("device_" + device.id + "_name", device.name)
            .putString("device_" + device.id + "_ip", device.ipAddress)
            .apply();
    }
    
    private boolean isPaired(String deviceId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.contains("device_" + deviceId + "_name");
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public int getLatency() {
        return latency;
    }
    
    public void cleanup() {
        disconnect();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    
    public interface DeviceDiscoveryCallback {
        void onDevicesFound(List<PCDevice> devices);
    }
}

