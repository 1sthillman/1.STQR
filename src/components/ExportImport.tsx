import { useState } from 'react';
import { Capacitor } from '@capacitor/core';
import { Filesystem, Directory, Encoding } from '@capacitor/filesystem';
import { Share } from '@capacitor/share';

interface ExportImportProps {
  storageKey: string;
  dataLabel: string;
  onExport?: () => Promise<any[]>; // SQLite'dan veri çek
  onImport?: (data: any[]) => Promise<boolean | void>; // SQLite'a veri yaz
}

export const ExportImport = ({ storageKey, dataLabel, onExport, onImport }: ExportImportProps) => {
  const [importing, setImporting] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  const showToast = (message: string) => {
    setToast(message);
    setTimeout(() => setToast(null), 3000);
  };

  const exportToJSON = async () => {
    try {
      // SQLite'dan veri çek (onExport callback ile)
      const data = onExport ? await onExport() : [];
      
      if (data.length === 0) {
        showToast('⚠️ Dışa aktarılacak veri yok');
        return;
      }

      const dataStr = JSON.stringify(data, null, 2);
      const fileName = `${storageKey}-${Date.now()}.json`;

      if (Capacitor.isNativePlatform()) {
        // Mobil: Cache'e yaz ve PAYLAŞ (Share API)
        await Filesystem.writeFile({
          path: fileName,
          data: dataStr,
          directory: Directory.Cache,
          encoding: Encoding.UTF8,
        });
        
        const fileUri = await Filesystem.getUri({
          path: fileName,
          directory: Directory.Cache,
        });
        
        await Share.share({
          title: 'JSON Dışa Aktar',
          text: `${dataLabel} - JSON Dosyası`,
          url: fileUri.uri,
          dialogTitle: 'Dosyayı Kaydet veya Paylaş'
        });
        
        showToast('✅ Dosya paylaşıldı!');
      } else {
        // Tarayıcı: Standart indirme
        const dataBlob = new Blob([dataStr], { type: 'application/json' });
        const url = URL.createObjectURL(dataBlob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.click();
        URL.revokeObjectURL(url);
        showToast('✅ JSON dosyası indirildi!');
      }
    } catch (error) {
      console.error('Export error:', error);
      showToast('❌ Dışa aktarma hatası');
    }
  };

  const exportToCSV = async () => {
    try {
      // SQLite'dan veri çek (onExport callback ile)
      const data = onExport ? await onExport() : [];
      
      if (data.length === 0) {
        showToast('⚠️ Dışa aktarılacak veri yok');
        return;
      }

      // CSV headers
      const headers = Object.keys(data[0]);
      const csvContent = [
        headers.join(','),
        ...data.map((row: any) => 
          headers.map(header => {
            const value = row[header];
            // Handle special characters
            if (typeof value === 'string' && (value.includes(',') || value.includes('"') || value.includes('\n'))) {
              return `"${value.replace(/"/g, '""')}"`;
            }
            return value;
          }).join(',')
        )
      ].join('\n');

      const fileName = `${storageKey}-${Date.now()}.csv`;

      if (Capacitor.isNativePlatform()) {
        // Mobil: Cache'e yaz ve PAYLAŞ (Share API)
        await Filesystem.writeFile({
          path: fileName,
          data: csvContent,
          directory: Directory.Cache,
          encoding: Encoding.UTF8,
        });
        
        const fileUri = await Filesystem.getUri({
          path: fileName,
          directory: Directory.Cache,
        });
        
        await Share.share({
          title: 'CSV Dışa Aktar',
          text: `${dataLabel} - CSV Dosyası`,
          url: fileUri.uri,
          dialogTitle: 'Dosyayı Kaydet veya Paylaş'
        });
        
        showToast('✅ Dosya paylaşıldı!');
      } else {
        // Tarayıcı: Standart indirme
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.click();
        URL.revokeObjectURL(url);
        showToast('✅ CSV dosyası indirildi!');
      }
    } catch (error) {
      console.error('Export error:', error);
      showToast('❌ Dışa aktarma hatası');
    }
  };

  const importData = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setImporting(true);
    const reader = new FileReader();
    
    reader.onload = async (e) => {
      try {
        const content = e.target?.result as string;
        let importedData: any[] = [];

        if (file.name.endsWith('.json')) {
          importedData = JSON.parse(content);
        } else if (file.name.endsWith('.csv')) {
          // Simple CSV parsing
          const lines = content.split('\n');
          const headers = lines[0].split(',');
          importedData = lines.slice(1)
            .filter(line => line.trim())
            .map(line => {
              const values = line.split(',');
              const obj: any = {};
              headers.forEach((header, index) => {
                obj[header.trim()] = values[index]?.trim();
              });
              return obj;
            });
        }

        if (Array.isArray(importedData) && importedData.length > 0) {
          // SQLite'a veri yaz (onImport callback ile)
          try {
            if (onImport) {
              const result = await onImport(importedData);
              if (result !== false) {
                showToast(`✅ ${importedData.length} ${dataLabel} başarıyla içe aktarıldı!`);
              }
            } else {
              showToast('❌ İçe aktarma işlemi desteklenmiyor');
            }
          } catch (error) {
            console.error('Import to SQLite error:', error);
            showToast('❌ Veri içe aktarılırken hata oluştu!');
          }
        } else {
          showToast('❌ Geçersiz veri formatı');
        }
      } catch (error) {
        console.error('Import error:', error);
        showToast('❌ İçe aktarma hatası: ' + (error as Error).message);
      } finally {
        setImporting(false);
        event.target.value = '';
      }
    };
    
    reader.onerror = () => {
      console.error('File read error');
      showToast('❌ Dosya okunamadı');
      setImporting(false);
      event.target.value = '';
    };

    reader.readAsText(file);
  };

  return (
    <>
      {/* 💎 VIP Premium Export/Import Card */}
      <div className="relative bg-gradient-to-br from-indigo-50 via-purple-50 to-pink-50 rounded-2xl p-6 border-2 border-purple-200 shadow-lg">
        {/* Glassmorphism overlay */}
        <div className="absolute inset-0 bg-white/40 backdrop-blur-sm rounded-2xl"></div>
        
        <div className="relative z-10">
          <div className="flex items-center gap-3 mb-5">
            <div className="w-12 h-12 bg-gradient-to-br from-purple-500 to-pink-500 rounded-xl flex items-center justify-center shadow-lg">
              <i className="ri-folder-transfer-line text-2xl text-white"></i>
            </div>
            <div>
              <h3 className="text-lg font-bold text-gray-900">Dışa / İçe Aktar</h3>
              <p className="text-sm text-gray-600">{dataLabel} verilerinizi yönetin</p>
            </div>
          </div>
          
          <div className="grid grid-cols-2 gap-3">
            {/* 💎 JSON Export */}
            <button
              onClick={exportToJSON}
              className="group relative bg-gradient-to-br from-blue-500 to-cyan-500 text-white py-3.5 px-4 rounded-xl font-semibold hover:from-blue-600 hover:to-cyan-600 transition-all shadow-lg hover:shadow-xl hover:scale-105 flex items-center justify-center gap-2"
            >
              <i className="ri-download-line text-xl group-hover:animate-bounce"></i>
              <span>JSON</span>
            </button>

            {/* 💎 CSV Export */}
            <button
              onClick={exportToCSV}
              className="group relative bg-gradient-to-br from-green-500 to-teal-500 text-white py-3.5 px-4 rounded-xl font-semibold hover:from-green-600 hover:to-teal-600 transition-all shadow-lg hover:shadow-xl hover:scale-105 flex items-center justify-center gap-2"
            >
              <i className="ri-download-line text-xl group-hover:animate-bounce"></i>
              <span>CSV</span>
            </button>

            {/* 💎 Import */}
            <label className="col-span-2 group relative bg-gradient-to-r from-purple-600 via-pink-600 to-rose-600 text-white py-3.5 px-4 rounded-xl font-semibold hover:from-purple-700 hover:via-pink-700 hover:to-rose-700 transition-all shadow-xl hover:shadow-2xl hover:scale-105 flex items-center justify-center gap-2 cursor-pointer">
              <i className={`${importing ? 'ri-loader-4-line animate-spin' : 'ri-upload-line'} text-xl`}></i>
              <span>{importing ? 'İçe Aktarılıyor...' : 'İçe Aktar'}</span>
              <input
                type="file"
                accept=".json,.csv"
                onChange={importData}
                className="hidden"
                disabled={importing}
              />
            </label>
          </div>
        </div>
      </div>

      {/* Toast Notification */}
      {toast && (
        <div className="fixed top-20 left-1/2 transform -translate-x-1/2 z-[9999] animate-slide-down">
          <div className="bg-gray-900/95 backdrop-blur-lg text-white px-6 py-3 rounded-full shadow-2xl border border-white/10">
            <p className="text-sm font-semibold">{toast}</p>
          </div>
        </div>
      )}
    </>
  );
};

export default ExportImport;





