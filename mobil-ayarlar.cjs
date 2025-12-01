// ============================================
// 📱 MOBİL AYARLARI - OTOMATIK YAPILANDIRMA
// ============================================

const os = require('os');
const fs = require('fs');
const path = require('path');

console.log('🔧 Mobil ayarları yapılandırılıyor...');

// Get local IP address
function getLocalIP() {
  const interfaces = os.networkInterfaces();
  
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      // Skip internal and non-IPv4 addresses
      if (iface.family === 'IPv4' && !iface.internal) {
        return iface.address;
      }
    }
  }
  
  return 'localhost';
}

const localIP = getLocalIP();
console.log(`📱 Yerel IP adresiniz: ${localIP}`);

// Backend için .env dosyası oluştur
const backendEnv = `# MySQL Database Configuration
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=
DB_NAME=qrmaster_db
DB_PORT=3306

# Server Configuration - Mobil için tüm IP'lerden erişim
PORT=5000
NODE_ENV=development
HOST=0.0.0.0

# JWT Secret
JWT_SECRET=qrmaster_super_secret_key_2024

# Upload Configuration
UPLOAD_DIR=uploads
MAX_FILE_SIZE=5242880`;

fs.writeFileSync('.env', backendEnv);
console.log('✅ Backend .env dosyası oluşturuldu');

// Frontend için .env dosyası oluştur
const frontendEnv = `# API URL - Mobil için yerel IP kullan
VITE_API_URL=http://${localIP}:5000/api

# Development ayarları
VITE_HOST=0.0.0.0
VITE_PORT=3000`;

fs.writeFileSync('.env.local', frontendEnv);
console.log('✅ Frontend .env.local dosyası oluşturuldu');

// Vite config'i mobil için güncelle
const viteConfigPath = 'vite.config.ts';
if (fs.existsSync(viteConfigPath)) {
  let viteConfig = fs.readFileSync(viteConfigPath, 'utf8');
  
  // Server host ayarını güncelle
  if (viteConfig.includes('host: true')) {
    viteConfig = viteConfig.replace(
      'host: true',
      "host: '0.0.0.0' // Mobil erişim için"
    );
    
    fs.writeFileSync(viteConfigPath, viteConfig);
    console.log('✅ Vite config mobil için güncellendi');
  }
}

console.log(`
🎉 MOBİL AYARLARI TAMAMLANDI!

📱 Mobil telefonunuzdan erişim için:
   Frontend: http://${localIP}:3000
   Backend:  http://${localIP}:5000/api

🔧 Yapılan ayarlar:
   ✅ Tüm IP adreslerinden erişim açıldı
   ✅ CORS ayarları güncellendi  
   ✅ Mobil-friendly host ayarları yapıldı

📋 Sonraki adımlar:
   1. MySQL kurulu olduğundan emin olun
   2. "kurulum.bat" çalıştırın
   3. "baslatici.bat" ile sunucuları başlatın
   4. Mobil telefonunuzdan ${localIP}:3000 adresine gidin

💡 Not: Bilgisayar ve telefon aynı WiFi ağında olmalı!
`);







































