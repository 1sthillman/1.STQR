import { useEffect, useState } from 'react';
import { BrowserRouter } from 'react-router-dom';
import Router from './router';
import { SplashVideo } from './components';
import { databaseService } from './services';

function App() {
  const [showSplashVideo, setShowSplashVideo] = useState(true);

  useEffect(() => {
    // Database'i arka planda başlat (sessizce)
    databaseService.ensureInitialized().catch(() => {
      // Hata olsa bile uygulama çalışmaya devam eder
      // Sorgular kuyruğa alınır, database hazır olunca işlenir
    });
  }, []);

  // BAŞARILI - Uygulama Hazır
  return (
    <BrowserRouter basename={__BASE_PATH__}>
      <div className="App">
        {/* Video splash screen - önce göster, programatik olarak kapat */}
        {showSplashVideo && (
          <SplashVideo
            videoSrc="splash.mp4"
            onComplete={() => setShowSplashVideo(false)}
          />
        )}
        <Router />
      </div>
    </BrowserRouter>
  );
}

export default App;


