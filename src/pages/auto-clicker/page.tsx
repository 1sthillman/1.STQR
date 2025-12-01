import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import AutoClicker from '../../plugins/AutoClicker';

export default function AutoClickerPage() {
  const navigate = useNavigate();
  const [isRunning, setIsRunning] = useState(false);
  const [overlayPermission, setOverlayPermission] = useState(false);
  const [accessibilityPermission, setAccessibilityPermission] = useState(false);

  useEffect(() => {
    checkPermissions();
    // Sayfa her görünür olduğunda izinleri kontrol et
    const interval = setInterval(checkPermissions, 1000);
    return () => clearInterval(interval);
  }, []);

  const checkPermissions = async () => {
    try {
      const [overlay, accessibility] = await Promise.all([
        AutoClicker.checkOverlayPermission(),
        AutoClicker.checkAccessibilityPermission(),
      ]);
      
      setOverlayPermission(overlay.granted);
      setAccessibilityPermission(accessibility.granted);
    } catch (error) {
      console.error('İzin kontrolü hatası:', error);
    }
  };

  const handleRequestOverlayPermission = async () => {
    try {
      await AutoClicker.requestOverlayPermission();
    } catch (error: any) {
      console.error('❌ Overlay izni isteği hatası:', error);
    }
  };

  const handleRequestAccessibilityPermission = async () => {
    try {
      await AutoClicker.requestAccessibilityPermission();
    } catch (error: any) {
      console.error('❌ Accessibility izni isteği hatası:', error);
    }
  };

  const handleStart = async () => {
    if (!overlayPermission || !accessibilityPermission) {
      return;
    }

    try {
      await AutoClicker.startService();
      setIsRunning(true);
    } catch (error) {
      console.error('❌ Servis başlatma hatası:', error);
    }
  };

  const handleStop = async () => {
    try {
      await AutoClicker.stopService();
      setIsRunning(false);
    } catch (error) {
      console.error('Servis durdurma hatası:', error);
    }
  };

  const allPermissionsGranted = overlayPermission && accessibilityPermission;

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-purple-900">
      {/* Premium Header */}
      <div className="bg-gradient-to-r from-cyan-600 via-blue-600 to-purple-600 text-white py-6 px-6 shadow-2xl">
        <div className="flex items-center justify-between mb-4">
          <button
            onClick={() => navigate('/')}
            className="flex items-center gap-2 bg-white/10 backdrop-blur-md hover:bg-white/20 px-4 py-2 rounded-xl transition-all"
          >
            <i className="ri-arrow-left-line text-xl"></i>
            <span className="font-semibold">Geri</span>
          </button>
          <div className="w-12 h-12 bg-white/20 backdrop-blur-md rounded-2xl flex items-center justify-center">
            <i className="ri-cursor-line text-2xl"></i>
          </div>
        </div>
        <h1 className="text-3xl font-bold mb-2">🎯 Otomatik Tıklayıcı</h1>
        <p className="text-blue-100 text-sm">Profesyonel Otomatik Tıklama Sistemi</p>
      </div>

      <div className="px-6 pb-24 -mt-4">
        {/* Status Card - Premium */}
        <div className="bg-white/10 backdrop-blur-xl rounded-3xl shadow-2xl p-6 mb-6 border border-white/20">
          <div className="flex items-center justify-center gap-3">
            <div className={`w-3 h-3 rounded-full animate-pulse ${
              isRunning ? 'bg-green-400' : 'bg-gray-400'
            }`}></div>
            <span className={`text-lg font-bold ${
              isRunning ? 'text-green-400' : 'text-gray-300'
            }`}>
              {isRunning ? '🟢 ÇALIŞIYOR' : '⚪ DURDURULDU'}
            </span>
          </div>
        </div>

        {/* Permissions - VIP Style */}
        <div className="bg-white/10 backdrop-blur-xl rounded-3xl shadow-2xl p-6 mb-6 border border-white/20">
          <div className="flex items-center gap-2 mb-5">
            <div className="w-8 h-8 bg-gradient-to-br from-cyan-400 to-blue-500 rounded-lg flex items-center justify-center">
              <i className="ri-shield-check-line text-white text-lg"></i>
            </div>
            <h2 className="text-xl font-bold text-white">İzinler</h2>
          </div>

          {/* Overlay Permission */}
          <div className="mb-4">
            <div className="bg-gradient-to-br from-white/5 to-white/10 rounded-2xl p-5 border border-white/10">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-3">
                  {overlayPermission ? (
                    <div className="w-10 h-10 bg-gradient-to-br from-green-400 to-emerald-500 rounded-xl flex items-center justify-center">
                      <i className="ri-check-line text-white text-xl font-bold"></i>
                    </div>
                  ) : (
                    <div className="w-10 h-10 bg-gradient-to-br from-red-400 to-rose-500 rounded-xl flex items-center justify-center">
                      <i className="ri-close-line text-white text-xl font-bold"></i>
                    </div>
                  )}
                  <div>
                    <h3 className="text-white font-bold text-base">Üstte Görüntüleme</h3>
                    <p className="text-blue-200 text-xs mt-1">
                      Yüzen panel için gerekli
                    </p>
                  </div>
                </div>
              </div>
              {!overlayPermission && (
                <button
                  onClick={handleRequestOverlayPermission}
                  className="w-full bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-600 hover:to-blue-700 text-white font-bold py-3 rounded-xl transition-all shadow-lg"
                >
                  ✓ İzin Ver
                </button>
              )}
            </div>
          </div>

          {/* Accessibility Permission */}
          <div>
            <div className="bg-gradient-to-br from-white/5 to-white/10 rounded-2xl p-5 border border-white/10">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-3">
                  {accessibilityPermission ? (
                    <div className="w-10 h-10 bg-gradient-to-br from-green-400 to-emerald-500 rounded-xl flex items-center justify-center">
                      <i className="ri-check-line text-white text-xl font-bold"></i>
                    </div>
                  ) : (
                    <div className="w-10 h-10 bg-gradient-to-br from-red-400 to-rose-500 rounded-xl flex items-center justify-center">
                      <i className="ri-close-line text-white text-xl font-bold"></i>
                    </div>
                  )}
                  <div>
                    <h3 className="text-white font-bold text-base">Erişilebilirlik</h3>
                    <p className="text-blue-200 text-xs mt-1">
                      Otomatik tıklama için gerekli
                    </p>
                  </div>
                </div>
              </div>
              {!accessibilityPermission && (
                <button
                  onClick={handleRequestAccessibilityPermission}
                  className="w-full bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-600 hover:to-blue-700 text-white font-bold py-3 rounded-xl transition-all shadow-lg"
                >
                  ✓ İzin Ver
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Control Panel - Premium */}
        {allPermissionsGranted && (
          <div className="bg-white/10 backdrop-blur-xl rounded-3xl shadow-2xl p-6 mb-6 border border-white/20">
            <div className="flex items-center gap-2 mb-5">
              <div className="w-8 h-8 bg-gradient-to-br from-purple-400 to-pink-500 rounded-lg flex items-center justify-center">
                <i className="ri-play-circle-line text-white text-lg"></i>
              </div>
              <h2 className="text-xl font-bold text-white">Kontrol</h2>
            </div>
            
            {!isRunning ? (
              <button
                onClick={handleStart}
                className="w-full bg-gradient-to-r from-green-500 via-emerald-500 to-teal-500 hover:from-green-600 hover:via-emerald-600 hover:to-teal-600 text-white font-bold py-5 rounded-2xl transition-all shadow-2xl text-lg"
              >
                <div className="flex items-center justify-center gap-3">
                  <i className="ri-play-fill text-2xl"></i>
                  <span>Başlat</span>
                </div>
              </button>
            ) : (
              <button
                onClick={handleStop}
                className="w-full bg-gradient-to-r from-red-500 via-rose-500 to-pink-500 hover:from-red-600 hover:via-rose-600 hover:to-pink-600 text-white font-bold py-5 rounded-2xl transition-all shadow-2xl text-lg"
              >
                <div className="flex items-center justify-center gap-3">
                  <i className="ri-stop-fill text-2xl"></i>
                  <span>Durdur</span>
                </div>
              </button>
            )}
          </div>
        )}

        {/* Features - VIP */}
        <div className="bg-white/10 backdrop-blur-xl rounded-3xl shadow-2xl p-6 mb-6 border border-white/20">
          <div className="flex items-center gap-2 mb-5">
            <div className="w-8 h-8 bg-gradient-to-br from-yellow-400 to-orange-500 rounded-lg flex items-center justify-center">
              <i className="ri-star-line text-white text-lg"></i>
            </div>
            <h2 className="text-xl font-bold text-white">Özellikler</h2>
          </div>
          
          <div className="space-y-3">
            <div className="flex items-center gap-4 p-4 bg-gradient-to-r from-white/5 to-white/10 rounded-xl border border-white/10">
              <div className="w-12 h-12 bg-gradient-to-br from-cyan-400 to-blue-500 rounded-xl flex items-center justify-center flex-shrink-0">
                <i className="ri-cursor-line text-white text-xl"></i>
              </div>
              <div>
                <h3 className="text-white font-bold">Çoklu Nokta</h3>
                <p className="text-blue-200 text-sm">10'a kadar tıklama noktası</p>
              </div>
            </div>
            
            <div className="flex items-center gap-4 p-4 bg-gradient-to-r from-white/5 to-white/10 rounded-xl border border-white/10">
              <div className="w-12 h-12 bg-gradient-to-br from-purple-400 to-pink-500 rounded-xl flex items-center justify-center flex-shrink-0">
                <i className="ri-flashlight-line text-white text-xl"></i>
              </div>
              <div>
                <h3 className="text-white font-bold">Ayarlanabilir Hız</h3>
                <p className="text-blue-200 text-sm">1-1000 ms arası</p>
              </div>
            </div>
            
            <div className="flex items-center gap-4 p-4 bg-gradient-to-r from-white/5 to-white/10 rounded-xl border border-white/10">
              <div className="w-12 h-12 bg-gradient-to-br from-green-400 to-teal-500 rounded-xl flex items-center justify-center flex-shrink-0">
                <i className="ri-repeat-line text-white text-xl"></i>
              </div>
              <div>
                <h3 className="text-white font-bold">Sonsuz Tekrar</h3>
                <p className="text-blue-200 text-sm">Belirli veya sonsuz</p>
              </div>
            </div>
            
            <div className="flex items-center gap-4 p-4 bg-gradient-to-r from-white/5 to-white/10 rounded-xl border border-white/10">
              <div className="w-12 h-12 bg-gradient-to-br from-orange-400 to-red-500 rounded-xl flex items-center justify-center flex-shrink-0">
                <i className="ri-drag-move-line text-white text-xl"></i>
              </div>
              <div>
                <h3 className="text-white font-bold">Swipe & Pinch</h3>
                <p className="text-blue-200 text-sm">Kaydırma ve yakınlaştırma</p>
              </div>
            </div>
          </div>
        </div>

        {/* Active Info */}
        {isRunning && (
          <div className="bg-gradient-to-r from-blue-500/20 to-purple-500/20 backdrop-blur-xl rounded-2xl p-5 border border-blue-400/30">
            <div className="flex items-start gap-3">
              <div className="w-10 h-10 bg-blue-400/30 rounded-full flex items-center justify-center flex-shrink-0 mt-1">
                <i className="ri-information-line text-blue-200 text-xl"></i>
              </div>
              <div>
                <h3 className="text-blue-100 font-bold mb-1">Yüzen Panel Aktif</h3>
                <p className="text-blue-200 text-sm leading-relaxed">
                  Ekranınızda yüzen kontrol paneli görünüyor. "NOKTA EKLE" butonuna tıklayarak 
                  tıklama noktaları ekleyin, KAYDIRMA/SCROLL/ZOOM işlemleri ekleyin, hızı ayarlayın 
                  ve otomatik tıklamayı başlatın. Paneli küçültmek için minimize butonunu kullanın.
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
