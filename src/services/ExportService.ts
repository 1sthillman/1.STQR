import { Share } from '@capacitor/share';
import { Filesystem, Directory, Encoding } from '@capacitor/filesystem';

export class ExportService {
  /**
   * CSV formatında veri dışa aktar
   * @param data Dışa aktarılacak veri (object array)
   * @param filename Dosya adı
   * @returns CSV formatındaki veri
   */
  static async exportAsCSV(data: any[], filename: string): Promise<string> {
    if (!data || !data.length) {
      throw new Error('Dışa aktarılacak veri bulunamadı');
    }
    
    // CSV başlıkları
    const headers = Object.keys(data[0]);
    
    // CSV satırları
    const csvRows = [
      headers.join(','), // Başlıklar
      ...data.map(row => 
        headers
          .map(header => {
            let cell = row[header];
            
            // İçerikte virgül, yeni satır veya tırnak işareti varsa özel işlem yap
            if (cell === null || cell === undefined) {
              return '';
            }
            
            cell = String(cell);
            
            // CSV içinde tırnak ve virgül varsa özel işlem
            if (cell.includes(',') || cell.includes('"') || cell.includes('\n')) {
              cell = cell.replace(/"/g, '""'); // Tırnakları escape et
              cell = `"${cell}"`; // Tırnak içine al
            }
            
            return cell;
          })
          .join(',')
      )
    ];
    
    // CSV içeriğini birleştir
    const csvContent = csvRows.join('\n');
    
    // Dosyayı kaydet
    try {
      // Mobil cihazlarda dosya sistemi kullan
      const result = await Filesystem.writeFile({
        path: `${filename}.csv`,
        data: csvContent,
        directory: Directory.Documents,
        encoding: Encoding.UTF8
      });
      
      return result.uri;
    } catch (error) {
      console.error('CSV dosyası kaydedilemedi:', error);
      
      // Web için indirme seçeneği
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${filename}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      return url;
    }
  }
  
  /**
   * JSON formatında veri dışa aktar
   * @param data Dışa aktarılacak veri
   * @param filename Dosya adı
   * @returns JSON dosya URL'i
   */
  static async exportAsJSON(data: any, filename: string): Promise<string> {
    const jsonString = JSON.stringify(data, null, 2);
    
    // Dosyayı kaydet
    try {
      // Mobil cihazlarda dosya sistemi kullan
      const result = await Filesystem.writeFile({
        path: `${filename}.json`,
        data: jsonString,
        directory: Directory.Documents,
        encoding: Encoding.UTF8
      });
      
      return result.uri;
    } catch (error) {
      console.error('JSON dosyası kaydedilemedi:', error);
      
      // Web için indirme seçeneği
      const blob = new Blob([jsonString], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${filename}.json`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      return url;
    }
  }
  
  /**
   * Share API ile paylaş
   * @param title Paylaşım başlığı
   * @param text Paylaşım metni
   * @param url Paylaşılacak URL (opsiyonel)
   * @param files Paylaşılacak dosyalar (opsiyonel)
   */
  static async shareContent(title: string, text: string, url?: string, files?: string[]): Promise<void> {
    try {
      await Share.share({
        title,
        text,
        url,
        files,
        dialogTitle: 'Paylaş',
      });
    } catch (error) {
      console.error('Paylaşım hatası:', error);
      throw new Error('İçerik paylaşılamadı');
    }
  }
}





































