#!/usr/bin/env python3
"""
QKeyboard WiFi Mouse Server + SCREEN STREAMING
Version 1.1.0 - Now with screen mirroring!
"""

import asyncio
import json
import socket
import sys
import io
import threading
from aiohttp import web
from PIL import Image
import mss
import pyautogui
import qrcode
import random
from pynput import keyboard as kb, mouse

# Constants
TCP_PORT = 58080
UDP_PORT = 59090
HTTP_PORT = 58081  # Screen streaming
VERSION = "1.1.0"
DEVICE_NAME = socket.gethostname()
current_pin = str(random.randint(1000, 9999))

# Screen streaming globals
screen_streaming_enabled = False
latest_screen_frame = None
frame_lock = threading.Lock()
connected_clients = {}

# PyAutoGUI settings
pyautogui.FAILSAFE = False
pyautogui.PAUSE = 0

class MouseEmulator:
    """Mouse/Keyboard emulator"""
    
    def __init__(self):
        self.keyboard_controller = kb.Controller()
        self.mouse_controller = mouse.Controller()
    
    def mouse_move(self, delta_x, delta_y):
        current_x, current_y = pyautogui.position()
        pyautogui.moveTo(current_x + delta_x, current_y + delta_y, duration=0)
    
    def mouse_click(self, button):
        if button == "LEFT":
            pyautogui.click()
        elif button == "RIGHT":
            pyautogui.rightClick()
    
    def mouse_scroll(self, delta):
        pyautogui.scroll(delta)
    
    def key_press(self, key_name):
        """Press keyboard key"""
        try:
            key_map = {
                "ENTER": kb.Key.enter, "SPACE": kb.Key.space,
                "BACKSPACE": kb.Key.backspace, "TAB": kb.Key.tab,
                "ESC": kb.Key.esc, "DELETE": kb.Key.delete,
            }
            
            if key_name in key_map:
                key = key_map[key_name]
                self.keyboard_controller.press(key)
                self.keyboard_controller.release(key)
            elif len(key_name) == 1:
                char_key = kb.KeyCode.from_char(key_name)
                self.keyboard_controller.press(char_key)
                self.keyboard_controller.release(char_key)
        except Exception as e:
            print(f"❌ Key error ({key_name}): {e}")

class ScreenCapture:
    """Screen capture for streaming"""
    
    def __init__(self):
        self.sct = None  # Thread içinde oluşturulacak
        self.running = False
        self.thread = None
        self.fps = 15  # 15 FPS
        
    def start(self):
        global screen_streaming_enabled
        if self.running:
            return
        
        self.running = True
        screen_streaming_enabled = True
        self.thread = threading.Thread(target=self._capture_loop, daemon=True)
        self.thread.start()
        print(f"📺 Screen capture başlatıldı: {self.fps} FPS")
    
    def stop(self):
        global screen_streaming_enabled
        self.running = False
        screen_streaming_enabled = False
        print(f"📺 Screen capture durduruldu")
    
    def _capture_loop(self):
        """Capture loop - mss instance thread içinde oluşturulmalı!"""
        global latest_screen_frame
        import time
        
        # MSS instance'ı BU THREAD içinde oluştur - thread-safe!
        with mss.mss() as sct:
            print(f"✅ MSS instance oluşturuldu (thread-safe)")
            
            while self.running:
                try:
                    # Primary monitor'u yakala
                    monitor = sct.monitors[1]
                    screenshot = sct.grab(monitor)
                    
                    # PIL Image'a çevir
                    img = Image.frombytes('RGB', screenshot.size, screenshot.rgb)
                    
                    # Resize to 640x360 (16:9)
                    img.thumbnail((640, 360), Image.Resampling.LANCZOS)
                    
                    # JPEG'e çevir
                    buffer = io.BytesIO()
                    img.save(buffer, format='JPEG', quality=70, optimize=True)
                    
                    # Global frame buffer'a yaz
                    with frame_lock:
                        latest_screen_frame = buffer.getvalue()
                    
                    # FPS control
                    time.sleep(1.0 / self.fps)
                    
                except Exception as e:
                    print(f"❌ Capture error: {e}")
                    import traceback
                    traceback.print_exc()
                    time.sleep(1)
            
            print(f"📺 Capture loop sonlandı")

class QKeyboardServer:
    """Main server"""
    
    def __init__(self):
        self.emulator = MouseEmulator()
        self.tcp_server = None
        self.screen_capture = ScreenCapture()
        self.http_app = None
    
    async def start(self):
        print(f"\n🚀 QKeyboard Server v{VERSION}")
        print(f"📱 Device: {DEVICE_NAME}")
        print(f"🔑 PIN: {current_pin}")
        print(f"📡 TCP: {TCP_PORT} | UDP: {UDP_PORT}")
        print(f"📺 HTTP (Screen): {HTTP_PORT}")
        print(f"🌐 IP: {self.get_local_ip()}\n")
        
        # Start HTTP server for screen streaming
        asyncio.create_task(self.start_http_server())
        
        # Start UDP listener
        asyncio.create_task(self.udp_listener())
        
        # Start TCP server
        self.tcp_server = await asyncio.start_server(
            self.handle_client, '0.0.0.0', TCP_PORT
        )
        
        print("✅ Server başlatıldı!\n")
        
        async with self.tcp_server:
            await self.tcp_server.serve_forever()
    
    async def start_http_server(self):
        """HTTP server for screen streaming"""
        self.http_app = web.Application()
        self.http_app.router.add_get('/screen', self.handle_screen_stream)
        self.http_app.router.add_post('/screen/enable', self.handle_screen_enable)
        self.http_app.router.add_post('/screen/disable', self.handle_screen_disable)
        
        runner = web.AppRunner(self.http_app)
        await runner.setup()
        site = web.TCPSite(runner, '0.0.0.0', HTTP_PORT)
        await site.start()
        print(f"📺 HTTP: http://{self.get_local_ip()}:{HTTP_PORT}/screen")
    
    async def handle_screen_stream(self, request):
        """MJPEG stream"""
        response = web.StreamResponse()
        response.content_type = 'multipart/x-mixed-replace; boundary=frame'
        await response.prepare(request)
        
        try:
            while screen_streaming_enabled:
                if latest_screen_frame:
                    with frame_lock:
                        frame_data = latest_screen_frame
                    
                    await response.write(b'--frame\r\n')
                    await response.write(b'Content-Type: image/jpeg\r\n\r\n')
                    await response.write(frame_data)
                    await response.write(b'\r\n')
                
                await asyncio.sleep(0.066)  # ~15 FPS
        except:
            pass
        return response
    
    async def handle_screen_enable(self, request):
        self.screen_capture.start()
        return web.json_response({"status": "enabled"})
    
    async def handle_screen_disable(self, request):
        self.screen_capture.stop()
        return web.json_response({"status": "disabled"})
    
    async def handle_client(self, reader, writer):
        addr = writer.get_extra_info('peername')
        print(f"🔗 Client: {addr}")
        
        try:
            data = await reader.readline()
            msg = json.loads(data.decode())
            
            if msg.get("type") == "AUTH" and msg.get("pin") == current_pin:
                writer.write((json.dumps({"status": "AUTH_OK"}) + "\n").encode())
                await writer.drain()
                print(f"✅ Authenticated: {msg.get('device_name')}")
                
                await self.message_loop(reader, writer)
        except Exception as e:
            print(f"❌ Client error: {e}")
        finally:
            print(f"🔌 Disconnected: {addr}")
    
    async def message_loop(self, reader, writer):
        """Handle messages"""
        last_ping = asyncio.get_event_loop().time()
        
        try:
            while True:
                try:
                    data = await asyncio.wait_for(reader.readline(), timeout=2.0)
                    if not data:
                        break
                    
                    msg = json.loads(data.decode().strip())
                    msg_type = msg.get("type")
                    
                    if msg_type == "PING":
                        last_ping = asyncio.get_event_loop().time()
                        response = {"type": "PONG", "timestamp": msg.get("timestamp")}
                        writer.write((json.dumps(response) + "\n").encode())
                        await writer.drain()
                    
                    elif msg_type == "MOUSE_CLICK":
                        self.emulator.mouse_click(msg.get("button"))
                    
                    elif msg_type == "MOUSE_SCROLL":
                        self.emulator.mouse_scroll(msg.get("delta"))
                    
                    elif msg_type == "KEY_PRESS":
                        key = msg.get("key")
                        if key:
                            self.emulator.key_press(key)
                    
                    elif msg_type == "DISCONNECT":
                        break
                
                except asyncio.TimeoutError:
                    elapsed = asyncio.get_event_loop().time() - last_ping
                    if elapsed > 15:
                        break
                    continue
        except:
            pass
    
    async def udp_listener(self):
        """UDP for mouse movement"""
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.bind(('0.0.0.0', UDP_PORT))
        sock.setblocking(False)
        print(f"📡 UDP: Port {UDP_PORT}")
        
        loop = asyncio.get_event_loop()
        
        while True:
            try:
                data, addr = await loop.sock_recvfrom(sock, 1024)
                
                if len(data) >= 5 and data[0] == 0x01:
                    import struct
                    deltaX = struct.unpack('>h', data[1:3])[0]
                    deltaY = struct.unpack('>h', data[3:5])[0]
                    self.emulator.mouse_move(deltaX, deltaY)
            except:
                await asyncio.sleep(0.001)
    
    def get_local_ip(self):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except:
            return "127.0.0.1"

if __name__ == "__main__":
    try:
        server = QKeyboardServer()
        asyncio.run(server.start())
    except KeyboardInterrupt:
        print("\n👋 Server kapatılıyor...")
        sys.exit(0)

