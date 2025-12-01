#!/usr/bin/env node

/**
 * Capacitor sync sonrası custom plugin'leri korur
 * Bu script capacitor.plugins.json'a custom plugin'leri ekler
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const pluginJsonPath = path.join(__dirname, '../android/app/src/main/assets/capacitor.plugins.json');

const customPlugins = [
  {
    pkg: 'floating-qr-scanner',
    classpath: 'com.qrmaster.app.FloatingQRPlugin'
  },
  {
    pkg: 'ocr-scanner',
    classpath: 'com.qrmaster.app.OCRPlugin'
  },
  {
    pkg: 'world-scanner',
    classpath: 'com.qrmaster.app.WorldScannerPlugin'
  }
];

try {
  // Dosyayı oku
  const content = fs.readFileSync(pluginJsonPath, 'utf8');
  const plugins = JSON.parse(content);
  
  // Her custom plugin için kontrol et
  customPlugins.forEach(customPlugin => {
    const exists = plugins.some(p => p.pkg === customPlugin.pkg);
    
    if (!exists) {
      console.log(`🔧 ${customPlugin.pkg} plugin ekleniyor...`);
      plugins.push(customPlugin);
      console.log(`✅ ${customPlugin.pkg} plugin eklendi!`);
    } else {
      console.log(`✅ ${customPlugin.pkg} plugin zaten mevcut`);
    }
  });
  
  // Dosyayı yaz
  fs.writeFileSync(pluginJsonPath, JSON.stringify(plugins, null, '\t'));
  console.log('🎉 Tüm plugin\'ler hazır!');
} catch (error) {
  console.error('❌ Hata:', error.message);
  process.exit(1);
}

