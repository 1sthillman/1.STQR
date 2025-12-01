@echo off
echo ==================================
echo   QRMaster Kolay Kurulum
echo ==================================
echo.

echo 1. Backend bagimliliklari yukleniyor...
npm install express mysql2 cors dotenv multer bcryptjs jsonwebtoken express-validator compression helmet

echo.
echo 2. Nodemon yukleniyor...
npm install -g nodemon

echo.
echo 3. Mobil ayarlari yapiliyor...
node mobil-ayarlar.cjs

echo 4. Veritabani olusturuluyor...
node scripts/init-database.js

echo.
echo 5. Frontend bagimliliklari yukleniyor...
npm install

echo.
echo ==================================
echo   Kurulum Tamamlandi!
echo ==================================
echo.
echo Backend baslatmak icin: nodemon server.js
echo Frontend baslatmak icin: npm run dev
echo.
pause
