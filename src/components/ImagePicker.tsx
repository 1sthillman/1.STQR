import React, { useEffect, useState } from 'react';
import { useCapacitorCamera } from '../hooks';

interface ImagePickerProps {
  onImageSelect: (imageData: string | null) => void;
  initialImage?: string | null;
  className?: string;
}

export default function ImagePicker({ 
  onImageSelect, 
  initialImage = null,
  className = ''
}: ImagePickerProps) {
  const [selectedImage, setSelectedImage] = useState<string | null>(initialImage);
  const { photo, loading, error, takePhoto, pickPhoto } = useCapacitorCamera();
  
  // Capacitor Camera ile seçilen fotoğrafı izle
  useEffect(() => {
    if (photo) {
      setSelectedImage(photo);
      onImageSelect(photo);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [photo]);

  // Dosya seçildiğinde
  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    
    const reader = new FileReader();
    reader.onload = (event) => {
      const imageData = event.target?.result as string;
      setSelectedImage(imageData);
      onImageSelect(imageData);
    };
    reader.readAsDataURL(file);
  };

  // Resmi sil
  const handleRemoveImage = () => {
    setSelectedImage(null);
    onImageSelect(null);
  };

  return (
    <div className={`w-full ${className}`}>
      <div className="flex items-center justify-center mb-2">
        {selectedImage ? (
          <div className="relative w-full aspect-square max-w-[200px] mx-auto">
            <img 
              src={selectedImage} 
              alt="Selected" 
              className="w-full h-full object-cover rounded-lg shadow-md"
            />
            <button
              type="button"
              onClick={handleRemoveImage}
              className="absolute -top-2 -right-2 w-8 h-8 bg-red-500 text-white rounded-full flex items-center justify-center shadow-md hover:bg-red-600"
            >
              <i className="ri-close-line text-lg"></i>
            </button>
          </div>
        ) : (
          <div className="w-full max-w-[200px] aspect-square bg-gray-100 border-2 border-dashed border-gray-300 rounded-lg flex items-center justify-center">
            <div className="text-center p-4">
              <i className="ri-image-add-line text-3xl text-gray-400"></i>
              <p className="text-sm text-gray-500 mt-1">Görsel Seçilmedi</p>
            </div>
          </div>
        )}
      </div>
      
      <div className="grid grid-cols-3 gap-2 mt-2">
        {/* Dosya yükleme butonu */}
        <div className="relative">
          <input
            type="file"
            accept="image/*"
            onChange={handleFileSelect}
            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
          />
          <button
            type="button"
            className="w-full bg-blue-500 hover:bg-blue-600 text-white py-2 px-3 rounded-lg text-sm font-medium flex items-center justify-center gap-1"
          >
            <i className="ri-upload-line"></i>
            <span>Yükle</span>
          </button>
        </div>
        
        {/* Kameradan çekme butonu */}
        <button
          type="button"
          onClick={takePhoto}
          disabled={loading}
          className="bg-green-500 hover:bg-green-600 text-white py-2 px-3 rounded-lg text-sm font-medium flex items-center justify-center gap-1"
        >
          {loading ? (
            <i className="ri-loader-4-line animate-spin"></i>
          ) : (
            <>
              <i className="ri-camera-line"></i>
              <span>Çek</span>
            </>
          )}
        </button>
        
        {/* Galeriden seçme butonu */}
        <button
          type="button"
          onClick={pickPhoto}
          disabled={loading}
          className="bg-purple-500 hover:bg-purple-600 text-white py-2 px-3 rounded-lg text-sm font-medium flex items-center justify-center gap-1"
        >
          {loading ? (
            <i className="ri-loader-4-line animate-spin"></i>
          ) : (
            <>
              <i className="ri-image-line"></i>
              <span>Galeri</span>
            </>
          )}
        </button>
      </div>
      
      {error && (
        <div className="mt-2 text-xs text-red-500 bg-red-50 p-2 rounded-lg">
          <i className="ri-error-warning-line mr-1"></i>
          {error}
        </div>
      )}
    </div>
  );
}


