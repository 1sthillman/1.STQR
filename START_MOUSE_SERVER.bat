@echo off
echo ========================================
echo   QKeyboard WiFi Mouse Server
echo ========================================
echo.
echo [*] Python kontrolu...
python --version >nul 2>&1
if errorlevel 1 (
    echo [!] Python bulunamadi!
    echo [!] Python 3.10+ yukleyin: https://python.org
    pause
    exit /b 1
)

echo [*] Gerekli kutuphaneler yukleniyor...
pip install pyautogui qrcode[pil] pillow >nul 2>&1

echo.
echo ========================================
echo   SERVER BASLATILIYOR...
echo ========================================
echo.
echo [DIKKAT] Windows Firewall uyarisi cikabilir
echo          - "Izin ver" butonuna tiklayin!
echo.
python qkeyboard_server.py

pause







