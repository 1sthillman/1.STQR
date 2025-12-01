#!/usr/bin/env python3
"""
QKeyboard WiFi Mouse - PC Server
Windows 11 Precision Touchpad Emulator
Ultra Low Latency: <15ms target
"""

import asyncio
import json
import socket
import struct
import sys
import qrcode
import pyautogui
import random
import io
import threading
from datetime import datetime
from pynput import keyboard as kb, mouse
from aiohttp import web
from PIL import Image
import mss

# Constants
TCP_PORT = 58080  # Farklı port (çakışma önleme)
UDP_PORT = 59090
HTTP_PORT = 58081  # Screen streaming için HTTP server
VERSION = "1.1.0"
DEVICE_NAME = socket.gethostname()
DEVICE_ID = f"{DEVICE_NAME}_{random.randint(1000, 9999)}"

# PIN for pairing
current_pin = str(random.randint(1000, 9999))

# Connected clients
connected_clients = {}

# Screen streaming
screen_streaming_enabled = False
latest_screen_frame = None
frame_lock = threading.Lock()

# PyAutoGUI settings for ULTRA FAST response
pyautogui.FAILSAFE = False
pyautogui.PAUSE = 0
pyautogui.MINIMUM_DURATION = 0  # En hızlı hareket
pyautogui.MINIMUM_SLEEP = 0  # Bekleme yok

class MouseEmulator:
    """Windows 11 Precision Touchpad Emulator"""
    
    def __init__(self):
        self.keyboard_controller = kb.Controller()
        self.mouse_controller = mouse.Controller()
    
    def mouse_move(self, delta_x, delta_y):
        """Move mouse cursor"""
        current_x, current_y = pyautogui.position()
        pyautogui.moveTo(current_x + delta_x, current_y + delta_y, duration=0)
    
    def mouse_click(self, button):
        """Click mouse button"""
        if button == "LEFT":
            pyautogui.click()
        elif button == "RIGHT":
            pyautogui.rightClick()
        elif button == "MIDDLE":
            pyautogui.middleClick()
        elif button == "DOUBLE":
            pyautogui.doubleClick()
    
    def mouse_scroll(self, delta):
        """Scroll mouse wheel"""
        pyautogui.scroll(delta)
    
    def execute_gesture(self, gesture_name):
        """Execute Windows 11 gestures"""
        gestures = {
            # 3 Finger Gestures
            "THREE_FINGER_UP": lambda: self._key_combo([kb.Key.cmd, kb.Key.tab]),  # Task View
            "THREE_FINGER_DOWN": lambda: self._key_combo([kb.Key.cmd, 'd']),  # Show Desktop
            "THREE_FINGER_LEFT": lambda: self._key_combo([kb.Key.alt, kb.Key.shift, kb.Key.tab]),  # Alt+Tab Prev
            "THREE_FINGER_RIGHT": lambda: self._key_combo([kb.Key.alt, kb.Key.tab]),  # Alt+Tab Next
            "THREE_FINGER_TAP": lambda: self._key_combo([kb.Key.cmd, 's']),  # Search
            
            # 4 Finger Gestures
            "FOUR_FINGER_TAP": lambda: self._key_combo([kb.Key.cmd, 'a']),  # Action Center
            "FOUR_FINGER_LEFT": lambda: self._key_combo([kb.Key.cmd, kb.Key.ctrl, kb.Key.left]),  # Virtual Desktop Left
            "FOUR_FINGER_RIGHT": lambda: self._key_combo([kb.Key.cmd, kb.Key.ctrl, kb.Key.right]),  # Virtual Desktop Right
            
            # Zoom
            "ZOOM_IN": lambda: self._key_combo([kb.Key.cmd, '+']),
            "ZOOM_OUT": lambda: self._key_combo([kb.Key.cmd, '-']),
            
            # Window Management
            "ALT_TAB": lambda: self._key_combo([kb.Key.alt, kb.Key.tab]),
        }
        
        if gesture_name in gestures:
            print(f"🖐️ Gesture: {gesture_name}")
            gestures[gesture_name]()
    
    def key_press(self, key_name):
        """Press a keyboard key - FIXED VERSION"""
        try:
            # Önce özel tuşları kontrol et
            key_map = {
                "LWIN": kb.Key.cmd,
                "ENTER": kb.Key.enter,
                "ESC": kb.Key.esc,
                "TAB": kb.Key.tab,
                "SPACE": kb.Key.space,
                "BACKSPACE": kb.Key.backspace,
                "DELETE": kb.Key.delete,
                "LEFT": kb.Key.left,
                "RIGHT": kb.Key.right,
                "UP": kb.Key.up,
                "DOWN": kb.Key.down,
                "HOME": kb.Key.home,
                "END": kb.Key.end,
                "PAGE_UP": kb.Key.page_up,
                "PAGE_DOWN": kb.Key.page_down,
                "SHIFT": kb.Key.shift,
                "CTRL": kb.Key.ctrl,
                "ALT": kb.Key.alt,
            }
            
            if key_name in key_map:
                # Özel tuş - key_map'ten al ve kullan
                key = key_map[key_name]
                self.keyboard_controller.press(key)
                self.keyboard_controller.release(key)
                print(f"⌨️ Özel tuş: {key_name}")
                
            elif len(key_name) == 1:
                # Tek karakter - KeyCode.from_char() kullan
                try:
                    char_key = kb.KeyCode.from_char(key_name)
                    self.keyboard_controller.press(char_key)
                    self.keyboard_controller.release(char_key)
                    # print(f"⌨️ Karakter: {key_name}")  # Spam önleme
                except Exception as char_err:
                    # Fallback - direkt dene
                    self.keyboard_controller.press(key_name)
                    self.keyboard_controller.release(key_name)
                    
            else:
                # Bilinmeyen - KeyCode.from_char() dene
                print(f"⚠️ Bilinmeyen tuş: {key_name}")
                char_key = kb.KeyCode.from_char(key_name)
                self.keyboard_controller.press(char_key)
                self.keyboard_controller.release(char_key)
                
        except Exception as e:
            # HATA OLUŞURSA BAĞLANTIYI KOPARMADAN LOGLA
            print(f"❌ Tuş hatası ({key_name}): {e}")
            # BAĞLANTIYI KOPARMADAN devam et!
    
    def _key_combo(self, keys):
        """Press multiple keys together"""
        for key in keys:
            if isinstance(key, str):
                self.keyboard_controller.press(kb.KeyCode.from_char(key))
            else:
                self.keyboard_controller.press(key)
        
        for key in reversed(keys):
            if isinstance(key, str):
                self.keyboard_controller.release(kb.KeyCode.from_char(key))
            else:
                self.keyboard_controller.release(key)


class QKeyboardServer:
    """Main server for handling keyboard connections"""
    
    def __init__(self):
        self.emulator = MouseEmulator()
        self.tcp_server = None
        self.udp_socket = None
    
    async def start(self):
        """Start TCP and UDP servers"""
        print(f"🚀 QKeyboard WiFi Mouse Server v{VERSION}")
        print(f"📱 Cihaz: {DEVICE_NAME}")
        print(f"🔑 PIN: {current_pin}")
        print(f"📡 TCP Port: {TCP_PORT}")
        print(f"📡 UDP Port: {UDP_PORT}")
        print(f"🌐 IP: {self.get_local_ip()}")
        print()
        
        # Generate QR code for easy pairing
        self.generate_qr_code()
        
        # Start UDP listener
        asyncio.create_task(self.udp_listener())
        
        # Start TCP server with retry
        try:
            self.tcp_server = await asyncio.start_server(
                self.handle_client,
                '0.0.0.0',
                TCP_PORT
            )
        except OSError as e:
            if e.errno == 10048:
                print(f"❌ Port {TCP_PORT} kullanımda! Başka bir port deneyin veya çalışan uygulamayı kapatın.")
                sys.exit(1)
            raise
        
        # Start broadcast responder
        asyncio.create_task(self.broadcast_responder())
        
        print("✅ Sunucu başlatıldı! Android klavyeden bağlan.")
        print("🔍 Ağda keşfedilebilir durumda.")
        print()
        
        async with self.tcp_server:
            await self.tcp_server.serve_forever()
    
    async def handle_client(self, reader, writer):
        """Handle TCP client connection"""
        addr = writer.get_extra_info('peername')
        print(f"🔗 Yeni bağlantı: {addr}")
        
        client_id = None  # Initialize to avoid UnboundLocalError
        
        try:
            # Wait for authentication
            data = await reader.readline()
            msg = json.loads(data.decode())
            
            if msg.get("type") == "AUTH":
                if msg.get("pin") == current_pin:
                    # Authentication successful
                    device_name = msg.get("device_name", "Unknown")
                    client_id = f"{addr[0]}:{addr[1]}"
                    connected_clients[client_id] = {
                        "name": device_name,
                        "address": addr,
                        "writer": writer
                    }
                    
                    response = {"status": "AUTH_OK", "message": "Bağlantı başarılı"}
                    writer.write((json.dumps(response) + "\n").encode())
                    await writer.drain()
                    
                    print(f"✅ Kimlik doğrulandı: {device_name}")
                    
                    # Handle messages
                    await self.message_loop(reader, writer, client_id)
                else:
                    response = {"status": "AUTH_FAILED", "error": "Yanlış PIN"}
                    writer.write((json.dumps(response) + "\n").encode())
                    await writer.drain()
                    writer.close()
                    await writer.wait_closed()
            
        except Exception as e:
            print(f"❌ Client hatası: {e}")
        finally:
            if client_id and client_id in connected_clients:
                del connected_clients[client_id]
                print(f"🔌 Bağlantı kesildi: {addr}")
    
    async def message_loop(self, reader, writer, client_id):
        """Handle client messages - ULTRA STABLE VERSION"""
        last_ping = asyncio.get_event_loop().time()
        ping_timeout = 15  # 15 saniye timeout (daha toleranslı)
        message_count = 0
        
        try:
            while True:
                try:
                    # Timeout ile readline - takılma önleme
                    data = await asyncio.wait_for(reader.readline(), timeout=2.0)
                    
                    if not data:
                        print(f"⚠️ Client veri göndermedi, bağlantı kontrol ediliyor...")
                        break
                    
                    msg = json.loads(data.decode().strip())
                    msg_type = msg.get("type")
                    message_count += 1
                    
                    if msg_type == "PING":
                        # Send pong back - IMMEDIATELY
                        last_ping = asyncio.get_event_loop().time()
                        response = {"type": "PONG", "timestamp": msg.get("timestamp")}
                        writer.write((json.dumps(response) + "\n").encode())
                        await writer.drain()
                        
                        if message_count % 10 == 0:
                            print(f"💚 Bağlantı stabil - {message_count} mesaj")
                    
                    elif msg_type == "MOUSE_CLICK":
                        try:
                            self.emulator.mouse_click(msg.get("button"))
                        except Exception as e:
                            print(f"⚠️ Click hatası: {e}")
                    
                    elif msg_type == "MOUSE_SCROLL":
                        try:
                            self.emulator.mouse_scroll(msg.get("delta"))
                        except Exception as e:
                            print(f"⚠️ Scroll hatası: {e}")
                    
                    elif msg_type == "GESTURE":
                        try:
                            self.emulator.execute_gesture(msg.get("name"))
                        except Exception as e:
                            print(f"⚠️ Gesture hatası: {e}")
                    
                    elif msg_type == "KEY_PRESS":
                        key = msg.get("key")
                        if key:
                            try:
                                self.emulator.key_press(key)
                            except Exception as key_err:
                                # KEY_PRESS hatası olsa bile bağlantıyı KOPARMADAN devam et
                                print(f"❌ KEY_PRESS hatası ({key}): {key_err}")
                                # continue - bağlantı açık kalsın
                        else:
                            print(f"⚠️ KEY_PRESS boş key gönderdi!")
                    
                    elif msg_type == "DISCONNECT":
                        print(f"📱 Client bağlantı kesmek istiyor")
                        break
                
                except asyncio.TimeoutError:
                    # Timeout - ping kontrol et
                    elapsed = asyncio.get_event_loop().time() - last_ping
                    if elapsed > ping_timeout:
                        print(f"⚠️ Ping timeout ({elapsed:.1f}s), bağlantı koptu")
                        break
                    # Devam et - sessiz timeout
                    continue
                
                except json.JSONDecodeError as e:
                    print(f"❌ JSON parse hatası: {e}")
                    continue
        
        except Exception as e:
            print(f"❌ Message loop hatası: {e}")
            import traceback
            traceback.print_exc()
        
        finally:
            print(f"📊 Toplam {message_count} mesaj işlendi")
    
    async def udp_listener(self):
        """Listen for UDP mouse move packets"""
        self.udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.udp_socket.bind(('0.0.0.0', UDP_PORT))
        self.udp_socket.setblocking(False)
        
        print(f"📡 UDP dinleyici başlatıldı: Port {UDP_PORT}")
        
        loop = asyncio.get_event_loop()
        
        while True:
            try:
                data, addr = await loop.sock_recvfrom(self.udp_socket, 1024)
                
                if len(data) >= 5 and data[0] == 0x01:  # MOUSE_MOVE
                    delta_x = struct.unpack('>h', data[1:3])[0]
                    delta_y = struct.unpack('>h', data[3:5])[0]
                    
                    self.emulator.mouse_move(delta_x, delta_y)
            
            except Exception as e:
                await asyncio.sleep(0.001)
    
    async def broadcast_responder(self):
        """Respond to device discovery broadcasts - FARKLI PORT KULLAN"""
        try:
            # 59091 portu kullan - çakışma olmasın
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            sock.bind(('0.0.0.0', 59091))  # Farklı port
            sock.setblocking(False)
            
            print(f"📡 Broadcast responder başlatıldı: Port 59091")
            
        except Exception as e:
            print(f"⚠️ Broadcast responder hatası: {e}")
            return
        
        loop = asyncio.get_event_loop()
        
        while True:
            try:
                data, addr = await loop.sock_recvfrom(sock, 1024)
                message = data.decode()
                
                if message == "QKEYBOARD_DISCOVERY":
                    # Send response - TELEFONA GÖNDER
                    response = {
                        "type": "QKEYBOARD_SERVER",
                        "name": DEVICE_NAME,
                        "id": DEVICE_ID,
                        "ip": self.get_local_ip(),
                        "tcp_port": TCP_PORT,
                        "udp_port": UDP_PORT
                    }
                    
                    # Telefona 59090 portuna gönder (buradan 59091'de dinliyoruz)
                    response_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                    response_sock.sendto(json.dumps(response).encode(), (addr[0], UDP_PORT))
                    response_sock.close()
                    
                    print(f"🔍 Discovery yanıtı gönderildi: {addr[0]}:{UDP_PORT}")
            
            except Exception as e:
                await asyncio.sleep(0.1)
    
    def get_local_ip(self):
        """Get local IP address"""
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except:
            return "127.0.0.1"
    
    def generate_qr_code(self):
        """Generate QR code for easy pairing + PNG dosyası kaydet"""
        try:
            from PIL import Image, ImageDraw, ImageFont
        except ImportError:
            print("⚠️ Pillow kütüphanesi yüklü değil. QR kod PNG olarak kaydedilemedi.")
            print("   Yüklemek için: pip install pillow")
            return
        
        connection_data = {
            "name": DEVICE_NAME,
            "ip": self.get_local_ip(),
            "tcp_port": TCP_PORT,
            "udp_port": UDP_PORT,
            "id": DEVICE_ID,
            "pin": current_pin
        }
        
        # Terminal için ASCII QR
        qr = qrcode.QRCode(version=1, box_size=2, border=1)
        qr.add_data(json.dumps(connection_data))
        qr.make(fit=True)
        
        print("📱 QR Kod (Android klavyeden tarayın):")
        qr.print_ascii(invert=True)
        print()
        
        # PNG dosyası olarak kaydet
        try:
            qr_img = qrcode.make(json.dumps(connection_data))
            qr_img = qr_img.resize((400, 400))
            
            # Bilgi metni ekle
            final_img = Image.new('RGB', (400, 520), 'white')
            final_img.paste(qr_img, (0, 0))
            
            draw = ImageDraw.Draw(final_img)
            try:
                font_title = ImageFont.truetype("arial.ttf", 18)
                font_info = ImageFont.truetype("arial.ttf", 14)
            except:
                font_title = ImageFont.load_default()
                font_info = ImageFont.load_default()
            
            # Başlık
            draw.text((200, 410), "QKeyboard Mouse", fill='black', font=font_title, anchor='mt')
            
            # Bağlantı bilgileri
            info_text = f"IP: {connection_data['ip']}\nPORT: {connection_data['tcp_port']}\nPIN: {connection_data['pin']}"
            draw.text((10, 445), info_text, fill='black', font=font_info)
            
            filename = "qkeyboard_qr.png"
            final_img.save(filename)
            
            print(f"✅ QR kod kaydedildi: {filename}")
            print(f"   Telefonda QR okuyucu ile tarayın!")
            
            # Windows'ta otomatik aç
            try:
                import os
                os.startfile(filename)
                print(f"   📂 QR kod otomatik açıldı!")
            except:
                pass
            
            print()
            
        except Exception as e:
            print(f"⚠️ QR PNG kaydetme hatası: {e}")
            print()


if __name__ == "__main__":
    try:
        server = QKeyboardServer()
        asyncio.run(server.start())
    except KeyboardInterrupt:
        print("\n👋 Sunucu kapatılıyor...")
        sys.exit(0)

