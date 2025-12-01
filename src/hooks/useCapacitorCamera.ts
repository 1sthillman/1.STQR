import { useState } from 'react';
import { Camera, CameraResultType, CameraSource } from '@capacitor/camera';

export const useCapacitorCamera = () => {
  const [photo, setPhoto] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Kameradan fotoğraf çek
  const takePhoto = async () => {
    try {
      setLoading(true);
      setError(null);

      const image = await Camera.getPhoto({
        quality: 90,
        allowEditing: true,
        resultType: CameraResultType.DataUrl,
        source: CameraSource.Camera
      });

      if (image.dataUrl) {
        setPhoto(image.dataUrl);
        return image.dataUrl;
      } else {
        throw new Error('Fotoğraf çekilemedi');
      }
    } catch (err: any) {
      console.error('Kamera hatası:', err);
      if (err.message === 'User cancelled photos app') {
        setError('Kullanıcı işlemi iptal etti');
      } else {
        setError(`Kamera hatası: ${err.message || err}`);
      }
      return null;
    } finally {
      setLoading(false);
    }
  };

  // Galeriden fotoğraf seç
  const pickPhoto = async () => {
    try {
      setLoading(true);
      setError(null);

      const image = await Camera.getPhoto({
        quality: 90,
        allowEditing: true,
        resultType: CameraResultType.DataUrl,
        source: CameraSource.Photos
      });

      if (image.dataUrl) {
        setPhoto(image.dataUrl);
        return image.dataUrl;
      } else {
        throw new Error('Fotoğraf seçilemedi');
      }
    } catch (err: any) {
      console.error('Galeri hatası:', err);
      if (err.message === 'User cancelled photos app') {
        setError('Kullanıcı işlemi iptal etti');
      } else {
        setError(`Galeri hatası: ${err.message || err}`);
      }
      return null;
    } finally {
      setLoading(false);
    }
  };

  return {
    photo,
    loading,
    error,
    takePhoto,
    pickPhoto,
    resetPhoto: () => setPhoto(null),
  };
};





































