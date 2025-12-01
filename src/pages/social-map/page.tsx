import { useState, useEffect, useRef } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import MarkerClusterGroup from 'react-leaflet-cluster';
import { Capacitor } from '@capacitor/core';
import { Camera, CameraResultType, CameraSource } from '@capacitor/camera';
import { Geolocation } from '@capacitor/geolocation';
import { useNavigate, useLocation } from 'react-router-dom';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { databaseService } from '../../services';

// Leaflet marker icon fix for Webpack
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

interface PhotoPost {
  id: string;
  postType: 'photo' | 'music' | 'audio' | 'qr' | 'note';
  photo?: string;
  music?: string;
  audio?: string;
  qrCode?: string;
  qrType?: string;
  note?: string;
  caption: string;
  latitude: number;
  longitude: number;
  timestamp: number;
  userName: string;
}

interface Toast {
  message: string;
  type: 'success' | 'error' | 'info';
}

// Kullanıcı konumuna haritayı taşıyan component
function LocationMarker({ position }: { position: [number, number] | null }) {
  const map = useMap();

  useEffect(() => {
    if (position) {
      map.flyTo(position, 15, { duration: 1.5 });
    }
  }, [position, map]);

  if (!position) return null;

  // Özel kullanıcı marker'ı
  const userIcon = L.divIcon({
    className: 'custom-user-marker',
    html: `
      <div style="
        width: 40px;
        height: 40px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border: 4px solid white;
        border-radius: 50%;
        box-shadow: 0 4px 12px rgba(0,0,0,0.3);
        display: flex;
        align-items: center;
        justify-content: center;
        position: relative;
      ">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="white">
          <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
        </svg>
        <div style="
          position: absolute;
          width: 60px;
          height: 60px;
          border: 2px solid rgba(102, 126, 234, 0.3);
          border-radius: 50%;
          animation: pulse 2s ease-in-out infinite;
        "></div>
      </div>
    `,
    iconSize: [40, 40],
    iconAnchor: [20, 40],
  });

  return (
    <Marker position={position} icon={userIcon}>
      <Popup>
        <div className="text-center">
          <p className="font-semibold text-purple-600">📍 Konumunuz</p>
          <p className="text-xs text-gray-600 mt-1">
            {position[0].toFixed(6)}, {position[1].toFixed(6)}
          </p>
        </div>
      </Popup>
    </Marker>
  );
}

export default function SocialMapPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [userLocation, setUserLocation] = useState<[number, number] | null>(null);
  const [photoPosts, setPhotoPosts] = useState<PhotoPost[]>([]);
  const [showAddModal, setShowAddModal] = useState(false);
  const [postType, setPostType] = useState<'photo' | 'music' | 'audio' | 'qr' | 'note'>('photo');
  const [selectedPhoto, setSelectedPhoto] = useState<string | null>(null);
  const [selectedMusic, setSelectedMusic] = useState<string | null>(null);
  const [selectedAudio, setSelectedAudio] = useState<string | null>(null);
  const [noteText, setNoteText] = useState<string>('');
  const [recording, setRecording] = useState(false);
  const [mediaRecorder, setMediaRecorder] = useState<MediaRecorder | null>(null);
  const [viewFullPhoto, setViewFullPhoto] = useState<PhotoPost | null>(null);
  const [caption, setCaption] = useState('');
  const [userName, setUserName] = useState('Anonim');
  const [toast, setToast] = useState<Toast | null>(null);
  const [loading, setLoading] = useState(false);
  const [mapStyle, setMapStyle] = useState<'standard' | 'dark'>('standard');
  const [showFabMenu, setShowFabMenu] = useState(false);
  const [scanningQR, setScanningQR] = useState<PhotoPost | null>(null);
  const [scannedContent, setScannedContent] = useState<string>('');
  const mapRef = useRef<L.Map | null>(null);

  // QR Create veya Scan History sayfasından gelen state'i kontrol et
  useEffect(() => {
    const state = location.state as any;
    if ((state?.from === 'qr-create' || state?.from === 'scan-history') && state?.qrContent) {
      console.log('📦 Veri alındı:', state);
      
      // QR paylaşım modalını aç
      setPostType('qr');
      
      // Image varsa kullan, yoksa text olarak paylaş
      if (state.qrImage) {
        setSelectedPhoto(state.qrImage);
      }
      
      setCaption(`${state.qrType === 'barcode' ? 'Barkod' : 'QR Kod'}: ${state.qrContent}`);
      setShowAddModal(true);
      
      // Toast göster
      setToast({ message: '📱 Paylaşıma hazır!', type: 'success' });
      setTimeout(() => setToast(null), 3000);
      
      // State'i temizle
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, [location]);

  // Konumu al ve URL parametrelerini kontrol et
  useEffect(() => {
    getCurrentLocation();
    loadPhotoPosts();
    
    // URL'den QR paylaşım parametrelerini kontrol et
    const params = new URLSearchParams(window.location.search);
    if (params.get('shareQR') === 'true') {
      const qrCode = params.get('qrCode');
      const qrType = params.get('qrType');
      const qrContent = params.get('qrContent');
      const caption = params.get('caption');
      
      if (qrCode && qrType && qrContent) {
        // QR paylaşım modalını aç
        setPostType('qr');
        setSelectedPhoto(qrCode); // QR resmi
        setCaption(caption || `QR Kod: ${qrContent}`);
        setShowAddModal(true);
        
        // URL'i temizle
        window.history.replaceState({}, '', window.location.pathname);
        
        showToast('📱 QR kod paylaşıma hazır!', 'success');
      }
    }
  }, []);

  const getCurrentLocation = async () => {
    try {
      console.log('🎯 Konum alma başladı...');
      
      // Mobil için konum izni kontrolü ve isteme
      if (Capacitor.isNativePlatform()) {
        try {
          // Önce izinleri kontrol et
          const permission = await Geolocation.checkPermissions();
          
          // İzin reddedildiyse tekrar iste
          if (permission.location === 'denied' || permission.location === 'prompt') {
            const request = await Geolocation.requestPermissions();
            
            if (request.location !== 'granted') {
              showToast('⚠️ Konum izni gerekli. Lütfen ayarlardan izin verin.', 'error');
              setUserLocation([41.0082, 28.9784]);
              return;
            }
          }
        } catch (permError) {
          // İzin hatası görmezden gel, konum almaya devam et
        }
      } else {
        // Web tarayıcı için konum izni kontrolü
        console.log('🌐 Web tarayıcı algılandı');
        if (!navigator.geolocation) {
          showToast('❌ Tarayıcınız konum özelliğini desteklemiyor.', 'error');
          setUserLocation([41.0082, 28.9784]);
          return;
        }
        
        // Web için izin durumunu kontrol et (mümkünse)
        try {
          if ('permissions' in navigator) {
            const result = await navigator.permissions.query({ name: 'geolocation' });
            console.log('🌐 Web konum izin durumu:', result.state);
            
            if (result.state === 'denied') {
              showToast('⚠️ Konum izni reddedildi. Lütfen tarayıcı ayarlarından konum iznini açın.', 'error');
              setUserLocation([41.0082, 28.9784]);
              return;
            }
          }
        } catch (e) {
          console.log('ℹ️ Permissions API desteklenmiyor, devam ediliyor...');
        }
      }
      
      console.log('📍 Konum bilgisi alınıyor...');
      showToast('📍 Konumunuz alınıyor...', 'info');
      
      const position = await Geolocation.getCurrentPosition({
        enableHighAccuracy: true,
        timeout: 20000,
        maximumAge: 5000,
      });
      
      const coords: [number, number] = [
        position.coords.latitude,
        position.coords.longitude,
      ];
      
      console.log('✅ Konum başarıyla alındı:', coords);
      setUserLocation(coords);
      showToast('✅ Konumunuz belirlendi!', 'success');
      
      // Haritayı konuma odakla
      if (mapRef.current) {
        mapRef.current.flyTo(coords, 15, { duration: 1.5 });
      }
      
    } catch (error: any) {
      // Sadece geliştirme ortamında detaylı hata göster
      if (process.env.NODE_ENV === 'development') {
        console.error('❌ Konum alma hatası:', error);
      }
      
      let errorMessage = 'Konum alınamadı.';
      
      if (error.code === 1) {
        errorMessage = '⚠️ Konum izni reddedildi. Varsayılan konum kullanılıyor.';
      } else if (error.code === 2) {
        errorMessage = '⚠️ Konum bilgisi alınamadı. Varsayılan konum kullanılıyor.';
      } else if (error.code === 3) {
        errorMessage = '⏱️ Konum alma zaman aşımına uğradı. Varsayılan konum kullanılıyor.';
      } else {
        errorMessage = `❌ Konum hatası: ${error.message || 'Bilinmeyen hata'}`;
      }
      
      showToast(errorMessage, 'error');
      setUserLocation([41.0082, 28.9784]); // İstanbul - Varsayılan
    }
  };

  const loadPhotoPosts = async () => {
    try {
      console.log('📸 loadPhotoPosts başladı');
      const db = databaseService;
      await db.ensureInitialized();
      console.log('✅ Database hazır, postlar sorgulanıyor...');
      
      const posts = await db.getPhotoPosts();
      console.log('✅ Query sonucu:', posts.length, 'post bulundu');
      setPhotoPosts(posts);
      console.log('📷 Fotoğraflar state\'e yüklendi:', posts.length);
    } catch (error) {
      console.error('❌ Fotoğraf yükleme hatası:', error);
    }
  };

  const deletePhoto = async (photoId: string) => {
    try {
      const db = databaseService;
      await db.ensureInitialized();

      await db.deletePhotoPost(photoId);
      
      await loadPhotoPosts();
      setViewFullPhoto(null);
      showToast('🗑️ Fotoğraf silindi!', 'success');
    } catch (error) {
      // Sadece geliştirme ortamında detaylı hata göster
      if (process.env.NODE_ENV === 'development') {
        console.error('Fotoğraf silme hatası:', error);
      }
      showToast('Fotoğraf silinemedi!', 'error');
    }
  };

  const showToast = (message: string, type: Toast['type']) => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const takePicture = async () => {
    try {
      const photo = await Camera.getPhoto({
        resultType: CameraResultType.DataUrl,
        source: CameraSource.Camera,
        quality: 80,
        width: 1024,
        height: 1024,
      });

      if (photo.dataUrl) {
        setSelectedPhoto(photo.dataUrl);
        setShowAddModal(true);
      }
    } catch (error) {
      // Sadece geliştirme ortamında detaylı hata göster
      if (process.env.NODE_ENV === 'development') {
        console.error('Fotoğraf çekme hatası:', error);
      }
      showToast('Fotoğraf çekilemedi!', 'error');
    }
  };

  const selectFromGallery = async () => {
    try {
      const photo = await Camera.getPhoto({
        resultType: CameraResultType.DataUrl,
        source: CameraSource.Photos,
        quality: 80,
        width: 1024,
        height: 1024,
      });

      if (photo.dataUrl) {
        setSelectedPhoto(photo.dataUrl);
        setShowAddModal(true);
      }
    } catch (error) {
      // Sadece geliştirme ortamında detaylı hata göster
      if (process.env.NODE_ENV === 'development') {
        console.error('Galeri seçme hatası:', error);
      }
      showToast('Fotoğraf seçilemedi!', 'error');
    }
  };

  // Müzik dosyası seç
  const selectMusic = () => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'audio/*';
    input.onchange = async (e: any) => {
      const file = e.target?.files?.[0];
      if (file) {
        const reader = new FileReader();
        reader.onload = () => {
          setSelectedMusic(reader.result as string);
          setPostType('music');
          setShowAddModal(true);
        };
        reader.readAsDataURL(file);
      }
    };
    input.click();
  };

  // Ses kaydı başlat/durdur
  const toggleRecording = async () => {
    if (recording && mediaRecorder) {
      mediaRecorder.stop();
      setRecording(false);
    } else {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const recorder = new MediaRecorder(stream);
        const chunks: Blob[] = [];

        recorder.ondataavailable = (e) => chunks.push(e.data);
        recorder.onstop = () => {
          const blob = new Blob(chunks, { type: 'audio/webm' });
          const reader = new FileReader();
          reader.onload = () => {
            setSelectedAudio(reader.result as string);
            setPostType('audio');
            setShowAddModal(true);
          };
          reader.readAsDataURL(blob);
          stream.getTracks().forEach(track => track.stop());
        };

        recorder.start();
        setMediaRecorder(recorder);
        setRecording(true);
        showToast('🎤 Ses kaydediliyor...', 'info');
      } catch (error) {
      // Sadece geliştirme ortamında detaylı hata göster
      if (process.env.NODE_ENV === 'development') {
        console.error('Ses kaydı hatası:', error);
      }
      showToast('Mikrofon erişimi reddedildi!', 'error');
      }
    }
  };
  
  // Not ekleme
  const addNote = () => {
    setPostType('note');
    setShowAddModal(true);
  };

  const sharePost = async () => {
    console.log('🚀 sharePost başladı');
    
    if (!userLocation) {
      showToast('Konum gerekli!', 'error');
      return;
    }

    // Not için özel kontrol
    if (postType === 'note' && !noteText.trim()) {
      showToast('Not içeriği gerekli!', 'error');
      return;
    }
    
    // Diğer içerik tipleri için kontrol
    if (postType !== 'note' && !selectedPhoto && !selectedMusic && !selectedAudio && !caption) {
      showToast('En az bir içerik gerekli!', 'error');
      return;
    }

    console.log('✅ Validasyon geçildi, loading başlatılıyor');
    setLoading(true);

    try {
      console.log('🗄️ Database başlatılıyor...');
      const db = databaseService;
      await db.ensureInitialized();
      console.log('✅ Database hazır');

      const newPost: Partial<PhotoPost> = {
        id: `post_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
        postType,
        caption: caption.trim() || `${
          postType === 'photo' ? 'Fotoğraf' : 
          postType === 'music' ? 'Müzik' : 
          postType === 'audio' ? 'Ses kaydı' : 
          postType === 'note' ? 'Not' : 
          'QR Kod'} paylaşımı`,
        latitude: userLocation[0],
        longitude: userLocation[1],
        timestamp: Date.now(),
        userName: userName.trim() || 'Anonim',
      };

      if (postType === 'photo' && selectedPhoto) newPost.photo = selectedPhoto;
      if (postType === 'music' && selectedMusic) newPost.music = selectedMusic;
      if (postType === 'audio' && selectedAudio) newPost.audio = selectedAudio;
      if (postType === 'note') newPost.note = noteText.trim();
      if (postType === 'qr' && selectedPhoto) {
        newPost.qrCode = selectedPhoto; // QR resmi selectedPhoto'da tutuluyor
        newPost.qrType = 'qr';
      }

      console.log('💾 Database INSERT çalıştırılıyor...', newPost);
      await db.addPhotoPost({
        id: newPost.id!,
        postType: newPost.postType!,
        photo: newPost.photo,
        music: newPost.music,
        audio: newPost.audio,
        qrCode: newPost.qrCode,
        qrType: newPost.qrType,
        note: newPost.note,
        caption: newPost.caption!,
        latitude: newPost.latitude!,
        longitude: newPost.longitude!,
        timestamp: newPost.timestamp!,
        userName: newPost.userName!,
      });
      console.log('✅ INSERT tamamlandı');

      console.log('📥 Postlar yeniden yükleniyor...');
      await loadPhotoPosts();
      console.log('✅ Postlar yüklendi');
      
      setShowAddModal(false);
      setSelectedPhoto(null);
      setSelectedMusic(null);
      setSelectedAudio(null);
      setCaption('');
      setNoteText('');
      showToast('✅ İçerik başarıyla paylaşıldı!', 'success');
      console.log('🎉 Paylaşım tamamlandı');
    } catch (error) {
      console.error('❌ Paylaşım hatası:', error);
      
      // Özel hata mesajları
      if (error instanceof Error && error.message.includes('no column named postType')) {
        // Veritabanı yapısı güncel değil, sıfırlama gerekiyor
        showToast('Veritabanı yapısı güncellenecek, lütfen bekleyin...', 'info');
        try {
          // Veritabanını sıfırla ve yeniden oluştur
          await databaseService.resetDatabase();
          showToast('Veritabanı yapısı güncellendi, lütfen tekrar deneyin', 'success');
          return;
        } catch (resetError) {
          console.error('❌ Reset hatası:', resetError);
          showToast('Veritabanı güncellenemedi, lütfen sayfayı yenileyin', 'error');
          return;
        }
      } else {
        showToast(`Paylaşım başarısız: ${error instanceof Error ? error.message : 'Bilinmeyen hata'}`, 'error');
      }
    } finally {
      console.log('🏁 Finally bloğu - loading kapatılıyor');
      setLoading(false);
    }
  };

  // Custom marker icon (post type'a göre) - BÜYÜK VE ŞIK
  const createPostMarkerIcon = (post: PhotoPost) => {
    let content = '';
    let bgColor = '';
    let iconSize: [number, number] = [80, 80];
    let iconAnchor: [number, number] = [40, 80];
    
    if (post.postType === 'photo' && post.photo) {
      return L.divIcon({
        className: 'custom-post-marker',
        html: `
          <div style="
            width: 80px;
            height: 80px;
            border-radius: 20px;
            border: 4px solid white;
            box-shadow: 0 8px 24px rgba(0,0,0,0.4), 0 0 0 2px rgba(102,126,234,0.3);
            overflow: hidden;
            background: white;
            cursor: pointer;
            transition: all 0.3s ease;
          " onmouseover="this.style.transform='scale(1.15) rotate(5deg)';this.style.boxShadow='0 12px 32px rgba(0,0,0,0.5), 0 0 0 3px rgba(102,126,234,0.5)'" onmouseout="this.style.transform='scale(1) rotate(0deg)';this.style.boxShadow='0 8px 24px rgba(0,0,0,0.4), 0 0 0 2px rgba(102,126,234,0.3)'">
            <img src="${post.photo}" style="width: 100%; height: 100%; object-fit: cover;" />
          </div>
        `,
        iconSize: [80, 80],
        iconAnchor: [40, 80],
      });
    }
    
    // Müzik, ses, not ve QR için icon
    if (post.postType === 'music') {
      bgColor = 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';
      content = `
        <svg style="width: 40px; height: 40px; color: white; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
        </svg>
        <div style="position:absolute;bottom:-30px;left:50%;transform:translateX(-50%);background:linear-gradient(135deg,#667eea,#764ba2);padding:6px 16px;border-radius:20px;font-size:13px;font-weight:800;color:white;white-space:nowrap;box-shadow:0 6px 16px rgba(102,126,234,0.5);border:2px solid white;letter-spacing:0.5px;">
          <svg style="width:14px;height:14px;display:inline-block;margin-right:4px;vertical-align:middle;" fill="white" viewBox="0 0 24 24">
            <path d="M8 5v14l11-7z"/>
          </svg>
          ÇAL
        </div>
      `;
      iconSize = [80, 112];
      iconAnchor = [40, 112];
    } else if (post.postType === 'audio') {
      bgColor = 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)';
      content = `
        <svg style="width: 40px; height: 40px; color: white; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
        </svg>
        <div style="position:absolute;bottom:-30px;left:50%;transform:translateX(-50%);background:linear-gradient(135deg,#f093fb,#f5576c);padding:6px 16px;border-radius:20px;font-size:13px;font-weight:800;color:white;white-space:nowrap;box-shadow:0 6px 16px rgba(240,147,251,0.5);border:2px solid white;letter-spacing:0.5px;">
          <svg style="width:14px;height:14px;display:inline-block;margin-right:4px;vertical-align:middle;" fill="white" viewBox="0 0 24 24">
            <path d="M8 5v14l11-7z"/>
          </svg>
          DİNLE
        </div>
      `;
      iconSize = [80, 112];
      iconAnchor = [40, 112];
    } else if (post.postType === 'qr') {
      bgColor = 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)';
      // QR için sadece ikon - içerik GİZLİ
      content = `
        <svg style="width: 48px; height: 48px; color: white; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" />
        </svg>
        <div style="position:absolute;bottom:-30px;left:50%;transform:translateX(-50%);background:linear-gradient(135deg,#43e97b,#38f9d7);padding:6px 16px;border-radius:20px;font-size:13px;font-weight:800;color:white;white-space:nowrap;box-shadow:0 6px 16px rgba(67,233,123,0.5);border:2px solid white;letter-spacing:0.5px;">
          <svg style="width:14px;height:14px;display:inline-block;margin-right:4px;vertical-align:middle;" fill="white" viewBox="0 0 24 24">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>
          </svg>
          TARA
        </div>
      `;
      iconSize = [80, 112];
      iconAnchor = [40, 112];
    } else if (post.postType === 'note') {
      bgColor = 'linear-gradient(135deg, #ffcf1b 0%, #ff881b 100%)';
      // Not içeriğini daha büyük göster
      const notePreview = post.note ? post.note.substring(0, 40) : '';
      content = `<div style="font-size:11px;color:white;font-weight:bold;text-align:center;padding:8px;max-width:72px;overflow:hidden;line-height:1.3;text-shadow:0 2px 4px rgba(0,0,0,0.4);">${notePreview}${notePreview.length >= 40 ? '...' : ''}</div>`;
    }
    
    return L.divIcon({
      className: 'custom-post-marker',
      html: `
        <div style="position:relative;">
          <div style="
            width: 80px;
            height: 80px;
            border-radius: 20px;
            border: 4px solid white;
            box-shadow: 0 8px 24px rgba(0,0,0,0.4), 0 0 0 2px rgba(102,126,234,0.2);
            background: ${bgColor};
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 32px;
            transition: all 0.3s ease;
            position: relative;
          " onmouseover="this.style.transform='scale(1.15) rotate(5deg)';this.style.boxShadow='0 12px 32px rgba(0,0,0,0.5), 0 0 0 3px rgba(102,126,234,0.4)'" onmouseout="this.style.transform='scale(1) rotate(0deg)';this.style.boxShadow='0 8px 24px rgba(0,0,0,0.4), 0 0 0 2px rgba(102,126,234,0.2)'">
            ${content}
          </div>
        </div>
      `,
      iconSize: iconSize,
      iconAnchor: iconAnchor,
    });
  };

  return (
    <div className="h-screen w-full flex flex-col bg-gradient-to-br from-purple-50 to-pink-50">
      {/* Header */}
      <div className="bg-white shadow-lg z-10">
        <div className="max-w-7xl mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-gradient-to-br from-purple-600 to-pink-600 rounded-xl flex items-center justify-center shadow-lg">
                <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                </svg>
              </div>
              <div>
                <h1 className="text-xl font-bold bg-gradient-to-r from-purple-600 to-pink-600 bg-clip-text text-transparent">
                  Sosyal Harita
                </h1>
                <p className="text-xs text-gray-500">{photoPosts.length} paylaşım</p>
              </div>
            </div>
            
            <div className="flex items-center gap-2">
              {/* Ana Sayfa Butonu */}
              <button
                onClick={() => navigate('/')}
                className="p-3 bg-gradient-to-br from-gray-500 to-gray-600 text-white rounded-xl shadow-lg hover:shadow-xl transition-all active:scale-95"
                title="Ana Sayfaya Dön"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
                </svg>
              </button>
              
              {/* Harita Stili Değiştir */}
              <button
                onClick={() => setMapStyle(mapStyle === 'standard' ? 'dark' : 'standard')}
                className="p-3 bg-gradient-to-br from-indigo-500 to-purple-600 text-white rounded-xl shadow-lg hover:shadow-xl transition-all active:scale-95"
                title={mapStyle === 'standard' ? 'Gece Modu' : 'Gündüz Modu'}
              >
                {mapStyle === 'standard' ? (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                  </svg>
                ) : (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
                  </svg>
                )}
              </button>
              
              {/* Konum Butonu */}
              <button
                onClick={getCurrentLocation}
                className="p-3 bg-gradient-to-br from-blue-500 to-blue-600 text-white rounded-xl shadow-lg hover:shadow-xl transition-all active:scale-95"
                title="Konumumu Bul"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Harita */}
      <div className="flex-1 relative">
        {userLocation ? (
          <MapContainer
            center={userLocation}
            zoom={13}
            style={{ height: '100%', width: '100%' }}
            ref={mapRef}
          >
            {/* Harita Katmanı - Dinamik */}
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url={
                mapStyle === 'standard'
                  ? "https://tiles.stadiamaps.com/tiles/alidade_smooth/{z}/{x}/{y}{r}.png"
                  : "https://tiles.stadiamaps.com/tiles/alidade_smooth_dark/{z}/{x}/{y}{r}.png"
              }
              maxZoom={20}
            />
            
            {/* Kullanıcı Konumu */}
            <LocationMarker position={userLocation} />
            
            {/* Paylaşımlar (Fotoğraf, Müzik, Ses, QR) - Cluster ile */}
            <MarkerClusterGroup
              chunkedLoading
              maxClusterRadius={30}
              disableClusteringAtZoom={17}
              spiderfyOnMaxZoom={true}
              showCoverageOnHover={false}
              zoomToBoundsOnClick={true}
              spiderfyDistanceMultiplier={3}
              iconCreateFunction={(cluster) => {
                const count = cluster.getChildCount();
                return L.divIcon({
                  html: `<div style="
                    width: 70px;
                    height: 70px;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    border: 5px solid white;
                    border-radius: 20px;
                    box-shadow: 0 8px 24px rgba(0,0,0,0.4), 0 0 0 3px rgba(102,126,234,0.3);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: white;
                    font-weight: bold;
                    font-size: 22px;
                    transition: all 0.3s ease;
                  ">${count}</div>`,
                  className: 'custom-cluster-icon',
                  iconSize: L.point(70, 70, true),
                });
              }}
            >
              {photoPosts.map((post) => (
                <Marker
                  key={post.id}
                  position={[post.latitude, post.longitude]}
                  icon={createPostMarkerIcon(post)}
                  eventHandlers={{
                    click: () => {
                      // QR için tara modalı aç
                      if (post.postType === 'qr') {
                        setScanningQR(post);
                      } 
                      // Ses/Müzik için otomatik çal
                      else if (post.postType === 'music' || post.postType === 'audio') {
                        // Audio'yu bul ve çal
                        const audioSrc = post.postType === 'music' ? post.music : post.audio;
                        if (audioSrc) {
                          const audio = new Audio(audioSrc);
                          audio.play().catch(() => showToast('Ses çalınamadı!', 'error'));
                        }
                      }
                      // Diğerleri için normal popup
                      else {
                        setViewFullPhoto(post);
                      }
                    }
                  }}
                >
                  <Popup maxWidth={300}>
                  <div className="p-2">
                    {post.postType === 'photo' && post.photo && (
                      <img
                        src={post.photo}
                        alt={post.caption}
                        className="w-full h-48 object-cover rounded-lg mb-2 cursor-pointer hover:opacity-90 transition-opacity"
                        onClick={() => setViewFullPhoto(post)}
                      />
                    )}
                    {post.postType === 'music' && post.music && (
                      <div className="bg-gradient-to-br from-purple-100 to-pink-100 p-6 rounded-xl mb-2 shadow-lg">
                        <div className="flex items-center justify-center mb-3">
                          <div className="w-16 h-16 bg-gradient-to-br from-purple-500 to-pink-500 rounded-full flex items-center justify-center shadow-lg">
                            <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
                            </svg>
                          </div>
                        </div>
                        <p className="text-purple-700 font-bold text-center mb-3">🎵 Müzik</p>
                        <audio controls className="w-full h-12 md:h-14" controlsList="nodownload" preload="metadata">
                          <source src={post.music} type="audio/mpeg" />
                          <source src={post.music} type="audio/ogg" />
                          <source src={post.music} type="audio/wav" />
                        </audio>
                      </div>
                    )}
                    {post.postType === 'audio' && post.audio && (
                      <div className="bg-gradient-to-br from-pink-100 to-red-100 p-6 rounded-xl mb-2 shadow-lg">
                        <div className="flex items-center justify-center mb-3">
                          <div className="w-16 h-16 bg-gradient-to-br from-pink-500 to-red-500 rounded-full flex items-center justify-center shadow-lg">
                            <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
                            </svg>
                          </div>
                        </div>
                        <p className="text-pink-700 font-bold text-center mb-3">🎙️ Ses Kaydı</p>
                        <audio controls className="w-full h-12 md:h-14" controlsList="nodownload" preload="metadata">
                          <source src={post.audio} type="audio/mpeg" />
                          <source src={post.audio} type="audio/ogg" />
                          <source src={post.audio} type="audio/wav" />
                          <source src={post.audio} type="audio/webm" />
                        </audio>
                      </div>
                    )}
                    {post.postType === 'qr' && post.qrCode && (
                      <div className="relative">
                        <img
                          src={post.qrCode}
                          alt="QR Kod"
                          className="w-full h-48 object-contain rounded-lg mb-2"
                        />
                        <button
                          onClick={() => setScanningQR(post)}
                          className="absolute inset-0 flex items-center justify-center bg-black/50 hover:bg-black/70 rounded-lg transition-all group"
                        >
                          <div className="text-center">
                            <div className="w-16 h-16 bg-green-500 rounded-full flex items-center justify-center mx-auto mb-2 group-hover:scale-110 transition-transform">
                              <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" />
                              </svg>
                            </div>
                            <p className="text-white font-bold">📸 Tara</p>
                          </div>
                        </button>
                      </div>
                    )}
                    
                    {post.postType === 'note' && post.note && (
                      <div className="bg-amber-100 p-4 rounded-lg mb-2 border-l-4 border-amber-500">
                        <p className="text-gray-800 whitespace-pre-wrap">{post.note}</p>
                      </div>
                    )}
                    <p className="font-semibold text-purple-700 mb-1">{post.userName}</p>
                    <p className="text-sm text-gray-700 mb-2">{post.caption}</p>
                    <p className="text-xs text-gray-500">
                      {new Date(post.timestamp).toLocaleString('tr-TR')}
                    </p>
                  </div>
                </Popup>
              </Marker>
            ))}
            </MarkerClusterGroup>
          </MapContainer>
        ) : (
          <div className="flex items-center justify-center h-full">
            <div className="text-center">
              <div className="w-16 h-16 mx-auto mb-4 border-4 border-purple-500 border-t-transparent rounded-full animate-spin"></div>
              <p className="text-gray-600">Harita yükleniyor...</p>
            </div>
          </div>
        )}

        {/* Glassmorphism Floating Action Buttons */}
        <div className="absolute bottom-24 right-4 flex flex-col items-center gap-3 z-[1000]">
          {/* Ana FAB Butonu */}
          <button
            onClick={() => setShowFabMenu(!showFabMenu)}
            className={`w-16 h-16 bg-white/20 backdrop-blur-xl border border-white/40 text-white rounded-full shadow-[0_8px_32px_rgba(0,0,0,0.2)] hover:shadow-[0_8px_32px_rgba(0,0,0,0.3)] transition-all duration-300 flex items-center justify-center ${showFabMenu ? 'rotate-45' : ''}`}
            style={{
              boxShadow: '0 8px 32px rgba(31, 38, 135, 0.2)',
            }}
            title="Paylaşım Menüsü"
          >
            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
            </svg>
          </button>
          
          {/* Açılır Menü */}
          <div className={`flex flex-col gap-3 transition-all duration-300 origin-bottom ${showFabMenu ? 'opacity-100 scale-100' : 'opacity-0 scale-0 pointer-events-none'}`}>
            {/* Not Ekle */}
            <button
              onClick={() => {
                addNote();
                setShowFabMenu(false);
              }}
              className="w-14 h-14 bg-gradient-to-br from-amber-400/80 to-amber-600/80 backdrop-blur-lg text-white rounded-full shadow-lg hover:shadow-xl transition-all active:scale-95 flex items-center justify-center"
              style={{
                boxShadow: '0 4px 20px rgba(251, 191, 36, 0.3)',
              }}
              title="Not Ekle"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
            </button>
            
            {/* Ses Kaydı */}
            <button
              onClick={(e) => {
                e.stopPropagation();
                toggleRecording();
                // Menüyü açık tut kayıt sırasında
              }}
              className={`w-14 h-14 backdrop-blur-lg text-white rounded-full shadow-lg hover:shadow-xl transition-all active:scale-95 flex items-center justify-center ${recording ? 'bg-gradient-to-br from-red-500/80 to-red-700/80 animate-pulse' : 'bg-gradient-to-br from-pink-500/80 to-pink-700/80'}`}
              style={{
                boxShadow: recording ? '0 4px 20px rgba(239, 68, 68, 0.4)' : '0 4px 20px rgba(236, 72, 153, 0.3)',
              }}
              title={recording ? 'Kaydı Durdur ⏹️' : 'Ses Kaydı Başlat 🎤'}
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                {recording ? (
                  <rect x="9" y="9" width="6" height="6" strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
                )}
              </svg>
            </button>
            
            {/* Müzik Ekle */}
            <button
              onClick={() => {
                selectMusic();
                setShowFabMenu(false);
              }}
              className="w-14 h-14 bg-gradient-to-br from-indigo-500/80 to-purple-700/80 backdrop-blur-lg text-white rounded-full shadow-lg hover:shadow-xl transition-all active:scale-95 flex items-center justify-center"
              style={{
                boxShadow: '0 4px 20px rgba(99, 102, 241, 0.3)',
              }}
              title="Müzik Ekle"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
              </svg>
            </button>
            
            {/* Galeriden Seç */}
            <button
              onClick={() => {
                setPostType('photo');
                selectFromGallery();
                setShowFabMenu(false);
              }}
              className="w-14 h-14 bg-gradient-to-br from-blue-500/80 to-cyan-600/80 backdrop-blur-lg text-white rounded-full shadow-lg hover:shadow-xl transition-all active:scale-95 flex items-center justify-center"
              style={{
                boxShadow: '0 4px 20px rgba(59, 130, 246, 0.3)',
              }}
              title="Galeriden Seç"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </button>
            
            {/* Fotoğraf Çek */}
            <button
              onClick={() => {
                setPostType('photo');
                takePicture();
                setShowFabMenu(false);
              }}
              className="w-14 h-14 bg-gradient-to-br from-purple-500/80 to-pink-600/80 backdrop-blur-lg text-white rounded-full shadow-lg hover:shadow-xl transition-all active:scale-95 flex items-center justify-center"
              style={{
                boxShadow: '0 4px 20px rgba(168, 85, 247, 0.3)',
              }}
              title="Fotoğraf Çek"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      {/* Fotoğraf Paylaşım Modalı */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-[2000] flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl max-w-md w-full max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-2xl font-bold bg-gradient-to-r from-purple-600 to-pink-600 bg-clip-text text-transparent">
                  {postType === 'photo' && '📸 Fotoğraf Paylaş'}
                  {postType === 'music' && '🎵 Müzik Paylaş'}
                  {postType === 'audio' && '🎤 Ses Kaydı Paylaş'}
                  {postType === 'qr' && '📱 QR Kod Paylaş'}
                  {postType === 'note' && '📝 Not Ekle'}
                </h2>
                <button
                  onClick={() => {
                    setShowAddModal(false);
                    setSelectedPhoto(null);
                    setSelectedMusic(null);
                    setSelectedAudio(null);
                    setNoteText('');
                    setCaption('');
                  }}
                  className="p-2 hover:bg-gray-100 rounded-full transition-colors"
                >
                  <svg className="w-6 h-6 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>

              {/* Fotoğraf Önizleme */}
              {postType === 'photo' && selectedPhoto && (
                <img
                  src={selectedPhoto}
                  alt="Seçili fotoğraf"
                  className="w-full h-64 object-cover rounded-2xl mb-4 shadow-lg"
                />
              )}
              
              {/* Müzik Önizleme */}
              {postType === 'music' && selectedMusic && (
                <div className="bg-gradient-to-br from-purple-100 to-purple-200 p-6 rounded-2xl mb-4 shadow-lg">
                  <div className="text-center mb-4">
                    <span className="text-6xl">🎵</span>
                    <p className="mt-2 text-purple-700 font-semibold">Müzik Dosyası Seçildi</p>
                  </div>
                  <audio controls className="w-full">
                    <source src={selectedMusic} />
                  </audio>
                </div>
              )}
              
              {/* Ses Kaydı Önizleme */}
              {postType === 'audio' && selectedAudio && (
                <div className="bg-gradient-to-br from-pink-100 to-pink-200 p-6 rounded-2xl mb-4 shadow-lg">
                  <div className="text-center mb-4">
                    <span className="text-6xl">🎤</span>
                    <p className="mt-2 text-pink-700 font-semibold">Ses Kaydı Hazır</p>
                  </div>
                  <audio controls className="w-full">
                    <source src={selectedAudio} />
                  </audio>
                </div>
              )}
              
              {/* Not Önizleme */}
              {postType === 'note' && (
                <div className="bg-gradient-to-br from-amber-100 to-amber-200 p-6 rounded-2xl mb-4 shadow-lg">
                  <div className="text-center mb-4">
                    <span className="text-6xl">📝</span>
                    <p className="mt-2 text-amber-700 font-semibold">Not Ekle</p>
                  </div>
                  <textarea
                    value={noteText}
                    onChange={(e) => setNoteText(e.target.value)}
                    placeholder="Notunuzu buraya yazın..."
                    className="w-full h-32 p-4 border-2 border-amber-300 rounded-xl focus:border-amber-500 focus:outline-none transition-colors"
                  />
                </div>
              )}

              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Kullanıcı Adı
                  </label>
                  <input
                    type="text"
                    value={userName}
                    onChange={(e) => setUserName(e.target.value)}
                    placeholder="İsminiz"
                    className="w-full px-4 py-3 border-2 border-gray-200 rounded-xl focus:border-purple-500 focus:outline-none transition-colors"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Açıklama
                  </label>
                  <textarea
                    value={caption}
                    onChange={(e) => setCaption(e.target.value)}
                    placeholder="Fotoğrafınız hakkında birşeyler yazın..."
                    rows={3}
                    className="w-full px-4 py-3 border-2 border-gray-200 rounded-xl focus:border-purple-500 focus:outline-none transition-colors resize-none"
                  />
                </div>

                {userLocation && (
                  <div className="bg-purple-50 p-3 rounded-xl">
                    <p className="text-sm text-purple-700 font-medium flex items-center gap-2">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                      </svg>
                      Konum: {userLocation[0].toFixed(4)}, {userLocation[1].toFixed(4)}
                    </p>
                  </div>
                )}

                <button
                  onClick={sharePost}
                  disabled={loading}
                  className="w-full py-4 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-xl font-semibold shadow-lg hover:shadow-xl transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {loading ? (
                    <span className="flex items-center justify-center gap-2">
                      <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      Paylaşılıyor...
                    </span>
                  ) : (
                    '📸 Paylaş'
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tam Ekran Fotoğraf Modalı */}
      {viewFullPhoto && (
        <div className="fixed inset-0 bg-black/95 z-[9999] flex items-center justify-center p-4" onClick={() => setViewFullPhoto(null)}>
          <div className="relative max-w-4xl w-full h-full flex items-center justify-center" onClick={(e) => e.stopPropagation()}>
            {/* Kapatma ve Silme Butonları */}
            <div className="absolute top-4 right-4 z-10 flex gap-3">
              {/* Silme Butonu */}
              <button
                onClick={() => {
                  if (window.confirm('Bu fotoğrafı silmek istediğinizden emin misiniz?')) {
                    deletePhoto(viewFullPhoto.id);
                  }
                }}
                className="p-3 bg-red-500/80 backdrop-blur-md text-white rounded-full hover:bg-red-600/90 transition-all shadow-2xl"
                title="Fotoğrafı Sil"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
              
              {/* Kapatma Butonu */}
              <button
                onClick={() => setViewFullPhoto(null)}
                className="p-3 bg-white/10 backdrop-blur-md text-white rounded-full hover:bg-white/20 transition-all shadow-2xl"
                title="Kapat"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            {/* İçerik */}
            <div className="w-full h-full flex flex-col items-center justify-center">
              {/* Fotoğraf */}
              {viewFullPhoto.postType === 'photo' && viewFullPhoto.photo && (
                <img
                  src={viewFullPhoto.photo}
                  alt={viewFullPhoto.caption}
                  className="max-w-full max-h-[70vh] object-contain rounded-2xl shadow-2xl"
                />
              )}
              
              {/* Müzik */}
              {viewFullPhoto.postType === 'music' && viewFullPhoto.music && (
                <div className="w-full max-w-2xl bg-gradient-to-br from-purple-500/20 to-pink-500/20 backdrop-blur-xl rounded-2xl p-8 shadow-2xl">
                  <div className="flex items-center justify-center mb-6">
                    <div className="w-32 h-32 bg-gradient-to-br from-purple-500 to-pink-500 rounded-full flex items-center justify-center shadow-2xl">
                      <svg className="w-16 h-16 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
                      </svg>
                    </div>
                  </div>
                  <p className="text-white text-2xl font-bold text-center mb-6">🎵 Müzik Paylaşımı</p>
                  <audio controls className="w-full h-16 md:h-20" controlsList="nodownload" preload="metadata">
                    <source src={viewFullPhoto.music} type="audio/mpeg" />
                    <source src={viewFullPhoto.music} type="audio/ogg" />
                    <source src={viewFullPhoto.music} type="audio/wav" />
                    Tarayıcınız ses dosyalarını desteklemiyor.
                  </audio>
                </div>
              )}
              
              {/* Ses Kaydı */}
              {viewFullPhoto.postType === 'audio' && viewFullPhoto.audio && (
                <div className="w-full max-w-2xl bg-gradient-to-br from-pink-500/20 to-red-500/20 backdrop-blur-xl rounded-2xl p-8 shadow-2xl">
                  <div className="flex items-center justify-center mb-6">
                    <div className="w-32 h-32 bg-gradient-to-br from-pink-500 to-red-500 rounded-full flex items-center justify-center shadow-2xl">
                      <svg className="w-16 h-16 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
                      </svg>
                    </div>
                  </div>
                  <p className="text-white text-2xl font-bold text-center mb-6">🎙️ Ses Kaydı</p>
                  <audio controls className="w-full h-16 md:h-20" controlsList="nodownload" preload="metadata">
                    <source src={viewFullPhoto.audio} type="audio/mpeg" />
                    <source src={viewFullPhoto.audio} type="audio/ogg" />
                    <source src={viewFullPhoto.audio} type="audio/wav" />
                    <source src={viewFullPhoto.audio} type="audio/webm" />
                    Tarayıcınız ses dosyalarını desteklemiyor.
                  </audio>
                </div>
              )}
              
              {/* QR Kod */}
              {viewFullPhoto.postType === 'qr' && viewFullPhoto.qrCode && (
                <div className="bg-white p-8 rounded-2xl shadow-2xl">
                  <img
                    src={viewFullPhoto.qrCode}
                    alt="QR Kod"
                    className="w-full max-w-md h-auto object-contain"
                  />
                </div>
              )}
              
              {/* Not */}
              {viewFullPhoto.postType === 'note' && viewFullPhoto.note && (
                <div className="w-full max-w-2xl bg-gradient-to-br from-amber-500/20 to-orange-500/20 backdrop-blur-xl rounded-2xl p-8 shadow-2xl">
                  <div className="flex items-center justify-center mb-6">
                    <div className="w-32 h-32 bg-gradient-to-br from-amber-500 to-orange-500 rounded-full flex items-center justify-center shadow-2xl">
                      <svg className="w-16 h-16 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </div>
                  </div>
                  <p className="text-white text-2xl font-bold text-center mb-6">📝 Not</p>
                  <div className="bg-white/10 backdrop-blur-md rounded-xl p-6 border border-white/20">
                    <p className="text-white text-lg leading-relaxed whitespace-pre-wrap">{viewFullPhoto.note}</p>
                  </div>
                </div>
              )}
              
              {/* Bilgi Kartı */}
              <div className="mt-6 bg-white/10 backdrop-blur-xl rounded-2xl p-6 max-w-2xl w-full">
                <div className="flex items-start gap-4">
                  <div className="w-12 h-12 bg-gradient-to-br from-purple-500 to-pink-500 rounded-full flex items-center justify-center flex-shrink-0">
                    <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                  </div>
                  <div className="flex-1">
                    <p className="text-xl font-bold text-white mb-2">{viewFullPhoto.userName}</p>
                    <p className="text-white/90 text-lg mb-3">{viewFullPhoto.caption}</p>
                    <div className="flex items-center gap-4 text-white/70 text-sm">
                      <span className="flex items-center gap-1">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        {new Date(viewFullPhoto.timestamp).toLocaleString('tr-TR')}
                      </span>
                      <span className="flex items-center gap-1">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                        </svg>
                        {viewFullPhoto.latitude.toFixed(4)}, {viewFullPhoto.longitude.toFixed(4)}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Toast Bildirim */}
      {/* QR Tarama Modal */}
      {scanningQR && (
        <div className="fixed inset-0 bg-black/90 z-[3000] flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl max-w-md w-full overflow-hidden">
            {/* Matrix scanning effect */}
            {!scannedContent && (
              <div className="relative bg-gradient-to-br from-green-900 to-black p-8">
                <div className="absolute inset-0 opacity-20">
                  <div className="absolute inset-0 bg-[repeating-linear-gradient(0deg,transparent,transparent_2px,#00ff00_2px,#00ff00_4px)] animate-pulse"></div>
                </div>
                <div className="relative">
                  <img
                    src={scanningQR.qrCode || ''}
                    alt="QR Kod"
                    className="w-full h-64 object-contain rounded-lg mb-4"
                  />
                  <div className="absolute inset-0 border-4 border-green-500 rounded-lg animate-pulse">
                    <div className="absolute top-0 left-0 w-8 h-8 border-t-4 border-l-4 border-green-400"></div>
                    <div className="absolute top-0 right-0 w-8 h-8 border-t-4 border-r-4 border-green-400"></div>
                    <div className="absolute bottom-0 left-0 w-8 h-8 border-b-4 border-l-4 border-green-400"></div>
                    <div className="absolute bottom-0 right-0 w-8 h-8 border-b-4 border-r-4 border-green-400"></div>
                  </div>
                  {/* Tarama çizgisi */}
                  <div className="absolute inset-x-0 h-1 bg-green-400 shadow-[0_0_20px_#00ff00] animate-scan-line"></div>
                </div>
                <p className="text-green-400 text-center font-mono text-lg animate-pulse mt-4">
                  🔍 TARANIY OR...
                </p>
              </div>
            )}
            
            {/* Tarama sonucu */}
            {scannedContent && (
              <div className="p-6">
                <div className="flex items-center justify-center mb-4">
                  <div className="w-16 h-16 bg-green-500 rounded-full flex items-center justify-center">
                    <svg className="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                </div>
                <h3 className="text-xl font-bold text-center mb-4 text-gray-800">✅ Tarama Başarılı!</h3>
                <div className="bg-gray-100 p-4 rounded-lg mb-4">
                  <p className="text-sm text-gray-600 mb-2">📝 İçerik:</p>
                  <p className="text-gray-900 font-mono break-all">{scannedContent}</p>
                </div>
              </div>
            )}
            
            <div className="p-4 bg-gray-50 flex gap-2">
              {!scannedContent && (
                <button
                  onClick={() => {
                    // QR kodu "tara" (simüle et)
                    setTimeout(() => {
                      setScannedContent(scanningQR.caption || 'QR Kod İçeriği');
                    }, 2000);
                  }}
                  className="flex-1 py-3 bg-gradient-to-r from-green-500 to-green-600 text-white rounded-xl font-semibold hover:shadow-lg transition-all"
                >
                  🔍 Taramayı Başlat
                </button>
              )}
              <button
                onClick={() => {
                  setScanningQR(null);
                  setScannedContent('');
                }}
                className="flex-1 py-3 bg-gray-200 text-gray-700 rounded-xl font-semibold hover:bg-gray-300 transition-all"
              >
                {scannedContent ? '✓ Tamam' : '✕ İptal'}
              </button>
            </div>
          </div>
        </div>
      )}

      {toast && (
        <div className="fixed top-20 left-1/2 transform -translate-x-1/2 z-[3000] animate-slide-down">
          <div
            className={`px-6 py-4 rounded-2xl shadow-2xl flex items-center gap-3 ${
              toast.type === 'success'
                ? 'bg-green-500 text-white'
                : toast.type === 'error'
                ? 'bg-red-500 text-white'
                : 'bg-blue-500 text-white'
            }`}
          >
            {toast.type === 'success' && (
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
            )}
            {toast.type === 'error' && (
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            )}
            <p className="font-medium">{toast.message}</p>
          </div>
        </div>
      )}

      {/* CSS Animations */}
      <style>{`
        @keyframes pulse {
          0%, 100% {
            opacity: 0.3;
            transform: scale(1);
          }
          50% {
            opacity: 0.1;
            transform: scale(1.2);
          }
        }
        
        @keyframes slide-down {
          from {
            opacity: 0;
            transform: translate(-50%, -20px);
          }
          to {
            opacity: 1;
            transform: translate(-50%, 0);
          }
        }
        
        .animate-slide-down {
          animation: slide-down 0.3s ease-out;
        }
        
        .custom-user-marker,
        .custom-photo-marker {
          background: transparent !important;
          border: none !important;
        }
        
        /* Mobil uyumlu audio player */
        audio {
          border-radius: 12px;
          outline: none;
        }
        
        audio::-webkit-media-controls-panel {
          background: linear-gradient(to right, rgba(139, 92, 246, 0.1), rgba(236, 72, 153, 0.1));
          border-radius: 12px;
        }
        
        audio::-webkit-media-controls-play-button,
        audio::-webkit-media-controls-pause-button {
          background: rgba(139, 92, 246, 0.2);
          border-radius: 50%;
          transform: scale(1.3);
          margin: 0 10px;
        }
        
        audio::-webkit-media-controls-current-time-display,
        audio::-webkit-media-controls-time-remaining-display {
          font-weight: 600;
          color: #6b21a8;
        }
        
        /* Mobil için dokunma alanlarını büyüt */
        @media (max-width: 768px) {
          audio {
            min-height: 56px;
          }
        }
      `}</style>
    </div>
  );
}

