import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './i18n'
import App from './App.tsx'
import { defineCustomElements } from '@ionic/pwa-elements/loader'
import { BarcodeScanner } from '@capacitor-mlkit/barcode-scanning'

// PWA Elements'i yükle (kamera için gerekli)
defineCustomElements(window);

// Klavyeden QR tarama için global fonksiyon
(window as any).startKeyboardQRScan = async () => {
  try {
    console.log('[Keyboard QR] Tarama başlatılıyor...');
    
    const result = await BarcodeScanner.scan();
    console.log('[Keyboard QR] Sonuç:', result);
    
    if (result && result.barcodes && result.barcodes.length > 0) {
      const barcode = result.barcodes[0].displayValue || result.barcodes[0].rawValue;
      console.log('[Keyboard QR] Barkod:', barcode);
      
      // KeyboardScanActivity'ye Intent URL ile gönder
      setTimeout(() => {
        window.location.href = `intent://keyboard-scan?text=${encodeURIComponent(barcode)}#Intent;scheme=qrmaster;package=com.qrmaster.app;component=com.qrmaster.app/.keyboard.KeyboardScanActivity;end`;
      }, 100);
    }
  } catch (error) {
    console.error('[Keyboard QR] Hata:', error);
  }
};

console.log('[Keyboard QR] Global fonksiyon hazır');

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)



