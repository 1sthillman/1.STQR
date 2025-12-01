# 1.STQR

# 🚀 QRMaster - Gelişmiş QR Kod Uygulaması

Modern, kapsamlı ve profesyonel QR kod oluşturma, tarama ve ürün yönetimi uygulaması.

![QRMaster](https://img.shields.io/badge/version-1.0.0-blue.svg)
![React](https://img.shields.io/badge/React-19.0.0-61dafb.svg)
![TypeScript](https://img.shields.io/badge/TypeScript-5.6.3-3178c6.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)

## ✨ Özellikler

### 📱 QR Kod Oluşturma
- **9+ QR Türü**: Metin, URL, WiFi, E-posta, SMS, Telefon, Konum, Kartvizit, Etkinlik
- **Gelişmiş Özelleştirme**: Renk, boyut, gradient, şekil, köşe stilleri
- **8 Hazır Tema**: Klasik, Gri, Yeşil, Kırmızı, Mavi, Mor, Turuncu, Cyan
- **Gradient Desteği**: 360° açı kontrolü ile doğrusal ve dairesel gradient
- **Hata Düzeltme**: L, M, Q, H seviyeleri
- **Anında Önizleme**: Canlı QR kod önizlemesi
- **İndirme & Paylaşma**: PNG formatında indirme ve sosyal paylaşım

### 📸 QR Kod Tarama
- **Gerçek Zamanlı Tarama**: jsQR ile yüksek performanslı kamera tarama
- **2 Tarama Modu**: Normal (tek tarama) ve Hızlı Sepet (sürekli tarama)
- **Flaş Kontrolü**: Düşük ışıkta flaş desteği
- **Tarama Geçmişi**: Son 50 tarama kaydı
- **Otomatik Tanıma**: QR türü otomatik algılama
- **Test Taramaları**: Örnek QR kodlarla test imkanı

### 📦 Ürün Yönetimi
- **Tam CRUD**: Ürün ekleme, düzenleme, silme
- **Otomatik QR**: Her ürün için otomatik QR kod oluşturma
- **Barkod Üretimi**: Rastgele 13 haneli barkod
- **Kategori Filtreleme**: 14 ürün kategorisi
- **Arama**: İsim, barkod ve açıklama araması
- **Stok Takibi**: Otomatik stok yönetimi
- **LocalStorage**: Tarayıcı tabanlı veri saklama

### 🛒 Akıllı Sepet
- **Normal Mod**: Tek ürün ekleme ve düzenleme
- **Hızlı Mod**: Sürekli tarama ile otomatik sepet ekleme
- **Ödeme Sistemi**: Nakit ve Kredi Kartı seçenekleri
- **Para Üstü**: Otomatik para üstü hesaplama
- **Sepet Yönetimi**: Miktar artır/azalt, ürün sil
- **Satış Geçmişi**: Tüm satışların kaydı

### 🗺️ Konum QR
- **GPS Entegrasyonu**: Yüksek hassasiyetli konum alma (±metre)
- **OpenStreetMap**: Canlı harita görüntüleme
- **Adres Arama**: Konum adı ile arama
- **Popüler Konumlar**: 8 popüler şehir hazır
- **Doğruluk Göstergesi**: Konum doğruluğu gösterimi
- **Harita Önizleme**: iframe ile harita görünümü

## 🛠 Teknolojiler

- **Frontend**: React 19, TypeScript
- **Styling**: TailwindCSS
- **Build Tool**: Vite
- **Router**: React Router DOM v6
- **QR Library**: qrcode, jsQR
- **i18n**: react-i18next
- **Icons**: Remix Icons
- **Fonts**: Inter, Pacifico (Google Fonts)

## 📦 Kurulum

### Gereksinimler
- Node.js 18+
- npm veya yarn

### Adımlar

1. **Projeyi klonlayın**
\`\`\`bash
git clone <repository-url>
cd 1STQR
\`\`\`

2. **Bağımlılıkları yükleyin**
\`\`\`bash
npm install
\`\`\`

3. **Geliştirme sunucusunu başlatın**
\`\`\`bash
npm run dev
\`\`\`

4. **Tarayıcınızda açın**
\`\`\`
http://localhost:3000
\`\`\`

## 🏗 Proje Yapısı

\`\`\`
src/
├── components/       # Yeniden kullanılabilir UI bileşenleri
│   ├── Button.tsx
│   ├── Modal.tsx
│   ├── Card.tsx
│   ├── Input.tsx
│   └── Select.tsx
├── constants/        # Sabitler ve yapılandırma
│   └── index.ts
├── hooks/            # Custom React hooks
│   ├── useLocalStorage.ts
│   ├── useQRScanner.ts
│   ├── useGeolocation.ts
│   ├── useProducts.ts
│   └── useCart.ts
├── i18n/             # Çoklu dil desteği
│   └── local/
│       ├── tr/
│       └── en/
├── pages/            # Sayfa bileşenleri
│   ├── home/
│   ├── qr-create/
│   ├── qr-scan/
│   ├── product-management/
│   ├── smart-cart/
│   └── location-qr/
├── router/           # Router konfigürasyonu
│   ├── index.ts
│   └── config.tsx
├── services/         # İş mantığı servisleri
│   ├── ProductService.ts
│   ├── QRService.ts
│   └── SalesService.ts
├── types/            # TypeScript tip tanımları
│   └── index.ts
├── utils/            # Yardımcı fonksiyonlar
│   ├── storage.ts
│   ├── qr.ts
│   ├── formatters.ts
│   └── validators.ts
├── App.tsx
├── main.tsx
└── index.css
\`\`\`

## 📝 Kullanım

### QR Kod Oluşturma

1. Ana sayfadan "QR Oluştur" sekmesine gidin
2. QR türünü seçin (Metin, URL, WiFi, vb.)
3. Gerekli bilgileri girin
4. Tasarım seçeneklerini özelleştirin
5. QR kodunu indirin veya paylaşın

### QR Kod Tarama

1. "QR Tara" sekmesine gidin
2. Tarama modunu seçin (Normal veya Hızlı)
3. Kamerayı başlatın
4. QR kodu kameraya tutun
5. Sonucu görüntüleyin ve işlem yapın

### Ürün Yönetimi

1. "Ürünler" sekmesine gidin
2. "Ürün Ekle" butonuna tıklayın
3. Ürün bilgilerini girin (veya barkod oluşturun)
4. Kategori ve fiyat belirleyin
5. Kaydedin - otomatik QR kod oluşturulur

### Akıllı Sepet

1. "Sepet" sekmesine gidin
2. "Hızlı Sepet" modunu açın
3. Ürün QR/barkodlarını tarayın
4. Sepette ürünleri görüntüleyin
5. Ödeme yöntemini seçin ve tamamlayın

## 🎨 Özelleştirme

### Renk Teması
`src/constants/index.ts` dosyasından renk temalarını özelleştirebilirsiniz.

### QR Ayarları
`src/constants/index.ts` dosyasında `DEFAULT_QR_CUSTOMIZATION` ile varsayılan ayarları değiştirebilirsiniz.

### Kategoriler
`PRODUCT_CATEGORIES` sabitini düzenleyerek ürün kategorilerini özelleştirebilirsiniz.

## 🔒 Güvenlik

- Tüm veriler LocalStorage'da saklanır
- Kamera erişimi kullanıcı iznine tabidir
- Konum bilgileri hassas şekilde işlenir
- XSS koruması için tüm girdiler temizlenir

## 📱 Mobil Uyumluluk

- Responsive tasarım (375px+)
- Touch-optimized UI
- Mobile-first yaklaşım
- PWA hazır yapı

## 🚀 Build ve Deploy

### Production Build
\`\`\`bash
npm run build
\`\`\`

### Preview
\`\`\`bash
npm run preview
\`\`\`

### Deploy
Build klasörünü (dist/) herhangi bir static host'a yükleyebilirsiniz:
- Vercel
- Netlify
- GitHub Pages
- AWS S3

## 🤝 Katkıda Bulunma

1. Fork yapın
2. Feature branch oluşturun (\`git checkout -b feature/amazing-feature\`)
3. Değişikliklerinizi commit edin (\`git commit -m 'feat: Add amazing feature'\`)
4. Branch'i push edin (\`git push origin feature/amazing-feature\`)
5. Pull Request oluşturun

## 📄 Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.

## 👥 Ekip

- **Geliştirici**: QRMaster Team
- **Versiyon**: 1.0.0
- **Son Güncelleme**: 2025

## 📞 İletişim

Sorularınız için:
- GitHub Issues: [Issues](https://github.com/your-username/qrmaster/issues)
- Email: support@qrmaster.app

## 🎯 Roadmap

- [ ] PWA desteği
- [ ] Dark mode
- [ ] Çoklu dil genişletme (Almanca, Fransızca)
- [ ] Supabase entegrasyonu
- [ ] Stripe ödeme entegrasyonu
- [ ] QR kod stilleri genişletme
- [ ] Toplu QR oluşturma
- [ ] Excel/CSV export
- [ ] QR kod düzenleme
- [ ] Şablon sistemi

---

⭐ **Projeyi beğendiyseniz yıldız vermeyi unutmayın!**

Made with ❤️ by QRMaster Team









































