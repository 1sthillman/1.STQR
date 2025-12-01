@echo off
cls
echo ================================================
echo    APK YUKLEME - Debug Build
echo ================================================
echo.

echo [1] Telefon bagli mi kontrol ediliyor...
adb devices
echo.

echo [2] Build yapiliyor...
cd android
call gradlew.bat assembleDebug
cd ..

echo.
echo [3] APK yukleniyor...
adb install -r android\app\build\outputs\apk\debug\app-debug.apk

echo.
echo ================================================
echo    TAMAMLANDI!
echo ================================================
echo.
echo Telefonda QKeyboard uygulamasini ac.
echo.
pause







