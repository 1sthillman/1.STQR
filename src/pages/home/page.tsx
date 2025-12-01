import { Link } from 'react-router-dom';
import { useEffect, useState, useMemo, memo } from 'react';
import Navigation from '../../components/Navigation';
import { useProducts, useScanHistory, useSales } from '../../hooks/useDatabase';
import { KeyboardManager } from '../../plugins/KeyboardManager';
import { Capacitor } from '@capacitor/core';
import { App } from '@capacitor/app';

interface KeyboardStatus {
  enabled: boolean;
  selected: boolean;
}

// Feature Card Component - Memoized
const FeatureCard = memo(({ feature }: { feature: any }) => (
  <Link
    to={feature.link}
    className="group bg-white/10 backdrop-blur-lg rounded-3xl shadow-xl hover:shadow-2xl transition-all duration-200 overflow-hidden border border-white/20 hover:scale-[1.02] hover:border-white/40 will-change-transform"
  >
    <div className={`bg-gradient-to-r ${feature.color} p-6 relative overflow-hidden`}>
      <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full blur-2xl"></div>
      <div className="relative flex items-center gap-4">
        <div className="w-16 h-16 bg-white/20 backdrop-blur-sm rounded-2xl flex items-center justify-center flex-shrink-0 shadow-lg will-change-transform">
          <i className={`${feature.icon} text-4xl text-white`}></i>
        </div>
        <div>
          <h3 className="text-xl font-bold text-white mb-1">{feature.title}</h3>
          <p className="text-white/90 text-sm font-medium">{feature.description}</p>
        </div>
      </div>
    </div>
    <div className="p-4 flex items-center justify-between bg-white/5">
      <span className="text-blue-200 text-sm font-semibold">Başlayın →</span>
      <i className="ri-arrow-right-line text-blue-300 group-hover:text-white group-hover:translate-x-1 transition-all text-xl"></i>
    </div>
  </Link>
));
FeatureCard.displayName = 'FeatureCard';

export default function Home() {
  const { products } = useProducts();
  const { history: scanHistory } = useScanHistory();
  const { getDailyRevenue } = useSales();
  const [stats, setStats] = useState({
    qrCodes: 0,
    scans: 0,
    products: 0,
    dailyRevenue: 0,
  });
  
  const [keyboardStatus, setKeyboardStatus] = useState<KeyboardStatus>({
    enabled: false,
    selected: false,
  });

  // Klavye durumunu kontrol et
  const refreshKeyboardStatus = async () => {
    if (Capacitor.getPlatform() === 'android') {
      try {
        const status = await KeyboardManager.getStatus();
        console.log('Klavye durumu:', status);
        setKeyboardStatus(status);
      } catch (error) {
        console.error('Klavye durumu alınamadı:', error);
      }
    }
  };

  // Component mount olduğunda ve app'e dönüldüğünde kontrol et
  useEffect(() => {
    refreshKeyboardStatus();
    
    // App'e her dönüldüğünde kontrol et
    let listenerHandle: any;
    App.addListener('resume', () => {
      console.log('App resumed - klavye durumu kontrol ediliyor');
      refreshKeyboardStatus();
    }).then(handle => {
      listenerHandle = handle;
    });
    
    return () => {
      if (listenerHandle) {
        listenerHandle.remove();
      }
    };
  }, []);

  const handleOpenKeyboardSettings = async () => {
    if (Capacitor.getPlatform() === 'android') {
      try {
        await KeyboardManager.openInputMethodSettings();
        // Ayarlar açıldı, 1 saniye sonra kontrol et
        setTimeout(refreshKeyboardStatus, 1000);
      } catch (error) {
        console.error('Klavye ayarları açılamadı:', error);
        alert('Klavye ayarları açılamadı. Lütfen Ayarlar > Dil ve Klavye bölümünden manuel olarak etkinleştirin.');
      }
    } else {
      alert('Klavye özelliği sadece Android cihazlarda kullanılabilir.');
    }
  };

  const handleShowKeyboardPicker = async () => {
    if (Capacitor.getPlatform() === 'android') {
      try {
        await KeyboardManager.showKeyboardPicker();
        setTimeout(refreshKeyboardStatus, 500);
      } catch (error) {
        console.error('Klavye seçici açılamadı:', error);
        alert('Klavye seçici açılamadı.');
      }
    } else {
      alert('Klavye özelliği sadece Android cihazlarda kullanılabilir.');
    }
  };

  useEffect(() => {
    setStats({
      qrCodes: 0,
      scans: scanHistory.length,
      products: products.length,
      dailyRevenue: getDailyRevenue(),
    });
  }, [products, scanHistory, getDailyRevenue]); // ✅ sales kaldırıldı - getDailyRevenue içinde zaten var

  // Memoized features - sadece bir kez oluşturulur
  const features = useMemo(() => [
    {
      icon: 'ri-qr-code-line',
      title: 'QR Kod Oluştur',
      description: '9+ farklı türde QR kod',
      color: 'from-blue-500 to-cyan-500',
      link: '/qr-olustur',
    },
    {
      icon: 'ri-scan-line',
      title: 'QR & Barkod Tara',
      description: 'Kamera ile anlık tarama',
      color: 'from-purple-500 to-pink-500',
      link: '/qr-tara',
    },
    {
      icon: 'ri-box-3-line',
      title: 'Ürün Yönetimi',
      description: 'Ürünlerinizi yönetin',
      color: 'from-orange-500 to-red-500',
      link: '/urun-yonetimi',
    },
    {
      icon: 'ri-shopping-cart-line',
      title: 'Akıllı Sepet',
      description: 'Hızlı ödeme sistemi',
      color: 'from-green-500 to-teal-500',
      link: '/akilli-sepet',
    },
    {
      icon: 'ri-map-pin-line',
      title: 'Konum QR',
      description: 'GPS konumlu QR',
      color: 'from-indigo-500 to-purple-500',
      link: '/konum-qr',
    },
    {
      icon: 'ri-picture-in-picture-2-line',
      title: '🎯 Floating QR',
      description: 'Her yerde QR tarama',
      color: 'from-pink-500 to-rose-500',
      link: '/floating-qr',
    },
    {
      icon: 'ri-article-line',
      title: '📝 Yazı Tanıma',
      description: 'Akıllı OCR sistemi',
      color: 'from-green-500 to-emerald-500',
      link: '/floating-ocr',
    },
    {
      icon: 'ri-cursor-line',
      title: '🎯 Auto Clicker',
      description: 'Otomatik tıklama',
      color: 'from-cyan-500 to-blue-600',
      link: '/auto-clicker',
    },
  ], []);


  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-purple-900 smooth-scroll">
      {/* VIP Premium Header */}
      <div className="bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 text-white py-8 px-6 shadow-xl will-change-transform">
        <div className="max-w-6xl mx-auto">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-5xl font-bold mb-3 bg-clip-text text-transparent bg-gradient-to-r from-white to-blue-100">
                1STQR
              </h1>
              <p className="text-blue-100 text-lg">Profesyonel QR & Otomasyon Platformu</p>
            </div>
            <div className="w-20 h-20 bg-white/20 backdrop-blur-lg rounded-3xl flex items-center justify-center shadow-xl border border-white/30 will-change-transform">
              <i className="ri-qr-code-line text-5xl"></i>
            </div>
          </div>
        </div>
      </div>

      {/* Premium Stats - Glassmorphism */}
      <div className="max-w-6xl mx-auto px-6 -mt-8 mb-8">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-white/10 backdrop-blur-lg rounded-3xl shadow-xl p-6 text-center border border-white/20 will-change-transform">
            <div className="w-12 h-12 bg-gradient-to-br from-blue-400 to-cyan-500 rounded-2xl flex items-center justify-center mx-auto mb-3">
              <i className="ri-qr-code-line text-white text-2xl"></i>
            </div>
            <div className="text-3xl font-bold text-white mb-2">{stats.qrCodes}</div>
            <div className="text-blue-200 text-sm font-medium">QR Kod</div>
          </div>
          
          <div className="bg-white/10 backdrop-blur-lg rounded-3xl shadow-xl p-6 text-center border border-white/20 will-change-transform">
            <div className="w-12 h-12 bg-gradient-to-br from-purple-400 to-pink-500 rounded-2xl flex items-center justify-center mx-auto mb-3">
              <i className="ri-scan-line text-white text-2xl"></i>
            </div>
            <div className="text-3xl font-bold text-white mb-2">{stats.scans}</div>
            <div className="text-purple-200 text-sm font-medium">Tarama</div>
          </div>
          
          <div className="bg-white/10 backdrop-blur-lg rounded-3xl shadow-xl p-6 text-center border border-white/20 will-change-transform">
            <div className="w-12 h-12 bg-gradient-to-br from-green-400 to-teal-500 rounded-2xl flex items-center justify-center mx-auto mb-3">
              <i className="ri-box-3-line text-white text-2xl"></i>
            </div>
            <div className="text-3xl font-bold text-white mb-2">{stats.products}</div>
            <div className="text-green-200 text-sm font-medium">Ürün</div>
          </div>
          
          <div className="bg-gradient-to-br from-orange-500 via-red-500 to-pink-500 backdrop-blur-lg rounded-3xl shadow-xl p-6 text-center border border-white/20 will-change-transform">
            <div className="w-12 h-12 bg-white/20 backdrop-blur-md rounded-2xl flex items-center justify-center mx-auto mb-3">
              <i className="ri-money-dollar-circle-line text-white text-2xl"></i>
            </div>
            <div className="text-2xl md:text-3xl font-bold mb-2 text-white">
              ₺{stats.dailyRevenue >= 10000 
                ? (stats.dailyRevenue / 1000).toFixed(1) + 'K' 
                : stats.dailyRevenue.toFixed(2)}
            </div>
            <div className="text-white/90 text-sm font-bold">Günlük Ciro</div>
          </div>
        </div>
      </div>

      {/* ⌨️ TÜRKÇE Q KLAVYE - QR TARAMALI */}
      <div className="max-w-6xl mx-auto px-6 pb-10">
        <div className="bg-gradient-to-br from-blue-500/20 via-cyan-500/20 to-teal-500/20 backdrop-blur-lg rounded-3xl shadow-2xl border border-cyan-400/30 p-6 md:p-8">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
            <div className="flex items-start gap-4">
              <div className="w-14 h-14 bg-gradient-to-br from-blue-500 to-cyan-600 rounded-2xl flex items-center justify-center shadow-lg animate-pulse">
                <i className="ri-keyboard-line text-2xl text-white"></i>
              </div>
              <div>
                <h2 className="text-2xl font-bold text-white mb-2">⌨️ Türkçe Q Klavye</h2>
                <p className="text-white/90 text-sm md:text-base font-medium mb-3">
                  <strong>Tam özellikli klavye + QR tarama!</strong> Emoji, GIF, çıkartma, pano, çeviri ve daha fazlası 🚀
                </p>
                <div className="flex flex-wrap gap-2">
                  <span className="px-3 py-1 bg-blue-500/20 text-blue-200 rounded-full text-xs font-semibold border border-blue-400/50">
                    📷 QR Tarama
                  </span>
                  <span className="px-3 py-1 bg-green-500/20 text-green-200 rounded-full text-xs font-semibold border border-green-400/50">
                    😊 Emoji & GIF
                  </span>
                  <span className="px-3 py-1 bg-purple-500/20 text-purple-200 rounded-full text-xs font-semibold border border-purple-400/50">
                    🌍 Çeviri
                  </span>
                  <span className="px-3 py-1 bg-orange-500/20 text-orange-200 rounded-full text-xs font-semibold border border-orange-400/50">
                    📋 Pano & Daha Fazla
                  </span>
                </div>
              </div>
            </div>

            <div className="flex-shrink-0 flex flex-col gap-2">
              {/* Durum göstergesi */}
              {Capacitor.getPlatform() === 'android' && (
                <div className="flex gap-2 text-sm">
                  {keyboardStatus.enabled ? (
                    <span className="px-3 py-1 bg-green-500/20 text-green-200 rounded-full font-semibold border border-green-400/50">
                      ✅ Etkin
                    </span>
                  ) : (
                    <span className="px-3 py-1 bg-red-500/20 text-red-200 rounded-full font-semibold border border-red-400/50">
                      ❌ Etkin Değil
                    </span>
                  )}
                  {keyboardStatus.selected ? (
                    <span className="px-3 py-1 bg-blue-500/20 text-blue-200 rounded-full font-semibold border border-blue-400/50">
                      🎯 Seçili
                    </span>
                  ) : keyboardStatus.enabled ? (
                    <span className="px-3 py-1 bg-yellow-500/20 text-yellow-200 rounded-full font-semibold border border-yellow-400/50">
                      ⚠️ Seçilmedi
                    </span>
                  ) : null}
                </div>
              )}
              
              {/* Butonlar */}
              <div className="flex gap-2">
                {!keyboardStatus.enabled && (
                  <button
                    onClick={handleOpenKeyboardSettings}
                    className="px-6 py-4 bg-gradient-to-r from-blue-500 to-cyan-500 rounded-2xl text-white font-bold shadow-lg hover:shadow-xl transition-all hover:scale-105"
                  >
                    <i className="ri-settings-3-line mr-2"></i>
                    Etkinleştir
                  </button>
                )}
                
                {keyboardStatus.enabled && !keyboardStatus.selected && (
                  <button
                    onClick={handleShowKeyboardPicker}
                    className="px-6 py-4 bg-gradient-to-r from-green-500 to-teal-500 rounded-2xl text-white font-bold shadow-lg hover:shadow-xl transition-all hover:scale-105"
                  >
                    <i className="ri-keyboard-line mr-2"></i>
                    Klavye Seç
                  </button>
                )}
                
                {keyboardStatus.enabled && keyboardStatus.selected && (
                  <button
                    onClick={refreshKeyboardStatus}
                    className="px-6 py-4 bg-gradient-to-r from-green-500 to-emerald-500 rounded-2xl text-white font-bold shadow-lg hover:shadow-xl transition-all"
                  >
                    <i className="ri-check-line mr-2"></i>
                    Kullanımda ✨
                  </button>
                )}
              </div>
            </div>
          </div>

          <div className="mt-6 grid md:grid-cols-4 gap-4 text-sm">
            <div className="bg-white/5 rounded-2xl p-4 border border-white/10">
              <div className="font-semibold text-white mb-1">📷 QR Tarama</div>
              <p className="text-white/80">Klavyeden direkt QR kodu tara!</p>
            </div>
            <div className="bg-white/5 rounded-2xl p-4 border border-white/10">
              <div className="font-semibold text-white mb-1">😊 Emoji & GIF</div>
              <p className="text-white/80">Binlerce emoji, GIF, çıkartma!</p>
            </div>
            <div className="bg-white/5 rounded-2xl p-4 border border-white/10">
              <div className="font-semibold text-white mb-1">🌍 Çeviri</div>
              <p className="text-white/80">Çok dilli çeviri desteği!</p>
            </div>
            <div className="bg-white/5 rounded-2xl p-4 border border-white/10">
              <div className="font-semibold text-white mb-1">🎨 Temalar</div>
              <p className="text-white/80">Özel temalar ve özelleştirme!</p>
            </div>
          </div>
        </div>
      </div>

      {/* 🎯 FLOATING QR BANNER */}
      <div className="max-w-6xl mx-auto px-6 pb-10">
        <Link to="/floating-qr" className="block bg-gradient-to-br from-purple-500/20 via-pink-500/20 to-red-500/20 backdrop-blur-lg rounded-3xl shadow-2xl border border-purple-400/30 p-6 md:p-8 will-change-transform hover:scale-[1.02] transition-all">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
            <div className="flex items-start gap-4">
              <div className="w-14 h-14 bg-gradient-to-br from-purple-500 to-pink-600 rounded-2xl flex items-center justify-center shadow-lg animate-pulse">
                <i className="ri-qr-scan-line text-2xl text-white"></i>
              </div>
              <div>
                <h2 className="text-2xl font-bold text-white mb-2">🎯 Floating QR Scanner</h2>
                <p className="text-white/90 text-sm md:text-base font-medium mb-3">
                  <strong>Her uygulamada QR tarama!</strong> Floating buton ile QR okut → WhatsApp, Notlar, Chrome her yere otomatik yapıştır 🚀
                </p>
                <div className="flex flex-wrap gap-2">
                  <span className="px-3 py-1 bg-green-500/20 text-green-200 rounded-full text-xs font-semibold border border-green-400/50">
                    ✨ Tüm Uygulamalarda
                  </span>
                  <span className="px-3 py-1 bg-blue-500/20 text-blue-200 rounded-full text-xs font-semibold border border-blue-400/50">
                    ⚡ Otomatik Yapıştır
                  </span>
                  <span className="px-3 py-1 bg-orange-500/20 text-orange-200 rounded-full text-xs font-semibold border border-orange-400/50">
                    🎯 Her Yerde Kullan
                  </span>
                </div>
              </div>
            </div>

            <div className="flex-shrink-0">
              <div className="px-6 py-4 bg-gradient-to-r from-purple-500 to-pink-500 rounded-2xl text-white font-bold shadow-lg hover:shadow-xl transition-all">
                <i className="ri-arrow-right-line mr-2"></i>
                Etkinleştir
              </div>
            </div>
          </div>

          <div className="mt-6 grid md:grid-cols-3 gap-4 text-sm">
            <div className="bg-white/5 rounded-2xl p-4 border border-white/10">
              <div className="font-semibold text-white mb-1">💡 Floating Buton</div>
              <p className="text-white/80">Ekranda sürüklenebilir QR tarama butonu!</p>
            </div>
            <div className="bg-white/5 rounded-2xl p-4 border border-white/10">
              <div className="font-semibold text-white mb-1">🚀 Hızlı Tarama</div>
              <p className="text-white/80">Tek dokunuşla QR tara, sonuç otomatik yapıştırılır!</p>
            </div>
            <div className="bg-white/5 rounded-2xl p-4 border border-white/10">
              <div className="font-semibold text-white mb-1">🎨 Overlay Modu</div>
              <p className="text-white/80">Diğer uygulamaların üzerinde çalışır!</p>
            </div>
          </div>
        </Link>
      </div>

      {/* Premium Features Grid */}
      <div className="max-w-6xl mx-auto px-6 pb-24">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 bg-gradient-to-br from-yellow-400 to-orange-500 rounded-xl flex items-center justify-center">
            <i className="ri-star-line text-white text-xl"></i>
          </div>
          <h2 className="text-3xl font-bold text-white">Özellikler</h2>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {features.map((feature, index) => (
            <FeatureCard key={feature.link} feature={feature} />
          ))}
        </div>

        {/* Quick Actions - Premium */}
        <div className="mt-8 bg-white/10 backdrop-blur-lg rounded-3xl shadow-xl p-6 border border-white/20 will-change-transform">
          <div className="flex items-center gap-3 mb-5">
            <div className="w-10 h-10 bg-gradient-to-br from-green-400 to-teal-500 rounded-xl flex items-center justify-center">
              <i className="ri-flashlight-line text-white text-xl"></i>
            </div>
            <h3 className="text-2xl font-bold text-white">Hızlı İşlemler</h3>
          </div>
          
          <div className="grid grid-cols-2 gap-4">
            <Link
              to="/qr-olustur"
              className="group flex items-center gap-3 p-5 bg-gradient-to-r from-blue-500/20 to-cyan-500/20 rounded-2xl hover:from-blue-500/30 hover:to-cyan-500/30 transition-all border border-blue-400/30 hover:border-blue-400/50 will-change-transform"
            >
              <div className="w-14 h-14 bg-gradient-to-br from-blue-500 to-cyan-600 rounded-2xl flex items-center justify-center flex-shrink-0 shadow-lg group-hover:scale-110 transition-transform will-change-transform">
                <i className="ri-qr-code-line text-3xl text-white"></i>
              </div>
              <div>
                <div className="font-bold text-white text-lg">QR Oluştur</div>
                <div className="text-sm text-blue-200">Hemen başla</div>
              </div>
            </Link>
            
            <Link
              to="/qr-tara"
              className="group flex items-center gap-3 p-5 bg-gradient-to-r from-purple-500/20 to-pink-500/20 rounded-2xl hover:from-purple-500/30 hover:to-pink-500/30 transition-all border border-purple-400/30 hover:border-purple-400/50 will-change-transform"
            >
              <div className="w-14 h-14 bg-gradient-to-br from-purple-500 to-pink-600 rounded-2xl flex items-center justify-center flex-shrink-0 shadow-lg group-hover:scale-110 transition-transform will-change-transform">
                <i className="ri-scan-line text-3xl text-white"></i>
              </div>
              <div>
                <div className="font-bold text-white text-lg">QR & Barkod Tara</div>
                <div className="text-sm text-purple-200">Kamera aç</div>
              </div>
            </Link>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <Navigation />
    </div>
  );
}
