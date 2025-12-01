import QRCode from 'qrcode';
import { QRType, QRCustomization, WiFiData, VCardData, EventData } from '../types';

/**
 * Generate QR code string based on type and data
 */
export const generateQRString = (type: QRType, data: any): string => {
  switch (type) {
    case 'text':
      return data.text || '';

    case 'url':
      return data.url || '';

    case 'wifi':
      const wifi = data as WiFiData;
      return `WIFI:T:${wifi.security || 'WPA'};S:${wifi.ssid || ''};P:${wifi.password || ''};;`;

    case 'email':
      return `mailto:${data.email || ''}?subject=${encodeURIComponent(data.subject || '')}&body=${encodeURIComponent(data.body || '')}`;

    case 'sms':
      return `SMSTO:${data.phone || ''}:${data.message || ''}`;

    case 'phone':
      return `tel:${data.phone || ''}`;

    case 'location':
      return `geo:${data.lat || '0'},${data.lng || '0'}`;

    case 'vcard':
      const vcard = data as VCardData;
      return `BEGIN:VCARD
VERSION:3.0
FN:${vcard.name || ''}
ORG:${vcard.company || ''}
TEL:${vcard.phone || ''}
EMAIL:${vcard.email || ''}
ADR:${vcard.address || ''}
URL:${vcard.website || ''}
END:VCARD`;

    case 'event':
      const event = data as EventData;
      return `BEGIN:VEVENT
SUMMARY:${event.title || ''}
DESCRIPTION:${event.description || ''}
LOCATION:${event.location || ''}
DTSTART:${event.start ? new Date(event.start).toISOString().replace(/[-:]/g, '').split('.')[0] + 'Z' : ''}
DTEND:${event.end ? new Date(event.end).toISOString().replace(/[-:]/g, '').split('.')[0] + 'Z' : ''}
END:VEVENT`;

    default:
      return String(data);
  }
};

/**
 * Generate QR code image
 */
export const generateQRImage = async (
  qrString: string,
  customization: QRCustomization
): Promise<string> => {
  try {
    const canvas = document.createElement('canvas');
    
    await QRCode.toCanvas(canvas, qrString, {
      width: customization.size,
      margin: customization.border,
      color: {
        dark: customization.color,
        light: customization.bgColor,
      },
      errorCorrectionLevel: customization.errorLevel,
    });

    // Apply gradient if enabled
    if (customization.gradient) {
      const ctx = canvas.getContext('2d');
      if (ctx) {
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const gradient = ctx.createLinearGradient(
          0,
          0,
          Math.cos((customization.gradientAngle * Math.PI) / 180) * canvas.width,
          Math.sin((customization.gradientAngle * Math.PI) / 180) * canvas.height
        );
        gradient.addColorStop(0, customization.gradientColor1);
        gradient.addColorStop(1, customization.gradientColor2);
        
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.globalCompositeOperation = 'destination-in';
        ctx.putImageData(imageData, 0, 0);
      }
    }

    return canvas.toDataURL('image/png');
  } catch (error) {
    console.error('QR generation error:', error);
    throw new Error('Failed to generate QR code');
  }
};

/**
 * Download QR code image
 */
export const downloadQRImage = (imageData: string, filename?: string): void => {
  const link = document.createElement('a');
  link.download = filename || `qrcode-${Date.now()}.png`;
  link.href = imageData;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

/**
 * Share QR code
 */
export const shareQRImage = async (imageData: string, title?: string): Promise<boolean> => {
  if (!navigator.share) {
    console.warn('Web Share API not supported');
    return false;
  }

  try {
    const blob = await (await fetch(imageData)).blob();
    const file = new File([blob], 'qrcode.png', { type: 'image/png' });
    
    await navigator.share({
      files: [file],
      title: title || 'QR Kod',
      text: 'QRMaster ile oluşturuldu',
    });
    
    return true;
  } catch (error) {
    console.error('Share error:', error);
    return false;
  }
};

/**
 * Detect QR code type from string
 */
export const detectQRType = (data: string): QRType => {
  if (data.startsWith('http://') || data.startsWith('https://')) return 'url';
  if (data.startsWith('WIFI:')) return 'wifi';
  if (data.startsWith('mailto:')) return 'email';
  if (data.startsWith('SMSTO:')) return 'sms';
  if (data.startsWith('tel:')) return 'phone';
  if (data.startsWith('geo:')) return 'location';
  if (data.startsWith('BEGIN:VCARD')) return 'vcard';
  if (data.startsWith('BEGIN:VEVENT')) return 'event';
  return 'text';
};

/**
 * Validate QR data
 */
export const validateQRData = (type: QRType, data: any): boolean => {
  switch (type) {
    case 'text':
      return Boolean(data.text && data.text.trim());
    
    case 'url':
      try {
        new URL(data.url);
        return true;
      } catch {
        return false;
      }
    
    case 'wifi':
      return Boolean(data.ssid && data.ssid.trim());
    
    case 'email':
      return Boolean(data.email && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.email));
    
    case 'sms':
    case 'phone':
      return Boolean(data.phone && data.phone.trim());
    
    case 'location':
      return Boolean(
        data.lat &&
        data.lng &&
        !isNaN(parseFloat(data.lat)) &&
        !isNaN(parseFloat(data.lng))
      );
    
    case 'vcard':
      return Boolean(data.name && data.name.trim());
    
    case 'event':
      return Boolean(data.title && data.title.trim());
    
    default:
      return true;
  }
};











































