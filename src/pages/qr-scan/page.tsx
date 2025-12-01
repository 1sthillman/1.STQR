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
import { Share } from '@capacitor/share';
import { Clipboard } from '@capacitor/clipboard';

export default function QRScanNew() {
  const navigate = useNavigate();
  const { cart, addToCart, updateCartItem, reload: reloadCart } = useCartDB();
  const { products, getProductByBarcode, reload: reloadProducts } = useProductsDB();
  const { history, addToHistory, clearHistory, deleteFromHistory, reload: reloadHistory } = useScanHistory();

  const [scanning, setScanning] = useState(false);
  const [scanMode, setScanMode] = useState<'normal' | 'fast'>('normal');
  const [autoScan, setAutoScan] = useState(true); // Otomatik/Manuel tarama
  const [useFlashlight, setUseFlashlight] = useState(false); // Telefon flaşı kullan
  const [cameraFacing, setCameraFacing] = useState<'environment' | 'user'>('environment'); // Arka/Ön kamera
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'warning' | 'info' } | null>(null);
  const [lastScan, setLastScan] = useState<string>('');
  const [processing, setProcessing] = useState(false); // İşlem devam ediyor mu?
  const [flashActive, setFlashActive] = useState(false); // Flash feedback (görsel)
  const [manualScanReady, setManualScanReady] = useState(false); // Manuel tarama hazır mı?
  
  const html5QrCode = useRef<Html5Qrcode | null>(null);
  const lastScanTime = useRef<number>(0);
  const cooldownTimer = useRef<any>(null);
  const processingLock = useRef<boolean>(false); // Double processing önleme
  const videoTrack = useRef<MediaStreamTrack | null>(null); // Video track for flashlight

  // History'yi yükle
  useEffect(() => {
    reloadHistory();
  }, []);

  // Kamera değiştiğinde scanner'ı yeniden başlat
  useEffect(() => {
    const restartCamera = async () => {
      if (scanning && html5QrCode.current) {
        console.log('📸 Kamera değişti, yeniden başlatılıyor:', cameraFacing);
        try {
          await html5QrCode.current.stop();
          videoTrack.current = null;
        } catch (e) {
          console.log('Stop hatası (normal):', e);
        }
        
        setTimeout(async () => {
          await startScanner();
        }, 300);
      }
    };
    
    restartCamera();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cameraFacing]);

  // ✅ YENİ: Mod değişikliklerinde otomatik yeniden başlat
  useEffect(() => {
    const restartOnModeChange = async () => {
      if (scanning && html5QrCode.current) {
        console.log('🔄 Mod değişti, yeniden başlatılıyor...');
        console.log('- Tarama Modu:', scanMode);
        console.log('- Otomatik/Manuel:', autoScan ? 'Otomatik' : 'Manuel');
        console.log('- Flaş:', useFlashlight ? 'Açık' : 'Kapalı');
        
        try {
          await html5QrCode.current.stop();
          videoTrack.current = null;
        } catch (e) {
          console.log('Stop hatası (normal):', e);
        }
        
        setTimeout(async () => {
          await startScanner();
          setToast({ 
            message: `✅ ${scanMode === 'fast' ? 'Hızlı Sepet' : 'Normal'} - ${autoScan ? 'Otomatik' : 'Manuel'} Mod`, 
            type: 'success' 
          });
        }, 300);
      }
    };
    
    restartOnModeChange();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [scanMode, autoScan]);

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
          Html5QrcodeSupportedFormats.CODE_93,
          Html5QrcodeSupportedFormats.UPC_A,
          Html5QrcodeSupportedFormats.UPC_E,
          Html5QrcodeSupportedFormats.UPC_EAN_EXTENSION,
          Html5QrcodeSupportedFormats.CODABAR,
          Html5QrcodeSupportedFormats.ITF,
          Html5QrcodeSupportedFormats.DATA_MATRIX,
          Html5QrcodeSupportedFormats.PDF_417,
          Html5QrcodeSupportedFormats.AZTEC,
        ],
        verbose: false, // Console'u temiz tut
      });
      console.log('✅ html5-qrcode hazır - TÜM FORMATLAR (QR + BARKOD) AKTİF!');
    }

    // ✅ KRİTİK: Component unmount olduğunda kamerayı ZORLA KAPAT
    return () => {
      console.log('🔴 Component unmount - Kamera kapatılıyor...');
      stopScanner();
      
      // Ekstra güvenlik: Tüm video stream'leri zorla kapat
      setTimeout(() => {
        const videoElement = document.querySelector('#qr-reader video') as HTMLVideoElement;
        if (videoElement && videoElement.srcObject) {
          const stream = videoElement.srcObject as MediaStream;
          stream.getTracks().forEach(track => {
            track.stop();
            console.log('🔴 Video track durduruldu:', track.label);
          });
        }
      }, 100);
    };
  }, []);

  // Görsel flash feedback
  const triggerFlash = () => {
    setFlashActive(true);
    setTimeout(() => setFlashActive(false), 150);
  };

  // Telefon flaşını aç/kapat
  const togglePhoneFlash = async (enable: boolean) => {
    if (!videoTrack.current) return;

    try {
      const capabilities = videoTrack.current.getCapabilities() as any;
      
      if (capabilities.torch) {
        await videoTrack.current.applyConstraints({
          advanced: [{ torch: enable } as any]
        });
        console.log(`📸 Telefon flaşı: ${enable ? 'AÇIK' : 'KAPALI'}`);
      } else {
        console.warn('⚠️ Bu cihaz torch özelliğini desteklemiyor');
      }
    } catch (error) {
      console.error('Flaş hatası:', error);
    }
  };

  // Her okumada flaşı yanıp söndür
  const blinkPhoneFlash = async () => {
    if (!useFlashlight || !videoTrack.current) return;

    try {
      await togglePhoneFlash(true); // Aç
      setTimeout(async () => {
        await togglePhoneFlash(false); // Kapat
      }, 200); // 200ms sonra kapat
    } catch (error) {
      console.error('Blink flaş hatası:', error);
    }
  };

  const startScanner = async () => {
    if (!html5QrCode.current || scanning) return;

    console.log('🚀 Scanner başlatılıyor...');
    setScanning(true);
    processingLock.current = false;
    setManualScanReady(false);

    try {
      await html5QrCode.current.start(
        { facingMode: cameraFacing },
        {
          fps: 30,
          qrbox: function(viewfinderWidth, viewfinderHeight) {
            const minEdge = Math.min(viewfinderWidth, viewfinderHeight);
            const qrboxSize = Math.floor(minEdge * 0.85);
            return {
              width: qrboxSize,
              height: Math.floor(qrboxSize * 0.7)
            };
          },
          aspectRatio: 1.0,
          disableFlip: false,
          videoConstraints: {
            facingMode: cameraFacing
          }
        },
        (decodedText, decodedResult) => {
          // 👆 MANUEL MOD KONTROLÜ - Sadece otomatik modda işle
          if (!autoScan && !manualScanReady) {
            console.log('👆 Manuel mod: Ekrana dokunulması bekleniyor');
            return; // Manuel modda ve dokunulmamışsa atla
          }

          const now = Date.now();
          
          // 🔒 GÜÇLENDİRİLMİŞ PROCESSING LOCK
          if (processingLock.current) {
            console.log('⏸️ İşlem devam ediyor - ATLA');
            return;
          }

          // 🕐 COOLDOWN CHECK - 2 saniyeye çıkarıldı
          if (now - lastScanTime.current < 2000) {
            console.log('⏰ Cooldown aktif - ATLA');
            return;
          }

          // 🔄 DUPLICATE CHECK - Daha sıkı
          if (decodedText === lastScan && (now - lastScanTime.current) < 3000) {
            console.log('🔁 Duplicate kod - ATLA');
            return;
          }

          console.log('🎉 YENİ OKUMA:', decodedText, 'Format:', decodedResult.result.format);
          
          // ✅ KRİTİK: Lock'u HEMEN aktifleştir
          processingLock.current = true;
          setProcessing(true);
          lastScanTime.current = now;
          setLastScan(decodedText);
          setManualScanReady(false); // Manuel tarama tamamlandı
          
          // ⚡ GÖRSEL FLASH
          triggerFlash();
          
          // 📱 TELEFON FLAŞI
          blinkPhoneFlash();
          
          // 🔊 SES EFEKTİ
          const audio = new Audio('/casual-click-pop-ui-2-262119.mp3');
          audio.play().catch(() => {});

          // 📦 İŞLE - İşlem bitene kadar lock açılmayacak
          handleScan(decodedText)
            .then(() => {
              console.log('✅ İşlem BAŞARILI');
            })
            .catch((error) => {
              console.error('❌ İşlem HATASI:', error);
            })
            .finally(() => {
              // İşlem tamamen bittikten sonra lock aç
            setTimeout(() => {
              processingLock.current = false;
              setProcessing(false);
                console.log('🔓 Lock açıldı, yeni tarama hazır');
              }, 1000); // 1 saniye ekstra güvenlik
          });

          // Manuel modda otomatik durdur
          if (!autoScan) {
            setTimeout(() => {
              stopScanner();
              setToast({ message: '✅ Tarama tamamlandı', type: 'success' });
            }, 1500);
          }
        },
        (errorMessage) => {
          // NotFoundException normal
        }
      );

      // Video track'i al (flaş için)
      setTimeout(async () => {
        try {
          const videoElement = document.querySelector('#qr-reader video') as HTMLVideoElement;
          if (videoElement && videoElement.srcObject) {
            const stream = videoElement.srcObject as MediaStream;
            const tracks = stream.getVideoTracks();
            if (tracks.length > 0) {
              videoTrack.current = tracks[0];
              console.log('📸 Video track alındı, flaş hazır');
            }
          }
        } catch (error) {
          console.error('Video track alma hatası:', error);
        }
      }, 1000);

      console.log('✅ Scanner başlatıldı!');
    } catch (error: any) {
      console.error('❌ Scanner başlatma hatası:', error);
      setToast({ message: 'Kamera başlatılamadı: ' + error.message, type: 'error' });
      setScanning(false);
      processingLock.current = false;
    }
  };

  const stopScanner = async () => {
    if (!html5QrCode.current) return;

    console.log('🛑 stopScanner() çağrıldı');

    try {
        // Flaşı kapat
        if (videoTrack.current) {
        try {
          await togglePhoneFlash(false);
        } catch (e) {
          console.log('Flaş kapatma hatası (normal):', e);
        }
          videoTrack.current = null;
        }
        
      // Scanner'ı durdur
      if (scanning) {
        await html5QrCode.current.stop();
        console.log('✅ Html5Qrcode.stop() başarılı');
      }

      // ✅ KRİTİK: Tüm video track'leri ZORLA DURDUR
      setTimeout(() => {
        try {
          const videoElement = document.querySelector('#qr-reader video') as HTMLVideoElement;
          if (videoElement && videoElement.srcObject) {
            const stream = videoElement.srcObject as MediaStream;
            const tracks = stream.getTracks();
            
            console.log(`🔴 ${tracks.length} video track bulundu, hepsi kapatılıyor...`);
            
            tracks.forEach((track, index) => {
              track.stop();
              console.log(`✅ Track ${index + 1} durduruldu:`, track.label, track.readyState);
            });
            
            // Stream'i temizle
            videoElement.srcObject = null;
            console.log('✅ Video element temizlendi');
      }
        } catch (error) {
          console.error('Video track temizleme hatası:', error);
        }
      }, 200);

      setScanning(false);
      setManualScanReady(false);
      console.log('🛑 Scanner tamamen durduruldu');
    } catch (error) {
      console.error('❌ Scanner durdurma hatası:', error);
      // Hata olsa bile state'i güncelle
      setScanning(false);
      setManualScanReady(false);
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
    console.log('🛒 HIZLI SEPET: Başlıyor...', barcode);
    
    try {
      // 1. Ürün ve sepet verilerini yenile
    await reloadProducts();
    await reloadCart();
      console.log('✅ Veriler yenilendi');

      // 2. Ürünü bul
    const product = await getProductByBarcode(barcode);

    if (!product) {
        console.error('❌ Ürün bulunamadı:', barcode);
      setToast({ message: '❌ Ürün kayıtlı değil!', type: 'error' });
      return;
    }

      console.log('✅ Ürün bulundu:', product.name);

      // 3. Stok kontrolü
    if (product.stock <= 0) {
        console.error('❌ Stok yok:', product.name);
      setToast({ message: '⚠️ Stokta yok!', type: 'error' });
      return;
    }

      // 4. Sepetteki mevcut miktarı kontrol et
    const existingItem = cart.find((item: any) => String(item.id) === String(product.id));
    const currentQty = existingItem ? existingItem.quantity : 0;

      console.log('📊 Mevcut miktar:', currentQty, '/ Stok:', product.stock);

    if (currentQty + 1 > product.stock) {
        console.error('❌ Stok yetersiz:', currentQty + 1, '>', product.stock);
      setToast({ message: `⚠️ Stok yetersiz! Max: ${product.stock}`, type: 'error' });
      return;
    }

      // 5. Sepete ekle/güncelle
    if (existingItem) {
        console.log('📝 Miktar güncelleniyor:', currentQty, '->', currentQty + 1);
      await updateCartItem(String(product.id), currentQty + 1);
    } else {
        console.log('➕ Yeni ürün ekleniyor');
      await addToCart({
        id: String(product.id),
        name: product.name,
        price: product.price,
        quantity: 1,
        barcode: product.barcode,
        image: product.image,
      });
    }

      // 6. Sepeti yenile ve onayla
    await reloadCart();
      
      // 7. Doğrulama - gerçekten eklendi mi?
      const updatedCart = cart.find((item: any) => String(item.id) === String(product.id));
      const finalQty = updatedCart ? updatedCart.quantity : 0;
      
      console.log('✅ SEPET GÜNCELLENDİ - Final Miktar:', finalQty);
      setToast({ message: `✅ ${product.name} (${finalQty}x) sepette!`, type: 'success' });
      
    } catch (error: any) {
      console.error('❌ HIZLI SEPET HATASI:', error);
      setToast({ message: `❌ Hata: ${error.message}`, type: 'error' });
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-pink-900 pb-20">
      {/* ✨ ULTRA MODERN COMPACT HEADER */}
      <div className="bg-black/80 backdrop-blur-xl border-b border-white/10 text-white px-3 py-2 shadow-2xl sticky top-0 z-50">
        <div className="flex items-center justify-between gap-2">
          {/* Back */}
          <Link 
            to="/" 
            className="w-10 h-10 flex items-center justify-center bg-white/5 hover:bg-white/10 rounded-full transition-all active:scale-90 border border-white/10"
          >
            <i className="ri-arrow-left-line text-lg"></i>
          </Link>

          {/* Scan Mode */}
          <button
            onClick={() => setScanMode(prev => prev === 'normal' ? 'fast' : 'normal')}
            className={`w-10 h-10 flex items-center justify-center rounded-full transition-all active:scale-90 border ${
              scanMode === 'normal' 
                ? 'bg-gradient-to-br from-purple-500 to-purple-600 border-purple-400/50 shadow-lg shadow-purple-500/30' 
                : 'bg-gradient-to-br from-green-500 to-green-600 border-green-400/50 shadow-lg shadow-green-500/30'
            }`}
            title={scanMode === 'normal' ? 'Normal Mod' : 'Hızlı Sepet'}
          >
            <i className={`${scanMode === 'normal' ? 'ri-scan-line' : 'ri-shopping-cart-2-fill'} text-lg`}></i>
          </button>

          {/* Spacer */}
          <div className="flex-1"></div>

          {/* Camera Switch */}
          <button
            onClick={() => setCameraFacing(prev => prev === 'environment' ? 'user' : 'environment')}
            className={`w-10 h-10 flex items-center justify-center rounded-full transition-all active:scale-90 border ${
              cameraFacing === 'environment' 
                ? 'bg-gradient-to-br from-blue-500 to-blue-600 border-blue-400/50 shadow-lg shadow-blue-500/30' 
                : 'bg-gradient-to-br from-cyan-500 to-cyan-600 border-cyan-400/50 shadow-lg shadow-cyan-500/30'
            }`}
            title={cameraFacing === 'environment' ? 'Arka Kamera' : 'Ön Kamera'}
          >
            <i className={`${cameraFacing === 'environment' ? 'ri-camera-line' : 'ri-camera-switch-fill'} text-lg`}></i>
          </button>

          {/* Auto/Manual */}
          <button
            onClick={() => setAutoScan(!autoScan)}
            className={`w-10 h-10 flex items-center justify-center rounded-full transition-all active:scale-90 border ${
              autoScan 
                ? 'bg-gradient-to-br from-blue-500 to-indigo-600 border-blue-400/50 shadow-lg shadow-blue-500/30' 
                : 'bg-gradient-to-br from-orange-500 to-orange-600 border-orange-400/50 shadow-lg shadow-orange-500/30'
            }`}
            title={autoScan ? 'Otomatik' : 'Manuel'}
          >
            <i className={`${autoScan ? 'ri-refresh-line' : 'ri-hand-coin-line'} text-lg`}></i>
          </button>

          {/* Flash */}
          <button
            onClick={() => setUseFlashlight(!useFlashlight)}
            className={`w-10 h-10 flex items-center justify-center rounded-full transition-all active:scale-90 border ${
              useFlashlight 
                ? 'bg-gradient-to-br from-yellow-400 to-yellow-500 border-yellow-300/50 shadow-lg shadow-yellow-500/30' 
                : 'bg-white/5 border-white/10'
            }`}
            title={useFlashlight ? 'Flaş Açık' : 'Flaş Kapalı'}
          >
            <i className={`ri-flashlight-${useFlashlight ? 'fill' : 'line'} text-lg ${useFlashlight ? 'text-gray-900' : 'text-white'}`}></i>
          </button>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-3 py-4 space-y-4">
        {/* Mod Seçimi - Compact */}
        <div className="bg-white/10 backdrop-blur-xl rounded-2xl p-4 border border-white/20">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-10 h-10 bg-gradient-to-br from-purple-600 to-pink-600 rounded-xl flex items-center justify-center shadow-lg flex-shrink-0">
              <i className="ri-settings-3-line text-white text-xl"></i>
            </div>
            <div>
              <h2 className="text-lg font-bold text-white">Tarama Modu</h2>
              <p className="text-white/60 text-xs">Normal veya hızlı sepet</p>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => setScanMode('normal')}
              className={`p-3 rounded-xl border-2 transition-all ${
                scanMode === 'normal'
                  ? 'border-purple-500 bg-purple-500/20 shadow-lg'
                  : 'border-white/20 hover:border-white/40 bg-white/5'
              }`}
            >
              <i className={`ri-scan-line text-2xl mb-1 block ${scanMode === 'normal' ? 'text-purple-400' : 'text-white/60'}`}></i>
              <div className={`font-semibold text-sm ${scanMode === 'normal' ? 'text-white' : 'text-white/60'}`}>
                📸 Normal
              </div>
            </button>
            <button
              onClick={() => setScanMode('fast')}
              className={`p-3 rounded-xl border-2 transition-all ${
                scanMode === 'fast'
                  ? 'border-green-500 bg-green-500/20 shadow-lg'
                  : 'border-white/20 hover:border-white/40 bg-white/5'
              }`}
            >
              <i className={`ri-shopping-cart-line text-2xl mb-1 block ${scanMode === 'fast' ? 'text-green-400' : 'text-white/60'}`}></i>
              <div className={`font-semibold text-sm ${scanMode === 'fast' ? 'text-white' : 'text-white/60'}`}>
                🛒 Hızlı Sepet
              </div>
            </button>
          </div>
        </div>

        {/* Scanner */}
        <div className="bg-black rounded-2xl overflow-hidden relative" style={{ minHeight: '600px', width: '100%' }}>
          {/* Flash Overlay */}
          {flashActive && (
            <div className="absolute inset-0 bg-white z-50 animate-pulse" style={{ animation: 'flash 150ms ease-out' }}></div>
          )}
          
          {/* Processing Overlay */}
          {processing && (
            <div className="absolute inset-0 bg-green-500/20 z-40 flex items-center justify-center backdrop-blur-sm">
              <div className="bg-green-500 text-white px-6 py-3 rounded-2xl font-bold shadow-2xl animate-pulse">
                ⚡ İşleniyor...
              </div>
            </div>
          )}

          {/* Manuel Mod - Ekrana Dokun - ANINDA ÇALIŞIR */}
          {!autoScan && scanning && !processing && !manualScanReady && (
            <div 
              onClick={() => {
                if (!processingLock.current) {
                  console.log('👆 Manuel tarama AKTİF - Şimdi barkodu göster!');
                  setManualScanReady(true);
                  setToast({ message: '✅ Tarama hazır! Barkodu göster', type: 'success' });
                  
                  // 5 saniye içinde kod okutulmazsa timeout
                  setTimeout(() => {
                    if (!processingLock.current && manualScanReady) {
                      setManualScanReady(false);
                      setToast({ message: '⏰ Zaman aşımı, tekrar dokunun', type: 'warning' });
                    }
                  }, 5000);
                }
              }}
              className="absolute inset-0 z-[60] cursor-pointer flex items-center justify-center bg-black/40 backdrop-blur-sm touch-manipulation"
            >
              <div className="bg-gradient-to-r from-blue-600 to-purple-600 text-white px-10 py-5 rounded-3xl font-bold shadow-2xl animate-bounce pointer-events-none border-2 border-white/30">
                <div className="flex flex-col items-center gap-2">
                  <i className="ri-hand-coin-line text-4xl"></i>
                  <span className="text-lg">Dokun & Tara</span>
                </div>
              </div>
            </div>
          )}
          
          {/* Manuel Tarama AKTİF - Bekleniyor */}
          {!autoScan && scanning && manualScanReady && !processing && (
            <div className="absolute inset-0 z-[60] flex items-center justify-center bg-green-500/20 backdrop-blur-sm pointer-events-none">
              <div className="bg-gradient-to-r from-green-600 to-emerald-600 text-white px-10 py-5 rounded-3xl font-bold shadow-2xl animate-pulse border-2 border-white/30">
                <div className="flex flex-col items-center gap-2">
                  <i className="ri-camera-line text-4xl"></i>
                  <span className="text-lg">Barkodu Göster</span>
                </div>
              </div>
            </div>
          )}

          <div id="qr-reader" style={{ width: '100%', height: '600px' }}></div>
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

        {/* Son Taramalar - PREMIUM DESIGN */}
        {history.length > 0 && (
          <div className="relative bg-gradient-to-br from-purple-900/30 via-blue-900/30 to-pink-900/30 backdrop-blur-2xl rounded-3xl p-6 border border-white/20 shadow-2xl overflow-hidden">
            {/* Animated background */}
            <div className="absolute inset-0 bg-gradient-to-r from-purple-500/10 via-blue-500/10 to-pink-500/10 animate-pulse"></div>
            
            {/* Header */}
            <div className="relative flex items-center justify-between mb-6">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 bg-gradient-to-br from-purple-600 to-pink-600 rounded-2xl flex items-center justify-center shadow-lg">
                  <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                  </svg>
                </div>
                <div>
                  <h2 className="text-xl font-bold text-white">Son Taramalar</h2>
                  <p className="text-white/70 text-sm">{history.length} kayıt</p>
                </div>
              </div>
              <button
                onClick={async () => {
                  if (confirm('Tüm tarama geçmişi silinecek. Emin misiniz?')) {
                    await clearHistory();
                    setToast({ message: '✅ Tüm geçmiş silindi', type: 'success' });
                  }
                }}
                className="px-4 py-2 bg-gradient-to-r from-red-600 to-pink-600 hover:from-red-700 hover:to-pink-700 text-white rounded-xl text-sm font-semibold shadow-lg hover:shadow-xl transition-all duration-300 transform hover:scale-105"
              >
                <svg className="w-4 h-4 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
                Tümünü Sil
              </button>
            </div>
            
            {/* List */}
            <div className="relative space-y-4 max-h-[600px] overflow-y-auto pr-2 custom-scrollbar">
              {history.slice(0, 10).map((item: any, index: number) => (
                <div
                  key={item.id}
                  className="group relative bg-gradient-to-r from-white/10 to-white/5 backdrop-blur-xl rounded-2xl p-4 border border-white/20 hover:border-white/40 transition-all duration-300 hover:shadow-2xl hover:scale-[1.02]"
                  style={{
                    animation: `slideInUp 0.${index + 3}s ease-out`
                  }}
                >
                  {/* Gradient overlay */}
                  <div className="absolute inset-0 bg-gradient-to-br from-purple-600/0 via-blue-600/0 to-pink-600/0 group-hover:from-purple-600/10 group-hover:via-blue-600/10 group-hover:to-pink-600/10 rounded-2xl transition-all duration-300"></div>
                  
                  <div className="relative">
                    {/* Content Header */}
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <div className={`w-8 h-8 rounded-xl flex items-center justify-center ${
                            item.type === 'barcode' ? 'bg-gradient-to-br from-blue-500 to-cyan-500' :
                            item.type === 'url' ? 'bg-gradient-to-br from-green-500 to-emerald-500' :
                            'bg-gradient-to-br from-purple-500 to-pink-500'
                          } shadow-lg`}>
                            <span className="text-lg">
                              {item.type === 'barcode' && '📊'}
                              {item.type === 'url' && '🔗'}
                              {item.type === 'text' && '📝'}
                            </span>
                          </div>
                          <span className={`px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider shadow-md ${
                            item.type === 'barcode' ? 'bg-gradient-to-r from-blue-600 to-cyan-600 text-white' :
                            item.type === 'url' ? 'bg-gradient-to-r from-green-600 to-emerald-600 text-white' :
                            'bg-gradient-to-r from-purple-600 to-pink-600 text-white'
                          }`}>
                            {item.type}
                          </span>
                        </div>
                        <div className="text-white font-semibold text-sm mb-1 line-clamp-2">
                          {item.content}
                        </div>
                        <div className="flex items-center gap-2 text-white/60 text-xs">
                          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                          </svg>
                          {new Date(item.timestamp).toLocaleString('tr-TR', { 
                            day: '2-digit', 
                            month: '2-digit',
                            year: 'numeric',
                            hour: '2-digit', 
                            minute: '2-digit' 
                          })}
                        </div>
                      </div>
                    </div>
                    
                    {/* ✨ ACTION BUTTONS - ULTRA MODERN ICON-ONLY */}
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={async () => {
                          await Clipboard.write({ string: item.content });
                          setToast({ message: '✅ Kopyalandı', type: 'success' });
                        }}
                        className="w-9 h-9 bg-gradient-to-br from-blue-500 to-cyan-500 hover:from-blue-600 hover:to-cyan-600 rounded-full text-white shadow-lg hover:shadow-xl transition-all duration-200 active:scale-90 flex items-center justify-center"
                        title="Kopyala"
                      >
                        <i className="ri-file-copy-line text-base"></i>
                      </button>

                      
                      <button
                        onClick={() => {
                          navigate('/qr-olustur', { 
                            state: { 
                              editContent: item.content, 
                              editType: item.type
                            } 
                          });
                        }}
                        className="w-9 h-9 bg-gradient-to-br from-purple-500 to-pink-500 hover:from-purple-600 hover:to-pink-600 rounded-full text-white shadow-lg hover:shadow-xl transition-all duration-200 active:scale-90 flex items-center justify-center"
                        title="Düzenle"
                      >
                        <i className="ri-edit-line text-base"></i>
                      </button>
                      
                      {item.type === 'barcode' && (
                        <button
                          onClick={() => {
                            navigate(`/urun-yonetimi?addBarcode=${encodeURIComponent(item.content)}`);
                          }}
                          className="w-9 h-9 bg-gradient-to-br from-green-500 to-emerald-500 hover:from-green-600 hover:to-emerald-600 rounded-full text-white shadow-lg hover:shadow-xl transition-all duration-200 active:scale-90 flex items-center justify-center"
                          title="Ürün Ekle"
                        >
                          <i className="ri-add-circle-line text-base"></i>
                        </button>
                      )}
                      
                      <button
                        onClick={async () => {
                          await Share.share({
                            text: item.content,
                            title: 'QR/Barkod Paylaş',
                            dialogTitle: 'Paylaş'
                          });
                        }}
                        className="w-9 h-9 bg-gradient-to-br from-amber-500 to-orange-500 hover:from-amber-600 hover:to-orange-600 rounded-full text-white shadow-lg hover:shadow-xl transition-all duration-200 active:scale-90 flex items-center justify-center"
                        title="Paylaş"
                      >
                        <i className="ri-share-forward-line text-base"></i>
                      </button>
                      
                      <button
                        onClick={() => {
                          navigate('/sosyal-harita', { 
                            state: { 
                              qrImage: `data:text/plain;base64,${btoa(item.content)}`,
                              qrContent: item.content, 
                              qrType: item.type,
                              from: 'scan-history'
                            } 
                          });
                        }}
                        className="w-9 h-9 bg-gradient-to-br from-red-500 to-rose-500 hover:from-red-600 hover:to-rose-600 rounded-full text-white shadow-lg hover:shadow-xl transition-all duration-200 active:scale-90 flex items-center justify-center"
                        title="Haritada Paylaş"
                      >
                        <i className="ri-map-pin-line text-base"></i>
                      </button>
                      
                      <button
                        onClick={async () => {
                          if (confirm('Bu kaydı silmek istediğinize emin misiniz?')) {
                            await deleteFromHistory(item.id);
                            setToast({ message: '✅ Silindi', type: 'success' });
                          }
                        }}
                        className="w-9 h-9 bg-gradient-to-br from-gray-600 to-gray-700 hover:from-gray-700 hover:to-gray-800 rounded-full text-white shadow-lg hover:shadow-xl transition-all duration-200 active:scale-90 flex items-center justify-center"
                        title="Sil"
                      >
                        <i className="ri-delete-bin-line text-base"></i>
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Toast */}
      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}

      {/* Navigation */}
      <Navigation />
    </div>
  );
}

