import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Capacitor } from '@capacitor/core';
import FloatingQRScanner, { QRScannedEvent } from '../../plugins/FloatingQRScanner';
import FloatingQRHistory from '../../components/FloatingQRHistory';

interface Toast {
  message: string;
  type: 'success' | 'error' | 'info' | 'warning';
}

const generateEventId = () => {
  if (typeof globalThis !== 'undefined' && (globalThis as any).crypto?.randomUUID) {
    return (globalThis as any).crypto.randomUUID() as string;
  }
  return `qr-${Date.now()}-${Math.random().toString(16).slice(2)}`;
};

export default function FloatingQRPage() {
  const navigate = useNavigate();
  const [isActive, setIsActive] = useState(false);
  const [hasPermission, setHasPermission] = useState(false);
  const [hasCameraPermission, setHasCameraPermission] = useState(false);
  const [hasAccessibilityPermission, setHasAccessibilityPermission] = useState(false);
  const [scannedHistory, setScannedHistory] = useState<QRScannedEvent[]>([]);
  const [toast, setToast] = useState<Toast | null>(null);
  const [isNative, setIsNative] = useState(false);

  useEffect(() => {
    setIsNative(Capacitor.isNativePlatform());
    
    if (Capacitor.isNativePlatform()) {
      checkPermission();
      setupQRListener();
      loadHistory();
      
      // Sayfa focus aldığında izinleri kontrol et
      const handleVisibilityChange = () => {
        if (!document.hidden) {
          checkPermission();
        }
      };
      
      const handleFocus = () => {
        checkPermission();
      };
      
      document.addEventListener('visibilitychange', handleVisibilityChange);
      window.addEventListener('focus', handleFocus);
      
      return () => {
        document.removeEventListener('visibilitychange', handleVisibilityChange);
        window.removeEventListener('focus', handleFocus);
      };
    }
  }, []);

  const loadHistory = () => {
    const saved = localStorage.getItem('floating-qr-history');
    if (saved) {
      try {
        const parsed: QRScannedEvent[] = JSON.parse(saved).map((item: QRScannedEvent): QRScannedEvent => ({
          ...item,
          timestamp: item.timestamp || Date.now(),
          autoFillAttempted: item.autoFillAttempted ?? false,
          autoFillSuccess: item.autoFillSuccess ?? false,
          source: item.source ?? 'history'
        }));
        setScannedHistory(parsed);
      } catch (e) {
        console.error('History yükleme hatası:', e);
      }
    }
  };

  const saveHistory = (history: QRScannedEvent[]) => {
    localStorage.setItem('floating-qr-history', JSON.stringify(history));
    setScannedHistory(history);
  };

  const checkPermission = async () => {
    try {
      const overlayResult = await FloatingQRScanner.checkOverlayPermission();
      setHasPermission(overlayResult.hasPermission);
      
      const cameraResult = await FloatingQRScanner.checkCameraPermission();
      setHasCameraPermission(cameraResult.granted);
      
      const accessibilityResult = await FloatingQRScanner.checkAccessibilityPermission();
      setHasAccessibilityPermission(accessibilityResult.hasPermission);
    } catch (error) {
      console.error('İzin kontrolü hatası:', error);
    }
  };

  const requestPermission = async () => {
    try {
      console.log('🔍 requestPermission BAŞLADI');
      console.log('📱 Platform:', Capacitor.getPlatform());
      console.log('🔌 Native platform?', Capacitor.isNativePlatform());
      
      const result = await FloatingQRScanner.requestOverlayPermission();
      console.log('✅ requestOverlayPermission SONUÇ:', result);
      
      showToast('✅ Ayarlar açıldı! İzni verin ve geri dönün.', 'info');
      
      // Kullanıcı ayarlardan döndüğünde kontrol et
      setTimeout(() => checkPermission(), 2000);
    } catch (error: any) {
      console.error('❌ İZİN İSTEME HATASI - DETAYLI:', {
        error: error,
        errorType: typeof error,
        errorMessage: error?.message,
        errorStack: error?.stack,
        errorCode: error?.code,
        platform: Capacitor.getPlatform(),
        isNative: Capacitor.isNativePlatform()
      });
      
      showToast(`❌ İzin istenemedi: ${error?.message || JSON.stringify(error)}`, 'error');
    }
  };
  
  const requestCameraPermission = async () => {
    try {
      const result = await FloatingQRScanner.requestCameraPermission();
      if (result.granted) {
        showToast('✅ Kamera izni verildi!', 'success');
        setHasCameraPermission(true);
        setTimeout(() => checkPermission(), 500);
      }
    } catch (error: any) {
      console.error('❌ Kamera izin hatası:', error);
      showToast(`❌ Kamera izni alınamadı: ${error?.message || 'Bilinmeyen hata'}`, 'error');
    }
  };

  const requestAccessibilityPermission = async () => {
    try {
      console.log('🔍 requestAccessibilityPermission BAŞLADI');
      console.log('📱 Platform:', Capacitor.getPlatform());
      
      const result = await FloatingQRScanner.requestAccessibilityPermission();
      console.log('✅ requestAccessibilityPermission SONUÇ:', result);
      
      showToast('✅ Erişilebilirlik ayarları açıldı! Servisi aktif edin.', 'info');
      
      // Kullanıcı ayarlardan döndüğünde kontrol et
      setTimeout(() => checkPermission(), 2000);
    } catch (error: any) {
      console.error('❌ ACCESSIBILITY İZİN HATASI - DETAYLI:', {
        error: error,
        errorType: typeof error,
        errorMessage: error?.message,
        errorStack: error?.stack,
        platform: Capacitor.getPlatform(),
        isNative: Capacitor.isNativePlatform()
      });
      
      showToast(`❌ Ayarlar açılamadı: ${error?.message || JSON.stringify(error)}`, 'error');
    }
  };

  const showToast = useCallback((message: string, type: Toast['type']) => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  }, []);

  const setupQRListener = () => {
    FloatingQRScanner.addListener('qrScanned', (event: QRScannedEvent) => {
      console.log('📸 QR Okundu:', event.qrCode);
      
      const eventId = event.id || (event as any).uuid || generateEventId();
      
      // ID ekle
      const newItem: QRScannedEvent = { 
        ...event, 
        id: eventId,
        timestamp: event.timestamp || Date.now(),
        qrCode: event.qrCode,
        autoFillAttempted: event.autoFillAttempted ?? false,
        autoFillSuccess: event.autoFillSuccess ?? false,
        source: event.source ?? 'floating'
      };
      
      console.log('📝 Geçmişe ekleniyor:', newItem);
      
      // History'e ekle - functional update kullan (closure problemi çözümü)
      setScannedHistory(prev => {
        const updated = [newItem, ...prev].slice(0, 50);
        console.log('✅ Yeni geçmiş:', updated.length, 'öğe');
        
        // LocalStorage'a kaydet
        localStorage.setItem('floating-qr-history', JSON.stringify(updated));
        
        return updated;
      });
      
      const toastMessage = newItem.autoFillAttempted
        ? newItem.autoFillSuccess
          ? '✅ Kod otomatik olarak input alanına işlendi!'
          : '⚠️ Kod otomatik yazılamadı. Panoya kopyalandı.'
        : '📋 Kod panoya kopyalandı.';
      
      const toastType: Toast['type'] = newItem.autoFillAttempted
        ? (newItem.autoFillSuccess ? 'success' : 'warning')
        : 'info';
      
      showToast(toastMessage, toastType);
    });
  };

  const startFloating = async () => {
    if (!hasPermission) {
      showToast('Önce overlay izni vermeniz gerekiyor!', 'warning');
      requestPermission();
      return;
    }
    
    if (!hasCameraPermission) {
      showToast('Kamera izni gerekiyor!', 'warning');
      await requestCameraPermission();
      return;
    }

    try {
      const result = await FloatingQRScanner.startFloatingScanner();
      
      if (result.success) {
        setIsActive(true);
        showToast('🎯 Floating Scanner başlatıldı!', 'success');
      } else {
        showToast(result.message, 'error');
      }
    } catch (error) {
      console.error('Scanner başlatma hatası:', error);
      showToast('Scanner başlatılamadı!', 'error');
    }
  };

  const stopFloating = async () => {
    try {
      const result = await FloatingQRScanner.stopFloatingScanner();
      
      if (result.success) {
        setIsActive(false);
        showToast('⏹️ Scanner durduruldu', 'info');
      }
    } catch (error) {
      console.error('Scanner durdurma hatası:', error);
      showToast('Scanner durdurulamadı!', 'error');
    }
  };

  const handleDelete = (id: string) => {
    const updated = scannedHistory.filter(item => item.id !== id);
    saveHistory(updated);
    showToast('🗑️ Silindi', 'success');
  };

  const handleDeleteAll = () => {
    if (confirm('Tüm geçmişi silmek istediğinizden emin misiniz?')) {
      saveHistory([]);
      showToast('🗑️ Tüm geçmiş silindi', 'success');
    }
  };

  const handleUpdate = (item: QRScannedEvent) => {
    const updated = scannedHistory.map(h => 
      h.id === item.id ? item : h
    );
    saveHistory(updated);
    showToast('✅ Güncellendi', 'success');
  };

  if (!isNative) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-violet-900">
        <div className="bg-gradient-to-r from-purple-600 to-pink-600 text-white py-6 px-6 sticky top-0 z-10 shadow-2xl shadow-purple-500/50">
          <div className="flex items-center gap-4">
            <button onClick={() => navigate('/')} className="p-2 hover:bg-white/20 rounded-xl transition-all">
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div>
              <h1 className="text-2xl font-bold">🎯 Floating QR Scanner</h1>
              <p className="text-sm text-purple-100">Hazır</p>
            </div>
          </div>
        </div>

        <div className="max-w-2xl mx-auto px-6 py-12 text-center">
          <div className="bg-gradient-to-br from-yellow-500/20 to-orange-500/20 backdrop-blur-xl border-2 border-yellow-500/50 rounded-3xl p-8 shadow-2xl shadow-yellow-500/20">
            <svg className="w-20 h-20 mx-auto mb-4 text-yellow-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
            <h2 className="text-2xl font-bold text-white mb-2">
              Sadece Android Cihazlarda Çalışır
            </h2>
            <p className="text-gray-300">
              Floating QR Scanner özelliği sadece Android cihazlarda kullanılabilir.
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-violet-900 pb-20">
      {/* Modern Header */}
      <div className="bg-gradient-to-r from-purple-600 via-pink-600 to-purple-600 text-white py-6 px-6 sticky top-0 z-10 shadow-2xl shadow-purple-500/50 backdrop-blur-lg">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button 
              onClick={() => navigate('/')} 
              className="p-2 hover:bg-white/20 rounded-xl transition-all active:scale-95"
            >
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div>
              <h1 className="text-2xl font-bold flex items-center gap-2">
                <svg className="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                </svg>
                Floating QR Scanner
              </h1>
              <p className="text-sm text-purple-100 flex items-center gap-1">
                {isActive ? (
                  <>
                    <span className="w-2 h-2 bg-green-400 rounded-full animate-pulse"></span>
                    Aktif - Diğer uygulamalarda kullanın!
                  </>
                ) : (
                  '⏸️ Hazır'
                )}
              </p>
            </div>
          </div>
          <button
            onClick={checkPermission}
            className="p-3 bg-white/20 hover:bg-white/30 rounded-xl transition-all active:scale-95 shadow-lg"
            title="İzinleri Yenile"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </button>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-6 py-6 space-y-6">
        {/* İzin Uyarıları - Modern 3D Kartlar */}
        {!hasCameraPermission && (
          <div className="bg-gradient-to-br from-red-500/20 to-rose-500/20 backdrop-blur-xl border border-red-500/50 rounded-3xl p-6 shadow-2xl shadow-red-500/20 transform hover:scale-[1.02] transition-all">
            <div className="flex items-start gap-4">
              <svg className="w-8 h-8 text-red-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 10l4.553-4.553a1 1 0 10-1.414-1.414L13.586 8.586 9 4H4v5l4.586 4.586-4.553 4.553a1 1 0 101.414 1.414L9 14.414 13.586 19H19v-5l-4-4z" />
              </svg>
              <div className="flex-1">
                <h3 className="text-lg font-bold text-white mb-2">
                  Kamera İzni Gerekli
                </h3>
                <p className="text-gray-200 mb-4 leading-relaxed">
                  Floating tarayıcı kamerayı doğrudan kullanır. Kamera izni olmadan tarama başlatılamaz.
                </p>
                <button
                  onClick={requestCameraPermission}
                  className="px-6 py-3 bg-gradient-to-r from-red-600 to-rose-600 text-white rounded-xl font-semibold shadow-xl shadow-red-500/30 active:scale-95 transition-transform flex items-center gap-2"
                >
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 7a2 2 0 012 2m-6 4a4 4 0 01-4-4m9 4h3m-6 4h6m-9 0H6m6-4V5m0 10v4" />
                  </svg>
                  Kamera İznini Ver
                </button>
              </div>
            </div>
          </div>
        )}
        {!hasPermission && (
          <div className="bg-gradient-to-br from-yellow-500/20 to-orange-500/20 backdrop-blur-xl border border-yellow-500/50 rounded-3xl p-6 shadow-2xl shadow-yellow-500/20 transform hover:scale-[1.02] transition-all">
            <div className="flex items-start gap-4">
              <svg className="w-8 h-8 text-yellow-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <div className="flex-1">
                <h3 className="text-lg font-bold text-white mb-2">
                  Overlay İzni Gerekli
                </h3>
                <p className="text-gray-300 mb-4">
                  Diğer uygulamaların üzerinde görünmesi için izin verin
                </p>
                <button
                  onClick={requestPermission}
                  className="px-6 py-3 bg-gradient-to-r from-yellow-600 to-orange-600 text-white rounded-xl font-semibold shadow-xl shadow-yellow-500/30 active:scale-95 transition-transform flex items-center gap-2"
                >
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                  </svg>
                  İzin Ver
                </button>
              </div>
            </div>
          </div>
        )}

        {hasPermission && !hasAccessibilityPermission && (
          <div className="bg-gradient-to-br from-blue-500/20 to-cyan-500/20 backdrop-blur-xl border border-blue-500/50 rounded-3xl p-6 shadow-2xl shadow-blue-500/20 transform hover:scale-[1.02] transition-all">
            <div className="flex items-start gap-4">
              <svg className="w-8 h-8 text-blue-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div className="flex-1">
                <h3 className="text-lg font-bold text-white mb-2">
                  Erişilebilirlik İzni (Opsiyonel)
                </h3>
                <p className="text-gray-300 mb-4">
                  QR kodları otomatik panoya kopyalanır
                </p>
                <button
                  onClick={requestAccessibilityPermission}
                  className="px-6 py-3 bg-gradient-to-r from-blue-600 to-cyan-600 text-white rounded-xl font-semibold shadow-xl shadow-blue-500/30 active:scale-95 transition-transform flex items-center gap-2"
                >
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 5.636l-3.536 3.536m0 5.656l3.536 3.536M9.172 9.172L5.636 5.636m3.536 9.192l-3.536 3.536M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-5 0a4 4 0 11-8 0 4 4 0 018 0z" />
                  </svg>
                  İzin Ver
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Kontrol Paneli - 3D Modern Kart */}
        <div className="bg-gradient-to-br from-purple-900/60 to-pink-900/60 backdrop-blur-2xl rounded-3xl p-8 border border-purple-500/30 shadow-2xl shadow-purple-900/50">
          <div className="flex items-center gap-3 mb-6">
            <svg className="w-8 h-8 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4" />
            </svg>
            <h2 className="text-2xl font-bold text-white">Kontrol Paneli</h2>
          </div>

          {isActive ? (
            <button
              onClick={stopFloating}
              className="w-full px-8 py-5 bg-gradient-to-r from-red-600 to-rose-600 text-white rounded-2xl text-lg font-bold shadow-2xl shadow-red-500/40 active:scale-95 transition-all flex items-center justify-center gap-3"
            >
              <svg className="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 10a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1v-4z" />
              </svg>
              Scanner'ı Durdur
            </button>
          ) : (
            <button
              onClick={startFloating}
              disabled={!hasPermission || !hasCameraPermission}
              className={`w-full px-8 py-5 ${
                hasPermission && hasCameraPermission
                  ? 'bg-gradient-to-r from-green-600 to-emerald-600 shadow-2xl shadow-green-500/40' 
                  : 'bg-gray-700 cursor-not-allowed'
              } text-white rounded-2xl text-lg font-bold active:scale-95 transition-all flex items-center justify-center gap-3`}
            >
              <svg className="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Floating Scanner Başlat
            </button>
          )}

          <div className="mt-6 p-4 bg-gradient-to-r from-purple-500/20 to-pink-500/20 rounded-xl border border-purple-400/30">
            <div className="flex items-start gap-3">
              <svg className="w-5 h-5 text-purple-300 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div className="text-sm text-gray-300 space-y-1">
                <p>📱 Ekranın köşesinde küçük kamera penceresi açılır</p>
                <p>✋ Penceyi sürükleyerek istediğiniz yere taşıyın</p>
                <p>📏 Sağ alt köşeden sürükleyerek boyutlandırın</p>
                <p>📸 Diğer uygulamalarda (WhatsApp, Notlar, vs.) kullanın</p>
              </div>
            </div>
          </div>
        </div>

        {/* Tarama Geçmişi - Modern Component */}
        <div className="bg-gradient-to-br from-gray-900/90 to-purple-900/90 backdrop-blur-2xl rounded-3xl p-6 border border-purple-500/30 shadow-2xl shadow-purple-900/50">
          <div className="flex items-center gap-3 mb-6">
            <svg className="w-8 h-8 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <h2 className="text-2xl font-bold text-white">Tarama Geçmişi</h2>
            <span className="ml-auto px-3 py-1 bg-purple-500/30 text-purple-300 rounded-full text-sm font-semibold">
              {scannedHistory.length} kayıt
            </span>
          </div>

          <FloatingQRHistory
            history={scannedHistory}
            onDelete={handleDelete}
            onDeleteAll={handleDeleteAll}
            onUpdate={handleUpdate}
          />
        </div>
      </div>

      {/* Toast Notification */}
      {toast && (
        <div className="fixed bottom-24 left-1/2 transform -translate-x-1/2 z-50 animate-bounce-in">
          <div className={`px-6 py-4 rounded-2xl shadow-2xl backdrop-blur-xl border ${
            toast.type === 'success' ? 'bg-green-500/90 border-green-400' :
            toast.type === 'error' ? 'bg-red-500/90 border-red-400' :
            toast.type === 'warning' ? 'bg-yellow-500/90 border-yellow-400' :
            'bg-blue-500/90 border-blue-400'
          } text-white font-semibold flex items-center gap-3`}>
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              {toast.type === 'success' && (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              )}
              {toast.type === 'error' && (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              )}
              {toast.type === 'warning' && (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              )}
              {toast.type === 'info' && (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              )}
            </svg>
            {toast.message}
          </div>
        </div>
      )}
    </div>
  );
}
