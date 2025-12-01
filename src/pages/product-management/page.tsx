import { useState, useEffect, useRef } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { PRODUCT_CATEGORIES } from '../../constants';
import { Navigation, ExportImport, PremiumButton, ImagePicker, Toast, AdvancedScanner } from '../../components';
import { ExportService } from '../../services';
import { useProducts } from '../../hooks/useDatabase';
import type { Product as DBProduct } from '../../services/DatabaseService';

interface Product extends DBProduct {
  qrCode?: string;
}

export default function ProductManagement() {
  const { products: dbProducts, addProduct, updateProduct, deleteProduct: dbDeleteProduct, reload } = useProducts();
  const [products, setProducts] = useState<Product[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [filter, setFilter] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'warning' | 'info' } | null>(null);
  const [showScanner, setShowScanner] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    barcode: '',
    price: 0,
    stock: 0,
    category: 'Genel',
    description: '',
    image: '',
  });

  useEffect(() => {
    // Convert DB products to local format
    setProducts(dbProducts as Product[]);
  }, [dbProducts]);

  // URL parametrelerini React Router ile al (SPA uyumlu)
  const location = useLocation();
  
  useEffect(() => {
    // URL'den barkod parametresi kontrolü - React Router kullanarak
    const urlParams = new URLSearchParams(location.search);
    const addBarcode = urlParams.get('addBarcode');
    
    // ✅ VALİDASYON: undefined veya 'undefined' string kontrolü
    if (addBarcode && addBarcode !== 'undefined' && addBarcode !== 'null' && addBarcode.trim() !== '') {
      try {
        const decodedBarcode = decodeURIComponent(addBarcode);
        
        // Decode sonrası da kontrol et
        if (decodedBarcode && decodedBarcode !== 'undefined' && decodedBarcode !== 'null') {
          console.log('📦 Ürün ekleme modalı açılıyor, barkod:', decodedBarcode);
          
          // Modal'ı aç ve barkodu doldur
          setFormData({
            name: '',
            barcode: decodedBarcode,
            price: 0,
            stock: 0,
            category: 'Genel',
            description: '',
            image: '',
          });
          setEditingProduct(null);
          setShowModal(true);
          
          // Toast bildirim
          setToast({
            message: '📦 Barkod eklendi! Ürün bilgilerini doldurun',
            type: 'success'
          });
        }
      } catch (error) {
        console.error('❌ Barkod decode hatası:', error);
      }
      
      // URL'i temizle
      window.history.replaceState({}, '', '/urun-yonetimi');
    }
  }, [location.search]);

  const saveProduct = async () => {
    if (!formData.name || !formData.barcode) {
      setToast({
        message: 'Ürün adı ve barkod zorunludur!',
        type: 'error'
      });
      return;
    }

    try {
      console.log('🔵 Ürün kaydediliyor...', formData);
      
      // 1️⃣ ÖNCE BARKOD KONTROLÜ YAP
      const databaseService = (await import('../../services/DatabaseService')).default;
      const existingProduct = await databaseService.getProductByBarcode(formData.barcode);
      
      if (existingProduct && !editingProduct) {
        // Barkod zaten var VE yeni ürün ekliyoruz (düzenleme değil)
        console.log('⚠️ Barkod zaten mevcut, güncelleme yapılacak:', existingProduct);
        setToast({
          message: `⚠️ Bu barkod zaten kayıtlı! "${existingProduct.name}" ürünü güncelleniyor...`,
          type: 'warning'
        });
        
        // Mevcut ürünü güncelle
        const updatedProduct: DBProduct = {
          id: existingProduct.id,
          name: formData.name,
          barcode: formData.barcode,
          price: formData.price,
          stock: formData.stock,
          category: formData.category,
          description: formData.description,
          image: formData.image,
          createdAt: existingProduct.createdAt,
        };
        
        await updateProduct(existingProduct.id, updatedProduct);
        console.log('✅ Mevcut ürün güncellendi!');
        setToast({
          message: 'Ürün başarıyla güncellendi!',
          type: 'success'
        });
      } else if (editingProduct) {
        // Düzenleme modu
        const product: DBProduct = {
          id: editingProduct.id,
          name: formData.name,
          barcode: formData.barcode,
          price: formData.price,
          stock: formData.stock,
          category: formData.category,
          description: formData.description,
          image: formData.image,
          createdAt: editingProduct.createdAt,
        };
        
        await updateProduct(product.id, product);
        console.log('✅ Ürün güncellendi!');
        setToast({
          message: 'Ürün başarıyla güncellendi!',
          type: 'success'
        });
      } else {
        // Yeni ürün ekleme
        const product: DBProduct = {
          id: `prod_${Date.now()}`,
          name: formData.name,
          barcode: formData.barcode,
          price: formData.price,
          stock: formData.stock,
          category: formData.category,
          description: formData.description,
          image: formData.image,
          createdAt: Date.now(),
        };
        
        console.log('🆕 Yeni ürün ekleniyor...');
        await addProduct(product);
        console.log('✅ Yeni ürün eklendi!');
        setToast({
          message: 'Ürün başarıyla eklendi!',
          type: 'success'
        });
      }
      
      resetForm();
      setShowModal(false);
      await reload();
    } catch (error: any) {
      // ✅ DETAYLI ERROR LOGGING!
      console.error('❌ ÜRÜN KAYDETME HATASI:', {
        error: error,
        errorMessage: error?.message || 'Bilinmeyen hata',
        errorStack: error?.stack,
        formData: formData,
        editingProduct: editingProduct
      });
      
      // Kullanıcıya anlaşılır hata mesajı
      let errorMessage = 'Ürün kaydedilirken hata oluştu!';
      if (error?.message?.includes('UNIQUE constraint')) {
        errorMessage = 'Bu barkod zaten kayıtlı! Lütfen farklı bir barkod kullanın.';
      } else if (error?.message) {
        errorMessage = error.message;
      }
      
      setToast({
        message: `❌ ${errorMessage}`,
        type: 'error'
      });
    }
  };

  const handleDeleteProduct = async (id: string) => {
    if (confirm('Bu ürünü silmek istediğinizden emin misiniz?')) {
      try {
        console.log('🗑️ Ürün siliniyor:', id);
        await dbDeleteProduct(id);
        console.log('✅ Ürün silindi!');
        setToast({
          message: 'Ürün silindi!',
          type: 'success'
        });
        await reload();
      } catch (error: any) {
        console.error('❌ ÜRÜN SİLME HATASI:', {
          error: error,
          errorMessage: error?.message,
          productId: id
        });
        setToast({
          message: `❌ ${error?.message || 'Ürün silinirken hata oluştu!'}`,
          type: 'error'
        });
      }
    }
  };

  const editProduct = (product: Product) => {
    setEditingProduct(product);
    setFormData({
      name: product.name,
      barcode: product.barcode,
      price: product.price,
      stock: product.stock,
      category: product.category,
      description: product.description || '',
      image: product.image || '',
    });
    setShowModal(true);
  };

  const resetForm = () => {
    setFormData({
      name: '',
      barcode: '',
      price: 0,
      stock: 0,
      category: 'Genel',
      description: '',
      image: '',
    });
    setEditingProduct(null);
  };

  const generateBarcode = () => {
    const barcode = Math.floor(Math.random() * 9000000000000) + 1000000000000;
    setFormData({ ...formData, barcode: barcode.toString() });
  };

  const exportProducts = async () => {
    if (products.length === 0) {
      setToast({
        message: 'Dışa aktarılacak ürün bulunamadı',
        type: 'warning'
      });
      return;
    }
    
    try {
      await ExportService.exportAsJSON(products, 'urunler');
      setToast({
        message: 'Ürünler başarıyla dışa aktarıldı',
        type: 'success'
      });
    } catch (error) {
      setToast({
        message: 'Dışa aktarma hatası: ' + (error as Error).message,
        type: 'error'
      });
    }
  };

  const importProducts = async (data: any) => {
    try {
      if (!Array.isArray(data)) {
        setToast({ message: 'Geçersiz veri formatı!', type: 'error' });
        return false;
      }
      
      const importedProducts = data as Product[];
      for (const product of importedProducts) {
        // createdAt format kontrolü ve düzeltmesi
        let createdAt = product.createdAt;
        
        // String kontrolü
        if (typeof createdAt === 'string') {
          try {
            // ISO format string kontrolü (2025-10-24T21:36:11.741Z)
            if (/^\d{4}-\d{2}-\d{2}T/.test(createdAt)) {
              createdAt = new Date(createdAt).getTime();
            } else if (!isNaN(Number(createdAt))) {
              // String olarak timestamp
              createdAt = Number(createdAt);
            }
          } catch (err) {
            console.warn('Tarih formatı dönüştürülemedi:', err);
            createdAt = Date.now();
          }
        }
        
        // Geçerli bir timestamp değil veya yoksa, şimdi oluştur
        if (!createdAt || typeof createdAt !== 'number' || isNaN(createdAt)) {
          createdAt = Date.now();
        }
        
        const dbProduct: DBProduct = {
          ...product,
          id: product.id || `prod_${Date.now()}_${Math.random()}`,
          createdAt: createdAt,
        };
        
        try {
          await addProduct(dbProduct);
        } catch (err) {
          console.warn(`Ürün içe aktarma hatası (devam ediliyor): ${(err as Error).message}`);
          // Tekil hatalarda devam et, tüm import'u kırma
        }
      }
      await reload();
      setToast({
        message: `${importedProducts.length} ürün içe aktarıldı!`,
        type: 'success'
      });
      return true;
    } catch (error) {
      setToast({
        message: 'İçe aktarma hatası: ' + (error as Error).message,
        type: 'error'
      });
      return false;
    }
  };

  // 📸 TARAYICI - AdvancedScanner kullan
  const handleScan = (code: string, type: 'qr' | 'barcode') => {
    // Modal açıksa formData'ya, değilse searchQuery'ye yaz
    if (showModal) {
      setFormData({ ...formData, barcode: code });
    } else {
      setSearchQuery(code);
    }
    
    setShowScanner(false);
    setToast({ message: `${type === 'qr' ? 'QR' : 'Barkod'} tarandı: ${code}`, type: 'success' });
  };

  const openScanner = () => {
    setShowScanner(true);
  };

  const closeScanner = () => {
    setShowScanner(false);
  };
  
  // Filtreleme ve Arama
  const filteredProducts = products.filter((product) => {
    // Kategori filtresi
    if (filter !== 'all' && product.category !== filter) return false;
    
    // Arama sorgusu
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      return (
        product.name.toLowerCase().includes(query) ||
        product.barcode.toLowerCase().includes(query) || // Barkod içeriğine göre filtreleme
        product.description?.toLowerCase().includes(query) ||
        product.id.toLowerCase().includes(query) // ID'ye göre de arama
      );
    }
    
    return true;
  });
  
  // Barkod/QR değerine göre kesin eşleşme varsa, o ürünü öncelikli göster
  const exactBarcodeMatch = searchQuery ? 
    products.find(product => product.barcode.toLowerCase() === searchQuery.toLowerCase()) : null;
  
  // Eğer tam eşleşme varsa, filtrelenmiş ürünleri düzenle (tam eşleşen en başa)
  const sortedFilteredProducts = exactBarcodeMatch 
    ? [exactBarcodeMatch, ...filteredProducts.filter(p => p.id !== exactBarcodeMatch.id)]
    : filteredProducts;
    
  // Barkod eşleşme durumu için highlight efekti
  const isExactMatch = Boolean(exactBarcodeMatch);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 pb-20">
      {/* Header */}
      <div className="bg-gradient-to-r from-orange-600 to-red-600 text-white py-6 px-6 sticky top-0 z-50 shadow-lg">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Link to="/" className="text-white hover:bg-white/20 p-2 rounded-lg transition-all">
              <i className="ri-arrow-left-line text-2xl"></i>
            </Link>
            <div>
              <h1 className="text-2xl font-bold">Ürün Yönetimi</h1>
              <p className="text-orange-100 text-sm">Ürünleri ekleyin, düzenleyin ve yönetin</p>
            </div>
          </div>
          <div className="flex gap-2">
            <span className="bg-white/20 px-4 py-2 rounded-lg font-semibold">
              {products.length} Ürün
            </span>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-6 py-6 space-y-6">
        {/* Filtreleme ve Arama */}
        <div className="bg-white rounded-xl shadow-lg p-4">
          <div className="mb-4">
            <div className="flex gap-2">
              <div className="relative flex-1">
                <i className="ri-search-line absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400"></i>
                <input
                  type="text"
                  placeholder="Ürün ara..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-orange-500 pl-10"
                />
                {searchQuery && (
                  <button 
                    onClick={() => setSearchQuery('')} 
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    <i className="ri-close-line"></i>
                  </button>
                )}
              </div>
              
              <button
                onClick={openScanner}
                className="flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl px-4 py-3 transition-all"
                title="Barkod Tara"
              >
                <i className="ri-scan-line text-lg"></i>
              </button>
              
            </div>
          </div>
          
          <div className="flex gap-2 overflow-x-auto scrollbar-hide">
            <button
              onClick={() => setFilter('all')}
              className={`px-4 py-2 rounded-lg font-medium whitespace-nowrap ${
                filter === 'all' ? 'bg-orange-600 text-white' : 'bg-gray-100 text-gray-600'
              }`}
            >
              Tümü
            </button>
            {PRODUCT_CATEGORIES.map(cat => (
              <button
                key={cat}
                onClick={() => setFilter(cat)}
                className={`px-4 py-2 rounded-lg font-medium whitespace-nowrap ${
                  filter === cat ? 'bg-orange-600 text-white' : 'bg-gray-100 text-gray-600'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        {/* Ürün Listesi */}
        <div className="bg-white rounded-xl shadow-lg p-6 pb-8">
          <h3 className="text-lg font-bold mb-6 flex items-center justify-between flex-wrap">
            <div className="flex items-center">
              <i className="ri-layout-grid-line mr-2 text-orange-600"></i>
              Ürün Listesi
            </div>
            <div className="flex gap-2 flex-wrap">
              <div className="dropdown relative">
                <button
                  className="bg-orange-100 text-orange-700 px-3 py-1.5 rounded-lg hover:bg-orange-200 transition-all text-sm font-medium flex items-center"
                  onClick={() => {
                    const dropdown = document.getElementById('exportDropdown');
                    if (dropdown) {
                      dropdown.classList.toggle('hidden');
                    }
                  }}
                >
                  <i className="ri-download-line mr-1"></i>
                  Dışa Aktar
                  <i className="ri-arrow-down-s-line ml-1"></i>
                </button>
                <div id="exportDropdown" className="absolute hidden z-[5] right-0 mt-1 bg-white rounded-lg shadow-xl border border-gray-200 w-48">
                  <ul className="py-1">
                    <li>
                      <button
                        className="w-full text-left px-4 py-2 hover:bg-gray-100 text-sm flex items-center gap-2"
                        onClick={async () => {
                          document.getElementById('exportDropdown')?.classList.add('hidden');
                          try {
                            await ExportService.exportAsCSV(products, 'urunler');
                            setToast({
                              message: 'Ürünler CSV olarak dışa aktarıldı',
                              type: 'success'
                            });
                          } catch (error) {
                            setToast({
                              message: 'Dışa aktarma hatası: ' + (error as Error).message,
                              type: 'error'
                            });
                          }
                        }}
                      >
                        <i className="ri-file-excel-2-line text-green-600"></i>
                        CSV Olarak İndir
                      </button>
                    </li>
                    <li>
                      <button
                        className="w-full text-left px-4 py-2 hover:bg-gray-100 text-sm flex items-center gap-2"
                        onClick={async () => {
                          document.getElementById('exportDropdown')?.classList.add('hidden');
                          try {
                            await ExportService.exportAsJSON(products, 'urunler');
                            setToast({
                              message: 'Ürünler JSON olarak dışa aktarıldı',
                              type: 'success'
                            });
                          } catch (error) {
                            setToast({
                              message: 'Dışa aktarma hatası: ' + (error as Error).message,
                              type: 'error'
                            });
                          }
                        }}
                      >
                        <i className="ri-file-code-line text-blue-600"></i>
                        JSON Olarak İndir
                      </button>
                    </li>
                    <li>
                      <button
                        className="w-full text-left px-4 py-2 hover:bg-gray-100 text-sm flex items-center gap-2"
                        onClick={async () => {
                          document.getElementById('exportDropdown')?.classList.add('hidden');
                          try {
                            await ExportService.shareContent('1STQR Ürünler', 'Ürün listesi');
                            setToast({
                              message: 'Paylaşım ekranı açıldı',
                              type: 'info'
                            });
                          } catch (error) {
                            setToast({
                              message: 'Paylaşım hatası: ' + (error as Error).message,
                              type: 'error'
                            });
                          }
                        }}
                      >
                        <i className="ri-share-line text-purple-600"></i>
                        Paylaş
                      </button>
                    </li>
                  </ul>
                </div>
              </div>
              <ExportImport 
                storageKey="products" 
                dataLabel="Ürünler"
                onExport={async () => products}
                onImport={importProducts}
              />
            </div>
          </h3>

          {/* Tam Eşleşme Sonucu Bildirimi */}
          {isExactMatch && (
            <div className="bg-green-50 border border-green-200 rounded-xl p-4 mb-6 flex items-center shadow-sm">
              <div className="bg-green-100 rounded-full p-2 mr-4">
                <i className="ri-checkbox-circle-line text-green-600 text-xl"></i>
              </div>
              <div>
                <h4 className="font-bold text-green-800">Tam Eşleşme Bulundu!</h4>
                <p className="text-green-700 text-sm">
                  "{searchQuery}" barkod/QR içeriği ile tam eşleşen ürün: {exactBarcodeMatch?.name}
                </p>
              </div>
            </div>
          )}
          
          {/* Arama Sonuçları - Hiç Sonuç Yoksa */}
          {sortedFilteredProducts.length === 0 ? (
            <div className="text-center py-12">
              <i className="ri-box-3-line text-6xl text-gray-300 mb-4"></i>
              <h3 className="text-xl font-bold text-gray-900 mb-2">
                {searchQuery ? 'Arama sonucu bulunamadı' : 'Henüz ürün yok'}
              </h3>
              <p className="text-gray-600 mb-6">
                {searchQuery 
                  ? `"${searchQuery}" içeren ürün bulunamadı. Yeni ürün eklemek ister misiniz?` 
                  : 'İlk ürününüzü ekleyerek başlayın'}
              </p>
              <div className="flex flex-col md:flex-row gap-3 justify-center">
                <button
                  onClick={() => {
                    resetForm();
                    if (searchQuery) {
                      setFormData(prev => ({...prev, barcode: searchQuery}));
                    }
                    setShowModal(true);
                  }}
                  className="bg-gradient-to-r from-orange-600 to-red-600 text-white px-8 py-3 rounded-xl font-semibold hover:from-orange-700 hover:to-red-700 transition-all shadow-lg"
                >
                  <i className="ri-add-line mr-2"></i>
                  {searchQuery ? 'Bu Barkodla Ürün Ekle' : 'Ürün Ekle'}
                </button>
                
                {searchQuery && (
                  <button
                    onClick={() => setSearchQuery('')}
                    className="bg-gray-100 text-gray-700 px-8 py-3 rounded-xl font-semibold hover:bg-gray-200 transition-all"
                  >
                    <i className="ri-close-line mr-2"></i>
                    Aramayı Temizle
                  </button>
                )}
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-3 md:gap-6">
              {sortedFilteredProducts.map(product => {
                const isHighlighted = exactBarcodeMatch?.id === product.id;
                
                return (
                <div 
                  key={product.id} 
                  className={`bg-white rounded-xl shadow-lg overflow-hidden hover:shadow-xl transition-all ${
                    isHighlighted ? 'ring-2 ring-green-500 relative z-10' : ''
                  }`}
                >
                  {isHighlighted && (
                    <div className="absolute top-0 right-0 bg-green-500 text-white text-xs px-3 py-1 font-bold z-10">
                      Tam Eşleşme
                    </div>
                  )}
                  <div className="p-3 md:p-4">
                    {/* Ürün Görseli */}
                    {product.image ? (
                      <div className="relative w-full aspect-square mb-2 rounded-lg overflow-hidden">
                        <img src={product.image} alt={product.name} className="w-full h-full object-cover" />
                        <div className="absolute top-2 right-2 bg-white/90 backdrop-blur px-2 py-1 rounded-full">
                          <span className={`text-xs font-bold ${
                            product.stock > 0 ? 'text-green-600' : 'text-red-600'
                          }`}>
                            {product.stock > 0 ? `${product.stock} adet` : 'Stok Yok'}
                          </span>
                        </div>
                        
                        {/* Barkod Gösterimi */}
                        <div className="absolute bottom-0 left-0 right-0 bg-black/60 backdrop-blur-sm text-white py-1 px-2">
                          <div className="text-xs font-mono truncate">
                            <i className="ri-barcode-line mr-1"></i> {product.barcode}
                          </div>
                        </div>
                      </div>
                    ) : (
                      <div className="relative w-full aspect-square mb-2 bg-gradient-to-br from-gray-100 to-gray-200 rounded-lg flex items-center justify-center">
                        <i className="ri-image-line text-4xl text-gray-400"></i>
                        <div className="absolute top-2 right-2 bg-white/90 backdrop-blur px-2 py-1 rounded-full">
                          <span className={`text-xs font-bold ${
                            product.stock > 0 ? 'text-green-600' : 'text-red-600'
                          }`}>
                            {product.stock > 0 ? `${product.stock}` : '0'}
                          </span>
                        </div>
                        
                        {/* Barkod Gösterimi */}
                        <div className="absolute bottom-0 left-0 right-0 bg-black/60 backdrop-blur-sm text-white py-1 px-2">
                          <div className="text-xs font-mono truncate">
                            <i className="ri-barcode-line mr-1"></i> {product.barcode}
                          </div>
                        </div>
                      </div>
                    )}
                    
                    {/* Ürün Bilgileri */}
                    <div className="space-y-2">
                      <h3 className="text-sm md:text-base font-bold text-gray-900 truncate">{product.name}</h3>
                      <div className="text-lg md:text-xl font-bold text-orange-600">₺{product.price.toFixed(2)}</div>
                      <span className="inline-block px-2 py-1 bg-orange-100 text-orange-600 rounded-md text-xs font-medium">
                        {product.category}
                      </span>
                    </div>

                    {/* Butonlar */}
                    <div className="flex gap-2 mt-3">
                      <button
                        onClick={() => editProduct(product)}
                        className="flex-1 bg-blue-50 text-blue-600 py-2 px-2 rounded-lg hover:bg-blue-100 transition-all text-xs md:text-sm font-medium"
                      >
                        <i className="ri-edit-line"></i>
                      </button>
                      <button
                        onClick={() => handleDeleteProduct(product.id)}
                        className="flex-1 bg-red-50 text-red-600 py-2 px-2 rounded-lg hover:bg-red-100 transition-all text-xs md:text-sm font-medium"
                      >
                        <i className="ri-delete-bin-line"></i>
                      </button>
                    </div>
                  </div>
                </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl shadow-2xl max-w-xl w-full my-4 max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-gradient-to-r from-orange-600 to-red-600 text-white p-6 rounded-t-2xl">
              <div className="flex items-center justify-between">
                <h2 className="text-2xl font-bold">
                  {editingProduct ? 'Ürün Düzenle' : 'Yeni Ürün Ekle'}
                </h2>
                <button
                  onClick={() => {
                    setShowModal(false);
                    resetForm();
                  }}
                  className="text-white hover:bg-white/20 p-2 rounded-lg transition-all"
                >
                  <i className="ri-close-line text-2xl"></i>
                </button>
              </div>
            </div>

            <div className="p-6 space-y-4">
              <div>
                <label className="text-sm font-medium text-gray-700 mb-2 block">Ürün Adı *</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-orange-500"
                  placeholder="Ürün adını girin"
                />
              </div>

              <div>
                <label className="text-sm font-medium text-gray-700 mb-2 block">Barkod *</label>
                <div className="space-y-2">
                  <input
                    type="text"
                    value={formData.barcode}
                    onChange={(e) => setFormData({ ...formData, barcode: e.target.value })}
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-orange-500"
                    placeholder="Barkod numarası"
                  />
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      onClick={openScanner}
                      className="bg-blue-100 text-blue-600 px-4 py-2.5 rounded-xl font-medium hover:bg-blue-200 transition-all flex items-center justify-center gap-2"
                    >
                      <i className="ri-scan-line"></i>
                      <span className="text-sm">Tara</span>
                    </button>
                  <button
                    onClick={generateBarcode}
                      className="bg-orange-100 text-orange-600 px-4 py-2.5 rounded-xl font-medium hover:bg-orange-200 transition-all flex items-center justify-center gap-2"
                  >
                    <i className="ri-refresh-line"></i>
                      <span className="text-sm">Oluştur</span>
                  </button>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-gray-700 mb-2 block">Fiyat (₺)</label>
                  <input
                    type="number"
                    value={formData.price}
                    onChange={(e) => setFormData({ ...formData, price: parseFloat(e.target.value) || 0 })}
                    onFocus={(e) => {
                      // Tıklayınca 0 ise temizle
                      if (formData.price === 0) {
                        e.target.select();
                      }
                    }}
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-orange-500"
                    placeholder="0.00"
                    step="0.01"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-700 mb-2 block">Stok</label>
                  <input
                    type="number"
                    value={formData.stock}
                    onChange={(e) => setFormData({ ...formData, stock: parseInt(e.target.value) || 0 })}
                    onFocus={(e) => {
                      // Tıklayınca 0 ise temizle
                      if (formData.stock === 0) {
                        e.target.select();
                      }
                    }}
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-orange-500"
                    placeholder="0"
                  />
                </div>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-700 mb-2 block">Kategori</label>
                <select
                  value={formData.category}
                  onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                  className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-orange-500"
                >
                  {PRODUCT_CATEGORIES.map(cat => (
                    <option key={cat} value={cat}>{cat}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-700 mb-2 block">Açıklama</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-orange-500 resize-none"
                  rows={3}
                  placeholder="Ürün açıklaması (opsiyonel)"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Ürün Görseli
                </label>
                <ImagePicker
                  onImageSelect={(imageData) => setFormData({ ...formData, image: imageData || '' })}
                  initialImage={formData.image}
                  className="mt-2"
                />
              </div>

              <div className="flex gap-3 pt-4 pb-24 sticky bottom-0 bg-white">
                <PremiumButton
                  onClick={() => {
                    setShowModal(false);
                    resetForm();
                  }}
                  variant="secondary"
                  className="flex-1"
                >
                  İptal
                </PremiumButton>
                <PremiumButton
                  onClick={saveProduct}
                  variant="warning"
                  className="flex-1"
                  icon={<i className="ri-save-line"></i>}
                >
                  {editingProduct ? 'Güncelle' : 'Kaydet'}
                </PremiumButton>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Footer - Floating Action Button */}
      <div className="fixed bottom-24 right-6">
        <button
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="w-16 h-16 bg-gradient-to-r from-orange-500 to-red-500 rounded-full flex items-center justify-center shadow-lg hover:shadow-2xl transition-all hover:scale-110"
        >
          <i className="ri-add-line text-white text-2xl"></i>
        </button>
      </div>

      {/* Toast Bildirimleri */}
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}
      
      {/* Tarama Modalı */}
      {showScanner && (
        <AdvancedScanner
          mode="barcode"
          onScan={handleScan}
          onClose={closeScanner}
        />
      )}

      {/* Navigation */}
      <Navigation />
    </div>
  );
}
