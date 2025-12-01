import { WebPlugin } from '@capacitor/core';
import type { OCRScannerPlugin } from './OCRScanner';

export class OCRScannerWeb extends WebPlugin implements OCRScannerPlugin {
  async startFloatingOCR(): Promise<{ success: boolean }> {
    console.warn('OCR Scanner is only available on Android');
    return { success: false };
  }

  async stopFloatingOCR(): Promise<{ success: boolean }> {
    console.warn('OCR Scanner is only available on Android');
    return { success: false };
  }

  async checkOverlayPermission(): Promise<{ granted: boolean }> {
    console.warn('OCR Scanner is only available on Android');
    return { granted: false };
  }

  async requestOverlayPermission(): Promise<{ success: boolean; opened?: boolean; alreadyGranted?: boolean; message?: string }> {
    throw new Error('OCR Scanner sadece Android\'de çalışır. Web platformunda desteklenmez.');
  }

  async checkCameraPermission(): Promise<{ granted: boolean }> {
    console.warn('OCR Scanner is only available on Android');
    return { granted: false };
  }

  async requestCameraPermission(): Promise<{ granted: boolean }> {
    throw new Error('OCR Scanner sadece Android\'de çalışır. Web platformunda desteklenmez.');
  }

  async checkAccessibilityPermission(): Promise<{ granted: boolean }> {
    console.warn('OCR Scanner is only available on Android');
    return { granted: false };
  }

  async requestAccessibilityPermission(): Promise<{ success: boolean; opened?: boolean; message?: string }> {
    throw new Error('Accessibility Service sadece Android\'de çalışır. Web platformunda desteklenmez.');
  }
}

