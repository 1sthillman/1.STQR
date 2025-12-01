@echo off
cls
color 0A
echo ================================================
echo    QKEYBOARD - TAM TEST
echo ================================================
echo.

echo [1/5] Eski server kapatiliyor...
taskkill /F /IM python.exe 2>nul
timeout /t 2 /nobreak >nul

echo.
echo [2/5] Firewall kontrol ediliyor...
echo     Port 58080 (TCP) - Mouse
echo     Port 59090 (UDP) - Mouse Movement  
echo     Port 58081 (HTTP) - Screen Streaming
netsh advfirewall firewall show rule name="QKeyboard Mouse" >nul 2>&1
if errorlevel 1 (
    echo     UYARI: Firewall kurallari bulunamadi!
    echo     FIX_FIREWALL.bat'i yonetici olarak calistirin.
) else (
    echo     OK: Firewall kurallari mevcut
)

echo.
echo [3/5] Telefon baglanti kontrol...
adb devices | findstr "device$" >nul
if errorlevel 1 (
    echo     UYARI: Telefon bulunamadi!
    echo     USB ile baglayin veya WiFi ADB kullanin.
) else (
    echo     OK: Telefon bagli
)

echo.
echo [4/5] Server baslatiliyor...
echo.
start "QKeyboard Screen Server" python qkeyboard_server_screen.py

timeout /t 3 /nobreak >nul

echo.
echo [5/5] Test URL'leri:
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4"') do (
    for /f "tokens=1" %%b in ("%%a") do (
        set IP=%%b
        goto :found_ip
    )
)
:found_ip
echo     Mouse:     http://%IP%:58080
echo     Streaming: http://%IP%:58081/screen
echo.

echo ================================================
echo    SERVER HAZIR!
echo ================================================
echo.
echo TELEFONDA:
echo   1. Quick Menu (hamburger) ac
echo   2. WiFi Mouse sec
echo   3. "WiFi ile Bul" tikla
echo   4. PIN gir ve baglan
echo   5. "Ekran Goruntusu: ACIK" tikla
echo.
echo LOGCAT IZLE:
echo   adb logcat -s MouseModeView:D ScreenStreamThread:D
echo.
pause







