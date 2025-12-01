const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const compression = require('compression');
const path = require('path');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;

// ============================================
// 🔒 SECURITY & MIDDLEWARE
// ============================================
app.use(helmet());
app.use(compression());
app.use(cors({
  origin: ['http://localhost:3000', 'http://localhost:5173'],
  credentials: true
}));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Static files (for QR code images, uploads)
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// ============================================
// 🗄️ DATABASE CONNECTION
// ============================================
const db = require('./config/database');

// SQLite veritabanı zaten config/database.js'de yükleniyor
// Bağlantı kontrolü gerekmiyor (senkron çalışıyor)

// ============================================
// 📋 API ROUTES  
// ============================================
app.use('/api/products', require('./routes/products'));
app.use('/api/qr-codes', require('./routes/qrcodes'));
app.use('/api/sales', require('./routes/sales'));
app.use('/api/scan-history', require('./routes/scanHistory'));
app.use('/api/stats', require('./routes/stats'));

// ============================================
// 🏥 HEALTH CHECK
// ============================================
app.get('/api/health', (req, res) => {
  res.json({
    status: 'OK',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    database: 'Connected'
  });
});

// ============================================
// 🚫 404 HANDLER
// ============================================
app.use('*', (req, res) => {
  res.status(404).json({
    success: false,
    message: 'API endpoint bulunamadı',
    path: req.originalUrl
  });
});

// ============================================
// 🚨 ERROR HANDLER
// ============================================
app.use((err, req, res, next) => {
  console.error('❌ Server Error:', err);
  res.status(500).json({
    success: false,
    message: 'Sunucu hatası',
    error: process.env.NODE_ENV === 'development' ? err.message : undefined
  });
});

// ============================================
// 🚀 START SERVER
// ============================================
app.listen(PORT, process.env.HOST || '0.0.0.0', () => {
  const host = process.env.HOST || '0.0.0.0';
  console.log(`
🎯 QRMaster Backend API Server
🚀 Server running on: http://${host === '0.0.0.0' ? 'localhost' : host}:${PORT}
📱 Mobil erişim: Aynı WiFi ağındaki cihazlardan erişilebilir
🗄️ Database: SQLite
🌍 Environment: ${process.env.NODE_ENV || 'production'}
📊 API Health: http://localhost:${PORT}/api/health
  `);
});

module.exports = app;
