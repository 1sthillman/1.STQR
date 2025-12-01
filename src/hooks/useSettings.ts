/**
 * ⚙️ Settings Hook - SQLite üzerinden ayarlar yönetimi
 */

import { useState, useEffect, useCallback } from 'react';
import { databaseService } from '../services/DatabaseService';
import { DEFAULT_SETTINGS } from '../constants';

export interface AppSettings {
  language: 'tr' | 'en';
  theme: 'light' | 'dark';
  scanMode: 'normal' | 'fast';
  notifications: boolean;
  autoSave: boolean;
}

export const useSettings = () => {
  const [settings, setSettings] = useState<AppSettings>(DEFAULT_SETTINGS);
  const [loading, setLoading] = useState(true);

  const loadSettings = useCallback(async () => {
    try {
      const settingsData: any = {};
      
      // Tüm ayarları SQLite'dan yükle
      for (const key of Object.keys(DEFAULT_SETTINGS)) {
        const value = await databaseService.getSetting(key);
        if (value !== null) {
          try {
            settingsData[key] = JSON.parse(value);
          } catch {
            settingsData[key] = value;
          }
        }
      }
      
      setSettings({ ...DEFAULT_SETTINGS, ...settingsData });
    } catch (error) {
      console.error('Error loading settings:', error);
      setSettings(DEFAULT_SETTINGS);
    } finally {
      setLoading(false);
    }
  }, []);

  const updateSetting = useCallback(async <K extends keyof AppSettings>(
    key: K,
    value: AppSettings[K]
  ) => {
    try {
      await databaseService.setSetting(key, JSON.stringify(value));
      setSettings(prev => ({ ...prev, [key]: value }));
    } catch (error) {
      console.error(`Error updating setting ${key}:`, error);
      throw error;
    }
  }, []);

  const updateSettings = useCallback(async (newSettings: Partial<AppSettings>) => {
    try {
      for (const [key, value] of Object.entries(newSettings)) {
        await databaseService.setSetting(key, JSON.stringify(value));
      }
      setSettings(prev => ({ ...prev, ...newSettings }));
    } catch (error) {
      console.error('Error updating settings:', error);
      throw error;
    }
  }, []);

  const resetSettings = useCallback(async () => {
    try {
      for (const [key, value] of Object.entries(DEFAULT_SETTINGS)) {
        await databaseService.setSetting(key, JSON.stringify(value));
      }
      setSettings(DEFAULT_SETTINGS);
    } catch (error) {
      console.error('Error resetting settings:', error);
      throw error;
    }
  }, []);

  useEffect(() => {
    loadSettings();
  }, [loadSettings]);

  return {
    settings,
    loading,
    updateSetting,
    updateSettings,
    resetSettings,
    reload: loadSettings
  };
};

export default useSettings;

