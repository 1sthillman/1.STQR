import { registerPlugin } from '@capacitor/core';

export interface AutoClickerPlugin {
  /**
   * Auto Clicker servisini başlat
   */
  startService(): Promise<void>;
  
  /**
   * Auto Clicker servisini durdur
   */
  stopService(): Promise<void>;
  
  /**
   * Overlay izni kontrolü
   */
  checkOverlayPermission(): Promise<{ granted: boolean }>;
  
  /**
   * Overlay izni iste
   */
  requestOverlayPermission(): Promise<{ success: boolean; opened?: boolean; alreadyGranted?: boolean; message?: string }>;
  
  /**
   * Accessibility izni kontrolü
   */
  checkAccessibilityPermission(): Promise<{ granted: boolean }>;
  
  /**
   * Accessibility izni iste
   */
  requestAccessibilityPermission(): Promise<{ success: boolean; opened?: boolean; message?: string }>;

}

const AutoClicker = registerPlugin<AutoClickerPlugin>('AutoClicker', {
  web: () => import('./AutoClickerWeb').then(m => new m.AutoClickerWeb()),
});

export default AutoClicker;


















