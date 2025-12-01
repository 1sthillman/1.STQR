import { useRef, useState, useEffect } from 'react';
import Quagga from '@ericblade/quagga2';

interface UseBarcodeProps {
  onScan?: (result: string) => void;
  onError?: (error: any) => void;
  onFlashFeedback?: () => void; // ⚡ Flaş feedback için
}

export function useBarcodeScanner({ onScan, onError, onFlashFeedback }: UseBarcodeProps = {}) {
  const scannerRef = useRef<HTMLDivElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const lastResult = useRef('');
  const confidenceCount = useRef(0);
  const pendingCode = useRef('');

  // EAN/UPC Checksum Validasyonu
  const isValidBarcode = (code: string): boolean => {
    if (code.length !== 8 && code.length !== 12 && code.length !== 13 && code.length !== 14) {
      return false;
    }

    const digits = code.split('').map(Number);
    const checkDigit = digits.pop()!;
    
    let sum = 0;
    digits.reverse().forEach((digit, index) => {
      sum += digit * (index % 2 === 0 ? 3 : 1);
    });
    
    const calculatedCheck = (10 - (sum % 10)) % 10;
    return calculatedCheck === checkDigit;
  };

  // Tarayıcıyı Başlat
  const startScanner = (element: HTMLDivElement) => {
    console.log('🔵 useBarcodeScanner.startScanner() ÇAĞRILDI');
    console.log('📦 Element:', element);
    
    if (!element) {
      console.error('❌ Element null!');
      return;
    }
    
    scannerRef.current = element;
    setIsScanning(true);
    setError(null);

    console.log('🚀 Quagga.init() BAŞLATILIYOR...');
    Quagga.init({
      inputStream: {
        name: "Live",
        type: "LiveStream",
        target: element,
        constraints: {
          facingMode: "environment",
          width: { ideal: 640 },     // ⚡ MAKSİMUM HIZ!
          height: { ideal: 480 },    // ⚡ MAKSİMUM HIZ!
          frameRate: { ideal: 30 }   // ⚡ YÜKSEK FPS!
        },
      },
      locator: {
        patchSize: "large", // LARGE - Daha iyi barkod tespiti
        halfSample: false, // FALSE - Tam çözünürlük, daha doğru
        willReadFrequently: true // Canvas2D performans uyarısını önle
      },
      numOfWorkers: 0, // ⚡ 0 = MAKSİMUM HIZ (main thread'de çalış)
      frequency: 30, // ⚡ 30 FPS - MAKSİMUM HIZ!
      decoder: {
        readers: [
          "ean_reader",        // EAN-13/EAN-8 (en yaygın) - ÖNCELİK
          "ean_8_reader",
          "code_128_reader",   // Code 128
          "upc_reader",        // UPC-A
          "upc_e_reader",      // UPC-E
          "code_39_reader",    // Code 39
          "code_93_reader",    // Code 93
          "codabar_reader",    // Codabar
          "i2of5_reader"       // Interleaved 2 of 5
        ],
        multiple: false,
        debug: {
          drawBoundingBox: false, // Performans için kapat
          showFrequency: false,
          drawScanline: false,
          showPattern: false
        }
      },
      locate: true
    }, (err: any) => {
      if (err) {
        console.error("❌ QUAGGA INIT HATASI:", err);
        console.error("Hata detayı:", err.message || err);
        setError(`Barkod tarayıcı başlatılamadı: ${err.message || err}`);
        setIsScanning(false);
        if (onError) onError(err);
        return;
      }

      console.log("✅ QUAGGA INIT BAŞARILI!");
      console.log("🚀 Quagga.start() ÇAĞRILIYOR...");
      
      Quagga.start();
      console.log("✅ QUAGGA BAŞLATILDI!");
      
      // Stream'i al (flaş için)
      setTimeout(() => {
        const videoElement = element.querySelector('video');
        if (videoElement && videoElement.srcObject) {
          streamRef.current = videoElement.srcObject as MediaStream;
          if (import.meta.env.DEV) {
            console.log('✅ Barkod stream alındı (flaş için)');
          }
        }
      }, 500);

      // Tarama sonuçlarını dinle - CONFIDENCE-BASED + VALIDATION
      Quagga.onDetected((result: any) => {
        console.log('📊 QUAGGA DETECTION EVENT:', result?.codeResult?.code);
        
        if (result && result.codeResult && result.codeResult.code) {
          // ❌ VALİDASYON 0: undefined check
          const rawCode = result.codeResult.code;
          if (!rawCode || rawCode === 'undefined' || typeof rawCode !== 'string') {
            console.error('❌ Geçersiz barkod verisi:', rawCode);
            return;
          }
          
          const code = rawCode.trim();
          
          // Boş string kontrolü
          if (!code || code === '') {
            console.error('❌ Boş barkod:', code);
            return;
          }
          
          // ✅ DOĞRULAMA 1: Minimum uzunluk (8+ karakter - gerçek barkodlar)
          if (code.length < 8 || code.length > 18) {
            console.log('❌ Geçersiz uzunluk, reddedildi:', code, 'Uzunluk:', code.length);
            return;
          }
          
          // ✅ DOĞRULAMA 2: Sadece RAKAM (gerçek barkodlar)
          if (!/^\d+$/.test(code)) {
            console.log('❌ Sadece rakam olmalı, reddedildi:', code);
            return;
          }
          
          // ⚡ CHECKSUM KONTROLÜ YOK - MAKSİMUM HIZ!
          
          // ⚡ CONFIDENCE SCORE - ÇOK TOLERANSLI!
          const confidence = result.codeResult.decodedCodes?.reduce((acc: number, c: any) => {
            return acc + (c.error || 0);
          }, 0) / (result.codeResult.decodedCodes?.length || 1);
          
          // ⚡ ÇOK TOLERANSLI - MAKSİMUM HIZ (0.8)
          if (confidence > 0.8) { // 0.5 → 0.8 (çok daha toleranslı)
            console.log('❌ Çok düşük confidence, reddedildi:', code, confidence);
            return;
          }
          
          // ⚡ CONFIDENCE YOK - ANINDA KABUL! MAKSİMUM HIZ!
          // Son sonucu kontrol et (aynı barkodu tekrar okumamak için)
          if (code === lastResult.current) {
            return;
          }
          
          // ✅ ANINDA KABUL! Sonucu sakla
          lastResult.current = code;
          console.log('⚡ ANINDA BARKOD KABUL:', code);
          
          // ⚡ FLAŞ FEEDBACK - BARKOD OKUNDU!
          if (onFlashFeedback) {
            onFlashFeedback();
          }
          
          // Çağırana bildir
          if (onScan) onScan(code);
          
          // 400ms sonra sıfırla
          setTimeout(() => {
            lastResult.current = '';
          }, 400);
        }
      });
    });
  };

  // Tarayıcıyı Durdur
  const stopScanner = () => {
    if (isScanning) {
      try {
        Quagga.stop();
        
        // Stream'i manuel olarak da durdur
        if (streamRef.current) {
          streamRef.current.getTracks().forEach(track => {
            track.stop();
            console.log('🛑 Barkod track durduruldu:', track.label);
          });
          streamRef.current = null;
        }
        
        setIsScanning(false);
        lastResult.current = '';
        confidenceCount.current = 0;
        pendingCode.current = '';
      } catch (error) {
        console.error('Barkod tarayıcı durdurma hatası:', error);
      }
    }
  };
  
  // Component kaldırıldığında temizle
  useEffect(() => {
    return () => {
      stopScanner();
    };
  }, []);

  // Stream'i al (flaş için)
  const getStream = () => streamRef.current;

  return {
    startScanner,
    stopScanner,
    isScanning,
    error,
    getStream,
  };
}

