@echo off
echo ================================================
echo    SCREEN STREAMING TEST
echo ================================================
echo.

echo [1] Python server'i kapatiyoruz...
taskkill /F /IM python.exe 2>nul
timeout /t 2 >nul

echo.
echo [2] Screen server'i baslatiyoruz...
start "QKeyboard Screen Server" python qkeyboard_server_screen.py

echo.
echo [3] Test URL'leri:
timeout /t 3 >nul

for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4"') do (
    for /f "tokens=1" %%b in ("%%a") do (
        echo    HTTP Stream: http://%%b:58081/screen
        echo    Enable:      http://%%b:58081/screen/enable
        echo    Disable:     http://%%b:58081/screen/disable
    )
)

echo.
echo [4] Tarayicida test et:
echo    Chrome'da ac: http://localhost:58081/screen
echo.
echo [5] curl ile test et:
curl -X POST http://localhost:58081/screen/enable
echo.
echo.
echo ================================================
echo Server calisiyor! Tarayicida test edin.
echo ================================================
pause







