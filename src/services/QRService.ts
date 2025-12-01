/**
 * @deprecated Bu servis artık kullanılmıyor.
 * Bunun yerine `databaseService.getQRCodes()` metodunu kullanın (src/services/DatabaseService.ts)
 * 
 * Tüm veri işlemleri artık SQLite üzerinden yapılmaktadır.
 * LocalStorage kullanımı kaldırılmıştır.
 */

import { QRCodeData } from '../types';
import { storage } from '../utils/storage';
import { STORAGE_KEYS } from '../constants';

class QRService {
  /**
   * @deprecated databaseService.getQRCodes() kullanın
   */
  getAll(): QRCodeData[] {
    console.warn('QRService.getAll() deprecated! databaseService.getQRCodes() kullanın.');
    return storage.getItem<QRCodeData[]>(STORAGE_KEYS.QR_CODES, []);
  }

  /**
   * Get QR code by ID
   */
  getById(id: number): QRCodeData | undefined {
    const qrCodes = this.getAll();
    return qrCodes.find((q) => q.id === id);
  }

  /**
   * Save QR code
   */
  save(qrCode: QRCodeData): boolean {
    const qrCodes = this.getAll();
    qrCodes.unshift(qrCode);
    // Keep only last 100 QR codes
    return storage.setItem(STORAGE_KEYS.QR_CODES, qrCodes.slice(0, 100));
  }

  /**
   * Delete QR code
   */
  delete(id: number): boolean {
    const qrCodes = this.getAll();
    const filtered = qrCodes.filter((q) => q.id !== id);
    return storage.setItem(STORAGE_KEYS.QR_CODES, filtered);
  }

  /**
   * Get QR codes by type
   */
  getByType(type: string): QRCodeData[] {
    const qrCodes = this.getAll();
    return qrCodes.filter((q) => q.type === type);
  }

  /**
   * Clear all QR codes
   */
  clearAll(): boolean {
    return storage.setItem(STORAGE_KEYS.QR_CODES, []);
  }

  /**
   * Export QR codes
   */
  export(): QRCodeData[] {
    return this.getAll();
  }

  /**
   * Import QR codes
   */
  import(qrCodes: QRCodeData[]): boolean {
    return storage.setItem(STORAGE_KEYS.QR_CODES, qrCodes);
  }
}

export const qrService = new QRService();
export default qrService;






