/**
 * Modern Color Palette - Ultra Premium Design System
 */

export const MODERN_COLORS = {
  // Primary Gradients
  primary: {
    from: '#6366f1',
    to: '#8b5cf6',
    gradient: 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)',
  },
  
  // Secondary Gradients
  secondary: {
    from: '#ec4899',
    to: '#f43f5e',
    gradient: 'linear-gradient(135deg, #ec4899 0%, #f43f5e 100%)',
  },
  
  // Success
  success: {
    from: '#10b981',
    to: '#059669',
    gradient: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
  },
  
  // Warning
  warning: {
    from: '#f59e0b',
    to: '#f97316',
    gradient: 'linear-gradient(135deg, #f59e0b 0%, #f97316 100%)',
  },
  
  // Danger
  danger: {
    from: '#ef4444',
    to: '#dc2626',
    gradient: 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)',
  },
  
  // Info
  info: {
    from: '#3b82f6',
    to: '#2563eb',
    gradient: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)',
  },
} as const;

export const GRADIENT_THEMES = [
  { 
    name: 'Sunset', 
    gradient: 'linear-gradient(135deg, #ff6b6b 0%, #feca57 100%)',
    colors: ['#ff6b6b', '#feca57']
  },
  { 
    name: 'Ocean', 
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    colors: ['#667eea', '#764ba2']
  },
  { 
    name: 'Forest', 
    gradient: 'linear-gradient(135deg, #56ab2f 0%, #a8e063 100%)',
    colors: ['#56ab2f', '#a8e063']
  },
  { 
    name: 'Fire', 
    gradient: 'linear-gradient(135deg, #f83600 0%, #f9d423 100%)',
    colors: ['#f83600', '#f9d423']
  },
  { 
    name: 'Purple Dream', 
    gradient: 'linear-gradient(135deg, #c471f5 0%, #fa71cd 100%)',
    colors: ['#c471f5', '#fa71cd']
  },
  { 
    name: 'Mint', 
    gradient: 'linear-gradient(135deg, #00b4db 0%, #0083b0 100%)',
    colors: ['#00b4db', '#0083b0']
  },
  { 
    name: 'Rose', 
    gradient: 'linear-gradient(135deg, #ed213a 0%, #93291e 100%)',
    colors: ['#ed213a', '#93291e']
  },
  { 
    name: 'Galaxy', 
    gradient: 'linear-gradient(135deg, #2c3e50 0%, #3498db 100%)',
    colors: ['#2c3e50', '#3498db']
  },
  { 
    name: 'Peach', 
    gradient: 'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
    colors: ['#ffecd2', '#fcb69f']
  },
  { 
    name: 'Northern Lights', 
    gradient: 'linear-gradient(135deg, #00c6ff 0%, #0072ff 100%)',
    colors: ['#00c6ff', '#0072ff']
  },
] as const;

// ============================================
// 🎯 PROFESYONEL QR KOD SİSTEMİ (genqrcode.com standardı)
// ============================================

// DATA MODULE ŞEKİLLERİ (QR'ın veri bölümü)
export const QR_DATA_SHAPES = [
  { id: 'square', name: 'Kare', icon: '■', desc: 'Klasik' },
  { id: 'circle', name: 'Daire', icon: '●', desc: 'Modern' },
  { id: 'rounded-square', name: 'Yumuşak Kare', icon: '▢', desc: 'Şık' },
  { id: 'diamond', name: 'Elmas', icon: '♦', desc: 'Özgün' },
  { id: 'dots', name: 'Nokta', icon: '⚫', desc: 'Minimal' },
] as const;

// FINDER PATTERN (GÖZ) ŞEKİLLERİ
export const QR_EYE_SHAPES = {
  // Dış çerçeve şekilleri
  outer: [
    { id: 'square', name: 'Kare', icon: '⬛', desc: 'Klasik' },
    { id: 'circle', name: 'Daire', icon: '⚫', desc: 'Modern' },
    { id: 'rounded', name: 'Yuvarlatılmış', icon: '🔲', desc: 'Yumuşak' },
    { id: 'leaf', name: 'Yaprak', icon: '🍃', desc: 'Organik' },
    { id: 'star', name: 'Yıldız', icon: '⭐', desc: 'Çarpıcı' },
  ],
  // İç nokta şekilleri  
  inner: [
    { id: 'square', name: 'Kare', icon: '⬛', desc: 'Klasik' },
    { id: 'circle', name: 'Daire', icon: '⚫', desc: 'Modern' },
    { id: 'rounded', name: 'Yuvarlatılmış', icon: '🔘', desc: 'Yumuşak' },
    { id: 'diamond', name: 'Elmas', icon: '♦️', desc: 'Özgün' },
    { id: 'cross', name: 'Artı', icon: '➕', desc: 'Benzersiz' },
  ],
} as const;

// ÇERÇEVE TİPLERİ
export const QR_FRAME_STYLES = [
  { id: 'none', name: 'Çerçevesiz', preview: '' },
  { 
    id: 'scan-me', 
    name: 'Scan Me', 
    preview: 'SCAN ME',
    colors: { bg: '#000000', text: '#FFFFFF' }
  },
  { 
    id: 'qr-code', 
    name: 'QR Code', 
    preview: 'QR CODE',
    colors: { bg: '#4F46E5', text: '#FFFFFF' }
  },
  { 
    id: 'tap-to-view', 
    name: 'Tap to View', 
    preview: 'TAP TO VIEW',
    colors: { bg: '#059669', text: '#FFFFFF' }
  },
] as const;

// LOGO TEMPLATES
export const QR_LOGO_TEMPLATES = [
  { id: 'none', name: 'Logo Yok', icon: '🚫' },
  { id: 'wifi', name: 'WiFi', icon: '📶', svg: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M1 9l2 2c4.97-4.97 13.03-4.97 18 0l2-2C16.93 2.93 7.08 2.93 1 9zm8 8l3 3 3-3c-1.65-1.66-4.34-1.66-6 0zm-4-4l2 2c2.76-2.76 7.24-2.76 10 0l2-2C15.14 9.14 8.87 9.14 5 13z"/></svg>' },
  { id: 'website', name: 'Website', icon: '🌐', svg: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/></svg>' },
  { id: 'email', name: 'Email', icon: '✉️', svg: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>' },
  { id: 'phone', name: 'Telefon', icon: '📞', svg: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/></svg>' },
] as const;

// ESKI EXPORT - GERİYE UYUMLULUK
export const QR_PATTERNS = QR_DATA_SHAPES;

export const BACKGROUND_PATTERNS = [
  { id: 'none', name: 'Yok' },
  { id: 'dots', name: 'Noktalar' },
  { id: 'grid', name: 'Izgara' },
  { id: 'diagonal', name: 'Çapraz' },
  { id: 'waves', name: 'Dalgalar' },
] as const;

