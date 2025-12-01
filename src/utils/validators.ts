/**
 * Validation utilities
 */

export const validators = {
  /**
   * Validate required field
   */
  required: (value: any, fieldName: string = 'Bu alan'): string | null => {
    if (!value || (typeof value === 'string' && !value.trim())) {
      return `${fieldName} zorunludur`;
    }
    return null;
  },

  /**
   * Validate email
   */
  email: (value: string): string | null => {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
      return 'Geçerli bir e-posta adresi girin';
    }
    return null;
  },

  /**
   * Validate phone number
   */
  phone: (value: string): string | null => {
    const cleaned = value.replace(/\D/g, '');
    if (cleaned.length < 10) {
      return 'Geçerli bir telefon numarası girin';
    }
    return null;
  },

  /**
   * Validate URL
   */
  url: (value: string): string | null => {
    try {
      new URL(value);
      return null;
    } catch {
      return 'Geçerli bir URL girin';
    }
  },

  /**
   * Validate number
   */
  number: (value: any, min?: number, max?: number): string | null => {
    const num = parseFloat(value);
    if (isNaN(num)) {
      return 'Geçerli bir sayı girin';
    }
    if (min !== undefined && num < min) {
      return `Minimum değer ${min} olmalıdır`;
    }
    if (max !== undefined && num > max) {
      return `Maksimum değer ${max} olmalıdır`;
    }
    return null;
  },

  /**
   * Validate barcode
   */
  barcode: (value: string): string | null => {
    const cleaned = value.replace(/\D/g, '');
    if (cleaned.length < 8 || cleaned.length > 13) {
      return 'Barkod 8-13 haneli olmalıdır';
    }
    return null;
  },

  /**
   * Validate coordinates
   */
  coordinates: (lat: any, lng: any): string | null => {
    const latitude = parseFloat(lat);
    const longitude = parseFloat(lng);
    
    if (isNaN(latitude) || isNaN(longitude)) {
      return 'Geçerli koordinatlar girin';
    }
    
    if (latitude < -90 || latitude > 90) {
      return 'Enlem -90 ile 90 arasında olmalıdır';
    }
    
    if (longitude < -180 || longitude > 180) {
      return 'Boylam -180 ile 180 arasında olmalıdır';
    }
    
    return null;
  },

  /**
   * Validate min length
   */
  minLength: (value: string, length: number): string | null => {
    if (value.length < length) {
      return `En az ${length} karakter olmalıdır`;
    }
    return null;
  },

  /**
   * Validate max length
   */
  maxLength: (value: string, length: number): string | null => {
    if (value.length > length) {
      return `En fazla ${length} karakter olmalıdır`;
    }
    return null;
  },
};

/**
 * Run multiple validators
 */
export const validate = (value: any, validatorFns: Array<(val: any) => string | null>): string | null => {
  for (const validator of validatorFns) {
    const error = validator(value);
    if (error) return error;
  }
  return null;
};











































