// ============================================
// 🌐 API CONFIGURATION
// ============================================

export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000/api';

// ============================================
// 📋 API ENDPOINTS
// ============================================

export const API_ENDPOINTS = {
  // Health check
  HEALTH: '/health',
  
  // Products
  PRODUCTS: '/products',
  PRODUCT_BY_ID: (id: string | number) => `/products/${id}`,
  PRODUCT_BY_BARCODE: (barcode: string) => `/products/barcode/${barcode}`,
  PRODUCT_CATEGORIES: '/products/categories/list',
  
  // QR Codes
  QR_CODES: '/qr-codes',
  QR_CODE_BY_ID: (id: string | number) => `/qr-codes/${id}`,
  QR_CODE_DOWNLOAD: (id: string | number) => `/qr-codes/${id}/download`,
  QR_TYPES_STATS: '/qr-codes/types/stats',
  
  // Sales
  SALES: '/sales',
  SALE_BY_ID: (id: string | number) => `/sales/${id}`,
  SALES_DAILY_STATS: '/sales/stats/daily',
  SALES_SUMMARY: '/sales/stats/summary',
  
  // Scan History
  SCAN_HISTORY: '/scan-history',
  SCAN_HISTORY_BY_ID: (id: string | number) => `/scan-history/${id}`,
  SCAN_HISTORY_STATS: '/scan-history/stats/summary',
  SCAN_HISTORY_EXPORT: '/scan-history/export',
  
  // Statistics
  STATS: '/stats',
  STATS_DASHBOARD: '/stats/dashboard',
  STATS_REVENUE: '/stats/revenue',
  STATS_PRODUCTS_PERFORMANCE: '/stats/products/performance',
  STATS_CATEGORIES: '/stats/categories'
} as const;

// ============================================
// 🔧 API CONFIGURATION
// ============================================

export const API_CONFIG = {
  timeout: 10000, // 10 seconds
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  withCredentials: false
} as const;

// ============================================
// 📊 REQUEST TYPES
// ============================================

export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
  errors?: any[];
}

export interface PaginatedResponse<T = any> extends ApiResponse<T> {
  pagination?: {
    page: number;
    limit: number;
    total: number;
    pages: number;
  };
}

// ============================================
// 🚫 ERROR TYPES
// ============================================

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number = 500,
    public response?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export class NetworkError extends Error {
  constructor(message: string = 'Ağ bağlantısı hatası') {
    super(message);
    this.name = 'NetworkError';
  }
}

export class ValidationError extends Error {
  constructor(
    message: string,
    public errors: any[] = []
  ) {
    super(message);
    this.name = 'ValidationError';
  }
}

// ============================================
// 🔍 QUERY PARAMETERS
// ============================================

export interface ProductQueryParams {
  category?: string;
  search?: string;
  page?: number;
  limit?: number;
}

export interface QRCodeQueryParams {
  type?: string;
  search?: string;
  page?: number;
  limit?: number;
}

export interface SalesQueryParams {
  payment_method?: string;
  start_date?: string;
  end_date?: string;
  page?: number;
  limit?: number;
}

export interface ScanHistoryQueryParams {
  qr_type?: string;
  successful_only?: boolean;
  start_date?: string;
  end_date?: string;
  search?: string;
  page?: number;
  limit?: number;
}







































