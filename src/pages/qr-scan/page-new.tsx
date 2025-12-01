/**
 * ⚡ ULTRA FAST QR & BARCODE SCANNER
 * html5-qrcode ile tek çözüm - MAKSİMUM HIZ!
 */

import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Html5Qrcode, Html5QrcodeSupportedFormats } from 'html5-qrcode';
import Navigation from '../../components/Navigation';
import Toast from '../../components/Toast';
import { useCart as useCartDB, useProducts as useProductsDB, useScanHistory } from '../../hooks';

export default function QRScanNew() {
  const navigate = useNavigate();
  const { cart, addToCart, updateCartItem, reload: reloadCart } = useCartDB();
  const { products, getProductByBarcode, reload: reloadProducts } = useProductsDB();
  const { addToHistory } = useScanHistory();

  const [scanning, setScanning] = useState(false);
  const [scanMode, setScanMode] = useState<'normal' | 'fast'>('normal');
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'warning' | 'info' } | null>(null);
  const [lastScan, setLastScan] = useState<string>('');
  
  const html5QrCode = useRef<Html5Qrcode | null>(null);
  const lastScanTime = useRef<number>(0);

  // html5-qrcode başlat
  useEffect(() => {
    if (!html5QrCode.current) {
      html5QrCode.current = new Html5Qrcode('qr-reader', {
        formatsToSupport: [
          Html5QrcodeSupportedFormats.QR_CODE,
          Html5QrcodeSupportedFormats.EAN_13,
          Html5QrcodeSupportedFormats.EAN_8,
          Html5QrcodeSupportedFormats.CODE_128,
          Html5QrcodeSupportedFormats.CODE_39,
          Html5QrcodeSupportedFormats.UPC_A,
          Html5QrcodeSupportedFormats.UPC_E,
        ],
        verbose: false,
      });
      console.log('✅ html5-qrcode hazır!');
    }

    return () => {
      stopScanner();
    };
  }, []);

  const startScanner = async () => {
    if (!html5QrCode.current || scanning) return;

    // GitHub Pages HTTPS kontrolü
    if (window.location.protocol !== 'https:' && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
      setToast({ 
        message: '⚠️ Kamera erişimi için HTTPS gerekli. GitHub Pages otomatik HTTPS kullanır.', 
        type: 'warning' 
      });
    }

    console.log('🚀 Scanner başlatılıyor...');
    setScanning(true);

    try {
      await html5QrCode.current.start(
        { facingMode: 'environment' },
        {
          fps: 30, // ⚡ MAKSİMUM FPS!
          qrbox: { width: 250, height: 250 }, // Odak alanı
          aspectRatio: 1.0,
          videoConstraints: {
            facingMode: 'environment',
            width: { ideal: 1280 },
            height: { ideal: 720 }
          }
        },
        (decodedText, decodedResult) => {
          // ⚡ ANINDA İŞLE!
          const now = Date.now();
          
          // Debounce - 300ms
          if (decodedText === lastScan && (now - lastScanTime.current) < 300) {
            return;
          }

          lastScanTime.current = now;
          setLastScan(decodedText);

          console.log('🎉 OKUMA:', decodedText);
          
          // Ses efekti - Base path desteği
          const basePath = (window as any).__BASE_PATH__ || '';
          const audio = new Audio(`${basePath}casual-click-pop-ui-2-262119.mp3`);
          audio.play().catch(() => {});

          // İşle
          handleScan(decodedText);

          // 300ms sonra tekrar okuyabilir
          setTimeout(() => setLastScan(''), 300);
        },
        (errorMessage) => {
          // NotFoundException normal - sessizce geç
        }
      );

      console.log('✅ Scanner başlatıldı!');
    } catch (error: any) {
      console.error('❌ Scanner başlatma hatası:', error);
      setToast({ message: 'Kamera başlatılamadı: ' + error.message, type: 'error' });
      setScanning(false);
    }
  };

  const stopScanner = async () => {
    if (!html5QrCode.current) return;

    try {
      if (scanning) {
        await html5QrCode.current.stop();
        console.log('🛑 Scanner durduruldu');
      }
      setScanning(false);
    } catch (error) {
      console.error('Scanner durdurma hatası:', error);
    }
  };

  const handleScan = async (data: string) => {
    console.log('🎯 handleScan:', data);

    // SQLite'a ekle
    await addToHistory({
      id: String(Date.now()),
      content: data,
      type: detectType(data),
      timestamp: Date.now(),
    });

    // Hızlı modda otomatik sepete ekle
    if (scanMode === 'fast') {
      await handleFastModeAddToCart(data);
    } else {
      // Normal mod - toast göster
      setToast({ message: `✅ Okundu: ${data.substring(0, 30)}...`, type: 'success' });
    }
  };

  const detectType = (data: string) => {
    if (data.startsWith('http')) return 'url';
    if (data.startsWith('WIFI:')) return 'wifi';
    if (data.startsWith('mailto:')) return 'email';
    if (/^\d{8,13}$/.test(data)) return 'barcode';
    return 'text';
  };

  const handleFastModeAddToCart = async (barcode: string) => {
    await reloadProducts();
    await reloadCart();

    const product = await getProductByBarcode(barcode);

    if (!product) {
      setToast({ message: '❌ Ürün kayıtlı değil!', type: 'error' });
      return;
    }

    if (product.stock <= 0) {
      setToast({ message: '⚠️ Stokta yok!', type: 'error' });
      return;
    }

    const existingItem = cart.find((item: any) => String(item.id) === String(product.id));
    const currentQty = existingItem ? existingItem.quantity : 0;

    if (currentQty + 1 > product.stock) {
      setToast({ message: `⚠️ Stok yetersiz! Max: ${product.stock}`, type: 'error' });
      return;
    }

    if (existingItem) {
      await updateCartItem(String(product.id), currentQty + 1);
    } else {
      await addToCart({
        id: String(product.id),
        name: product.name,
        price: product.price,
        quantity: 1,
        barcode: product.barcode,
        image: product.image,
      });
    }

    await reloadCart();
    setToast({ message: `✅ ${product.name} sepete eklendi!`, type: 'success' });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-pink-900 pb-20">
      {/* Header */}
      <div className="bg-gradient-to-r from-purple-600 to-pink-600 text-white py-6 px-6 shadow-xl">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link to="/" className="bg-white/10 backdrop-blur-lg hover:bg-white/20 p-3 rounded-2xl transition-all">
              <i className="ri-arrow-left-line text-2xl"></i>
            </Link>
            <div>
              <h1 className="text-3xl font-bold">⚡ ULTRA FAST QR & BARKOD</h1>
              <p className="text-white/90 text-sm">{scanMode === 'normal' ? '📸 Normal' : '🛒 Hızlı Sepet'}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-6 py-6 space-y-6">
        {/* Mod Seçimi */}
        <div className="bg-white/10 backdrop-blur-xl rounded-3xl p-6 border border-white/20">
          <h2 className="text-xl font-bold text-white mb-4">🎯 Tarama Modu</h2>
          <div className="grid grid-cols-2 gap-4">
            <button
              onClick={() => setScanMode('normal')}
              className={`p-4 rounded-xl border-2 transition-all ${
                scanMode === 'normal'
                  ? 'border-purple-600 bg-purple-50'
                  : 'border-gray-200 hover:border-purple-300'
              }`}
            >
              <i className={`ri-scan-line text-3xl mb-2 block ${scanMode === 'normal' ? 'text-purple-600' : 'text-gray-600'}`}></i>
              <div className={`font-semibold ${scanMode === 'normal' ? 'text-purple-600' : 'text-gray-600'}`}>
                Normal Mod
              </div>
            </button>
            <button
              onClick={() => setScanMode('fast')}
              className={`p-4 rounded-xl border-2 transition-all ${
                scanMode === 'fast'
                  ? 'border-green-600 bg-green-50'
                  : 'border-gray-200 hover:border-green-300'
              }`}
            >
              <i className={`ri-shopping-cart-line text-3xl mb-2 block ${scanMode === 'fast' ? 'text-green-600' : 'text-gray-600'}`}></i>
              <div className={`font-semibold ${scanMode === 'fast' ? 'text-green-600' : 'text-gray-600'}`}>
                Hızlı Sepet
              </div>
            </button>
          </div>
        </div>

        {/* Scanner */}
        <div className="bg-black rounded-2xl overflow-hidden" style={{ minHeight: '400px' }}>
          <div id="qr-reader"></div>
        </div>

        {/* Kontroller */}
        <div className="flex justify-center gap-4">
          {!scanning ? (
            <button
              onClick={startScanner}
              className="px-8 py-4 bg-gradient-to-r from-green-500 to-emerald-600 text-white rounded-2xl font-bold text-lg shadow-lg hover:shadow-xl transition-all"
            >
              <i className="ri-play-line mr-2"></i>
              Taramayı Başlat
            </button>
          ) : (
            <button
              onClick={stopScanner}
              className="px-8 py-4 bg-gradient-to-r from-red-500 to-pink-500 text-white rounded-2xl font-bold text-lg shadow-lg hover:shadow-xl transition-all"
            >
              <i className="ri-stop-line mr-2"></i>
              Durdur
            </button>
          )}
        </div>
      </div>

      {/* Toast */}
      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}

      {/* Navigation */}
      <Navigation />
    </div>
  );
}














