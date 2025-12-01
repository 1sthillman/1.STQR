@echo off
echo ========================================
echo   Windows Firewall Kurallari
echo ========================================
echo.
echo [*] QKeyboard icin firewall kurallari ekleniyor...
echo.

REM Eski kurallari sil
netsh advfirewall firewall delete rule name="QKeyboard TCP" >nul 2>&1
netsh advfirewall firewall delete rule name="QKeyboard UDP 59090" >nul 2>&1
netsh advfirewall firewall delete rule name="QKeyboard UDP 59091" >nul 2>&1

REM Yeni kurallari ekle
netsh advfirewall firewall add rule name="QKeyboard TCP" dir=in action=allow protocol=TCP localport=58080 profile=any
netsh advfirewall firewall add rule name="QKeyboard UDP 59090" dir=in action=allow protocol=UDP localport=59090 profile=any
netsh advfirewall firewall add rule name="QKeyboard UDP 59091" dir=in action=allow protocol=UDP localport=59091 profile=any

echo.
echo [OK] Firewall kurallari eklendi!
echo.
echo Port 58080 (TCP) - Acik
echo Port 59090 (UDP) - Acik
echo Port 59091 (UDP) - Acik
echo.
echo ========================================
pause







