/**
 * 🎨 PREMIUM QR CODE GENERATOR
 * Modern, VIP, Professional Design
 */

import { useState, useRef, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Navigation from '../../components/Navigation';
import Toast from '../../components/Toast';

// @ts-ignore
import QRCodeStyling from 'qr-code-styling';

interface QRType {
  id: string;
  name: string;
  svg: string;
}

const qrTypes: QRType[] = [
  { id: 'text', name: 'Metin', svg: '<path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>' },
  { id: 'url', name: 'URL', svg: '<path d="M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z"/>' },
  { id: 'email', name: 'E-posta', svg: '<path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/>' },
  { id: 'phone', name: 'Telefon', svg: '<path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/>' },
  { id: 'wifi', name: 'WiFi', svg: '<path d="M1 9l2 2c4.97-4.97 13.03-4.97 18 0l2-2C16.93 2.93 7.08 2.93 1 9zm8 8l3 3 3-3c-1.65-1.66-4.34-1.66-6 0zm-4-4l2 2c2.76-2.76 7.24-2.76 10 0l2-2C15.14 9.14 8.87 9.14 5 13z"/>' },
  { id: 'vcard', name: 'Kartvizit', svg: '<path d="M12 5.9c1.16 0 2.1.94 2.1 2.1s-.94 2.1-2.1 2.1S9.9 9.16 9.9 8s.94-2.1 2.1-2.1m0 9c2.97 0 6.1 1.46 6.1 2.1v1.1H5.9V17c0-.64 3.13-2.1 6.1-2.1M12 4C9.79 4 8 5.79 8 8s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm0 9c-2.67 0-8 1.34-8 4v3h16v-3c0-2.66-5.33-4-8-4z"/>' },
];

const presetTemplates = [
  {
    name: 'Klasik',
    config: { dotsType: 'square' as const, dotsColor: '#000000', backgroundColor: '#ffffff', gradientType: 'none' as const, cornerSquareColor: '#000000', cornerDotColor: '#000000' }
  },
  {
    name: 'Modern',
    config: { dotsType: 'extra-rounded' as const, dotsColor: '#8b5cf6', backgroundColor: '#ffffff', gradientType: 'linear' as const, cornerSquareColor: '#ec4899', cornerDotColor: '#ec4899' }
  },
  {
    name: 'Profesyonel',
    config: { dotsType: 'classy-rounded' as const, dotsColor: '#2563eb', backgroundColor: '#f0f9ff', gradientType: 'linear' as const, cornerSquareColor: '#0891b2', cornerDotColor: '#0891b2' }
  },
  {
    name: 'Minimalist',
    config: { dotsType: 'rounded' as const, dotsColor: '#475569', backgroundColor: '#f8fafc', gradientType: 'none' as const, cornerSquareColor: '#475569', cornerDotColor: '#475569' }
  },
];

export default function QRCreateNew() {
  const [selectedType, setSelectedType] = useState<QRType>(qrTypes[0]);
  const [inputData, setInputData] = useState('');
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'warning' | 'info' } | null>(null);
  
  const [activeTab, setActiveTab] = useState<'type' | 'style' | 'colors' | 'advanced'>('type');
  
  // QR Options
  const [qrSize, setQrSize] = useState(512);
  const [dotsType, setDotsType] = useState<'rounded' | 'dots' | 'classy' | 'classy-rounded' | 'square' | 'extra-rounded'>('rounded');
  const [cornerSquareType, setCornerSquareType] = useState<'dot' | 'square' | 'extra-rounded'>('extra-rounded');
  const [cornerDotType, setCornerDotType] = useState<'dot' | 'square'>('dot');
  const [dotsColor, setDotsColor] = useState('#000000');
  const [backgroundColor, setBackgroundColor] = useState('#ffffff');
  const [cornerSquareColor, setCornerSquareColor] = useState('#000000');
  const [cornerDotColor, setCornerDotColor] = useState('#000000');
  const [gradientType, setGradientType] = useState<'none' | 'linear' | 'radial'>('none');
  const [gradientColor1, setGradientColor1] = useState('#000000');
  const [gradientColor2, setGradientColor2] = useState('#3b82f6');
  const [logoImage, setLogoImage] = useState<string>('');
  const [errorCorrection, setErrorCorrection] = useState<'L' | 'M' | 'Q' | 'H'>('M');
  const [margin, setMargin] = useState(10);

  const qrCodeRef = useRef<HTMLDivElement>(null);
  const qrCode = useRef<any>(null);
  const logoInputRef = useRef<HTMLInputElement>(null);

  // Initialize QR
  useEffect(() => {
    if (!qrCode.current && qrCodeRef.current) {
      qrCode.current = new QRCodeStyling({
        width: qrSize,
        height: qrSize,
        type: 'svg',
        data: 'https://1stqr.app',
        margin: margin,
        qrOptions: {
          typeNumber: 0,
          mode: 'Byte',
          errorCorrectionLevel: errorCorrection
        },
        imageOptions: {
          hideBackgroundDots: true,
          imageSize: 0.4,
          margin: 5,
        },
        dotsOptions: {
          color: dotsColor,
          type: dotsType
        },
        backgroundOptions: {
          color: backgroundColor,
        },
        cornersSquareOptions: {
          color: cornerSquareColor,
          type: cornerSquareType,
        },
        cornersDotOptions: {
          color: cornerDotColor,
          type: cornerDotType,
        }
      });

      qrCodeRef.current.innerHTML = '';
      qrCode.current.append(qrCodeRef.current);
    }
  }, []);

  // Update QR
  useEffect(() => {
    if (qrCode.current) {
      const dotsColorFinal = gradientType === 'none' ? dotsColor : undefined;
      const gradient = gradientType !== 'none' ? {
        type: gradientType,
        rotation: 0,
        colorStops: [
          { offset: 0, color: gradientColor1 },
          { offset: 1, color: gradientColor2 }
        ]
      } : undefined;

      qrCode.current.update({
        width: qrSize,
        height: qrSize,
        margin: margin,
        qrOptions: { errorCorrectionLevel: errorCorrection },
        image: logoImage || undefined,
        dotsOptions: {
          color: dotsColorFinal,
          gradient: gradient,
          type: dotsType
        },
        backgroundOptions: { color: backgroundColor },
        cornersSquareOptions: { color: cornerSquareColor, type: cornerSquareType },
        cornersDotOptions: { color: cornerDotColor, type: cornerDotType }
      });
    }
  }, [qrSize, dotsType, cornerSquareType, cornerDotType, dotsColor, backgroundColor, cornerSquareColor, cornerDotColor, gradientType, gradientColor1, gradientColor2, logoImage, errorCorrection, margin]);

  const generateQR = () => {
    if (!inputData.trim()) {
      setToast({ message: 'Lütfen içerik girin', type: 'warning' });
      return;
    }

    let qrData = inputData;
    switch (selectedType.id) {
      case 'email': qrData = `mailto:${inputData}`; break;
      case 'phone': qrData = `tel:${inputData}`; break;
      case 'wifi': qrData = `WIFI:T:WPA;S:${inputData};P:;;`; break;
    }

    if (qrCode.current) {
      qrCode.current.update({ data: qrData });
      setToast({ message: 'QR kod oluşturuldu', type: 'success' });
    }
  };

  const downloadQR = (format: 'png' | 'svg') => {
    if (qrCode.current) {
      qrCode.current.download({
        name: `qr-${selectedType.id}-${Date.now()}`,
        extension: format
      });
      setToast({ message: `${format.toUpperCase()} olarak indirildi`, type: 'success' });
    }
  };

  const applyPreset = (preset: any) => {
    setDotsType(preset.config.dotsType);
    setDotsColor(preset.config.dotsColor);
    setBackgroundColor(preset.config.backgroundColor);
    setGradientType(preset.config.gradientType);
    setCornerSquareColor(preset.config.cornerSquareColor);
    setCornerDotColor(preset.config.cornerDotColor);
    if (preset.config.gradientType !== 'none') {
      setGradientColor1(preset.config.dotsColor);
      setGradientColor2(preset.config.cornerSquareColor);
    }
    setToast({ message: `${preset.name} teması uygulandı`, type: 'success' });
  };

  const handleLogoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        setLogoImage(event.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-purple-950 to-slate-950">
      {/* Compact Header */}
      <div className="bg-gradient-to-r from-purple-600/90 to-pink-600/90 backdrop-blur-xl border-b border-white/10">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Link to="/" className="p-2 hover:bg-white/10 rounded-lg transition-all">
              <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
            </Link>
            <div>
              <h1 className="text-lg font-bold text-white">QR Kod Oluştur PRO</h1>
              <p className="text-xs text-white/80">Profesyonel Tasarım</p>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto p-4 pb-24">
        <div className="grid lg:grid-cols-2 gap-4">
          {/* Left: Controls */}
          <div className="space-y-4">
            {/* Tabs */}
            <div className="bg-white/5 backdrop-blur-xl rounded-2xl p-1 border border-white/10">
              <div className="grid grid-cols-4 gap-1">
                {[
                  { id: 'type' as const, label: 'Tür', icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z' },
                  { id: 'style' as const, label: 'Stil', icon: 'M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01' },
                  { id: 'colors' as const, label: 'Renk', icon: 'M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01' },
                  { id: 'advanced' as const, label: 'Gelişmiş', icon: 'M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4' }
                ].map((tab) => (
                  <button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id)}
                    className={`p-2.5 rounded-xl transition-all ${
                      activeTab === tab.id
                        ? 'bg-gradient-to-r from-purple-600 to-pink-600 text-white shadow-lg'
                        : 'text-white/60 hover:text-white hover:bg-white/5'
                    }`}
                  >
                    <svg className="w-5 h-5 mx-auto mb-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={tab.icon} />
                    </svg>
                    <div className="text-[10px] font-medium">{tab.label}</div>
                  </button>
                ))}
              </div>
            </div>

            {/* Tab Content */}
            <div className="bg-white/5 backdrop-blur-xl rounded-2xl p-4 border border-white/10">
              {activeTab === 'type' && (
                <div className="space-y-4">
                  <h3 className="text-sm font-semibold text-white">QR Kod Türü</h3>
                  <div className="grid grid-cols-3 gap-2">
                    {qrTypes.map((type) => (
                      <button
                        key={type.id}
                        onClick={() => setSelectedType(type)}
                        className={`p-3 rounded-xl border transition-all ${
                          selectedType.id === type.id
                            ? 'border-purple-500 bg-purple-500/20'
                            : 'border-white/10 hover:border-white/30'
                        }`}
                      >
                        <svg className="w-6 h-6 mx-auto mb-1 text-white" fill="currentColor" viewBox="0 0 24 24">
                          <path d={type.svg} />
                        </svg>
                        <div className="text-xs text-white font-medium">{type.name}</div>
                      </button>
                    ))}
                  </div>
                  
                  <div>
                    <label className="block text-xs font-medium text-white/80 mb-2">İçerik</label>
                    <textarea
                      value={inputData}
                      onChange={(e) => setInputData(e.target.value)}
                      placeholder={`${selectedType.name} içeriğini girin...`}
                      className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-purple-500"
                      rows={3}
                    />
                  </div>

                  <button
                    onClick={generateQR}
                    className="w-full py-3 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-xl font-semibold hover:shadow-lg transition-all"
                  >
                    QR Kod Oluştur
                  </button>
                </div>
              )}

              {activeTab === 'style' && (
                <div className="space-y-4">
                  <h3 className="text-sm font-semibold text-white">Hazır Tasarımlar</h3>
                  <div className="grid grid-cols-2 gap-2">
                    {presetTemplates.map((preset) => (
                      <button
                        key={preset.name}
                        onClick={() => applyPreset(preset)}
                        className="p-3 rounded-xl border border-white/10 hover:border-white/30 bg-white/5 hover:bg-white/10 transition-all text-white text-sm font-medium"
                      >
                        {preset.name}
                      </button>
                    ))}
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-white/80 mb-2">Nokta Şekli</label>
                    <div className="grid grid-cols-3 gap-2">
                      {(['square', 'rounded', 'dots', 'classy', 'classy-rounded', 'extra-rounded'] as const).map((type) => (
                        <button
                          key={type}
                          onClick={() => setDotsType(type)}
                          className={`p-2 rounded-lg border text-xs ${
                            dotsType === type
                              ? 'border-purple-500 bg-purple-500/20 text-white'
                              : 'border-white/10 text-white/60 hover:text-white'
                          }`}
                        >
                          {type}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div>
                    <input
                      ref={logoInputRef}
                      type="file"
                      accept="image/*"
                      onChange={handleLogoUpload}
                      className="hidden"
                      id="logo-upload-new"
                    />
                    <label
                      htmlFor="logo-upload-new"
                      className="block w-full p-3 bg-white/5 border border-white/10 rounded-xl text-white text-sm font-medium text-center cursor-pointer hover:bg-white/10 transition-all"
                    >
                      {logoImage ? 'Logo Değiştir' : 'Logo Ekle'}
                    </label>
                  </div>
                </div>
              )}

              {activeTab === 'colors' && (
                <div className="space-y-4">
                  <div>
                    <label className="block text-xs font-medium text-white/80 mb-2">Gradient</label>
                    <div className="grid grid-cols-3 gap-2">
                      {(['none', 'linear', 'radial'] as const).map((type) => (
                        <button
                          key={type}
                          onClick={() => setGradientType(type)}
                          className={`p-2 rounded-lg border text-xs ${
                            gradientType === type
                              ? 'border-purple-500 bg-purple-500/20 text-white'
                              : 'border-white/10 text-white/60 hover:text-white'
                          }`}
                        >
                          {type === 'none' ? 'Yok' : type === 'linear' ? 'Doğrusal' : 'Radyal'}
                        </button>
                      ))}
                    </div>
                  </div>

                  {gradientType === 'none' ? (
                    <div>
                      <label className="block text-xs font-medium text-white/80 mb-2">Nokta Rengi</label>
                      <input
                        type="color"
                        value={dotsColor}
                        onChange={(e) => setDotsColor(e.target.value)}
                        className="w-full h-10 rounded-lg cursor-pointer"
                      />
                    </div>
                  ) : (
                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className="block text-xs font-medium text-white/80 mb-2">Renk 1</label>
                        <input
                          type="color"
                          value={gradientColor1}
                          onChange={(e) => setGradientColor1(e.target.value)}
                          className="w-full h-10 rounded-lg cursor-pointer"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-medium text-white/80 mb-2">Renk 2</label>
                        <input
                          type="color"
                          value={gradientColor2}
                          onChange={(e) => setGradientColor2(e.target.value)}
                          className="w-full h-10 rounded-lg cursor-pointer"
                        />
                      </div>
                    </div>
                  )}

                  <div>
                    <label className="block text-xs font-medium text-white/80 mb-2">Arka Plan</label>
                    <input
                      type="color"
                      value={backgroundColor}
                      onChange={(e) => setBackgroundColor(e.target.value)}
                      className="w-full h-10 rounded-lg cursor-pointer"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-medium text-white/80 mb-2">Köşe Kare</label>
                      <input
                        type="color"
                        value={cornerSquareColor}
                        onChange={(e) => setCornerSquareColor(e.target.value)}
                        className="w-full h-10 rounded-lg cursor-pointer"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-white/80 mb-2">Köşe Nokta</label>
                      <input
                        type="color"
                        value={cornerDotColor}
                        onChange={(e) => setCornerDotColor(e.target.value)}
                        className="w-full h-10 rounded-lg cursor-pointer"
                      />
                    </div>
                  </div>
                </div>
              )}

              {activeTab === 'advanced' && (
                <div className="space-y-4">
                  <div>
                    <label className="block text-xs font-medium text-white/80 mb-2">Boyut: {qrSize}px</label>
                    <input
                      type="range"
                      min="256"
                      max="1024"
                      step="128"
                      value={qrSize}
                      onChange={(e) => setQrSize(Number(e.target.value))}
                      className="w-full accent-purple-600"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-white/80 mb-2">Kenar: {margin}px</label>
                    <input
                      type="range"
                      min="0"
                      max="50"
                      value={margin}
                      onChange={(e) => setMargin(Number(e.target.value))}
                      className="w-full accent-purple-600"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-white/80 mb-2">Hata Düzeltme</label>
                    <div className="grid grid-cols-4 gap-2">
                      {(['L', 'M', 'Q', 'H'] as const).map((level) => (
                        <button
                          key={level}
                          onClick={() => setErrorCorrection(level)}
                          className={`p-2 rounded-lg border text-xs ${
                            errorCorrection === level
                              ? 'border-purple-500 bg-purple-500/20 text-white'
                              : 'border-white/10 text-white/60 hover:text-white'
                          }`}
                        >
                          {level}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-white/80 mb-2">Köşe Şekilleri</label>
                    <div className="space-y-2">
                      <div>
                        <div className="text-[10px] text-white/60 mb-1">Kare</div>
                        <div className="grid grid-cols-3 gap-2">
                          {(['dot', 'square', 'extra-rounded'] as const).map((type) => (
                            <button
                              key={type}
                              onClick={() => setCornerSquareType(type)}
                              className={`p-1.5 rounded-lg border text-[10px] ${
                                cornerSquareType === type
                                  ? 'border-purple-500 bg-purple-500/20 text-white'
                                  : 'border-white/10 text-white/60'
                              }`}
                            >
                              {type}
                            </button>
                          ))}
                        </div>
                      </div>
                      <div>
                        <div className="text-[10px] text-white/60 mb-1">Nokta</div>
                        <div className="grid grid-cols-2 gap-2">
                          {(['dot', 'square'] as const).map((type) => (
                            <button
                              key={type}
                              onClick={() => setCornerDotType(type)}
                              className={`p-1.5 rounded-lg border text-[10px] ${
                                cornerDotType === type
                                  ? 'border-purple-500 bg-purple-500/20 text-white'
                                  : 'border-white/10 text-white/60'
                              }`}
                            >
                              {type}
                            </button>
                          ))}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Right: Preview */}
          <div className="space-y-4">
            <div className="bg-white/5 backdrop-blur-xl rounded-2xl p-6 border border-white/10">
              <h3 className="text-sm font-semibold text-white mb-4 text-center">Önizleme</h3>
              <div className="bg-white rounded-2xl p-6 flex items-center justify-center min-h-[400px]">
                <div ref={qrCodeRef} className="flex items-center justify-center"></div>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <button
                onClick={() => downloadQR('png')}
                className="p-3 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-xl font-semibold hover:shadow-lg transition-all"
              >
                PNG İndir
              </button>
              <button
                onClick={() => downloadQR('svg')}
                className="p-3 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-xl font-semibold hover:shadow-lg transition-all"
              >
                SVG İndir
              </button>
            </div>
          </div>
        </div>
      </div>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
      <Navigation />
    </div>
  );
}













