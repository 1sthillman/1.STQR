# 📱 WiFi Mouse Nasıl Kullanılır?

## ⚠️ ÖNEMLİ: WiFi ÇALIŞMIYOR MU?

Eğer sadece **USB kablosu takılıyken** çalışıyorsa:

### 🔥 **1. Windows Firewall'u Kapat veya Kural Ekle**

#### Kolay Yol (Firewall'u geçici kapat):
```cmd
# Administrator olarak PowerShell aç:
Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled False
```

#### Doğru Yol (Kural ekle):
```cmd
# FIX_FIREWALL.bat dosyasını ADMINISTRATOR olarak çalıştır
# Sağ tık → Yönetici olarak çalıştır
```

### 📡 **2. Aynı WiFi Ağında Olduğunuzu Kontrol Edin**

**PC:**
```cmd
ipconfig
```
→ IPv4 adresine bak: `192.168.1.XXX` gibi

**Telefon:**
- Ayarlar → WiFi → Bağlı ağa tıkla
- IP adresine bak: `192.168.1.YYY` gibi

**İlk 3 rakam aynı olmalı:** `192.168.1`

### 🔌 **3. Router Ayarları**

Bazı routerlar cihazlar arası iletişimi engelliyor:

1. Router admin paneline gir (genellikle `192.168.1.1`)
2. **AP Isolation** veya **Client Isolation** → **KAPALI** olmalı
3. **Wireless Isolation** → **KAPALI** olmalı

### 🎯 **4. Test Et**

```cmd
# PC'de:
python qkeyboard_server.py

# Göreceksin:
📡 Broadcast responder başlatıldı: Port 59091
✅ Sunucu başlatıldı!
```

**Telefonda:**
1. Mouse moduna gir
2. 🔗 **Bağlan** → 2 saniye bekle
3. PC bulunmazsa → 🔢 **Manuel IP** → PC'nin IP'sini gir

### ❌ **Hala Çalışmıyor mu?**

**1. Portları kontrol et:**
```cmd
netstat -an | findstr "58080 59090 59091"
```

Görmeli:
```
UDP    0.0.0.0:59090          *:*
UDP    0.0.0.0:59091          *:*
TCP    0.0.0.0:58080          *:*
```

**2. Python iznini kontrol et:**
```cmd
# Windows Defender Firewall
# → İzin verilen uygulamalar
# → Python'u bul ve tik at
```

**3. Antivirüs yazılımını kapat** (geçici olarak)

### 📲 **Manuel Bağlantı (Her Zaman Çalışır):**

1. **PC'de:** `python qkeyboard_server.py`
2. **PC'nin IP'sini not et:** örn. `192.168.1.207`
3. **PIN'i not et:** örn. `4108`
4. **Telefonda:**
   - Mouse → 🔢 (Manuel IP)
   - IP gir: `192.168.1.207`
   - PIN gir: `4108`
   - **Bağlan ✓**

### ✅ **Çalışıyor Artık!**

- **Tek parmak** → Mouse
- **İki parmak ↑↓** → Scroll (ULTRA HIZLI!)
- **Text kutusu** → PC'ye yaz
- **Enter** → Gönder

---

**Not:** Windows Firewall en yaygın sorun! `FIX_FIREWALL.bat`'ı yönetici olarak çalıştırmayı unutma!







