import { useEffect, useRef, useState } from 'react';
import { Capacitor } from '@capacitor/core';

// Android için yardımcı bekletme fonksiyonu
const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

interface SplashVideoProps {
  onComplete?: () => void;
  videoSrc?: string;
}

const SplashVideo = ({ onComplete, videoSrc = 'splash.mp4' }: SplashVideoProps) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [showVideo, setShowVideo] = useState(true);
  const playAttemptedRef = useRef(false);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    // PLATFORM-SPECIFIC VIDEO PATH
    let videoPath: string;
    if (Capacitor.isNativePlatform()) {
      videoPath = `${window.location.origin}/${videoSrc}`;
    } else {
      videoPath = `/${videoSrc}`;
    }
    
    // Video elementini hazırla
    video.src = videoPath;
    video.muted = true; // Önce sessiz başlat (autoplay için)
    video.autoplay = false; // Manuel kontrol
    video.playsInline = true;
    video.controls = false;
    video.loop = false;
    
    // Tüm platformlar için kritik ayarlar
    video.setAttribute('playsinline', 'true');
    video.setAttribute('webkit-playsinline', 'true');
    video.setAttribute('preload', 'auto');
    
    // Android özel ayarlar
    if (Capacitor.isNativePlatform()) {
      video.setAttribute('x5-video-player-type', 'h5');
      video.setAttribute('x5-video-player-fullscreen', 'false');
      video.setAttribute('x5-video-orientation', 'portraint');
      video.setAttribute('x-webkit-airplay', 'allow');
    }
    
    // Video bittiğinde
    const handleVideoEnd = () => {
      setShowVideo(false);
      setTimeout(() => {
        if (onComplete) {
          onComplete();
        }
      }, 500);
    };
    
    video.addEventListener('ended', handleVideoEnd);
    
    // UNİVERSAL VIDEO BAŞLATMA - TEK SEFER
    const playVideo = async () => {
      if (playAttemptedRef.current) return;
      playAttemptedRef.current = true;
      
      try {
        // Video metadata yüklenmesini bekle
        if (video.readyState < 2) {
          await new Promise((resolve) => {
            const metadataHandler = () => resolve(true);
            video.addEventListener('loadedmetadata', metadataHandler, { once: true });
            video.addEventListener('canplay', metadataHandler, { once: true });
            setTimeout(() => resolve(false), 5000);
          });
        }
        
        // İLK DENEME: SESLİ OYNATMA
        try {
          video.muted = false;
          video.volume = 1.0;
          await video.play();
          return;
        } catch {
          // Sessiz başlat
          video.muted = true;
          await video.play();
          
          // Mobilde ses aç
          if (Capacitor.isNativePlatform()) {
            await sleep(500);
            video.muted = false;
            video.volume = 1.0;
          } else {
            // Web'de kullanıcı etkileşimi bekle
            const unlockAudio = () => {
              video.muted = false;
              video.volume = 1.0;
            };
            document.addEventListener('click', unlockAudio, { once: true });
            document.addEventListener('touchstart', unlockAudio, { once: true });
          }
        }
      } catch (error) {
        // Hata durumunda video geç
        setTimeout(() => handleVideoEnd(), 1000);
      }
    };
    
    // Video yüklendiğinde başlat
    const handleCanPlay = () => {
      if (!playAttemptedRef.current) {
        playVideo();
      }
    };
    
    video.addEventListener('canplay', handleCanPlay);
    
    // Kısa bir gecikme sonra başlat (fallback)
    const initTimer = setTimeout(() => {
      if (!playAttemptedRef.current) {
        playVideo();
      }
    }, 500);
    
    return () => {
      clearTimeout(initTimer);
      video.removeEventListener('ended', handleVideoEnd);
      video.removeEventListener('canplay', handleCanPlay);
    };
  }, [onComplete, videoSrc]);

  return (
    <div 
      className={`fixed inset-0 z-[9999] bg-black flex items-center justify-center 
                 transition-opacity duration-500 ${showVideo ? 'opacity-100' : 'opacity-0 pointer-events-none'}`}
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        zIndex: 999999, // Ultra yüksek zIndex
        backgroundColor: 'black',
        userSelect: 'none', // Kullanıcı seçimi engelle
        WebkitTapHighlightColor: 'transparent', // Mobil tap vurgusunu kaldır
        touchAction: 'manipulation', // Mobil touch optimize
      }}
    >
      {/* Ana video oynatıcı - HER PLATFORMDA ÇALIŞIR */}
      <video 
        ref={videoRef}
        className="max-w-full max-h-full w-full h-full object-cover"
        playsInline
        preload="auto"
        crossOrigin="anonymous"
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'cover',
          position: 'absolute',
          top: 0,
          left: 0,
          background: 'black',
          touchAction: 'manipulation',
          zIndex: 1,
        }}
        onClick={e => {
          // Video tıklamayı ele al - ses aç ve oynat
          const video = e.currentTarget as HTMLVideoElement;
          if (video) {
            video.muted = false;
            video.volume = 1.0;
            if (video.paused) {
              video.play().catch(() => {});
            }
          }
          e.preventDefault();
          e.stopPropagation();
        }}
      />
      
      {/* Arka plan güvenlik katmanı - birden fazla element kullanarak alternatif kanallar */}
      <div 
        className="absolute inset-0 z-10 opacity-0"
        onClick={() => {
          // Arka plan güvenliği - videonun oynatılmaya çalışılması
          const video = videoRef.current;
          if (video) {
            video.muted = true;
            video.play().catch(() => {});
          }
        }}
      />
    </div>
  );
};

export default SplashVideo;
