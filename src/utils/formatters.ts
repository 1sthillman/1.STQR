/**
 * Format currency
 */
export const formatCurrency = (amount: number, currency: string = '₺'): string => {
  return `${currency}${amount.toFixed(2)}`;
};

/**
 * Format date
 */
export const formatDate = (date: string | Date, format: 'short' | 'long' | 'time' = 'short'): string => {
  const d = typeof date === 'string' ? new Date(date) : date;
  
  if (format === 'time') {
    return d.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' });
  }
  
  if (format === 'long') {
    return d.toLocaleDateString('tr-TR', { 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
  
  return d.toLocaleDateString('tr-TR');
};

/**
 * Format phone number
 */
export const formatPhoneNumber = (phone: string): string => {
  const cleaned = phone.replace(/\D/g, '');
  if (cleaned.length === 10) {
    return `(${cleaned.slice(0, 3)}) ${cleaned.slice(3, 6)} ${cleaned.slice(6)}`;
  }
  return phone;
};

/**
 * Format barcode
 */
export const formatBarcode = (barcode: string): string => {
  if (barcode.length === 13) {
    return `${barcode.slice(0, 1)}-${barcode.slice(1, 7)}-${barcode.slice(7, 12)}-${barcode.slice(12)}`;
  }
  return barcode;
};

/**
 * Truncate text
 */
export const truncateText = (text: string, maxLength: number = 50): string => {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength) + '...';
};

/**
 * Format file size
 */
export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
};

/**
 * Format coordinates
 */
export const formatCoordinates = (lat: number, lng: number, precision: number = 6): string => {
  return `${lat.toFixed(precision)}, ${lng.toFixed(precision)}`;
};

/**
 * Generate random ID
 */
export const generateId = (): number => {
  return Date.now() + Math.floor(Math.random() * 1000);
};

/**
 * Generate barcode
 */
export const generateBarcode = (): string => {
  return Math.floor(Math.random() * 9000000000000 + 1000000000000).toString();
};

/**
 * Validate email
 */
export const isValidEmail = (email: string): boolean => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
};

/**
 * Validate URL
 */
export const isValidURL = (url: string): boolean => {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
};

/**
 * Clean phone number
 */
export const cleanPhoneNumber = (phone: string): string => {
  return phone.replace(/\D/g, '');
};

/**
 * Parse QR WiFi data
 */
export const parseWiFiQR = (data: string): { ssid: string; password: string; security: string } | null => {
  const match = data.match(/WIFI:T:([^;]*);S:([^;]*);P:([^;]*);/);
  if (match) {
    return {
      security: match[1],
      ssid: match[2],
      password: match[3],
    };
  }
  return null;
};

/**
 * Parse email QR data
 */
export const parseEmailQR = (data: string): { email: string; subject?: string; body?: string } | null => {
  if (data.startsWith('mailto:')) {
    const url = new URL(data);
    return {
      email: url.pathname,
      subject: url.searchParams.get('subject') || undefined,
      body: url.searchParams.get('body') || undefined,
    };
  }
  return null;
};











































