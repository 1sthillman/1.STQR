import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import OCRScanner, { OCRScannedEvent } from '../../plugins/OCRScanner';

export default function FloatingOCRPage() {
  const navigate = useNavigate();
  const [ocrRunning, setOcrRunning] = useState(false);
  const [overlayPermission, setOverlayPermission] = useState(false);
  const [cameraPermission, setCameraPermission] = useState(false);
  const [accessibilityPermission, setAccessibilityPermission] = useState(false);
  const [scannedHistory, setScannedHistory] = useState<OCRScannedEvent[]>([]);
  const [checkingPermissions, setCheckingPermissions] = useState(false);

  const generateEventId = () => {
    if (typeof globalThis !== 'undefined' && (globalThis as any).crypto?.randomUUID) {
      return (globalThis as any).crypto.randomUUID() as string;
    }
    return `ocr-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  };

  // İzinleri kontrol et
  const checkPermissions = async () => {
    try {
      setCheckingPermissions(true);
      console.log('🔍 İzinler kontrol ediliyor...');
      
      const overlayResult = await OCRScanner.checkOverlayPermission();
      setOverlayPermission(overlayResult.granted);
      
      const cameraResult = await OCRScanner.checkCameraPermission();
      setCameraPermission(cameraResult.granted);
      
      const accessibilityResult = await OCRScanner.checkAccessibilityPermission();
      setAccessibilityPermission(accessibilityResult.granted);
      
      console.log('📋 İzin durumu:', { 
        overlay: overlayResult.granted,
        camera: cameraResult.granted,
        accessibility: accessibilityResult.granted 
      });
    } catch (error) {
      console.error('❌ İzin kontrol hatası:', error);
    } finally {
      setCheckingPermissions(false);
    }
  };

  // Sayfa yüklendiğinde izinleri kontrol et
  useEffect(() => {
    checkPermissions();
    
    // LocalStorage'dan geçmişi yükle
    const savedHistory = localStorage.getItem('floating-ocr-history');
    if (savedHistory) {
      try {
        const parsed: OCRScannedEvent[] = JSON.parse(savedHistory).map((item: OCRScannedEvent) => ({
          ...item,
          timestamp: item.timestamp || Date.now(),
          confidence: item.confidence ?? 0,
          lineCount: item.lineCount ?? undefined,
          wordCount: item.wordCount ?? undefined,
          charCount: item.charCount ?? undefined,
          turkishCharCount: item.turkishCharCount ?? undefined,
          hasTurkish: item.hasTurkish ?? undefined,
          autoFillAttempted: item.autoFillAttempted ?? false,
          autoFillSuccess: item.autoFillSuccess ?? false,
          source: item.source ?? 'history'
        }));
        setScannedHistory(parsed);
      } catch (e) {
        console.error('❌ Geçmiş yükleme hatası:', e);
      }
    }
    
    // Visibility change event (arka plandan dönünce)
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) {
        console.log('👀 Sayfa görünür oldu - izinler kontrol ediliyor');
        checkPermissions();
      }
    });
    
    // Window focus event
    window.addEventListener('focus', () => {
      console.log('🔄 Sayfa focus aldı - izinler kontrol ediliyor');
      checkPermissions();
    });
    
    // OCR event listener kur
    setupOCRListener();
    
    return () => {
      OCRScanner.removeAllListeners();
    };
  }, []);

  const setupOCRListener = () => {
    OCRScanner.addListener('textScanned', (event: OCRScannedEvent) => {
      console.log('📝 Yazı okundu:', event.text);
      
      const eventId = event.id || (event as any).uuid || generateEventId();
      
      const newItem: OCRScannedEvent = { 
        ...event, 
        id: eventId,
        timestamp: event.timestamp || Date.now(),
        confidence: event.confidence ?? 0,
        lineCount: event.lineCount ?? undefined,
        wordCount: event.wordCount ?? undefined,
        charCount: event.charCount ?? undefined,
        turkishCharCount: event.turkishCharCount ?? undefined,
        hasTurkish: event.hasTurkish ?? undefined,
        autoFillAttempted: event.autoFillAttempted ?? false,
        autoFillSuccess: event.autoFillSuccess ?? false,
        source: event.source ?? 'floating'
      };
      
      console.log('📝 Geçmişe ekleniyor:', newItem);
      
      // History'e ekle - functional update
      setScannedHistory(prev => {
        const updated = [newItem, ...prev].slice(0, 200); // Max 200 kayıt
        console.log('✅ Yeni geçmiş:', updated.length, 'öğe');
        
        // LocalStorage'a kaydet
        localStorage.setItem('floating-ocr-history', JSON.stringify(updated));
        
        return updated;
      });
      
      const toastMessage = newItem.autoFillAttempted
        ? newItem.autoFillSuccess
          ? '✅ Yazı otomatik olarak yazıldı!'
          : '⚠️ Yazı otomatik yazılamadı, panoya kopyalandı'
        : '📋 Yazı panoya kopyalandı';
      
      const toastType: 'success' | 'error' = newItem.autoFillAttempted
        ? (newItem.autoFillSuccess ? 'success' : 'error')
        : 'success';
      
      showToast(toastMessage, toastType);
    });
  };

  const showToast = (message: string, type: 'success' | 'error') => {
    // Basit toast implementasyonu
    const toast = document.createElement('div');
    toast.textContent = message;
    toast.style.cssText = `
      position: fixed;
      top: 20px;
      left: 50%;
      transform: translateX(-50%);
      background: ${type === 'success' ? '#10B981' : '#EF4444'};
      color: white;
      padding: 12px 24px;
      border-radius: 8px;
      z-index: 10000;
      font-weight: bold;
      box-shadow: 0 4px 12px rgba(0,0,0,0.3);
    `;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
  };

  const handleRequestCameraPermission = async () => {
    try {
      console.log('📱 Kamera izni isteniyor...');
      const result = await OCRScanner.requestCameraPermission();
      if (result.granted) {
        showToast('✅ Kamera izni verildi!', 'success');
        setCameraPermission(true);
      }
      setTimeout(() => {
        checkPermissions();
      }, 1500);
    } catch (error) {
      console.error('❌ Kamera izin hatası:', error);
      showToast('❌ Kamera izni alınamadı', 'error');
    }
  };

  const handleRequestOverlayPermission = async () => {
    try {
      console.log('📱 Overlay izni isteniyor...');
      await OCRScanner.requestOverlayPermission();
      showToast('⚠️ Lütfen ayarlardan izni verin ve geri dönün', 'success');
      
      // 2 saniye sonra tekrar kontrol et
      setTimeout(() => {
        checkPermissions();
      }, 2000);
    } catch (error) {
      console.error('❌ İzin isteme hatası:', error);
      showToast('❌ İzin istenemedi', 'error');
    }
  };

  const handleRequestAccessibilityPermission = async () => {
    try {
      console.log('📱 Accessibility izni isteniyor...');
      await OCRScanner.requestAccessibilityPermission();
      showToast('⚠️ "QR Master / 1STQR" servisini açın ve geri dönün', 'success');
      
      // 2 saniye sonra tekrar kontrol et
      setTimeout(() => {
        checkPermissions();
      }, 2000);
    } catch (error) {
      console.error('❌ İzin isteme hatası:', error);
      showToast('❌ İzin istenemedi', 'error');
    }
  };

  const handleStartOCR = async () => {
    try {
      console.log('🚀 OCR başlatılıyor...');
      
      if (!overlayPermission) {
        showToast('❌ Önce overlay iznini verin', 'error');
        return;
      }
      
      if (!cameraPermission) {
        showToast('❌ Kamera izni olmadan başlatılamaz', 'error');
        await handleRequestCameraPermission();
        return;
      }
      
      const result = await OCRScanner.startFloatingOCR();
      
      if (result.success) {
        setOcrRunning(true);
        showToast('✅ OCR Tarayıcı başlatıldı!', 'success');
        console.log('✅ OCR başarıyla başlatıldı');
      } else {
        showToast('❌ OCR başlatılamadı', 'error');
      }
    } catch (error: any) {
      console.error('❌ OCR başlatma hatası:', error);
      showToast(`❌ Hata: ${error.message || 'Bilinmeyen hata'}`, 'error');
    }
  };

  const handleStopOCR = async () => {
    try {
      console.log('🛑 OCR durduruluyor...');
      
      const result = await OCRScanner.stopFloatingOCR();
      
      if (result.success) {
        setOcrRunning(false);
        showToast('✅ OCR Tarayıcı durduruldu', 'success');
        console.log('✅ OCR başarıyla durduruldu');
      } else {
        showToast('❌ OCR durdurulamadı', 'error');
      }
    } catch (error: any) {
      console.error('❌ OCR durdurma hatası:', error);
      showToast(`❌ Hata: ${error.message || 'Bilinmeyen hata'}`, 'error');
    }
  };

  const clearHistory = () => {
    if (confirm('Tüm geçmişi silmek istediğinizden emin misiniz?')) {
      setScannedHistory([]);
      localStorage.removeItem('floating-ocr-history');
      showToast('✅ Geçmiş temizlendi', 'success');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-green-900 to-gray-900 pb-20">
      {/* Header */}
      <div className="bg-gradient-to-r from-green-600 via-emerald-600 to-teal-600 text-white p-6 shadow-2xl">
        <div className="flex items-center justify-between">
          <button
            onClick={() => navigate(-1)}
            className="p-2 hover:bg-white/20 rounded-lg transition-colors"
          >
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <svg className="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            Yazı Tanıma (OCR)
          </h1>
          <button
            onClick={checkPermissions}
            disabled={checkingPermissions}
            className="p-2 hover:bg-white/20 rounded-lg transition-colors disabled:opacity-50"
          >
            <svg className={`w-6 h-6 ${checkingPermissions ? 'animate-spin' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </button>
        </div>
      </div>

      <div className="p-6 space-y-6">
        {/* İzin Durumu */}
        <div className="bg-gradient-to-br from-green-800/50 to-emerald-900/50 backdrop-blur-sm border-2 border-green-500/30 rounded-2xl p-6 shadow-xl">
          <h2 className="text-white text-xl font-bold mb-4 flex items-center gap-2">
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
            İzin Durumu
          </h2>
          
          <div className="space-y-3">
            {/* Overlay Permission */}
            <div className="flex items-center justify-between bg-gray-900/50 p-4 rounded-xl">
              <div className="flex items-center gap-3">
                <div className={`w-3 h-3 rounded-full ${overlayPermission ? 'bg-green-500' : 'bg-red-500'} animate-pulse`} />
                <span className="text-white font-semibold">Overlay İzni</span>
              </div>
              {!overlayPermission && (
                <button
                  onClick={handleRequestOverlayPermission}
                  className="px-4 py-2 bg-gradient-to-r from-yellow-500 to-orange-500 text-white rounded-xl font-semibold shadow-lg shadow-yellow-500/30 active:scale-95 transition-transform"
                >
                  İzin Ver
                </button>
              )}
              {overlayPermission && (
                <span className="text-green-400 font-bold">✓ Verildi</span>
              )}
            </div>

            {/* Camera Permission */}
            <div className="flex items-center justify-between bg-gray-900/50 p-4 rounded-xl">
              <div className="flex items-center gap-3">
                <div className={`w-3 h-3 rounded-full ${cameraPermission ? 'bg-green-500' : 'bg-red-500'} animate-pulse`} />
                <span className="text-white font-semibold">Kamera İzni</span>
              </div>
              {!cameraPermission ? (
                <button
                  onClick={handleRequestCameraPermission}
                  className="px-4 py-2 bg-gradient-to-r from-red-500 to-rose-500 text-white rounded-xl font-semibold shadow-lg shadow-red-500/30 active:scale-95 transition-transform"
                >
                  İzin Ver
                </button>
              ) : (
                <span className="text-green-400 font-bold">✓ Verildi</span>
              )}
            </div>

            {/* Accessibility Permission */}
            <div className="flex items-center justify-between bg-gray-900/50 p-4 rounded-xl">
              <div className="flex items-center gap-3">
                <div className={`w-3 h-3 rounded-full ${accessibilityPermission ? 'bg-green-500' : 'bg-red-500'} animate-pulse`} />
                <span className="text-white font-semibold">Erişilebilirlik İzni</span>
              </div>
              {!accessibilityPermission && (
                <button
                  onClick={handleRequestAccessibilityPermission}
                  className="px-4 py-2 bg-gradient-to-r from-yellow-500 to-orange-500 text-white rounded-xl font-semibold shadow-lg shadow-yellow-500/30 active:scale-95 transition-transform"
                >
                  İzin Ver
                </button>
              )}
              {accessibilityPermission && (
                <span className="text-green-400 font-bold">✓ Verildi</span>
              )}
            </div>
          </div>

          {/* Talimatlar */}
          {(!overlayPermission || !cameraPermission || !accessibilityPermission) && (
            <div className="mt-4 p-4 bg-yellow-500/20 border-2 border-yellow-500/50 rounded-xl space-y-2">
              {!overlayPermission && (
                <p className="text-yellow-200 text-sm font-medium">
                  📱 <strong>Overlay:</strong> "İzin Ver" → Ayarlarda izni etkinleştir → Geri dön
                </p>
              )}
              {!cameraPermission && (
                <p className="text-yellow-200 text-sm font-medium">
                  📸 <strong>Kamera:</strong> Açılan izin penceresinden kameraya erişime "İzin Ver" deyin
                </p>
              )}
              {!accessibilityPermission && (
                <p className="text-yellow-200 text-sm font-medium">
                  ♿ <strong>Erişilebilirlik:</strong> "İzin Ver" → "QR Master / 1STQR" servisini AÇ → Geri dön
                  <br />
                  <span className="text-green-200">✨ WhatsApp, Notes gibi uygulamalara direkt yazma için gerekli!</span>
                </p>
              )}
            </div>
          )}
        </div>

        {/* Kontrol Butonları */}
        <div className="bg-gradient-to-br from-gray-800/50 to-green-900/50 backdrop-blur-sm border-2 border-green-500/30 rounded-2xl p-6 shadow-xl">
          <h2 className="text-white text-xl font-bold mb-4 flex items-center gap-2">
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Kontrol
          </h2>
          
          <div className="space-y-3">
            {!ocrRunning ? (
              <button
                onClick={handleStartOCR}
                disabled={!overlayPermission || !cameraPermission}
                className="w-full px-6 py-4 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-xl font-bold text-lg shadow-lg shadow-green-500/50 active:scale-95 transition-transform disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-3"
              >
                <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                OCR Tarayıcı Başlat
              </button>
            ) : (
              <button
                onClick={handleStopOCR}
                className="w-full px-6 py-4 bg-gradient-to-r from-red-600 to-pink-600 text-white rounded-xl font-bold text-lg shadow-lg shadow-red-500/50 active:scale-95 transition-transform flex items-center justify-center gap-3"
              >
                <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 10a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1v-4z" />
                </svg>
                OCR Tarayıcı Durdur
              </button>
            )}
          </div>

          {ocrRunning && (
            <div className="mt-4 p-4 bg-green-500/20 border-2 border-green-500/50 rounded-xl animate-pulse">
              <p className="text-green-200 text-center font-bold flex items-center justify-center gap-2">
                <span className="w-3 h-3 bg-green-400 rounded-full animate-ping" />
                📝 OCR Tarayıcı Aktif
              </p>
            </div>
          )}
        </div>

        {/* Tarama Geçmişi */}
        {scannedHistory.length > 0 && (
          <div className="bg-gradient-to-br from-gray-800/50 to-green-900/50 backdrop-blur-sm border-2 border-green-500/30 rounded-2xl p-6 shadow-xl">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-white text-xl font-bold flex items-center gap-2">
                <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                Tarama Geçmişi ({scannedHistory.length})
              </h2>
              <button
                onClick={clearHistory}
                className="px-4 py-2 bg-gradient-to-r from-red-600 to-pink-600 text-white rounded-xl font-semibold shadow-lg shadow-red-500/30 active:scale-95 transition-transform text-sm"
              >
                🗑️ Tümünü Sil
              </button>
            </div>

            <div className="space-y-3 max-h-[600px] overflow-y-auto pr-2">
              {scannedHistory.map((item) => (
                <div
                  key={item.id}
                  className="bg-gradient-to-br from-gray-900/70 to-green-900/30 border-2 border-green-500/20 rounded-xl p-4 shadow-lg hover:shadow-green-500/30 hover:border-green-500/40 transition-all"
                >
                  <div className="flex flex-col gap-3">
                    <p className="text-white font-mono text-sm leading-relaxed whitespace-pre-wrap">
                        {item.text}
                      </p>

                    <div className="flex flex-wrap items-center gap-2 text-xs">
                      <span className="px-2 py-1 rounded-md bg-gray-800/50 border border-gray-700/40 text-green-200">
                        ⏰ {new Date(item.timestamp).toLocaleString('tr-TR')}
                      </span>
                      <span className="px-2 py-1 rounded-md bg-gray-800/50 border border-gray-700/40 text-green-200">
                        🌐 Kaynak: {item.source === 'floating' ? 'Floating Panel' : item.source || 'Bilinmiyor'}
                      </span>
                      {typeof item.confidence === 'number' && (
                        <span className="px-2 py-1 rounded-md bg-yellow-500/15 border border-yellow-400/40 text-yellow-200">
                          🎯 Güven: {(item.confidence * 100).toFixed(0)}%
                        </span>
                      )}
                      {typeof item.lineCount === 'number' && (
                        <span className="px-2 py-1 rounded-md bg-blue-500/15 border border-blue-400/40 text-blue-200">
                          📏 Satır: {item.lineCount}
                        </span>
                      )}
                      {typeof item.wordCount === 'number' && (
                        <span className="px-2 py-1 rounded-md bg-indigo-500/15 border border-indigo-400/40 text-indigo-200">
                          🔠 Kelime: {item.wordCount}
                        </span>
                      )}
                      {typeof item.charCount === 'number' && (
                        <span className="px-2 py-1 rounded-md bg-purple-500/15 border border-purple-400/40 text-purple-200">
                          🔡 Karakter: {item.charCount}
                        </span>
                      )}
                      {item.hasTurkish && typeof item.turkishCharCount === 'number' && (
                        <span className="px-2 py-1 rounded-md bg-emerald-500/15 border border-emerald-400/40 text-emerald-200">
                          🇹🇷 Türkçe harf: {item.turkishCharCount}
                        </span>
                      )}
                      {item.autoFillAttempted ? (
                        <span
                          className={`px-2 py-1 rounded-md border ${
                            item.autoFillSuccess
                              ? 'bg-emerald-500/15 border-emerald-400/60 text-emerald-200'
                              : 'bg-rose-500/15 border-rose-400/60 text-rose-200'
                          }`}
                        >
                          {item.autoFillSuccess ? '✅ Otomatik dolduruldu' : '⚠️ Otomatik doldurulamadı'}
                        </span>
                      ) : (
                        <span className="px-2 py-1 rounded-md bg-amber-500/15 border border-amber-400/60 text-amber-200">
                          📋 Panoya kopyalandı
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {scannedHistory.length === 0 && (
          <div className="bg-gradient-to-br from-gray-800/50 to-green-900/50 backdrop-blur-sm border-2 border-green-500/30 rounded-2xl p-12 shadow-xl text-center">
            <svg className="w-24 h-24 mx-auto text-green-500/30 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <p className="text-green-300 text-lg">Henüz tarama yapılmadı</p>
            <p className="text-green-500 text-sm mt-2">OCR tarayıcıyı başlatın ve yazıları okutun</p>
          </div>
        )}
      </div>
    </div>
  );
}

