import { useState } from 'react';

interface ReceiptPrinterProps {
  cart: any[];
  total: number;
  paymentMethod: string;
  cashAmount?: number;
  change?: number;
  onClose: () => void;
}

export default function ReceiptPrinter({ cart, total, paymentMethod, cashAmount, change, onClose }: ReceiptPrinterProps) {
  const [isPrinting, setIsPrinting] = useState(false);

  const generateReceipt = () => {
    const date = new Date().toLocaleString('tr-TR');
    const receiptNumber = Date.now().toString().slice(-8);
    
    return `
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    @page {
      size: 80mm auto;
      margin: 0;
    }
    body {
      font-family: 'Courier New', monospace;
      font-size: 12px;
      margin: 0;
      padding: 10px;
      width: 80mm;
    }
    .header {
      text-align: center;
      margin-bottom: 15px;
      border-bottom: 2px dashed #000;
      padding-bottom: 10px;
    }
    .logo {
      font-size: 24px;
      font-weight: bold;
      letter-spacing: 2px;
    }
    .info {
      font-size: 10px;
      margin-top: 5px;
    }
    .items {
      margin: 15px 0;
    }
    .item {
      margin: 8px 0;
      display: flex;
      justify-content: space-between;
    }
    .item-name {
      flex: 1;
    }
    .item-qty {
      width: 30px;
      text-align: center;
    }
    .item-price {
      width: 60px;
      text-align: right;
    }
    .divider {
      border-top: 1px dashed #000;
      margin: 10px 0;
    }
    .total-section {
      margin-top: 15px;
      border-top: 2px solid #000;
      padding-top: 10px;
    }
    .total-row {
      display: flex;
      justify-content: space-between;
      margin: 5px 0;
    }
    .total-row.grand {
      font-size: 16px;
      font-weight: bold;
      margin-top: 10px;
      border-top: 2px solid #000;
      padding-top: 10px;
    }
    .footer {
      text-align: center;
      margin-top: 20px;
      font-size: 10px;
      border-top: 2px dashed #000;
      padding-top: 10px;
    }
    .barcode {
      text-align: center;
      margin: 10px 0;
      font-family: 'Libre Barcode 128', cursive;
      font-size: 40px;
    }
  </style>
</head>
<body>
  <div class="header">
    <div class="logo">1'STQR</div>
    <div class="info">MARKET KASASI</div>
    <div class="info">FİŞ NO: ${receiptNumber}</div>
    <div class="info">${date}</div>
  </div>

  <div class="items">
    ${cart.map(item => `
      <div class="item">
        <div class="item-name">${item.name}</div>
        <div class="item-qty">x${item.quantity}</div>
        <div class="item-price">₺${(item.price * item.quantity).toFixed(2)}</div>
      </div>
      <div style="font-size: 10px; color: #666; margin-left: 5px;">
        #${item.barcode}
      </div>
    `).join('')}
  </div>

  <div class="total-section">
    <div class="total-row">
      <span>Ürün Sayısı:</span>
      <span>${cart.reduce((sum, item) => sum + item.quantity, 0)} Adet</span>
    </div>
    <div class="total-row">
      <span>Ara Toplam:</span>
      <span>₺${total.toFixed(2)}</span>
    </div>
    <div class="total-row grand">
      <span>TOPLAM:</span>
      <span>₺${total.toFixed(2)}</span>
    </div>
    
    ${paymentMethod === 'cash' && cashAmount ? `
      <div style="margin-top: 15px; border-top: 1px dashed #000; padding-top: 10px;">
        <div class="total-row">
          <span>Ödeme Yöntemi:</span>
          <span>NAKİT</span>
        </div>
        <div class="total-row">
          <span>Verilen:</span>
          <span>₺${cashAmount.toFixed(2)}</span>
        </div>
        <div class="total-row" style="font-weight: bold;">
          <span>Para Üstü:</span>
          <span>₺${(change || 0).toFixed(2)}</span>
        </div>
      </div>
    ` : `
      <div style="margin-top: 15px; border-top: 1px dashed #000; padding-top: 10px;">
        <div class="total-row">
          <span>Ödeme Yöntemi:</span>
          <span>KREDİ KARTI</span>
        </div>
      </div>
    `}
  </div>

  <div class="barcode">${receiptNumber}</div>

  <div class="footer">
    <div>Bizi Tercih Ettiğiniz İçin Teşekkürler!</div>
    <div style="margin-top: 5px;">www.1stqr.com</div>
    <div style="margin-top: 10px; font-size: 9px;">
      Bu fiş fatura yerine geçmez
    </div>
  </div>
</body>
</html>
    `.trim();
  };

  const handlePrint = async () => {
    setIsPrinting(true);
    
    try {
      const receiptHTML = generateReceipt();
      
      // Yeni pencere aç ve yazdır
      const printWindow = window.open('', '_blank', 'width=800,height=600');
      if (printWindow) {
        printWindow.document.write(receiptHTML);
        printWindow.document.close();
        
        // Biraz bekle ve yazdır
        setTimeout(() => {
          printWindow.print();
          printWindow.close();
        }, 250);
      }
      
      alert('✅ Fiş yazdırılıyor!');
      onClose();
    } catch (error) {
      console.error('Yazdırma hatası:', error);
      alert('⚠️ Yazdırma işlemi başarısız oldu.');
    } finally {
      setIsPrinting(false);
    }
  };

  const handleDownloadPDF = () => {
    const receiptHTML = generateReceipt();
    const blob = new Blob([receiptHTML], { type: 'text/html' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `fis-${Date.now()}.html`;
    a.click();
    URL.revokeObjectURL(url);
    alert('✅ Fiş indirildi!');
  };

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-6">
      <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full">
        <div className="bg-gradient-to-r from-purple-600 to-pink-600 text-white p-6 rounded-t-2xl">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-bold">🧾 Fiş Yazdır</h2>
            <button
              onClick={onClose}
              className="text-white hover:bg-white/20 p-2 rounded-lg transition-all"
            >
              <i className="ri-close-line text-2xl"></i>
            </button>
          </div>
        </div>

        <div className="p-6 space-y-4">
          <div className="text-center py-4 bg-gray-50 rounded-xl">
            <div className="text-4xl mb-2">📄</div>
            <div className="text-lg font-bold text-gray-900">Fiş Hazır!</div>
            <div className="text-sm text-gray-600 mt-1">
              {cart.length} ürün - ₺{total.toFixed(2)}
            </div>
          </div>

          <div className="space-y-3">
            <button
              onClick={handlePrint}
              disabled={isPrinting}
              className="w-full bg-gradient-to-r from-green-600 to-teal-600 text-white py-4 rounded-xl font-bold hover:shadow-lg transition-all flex items-center justify-center space-x-2 disabled:opacity-50"
            >
              <i className="ri-printer-line text-2xl"></i>
              <span>{isPrinting ? 'Yazdırılıyor...' : 'Yazıcıdan Yazdır'}</span>
            </button>

            <button
              onClick={handleDownloadPDF}
              className="w-full bg-gradient-to-r from-blue-600 to-purple-600 text-white py-4 rounded-xl font-bold hover:shadow-lg transition-all flex items-center justify-center space-x-2"
            >
              <i className="ri-download-line text-2xl"></i>
              <span>HTML Olarak İndir</span>
            </button>

            <button
              onClick={onClose}
              className="w-full bg-gray-100 text-gray-700 py-3 rounded-xl font-semibold hover:bg-gray-200 transition-all"
            >
              Kapat
            </button>
          </div>

          <div className="bg-blue-50 border-2 border-blue-200 rounded-xl p-4">
            <div className="flex items-start gap-3">
              <i className="ri-information-line text-2xl text-blue-600"></i>
              <div className="text-sm text-gray-700">
                <p className="font-semibold text-blue-900 mb-1">💡 İpucu:</p>
                <p>Thermal yazıcınız aynı ağdaysa, yazdır butonuna basın. Android sistem yazdırma servisini kullanır.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}






































