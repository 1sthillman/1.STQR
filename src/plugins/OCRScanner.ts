import { registerPlugin } from '@capacitor/core';

export interface OCRScannedEvent {
  text: string;
  timestamp: number;
  id?: string;
  description?: string;
  category?: string;
  notes?: string;
  confidence?: number;
  lineCount?: number;
  charCount?: number;
  wordCount?: number;
  turkishCharCount?: number;
  hasTurkish?: boolean;
  autoFillAttempted?: boolean;
  autoFillSuccess?: boolean;
  source?: string;
}

export interface OCRScannerPlugin {
  /**
   * Floating OCR tarayıcısını başlat
   */
  startFloatingOCR(): Promise<{ success: boolean }>;

  /**
   * Floating OCR tarayıcısını durdur
   */
  stopFloatingOCR(): Promise<{ success: boolean }>;

  /**
   * Overlay iznini kontrol et
   */
  checkOverlayPermission(): Promise<{ granted: boolean }>;

  /**
   * Overlay izni iste
   */
  requestOverlayPermission(): Promise<{ success: boolean; opened?: boolean; alreadyGranted?: boolean; message?: string }>;

  /**
   * Kamera iznini kontrol et
   */
  checkCameraPermission(): Promise<{ granted: boolean }>;

  /**
   * Kamera izni iste
   */
  requestCameraPermission(): Promise<{ granted: boolean }>;

  /**
   * Accessibility iznini kontrol et
   */
  checkAccessibilityPermission(): Promise<{ granted: boolean }>;

  /**
   * Accessibility izni iste
   */
  requestAccessibilityPermission(): Promise<{ success: boolean; opened?: boolean; message?: string }>;


  /**
   * OCR tarama eventi dinle
   */
  addListener(
    eventName: 'textScanned',
    listenerFunc: (event: OCRScannedEvent) => void
  ): Promise<{ remove: () => void }>;

  /**
   * Tüm listener'ları kaldır
   */
  removeAllListeners(): Promise<void>;
}

const OCRScanner = registerPlugin<OCRScannerPlugin>('OCRScanner', {
  web: () => import('./OCRScannerWeb').then(m => new m.OCRScannerWeb()),
});

export default OCRScanner;

