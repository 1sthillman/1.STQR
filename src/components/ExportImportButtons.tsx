import React, { useRef, useState } from 'react';
import { exportToCSV, exportToJSON, importFromCSV, importFromJSON } from '../utils/export';

interface ExportImportButtonsProps {
  data: any[];
  onImport: (data: any[]) => void;
  filename: string;
  type?: 'products' | 'history' | 'sales';
}

export const ExportImportButtons: React.FC<ExportImportButtonsProps> = ({
  data,
  onImport,
  filename,
  type = 'products',
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'warning' } | null>(null);
  const [confirmDialog, setConfirmDialog] = useState<{ message: string; onConfirm: () => void } | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const showToast = (message: string, type: 'success' | 'error' | 'warning' = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const handleExportCSV = () => {
    try {
      if (data.length === 0) {
        showToast('⚠️ Dışa aktarılacak veri yok!', 'warning');
        return;
      }
      exportToCSV(data, filename);
      showToast('✅ CSV dosyası indirildi!', 'success');
    } catch (error: any) {
      console.error('CSV Export error:', error);
      showToast('❌ Dışa aktarma hatası: ' + error.message, 'error');
    }
  };

  const handleExportJSON = () => {
    try {
      if (data.length === 0) {
        showToast('⚠️ Dışa aktarılacak veri yok!', 'warning');
        return;
      }
      exportToJSON(data, filename);
      showToast('✅ JSON dosyası indirildi!', 'success');
    } catch (error: any) {
      console.error('JSON Export error:', error);
      showToast('❌ Dışa aktarma hatası: ' + error.message, 'error');
    }
  };

  const handleImportClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (isProcessing) {
      showToast('⏳ İşlem devam ediyor, lütfen bekleyin...', 'warning');
      return;
    }

    setIsProcessing(true);

    try {
      console.log('📂 Dosya okunuyor:', file.name);
      let importedData: any[];
      
      if (file.name.endsWith('.csv')) {
        importedData = await importFromCSV(file);
      } else if (file.name.endsWith('.json')) {
        importedData = await importFromJSON(file);
      } else {
        showToast('❌ Sadece CSV veya JSON dosyaları desteklenir', 'error');
        setIsProcessing(false);
        return;
      }

      if (importedData.length === 0) {
        showToast('⚠️ Dosya boş!', 'warning');
        setIsProcessing(false);
        return;
      }

      console.log('✅ Dosya okundu:', importedData.length, 'kayıt');

      // MOBİL UYUMLU CONFIRM DIALOG
      setConfirmDialog({
        message: `${importedData.length} kayıt içe aktarılacak. Devam edilsin mi?`,
        onConfirm: async () => {
          try {
            console.log('📥 İçe aktarma başlıyor...');
            await onImport(importedData);
            console.log('✅ İçe aktarma tamamlandı');
            showToast(`✅ ${importedData.length} kayıt başarıyla içe aktarıldı!`, 'success');
          } catch (error: any) {
            console.error('❌ İçe aktarma hatası:', error);
            showToast('❌ İçe aktarma hatası: ' + (error.message || 'Bilinmeyen hata'), 'error');
          } finally {
            setIsProcessing(false);
            setConfirmDialog(null);
          }
        }
      });
    } catch (error: any) {
      console.error('❌ Dosya okuma hatası:', error);
      showToast('❌ Hata: ' + (error.message || 'Dosya okunamadı'), 'error');
      setIsProcessing(false);
    }

    // Reset input
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <>
      <div className="flex gap-2 flex-wrap">
        <input
          ref={fileInputRef}
          type="file"
          accept=".csv,.json"
          onChange={handleFileSelect}
          className="hidden"
        />
        
        <button
          onClick={handleExportCSV}
          disabled={isProcessing}
          className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-green-500 to-emerald-500 text-white rounded-xl hover:from-green-600 hover:to-emerald-600 transition-all shadow-md hover:shadow-lg text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <i className="ri-file-excel-2-line"></i>
          CSV Dışa Aktar
        </button>

        <button
          onClick={handleExportJSON}
          disabled={isProcessing}
          className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-blue-500 to-cyan-500 text-white rounded-xl hover:from-blue-600 hover:to-cyan-600 transition-all shadow-md hover:shadow-lg text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <i className="ri-file-code-line"></i>
          JSON Dışa Aktar
        </button>

        <button
          onClick={handleImportClick}
          disabled={isProcessing}
          className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-xl hover:from-purple-600 hover:to-pink-600 transition-all shadow-md hover:shadow-lg text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <i className={`${isProcessing ? 'ri-loader-4-line animate-spin' : 'ri-file-upload-line'}`}></i>
          {isProcessing ? 'İşleniyor...' : 'İçe Aktar'}
        </button>
      </div>

      {/* Toast Notification - MOBİL UYUMLU */}
      {toast && (
        <div className={`fixed top-20 left-1/2 transform -translate-x-1/2 z-50 px-6 py-3 rounded-xl shadow-2xl border-2 animate-bounce ${
          toast.type === 'success' ? 'bg-green-500 border-green-300 text-white' :
          toast.type === 'error' ? 'bg-red-500 border-red-300 text-white' :
          'bg-orange-500 border-orange-300 text-white'
        }`}>
          <p className="font-bold text-sm">{toast.message}</p>
        </div>
      )}

      {/* Confirm Dialog - MOBİL UYUMLU */}
      {confirmDialog && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-6 animate-scale-in">
            <div className="text-center mb-6">
              <div className="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <i className="ri-question-line text-3xl text-purple-600"></i>
              </div>
              <h3 className="text-lg font-bold text-gray-900 mb-2">Onay Gerekli</h3>
              <p className="text-gray-600">{confirmDialog.message}</p>
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => {
                  setConfirmDialog(null);
                  setIsProcessing(false);
                }}
                className="flex-1 px-4 py-3 bg-gray-200 text-gray-700 rounded-xl hover:bg-gray-300 transition-all font-bold"
              >
                İptal
              </button>
              <button
                onClick={confirmDialog.onConfirm}
                className="flex-1 px-4 py-3 bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-xl hover:from-purple-600 hover:to-pink-600 transition-all font-bold shadow-lg"
              >
                Devam Et
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};







