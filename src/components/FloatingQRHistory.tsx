import { useState, memo } from 'react';
import { Share } from '@capacitor/share';
import { Clipboard } from '@capacitor/clipboard';
import { QRScannedEvent } from '../plugins/FloatingQRScanner';
import QRCode from 'qrcode';
import ExcelJS from 'exceljs';

interface FloatingQRHistoryProps {
  history: QRScannedEvent[];
  onDelete: (id: string) => void;
  onDeleteAll: () => void;
  onUpdate: (item: QRScannedEvent) => void;
}

function FloatingQRHistory({ 
  history, 
  onDelete, 
  onDeleteAll,
  onUpdate 
}: FloatingQRHistoryProps) {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [showShareMenu, setShowShareMenu] = useState(false);
  const [editForm, setEditForm] = useState<Partial<QRScannedEvent>>({});

  const startEdit = (item: QRScannedEvent) => {
    setEditingId(item.id || '');
    setEditForm({ ...item });
  };

  const saveEdit = () => {
    if (editingId && editForm) {
      onUpdate({ ...editForm, id: editingId } as QRScannedEvent);
      setEditingId(null);
      setEditForm({});
    }
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditForm({});
  };

  const copyToClipboard = async (text: string) => {
    try {
      await Clipboard.write({ string: text });
      alert('📋 Panoya kopyalandı!');
    } catch (error) {
      console.error('Kopyalama hatası:', error);
    }
  };

  const exportToJSON = async () => {
    try {
      const jsonData = JSON.stringify(history, null, 2);
      const fileName = `floating-qr-history-${Date.now()}.json`;
      
      // Capacitor Filesystem API ile geçici dosya oluştur
      const { Filesystem, Directory } = await import('@capacitor/filesystem');
      
      const result = await Filesystem.writeFile({
        path: fileName,
        data: jsonData,
        directory: Directory.Cache, // Cache klasörü - geçici
        encoding: 'utf8' as any
      });
      
      console.log('✅ Dosya yazıldı:', result.uri);
      
      // Capacitor Share API ile paylaş
      await Share.share({
        title: 'Floating QR Geçmişi (JSON)',
        text: `${history.length} adet QR kod taraması`,
        url: result.uri,
        dialogTitle: 'JSON Dosyasını Paylaş'
      });
      
      alert('✅ JSON dosyası paylaşıldı!');
    } catch (error) {
      console.error('Export hatası:', error);
      alert('❌ Dışa aktarma başarısız: ' + JSON.stringify(error));
    }
  };

  const exportToCSV = async () => {
    try {
      const csvHeader = 'QR Code,Timestamp,Description,Stock,Price,Category,Notes\n';
      const csvRows = history.map(item => 
        `"${item.qrCode}","${new Date(item.timestamp).toLocaleString()}","${item.description || ''}","${item.stock || ''}","${item.price || ''}","${item.category || ''}","${item.notes || ''}"`
      ).join('\n');
      
      const csvData = csvHeader + csvRows;
      const fileName = `floating-qr-history-${Date.now()}.csv`;
      
      // Capacitor Filesystem API ile geçici dosya oluştur
      const { Filesystem, Directory } = await import('@capacitor/filesystem');
      
      const result = await Filesystem.writeFile({
        path: fileName,
        data: csvData,
        directory: Directory.Cache,
        encoding: 'utf8' as any
      });
      
      console.log('✅ CSV yazıldı:', result.uri);
      
      // Capacitor Share API ile paylaş
      await Share.share({
        title: 'Floating QR Geçmişi (CSV)',
        text: `${history.length} adet QR kod taraması`,
        url: result.uri,
        dialogTitle: 'CSV Dosyasını Paylaş'
      });
      
      alert('✅ CSV dosyası paylaşıldı!');
    } catch (error) {
      console.error('Export hatası:', error);
      alert('❌ Dışa aktarma başarısız: ' + JSON.stringify(error));
    }
  };

  const generateQRCode = async (text: string): Promise<string> => {
    try {
      // QRCode library ile gerçek QR kod oluştur
      const dataUrl = await QRCode.toDataURL(text, {
        width: 300,
        margin: 2,
        color: {
          dark: '#000000',
          light: '#FFFFFF'
        }
      });
      return dataUrl;
    } catch (error) {
      console.error('QR kod oluşturma hatası:', error);
      // Fallback: Basit görsel
      const canvas = document.createElement('canvas');
      canvas.width = 300;
      canvas.height = 300;
      const ctx = canvas.getContext('2d')!;
      ctx.fillStyle = '#FFFFFF';
      ctx.fillRect(0, 0, 300, 300);
      ctx.fillStyle = '#000000';
      ctx.font = '16px Arial';
      ctx.fillText('QR: ' + text.substring(0, 20), 10, 150);
      return canvas.toDataURL('image/png');
    }
  };

  const exportToExcelSimple = async () => {
    try {
      console.log('📊 Basit Excel (CSV) oluşturuluyor...');
      
      // CSV formatı (Excel'de mükemmel açılır)
      const csvHeader = 'Sıra,QR/Barkod,Tarih,Açıklama,Stok,Fiyat,Kategori,Notlar\n';
      const csvRows = history.map((item, index) => 
        `${index + 1},"${item.qrCode}","${new Date(item.timestamp).toLocaleString('tr-TR')}","${item.description || '-'}","${item.stock || '-'}","${item.price ? item.price + ' TL' : '-'}","${item.category || '-'}","${item.notes || '-'}"`
      ).join('\n');
      
      const csvData = '\uFEFF' + csvHeader + csvRows; // UTF-8 BOM
      const fileName = `floating-qr-history-${Date.now()}.csv`;
      
      // Capacitor Filesystem ile kaydet
      const { Filesystem, Directory } = await import('@capacitor/filesystem');
      
      const result = await Filesystem.writeFile({
        path: fileName,
        data: csvData,
        directory: Directory.Cache,
        encoding: 'utf8' as any
      });
      
      console.log('✅ CSV dosyası yazıldı:', result.uri);
      
      // Paylaş
      await Share.share({
        title: 'Floating QR Geçmişi (Excel CSV)',
        text: `${history.length} adet QR/Barkod - Excel'de açılabilir`,
        url: result.uri,
        dialogTitle: 'Excel Dosyasını Paylaş'
      });
      
      setShowShareMenu(false);
      alert('✅ Excel (CSV) dosyası paylaşıldı!\n\n📝 Excel\'de açınca QR/Barkod kodları metin olarak görünür.\n🖼️ Görselli için "HTML + QR Görselleri" seçeneğini kullanın.');
    } catch (error) {
      console.error('CSV export hatası:', error);
      alert('❌ Export başarısız: ' + JSON.stringify(error));
    }
  };

  const exportToExcel = async () => {
    try {
      console.log('📊 Excel dosyası oluşturuluyor...');
      console.log('🎨 QR görselleri oluşturuluyor...');
      
      // Önce tüm QR görsellerini oluştur
      const qrImages = await Promise.all(
        history.map(async (item) => {
          const qrDataUrl = await generateQRCode(item.qrCode);
          return qrDataUrl;
        })
      );
      
      console.log(`✅ ${qrImages.length} QR görseli hazır`);
      
      // HTML Excel formatı oluştur (Excel tarafından açılabilir)
      let excelHtml = `
<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel">
<head>
  <meta charset="utf-8">
  <style>
    table { 
      border-collapse: collapse; 
      width: 100%; 
      font-family: Arial, sans-serif; 
    }
    th { 
      background-color: #8B5CF6; 
      color: white; 
      font-weight: bold; 
      padding: 12px; 
      text-align: center;
      border: 1px solid #ddd;
    }
    td { 
      padding: 8px; 
      border: 1px solid #ddd; 
      text-align: center;
      vertical-align: middle;
    }
    .qr-cell {
      height: 75px;
      width: 190px;
      padding: 8px;
      text-align: center;
      vertical-align: middle;
    }
    img {
      display: block;
      margin: 0 auto;
    }
  </style>
</head>
<body>
  <table border="1">
    <thead>
      <tr>
        <th>Sıra</th>
        <th>QR Görsel</th>
        <th>QR Kod</th>
        <th>Tarih</th>
        <th>Açıklama</th>
        <th>Stok</th>
        <th>Fiyat</th>
        <th>Kategori</th>
        <th>Notlar</th>
      </tr>
    </thead>
    <tbody>`;
      
      // Her satırı ekle - QR/Barkod otomatik algılama
      history.forEach((item, index) => {
        // Barkod mu QR kod mu kontrol et (sadece rakam varsa barkod)
        const isBarcode = /^\d+$/.test(item.qrCode) && item.qrCode.length >= 8;
        
        let codeImageUrl = '';
        let codeType = '';
        
        if (isBarcode) {
          // Barkod için (EAN13, EAN8, Code128 vs.)
          const barcodeFormat = item.qrCode.length === 13 ? 'ean13' : 
                               item.qrCode.length === 8 ? 'ean8' : 'code128';
          codeImageUrl = `https://bwipjs-api.metafloor.com/?bcid=${barcodeFormat}&text=${item.qrCode}&scale=3&height=10&includetext`;
          codeType = 'BARKOD';
        } else {
          // QR kod için
          codeImageUrl = `https://api.qrserver.com/v1/create-qr-code/?size=100x100&margin=0&data=${encodeURIComponent(item.qrCode)}`;
          codeType = 'QR';
        }
        
        excelHtml += `
      <tr style="height:70px;">
        <td>${index + 1}</td>
        <td class="qr-cell" style="padding:8px; text-align:center; vertical-align:middle;">
          <img src="${codeImageUrl}" style="${isBarcode ? 'max-width:180px; height:50px; object-fit:contain;' : 'width:70px; height:70px; object-fit:contain;'}" alt="${codeType} ${index + 1}">
        </td>
        <td>${item.qrCode}</td>
        <td>${new Date(item.timestamp).toLocaleString('tr-TR')}</td>
        <td>${item.description || '-'}</td>
        <td>${item.stock || '-'}</td>
        <td>${item.price ? item.price + ' TL' : '-'}</td>
        <td>${item.category || '-'}</td>
        <td>${item.notes || '-'}</td>
      </tr>`;
      });
      
      excelHtml += `
    </tbody>
  </table>
</body>
</html>`;
      
      console.log('✅ Excel HTML hazırlandı');
      
      // .html olarak kaydet (Excel ve tarayıcıda açılabilir)
      const fileName = `floating-qr-history-${Date.now()}.html`;
      
      // Capacitor Filesystem ile kaydet
      const { Filesystem, Directory } = await import('@capacitor/filesystem');
      
      const result = await Filesystem.writeFile({
        path: fileName,
        data: excelHtml,
        directory: Directory.Cache,
        encoding: 'utf8' as any
      });
      
      console.log('✅ Excel dosyası yazıldı:', result.uri);
      
      // Paylaş
      await Share.share({
        title: 'Floating QR Geçmişi (Excel + QR Görselleri)',
        text: `${history.length} adet QR kod taraması - QR görselleri ile Excel!`,
        url: result.uri,
        dialogTitle: 'Excel Dosyasını Paylaş'
      });
      
      setShowShareMenu(false);
      alert('✅ Excel dosyası (QR görselleri ile) paylaşıldı!');
    } catch (error) {
      console.error('Excel export hatası:', error);
      alert('❌ Excel export başarısız: ' + JSON.stringify(error));
    }
  };
  
  const exportToTXT = async () => {
    try {
      console.log('📄 TXT dosyası oluşturuluyor...');
      
      let txtContent = `🎯 FLOATING QR TARAMA GEÇMİŞİ\n`;
      txtContent += `Toplam Kayıt: ${history.length}\n`;
      txtContent += `Tarih: ${new Date().toLocaleString('tr-TR')}\n`;
      txtContent += `${'='.repeat(60)}\n\n`;
      
      history.forEach((item, index) => {
        txtContent += `[${index + 1}] QR KOD\n`;
        txtContent += `━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n`;
        txtContent += `📱 İçerik: ${item.qrCode}\n`;
        txtContent += `⏰ Tarih: ${new Date(item.timestamp).toLocaleString('tr-TR')}\n`;
        if (item.description) txtContent += `📝 Açıklama: ${item.description}\n`;
        if (item.stock) txtContent += `📦 Stok: ${item.stock}\n`;
        if (item.price) txtContent += `💰 Fiyat: ${item.price} TL\n`;
        if (item.category) txtContent += `🏷️ Kategori: ${item.category}\n`;
        if (item.notes) txtContent += `📋 Notlar: ${item.notes}\n`;
        txtContent += `\n`;
      });
      
      const fileName = `floating-qr-history-${Date.now()}.txt`;
      
      // Capacitor Filesystem ile kaydet
      const { Filesystem, Directory } = await import('@capacitor/filesystem');
      
      const result = await Filesystem.writeFile({
        path: fileName,
        data: txtContent,
        directory: Directory.Cache,
        encoding: 'utf8' as any
      });
      
      console.log('✅ TXT dosyası yazıldı:', result.uri);
      
      // Paylaş
      await Share.share({
        title: 'Floating QR Geçmişi (TXT)',
        text: `${history.length} adet QR kod taraması`,
        url: result.uri,
        dialogTitle: 'TXT Dosyasını Paylaş'
      });
      
      setShowShareMenu(false);
      alert('✅ TXT dosyası paylaşıldı!');
    } catch (error) {
      console.error('TXT export hatası:', error);
      alert('❌ TXT export başarısız: ' + JSON.stringify(error));
    }
  };

  const shareHistoryHTML = async () => {
    try {
      // TÜM QR kodları ile HTML oluştur (limit yok!)
      console.log(`🎨 ${history.length} adet QR görseli oluşturuluyor...`);
      const qrImages = await Promise.all(
        history.map(async (item) => {
          const qrDataUrl = await generateQRCode(item.qrCode);
          return { ...item, qrImage: qrDataUrl };
        })
      );
      
      console.log('✅ QR görselleri hazır:', qrImages.length);
      
      // HTML içerik oluştur
      let htmlContent = `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <style>
    body { font-family: Arial, sans-serif; padding: 20px; background: #f5f5f5; }
    h1 { color: #8B5CF6; text-align: center; }
    .item { 
      background: white; 
      margin: 20px 0; 
      border: 2px solid #8B5CF6; 
      padding: 15px; 
      border-radius: 10px; 
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    }
    .qr-image { 
      width: 200px; 
      height: 200px; 
      margin: 10px auto; 
      display: block;
      border: 1px solid #ddd;
    }
    .info { color: #666; font-size: 14px; }
    p { margin: 8px 0; }
  </style>
</head>
<body>
  <h1>🎯 Floating QR Tarama Geçmişi</h1>
  <p style="text-align: center; color: #666;">Toplam ${history.length} kayıt</p>`;
      
      qrImages.forEach((item, index) => {
        htmlContent += `
  <div class="item">
    <h3>📱 QR Kod #${index + 1}</h3>
    <img src="${item.qrImage}" class="qr-image" alt="QR Code ${index + 1}">
    <p><strong>İçerik:</strong> ${item.qrCode}</p>
    <p class="info">⏰ ${new Date(item.timestamp).toLocaleString('tr-TR')}</p>
    ${item.description ? `<p>📝 ${item.description}</p>` : ''}
    ${item.stock ? `<p>📦 Stok: ${item.stock}</p>` : ''}
    ${item.price ? `<p>💰 Fiyat: ${item.price} TL</p>` : ''}
    ${item.category ? `<p>🏷️ Kategori: ${item.category}</p>` : ''}
    ${item.notes ? `<p>📋 Notlar: ${item.notes}</p>` : ''}
  </div>`;
      });
      
      htmlContent += `
</body>
</html>`;
      
      const fileName = `qr-gecmis-${Date.now()}.html`;
      
      // Capacitor Filesystem API ile kaydet
      const { Filesystem, Directory } = await import('@capacitor/filesystem');
      
      const result = await Filesystem.writeFile({
        path: fileName,
        data: htmlContent,
        directory: Directory.Cache,
        encoding: 'utf8' as any
      });
      
      console.log('✅ HTML dosyası yazıldı:', result.uri);
      
      // Capacitor Share API ile paylaş
      await Share.share({
        title: 'Floating QR Tarama Geçmişi (HTML)',
        text: `${history.length} adet QR kod taraması (QR görselleri dahil)`,
        url: result.uri,
        dialogTitle: 'HTML Dosyasını Paylaş'
      });
      
      setShowShareMenu(false);
      alert('✅ QR görselleri ile HTML paylaşıldı!');
    } catch (error) {
      console.error('Paylaşma hatası:', error);
      alert('❌ Paylaşma başarısız: ' + JSON.stringify(error));
    }
  };

  if (history.length === 0) {
    return (
      <div className="text-center py-12 px-4">
        <div className="text-6xl mb-4">📭</div>
        <p className="text-gray-400 text-lg">Henüz tarama yapılmadı</p>
        <p className="text-gray-500 text-sm mt-2">
          Floating Scanner başlattığınızda QR kodları burada görünecek
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Üst Kontrol Butonları */}
      <div className="bg-gradient-to-r from-purple-900/30 to-pink-900/30 backdrop-blur-sm rounded-2xl p-4 border border-purple-500/30 shadow-lg shadow-purple-500/20">
        <div className="flex flex-wrap gap-2">
          <button
            onClick={exportToJSON}
            className="flex-1 min-w-[140px] px-4 py-2 bg-gradient-to-r from-blue-600 to-cyan-600 text-white rounded-xl font-semibold shadow-lg shadow-blue-500/30 active:scale-95 transition-transform flex items-center justify-center gap-2"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            JSON
          </button>

          <button
            onClick={exportToCSV}
            className="flex-1 min-w-[140px] px-4 py-2 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-xl font-semibold shadow-lg shadow-green-500/30 active:scale-95 transition-transform flex items-center justify-center gap-2"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            CSV
          </button>

          <button
            onClick={() => setShowShareMenu(!showShareMenu)}
            className="flex-1 min-w-[140px] px-4 py-2 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-xl font-semibold shadow-lg shadow-purple-500/30 active:scale-95 transition-transform flex items-center justify-center gap-2"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
            </svg>
            Paylaş Menü {showShareMenu ? '▲' : '▼'}
          </button>

          <button
            onClick={onDeleteAll}
            className="flex-1 min-w-[140px] px-4 py-2 bg-gradient-to-r from-red-600 to-rose-600 text-white rounded-xl font-semibold shadow-lg shadow-red-500/30 active:scale-95 transition-transform flex items-center justify-center gap-2"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            Tümünü Sil
          </button>
        </div>

        {/* Paylaşım Menüsü */}
        {showShareMenu && (
          <div className="mt-4 p-4 bg-gradient-to-br from-purple-900/50 to-pink-900/50 rounded-2xl border-2 border-purple-500/30 backdrop-blur-sm">
            <h3 className="text-white font-bold text-lg mb-3 flex items-center gap-2">
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
              </svg>
              Paylaşım Formatı Seçin
            </h3>
            
            <div className="grid grid-cols-1 gap-3">
              {/* Excel CSV (Basit) */}
              <button
                onClick={exportToExcelSimple}
                className="w-full px-4 py-3 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-xl font-semibold shadow-lg shadow-green-500/30 active:scale-95 transition-all flex items-center justify-between"
              >
                <div className="flex items-center gap-3">
                  <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  <div className="text-left">
                    <div className="font-bold">Excel (CSV)</div>
                    <div className="text-xs opacity-80">Sadece veri - {history.length} kayıt</div>
                  </div>
                </div>
                <span className="text-2xl">📊</span>
              </button>

              {/* HTML + QR Görselleri */}
              <button
                onClick={shareHistoryHTML}
                className="w-full px-4 py-3 bg-gradient-to-r from-blue-600 to-cyan-600 text-white rounded-xl font-semibold shadow-lg shadow-blue-500/30 active:scale-95 transition-all flex items-center justify-between"
              >
                <div className="flex items-center gap-3">
                  <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                  <div className="text-left">
                    <div className="font-bold">HTML + Barkod/QR Görselleri</div>
                    <div className="text-xs opacity-80">Tarayıcıda aç (ÖNERİLEN!) - {history.length} kayıt</div>
                  </div>
                </div>
                <span className="text-2xl">🌐</span>
              </button>

              {/* TXT */}
              <button
                onClick={exportToTXT}
                className="w-full px-4 py-3 bg-gradient-to-r from-gray-600 to-slate-600 text-white rounded-xl font-semibold shadow-lg shadow-gray-500/30 active:scale-95 transition-all flex items-center justify-between"
              >
                <div className="flex items-center gap-3">
                  <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  <div className="text-left">
                    <div className="font-bold">Metin (.txt)</div>
                    <div className="text-xs opacity-80">Sade metin formatı - {history.length} kayıt</div>
                  </div>
                </div>
                <span className="text-2xl">📄</span>
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Geçmiş Listesi */}
      <div className="space-y-3">
        {history.map((item, index) => {
          const isEditing = editingId === (item.id || index.toString());
          
          return (
            <div
              key={item.id || index}
              className="bg-gradient-to-br from-gray-900/90 to-gray-800/90 backdrop-blur-xl rounded-2xl p-4 border border-purple-500/20 shadow-xl shadow-purple-900/20 transform transition-all duration-200 hover:scale-[1.02] hover:shadow-2xl hover:shadow-purple-500/30"
            >
              {isEditing ? (
                /* Düzenleme Modu */
                <div className="space-y-3">
                  <input
                    type="text"
                    value={editForm.description || ''}
                    onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                    placeholder="📝 Açıklama"
                    className="w-full px-4 py-2 bg-gray-800/50 border border-purple-500/30 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-purple-500"
                  />
                  
                  <div className="grid grid-cols-2 gap-3">
                    <input
                      type="number"
                      value={editForm.stock || ''}
                      onChange={(e) => setEditForm({ ...editForm, stock: Number(e.target.value) })}
                      placeholder="📦 Stok"
                      className="px-4 py-2 bg-gray-800/50 border border-purple-500/30 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-purple-500"
                    />
                    <input
                      type="number"
                      value={editForm.price || ''}
                      onChange={(e) => setEditForm({ ...editForm, price: Number(e.target.value) })}
                      placeholder="💰 Fiyat (TL)"
                      className="px-4 py-2 bg-gray-800/50 border border-purple-500/30 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-purple-500"
                    />
                  </div>
                  
                  <input
                    type="text"
                    value={editForm.category || ''}
                    onChange={(e) => setEditForm({ ...editForm, category: e.target.value })}
                    placeholder="🏷️ Kategori"
                    className="w-full px-4 py-2 bg-gray-800/50 border border-purple-500/30 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-purple-500"
                  />
                  
                  <textarea
                    value={editForm.notes || ''}
                    onChange={(e) => setEditForm({ ...editForm, notes: e.target.value })}
                    placeholder="📋 Notlar"
                    rows={2}
                    className="w-full px-4 py-2 bg-gray-800/50 border border-purple-500/30 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 resize-none"
                  />
                  
                  <div className="flex gap-2">
                    <button
                      onClick={saveEdit}
                      className="flex-1 px-4 py-2 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-xl font-semibold shadow-lg shadow-green-500/30 active:scale-95 transition-transform"
                    >
                      ✅ Kaydet
                    </button>
                    <button
                      onClick={cancelEdit}
                      className="flex-1 px-4 py-2 bg-gradient-to-r from-gray-600 to-gray-700 text-white rounded-xl font-semibold shadow-lg shadow-gray-500/30 active:scale-95 transition-transform"
                    >
                      ❌ İptal
                    </button>
                  </div>
                </div>
              ) : (
                /* Görüntüleme Modu */
                <div className="space-y-3">
                  {/* QR Kod */}
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="text-xs text-purple-400 mb-1">QR KOD</div>
                      <div className="text-white font-mono text-sm break-all bg-gray-800/50 px-3 py-2 rounded-lg border border-purple-500/20">
                        {item.qrCode}
                      </div>
                    </div>
                    <button
                      onClick={() => copyToClipboard(item.qrCode)}
                      className="p-2 bg-purple-600/30 hover:bg-purple-600/50 rounded-lg transition-colors"
                    >
                      <svg className="w-5 h-5 text-purple-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                      </svg>
                    </button>
                  </div>

                  {/* Zaman */}
                  <div className="flex flex-wrap items-center gap-2 text-xs text-gray-300">
                    <span className="px-2 py-1 rounded-md bg-gray-800/60 border border-gray-700/60">
                    ⏰ {new Date(item.timestamp).toLocaleString('tr-TR')}
                    </span>
                    <span className="px-2 py-1 rounded-md bg-gray-800/60 border border-gray-700/60">
                      🌐 Kaynak: {item.source === 'floating' ? 'Floating Panel' : item.source || 'Bilinmiyor'}
                    </span>
                    {item.autoFillAttempted && (
                      <span
                        className={`px-2 py-1 rounded-md border ${
                          item.autoFillSuccess
                            ? 'bg-emerald-500/15 border-emerald-400/60 text-emerald-200'
                            : 'bg-rose-500/15 border-rose-400/60 text-rose-200'
                        }`}
                      >
                        {item.autoFillSuccess ? '✅ Otomatik dolduruldu' : '⚠️ Otomatik doldurulamadı'}
                      </span>
                    )}
                    {!item.autoFillAttempted && (
                      <span className="px-2 py-1 rounded-md bg-amber-500/15 border border-amber-400/60 text-amber-200">
                        📋 Panoya kopyalandı
                      </span>
                    )}
                  </div>

                  {/* Metadata */}
                  {(item.description || item.stock || item.price || item.category || item.notes) && (
                    <div className="space-y-1.5 bg-gray-800/30 rounded-lg p-3 border border-purple-500/10">
                      {item.description && (
                        <div className="text-sm text-gray-300">📝 {item.description}</div>
                      )}
                      <div className="flex flex-wrap gap-2">
                        {item.stock !== undefined && (
                          <span className="px-2 py-1 bg-blue-500/20 text-blue-300 text-xs rounded-md">
                            📦 Stok: {item.stock}
                          </span>
                        )}
                        {item.price !== undefined && (
                          <span className="px-2 py-1 bg-green-500/20 text-green-300 text-xs rounded-md">
                            💰 {item.price} TL
                          </span>
                        )}
                        {item.category && (
                          <span className="px-2 py-1 bg-purple-500/20 text-purple-300 text-xs rounded-md">
                            🏷️ {item.category}
                          </span>
                        )}
                      </div>
                      {item.notes && (
                        <div className="text-xs text-gray-400 mt-2">📋 {item.notes}</div>
                      )}
                    </div>
                  )}

                  {/* Aksiyon Butonları */}
                  <div className="flex gap-2">
                    <button
                      onClick={() => startEdit(item)}
                      className="flex-1 px-3 py-2 bg-gradient-to-r from-blue-600/80 to-cyan-600/80 text-white text-sm rounded-lg font-semibold shadow-md shadow-blue-500/20 active:scale-95 transition-transform flex items-center justify-center gap-1"
                    >
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                      Düzenle
                    </button>
                    <button
                      onClick={() => onDelete(item.id || index.toString())}
                      className="flex-1 px-3 py-2 bg-gradient-to-r from-red-600/80 to-rose-600/80 text-white text-sm rounded-lg font-semibold shadow-md shadow-red-500/20 active:scale-95 transition-transform flex items-center justify-center gap-1"
                    >
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                      Sil
                    </button>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default memo(FloatingQRHistory);

