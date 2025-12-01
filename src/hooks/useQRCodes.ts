/**
 * 📱 QR Codes Hook - SQLite üzerinden QR kod yönetimi
 */

import { useState, useEffect } from 'react';
import { databaseService } from '../services/DatabaseService';
import type { QRCode } from '../services/DatabaseService';

export const useQRCodes = () => {
  const [qrCodes, setQRCodes] = useState<QRCode[]>([]);
  const [loading, setLoading] = useState(true);

  const loadQRCodes = async () => {
    try {
      const data = await databaseService.getQRCodes(100);
      setQRCodes(data);
    } catch (error) {
      console.error('Error loading QR codes:', error);
    } finally {
      setLoading(false);
    }
  };

  const addQRCode = async (qrCode: Omit<QRCode, 'id' | 'timestamp'>) => {
    const newQR: QRCode = {
      id: `qr_${Date.now()}`,
      ...qrCode,
      timestamp: Date.now()
    };
    
    await databaseService.addQRCode(newQR);
    await loadQRCodes();
    return newQR;
  };

  const deleteQRCode = async (id: string) => {
    await databaseService.deleteQRCode(id);
    await loadQRCodes();
  };

  const clearAll = async () => {
    // SQLite'da tüm QR kodları sil
    for (const qr of qrCodes) {
      await databaseService.deleteQRCode(qr.id);
    }
    await loadQRCodes();
  };

  useEffect(() => {
    loadQRCodes();
  }, []);

  return {
    qrCodes,
    loading,
    addQRCode,
    deleteQRCode,
    clearAll,
    reload: loadQRCodes
  };
};

export default useQRCodes;



