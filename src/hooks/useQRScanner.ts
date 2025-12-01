import { useState, useRef, useCallback, useEffect } from 'react';
import jsQR from 'jsqr';
import { detectQRType } from '../utils/qr';
import { ScanResult } from '../types';

interface UseQRScannerOptions {
  onScan?: (result: ScanResult) => void;
  onError?: (error: Error) => void;
  continuous?: boolean;
}

export function useQRScanner(options: UseQRScannerOptions = {}) {
  const [scanning, setScanning] = useState(false);
  const [result, setResult] = useState<ScanResult | null>(null);
  const [error, setError] = useState<Error | null>(null);
  
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const scanningRef = useRef(false);
  const animationRef = useRef<number>();

  const stopScanning = useCallback(() => {
    scanningRef.current = false;
    setScanning(false);

    if (animationRef.current) {
      cancelAnimationFrame(animationRef.current);
    }

    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }

    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
  }, []);

  const scanFrame = useCallback(() => {
    if (!scanningRef.current) return;

    const video = videoRef.current;
    const canvas = canvasRef.current;

    if (video && canvas && video.readyState === video.HAVE_ENOUGH_DATA) {
      const ctx = canvas.getContext('2d');
      if (ctx) {
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const code = jsQR(imageData.data, imageData.width, imageData.height);

        if (code) {
          const scanResult: ScanResult = {
            data: code.data,
            type: detectQRType(code.data),
            timestamp: new Date().toISOString(),
          };

          setResult(scanResult);
          options.onScan?.(scanResult);

          if (!options.continuous) {
            stopScanning();
            return;
          }
        }
      }
    }

    animationRef.current = requestAnimationFrame(scanFrame);
  }, [options, stopScanning]);

  const startScanning = useCallback(async () => {
    try {
      setError(null);
      setResult(null);

      // Önce var olan stream'i temizleyelim
      if (streamRef.current) {
        streamRef.current.getTracks().forEach(track => track.stop());
        streamRef.current = null;
      }
      if (videoRef.current) {
        videoRef.current.srcObject = null;
      }

      console.log('🎥 Kamera erişimi isteniyor...');

      const constraints = {
        video: {
          facingMode: 'environment',
          width: { ideal: 1280 },
          height: { ideal: 720 },
          // iOS için ek parametreler
          aspectRatio: { ideal: 1 },
        },
        // Ses izni istenmesin
        audio: false
      };

      console.log('Constraints:', constraints);
      
      // ✅ Kamera erişimi
      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      streamRef.current = stream;
      
      console.log('✅ Kamera erişimi sağlandı, stream hazır');

      if (videoRef.current) {
        // Video elementine stream'i bağla
        videoRef.current.srcObject = stream;
        videoRef.current.setAttribute('autoplay', '');
        videoRef.current.setAttribute('muted', '');
        videoRef.current.setAttribute('playsinline', '');
        
        console.log('🎬 Video oynatma başlatılıyor...');
        
        try {
          await videoRef.current.play();
          console.log('✅ Video oynatma başlatıldı');
        } catch (e) {
          console.error('❌ Video oynatılamadı:', e);
          // Manuel oynatma butonu göster
          const playButton = document.createElement('button');
          playButton.innerHTML = '▶️ Kamerayı Başlat';
          playButton.style.cssText = 'position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);z-index:100;padding:12px 24px;background:rgba(0,0,0,0.7);color:white;border:none;border-radius:8px;font-size:16px;cursor:pointer;';
          
          // Butona tıklama olayı ekle
          playButton.onclick = async () => {
            try {
              if (videoRef.current) {
                await videoRef.current.play();
                // Başarılı olursa butonu kaldır
                if (playButton.parentNode) {
                  playButton.parentNode.removeChild(playButton);
                }
              }
            } catch (playErr) {
              console.error('Manuel oynatma hatası:', playErr);
            }
          };
          
          // Butonu ekle
          if (videoRef.current.parentNode) {
            videoRef.current.parentNode.appendChild(playButton);
          }
        }
      }

      setScanning(true);
      scanningRef.current = true;
      scanFrame();
    } catch (err) {
      const error = err as Error;
      setError(error);
      options.onError?.(error);
      stopScanning();
    }
  }, [scanFrame, stopScanning, options]);

  const toggleFlash = useCallback(async (enabled: boolean) => {
    if (streamRef.current) {
      const track = streamRef.current.getVideoTracks()[0];
      const capabilities = track.getCapabilities() as any;

      if (capabilities.torch) {
        try {
          await track.applyConstraints({
            advanced: [{ torch: enabled } as any],
          });
          return true;
        } catch (err) {
          console.error('Flash toggle error:', err);
          return false;
        }
      }
    }
    return false;
  }, []);

  useEffect(() => {
    return () => {
      stopScanning();
    };
  }, [stopScanning]);

  return {
    scanning,
    result,
    error,
    videoRef,
    canvasRef,
    startScanning,
    stopScanning,
    toggleFlash,
  };
}







