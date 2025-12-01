/**
 * @deprecated Bu utility artık kullanılmıyor.
 * Bunun yerine `databaseService` kullanın (src/services/DatabaseService.ts)
 * 
 * Tüm veri işlemleri artık SQLite üzerinden yapılmaktadır.
 * LocalStorage kullanımı kaldırılmıştır.
 */

import { STORAGE_KEYS } from '../constants';

/**
 * @deprecated LocalStorage utility - Artık SQLite kullanılıyor
 */
class StorageService {
  /**
   * Get item from localStorage
   */
  getItem<T>(key: string, defaultValue: T): T {
    try {
      const item = localStorage.getItem(key);
      return item ? JSON.parse(item) : defaultValue;
    } catch (error) {
      console.error(`Error reading from localStorage (${key}):`, error);
      return defaultValue;
    }
  }

  /**
   * Set item to localStorage
   */
  setItem<T>(key: string, value: T): boolean {
    try {
      localStorage.setItem(key, JSON.stringify(value));
      return true;
    } catch (error) {
      console.error(`Error writing to localStorage (${key}):`, error);
      return false;
    }
  }

  /**
   * Remove item from localStorage
   */
  removeItem(key: string): boolean {
    try {
      localStorage.removeItem(key);
      return true;
    } catch (error) {
      console.error(`Error removing from localStorage (${key}):`, error);
      return false;
    }
  }

  /**
   * Clear all items from localStorage
   */
  clear(): boolean {
    try {
      localStorage.clear();
      return true;
    } catch (error) {
      console.error('Error clearing localStorage:', error);
      return false;
    }
  }

  /**
   * Check if key exists
   */
  hasItem(key: string): boolean {
    return localStorage.getItem(key) !== null;
  }

  /**
   * Get all keys
   */
  getAllKeys(): string[] {
    return Object.keys(localStorage);
  }

  /**
   * Get storage size in bytes
   */
  getStorageSize(): number {
    let size = 0;
    for (const key in localStorage) {
      if (localStorage.hasOwnProperty(key)) {
        size += localStorage[key].length + key.length;
      }
    }
    return size;
  }

  /**
   * Export all data
   */
  exportData(): Record<string, any> {
    const data: Record<string, any> = {};
    Object.values(STORAGE_KEYS).forEach(key => {
      const value = this.getItem(key, null);
      if (value) {
        data[key] = value;
      }
    });
    return data;
  }

  /**
   * Import data
   */
  importData(data: Record<string, any>): boolean {
    try {
      Object.entries(data).forEach(([key, value]) => {
        this.setItem(key, value);
      });
      return true;
    } catch (error) {
      console.error('Error importing data:', error);
      return false;
    }
  }
}

export const storage = new StorageService();
export default storage;






