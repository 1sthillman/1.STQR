import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 flex items-center justify-center px-6">
      <div className="text-center max-w-md">
        <div className="mb-8">
          <div className="w-64 h-48 mx-auto bg-gradient-to-br from-blue-500 to-purple-600 rounded-2xl flex items-center justify-center text-white text-9xl font-bold">
            404
          </div>
        </div>
        
        <h1 className="text-6xl font-bold text-gray-900 mb-4">404</h1>
        <h2 className="text-2xl font-semibold text-gray-700 mb-4">Sayfa Bulunamadı</h2>
        <p className="text-gray-600 mb-8 leading-relaxed">
          Aradığınız sayfa mevcut değil veya taşınmış olabilir. 
          Ana sayfaya dönerek devam edebilirsiniz.
        </p>
        
        <div className="space-y-4">
          <Link
            to="/"
            className="inline-flex items-center space-x-2 bg-gradient-to-r from-blue-600 to-purple-600 text-white px-8 py-4 rounded-2xl font-semibold hover:from-blue-700 hover:to-purple-700 transition-all shadow-lg hover:shadow-xl transform hover:scale-105"
          >
            <i className="ri-home-line text-xl"></i>
            <span>Ana Sayfaya Dön</span>
          </Link>
          
          <div className="flex justify-center space-x-4 mt-6">
            <Link
              to="/qr-olustur"
              className="flex items-center space-x-2 text-gray-600 hover:text-blue-600 transition-colors"
            >
              <i className="ri-qr-code-line"></i>
              <span>QR Oluştur</span>
            </Link>
            <Link
              to="/qr-tara"
              className="flex items-center space-x-2 text-gray-600 hover:text-blue-600 transition-colors"
            >
              <i className="ri-scan-line"></i>
              <span>QR Tara</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}












































