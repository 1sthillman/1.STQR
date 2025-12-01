import { WebPlugin } from '@capacitor/core';
import type { FloatingQRScannerPlugin } from './FloatingQRScanner';

export class FloatingQRScannerWeb extends WebPlugin implements FloatingQRScannerPlugin {
  async startFloatingScanner(): Promise<{ success: boolean; message: string }> {
    return {
      success: false,
      message: 'Floating QR Scanner sadece Android\'de çalışır'
    };
  }

  async stopFloatingScanner(): Promise<{ success: boolean; message: string }> {
    return {
      success: false,
      message: 'Floating QR Scanner sadece Android\'de çalışır'
    };
  }

  async checkOverlayPermission(): Promise<{ hasPermission: boolean }> {
    return { hasPermission: false };
  }

  async requestOverlayPermission(): Promise<{ success: boolean; opened?: boolean; alreadyGranted?: boolean; notRequired?: boolean; message?: string }> {
    throw new Error('Floating QR Scanner sadece Android\'de çalışır. Web platformunda desteklenmez.');
  }

  async checkCameraPermission(): Promise<{ granted: boolean }> {
    return { granted: false };
  }

  async requestCameraPermission(): Promise<{ granted: boolean }> {
    throw new Error('Floating QR Scanner sadece Android\'de çalışır. Web platformunda desteklenmez.');
  }

  async checkAccessibilityPermission(): Promise<{ hasPermission: boolean; granted: boolean }> {
    return { hasPermission: false, granted: false };
  }

  async requestAccessibilityPermission(): Promise<{ success: boolean; opened?: boolean; message?: string }> {
    throw new Error('Accessibility Service sadece Android\'de çalışır. Web platformunda desteklenmez.');
  }
}

