import { WebPlugin } from '@capacitor/core';
import type { AutoClickerPlugin } from './AutoClicker';

export class AutoClickerWeb extends WebPlugin implements AutoClickerPlugin {
  async startService(): Promise<void> {
    console.warn('Auto Clicker sadece Android\'de çalışır');
  }
  
  async stopService(): Promise<void> {
    console.warn('Auto Clicker sadece Android\'de çalışır');
  }
  
  async checkOverlayPermission(): Promise<{ granted: boolean }> {
    console.warn('Auto Clicker sadece Android\'de çalışır');
    return { granted: false };
  }
  
  async requestOverlayPermission(): Promise<{ success: boolean; opened?: boolean; alreadyGranted?: boolean; message?: string }> {
    throw new Error('Auto Clicker sadece Android\'de çalışır. Web platformunda desteklenmez.');
  }
  
  async checkAccessibilityPermission(): Promise<{ granted: boolean }> {
    console.warn('Auto Clicker sadece Android\'de çalışır');
    return { granted: false };
  }
  
  async requestAccessibilityPermission(): Promise<{ success: boolean; opened?: boolean; message?: string }> {
    throw new Error('Accessibility Service sadece Android\'de çalışır. Web platformunda desteklenmez.');
  }
}


















