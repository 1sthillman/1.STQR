import { useState, useRef, useEffect, useCallback, memo } from 'react';
import { Html5Qrcode, Html5QrcodeSupportedFormats } from 'html5-qrcode';

interface AdvancedScannerProps {
  onScan: (code: string, type: 'qr' | 'barcode') => void;
  onClose: () => void;
  mode?: 'qr' | 'barcode';
}

const AdvancedScanner = memo(function AdvancedScanner({ onScan, onClose, mode = 'barcode' }: AdvancedScannerProps) {
  const [scanning, setScanning] = useState(false);
  const [flashOn, setFlashOn] = useState(false);
  const [cameraFacing, setCameraFacing] = useState<'user' | 'environment'>('environment');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const html5QrCode = useRef<Html5Qrcode | null>(null);
  const videoTrack = useRef<MediaStreamTrack | null>(null);
  const processingLock = useRef(false);
  const lastScanTime = useRef(0);
  const lastScan = useRef('');
  const isMountedRef = useRef(false);
  const cleanupInProgressRef = useRef(false);

  // CLEANUP FUNCTION - Tüm kamera kaynaklarını tamamen temizle
  const cleanupCamera = useCallback(async () => {
    if (cleanupInProgressRef.current) {
      console.log('[AdvancedScanner] Cleanup zaten devam ediyor, atlanıyor...');
      return;
    }

    cleanupInProgressRef.current = true;
    console.log('[AdvancedScanner] 🧹 Kamera kaynakları temizleniyor...');

    try {
      // 1. Flash'ı kapat
      if (videoTrack.current) {
        try {
          const capabilities = videoTrack.current.getCapabilities() as any;
          if (capabilities?.torch) {
            await videoTrack.current.applyConstraints({ advanced: [{ torch: false }] as any });
          }
        } catch (e) {
          console.debug('[AdvancedScanner] Flash kapatma hatası:', e);
        }
      }

      // 2. Video track'i durdur
      if (videoTrack.current) {
        try {
          videoTrack.current.stop();
          console.log('[AdvancedScanner] ✅ Video track durduruldu');
        } catch (e) {
          console.debug('[AdvancedScanner] Video track stop hatası:', e);
        }
        videoTrack.current = null;
      }

      // 3. Html5Qrcode'u durdur
      if (html5QrCode.current) {
        try {
          const state = html5QrCode.current.getState?.();
          if (state === 2) { // SCANNING
            await html5QrCode.current.stop();
            console.log('[AdvancedScanner] ✅ Html5Qrcode durduruldu');
          }
        } catch (e) {
          console.debug('[AdvancedScanner] Html5Qrcode stop hatası:', e);
        }
      }

      // 4. DOM'daki video elementlerini temizle
      const videoElements = document.querySelectorAll('#product-qr-reader video');
      videoElements.forEach((video) => {
        try {
          const htmlVideo = video as HTMLVideoElement;
          if (htmlVideo.srcObject) {
            const stream = htmlVideo.srcObject as MediaStream;
            stream.getTracks().forEach(track => {
              try {
                track.stop();
              } catch (_) {}
            });
            htmlVideo.srcObject = null;
          }
          htmlVideo.pause();
          htmlVideo.remove();
        } catch (e) {
          console.debug('[AdvancedScanner] Video element temizleme hatası:', e);
        }
      });

      // 5. State'leri sıfırla
      if (isMountedRef.current) {
        setScanning(false);
        setFlashOn(false);
      }

      processingLock.current = false;
      lastScan.current = '';
      lastScanTime.current = 0;

      console.log('[AdvancedScanner] ✅ Tüm kamera kaynakları temizlendi');
    } finally {
      cleanupInProgressRef.current = false;
    }
  }, []);

  // Flaş toggle
  const toggleFlash = useCallback(async () => {
    if (!videoTrack.current) return;

    const newState = !flashOn;
    setFlashOn(newState);

    try {
      const capabilities = videoTrack.current.getCapabilities() as any;
      if (capabilities?.torch) {
        await videoTrack.current.applyConstraints({
          advanced: [{ torch: newState }] as any
        });
      }
    } catch (error) {
      console.debug('[AdvancedScanner] Flash toggle hatası:', error);
    }
  }, [flashOn]);

  // START SCANNER - Kamerayı başlat
  const startScanner = useCallback(async () => {
    if (!html5QrCode.current || scanning || cleanupInProgressRef.current) {
      console.log('[AdvancedScanner] Başlatma atlandı - scanning:', scanning, 'cleanup:', cleanupInProgressRef.current);
      return;
    }

    console.log('[AdvancedScanner] 🚀 Kamera başlatılıyor...');
    console.log('[AdvancedScanner] html5QrCode instance:', html5QrCode.current);
    console.log('[AdvancedScanner] cameraFacing:', cameraFacing);
    
    setScanning(true);
    setErrorMessage(null);
    processingLock.current = false;

    try {
      const screenWidth = window.innerWidth;
      const screenHeight = window.innerHeight;
      const qrboxSize = Math.min(screenWidth * 0.8, 280);

      const cameraConfig = { facingMode: cameraFacing };
      
      console.log('[AdvancedScanner] Kamera config:', cameraConfig);
      console.log('[AdvancedScanner] QR box size:', qrboxSize);
      console.log('[AdvancedScanner] html5QrCode.start() çağrılıyor...');

      await html5QrCode.current.start(
        cameraConfig,
        {
          fps: 15,
          qrbox: qrboxSize,
          aspectRatio: 1.0,
        },
        (decodedText) => {
          const now = Date.now();
          
          // Debounce ve duplicate kontrolü
          if (processingLock.current || !isMountedRef.current) return;
          if (now - lastScanTime.current < 2000) return;
          if (decodedText === lastScan.current && (now - lastScanTime.current) < 3000) return;

          processingLock.current = true;
          lastScanTime.current = now;
          lastScan.current = decodedText;

          console.log('[AdvancedScanner] ✅ Kod tarandı:', decodedText);

          const type = decodedText.length > 20 || decodedText.includes('http') ? 'qr' : 'barcode';
          onScan(decodedText, type);
          
          setTimeout(() => { processingLock.current = false; }, 500);
        },
        (error) => {
          // NotFoundException normal - sessiz geç
        }
      );

      // Video track'i bul ve kaydet (flash için)
      setTimeout(() => {
        try {
          const videoElement = document.querySelector('#product-qr-reader video') as HTMLVideoElement | null;
          if (videoElement?.srcObject) {
            const stream = videoElement.srcObject as MediaStream;
            const tracks = stream.getVideoTracks();
            if (tracks.length > 0) {
              videoTrack.current = tracks[0];
              console.log('[AdvancedScanner] ✅ Video track bağlandı');
            }
          }
        } catch (e) {
          console.debug('[AdvancedScanner] Video track atama hatası:', e);
        }
      }, 500);

      console.log('[AdvancedScanner] ✅ Kamera başarıyla başlatıldı');
    } catch (error: any) {
      console.error('[AdvancedScanner] ❌ Kamera başlatma hatası:', error);
      
      let userMessage = 'Kamera başlatılamadı.';
      const errorName = error?.name || '';
      
      if (errorName === 'NotAllowedError' || errorName === 'PermissionDeniedError') {
        userMessage = 'Kamera izni reddedildi. Lütfen ayarlardan izin verin.';
      } else if (errorName === 'NotFoundError' || errorName === 'DevicesNotFoundError') {
        userMessage = 'Kamera bulunamadı. Cihazınızda kamera olduğundan emin olun.';
      } else if (errorName === 'NotReadableError' || errorName === 'TrackStartError') {
        userMessage = 'Kamera başka uygulama tarafından kullanılıyor.';
      }

      setErrorMessage(userMessage);
      setScanning(false);
      processingLock.current = false;
    }
  }, [scanning, cameraFacing, onScan]);

  // STOP SCANNER - Kamerayı durdur
  const stopScanner = useCallback(async () => {
    console.log('[AdvancedScanner] 🛑 Kamera durduruluyor...');
    await cleanupCamera();
  }, [cleanupCamera]);

  // Kamera değiştir
  const switchCamera = useCallback(async () => {
    if (!scanning) return;
    
    const newFacing = cameraFacing === 'environment' ? 'user' : 'environment';
    console.log('[AdvancedScanner] 🔄 Kamera değiştiriliyor:', cameraFacing, '->', newFacing);
    
    setCameraFacing(newFacing);
    setErrorMessage(null);
    
    await cleanupCamera();
    setTimeout(() => { startScanner(); }, 400);
  }, [scanning, cameraFacing, cleanupCamera, startScanner]);

  // MOUNT / UNMOUNT - Component lifecycle
  useEffect(() => {
    console.log('[AdvancedScanner] 📱 Component mount ediliyor');
    isMountedRef.current = true;

    // DOM elementinin hazır olmasını bekle
    const initializeScanner = () => {
      const element = document.getElementById('product-qr-reader');
      
      if (!element) {
        console.error('[AdvancedScanner] ❌ #product-qr-reader elementi DOM\'da bulunamadı!');
        setErrorMessage('Tarayıcı başlatılamadı: DOM elementi hazır değil');
        return;
      }

      console.log('[AdvancedScanner] ✅ #product-qr-reader elementi bulundu:', element);

      // Html5Qrcode instance'ı oluştur
      if (!html5QrCode.current) {
        try {
          html5QrCode.current = new Html5Qrcode('product-qr-reader', {
            formatsToSupport: [
              Html5QrcodeSupportedFormats.QR_CODE,
              Html5QrcodeSupportedFormats.EAN_13,
              Html5QrcodeSupportedFormats.EAN_8,
              Html5QrcodeSupportedFormats.CODE_128,
              Html5QrcodeSupportedFormats.CODE_39,
              Html5QrcodeSupportedFormats.UPC_A,
              Html5QrcodeSupportedFormats.UPC_E,
              Html5QrcodeSupportedFormats.CODE_93,
              Html5QrcodeSupportedFormats.CODABAR,
              Html5QrcodeSupportedFormats.ITF,
            ],
            verbose: false,
          });
          console.log('[AdvancedScanner] ✅ Html5Qrcode instance oluşturuldu');
        } catch (err) {
          console.error('[AdvancedScanner] ❌ Html5Qrcode oluşturulamadı:', err);
          setErrorMessage('Tarayıcı başlatılamadı: ' + (err as Error).message);
          return;
        }
      }

      // Kamerayı başlat
      setTimeout(() => {
        startScanner();
      }, 500);
    };

    // DOM hazır olana kadar bekle
    const initTimeout = setTimeout(initializeScanner, 100);

    // CLEANUP - Component unmount
    return () => {
      console.log('[AdvancedScanner] 🔥 Component unmount ediliyor');
      isMountedRef.current = false;
      clearTimeout(initTimeout);

      // Tüm kamera kaynaklarını temizle
      (async () => {
        await cleanupCamera();
        
        // Html5Qrcode instance'ı tamamen temizle
        if (html5QrCode.current) {
          try {
            await html5QrCode.current.clear();
          } catch (e) {
            console.debug('[AdvancedScanner] Clear hatası:', e);
          }
          html5QrCode.current = null;
        }
      })();
    };
  }, [startScanner, cleanupCamera]);

  return (
    <div className="fixed inset-0 bg-black z-[9999] flex flex-col">
      {/* Header */}
      <div className="bg-gradient-to-r from-blue-600 to-purple-600 text-white px-4 py-3 flex items-center justify-between shrink-0 safe-area-top">
        <div>
          <h2 className="text-lg font-bold">Ürün Tara</h2>
          <p className="text-xs opacity-90">QR Kod veya Barkod</p>
        </div>
        <button
          onClick={async () => {
            await stopScanner();
            setTimeout(() => onClose(), 200);
          }}
          className="w-10 h-10 rounded-full bg-white/20 hover:bg-white/30 flex items-center justify-center shrink-0"
        >
          <i className="ri-close-line text-2xl"></i>
        </button>
      </div>

      {/* Kamera - TAM EKRAN */}
      <div className="flex-1 relative overflow-hidden bg-black">
        <div id="product-qr-reader" style={{
          width: '100%',
          height: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}></div>
                
                {/* Tarama Çerçevesi */}
        {scanning && (
                <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
            <div className="relative" style={{ width: 'min(80vw, 280px)', height: 'min(80vw, 280px)' }}>
              <div className="absolute inset-0 border-2 border-blue-400 rounded-2xl"></div>
              <div className="absolute top-0 left-0 w-8 h-8 border-t-4 border-l-4 border-white rounded-tl-xl"></div>
              <div className="absolute top-0 right-0 w-8 h-8 border-t-4 border-r-4 border-white rounded-tr-xl"></div>
              <div className="absolute bottom-0 left-0 w-8 h-8 border-b-4 border-l-4 border-white rounded-bl-xl"></div>
              <div className="absolute bottom-0 right-0 w-8 h-8 border-b-4 border-r-4 border-white rounded-br-xl"></div>
                  </div>
                </div>
        )}

        {/* Loading */}
        {!scanning && (
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="text-white text-center max-w-xs px-4">
              <i className={`ri-${errorMessage ? 'error-warning-line text-red-400' : 'camera-line text-white/70 animate-pulse'} text-6xl mb-4 opacity-80`}></i>
              <p className="font-semibold">
                {errorMessage || 'Kamera hazırlanıyor...'}
              </p>
              {errorMessage && (
                <p className="text-sm text-white/70 mt-2">
                  İzinleri kontrol edip tekrar deneyin. Sorun devam ederse uygulamayı yeniden başlatın.
                </p>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Kontroller */}
      <div className="bg-black/90 backdrop-blur-sm px-4 py-4 shrink-0 safe-area-bottom">
        <div className="flex items-center justify-center gap-3 max-w-md mx-auto">
          {/* Flaş */}
        <button
          onClick={toggleFlash}
            className={`w-14 h-14 rounded-xl ${flashOn ? 'bg-yellow-500' : 'bg-white/20'} hover:bg-white/30 flex items-center justify-center transition-all shrink-0`}
        >
            <i className={`${flashOn ? 'ri-flashlight-fill' : 'ri-flashlight-line'} text-white text-xl`}></i>
        </button>
        
          {/* Durdur / Başlat */}
        <button
            onClick={scanning ? stopScanner : startScanner}
            className={`flex-1 px-6 py-3.5 rounded-xl font-bold text-white flex items-center justify-center gap-2 transition-all ${
              scanning ? 'bg-red-600 hover:bg-red-700' : 'bg-green-600 hover:bg-green-700'
            }`}
          >
            <i className={scanning ? 'ri-stop-circle-line text-xl' : 'ri-play-circle-line text-xl'}></i>
            <span className="text-sm">{scanning ? 'Durdur' : 'Başlat'}</span>
        </button>
        
          {/* Kamera Değiştir */}
        <button
            onClick={switchCamera}
            disabled={!scanning}
            className="w-14 h-14 rounded-xl bg-white/20 hover:bg-white/30 flex items-center justify-center disabled:opacity-50 transition-all shrink-0"
          >
            <i className="ri-camera-switch-line text-white text-xl"></i>
        </button>
        </div>
      </div>

      {/* CSS Override - Video Full Screen */}
      <style>{`
        #product-qr-reader video {
          width: 100% !important;
          height: 100% !important;
          object-fit: cover !important;
          position: absolute !important;
          top: 0 !important;
          left: 0 !important;
        }
        #product-qr-reader > div {
          width: 100% !important;
          height: 100% !important;
        }
      `}</style>
    </div>
  );
});

export default AdvancedScanner;
