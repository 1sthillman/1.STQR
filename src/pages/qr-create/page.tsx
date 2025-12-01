/**
 * 🎨 ULTIMATE QR & BARCODE GENERATOR
 * Full-featured, Mobile-optimized, Share & Save
 * Features: 44 Themes + QR + Barcode + Share + Products
 */

import { useState, useRef, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Share } from '@capacitor/share';
import { Filesystem, Directory } from '@capacitor/filesystem';
import Navigation from '../../components/Navigation';
import Toast from '../../components/Toast';
// @ts-ignore
import JsBarcode from 'jsbarcode';
// @ts-ignore
import QRCodeStyling from 'qr-code-styling';

interface QRType {
  id: string;
  name: string;
}

const qrTypes: QRType[] = [
  { id: 'text', name: 'Metin' },
  { id: 'url', name: 'URL' },
  { id: 'email', name: 'E-posta' },
  { id: 'phone', name: 'Telefon' },
  { id: 'sms', name: 'SMS' },
  { id: 'wifi', name: 'WiFi' },
  { id: 'vcard', name: 'Kartvizit' },
  { id: 'location', name: 'Konum' },
];

const barcodeTypes = [
  { id: 'CODE128', name: 'CODE128', desc: 'Evrensel (Harf+Rakam)' },
  { id: 'EAN13', name: 'EAN-13', desc: 'Perakende (12-13 rakam)' },
  { id: 'EAN8', name: 'EAN-8', desc: 'Küçük Ürünler (7-8 rakam)' },
  { id: 'UPC', name: 'UPC', desc: 'ABD Perakende (11-12 rakam)' },
  { id: 'CODE39', name: 'CODE39', desc: 'Endüstriyel (Harf+Rakam)' },
  { id: 'ITF14', name: 'ITF-14', desc: 'Koli/Palet (13-14 rakam)' },
];

// Barcode Presets (25 Professional Themes)
const barcodePresets = [
  // Classic & Professional
  { name: 'Klasik', color: '#000000', bg: '#ffffff', height: 100, width: 2 },
  { name: 'Koyu', color: '#ffffff', bg: '#000000', height: 120, width: 2.5 },
  { name: 'Kompakt', color: '#1f2937', bg: '#f3f4f6', height: 90, width: 2 },
  { name: 'İnce', color: '#000000', bg: '#ffffff', height: 80, width: 1.5 },
  { name: 'Kalın', color: '#000000', bg: '#ffffff', height: 150, width: 3 },
  
  // Vibrant Colors
  { name: 'Mavi', color: '#1e40af', bg: '#dbeafe', height: 100, width: 2 },
  { name: 'Yeşil', color: '#166534', bg: '#dcfce7', height: 100, width: 2 },
  { name: 'Kırmızı', color: '#991b1b', bg: '#fee2e2', height: 100, width: 2 },
  { name: 'Mor', color: '#6b21a8', bg: '#f3e8ff', height: 100, width: 2 },
  { name: 'Turuncu', color: '#9a3412', bg: '#ffedd5', height: 100, width: 2 },
  
  // Dark Themes
  { name: 'Midnight', color: '#e0e7ff', bg: '#1e1b4b', height: 110, width: 2.5 },
  { name: 'Carbon', color: '#fafafa', bg: '#171717', height: 110, width: 2.5 },
  { name: 'Slate', color: '#f1f5f9', bg: '#334155', height: 100, width: 2 },
  
  // Neon & Bright
  { name: 'Neon Yeşil', color: '#22c55e', bg: '#052e16', height: 110, width: 2.5 },
  { name: 'Neon Pink', color: '#ec4899', bg: '#1f2937', height: 110, width: 2.5 },
  { name: 'Neon Mavi', color: '#06b6d4', bg: '#1e293b', height: 110, width: 2.5 },
  
  // Pastel
  { name: 'Pastel Mor', color: '#7c3aed', bg: '#faf5ff', height: 100, width: 2 },
  { name: 'Pastel Mavi', color: '#0284c7', bg: '#f0f9ff', height: 100, width: 2 },
  { name: 'Pastel Pembe', color: '#be185d', bg: '#fdf2f8', height: 100, width: 2 },
  
  // Special
  { name: 'Retro', color: '#dc2626', bg: '#fef3c7', height: 100, width: 2 },
  { name: 'Ocean', color: '#0369a1', bg: '#e0f2fe', height: 100, width: 2 },
  { name: 'Forest', color: '#15803d', bg: '#f0fdf4', height: 100, width: 2 },
  { name: 'Sunset', color: '#ea580c', bg: '#fff7ed', height: 100, width: 2 },
  { name: 'Gold', color: '#a16207', bg: '#fefce8', height: 110, width: 2.5 },
  { name: 'Royal', color: '#5b21b6', bg: '#f5f3ff', height: 110, width: 2.5 },
];

// 44 PROFESSIONAL PRESETS
const presetTemplates = [
  { name: 'Klasik', dots: 'square' as const, bg: '#ffffff', fg: '#000000', bgfg: '#000000', gradient: 'none' as const, corner: 'square' as const },
  { name: 'Modern', dots: 'extra-rounded' as const, bg: '#ffffff', fg: '#8b5cf6', bgfg: '#ec4899', gradient: 'linear' as const, corner: 'extra-rounded' as const },
  { name: 'Profesyonel', dots: 'classy-rounded' as const, bg: '#f0f9ff', fg: '#2563eb', bgfg: '#0891b2', gradient: 'linear' as const, corner: 'extra-rounded' as const },
  { name: 'Minimalist', dots: 'rounded' as const, bg: '#f8fafc', fg: '#475569', bgfg: '#475569', gradient: 'none' as const, corner: 'extra-rounded' as const },
  { name: 'Elegant', dots: 'classy-rounded' as const, bg: '#fafafa', fg: '#18181b', bgfg: '#52525b', gradient: 'linear' as const, corner: 'extra-rounded' as const },
  { name: 'Koyu', dots: 'classy' as const, bg: '#0f172a', fg: '#ffffff', bgfg: '#94a3b8', gradient: 'radial' as const, corner: 'extra-rounded' as const },
  { name: 'Midnight', dots: 'extra-rounded' as const, bg: '#1e1b4b', fg: '#a78bfa', bgfg: '#c4b5fd', gradient: 'radial' as const, corner: 'extra-rounded' as const },
  { name: 'Obsidian', dots: 'classy' as const, bg: '#0a0a0a', fg: '#e4e4e7', bgfg: '#71717a', gradient: 'linear' as const, corner: 'square' as const },
  { name: 'Carbon', dots: 'square' as const, bg: '#171717', fg: '#fafafa', bgfg: '#a1a1aa', gradient: 'none' as const, corner: 'square' as const },
  { name: 'Renkli', dots: 'dots' as const, bg: '#ffffff', fg: '#ef4444', bgfg: '#22c55e', gradient: 'radial' as const, corner: 'dot' as const },
  { name: 'Neon', dots: 'extra-rounded' as const, bg: '#000000', fg: '#00ff00', bgfg: '#00ff00', gradient: 'radial' as const, corner: 'extra-rounded' as const },
  { name: 'Cyber', dots: 'square' as const, bg: '#0a0a0a', fg: '#00ffff', bgfg: '#00ffff', gradient: 'radial' as const, corner: 'square' as const },
  { name: 'Neon Pink', dots: 'extra-rounded' as const, bg: '#000000', fg: '#ff00ff', bgfg: '#ff00ff', gradient: 'radial' as const, corner: 'dot' as const },
  { name: 'Pastel', dots: 'rounded' as const, bg: '#fef3c7', fg: '#f59e0b', bgfg: '#fbbf24', gradient: 'linear' as const, corner: 'extra-rounded' as const },
  { name: 'Lavender', dots: 'classy-rounded' as const, bg: '#f5f3ff', fg: '#8b5cf6', bgfg: '#a78bfa', gradient: 'linear' as const, corner: 'extra-rounded' as const },
  { name: 'Mint', dots: 'dots' as const, bg: '#f0fdfa', fg: '#14b8a6', bgfg: '#2dd4bf', gradient: 'radial' as const, corner: 'dot' as const },
  { name: 'Peach', dots: 'rounded' as const, bg: '#fff7ed', fg: '#f97316', bgfg: '#fb923c', gradient: 'linear' as const, corner: 'extra-rounded' as const },
  { name: 'Ocean', dots: 'classy-rounded' as const, bg: '#e0f2fe', fg: '#0284c7', bgfg: '#0ea5e9', gradient: 'linear' as const, corner: 'extra-rounded' as const },
  { name: 'Forest', dots: 'classy' as const, bg: '#f0fdf4', fg: '#16a34a', bgfg: '#22c55e', gradient: 'radial' as const, corner: 'extra-rounded' as const },
  { name: 'Sunset', dots: 'extra-rounded' as const, bg: '#ffffff', fg: '#f97316', bgfg: '#fbbf24', gradient: 'linear' as const, corner: 'extra-rounded' as const },
  { name: 'Sky', dots: 'classy-rounded' as const, bg: '#dbeafe', fg: '#3b82f6', bgfg: '#60a5fa', gradient: 'radial' as const, corner: 'extra-rounded' as const },
  { name: 'Volcano', dots: 'dots' as const, bg: '#7f1d1d', fg: '#fbbf24', bgfg: '#ef4444', gradient: 'radial' as const, corner: 'dot' as const },
  { name: 'Gold', dots: 'classy-rounded' as const, bg: '#fffbeb', fg: '#ca8a04', bgfg: '#eab308', gradient: 'linear' as const, corner: 'extra-rounded' as const },
  { name: 'Silver', dots: 'classy' as const, bg: '#f8fafc', fg: '#64748b', bgfg: '#94a3b8', gradient: 'linear' as const, corner: 'extra-rounded' as const },
  { name: 'Bronze', dots: 'rounded' as const, bg: '#fff7ed', fg: '#92400e', bgfg: '#c2410c', gradient: 'linear' as const, corner: 'extra-rounded' as const },
  { name: 'Platinum', dots: 'classy-rounded' as const, bg: '#fafafa', fg: '#525252', bgfg: '#737373', gradient: 'radial' as const, corner: 'extra-rounded' as const },
  { name: 'Fire', dots: 'dots' as const, bg: '#000000', fg: '#dc2626', bgfg: '#f97316', gradient: 'radial' as const, corner: 'dot' as const },
  { name: 'Ice', dots: 'extra-rounded' as const, bg: '#f0f9ff', fg: '#0ea5e9', bgfg: '#38bdf8', gradient: 'radial' as const, corner: 'extra-rounded' as const },
  { name: 'Thunder', dots: 'square' as const, bg: '#18181b', fg: '#fbbf24', bgfg: '#fef3c7', gradient: 'radial' as const, corner: 'square' as const },
  { name: 'Earth', dots: 'classy' as const, bg: '#fef3c7', fg: '#78350f', bgfg: '#92400e', gradient: 'linear' as const, corner: 'square' as const },
  { name: 'Royal', dots: 'classy-rounded' as const, bg: '#faf5ff', fg: '#7c3aed', bgfg: '#8b5cf6', gradient: 'linear' as const, corner: 'extra-rounded' as const },
  { name: 'Pink', dots: 'extra-rounded' as const, bg: '#fdf2f8', fg: '#ec4899', bgfg: '#f472b6', gradient: 'radial' as const, corner: 'extra-rounded' as const },
  { name: 'Candy', dots: 'dots' as const, bg: '#ffffff', fg: '#ec4899', bgfg: '#a855f7', gradient: 'radial' as const, corner: 'dot' as const },
  { name: 'Bubblegum', dots: 'rounded' as const, bg: '#fce7f3', fg: '#f472b6', bgfg: '#fbcfe8', gradient: 'linear' as const, corner: 'extra-rounded' as const },
  { name: 'Retro', dots: 'rounded' as const, bg: '#fef3c7', fg: '#dc2626', bgfg: '#dc2626', gradient: 'none' as const, corner: 'square' as const },
  { name: 'Vintage', dots: 'square' as const, bg: '#fef3c7', fg: '#78350f', bgfg: '#78350f', gradient: 'none' as const, corner: 'square' as const },
  { name: '80s', dots: 'square' as const, bg: '#fae8ff', fg: '#ec4899', bgfg: '#06b6d4', gradient: 'linear' as const, corner: 'square' as const },
  { name: '90s', dots: 'dots' as const, bg: '#dbeafe', fg: '#8b5cf6', bgfg: '#22c55e', gradient: 'radial' as const, corner: 'dot' as const },
  { name: 'Mono', dots: 'classy' as const, bg: '#f5f5f5', fg: '#404040', bgfg: '#404040', gradient: 'none' as const, corner: 'extra-rounded' as const },
  { name: 'Grayscale', dots: 'rounded' as const, bg: '#e5e5e5', fg: '#262626', bgfg: '#525252', gradient: 'linear' as const, corner: 'extra-rounded' as const },
  { name: 'BlackWhite', dots: 'square' as const, bg: '#000000', fg: '#ffffff', bgfg: '#ffffff', gradient: 'none' as const, corner: 'square' as const },
  { name: 'Matrix', dots: 'square' as const, bg: '#000000', fg: '#00ff00', bgfg: '#00ff00', gradient: 'none' as const, corner: 'square' as const },
  { name: 'Holographic', dots: 'extra-rounded' as const, bg: '#ffffff', fg: '#8b5cf6', bgfg: '#06b6d4', gradient: 'radial' as const, corner: 'dot' as const },
  { name: 'Neon Grid', dots: 'square' as const, bg: '#1e1b4b', fg: '#00ffff', bgfg: '#ff00ff', gradient: 'linear' as const, corner: 'square' as const },
  { name: 'Aurora', dots: 'classy-rounded' as const, bg: '#0f172a', fg: '#818cf8', bgfg: '#34d399', gradient: 'radial' as const, corner: 'extra-rounded' as const },
];

export default function QRCreate() {
  const navigate = useNavigate();
  const location = useLocation();
  const [codeType, setCodeType] = useState<'qr' | 'barcode'>('qr');
  const [selectedType, setSelectedType] = useState<QRType>(qrTypes[0]);
  const [barcodeType, setBarcodeType] = useState(barcodeTypes[0].id);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'warning' | 'info' } | null>(null);
  
  // Text/URL
  const [textData, setTextData] = useState('');
  
  // VCard
  const [vcardFirstName, setVcardFirstName] = useState('');
  const [vcardLastName, setVcardLastName] = useState('');
  const [vcardOrg, setVcardOrg] = useState('');
  const [vcardTitle, setVcardTitle] = useState('');
  const [vcardPhone, setVcardPhone] = useState('');
  const [vcardEmail, setVcardEmail] = useState('');
  const [vcardWebsite, setVcardWebsite] = useState('');
  const [vcardAddress, setVcardAddress] = useState('');
  
  // WiFi
  const [wifiSSID, setWifiSSID] = useState('');
  const [wifiPassword, setWifiPassword] = useState('');
  const [wifiSecurity, setWifiSecurity] = useState<'WPA' | 'WEP' | 'nopass'>('WPA');
  const [wifiHidden, setWifiHidden] = useState(false);
  
  // SMS
  const [smsNumber, setSmsNumber] = useState('');
  const [smsMessage, setSmsMessage] = useState('');
  
  // Location
  const [locationLat, setLocationLat] = useState('');
  const [locationLng, setLocationLng] = useState('');
  
  // QR Options
  const [qrSize, setQrSize] = useState(300);
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
  const [hideBackgroundDots, setHideBackgroundDots] = useState(true);
  const [logoSize, setLogoSize] = useState(0.4);
  const [logoMargin, setLogoMargin] = useState(5);

  // Barcode options
  const [barcodeColor, setBarcodeColor] = useState('#000000');
  const [barcodeBackground, setBarcodeBackground] = useState('#ffffff');
  const [barcodeHeight, setBarcodeHeight] = useState(100);
  const [barcodeWidth, setBarcodeWidth] = useState(2);

  const qrCodeRef = useRef<HTMLDivElement>(null);
  const barcodeRef = useRef<HTMLCanvasElement>(null);
  const qrCode = useRef<any>(null);
  const logoInputRef = useRef<HTMLInputElement>(null);

  // Auto scroll to preview
  const scrollToPreview = () => {
    setTimeout(() => {
      qrCodeRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 100);
  };

  // Initialize QR & Clear on type change
  // Düzenleme modu - location.state'ten veri al
  useEffect(() => {
    const state = location.state as any;
    if (state?.editContent) {
      console.log('✏️ Düzenleme modu:', state);
      
      // İçeriği doldur
      setTextData(state.editContent);
      
      // Tip belirle
      if (state.editType === 'barcode') {
        setCodeType('barcode');
      } else if (state.editType === 'url') {
        setSelectedType(qrTypes.find(t => t.id === 'url') || qrTypes[0]);
      } else if (state.editType === 'text') {
        setSelectedType(qrTypes.find(t => t.id === 'text') || qrTypes[0]);
      }
      
      // State'i temizle
      window.history.replaceState({}, '', window.location.pathname);
      
      setToast({ message: '✏️ Düzenleme modunda', type: 'info' });
      setTimeout(() => setToast(null), 2000);
    }
  }, [location]);

  useEffect(() => {
    if (codeType === 'qr') {
      if (!qrCode.current && qrCodeRef.current) {
        try {
          qrCode.current = new QRCodeStyling({
            width: qrSize,
            height: qrSize,
            type: 'svg',
            data: 'https://1stqr.app',
            margin: margin,
            qrOptions: { typeNumber: 0, mode: 'Byte', errorCorrectionLevel: errorCorrection },
            imageOptions: { hideBackgroundDots: hideBackgroundDots, imageSize: logoSize, margin: logoMargin },
            dotsOptions: { color: dotsColor, type: dotsType },
            backgroundOptions: { color: backgroundColor },
            cornersSquareOptions: { color: cornerSquareColor, type: cornerSquareType },
            cornersDotOptions: { color: cornerDotColor, type: cornerDotType }
          });

          qrCodeRef.current.innerHTML = '';
          qrCode.current.append(qrCodeRef.current);
          console.log('✅ QR Code initialized');
        } catch (error) {
          console.error('❌ QR Code initialization error:', error);
        }
      } else if (qrCode.current && qrCodeRef.current && !qrCodeRef.current.querySelector('svg')) {
        // Re-append if SVG is missing
        try {
          qrCodeRef.current.innerHTML = '';
          qrCode.current.append(qrCodeRef.current);
          console.log('✅ QR Code re-appended');
        } catch (error) {
          console.error('❌ QR Code re-append error:', error);
        }
      }
    } else if (codeType === 'barcode' && barcodeRef.current) {
      // Clear barcode when switching
      const ctx = barcodeRef.current.getContext('2d');
      if (ctx) {
        ctx.clearRect(0, 0, barcodeRef.current.width, barcodeRef.current.height);
      }
    }
  }, [codeType]);

  // Update QR
  useEffect(() => {
    if (qrCode.current && codeType === 'qr') {
      try {
        const dotsColorFinal = gradientType === 'none' ? dotsColor : undefined;
        const gradient = gradientType !== 'none' ? {
          type: gradientType,
          rotation: 0,
          colorStops: [{ offset: 0, color: gradientColor1 }, { offset: 1, color: gradientColor2 }]
        } : undefined;

        qrCode.current.update({
          width: qrSize,
          height: qrSize,
          margin: margin,
          qrOptions: { errorCorrectionLevel: errorCorrection },
          image: logoImage || undefined,
          imageOptions: { hideBackgroundDots: hideBackgroundDots, imageSize: logoSize, margin: logoMargin },
          dotsOptions: { color: dotsColorFinal, gradient: gradient, type: dotsType },
          backgroundOptions: { color: backgroundColor },
          cornersSquareOptions: { color: cornerSquareColor, type: cornerSquareType },
          cornersDotOptions: { color: cornerDotColor, type: cornerDotType }
        });
        
        // Ensure SVG is visible
        if (qrCodeRef.current && !qrCodeRef.current.querySelector('svg')) {
          qrCodeRef.current.innerHTML = '';
          qrCode.current.append(qrCodeRef.current);
        }
        
        // Auto scroll to preview
        scrollToPreview();
      } catch (error) {
        console.error('❌ QR Code update error:', error);
      }
    }
  }, [codeType, qrSize, dotsType, cornerSquareType, cornerDotType, dotsColor, backgroundColor, cornerSquareColor, cornerDotColor, gradientType, gradientColor1, gradientColor2, logoImage, errorCorrection, margin, hideBackgroundDots, logoSize, logoMargin]);

  // Barcode validation helper
  const isBarcodeValid = (data: string, format: string): boolean => {
    if (!data || !data.trim()) return false;
    
    const numericFormats = ['EAN13', 'EAN8', 'UPC', 'ITF14'];
    
    if (numericFormats.includes(format)) {
      // Only numbers allowed
      if (!/^\d+$/.test(data)) return false;
      
      // Check length requirements
      if (format === 'EAN13' && data.length !== 12 && data.length !== 13) return false;
      if (format === 'EAN8' && data.length !== 7 && data.length !== 8) return false;
      if (format === 'UPC' && data.length !== 11 && data.length !== 12) return false;
      if (format === 'ITF14' && data.length !== 13 && data.length !== 14) return false;
    }
    
    return true;
  };

  // Update Barcode - Real-time
  useEffect(() => {
    if (codeType === 'barcode' && barcodeRef.current && textData.trim()) {
      // Validate before generating
      if (!isBarcodeValid(textData, barcodeType)) {
        // Silent fail for real-time updates - no console output
        return;
      }
      
      try {
        const ctx = barcodeRef.current.getContext('2d');
        if (ctx) {
          ctx.clearRect(0, 0, barcodeRef.current.width, barcodeRef.current.height);
        }
        
        barcodeRef.current.width = 400;
        barcodeRef.current.height = barcodeHeight + 40;
        
        JsBarcode(barcodeRef.current, textData, {
          format: barcodeType,
          lineColor: barcodeColor,
          background: barcodeBackground,
          height: barcodeHeight,
          width: barcodeWidth,
          displayValue: true,
          fontSize: 16,
          margin: 10,
          textMargin: 5
        });
        
        // Auto scroll to preview
        scrollToPreview();
      } catch (error) {
        // Silent fail - no console output for better UX
      }
    }
  }, [codeType, barcodeType, barcodeColor, barcodeBackground, barcodeHeight, barcodeWidth, textData]);

  const generateQR = () => {
    let qrData = '';

    switch (selectedType.id) {
      case 'text':
      case 'url':
        if (!textData.trim()) {
          setToast({ message: 'İçerik boş olamaz', type: 'warning' });
          return;
        }
        qrData = textData;
        break;
        
      case 'email':
        if (!textData.trim()) {
          setToast({ message: 'E-posta adresi boş olamaz', type: 'warning' });
          return;
        }
        qrData = `mailto:${textData}`;
        break;
        
      case 'phone':
        if (!textData.trim()) {
          setToast({ message: 'Telefon numarası boş olamaz', type: 'warning' });
          return;
        }
        qrData = `tel:${textData}`;
        break;
        
      case 'sms':
        if (!smsNumber.trim()) {
          setToast({ message: 'SMS numarası boş olamaz', type: 'warning' });
          return;
        }
        qrData = `SMSTO:${smsNumber}:${smsMessage}`;
        break;
        
      case 'wifi':
        if (!wifiSSID.trim()) {
          setToast({ message: 'WiFi SSID boş olamaz', type: 'warning' });
          return;
        }
        qrData = `WIFI:T:${wifiSecurity};S:${wifiSSID};P:${wifiPassword};H:${wifiHidden ? 'true' : 'false'};;`;
        break;
        
      case 'vcard':
        if (!vcardFirstName.trim() && !vcardLastName.trim()) {
          setToast({ message: 'İsim boş olamaz', type: 'warning' });
          return;
        }
        qrData = `BEGIN:VCARD\nVERSION:3.0\nN:${vcardLastName};${vcardFirstName};;;\nFN:${vcardFirstName} ${vcardLastName}\n`;
        if (vcardOrg) qrData += `ORG:${vcardOrg}\n`;
        if (vcardTitle) qrData += `TITLE:${vcardTitle}\n`;
        if (vcardPhone) qrData += `TEL:${vcardPhone}\n`;
        if (vcardEmail) qrData += `EMAIL:${vcardEmail}\n`;
        if (vcardWebsite) qrData += `URL:${vcardWebsite}\n`;
        if (vcardAddress) qrData += `ADR:;;${vcardAddress};;;;\n`;
        qrData += `END:VCARD`;
        break;
        
      case 'location':
        if (!locationLat.trim() || !locationLng.trim()) {
          setToast({ message: 'Koordinatlar boş olamaz', type: 'warning' });
          return;
        }
        qrData = `geo:${locationLat},${locationLng}`;
        break;
    }

    if (qrCode.current) {
      qrCode.current.update({ data: qrData });
      setToast({ message: 'QR kod oluşturuldu', type: 'success' });
    }
  };

  const generateBarcode = () => {
    if (!textData.trim()) {
      setToast({ message: '⚠️ Barkod verisi boş olamaz', type: 'warning' });
      return;
    }

    // Validate barcode format
    if (!isBarcodeValid(textData, barcodeType)) {
      const numericFormats = ['EAN13', 'EAN8', 'UPC', 'ITF14'];
      
      if (numericFormats.includes(barcodeType)) {
        let message = `⚠️ ${barcodeType} formatı sadece rakam kabul eder.`;
        
        if (barcodeType === 'EAN13') message += ' (12-13 haneli)';
        else if (barcodeType === 'EAN8') message += ' (7-8 haneli)';
        else if (barcodeType === 'UPC') message += ' (11-12 haneli)';
        else if (barcodeType === 'ITF14') message += ' (13-14 haneli)';
        
        setToast({ message, type: 'warning' });
      } else {
        setToast({ message: `⚠️ Geçersiz barkod verisi`, type: 'warning' });
      }
      return;
    }

    if (barcodeRef.current) {
      try {
        // Clear canvas first
        const ctx = barcodeRef.current.getContext('2d');
        if (ctx) {
          ctx.clearRect(0, 0, barcodeRef.current.width, barcodeRef.current.height);
        }
        
        // Set canvas size dynamically
        barcodeRef.current.width = 400;
        barcodeRef.current.height = barcodeHeight + 40;
        
        JsBarcode(barcodeRef.current, textData, {
          format: barcodeType,
          lineColor: barcodeColor,
          background: barcodeBackground,
          height: barcodeHeight,
          width: barcodeWidth,
          displayValue: true,
          fontSize: 16,
          margin: 10,
          textMargin: 5
        });
        setToast({ message: '✅ Barkod oluşturuldu', type: 'success' });
      } catch (error: any) {
        setToast({ message: `❌ Hata: ${error.message}`, type: 'error' });
      }
    }
  };

  const applyBarcodePreset = (preset: typeof barcodePresets[0]) => {
    setBarcodeColor(preset.color);
    setBarcodeBackground(preset.bg);
    setBarcodeHeight(preset.height);
    setBarcodeWidth(preset.width);
    
    // Anlık olarak barkodu oluştur (sadece geçerli veri varsa)
    if (textData.trim() && barcodeRef.current && isBarcodeValid(textData, barcodeType)) {
      setTimeout(() => {
        try {
          const ctx = barcodeRef.current?.getContext('2d');
          if (ctx && barcodeRef.current) {
            ctx.clearRect(0, 0, barcodeRef.current.width, barcodeRef.current.height);
            barcodeRef.current.width = 400;
            barcodeRef.current.height = preset.height + 40;
            
            JsBarcode(barcodeRef.current, textData, {
              format: barcodeType,
              lineColor: preset.color,
              background: preset.bg,
              height: preset.height,
              width: preset.width,
              displayValue: true,
              fontSize: 16,
              margin: 10,
              textMargin: 5
            });
          }
        } catch (error) {
          // Silent fail
        }
      }, 50);
    }
    
    setToast({ message: `${preset.name} teması uygulandı`, type: 'success' });
  };

  const randomBarcodePreset = () => {
    const random = barcodePresets[Math.floor(Math.random() * barcodePresets.length)];
    applyBarcodePreset(random);
  };

  const downloadQR = async (format: 'png' | 'svg' | 'jpeg') => {
    try {
      // Mobil için Filesystem API kullan
      const isMobile = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);
      
      if (isMobile) {
        // Mobilde saveToDevice'i çağır
        await saveToDevice();
        return;
      }
      
      // Web için qr-code-styling download
      if (codeType === 'qr' && qrCode.current) {
        qrCode.current.download({ name: `qr-${selectedType.id}-${Date.now()}`, extension: format });
        setToast({ message: `${format.toUpperCase()} indirildi`, type: 'success' });
      }
    } catch (error: any) {
      setToast({ message: '❌ İndirme hatası: ' + error.message, type: 'error' });
    }
  };

  const downloadBarcode = async (format: 'png') => {
    try {
      const isMobile = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);
      
      if (isMobile) {
        // Mobilde saveToDevice'i çağır
        await saveToDevice();
        return;
      }
      
      // Web için canvas download
      if (barcodeRef.current) {
        const link = document.createElement('a');
        link.download = `barcode-${Date.now()}.${format}`;
        link.href = barcodeRef.current.toDataURL(`image/${format}`);
        link.click();
        setToast({ message: `${format.toUpperCase()} indirildi`, type: 'success' });
      }
    } catch (error: any) {
      setToast({ message: '❌ İndirme hatası: ' + error.message, type: 'error' });
    }
  };

  const shareCode = async () => {
    try {
      let dataUrl = '';
      
      if (codeType === 'qr' && qrCodeRef.current) {
        const svgElement = qrCodeRef.current.querySelector('svg');
        if (svgElement) {
          const svgData = new XMLSerializer().serializeToString(svgElement);
          dataUrl = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svgData)));
        }
      } else if (codeType === 'barcode' && barcodeRef.current) {
        dataUrl = barcodeRef.current.toDataURL();
      }

      if (!dataUrl) {
        setToast({ message: 'Önce kod oluşturun', type: 'warning' });
        return;
      }

      // Convert to base64
      const response = await fetch(dataUrl);
      const blob = await response.blob();
      const reader = new FileReader();
      
      reader.onloadend = async () => {
        const base64data = reader.result as string;
        
        // Save temporarily
        const fileName = `${codeType}-${Date.now()}.png`;
        await Filesystem.writeFile({
          path: fileName,
          data: base64data.split(',')[1],
          directory: Directory.Cache
        });

        const fileUri = await Filesystem.getUri({
          directory: Directory.Cache,
          path: fileName
        });

        await Share.share({
          title: `${codeType === 'qr' ? 'QR Kod' : 'Barkod'} Paylaş`,
          text: `1STQR ile oluşturuldu - ${textData.substring(0, 50)}`,
          url: fileUri.uri,
          dialogTitle: 'Paylaş'
        });
        
        setToast({ message: 'Paylaşıldı', type: 'success' });
      };
      
      reader.readAsDataURL(blob);
    } catch (error: any) {
      setToast({ message: 'Paylaşma hatası: ' + error.message, type: 'error' });
    }
  };

  const saveToDevice = async () => {
    try {
      // Request permissions first
      const permissions = await Filesystem.checkPermissions();
      if (permissions.publicStorage !== 'granted') {
        const request = await Filesystem.requestPermissions();
        if (request.publicStorage !== 'granted') {
          setToast({ message: '⚠️ Depolama izni gerekli', type: 'warning' });
          return;
        }
      }

      let dataUrl = '';
      let fileName = '';
      
      if (codeType === 'qr' && qrCodeRef.current) {
        const svgElement = qrCodeRef.current.querySelector('svg');
        if (svgElement) {
          const canvas = document.createElement('canvas');
          const ctx = canvas.getContext('2d');
          const svgData = new XMLSerializer().serializeToString(svgElement);
          const img = new Image();
          
          await new Promise<void>((resolve, reject) => {
            img.onload = () => {
              canvas.width = img.width || 300;
              canvas.height = img.height || 300;
              ctx?.drawImage(img, 0, 0);
              dataUrl = canvas.toDataURL('image/png');
              fileName = `1STQR_${Date.now()}.png`;
              resolve();
            };
            img.onerror = () => reject(new Error('SVG yükleme hatası'));
            img.src = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svgData)));
          });
        }
      } else if (codeType === 'barcode' && barcodeRef.current) {
        dataUrl = barcodeRef.current.toDataURL('image/png');
        fileName = `1STQR_Barcode_${Date.now()}.png`;
      }

      if (!dataUrl) {
        setToast({ message: '⚠️ Önce kod oluşturun', type: 'warning' });
        return;
      }

      const base64data = dataUrl.split(',')[1];
      
      // Save to app's Documents directory (accessible via Files app)
      const result = await Filesystem.writeFile({
        path: `1STQR/${fileName}`,
        data: base64data,
        directory: Directory.Documents,
        recursive: true
      });
      
      setToast({ message: `✅ Kaydedildi: ${fileName}`, type: 'success' });
      console.log('File saved:', result.uri);
    } catch (error: any) {
      console.error('Save error:', error);
      setToast({ message: `❌ Kayıt hatası: ${error.message}`, type: 'error' });
    }
  };

  const shareOnMap = async () => {
    try {
      // Get QR/Barcode image data
      let imageData = '';
      let codeContent = '';
      
      if (codeType === 'qr' && qrCodeRef.current) {
        const svgElement = qrCodeRef.current.querySelector('svg');
        if (svgElement) {
          const svgData = new XMLSerializer().serializeToString(svgElement);
          imageData = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svgData)));
          
          // Get content based on type
          switch (selectedType.id) {
            case 'text':
            case 'url':
            case 'email':
            case 'phone':
              codeContent = textData;
              break;
            case 'sms':
              codeContent = `SMS: ${smsNumber}`;
              break;
            case 'wifi':
              codeContent = `WiFi: ${wifiSSID}`;
              break;
            case 'vcard':
              codeContent = `${vcardFirstName} ${vcardLastName}`;
              break;
            case 'location':
              codeContent = `${locationLat},${locationLng}`;
              break;
          }
        }
      } else if (codeType === 'barcode' && barcodeRef.current) {
        imageData = barcodeRef.current.toDataURL('image/png');
        codeContent = textData;
      }
      
      if (!imageData || !codeContent) {
        setToast({ message: '⚠️ Önce bir kod oluşturun', type: 'warning' });
        return;
      }
      
      // Navigate to social map with QR data
      navigate('/sosyal-harita', { 
        state: { 
          qrImage: imageData, 
          qrContent: codeContent,
          qrType: codeType,
          from: 'qr-create'
        } 
      });
      setToast({ message: '🗺️ Sosyal haritaya yönlendiriliyorsunuz', type: 'info' });
    } catch (error: any) {
      setToast({ message: `Hata: ${error.message}`, type: 'error' });
    }
  };

  const addToProducts = () => {
    // Get the current code/data
    let codeData = '';
    
    if (codeType === 'barcode') {
      codeData = textData;
    } else if (codeType === 'qr') {
      // Get QR data based on selected type
      switch (selectedType.id) {
        case 'text':
        case 'url':
        case 'email':
        case 'phone':
          codeData = textData;
          break;
        case 'sms':
          codeData = smsNumber;
          break;
        case 'wifi':
          codeData = wifiSSID;
          break;
        case 'vcard':
          codeData = `${vcardFirstName} ${vcardLastName} - ${vcardPhone}`;
          break;
        case 'location':
          codeData = `${locationLat},${locationLng}`;
          break;
      }
    }
    
    if (!codeData.trim()) {
      setToast({ message: '⚠️ Önce bir kod oluşturun', type: 'warning' });
      return;
    }
    
    // Navigate with barcode data
    navigate(`/urun-yonetimi?addBarcode=${encodeURIComponent(codeData)}`);
    setToast({ message: '📦 Ürün ekleme ekranına yönlendiriliyorsunuz', type: 'info' });
  };

  const applyPreset = (preset: typeof presetTemplates[0]) => {
    setDotsType(preset.dots);
    setDotsColor(preset.fg);
    setBackgroundColor(preset.bg);
    setGradientType(preset.gradient);
    setCornerSquareType(preset.corner);
    setCornerSquareColor(preset.fg);
    setCornerDotColor(preset.fg);
    if (preset.gradient !== 'none') {
      setGradientColor1(preset.fg);
      setGradientColor2(preset.bgfg);
    }
    setToast({ message: `${preset.name} teması uygulandı`, type: 'success' });
  };

  const randomPreset = () => {
    const random = presetTemplates[Math.floor(Math.random() * presetTemplates.length)];
    applyPreset(random);
  };

  const handleLogoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => setLogoImage(event.target?.result as string);
      reader.readAsDataURL(file);
    }
  };

  const renderFormFields = () => {
    if (codeType === 'barcode') {
      return (
        <div className="space-y-2">
          <div className="relative">
            <select
              value={barcodeType}
              onChange={(e) => setBarcodeType(e.target.value)}
              className="w-full px-3 py-2 bg-white border border-purple-300 rounded-lg text-gray-900 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            >
              {barcodeTypes.map(type => (
                <option key={type.id} value={type.id}>{type.name} - {type.desc}</option>
              ))}
            </select>
            <div className="mt-1 text-xs text-purple-600 bg-purple-50 px-2 py-1 rounded">
              💡 {barcodeTypes.find(t => t.id === barcodeType)?.desc}
            </div>
          </div>
            <input
              type="text"
              value={textData}
              onChange={(e) => setTextData(e.target.value)}
              placeholder={
                ['EAN13', 'EAN8', 'UPC', 'ITF14'].includes(barcodeType) 
                  ? 'Sadece rakam girin'
                  : 'Barkod verisi (harf/rakam)'
              }
              className="w-full px-3 py-2 bg-white border border-purple-300 rounded-lg text-gray-900 placeholder-gray-500 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
        </div>
      );
    }

    switch (selectedType.id) {
      case 'text':
      case 'url':
      case 'email':
      case 'phone':
        return (
          <textarea
            value={textData}
            onChange={(e) => setTextData(e.target.value)}
            placeholder={`${selectedType.name} içeriğini girin...`}
            className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-purple-500"
            rows={3}
          />
        );
        
      case 'sms':
        return (
          <div className="space-y-2">
            <input
              type="tel"
              value={smsNumber}
              onChange={(e) => setSmsNumber(e.target.value)}
              placeholder="Telefon Numarası"
              className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            <textarea
              value={smsMessage}
              onChange={(e) => setSmsMessage(e.target.value)}
              placeholder="Mesaj (opsiyonel)"
              className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-purple-500"
              rows={2}
            />
          </div>
        );
        
      case 'wifi':
        return (
          <div className="space-y-2">
            <input
              type="text"
              value={wifiSSID}
              onChange={(e) => setWifiSSID(e.target.value)}
              placeholder="Ağ Adı (SSID)"
              className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            <input
              type="password"
              value={wifiPassword}
              onChange={(e) => setWifiPassword(e.target.value)}
              placeholder="Şifre"
              className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            <select
              value={wifiSecurity}
              onChange={(e) => setWifiSecurity(e.target.value as any)}
              className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            >
              <option value="WPA">WPA/WPA2</option>
              <option value="WEP">WEP</option>
              <option value="nopass">Şifresiz</option>
            </select>
            <label className="flex items-center gap-2 text-white text-sm">
              <input
                type="checkbox"
                checked={wifiHidden}
                onChange={(e) => setWifiHidden(e.target.checked)}
                className="rounded"
              />
              Gizli Ağ
            </label>
          </div>
        );
        
      case 'vcard':
        return (
          <div className="space-y-2">
            <div className="grid grid-cols-2 gap-2">
              <input
                type="text"
                value={vcardFirstName}
                onChange={(e) => setVcardFirstName(e.target.value)}
                placeholder="Ad"
                className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
              <input
                type="text"
                value={vcardLastName}
                onChange={(e) => setVcardLastName(e.target.value)}
                placeholder="Soyad"
                className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
            </div>
            <input
              type="text"
              value={vcardOrg}
              onChange={(e) => setVcardOrg(e.target.value)}
              placeholder="Şirket"
              className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            <input
              type="text"
              value={vcardTitle}
              onChange={(e) => setVcardTitle(e.target.value)}
              placeholder="Ünvan"
              className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            <input
              type="tel"
              value={vcardPhone}
              onChange={(e) => setVcardPhone(e.target.value)}
              placeholder="Telefon"
              className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            <input
              type="email"
              value={vcardEmail}
              onChange={(e) => setVcardEmail(e.target.value)}
              placeholder="E-posta"
              className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            <input
              type="url"
              value={vcardWebsite}
              onChange={(e) => setVcardWebsite(e.target.value)}
              placeholder="Website"
              className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            <textarea
              value={vcardAddress}
              onChange={(e) => setVcardAddress(e.target.value)}
              placeholder="Adres"
              className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-purple-500"
              rows={2}
            />
          </div>
        );
        
      case 'location':
        return (
          <div className="grid grid-cols-2 gap-2">
            <input
              type="number"
              step="any"
              value={locationLat}
              onChange={(e) => setLocationLat(e.target.value)}
              placeholder="Enlem (Latitude)"
              className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            <input
              type="number"
              step="any"
              value={locationLng}
              onChange={(e) => setLocationLng(e.target.value)}
              placeholder="Boylam (Longitude)"
              className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-white/40 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
          </div>
        );
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-purple-950 to-slate-950 pb-24">
      {/* Header */}
      <div className="bg-gradient-to-r from-purple-600/90 to-pink-600/90 backdrop-blur-xl border-b border-white/10 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-3 py-2.5 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Link to="/" className="p-1.5 hover:bg-white/10 rounded-lg transition-all">
              <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
            </Link>
            <div>
              <h1 className="text-sm font-bold text-white">QR & Barkod ULTIMATE</h1>
              <p className="text-[10px] text-white/80">{presetTemplates.length} QR + {barcodePresets.length} Barkod</p>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto p-3">
        {/* QR/Barcode Toggle */}
        <div className="bg-white/5 backdrop-blur-xl rounded-2xl p-3 border border-white/10 mb-3">
          <div className="grid grid-cols-2 gap-2">
            <button
              onClick={() => setCodeType('qr')}
              className={`p-2.5 rounded-lg font-semibold text-sm transition-all ${
                codeType === 'qr'
                  ? 'bg-gradient-to-r from-purple-600 to-pink-600 text-white'
                  : 'bg-white/5 text-white/60 hover:text-white'
              }`}
            >
              QR Kod
            </button>
            <button
              onClick={() => setCodeType('barcode')}
              className={`p-2.5 rounded-lg font-semibold text-sm transition-all ${
                codeType === 'barcode'
                  ? 'bg-gradient-to-r from-purple-600 to-pink-600 text-white'
                  : 'bg-white/5 text-white/60 hover:text-white'
              }`}
            >
              Barkod
            </button>
          </div>
        </div>

        {/* Type Selection (QR only) */}
        {codeType === 'qr' && (
          <div className="bg-white/5 backdrop-blur-xl rounded-2xl p-3 border border-white/10 mb-3">
            <h3 className="text-xs font-semibold text-white mb-2">QR Türü</h3>
            <div className="grid grid-cols-4 gap-2">
            {qrTypes.map((type) => (
              <button
                key={type.id}
                  onClick={() => setSelectedType(type)}
                  className={`p-2 rounded-lg border text-[10px] font-medium transition-all ${
                  selectedType.id === type.id
                      ? 'border-purple-500 bg-purple-500/20 text-white'
                      : 'border-white/10 text-white/60 hover:text-white'
                }`}
              >
                  {type.name}
              </button>
            ))}
          </div>
        </div>
        )}

        {/* Form Fields */}
        <div className="bg-white/5 backdrop-blur-xl rounded-2xl p-3 border border-white/10 mb-3">
          <h3 className="text-xs font-semibold text-white mb-2">İçerik</h3>
          {renderFormFields()}
          <button
            onClick={codeType === 'qr' ? generateQR : generateBarcode}
            className="w-full mt-3 py-2.5 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-lg font-semibold text-sm hover:shadow-lg transition-all"
          >
            {codeType === 'qr' ? 'QR' : 'Barkod'} Oluştur
          </button>
        </div>

        {/* Preview */}
        <div className="bg-white/5 backdrop-blur-xl rounded-2xl p-3 border border-white/10 mb-3">
          <h3 className="text-xs font-semibold text-white mb-2 text-center">Önizleme</h3>
          <div className="bg-white rounded-xl p-3 flex items-center justify-center overflow-hidden min-h-[200px]">
            {codeType === 'qr' ? (
              <div 
                ref={qrCodeRef} 
                className="w-full max-w-[280px]"
                style={{ display: 'flex', justifyContent: 'center', alignItems: 'center' }}
              ></div>
            ) : (
              <canvas 
                ref={barcodeRef}
                className="max-w-full h-auto"
                style={{ maxHeight: '200px' }}
              ></canvas>
            )}
          </div>
          
          {/* Action Buttons - Modern & Compact */}
          <div className="grid grid-cols-2 gap-2 mt-3">
            <button
              onClick={() => codeType === 'qr' ? downloadQR('png') : downloadBarcode('png')}
              className="py-2.5 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-xl font-semibold text-xs hover:shadow-lg hover:scale-105 transition-all flex items-center justify-center gap-1"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10" /></svg>
              PNG İndir
            </button>
            <button
              onClick={shareCode}
              className="py-2.5 bg-gradient-to-r from-blue-600 to-cyan-600 text-white rounded-xl font-semibold text-xs hover:shadow-lg hover:scale-105 transition-all flex items-center justify-center gap-1"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" /></svg>
              Paylaş
            </button>
          </div>
          
          <div className="grid grid-cols-3 gap-2 mt-2">
            <button
              onClick={saveToDevice}
              className="py-2.5 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-xl font-semibold text-xs hover:shadow-lg hover:scale-105 transition-all flex items-center justify-center gap-1"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" /></svg>
              Kaydet
            </button>
            <button
              onClick={addToProducts}
              className="py-2.5 bg-gradient-to-r from-amber-600 to-orange-600 text-white rounded-xl font-semibold text-xs hover:shadow-lg hover:scale-105 transition-all flex items-center justify-center gap-1"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
              Ürün Ekle
            </button>
            <button
              onClick={shareOnMap}
              className="py-2.5 bg-gradient-to-r from-red-600 to-rose-600 text-white rounded-xl font-semibold text-xs hover:shadow-lg hover:scale-105 transition-all flex items-center justify-center gap-1"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
              Haritada
            </button>
              </div>
            </div>

        {/* Barcode Presets */}
        {codeType === 'barcode' && (
          <details open className="bg-white/5 backdrop-blur-xl rounded-2xl border border-white/10 mb-3 overflow-hidden">
            <summary className="cursor-pointer p-3 text-white font-semibold text-xs hover:bg-white/5 transition-all flex items-center justify-between">
              <span>🎨 Hazır Barkod Temaları ({barcodePresets.length})</span>
              <button
                onClick={(e) => { e.preventDefault(); randomBarcodePreset(); }}
                className="px-3 py-1 bg-purple-600 hover:bg-purple-700 rounded-lg text-[10px] font-medium transition-all"
              >
                🎲 Rastgele
              </button>
            </summary>
            <div className="px-3 pb-3">
              <div className="grid grid-cols-3 gap-2">
                {barcodePresets.map((preset) => (
                  <button
                    key={preset.name}
                    onClick={() => applyBarcodePreset(preset)}
                    className="p-2 rounded-lg border border-white/10 hover:border-white/30 bg-white/5 hover:bg-white/10 transition-all text-white text-[10px] font-medium"
                  >
                    {preset.name}
                  </button>
                ))}
              </div>
            </div>
          </details>
        )}

        {/* Barcode Styles */}
        {codeType === 'barcode' && (
          <details className="bg-white/5 backdrop-blur-xl rounded-2xl border border-white/10 mb-3 overflow-hidden">
            <summary className="cursor-pointer p-3 text-white font-semibold text-xs hover:bg-white/5 transition-all">
              ⚙️ Barkod Ayarları
            </summary>
            <div className="px-3 pb-3 space-y-2">
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-[10px] font-medium text-white/80 mb-1">Renk</label>
                  <input
                    type="color"
                    value={barcodeColor}
                    onChange={(e) => setBarcodeColor(e.target.value)}
                    className="w-full h-8 rounded-lg cursor-pointer"
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-medium text-white/80 mb-1">Arka Plan</label>
                  <input
                    type="color"
                    value={barcodeBackground}
                    onChange={(e) => setBarcodeBackground(e.target.value)}
                    className="w-full h-8 rounded-lg cursor-pointer"
                  />
                </div>
              </div>
              <div>
                <label className="block text-[10px] font-medium text-white/80 mb-1">Yükseklik: {barcodeHeight}px</label>
                <input
                  type="range"
                  min="50"
                  max="200"
                  value={barcodeHeight}
                  onChange={(e) => setBarcodeHeight(Number(e.target.value))}
                  className="w-full accent-purple-600"
                />
              </div>
              <div>
                <label className="block text-[10px] font-medium text-white/80 mb-1">Genişlik: {barcodeWidth}px</label>
                <input
                  type="range"
                  min="1"
                  max="4"
                  value={barcodeWidth}
                  onChange={(e) => setBarcodeWidth(Number(e.target.value))}
                  className="w-full accent-purple-600"
                />
              </div>
            </div>
          </details>
        )}

        {/* QR Presets & Styles */}
        {codeType === 'qr' && (
          <>
            <details className="bg-white/5 backdrop-blur-xl rounded-2xl border border-white/10 mb-3 overflow-hidden">
              <summary className="cursor-pointer p-3 text-white font-semibold text-xs hover:bg-white/5 transition-all flex items-center justify-between">
                <span>🎨 Hazır Temalar ({presetTemplates.length})</span>
                <button
                  onClick={(e) => { e.preventDefault(); randomPreset(); }}
                  className="px-3 py-1 bg-purple-600 hover:bg-purple-700 rounded-lg text-[10px] font-medium transition-all"
                >
                  Rastgele
                </button>
              </summary>
              <div className="px-3 pb-3">
                <div className="grid grid-cols-3 gap-2 max-h-[300px] overflow-y-auto">
                  {presetTemplates.map((preset) => (
                    <button
                      key={preset.name}
                      onClick={() => applyPreset(preset)}
                      className="p-2 rounded-lg border border-white/10 hover:border-white/30 bg-white/5 hover:bg-white/10 transition-all text-white text-[10px] font-medium"
                    >
                      {preset.name}
                    </button>
                  ))}
                </div>
              </div>
            </details>

            <details className="bg-white/5 backdrop-blur-xl rounded-2xl border border-white/10 mb-3 overflow-hidden">
              <summary className="cursor-pointer p-3 text-white font-semibold text-xs hover:bg-white/5 transition-all">
                🎯 Stil & Logo
              </summary>
              <div className="px-3 pb-3 space-y-2">
                <div>
                  <label className="block text-[10px] font-medium text-white/80 mb-1">Nokta Şekli</label>
                  <div className="grid grid-cols-3 gap-1">
                    {(['square', 'rounded', 'dots', 'classy', 'classy-rounded', 'extra-rounded'] as const).map((type) => (
                      <button
                        key={type}
                        onClick={() => setDotsType(type)}
                        className={`p-1.5 rounded-lg border text-[9px] ${
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
                    id="logo-upload"
                  />
                  <label
                    htmlFor="logo-upload"
                    className="block w-full p-2 bg-white/5 border border-white/10 rounded-lg text-white text-[10px] font-medium text-center cursor-pointer hover:bg-white/10 transition-all"
                  >
                    {logoImage ? 'Logo Değiştir' : 'Logo Ekle'}
                  </label>
                </div>

                {logoImage && (
                  <>
                    <div>
                      <label className="block text-[10px] font-medium text-white/80 mb-1">Logo Boyutu: {logoSize.toFixed(2)}</label>
                      <input
                        type="range"
                        min="0.1"
                        max="0.8"
                        step="0.05"
                        value={logoSize}
                        onChange={(e) => setLogoSize(Number(e.target.value))}
                        className="w-full accent-purple-600"
                      />
                    </div>
                    <label className="flex items-center gap-2 text-white text-[10px]">
                      <input
                        type="checkbox"
                        checked={hideBackgroundDots}
                        onChange={(e) => setHideBackgroundDots(e.target.checked)}
                        className="rounded"
                      />
                      Logo arkasını gizle
                    </label>
                  </>
                )}
              </div>
            </details>

            <details className="bg-white/5 backdrop-blur-xl rounded-2xl border border-white/10 mb-3 overflow-hidden">
              <summary className="cursor-pointer p-3 text-white font-semibold text-xs hover:bg-white/5 transition-all">
                🌈 Renkler
              </summary>
              <div className="px-3 pb-3 space-y-2">
                <div>
                  <label className="block text-[10px] font-medium text-white/80 mb-1">Gradient</label>
                  <div className="grid grid-cols-3 gap-1">
                    {(['none', 'linear', 'radial'] as const).map((type) => (
                      <button
                        key={type}
                        onClick={() => setGradientType(type)}
                        className={`p-1.5 rounded-lg border text-[9px] ${
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

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-[10px] font-medium text-white/80 mb-1">
                      {gradientType === 'none' ? 'Nokta' : 'Renk 1'}
                    </label>
                    <input
                      type="color"
                      value={gradientType === 'none' ? dotsColor : gradientColor1}
                      onChange={(e) => gradientType === 'none' ? setDotsColor(e.target.value) : setGradientColor1(e.target.value)}
                      className="w-full h-8 rounded-lg cursor-pointer"
                    />
                  </div>
                  {gradientType !== 'none' && (
                    <div>
                      <label className="block text-[10px] font-medium text-white/80 mb-1">Renk 2</label>
                      <input
                        type="color"
                        value={gradientColor2}
                        onChange={(e) => setGradientColor2(e.target.value)}
                        className="w-full h-8 rounded-lg cursor-pointer"
                      />
                    </div>
                  )}
                  <div>
                    <label className="block text-[10px] font-medium text-white/80 mb-1">Arka Plan</label>
                    <input
                      type="color"
                      value={backgroundColor}
                      onChange={(e) => setBackgroundColor(e.target.value)}
                      className="w-full h-8 rounded-lg cursor-pointer"
                    />
                  </div>
                </div>
              </div>
            </details>

            <details className="bg-white/5 backdrop-blur-xl rounded-2xl border border-white/10 overflow-hidden">
              <summary className="cursor-pointer p-3 text-white font-semibold text-xs hover:bg-white/5 transition-all">
                ⚙️ Gelişmiş
              </summary>
              <div className="px-3 pb-3 space-y-2">
                <div>
                  <label className="block text-[10px] font-medium text-white/80 mb-1">Boyut: {qrSize}px</label>
                  <input
                    type="range"
                    min="200"
                    max="600"
                    step="50"
                    value={qrSize}
                    onChange={(e) => setQrSize(Number(e.target.value))}
                    className="w-full accent-purple-600"
                  />
                </div>

                <div>
                  <label className="block text-[10px] font-medium text-white/80 mb-1">Kenar: {margin}px</label>
                  <input
                    type="range"
                    min="0"
                    max="30"
                    value={margin}
                    onChange={(e) => setMargin(Number(e.target.value))}
                    className="w-full accent-purple-600"
                  />
                </div>

                <div>
                  <label className="block text-[10px] font-medium text-white/80 mb-1">Hata Düzeltme</label>
                  <div className="grid grid-cols-4 gap-1">
                    {(['L', 'M', 'Q', 'H'] as const).map((level) => (
                      <button
                        key={level}
                        onClick={() => setErrorCorrection(level)}
                        className={`p-1.5 rounded-lg border text-[9px] ${
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
          </div>
            </details>
          </>
        )}
      </div>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
      <Navigation />
    </div>
  );
}
