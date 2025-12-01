@echo off
cls
echo ================================================
echo    QKEYBOARD SCREEN STREAMING SERVER
echo ================================================
echo.
echo [1] Python server'i kapatiyoruz...
taskkill /F /IM python.exe 2>nul
timeout /t 2 /nobreak >nul

echo.
echo [2] Ekran streaming server'ini baslatiyoruz...
echo.
python qkeyboard_server_screen.py

pause







