import { QRType } from '../types';

// QR Code Types
export const QR_TYPES: { id: QRType; name: string; icon: string; description: string }[] = [
  { id: 'text', name: 'Metin', icon: 'ri-text', description: 'Düz metin QR kodu' },
  { id: 'url', name: 'Website', icon: 'ri-global-line', description: 'Web sitesi bağlantısı' },
  { id: 'wifi', name: 'WiFi', icon: 'ri-wifi-line', description: 'WiFi ağ bilgileri' },
  { id: 'email', name: 'E-posta', icon: 'ri-mail-line', description: 'E-posta adresi ve mesaj' },
  { id: 'sms', name: 'SMS', icon: 'ri-message-2-line', description: 'SMS mesajı' },
  { id: 'phone', name: 'Telefon', icon: 'ri-phone-line', description: 'Telefon numarası' },
  { id: 'location', name: 'Konum', icon: 'ri-map-pin-line', description: 'GPS koordinatları' },
  { id: 'vcard', name: 'Kartvizit', icon: 'ri-contacts-line', description: 'Kişi bilgileri (vCard)' },
  { id: 'event', name: 'Etkinlik', icon: 'ri-calendar-line', description: 'Takvim etkinliği' },
];

// Product Categories
export const PRODUCT_CATEGORIES = [
  'Genel',
  'Elektronik',
  'Gıda',
  'Giyim',
  'Ev & Yaşam',
  'Kozmetik',
  'Spor',
  'Kitap',
  'Oyuncak',
  'Sağlık',
  'Otomotiv',
  'Bahçe',
  'Mobilya',
  'Aksesuar',
];

// Color Themes for QR
export const COLOR_THEMES = [
  { name: 'Klasik', fg: '#000000', bg: '#ffffff' },
  { name: 'Gri', fg: '#4b5563', bg: '#f3f4f6' },
  { name: 'Yeşil', fg: '#059669', bg: '#ecfdf5' },
  { name: 'Kırmızı', fg: '#dc2626', bg: '#fef2f2' },
  { name: 'Mavi', fg: '#2563eb', bg: '#eff6ff' },
  { name: 'Mor', fg: '#7c3aed', bg: '#f5f3ff' },
  { name: 'Turuncu', fg: '#ea580c', bg: '#fff7ed' },
  { name: 'Cyan', fg: '#0891b2', bg: '#ecfeff' },
  { name: 'Pembe', fg: '#db2777', bg: '#fdf2f8' },
  { name: 'İndigo', fg: '#4f46e5', bg: '#eef2ff' },
];

// Popular Locations
export const POPULAR_LOCATIONS = [
  { name: 'İstanbul', lat: 41.0082, lng: 28.9784, country: 'Türkiye' },
  { name: 'Ankara', lat: 39.9334, lng: 32.8597, country: 'Türkiye' },
  { name: 'İzmir', lat: 38.4192, lng: 27.1287, country: 'Türkiye' },
  { name: 'Antalya', lat: 36.8969, lng: 30.7133, country: 'Türkiye' },
  { name: 'Bursa', lat: 40.1826, lng: 29.0665, country: 'Türkiye' },
  { name: 'Trabzon', lat: 41.0027, lng: 39.7168, country: 'Türkiye' },
  { name: 'Adana', lat: 37.0017, lng: 35.3213, country: 'Türkiye' },
  { name: 'Gaziantep', lat: 37.0662, lng: 37.3833, country: 'Türkiye' },
];

// Storage Keys
export const STORAGE_KEYS = {
  PRODUCTS: 'qrmaster_products',
  QR_CODES: 'qrmaster_qr_codes',
  CART: 'qrmaster_cart',
  SCAN_HISTORY: 'qrmaster_scan_history',
  SALES_HISTORY: 'qrmaster_sales_history',
  SETTINGS: 'qrmaster_settings',
  STATISTICS: 'qrmaster_statistics',
} as const;

// Default Settings
export const DEFAULT_SETTINGS = {
  language: 'tr' as const,
  theme: 'light' as const,
  scanMode: 'normal' as const,
  notifications: true,
  autoSave: true,
};

// Default QR Customization
export const DEFAULT_QR_CUSTOMIZATION = {
  size: 256,
  color: '#000000',
  bgColor: '#ffffff',
  errorLevel: 'M' as const,
  shape: 'square' as const,
  corner: 'sharp' as const,
  gradient: false,
  gradientAngle: 45,
  gradientColor1: '#3b82f6',
  gradientColor2: '#8b5cf6',
  border: 0,
};

// Error Messages
export const ERROR_MESSAGES = {
  CAMERA_ACCESS_DENIED: 'Kamera erişimi reddedildi. Lütfen tarayıcı ayarlarından kamera iznini aktif edin.',
  LOCATION_ACCESS_DENIED: 'Konum erişimi reddedildi. Lütfen tarayıcı ayarlarından konum iznini aktif edin.',
  QR_GENERATION_FAILED: 'QR kod oluşturulamadı. Lütfen tekrar deneyin.',
  QR_SCAN_FAILED: 'QR kod taranamadı. Lütfen tekrar deneyin.',
  PRODUCT_NOT_FOUND: 'Ürün bulunamadı.',
  INVALID_BARCODE: 'Geçersiz barkod.',
  PAYMENT_FAILED: 'Ödeme işlemi başarısız. Lütfen tekrar deneyin.',
  INSUFFICIENT_CASH: 'Yetersiz tutar. Lütfen daha fazla nakit girin.',
  NETWORK_ERROR: 'Ağ hatası. Lütfen internet bağlantınızı kontrol edin.',
} as const;

// Success Messages
export const SUCCESS_MESSAGES = {
  QR_GENERATED: 'QR kod başarıyla oluşturuldu!',
  QR_DOWNLOADED: 'QR kod indirildi!',
  PRODUCT_ADDED: 'Ürün başarıyla eklendi!',
  PRODUCT_UPDATED: 'Ürün güncellendi!',
  PRODUCT_DELETED: 'Ürün silindi!',
  CART_UPDATED: 'Sepet güncellendi!',
  PAYMENT_COMPLETED: 'Ödeme başarıyla tamamlandı!',
  SCAN_COMPLETED: 'Tarama başarılı!',
} as const;

// App Info
export const APP_INFO = {
  NAME: 'QRMaster',
  VERSION: '1.0.0',
  DESCRIPTION: 'Gelişmiş QR Kod ve Ürün Yönetimi Uygulaması',
  AUTHOR: 'QRMaster Team',
  WEBSITE: 'https://qrmaster.app',
} as const;

// API Endpoints (for future use)
export const API_ENDPOINTS = {
  BASE_URL: import.meta.env.VITE_API_URL || 'http://localhost:3000/api',
  PRODUCTS: '/products',
  QR_CODES: '/qr-codes',
  SALES: '/sales',
  STATISTICS: '/statistics',
} as const;

