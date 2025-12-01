@echo off
echo ========================================
echo   WiFi Baglanti Test
echo ========================================
echo.

echo [*] Bilgisayar IP Adresi:
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4"') do echo    %%a

echo.
echo [*] Windows Firewall Durumu:
netsh advfirewall show allprofiles state

echo.
echo [*] Port 58080 (TCP) dinliyor mu:
netstat -an | findstr ":58080"
if errorlevel 1 (
    echo    [!] Port 58080 KAPALI - Server calisiyor mu?
) else (
    echo    [OK] Port 58080 ACIK
)

echo.
echo [*] Port 59090 (UDP) dinliyor mu:
netstat -an | findstr ":59090"
if errorlevel 1 (
    echo    [!] Port 59090 KAPALI - Server calisiyor mu?
) else (
    echo    [OK] Port 59090 ACIK
)

echo.
echo [*] Port 59091 (UDP) dinliyor mu:
netstat -an | findstr ":59091"
if errorlevel 1 (
    echo    [!] Port 59091 KAPALI
) else (
    echo    [OK] Port 59091 ACIK
)

echo.
echo ========================================
echo   Firewall Kurallari Ekleniyor...
echo ========================================
echo.

netsh advfirewall firewall add rule name="QKeyboard TCP" dir=in action=allow protocol=TCP localport=58080
netsh advfirewall firewall add rule name="QKeyboard UDP 59090" dir=in action=allow protocol=UDP localport=59090
netsh advfirewall firewall add rule name="QKeyboard UDP 59091" dir=in action=allow protocol=UDP localport=59091

echo.
echo [OK] Firewall kurallari eklendi!
echo.
echo ========================================
pause







