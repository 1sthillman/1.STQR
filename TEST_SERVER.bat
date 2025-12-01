@echo off
echo ========================================
echo   Server Test
echo ========================================
echo.

REM IP adresini göster
echo [*] Bilgisayar IP Adresi:
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4"') do echo %%a

echo.
echo [*] Port 59090 UDP dinliyor mu:
netstat -an | findstr ":59090"

echo.
echo [*] Port 58080 TCP dinliyor mu:
netstat -an | findstr ":58080"

echo.
echo ========================================
pause







