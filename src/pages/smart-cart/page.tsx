import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Navigation from '../../components/Navigation';
import ReceiptPrinter from '../../components/ReceiptPrinter';
import Toast from '../../components/Toast';
import { useCart as useCartDB, useProducts as useProductsDB, useSales } from '../../hooks/useDatabase';
import type { CartItem } from '../../services';

export default function SmartCart() {
  const { cart, loading: cartLoading, updateCartItem, removeFromCart, clearCart: clearCartDB, reload: reloadCart } = useCartDB();
  const { products, getProductByBarcode, getProductById, updateProduct: updateProductStock, reload: reloadProducts } = useProductsDB();
  const { addSale } = useSales();
  
  const [showPayment, setShowPayment] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<'cash' | 'card'>('cash');
  const [cashAmount, setCashAmount] = useState(0);
  const [showReceipt, setShowReceipt] = useState(false);
  const [lastSale, setLastSale] = useState<any>(null);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'warning' | 'info' } | null>(null);
  const [loading, setLoading] = useState(false);

  // İlk yükleme - sadece mount'ta çalışsın
  useEffect(() => {
    reloadCart();
    reloadProducts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  
  // Loading state
  if (cartLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-green-50 to-emerald-50 flex items-center justify-center">
        <div className="text-center">
          <div className="w-16 h-16 mx-auto mb-4 border-4 border-green-600 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-xl font-semibold text-gray-900">Sepet Yükleniyor...</p>
          <p className="text-sm text-gray-600 mt-2">SQLite veriler getiriliyor</p>
        </div>
      </div>
    );
  }

  const updateQuantity = async (id: string | number, change: number) => {
    const item = cart.find(i => String(i.id) === String(id));
    if (!item) return;
    
        const newQty = Math.max(0, item.quantity + change);
    
    // STOK KONTROLÜ
    const product = products.find(p => String(p.id) === String(id));
    if (product && newQty > product.stock) {
      setToast({ message: `⚠️ Maksimum stok: ${product.stock} adet`, type: 'warning' });
      return;
    }
    
    if (newQty === 0) {
      // Sil
      await removeFromCart(String(id));
    } else {
      // Güncelle
      await updateCartItem(String(id), newQty);
    }
    
    await reloadCart();
  };

  const removeItem = async (id: string | number) => {
    await removeFromCart(String(id));
    await reloadCart();
    setToast({ message: '✅ Ürün sepetten çıkarıldı', type: 'success' });
  };

  const clearCart = async () => {
    if (confirm('Sepeti temizlemek istediğinizden emin misiniz?')) {
      await clearCartDB();
      await reloadCart();
      setToast({ message: '✅ Sepet temizlendi', type: 'success' });
    }
  };

  const total = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);

  const completePayment = async () => {
    // Validasyon
    if (cart.length === 0) {
      setToast({ message: '⚠️ Sepetiniz boş!', type: 'warning' });
      return;
    }

    if (paymentMethod === 'cash' && cashAmount < total) {
      setToast({ message: `⚠️ Yetersiz tutar! Lütfen en az ₺${total.toFixed(2)} giriniz.`, type: 'error' });
      return;
    }

    // Loading göster
    setLoading(true);

    try {
      console.log('💳 Ödeme başlatıldı...');
      
      // 1. STOK AZALTMA - Database'den direkt çek
      console.log('📦 Stoklar güncelleniyor...');
      console.log('📦 Sepetteki ürünler:', cart.map(i => ({ id: i.id, name: i.name, barcode: i.barcode, quantity: i.quantity })));
      
      for (const item of cart) {
        try {
          console.log(`🔍 Ürün aranıyor: ${item.name} (ID: ${item.id}, Barkod: ${item.barcode || 'YOK'})`);
          
          // ✅ KESİN ÇÖZÜM: Önce ID ile bul, bulamazsan barkod ile bul
          let product = await getProductById(String(item.id));
          
          if (!product) {
            console.log(`⚠️ ID ile bulunamadı, barkod deneniyor...`);
            if (item.barcode && item.barcode.trim()) {
              product = await getProductByBarcode(item.barcode);
            }
          }
          
          // SON KONTROL
          if (!product) {
            console.error(`❌ ÜRÜN BULUNAMADI:`, {
              itemId: item.id,
              itemName: item.name,
              itemBarcode: item.barcode
            });
            throw new Error(`Ürün bulunamadı: ${item.name}`);
          }
          
          console.log(`✅ Ürün bulundu: ${product.name} (Stock: ${product.stock})`);
          
          // Stok kontrolü
          if (product.stock < item.quantity) {
            throw new Error(`${product.name} için yetersiz stok! Mevcut: ${product.stock}, İstenen: ${item.quantity}`);
          }
          
          // Stok azalt
          const newStock = Math.max(0, product.stock - item.quantity);
          await updateProductStock(String(product.id), { stock: newStock });
          console.log(`✅ Stok güncellendi: ${product.stock} → ${newStock}`);
          
        } catch (itemError: any) {
          console.error(`❌ ${item.name} için hata:`, itemError);
          setLoading(false);
          setToast({ message: `❌ ${itemError.message}`, type: 'error' });
          return; // Ödemeyi durdur
        }
      }

      // 4. Para üstü hesapla
      const change = paymentMethod === 'cash' ? cashAmount - total : 0;

      // 5. Satış kaydet - SQLite
      console.log('💾 Satış kaydediliyor...');
      const saleId = String(Date.now());
      await addSale({
        id: saleId,
        items: cart.map(item => ({
          ...item,
          id: String(item.id)
        })),
        total,
        paymentMethod,
        cashAmount: paymentMethod === 'cash' ? cashAmount : 0,
        change: paymentMethod === 'cash' ? change : 0,
        timestamp: Date.now(),
      });
      console.log(`✅ Satış kaydedildi: ${saleId}`);

      // 6. Son satışı kaydet (fiş için)
      const saleData = {
        id: Date.now(),
        items: cart,
        total,
        paymentMethod,
        cashAmount: paymentMethod === 'cash' ? cashAmount : 0,
        change: paymentMethod === 'cash' ? change : 0,
        date: new Date().toISOString(),
      };
      setLastSale(saleData);

      // 7. Başarı mesajı
      let message = '✅ ÖDEME BAŞARILI! ';
      if (paymentMethod === 'cash') {
        message += `Para Üstü: ₺${change.toFixed(2)}`;
      } else {
        message += `Kredi Kartı ile ödendi`;
      }
      setToast({ message, type: 'success' });

      // 8. Sepeti temizle ve verileri yenile
      console.log('🧹 Sepet temizleniyor...');
      await clearCartDB();
      await reloadCart();
      await reloadProducts();
      console.log('✅ Tüm veriler güncellendi');

      // 9. Modal'ı kapat ve fiş ekranını aç
      setShowPayment(false);
      setCashAmount(0);
      setShowReceipt(true);

    } catch (error: any) {
      console.error('❌ Ödeme hatası:', error);
      
      // Detaylı hata mesajı
      let errorMessage = '❌ Ödeme işlemi başarısız!';
      if (error.message) {
        errorMessage += ` ${error.message}`;
      }
      
      setToast({ message: errorMessage, type: 'error' });
      
      // Verileri yenile (hata durumunda bile)
      await reloadProducts();
      await reloadCart();
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 pb-20">
      {/* Header - Market Kasası Tarzı */}
      <div className="bg-gradient-to-r from-green-600 to-teal-600 text-white py-4 px-6 sticky top-0 z-10 shadow-lg">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Link to="/" className="text-white hover:bg-white/20 p-2 rounded-lg transition-all">
              <i className="ri-arrow-left-line text-2xl"></i>
            </Link>
            <div>
              <h1 className="text-2xl font-bold flex items-center gap-2">
                <i className="ri-store-2-line"></i>
                Market Kasası
              </h1>
              <div className="flex items-center gap-4 text-green-100 text-sm mt-1">
                <span className="flex items-center gap-1">
                  <i className="ri-shopping-bag-line"></i>
                  {cart.reduce((sum, item) => sum + item.quantity, 0)} Ürün
                </span>
                {cart.length > 0 && (
                  <span className="flex items-center gap-1 font-bold text-yellow-300">
                    <i className="ri-money-dollar-circle-line"></i>
                    ₺{total.toFixed(2)}
                  </span>
                )}
              </div>
            </div>
          </div>
          {cart.length > 0 && (
            <div className="flex gap-2">
            <button
              onClick={clearCart}
                className="bg-red-500/90 text-white px-4 py-2 rounded-xl font-semibold hover:bg-red-600 transition-all"
            >
              <i className="ri-delete-bin-line mr-2"></i>
              Temizle
            </button>
            </div>
          )}
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-6 py-6 space-y-6">
        {/* Sepet Boş */}
        {cart.length === 0 ? (
          <div className="bg-white rounded-2xl shadow-lg p-12 text-center">
            <i className="ri-shopping-cart-line text-6xl text-gray-300 mb-4 block"></i>
            <h3 className="text-xl font-bold text-gray-900 mb-2">Sepetiniz Boş</h3>
            <p className="text-gray-600 mb-6">QR kod tarayarak ürün ekleyin</p>
            <Link
              to="/qr-tara"
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-green-600 to-teal-600 text-white px-8 py-3 rounded-xl font-semibold hover:from-green-700 hover:to-teal-700 transition-all shadow-lg"
            >
              <i className="ri-scan-line text-xl"></i>
              <span>Taramaya Başla</span>
            </Link>
          </div>
        ) : (
          <>
            {/* Sepet İçeriği - Premium VIP Kasa */}
            <div className="bg-gradient-to-br from-white via-green-50 to-teal-50 rounded-3xl shadow-2xl overflow-hidden border-2 border-green-300">
              <div className="bg-gradient-to-r from-green-600 via-teal-600 to-green-600 px-6 py-4 shadow-lg">
                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-bold text-white flex items-center gap-2">
                    <div className="w-10 h-10 bg-white/20 backdrop-blur rounded-xl flex items-center justify-center">
                      <i className="ri-file-list-3-line text-white text-xl"></i>
                    </div>
                    Premium Ürün Listesi
                  </h2>
                  <div className="bg-white/20 backdrop-blur px-4 py-2 rounded-xl">
                    <span className="font-bold text-white text-lg">{cart.length}</span>
                    <span className="text-green-100 text-sm ml-1">kalem</span>
                  </div>
                </div>
              </div>
              <div className="p-4">
                <div className="space-y-2 max-h-[60vh] overflow-y-auto scrollbar-hide">
                  {cart.map((item, index) => {
                    // Stok kontrolü - SQLite
                    const product = products.find((p: any) => String(p.id) === String(item.id));
                    const lowStock = product && product.stock < 5;
                    
                    return (
                      <div 
                        key={`cart-item-${item.id}-${index}-${Date.now()}`}
                        className={`flex items-center gap-3 p-3 rounded-2xl transition-all shadow-md hover:shadow-lg ${
                          lowStock ? 'bg-gradient-to-r from-orange-50 to-red-50 border-2 border-orange-400' : 'bg-gradient-to-r from-white to-green-50 border-2 border-green-200'
                        }`}
                      >
                        {/* Sol: Premium Ürün Görseli + Bilgisi */}
                        <div className="flex items-center gap-3 flex-1 min-w-0">
                          {/* Görsel - Daha Büyük ve Belirgin */}
                          {item.image ? (
                            <div className="relative flex-shrink-0">
                              <img 
                                src={item.image} 
                                alt={item.name || 'Ürün'}
                                className="w-16 h-16 object-cover rounded-xl shadow-lg border-2 border-white"
                              />
                              <div className="absolute -bottom-1 -right-1 w-6 h-6 bg-green-500 rounded-full flex items-center justify-center text-white text-xs font-bold shadow-md">
                                {index + 1}
                              </div>
                            </div>
                          ) : (
                            <div className="relative flex-shrink-0">
                              <div className="w-16 h-16 bg-gradient-to-br from-green-400 via-teal-400 to-blue-400 rounded-xl flex items-center justify-center shadow-lg border-2 border-white">
                                <i className="ri-shopping-bag-3-fill text-white text-2xl"></i>
                              </div>
                              <div className="absolute -bottom-1 -right-1 w-6 h-6 bg-white rounded-full flex items-center justify-center text-green-600 text-xs font-bold shadow-md border-2 border-green-400">
                                {index + 1}
                              </div>
                            </div>
                          )}
                          
                          {/* Ürün Bilgisi - Daha Okunabilir */}
                          <div className="flex-1 min-w-0">
                            <h3 className="font-bold text-gray-900 text-base leading-snug mb-1.5" style={{
                              display: '-webkit-box',
                              WebkitLineClamp: 2,
                              WebkitBoxOrient: 'vertical',
                              overflow: 'hidden',
                              wordBreak: 'break-word'
                            }}>
                              {item.name || item.barcode || 'Ürün'}
                            </h3>
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className="text-sm font-bold text-green-700 bg-green-100 px-2 py-1 rounded-lg">
                                ₺{item.price.toFixed(2)}
                              </span>
                              {lowStock && (
                                <span className="text-xs bg-orange-500 text-white px-2 py-1 rounded-lg font-bold shadow-sm animate-pulse">
                                  ⚠️ Az Stok
                                </span>
                              )}
                            </div>
                          </div>
                      </div>
                      
                        {/* Orta: Miktar Kontrolü */}
                        <div className="flex items-center gap-2 bg-gray-50 rounded-xl p-1">
                        <button
                          onClick={() => updateQuantity(item.id, -1)}
                            className="w-8 h-8 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-all flex items-center justify-center active:scale-95"
                        >
                            <i className="ri-subtract-line"></i>
                        </button>
                          <div className="w-10 text-center font-bold text-gray-900">
                            {item.quantity}
                          </div>
                        <button
                          onClick={() => updateQuantity(item.id, 1)}
                            className="w-8 h-8 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-all flex items-center justify-center active:scale-95"
                        >
                            <i className="ri-add-line"></i>
                        </button>
                        </div>
                        
                        {/* Sağ: Toplam + Sil */}
                        <div className="flex items-center gap-3">
                          <div className="text-right">
                            <div className="text-lg font-bold text-green-600">
                              ₺{(item.price * item.quantity).toFixed(2)}
                            </div>
                          </div>
                        <button
                          onClick={() => removeItem(item.id)}
                            className="w-8 h-8 bg-gray-200 text-gray-600 rounded-lg hover:bg-red-100 hover:text-red-600 transition-all flex items-center justify-center active:scale-95"
                        >
                            <i className="ri-close-line"></i>
                        </button>
                      </div>
                    </div>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Özet - Premium VIP */}
            <div className="bg-gradient-to-br from-white via-blue-50 to-purple-50 rounded-3xl shadow-2xl p-6 border-2 border-blue-200">
              <div className="flex items-center gap-3 mb-6">
                <div className="w-12 h-12 bg-gradient-to-br from-blue-600 to-purple-600 rounded-2xl flex items-center justify-center shadow-lg">
                  <i className="ri-calculator-line text-2xl text-white"></i>
                </div>
                <h2 className="text-2xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-purple-600">
                  Ödeme Özeti
                </h2>
              </div>
              
              <div className="space-y-4 mb-6">
                <div className="flex justify-between items-center p-3 bg-white/70 backdrop-blur rounded-xl">
                  <span className="text-gray-700 font-medium">Ürün Sayısı</span>
                  <span className="font-bold text-gray-900 text-lg">{cart.reduce((sum, item) => sum + item.quantity, 0)} adet</span>
                </div>
                <div className="flex justify-between items-center p-3 bg-white/70 backdrop-blur rounded-xl">
                  <span className="text-gray-700 font-medium">Ara Toplam</span>
                  <span className="font-bold text-gray-900 text-lg">₺{total.toFixed(2)}</span>
                </div>
                <div className="h-0.5 bg-gradient-to-r from-transparent via-gray-300 to-transparent"></div>
                <div className="flex justify-between items-center p-4 bg-gradient-to-r from-green-600 to-teal-600 rounded-2xl shadow-lg">
                  <span className="text-white font-bold text-xl">TOPLAM</span>
                  <span className="font-bold text-white text-3xl">₺{total.toFixed(2)}</span>
                </div>
              </div>

              <button
                onClick={() => setShowPayment(true)}
                className="w-full bg-gradient-to-r from-green-600 via-teal-600 to-green-600 text-white py-5 rounded-2xl font-bold text-xl hover:shadow-2xl transition-all shadow-lg flex items-center justify-center space-x-3 hover:scale-105 active:scale-95 animate-gradient"
              >
                <div className="w-12 h-12 bg-white/20 backdrop-blur rounded-xl flex items-center justify-center">
                <i className="ri-secure-payment-line text-2xl"></i>
                </div>
                <span>ÖDEME YAP</span>
                <i className="ri-arrow-right-line text-2xl"></i>
              </button>
            </div>

            {/* Hızlı Tarama İpucu */}
            <div className="bg-gradient-to-r from-blue-50 to-purple-50 rounded-2xl p-6 border-2 border-blue-200">
              <div className="flex items-start space-x-4">
                <div className="w-12 h-12 bg-blue-600 rounded-xl flex items-center justify-center flex-shrink-0">
                  <i className="ri-lightbulb-line text-2xl text-white"></i>
                </div>
                <div>
                  <h3 className="font-bold text-gray-900 mb-2">Hızlı Tarama İpucu</h3>
                  <p className="text-gray-600 text-sm">
                    QR Tara sayfasından "Hızlı Sepet" modunu kullanarak sürekli tarama yapabilir ve ürünleri otomatik olarak sepete ekleyebilirsiniz.
                  </p>
                  <Link
                    to="/qr-tara"
                    className="inline-flex items-center space-x-2 text-blue-600 font-medium mt-3 hover:text-blue-700 transition-colors"
                  >
                    <span>Hızlı Taramaya Git</span>
                    <i className="ri-arrow-right-line"></i>
                  </Link>
                </div>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Ödeme Modal - Scrollable */}
      {showPayment && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full my-20">
            <div className="bg-gradient-to-r from-green-600 to-teal-600 text-white p-6 rounded-t-2xl">
              <div className="flex items-center justify-between">
                <h2 className="text-2xl font-bold">Ödeme</h2>
                <button
                  onClick={() => {
                    setShowPayment(false);
                    setCashAmount(0);
                  }}
                  className="text-white hover:bg-white/20 p-2 rounded-lg transition-all"
                >
                  <i className="ri-close-line text-2xl"></i>
                </button>
              </div>
            </div>

            <div className="p-6 space-y-6 max-h-[60vh] overflow-y-auto">
              <div className="text-center py-6 bg-gray-50 rounded-xl">
                <div className="text-sm text-gray-600 mb-2">Toplam Tutar</div>
                <div className="text-4xl font-bold text-green-600">₺{total.toFixed(2)}</div>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-700 mb-3 block">Ödeme Yöntemi</label>
                <div className="grid grid-cols-2 gap-4">
                  <button
                    onClick={() => setPaymentMethod('cash')}
                    className={`p-4 rounded-xl border-2 transition-all ${
                      paymentMethod === 'cash'
                        ? 'border-green-600 bg-green-50'
                        : 'border-gray-200 hover:border-green-300'
                    }`}
                  >
                    <i className={`ri-wallet-line text-3xl mb-2 block ${
                      paymentMethod === 'cash' ? 'text-green-600' : 'text-gray-600'
                    }`}></i>
                    <div className={`font-semibold ${
                      paymentMethod === 'cash' ? 'text-green-600' : 'text-gray-600'
                    }`}>
                      Nakit
                    </div>
                  </button>
                  <button
                    onClick={() => setPaymentMethod('card')}
                    className={`p-4 rounded-xl border-2 transition-all ${
                      paymentMethod === 'card'
                        ? 'border-blue-600 bg-blue-50'
                        : 'border-gray-200 hover:border-blue-300'
                    }`}
                  >
                    <i className={`ri-bank-card-line text-3xl mb-2 block ${
                      paymentMethod === 'card' ? 'text-blue-600' : 'text-gray-600'
                    }`}></i>
                    <div className={`font-semibold ${
                      paymentMethod === 'card' ? 'text-blue-600' : 'text-gray-600'
                    }`}>
                      Kredi Kartı
                    </div>
                  </button>
                </div>
              </div>

              {paymentMethod === 'cash' && (
                <div>
                  <label className="text-sm font-medium text-gray-700 mb-2 block">Verilen Tutar (₺)</label>
                  <input
                    type="number"
                    value={cashAmount || ''}
                    onChange={(e) => setCashAmount(parseFloat(e.target.value) || 0)}
                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-green-500 text-lg font-bold text-center"
                    placeholder="0.00"
                    step="0.01"
                  />
                  
                  {/* Hızlı Tutar Butonları */}
                  <div className="mt-3">
                    <div className="text-xs font-medium text-gray-600 mb-2">Hızlı Tutar:</div>
                    <div className="grid grid-cols-4 gap-2">
                      {[10, 20, 50, 100, 200, 500].map((amount) => (
                        <button
                          key={amount}
                          onClick={() => setCashAmount(amount)}
                          className="px-3 py-2 bg-green-100 text-green-700 rounded-lg hover:bg-green-200 transition-all font-semibold text-sm"
                        >
                          ₺{amount}
                        </button>
                      ))}
                      <button
                        onClick={() => setCashAmount(total)}
                        className="px-3 py-2 bg-blue-100 text-blue-700 rounded-lg hover:bg-blue-200 transition-all font-semibold text-sm"
                      >
                        Tam
                      </button>
                      <button
                        onClick={() => setCashAmount(Math.ceil(total / 10) * 10)}
                        className="px-3 py-2 bg-purple-100 text-purple-700 rounded-lg hover:bg-purple-200 transition-all font-semibold text-sm"
                      >
                        Yukarla
                      </button>
                    </div>
                  </div>
                  
                  {/* Para Üstü veya Yetersiz Uyarısı */}
                  {cashAmount > 0 && (
                    <div className={`mt-3 p-4 rounded-xl border-2 ${
                      cashAmount >= total 
                        ? 'bg-green-50 border-green-300' 
                        : 'bg-red-50 border-red-300'
                    }`}>
                      {cashAmount >= total ? (
                        <>
                          <div className="flex items-center justify-between mb-1">
                            <span className="text-sm text-gray-700">Verilen</span>
                            <span className="font-bold text-gray-900">₺{cashAmount.toFixed(2)}</span>
                          </div>
                          <div className="flex items-center justify-between mb-1">
                            <span className="text-sm text-gray-700">Toplam</span>
                            <span className="font-bold text-gray-900">₺{total.toFixed(2)}</span>
                          </div>
                          <div className="h-px bg-green-300 my-2"></div>
                          <div className="flex items-center justify-between">
                            <span className="text-base font-bold text-green-800">🎉 Para Üstü</span>
                            <span className="text-2xl font-bold text-green-600">₺{(cashAmount - total).toFixed(2)}</span>
                          </div>
                        </>
                      ) : (
                        <div className="flex items-center gap-2">
                          <i className="ri-error-warning-line text-2xl text-red-600"></i>
                          <div>
                            <div className="text-sm font-bold text-red-900">Yetersiz Tutar</div>
                            <div className="text-xs text-red-700">En az ₺{total.toFixed(2)} gerekli</div>
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}

              <div className="flex gap-3 pt-2">
                <button
                  onClick={() => {
                    setShowPayment(false);
                    setCashAmount(0);
                  }}
                  className="flex-1 bg-gray-100 text-gray-700 py-4 px-6 rounded-xl font-bold text-lg hover:bg-gray-200 transition-all"
                >
                  İptal
                </button>
                <button
                  onClick={completePayment}
                  disabled={loading}
                  className={`flex-1 py-4 px-6 rounded-xl font-bold text-lg transition-all shadow-lg ${
                    loading 
                      ? 'bg-gray-400 cursor-not-allowed opacity-50' 
                      : 'bg-gradient-to-r from-green-600 to-teal-600 text-white hover:from-green-700 hover:to-teal-700'
                  }`}
                >
                  {loading ? (
                    <span className="flex items-center justify-center gap-2">
                      <i className="ri-loader-4-line animate-spin"></i>
                      İşleniyor...
                    </span>
                  ) : (
                    'Onayla'
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Toast Bildirimleri */}
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}

      {/* Fiş Yazdırma */}
      {showReceipt && lastSale && (
        <ReceiptPrinter
          cart={lastSale.items}
          total={lastSale.total}
          paymentMethod={lastSale.paymentMethod}
          cashAmount={lastSale.cashAmount}
          change={lastSale.change}
          onClose={() => {
            setShowReceipt(false);
            setLastSale(null);
          }}
        />
      )}

      {/* Navigation */}
      <Navigation />
    </div>
  );
}


