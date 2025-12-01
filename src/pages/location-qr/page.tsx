import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import QRCode from 'qrcode';
import Navigation from '../../components/Navigation';

export default function LocationQR() {
  const [location, setLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [accuracy, setAccuracy] = useState<number>(0);
  const [loading, setLoading] = useState(false);
  const [address, setAddress] = useState('');
  const [qrImage, setQrImage] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  const popularLocations = [
    { name: 'İstanbul', lat: 41.0082, lng: 28.9784 },
    { name: 'Ankara', lat: 39.9334, lng: 32.8597 },
    { name: 'İzmir', lat: 38.4192, lng: 27.1287 },
    { name: 'Antalya', lat: 36.8969, lng: 30.7133 },
    { name: 'Bursa', lat: 40.1826, lng: 29.0665 },
    { name: 'Trabzon', lat: 41.0027, lng: 39.7168 },
  ];

  useEffect(() => {
    if (location) {
      generateQR();
    }
  }, [location]);

  const getCurrentLocation = () => {
    if (!navigator.geolocation) {
      alert('Tarayıcınız konum hizmetlerini desteklemiyor');
      return;
    }

    setLoading(true);
    
    // Yüksek doğruluklu konum alma
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const coords = {
          lat: parseFloat(position.coords.latitude.toFixed(8)),
          lng: parseFloat(position.coords.longitude.toFixed(8)),
        };
        setLocation(coords);
        setAccuracy(Math.round(position.coords.accuracy));
        setLoading(false);
        
        // Reverse geocoding (adres bul)
        fetch(`/api/nominatim/reverse?lat=${coords.lat}&lon=${coords.lng}&format=json`)
          .then(res => res.json())
          .then(data => {
            setAddress(data.display_name || 'Adres bulunamadı');
          })
          .catch(() => {
            setAddress('Adres alınamadı');
          });
      },
      (error) => {
        setLoading(false);
        let errorMsg = 'Konum alınamadı';
        switch(error.code) {
          case error.PERMISSION_DENIED:
            errorMsg = 'Konum izni reddedildi. Lütfen tarayıcı ayarlarından konum iznini açın.';
            break;
          case error.POSITION_UNAVAILABLE:
            errorMsg = 'Konum bilgisi kullanılamıyor.';
            break;
          case error.TIMEOUT:
            errorMsg = 'Konum alma zaman aşımına uğradı. Tekrar deneyin.';
            break;
        }
        alert(errorMsg);
      },
      {
        enableHighAccuracy: true, // GPS kullan
        timeout: 15000, // 15 saniye bekle
        maximumAge: 0, // Cache kullanma, her zaman yeni konum al
      }
    );
  };

  const searchLocation = async () => {
    if (!searchQuery.trim()) return;

    setLoading(true);
    try {
      const response = await fetch(
        `/api/nominatim/search?q=${encodeURIComponent(searchQuery)}&format=json&limit=1`
      );
      const data = await response.json();
      
      if (data && data.length > 0) {
        const coords = {
          lat: parseFloat(parseFloat(data[0].lat).toFixed(8)),
          lng: parseFloat(parseFloat(data[0].lon).toFixed(8)),
        };
        setLocation(coords);
        setAddress(data[0].display_name);
        setAccuracy(0);
      } else {
        alert('Konum bulunamadı');
      }
    } catch (error) {
      alert('Arama hatası');
    } finally {
      setLoading(false);
    }
  };

  const selectPopularLocation = (loc: any) => {
    setLocation({ lat: loc.lat, lng: loc.lng });
    setAddress(loc.name);
    setAccuracy(0);
  };

  const generateQR = async () => {
    if (!location) return;

    const geoUri = `geo:${location.lat},${location.lng}`;
    const qr = await QRCode.toDataURL(geoUri, {
      width: 300,
      margin: 2,
      color: {
        dark: '#3b82f6',
        light: '#ffffff',
      },
    });
    setQrImage(qr);
  };

  const downloadQR = () => {
    if (!qrImage) return;
    
    const link = document.createElement('a');
    link.download = `location-qr-${Date.now()}.png`;
    link.href = qrImage;
    link.click();
  };

  const shareQR = async () => {
    if (!qrImage || !location || !('share' in navigator)) return;
    
    try {
      const blob = await (await fetch(qrImage)).blob();
      const file = new File([blob], 'location-qr.png', { type: 'image/png' });
      await navigator.share({
        files: [file],
        title: 'Konum QR Kod',
        text: `Konum: ${location.lat}, ${location.lng}`,
      });
    } catch (error) {
      console.error('Paylaşım hatası:', error);
    }
  };

  const openInMaps = () => {
    if (!location) return;
    window.open(`https://www.google.com/maps?q=${location.lat},${location.lng}`, '_blank');
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 pb-20">
      {/* Header */}
      <div className="bg-gradient-to-r from-indigo-600 to-purple-600 text-white py-6 px-6 sticky top-0 z-10 shadow-lg">
        <div className="flex items-center space-x-4">
          <Link to="/" className="text-white">
            <i className="ri-arrow-left-line text-2xl"></i>
          </Link>
          <div>
            <h1 className="text-2xl font-bold">Konum QR Kod</h1>
            <p className="text-indigo-100 text-sm">GPS konumunuzu QR'a dönüştürün</p>
          </div>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-6 py-6 space-y-6">
        {/* Konum Al */}
        <div className="bg-white rounded-2xl shadow-lg p-6">
          <h2 className="text-lg font-bold text-gray-900 mb-4">Konum Seç</h2>
          
          <button
            onClick={getCurrentLocation}
            disabled={loading}
            className="w-full bg-gradient-to-r from-indigo-600 to-purple-600 text-white py-4 rounded-xl font-semibold hover:from-indigo-700 hover:to-purple-700 transition-all shadow-lg hover:shadow-xl flex items-center justify-center space-x-2 mb-4 disabled:opacity-50"
          >
            {loading ? (
              <>
                <div className="animate-spin w-5 h-5 border-2 border-white border-t-transparent rounded-full"></div>
                <span>Konum alınıyor...</span>
              </>
            ) : (
              <>
                <i className="ri-map-pin-line text-2xl"></i>
                <span>Mevcut Konumumu Kullan</span>
              </>
            )}
          </button>

          {/* Arama */}
          <div className="mb-4">
            <div className="flex gap-2">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && searchLocation()}
                placeholder="Şehir, adres veya yer ara..."
                className="flex-1 px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500"
              />
              <button
                onClick={searchLocation}
                className="bg-indigo-100 text-indigo-600 px-6 py-3 rounded-xl font-semibold hover:bg-indigo-200 transition-all"
              >
                <i className="ri-search-line"></i>
              </button>
            </div>
          </div>

          {/* Popüler Konumlar */}
          <div>
            <h3 className="text-sm font-medium text-gray-700 mb-3">Popüler Konumlar</h3>
            <div className="grid grid-cols-3 gap-2">
              {popularLocations.map((loc) => (
                <button
                  key={loc.name}
                  onClick={() => selectPopularLocation(loc)}
                  className="p-3 bg-gray-50 hover:bg-indigo-50 border border-gray-200 hover:border-indigo-300 rounded-lg text-sm font-medium text-gray-700 hover:text-indigo-600 transition-all"
                >
                  {loc.name}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Konum Bilgisi */}
        {location && (
          <div className="bg-white rounded-2xl shadow-lg p-6 space-y-4">
            <h2 className="text-lg font-bold text-gray-900 mb-4">Konum Bilgisi</h2>
            
            <div className="grid grid-cols-2 gap-4">
              <div className="bg-blue-50 p-4 rounded-xl">
                <div className="text-xs text-gray-600 mb-1">Enlem (Latitude)</div>
                <div className="text-lg font-bold text-blue-600">{location.lat}°</div>
              </div>
              <div className="bg-purple-50 p-4 rounded-xl">
                <div className="text-xs text-gray-600 mb-1">Boylam (Longitude)</div>
                <div className="text-lg font-bold text-purple-600">{location.lng}°</div>
              </div>
            </div>

            {accuracy > 0 && (
              <div className="bg-green-50 p-4 rounded-xl flex items-center space-x-3">
                <i className="ri-checkbox-circle-line text-2xl text-green-600"></i>
                <div>
                  <div className="text-sm font-semibold text-gray-900">Konum Doğruluğu</div>
                  <div className="text-xs text-gray-600">±{accuracy} metre</div>
                </div>
              </div>
            )}

            {address && (
              <div className="bg-gray-50 p-4 rounded-xl">
                <div className="text-xs text-gray-600 mb-2">Adres</div>
                <div className="text-sm text-gray-900">{address}</div>
              </div>
            )}

            <button
              onClick={openInMaps}
              className="w-full bg-gray-100 text-gray-700 py-3 rounded-xl font-semibold hover:bg-gray-200 transition-all flex items-center justify-center space-x-2"
            >
              <i className="ri-map-line"></i>
              <span>Haritada Aç</span>
            </button>

            {/* Harita Önizleme */}
            <div className="rounded-xl overflow-hidden border-2 border-gray-200">
              <iframe
                width="100%"
                height="300"
                frameBorder="0"
                src={`https://www.openstreetmap.org/export/embed.html?bbox=${location.lng-0.01},${location.lat-0.01},${location.lng+0.01},${location.lat+0.01}&marker=${location.lat},${location.lng}`}
                style={{ border: 0 }}
              ></iframe>
            </div>
          </div>
        )}

        {/* QR Kod Önizleme */}
        {qrImage && (
          <div className="bg-white rounded-2xl shadow-lg p-6">
            <h2 className="text-lg font-bold text-gray-900 mb-6 text-center">QR Kod</h2>
            
            <div className="flex justify-center mb-6">
              <img
                src={qrImage}
                alt="Location QR Code"
                className="rounded-2xl shadow-xl"
              />
            </div>

            <div className="bg-blue-50 p-4 rounded-xl mb-4">
              <div className="text-sm text-gray-600 mb-2">QR Kod İçeriği</div>
              <div className="text-sm font-mono text-blue-600 break-all">
                geo:{location?.lat},{location?.lng}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <button
                onClick={downloadQR}
                className="bg-gradient-to-r from-indigo-600 to-purple-600 text-white py-3 rounded-xl font-semibold hover:from-indigo-700 hover:to-purple-700 transition-all shadow-lg flex items-center justify-center space-x-2"
              >
                <i className="ri-download-line text-xl"></i>
                <span>İndir</span>
              </button>
              
              {'share' in navigator && (
                <button
                  onClick={shareQR}
                  className="bg-white border-2 border-indigo-600 text-indigo-600 py-3 rounded-xl font-semibold hover:bg-indigo-50 transition-all flex items-center justify-center space-x-2"
                >
                  <i className="ri-share-line text-xl"></i>
                  <span>Paylaş</span>
                </button>
              )}
            </div>
          </div>
        )}

        {/* Bilgilendirme */}
        <div className="bg-gradient-to-r from-blue-50 to-purple-50 rounded-2xl p-6 border-2 border-blue-200">
          <div className="flex items-start space-x-4">
            <div className="w-12 h-12 bg-indigo-600 rounded-xl flex items-center justify-center flex-shrink-0">
              <i className="ri-information-line text-2xl text-white"></i>
            </div>
            <div>
              <h3 className="font-bold text-gray-900 mb-2">Nasıl Kullanılır?</h3>
              <ul className="text-gray-600 text-sm space-y-1">
                <li>• QR kodu tarayarak konum bilgisine erişebilirsiniz</li>
                <li>• Çoğu telefon QR kodu tarayınca harita uygulamasını açar</li>
                <li>• GPS koordinatları 8 haneli hassasiyetle kaydedilir</li>
                <li>• İnternet olmadan da QR kod oluşturabilirsiniz</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <Navigation />
    </div>
  );
}


