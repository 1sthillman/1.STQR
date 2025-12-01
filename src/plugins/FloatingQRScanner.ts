import { registerPlugin, PluginListenerHandle } from '@capacitor/core';

export interface FloatingQRScannerPlugin {
  /**
   * Floating QR Scanner'ı başlat
   */
  startFloatingScanner(): Promise<{ success: boolean; message: string }>;
  
  /**
   * Floating QR Scanner'ı durdur
   */
  stopFloatingScanner(): Promise<{ success: boolean; message: string }>;
  
  /**
   * Overlay iznini kontrol et
   */
  checkOverlayPermission(): Promise<{ hasPermission: boolean }>;
  
  /**
   * Overlay izni iste
   */
  requestOverlayPermission(): Promise<{ success: boolean; opened?: boolean; alreadyGranted?: boolean; notRequired?: boolean; message?: string }>;
  
  /**
   * Kamera iznini kontrol et
   */
  checkCameraPermission(): Promise<{ granted: boolean }>;
  
  /**
   * Kamera izni iste
   */
  requestCameraPermission(): Promise<{ granted: boolean }>;
  
  /**
   * Accessibility iznini kontrol et (Direkt input yazma)
   */
  checkAccessibilityPermission(): Promise<{ hasPermission: boolean; granted: boolean }>;
  
  /**
   * Accessibility izni iste (Direkt input yazma)
   */
  requestAccessibilityPermission(): Promise<{ success: boolean; opened?: boolean; message?: string }>;

  
  /**
   * QR kod tarandığında event dinle
   */
  addListener(
    eventName: 'qrScanned',
    listenerFunc: (event: QRScannedEvent) => void
  ): Promise<PluginListenerHandle>;
  
  /**
   * Tüm listener'ları kaldır
   */
  removeAllListeners(): Promise<void>;
}

export interface QRScannedEvent {
  qrCode: string;
  timestamp: number;
  id?: string;
  description?: string;
  stock?: number;
  price?: number;
  category?: string;
  notes?: string;
  autoFillAttempted?: boolean;
  autoFillSuccess?: boolean;
  source?: string;
}

const FloatingQRScanner = registerPlugin<FloatingQRScannerPlugin>('FloatingQRScanner', {
  web: () => import('./FloatingQRScannerWeb').then(m => new m.FloatingQRScannerWeb()),
});

export default FloatingQRScanner;

