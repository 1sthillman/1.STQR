# 🖱️ QKeyboard WiFi Mouse - TAM KULLANIM KILAVUZU

## ✅ ÖZELLİKLER
- ✅ **Ultra Stabil WiFi Bağlantısı** (TCP Keepalive, 64KB buffer)
- ✅ **Düşük Latency** (< 20ms hedef)
- ✅ **Otomatik Yeniden Bağlanma**
- ✅ **Klavye Yazma Modu** - PC'ye direkt yaz
- ✅ **Trackpad + Gesture** desteği
- ✅ **Tüm Windows 11 Gesture'ları**

## 🚀 KURULUM

### 1. Python Server'ı Başlat
```bash
cd C:\1STQR
python qkeyboard_server.py
```

**Gerekli kütüphaneler:**
```bash
pip install pyautogui pynput qrcode pillow
```

### 2. Firewall Ayarları
```bash
# YÖNETİCİ OLARAK çalıştır:
FIX_FIREWALL.bat
```

**Veya manuel:**
- TCP Port: 58080
- UDP Port: 59090, 59091

### 3. APK'yı Yükle
```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

## 📱 TELEFONDA KULLANIM

### Adım 1: Mouse Moduna Geç
- Quick Menu (☰) → WiFi Mouse

### Adım 2: PC'yi Bul
- "WiFi ile Bul" → Server otomatik bulunacak
- VEYA "Manuel IP Gir" → IP: `192.168.1.X`, PIN: `XXXX`

### Adım 3: Bağlan
- PIN gir → **Bağlan ✓**
- ✅ "Bağlandı: PC_ADI" göreceksin

### Adım 4: Trackpad Kullan
- **1 Parmak** → Mouse hareket
- **1 Parmak Tap** → Sol click
- **2 Parmak Tap** → Sağ click
- **2 Parmak Scroll** → Kaydır (HIZLI!)

### Adım 5: PC'ye Yaz
1. **"PC'ye Yazma Modu: KAPALI"** butonuna tıkla
2. → **AÇIK ✓** olacak
3. Artık klavyeden yazdığın her şey **PC'ye gider**!
   - Harfler ✅
   - SPACE ✅
   - ENTER ✅ (Ana klavye + 123 klavyesi)
   - BACKSPACE ✅
   - Noktalama ✅

## 🔧 SORUN GİDERME

### ❌ "PC Bulunamadı"
1. **Aynı WiFi** ağında mısın?
2. **Firewall** açık mı? → `FIX_FIREWALL.bat` çalıştır
3. **Server** çalışıyor mu? → `python qkeyboard_server.py`
4. **Manuel IP** dene

### ❌ Bağlantı Kopuyor
- **Normal!** 3 başarısız ping sonrası otomatik reconnect
- Server'da şu mesajı göreceksin:
  ```
  💚 Bağlantı stabil - 10 mesaj
  💚 Bağlantı stabil - 20 mesaj
  ```

### ❌ Enter Çalışmıyor
- **Ana klavyede** Enter var mı?
- **123 klavyesinde** sağ altta Enter var
- **Her iki Enter de** Mouse modunda PC'ye gider

### ❌ Yazı Gönderilmiyor
1. **"PC'ye Yazma Modu: AÇIK ✓"** olmalı
2. Server'da şu mesajı göreceksin:
   ```
   ⌨️ Özel tuş: SPACE
   ⌨️ Özel tuş: ENTER
   ⌨️ Özel tuş: BACKSPACE
   ```

## 📊 TEKNIK DETAYLAR

### Bağlantı Özellikleri
- **TCP KeepAlive**: Bağlantı canlı tutar
- **Nagle Algoritması**: Devre dışı (düşük latency)
- **Buffer Boyutu**: 64KB (hem send hem receive)
- **Ping Interval**: 1 saniye
- **Ping Timeout**: 3 başarısız → disconnect
- **Genel Timeout**: 15 saniye

### Optimize Edilmiş Protokol
- **UDP**: Mouse hareketi (ultra hızlı)
- **TCP**: Keyboard, click, scroll (güvenilir)
- **Binary Format**: Mouse move için kompakt
- **JSON**: Diğer tüm mesajlar

### Python Server Özellikleri
- **asyncio**: Non-blocking I/O
- **PyAutoGUI**: Tuş/mouse simülasyonu
- **Pynput**: Key mapping
- **Timeout**: 2 saniye readline, 15 saniye ping

## 🎯 İPUÇLARI

### En İyi Performans İçin
1. **5GHz WiFi** kullan (2.4GHz değil)
2. **Router'a yakın** ol
3. **Python server'ı** öncelikli yap:
   ```bash
   # Windows'ta:
   wmic process where name="python.exe" CALL setpriority "high priority"
   ```

### Klavye Kısayolları
- **Shift + Harf** → Büyük harf
- **123** → Sembol klavyesi
- **ABC** → Harf klavyesi

## ✅ DURUM KONTROLLERI

### Server Sağlıklı
```
💚 Bağlantı stabil - 10 mesaj
💚 Bağlantı stabil - 20 mesaj
⌨️ Özel tuş: SPACE
⌨️ Özel tuş: ENTER
```

### Server Sorunlu
```
⚠️ Ping timeout (5.2s), bağlantı koptu
❌ KEY_PRESS hatası (X): ...
🔌 Bağlantı kesildi: ('192.168.1.X', PORT)
```

## 🔄 YENİDEN BAĞLANMA

Bağlantı koptuğunda:
1. **Otomatik**: Telefon 3 saniye bekler
2. **Manuel**: "Bağlan" butonuna tekrar bas
3. **Hızlı**: PIN aynı kalıyor

---

**🎉 Artık profesyonel bir WiFi Mouse'un var!**

Not: Bluetooth HID desteği için root gerekiyor, WiFi önerilir.







