/**
 * 🚀 ZXing Scanner Hook - ULTRA FAST & ACCURATE
 * Hem QR hem Barkod için tek çözüm
 * jsQR + Quagga2'den 10X daha hızlı ve doğru!
 */

import { useRef, useEffect, useState, useCallback } from 'react';
import { BrowserMultiFormatReader, DecodeHintType, BarcodeFormat } from '@zxing/library';

interface UseZXingScannerProps {
  onScan: (code: string, format: string) => void;
  onError?: (error: any) => void;
  videoElement: HTMLVideoElement | null;
  enabled: boolean;
}

export function useZXingScanner({ onScan, onError, videoElement, enabled }: UseZXingScannerProps) {
  const readerRef = useRef<BrowserMultiFormatReader | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const lastResult = useRef('');
  const lastScanTime = useRef(0);

  // Reader'ı başlat
  useEffect(() => {
    if (!readerRef.current) {
      console.log('🚀 ZXing Reader oluşturuluyor...');
      
      // Hints - format optimizasyonu
      const hints = new Map();
      
      // TÜM FORMATLAR - QR + BARKOD
      const formats = [
        BarcodeFormat.QR_CODE,      // QR
        BarcodeFormat.EAN_13,       // En yaygın barkod
        BarcodeFormat.EAN_8,
        BarcodeFormat.CODE_128,
        BarcodeFormat.CODE_39,
        BarcodeFormat.UPC_A,
        BarcodeFormat.UPC_E,
        BarcodeFormat.ITF,
        BarcodeFormat.CODABAR,
        BarcodeFormat.DATA_MATRIX,
        BarcodeFormat.AZTEC,
      ];
      
      hints.set(DecodeHintType.POSSIBLE_FORMATS, formats);
      hints.set(DecodeHintType.TRY_HARDER, false); // FALSE = HIZLI MOD!
      
      readerRef.current = new BrowserMultiFormatReader(hints);
      console.log('✅ ZXing Reader hazır!');
    }

    return () => {
      if (readerRef.current) {
        readerRef.current.reset();
        console.log('🛑 ZXing Reader temizlendi');
      }
    };
  }, []);

  // Tarama fonksiyonu
  const startScanning = useCallback(async () => {
    if (!enabled || !videoElement || !readerRef.current || isScanning) {
      return;
    }

    console.log('🎯 ZXing tarama başlatılıyor...');
    setIsScanning(true);

    try {
      // Sürekli tarama loop'u
      const decode = async () => {
        if (!enabled || !videoElement) return;

        try {
          const result = await readerRef.current!.decodeFromVideoElement(videoElement);
          
          if (result) {
            const code = result.getText();
            const format = result.getBarcodeFormat();
            const now = Date.now();

            // Debounce - aynı kodu 500ms içinde tekrar okuma
            if (code === lastResult.current && (now - lastScanTime.current) < 500) {
              requestAnimationFrame(decode);
              return;
            }

            lastResult.current = code;
            lastScanTime.current = now;

            console.log('🎉 ZXING OKUMA:', code, 'Format:', format);
            onScan(code, String(format));

            // 500ms sonra tekrar okuyabilir
            setTimeout(() => {
              lastResult.current = '';
            }, 500);
          }
        } catch (error: any) {
          // NotFoundException normal - kod bulunamadı demek
          if (error.name !== 'NotFoundException' && error.message !== 'No MultiFormat Readers were able to detect the code.') {
            console.error('ZXing decode hatası:', error);
          }
        }

        // Sürekli tara
        if (enabled && videoElement) {
          requestAnimationFrame(decode);
        }
      };

      // İlk decode'u başlat
      decode();
    } catch (error) {
      console.error('❌ ZXing başlatma hatası:', error);
      if (onError) onError(error);
      setIsScanning(false);
    }
  }, [enabled, videoElement, isScanning, onScan, onError]);

  // Taramayı durdur
  const stopScanning = useCallback(() => {
    if (readerRef.current) {
      console.log('🛑 ZXing tarama durduruluyor...');
      readerRef.current.reset();
      setIsScanning(false);
      lastResult.current = '';
      lastScanTime.current = 0;
    }
  }, []);

  // enabled veya videoElement değiştiğinde taramayı yönet
  useEffect(() => {
    if (enabled && videoElement) {
      startScanning();
    } else {
      stopScanning();
    }

    return () => {
      stopScanning();
    };
  }, [enabled, videoElement, startScanning, stopScanning]);

  return {
    isScanning,
    stopScanning
  };
}

