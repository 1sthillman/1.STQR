/**
 * @deprecated Bu hook artık kullanılmıyor.
 * Bunun yerine `useDatabase` hooks'larını kullanın (src/hooks/useDatabase.ts)
 * 
 * Tüm veri işlemleri artık SQLite üzerinden yapılmaktadır.
 * LocalStorage kullanımı kaldırılmıştır.
 */

import { useState, useEffect, useCallback } from 'react';
import { storage } from '../utils/storage';

/**
 * @deprecated useDatabase hooks kullanın (useProducts, useCart, useSales, useScanHistory)
 */
export function useLocalStorage<T>(key: string, initialValue: T) {
  console.warn('useLocalStorage() deprecated! useDatabase hooks kullanın.');
  
  const [storedValue, setStoredValue] = useState<T>(() => {
    return storage.getItem(key, initialValue);
  });

  const setValue = useCallback((value: T | ((val: T) => T)) => {
    try {
      const valueToStore = value instanceof Function ? value(storedValue) : value;
      setStoredValue(valueToStore);
      storage.setItem(key, valueToStore);
    } catch (error) {
      console.error(`Error setting localStorage key "${key}":`, error);
    }
  }, [key, storedValue]);

  const removeValue = useCallback(() => {
    try {
      setStoredValue(initialValue);
      storage.removeItem(key);
    } catch (error) {
      console.error(`Error removing localStorage key "${key}":`, error);
    }
  }, [key, initialValue]);

  // Listen for changes from other tabs/windows
  useEffect(() => {
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === key && e.newValue) {
        try {
          setStoredValue(JSON.parse(e.newValue));
        } catch {
          setStoredValue(initialValue);
        }
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [key, initialValue]);

  return [storedValue, setValue, removeValue] as const;
}






