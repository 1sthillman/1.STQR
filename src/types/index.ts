// Product Types
export interface Product {
  id: number;
  name: string;
  barcode: string;
  price: number;
  stock: number;
  category: string;
  description: string;
  qrCode: string;
  image?: string;
  createdAt: string;
  updatedAt?: string;
}

// QR Code Types
export type QRType = 'text' | 'url' | 'wifi' | 'email' | 'sms' | 'phone' | 'location' | 'vcard' | 'event';

export interface QRCodeData {
  id?: number;
  type: QRType;
  data: any;
  image: string;
  customization?: QRCustomization;
  createdAt: string;
}

export interface QRCustomization {
  size: number;
  color: string;
  bgColor: string;
  errorLevel: 'L' | 'M' | 'Q' | 'H';
  shape: 'square' | 'dots' | 'rounded';
  corner: 'sharp' | 'rounded' | 'extra-rounded';
  gradient: boolean;
  gradientAngle: number;
  gradientColor1: string;
  gradientColor2: string;
  border: number;
}

// Cart Types
export interface CartItem {
  id: number;
  productId: number;
  name: string;
  price: number;
  quantity: number;
  barcode: string;
  image?: string;
  addedAt: string;
}

export interface Cart {
  items: CartItem[];
  total: number;
  itemCount: number;
  updatedAt: string;
}

// Location Types
export interface Location {
  lat: number;
  lng: number;
  accuracy?: number;
  address?: string;
  timestamp: string;
}

// Scan Types
export interface ScanResult {
  data: string;
  type: string;
  timestamp: string;
  addedToCart?: boolean;
}

export interface ScanHistory {
  id: number;
  result: ScanResult;
  action?: string;
}

// Payment Types
export type PaymentMethod = 'cash' | 'card';

export interface Payment {
  id: number;
  method: PaymentMethod;
  amount: number;
  cashGiven?: number;
  change?: number;
  items: CartItem[];
  timestamp: string;
  status: 'completed' | 'pending' | 'failed';
}

// Sales Types
export interface Sale {
  id: number;
  items: CartItem[];
  total: number;
  paymentMethod: PaymentMethod;
  date: string;
  status: 'completed' | 'refunded';
}

// WiFi QR Data
export interface WiFiData {
  ssid: string;
  password: string;
  security: 'WPA' | 'WEP' | 'nopass';
}

// vCard Data
export interface VCardData {
  name: string;
  company?: string;
  phone?: string;
  email?: string;
  address?: string;
  website?: string;
}

// Event Data
export interface EventData {
  title: string;
  description?: string;
  location?: string;
  start: string;
  end: string;
}

// Settings
export interface AppSettings {
  language: 'tr' | 'en';
  theme: 'light' | 'dark';
  scanMode: 'normal' | 'fast';
  notifications: boolean;
  autoSave: boolean;
}

// Statistics
export interface Statistics {
  qrCodesCreated: number;
  scansCompleted: number;
  productsManaged: number;
  salesCompleted: number;
  totalRevenue: number;
}











































