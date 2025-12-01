const express = require('express');
const { body, validationResult } = require('express-validator');
const db = require('../config/database');
const router = express.Router();

// ============================================
// 📱 QR CODES API ENDPOINTS
// ============================================

// GET /api/qr-codes - Tüm QR kodları listele
router.get('/', async (req, res) => {
  try {
    const { type, search, page = 1, limit = 20 } = req.query;
    let sql = 'SELECT * FROM qr_codes WHERE is_active = TRUE';
    const params = [];
    
    // Type filter
    if (type && type !== 'all') {
      sql += ' AND type = ?';
      params.push(type);
    }
    
    // Search filter
    if (search) {
      sql += ' AND (title LIKE ? OR content LIKE ? OR description LIKE ?)';
      params.push(`%${search}%`, `%${search}%`, `%${search}%`);
    }
    
    // Pagination
    const offset = (page - 1) * limit;
    sql += ' ORDER BY created_at DESC LIMIT ? OFFSET ?';
    params.push(parseInt(limit), parseInt(offset));
    
    const qrCodes = await db.query(sql, params);
    
    // Parse customization JSON
    const processedQrCodes = qrCodes.map(qr => ({
      ...qr,
      customization: qr.customization ? JSON.parse(qr.customization) : null
    }));
    
    // Total count
    let countSql = 'SELECT COUNT(*) as total FROM qr_codes WHERE is_active = TRUE';
    const countParams = [];
    
    if (type && type !== 'all') {
      countSql += ' AND type = ?';
      countParams.push(type);
    }
    
    if (search) {
      countSql += ' AND (title LIKE ? OR content LIKE ? OR description LIKE ?)';
      countParams.push(`%${search}%`, `%${search}%`, `%${search}%`);
    }
    
    const [{ total }] = await db.query(countSql, countParams);
    
    res.json({
      success: true,
      data: processedQrCodes,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        total,
        pages: Math.ceil(total / limit)
      }
    });
  } catch (error) {
    console.error('❌ QR Codes fetch error:', error);
    res.status(500).json({
      success: false,
      message: 'QR kodları yüklenirken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/qr-codes/:id - Tek QR kod detayı
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const qrCode = await db.queryOne('SELECT * FROM qr_codes WHERE id = ? AND is_active = TRUE', [id]);
    
    if (!qrCode) {
      return res.status(404).json({
        success: false,
        message: 'QR kod bulunamadı'
      });
    }
    
    // Parse customization JSON
    qrCode.customization = qrCode.customization ? JSON.parse(qrCode.customization) : null;
    
    res.json({
      success: true,
      data: qrCode
    });
  } catch (error) {
    console.error('❌ QR Code fetch error:', error);
    res.status(500).json({
      success: false,
      message: 'QR kod yüklenirken hata oluştu',
      error: error.message
    });
  }
});

// POST /api/qr-codes - Yeni QR kod oluştur
router.post('/', [
  body('type').trim().isLength({ min: 1 }).withMessage('QR kod tipi gerekli'),
  body('content').trim().isLength({ min: 1 }).withMessage('QR kod içeriği gerekli')
], async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({
        success: false,
        message: 'Validasyon hatası',
        errors: errors.array()
      });
    }
    
    const { type, content, qr_image_url, customization, title, description } = req.body;
    
    // Convert customization object to JSON string
    const customizationJson = customization ? JSON.stringify(customization) : null;
    
    const result = await db.query(`
      INSERT INTO qr_codes (type, content, qr_image_url, customization, title, description)
      VALUES (?, ?, ?, ?, ?, ?)
    `, [type, content, qr_image_url, customizationJson, title, description]);
    
    const newQrCode = await db.queryOne('SELECT * FROM qr_codes WHERE id = ?', [result.insertId]);
    
    // Parse customization back
    if (newQrCode.customization) {
      newQrCode.customization = JSON.parse(newQrCode.customization);
    }
    
    res.status(201).json({
      success: true,
      message: 'QR kod başarıyla oluşturuldu',
      data: newQrCode
    });
  } catch (error) {
    console.error('❌ QR Code create error:', error);
    res.status(500).json({
      success: false,
      message: 'QR kod oluştururken hata oluştu',
      error: error.message
    });
  }
});

// PUT /api/qr-codes/:id - QR kod güncelle
router.put('/:id', [
  body('type').trim().isLength({ min: 1 }).withMessage('QR kod tipi gerekli'),
  body('content').trim().isLength({ min: 1 }).withMessage('QR kod içeriği gerekli')
], async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({
        success: false,
        message: 'Validasyon hatası',
        errors: errors.array()
      });
    }
    
    const { id } = req.params;
    const { type, content, qr_image_url, customization, title, description } = req.body;
    
    const existingQrCode = await db.queryOne('SELECT * FROM qr_codes WHERE id = ? AND is_active = TRUE', [id]);
    if (!existingQrCode) {
      return res.status(404).json({
        success: false,
        message: 'QR kod bulunamadı'
      });
    }
    
    const customizationJson = customization ? JSON.stringify(customization) : null;
    
    await db.query(`
      UPDATE qr_codes 
      SET type = ?, content = ?, qr_image_url = ?, customization = ?, title = ?, description = ?,
          updated_at = datetime('now')
      WHERE id = ?
    `, [type, content, qr_image_url, customizationJson, title, description, id]);
    
    const updatedQrCode = await db.queryOne('SELECT * FROM qr_codes WHERE id = ?', [id]);
    
    // Parse customization back
    if (updatedQrCode.customization) {
      updatedQrCode.customization = JSON.parse(updatedQrCode.customization);
    }
    
    res.json({
      success: true,
      message: 'QR kod başarıyla güncellendi',
      data: updatedQrCode
    });
  } catch (error) {
    console.error('❌ QR Code update error:', error);
    res.status(500).json({
      success: false,
      message: 'QR kod güncellenirken hata oluştu',
      error: error.message
    });
  }
});

// DELETE /api/qr-codes/:id - QR kod sil (soft delete)
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    
    const existingQrCode = await db.queryOne('SELECT * FROM qr_codes WHERE id = ? AND is_active = TRUE', [id]);
    if (!existingQrCode) {
      return res.status(404).json({
        success: false,
        message: 'QR kod bulunamadı'
      });
    }
    
    await db.query('UPDATE qr_codes SET is_active = FALSE WHERE id = ?', [id]);
    
    res.json({
      success: true,
      message: 'QR kod başarıyla silindi'
    });
  } catch (error) {
    console.error('❌ QR Code delete error:', error);
    res.status(500).json({
      success: false,
      message: 'QR kod silinirken hata oluştu',
      error: error.message
    });
  }
});

// POST /api/qr-codes/:id/download - Download count artır
router.post('/:id/download', async (req, res) => {
  try {
    const { id } = req.params;
    
    const existingQrCode = await db.queryOne('SELECT * FROM qr_codes WHERE id = ? AND is_active = TRUE', [id]);
    if (!existingQrCode) {
      return res.status(404).json({
        success: false,
        message: 'QR kod bulunamadı'
      });
    }
    
    await db.query('UPDATE qr_codes SET download_count = download_count + 1 WHERE id = ?', [id]);
    
    res.json({
      success: true,
      message: 'Download sayısı güncellendi'
    });
  } catch (error) {
    console.error('❌ QR Code download error:', error);
    res.status(500).json({
      success: false,
      message: 'Download sayısı güncellenirken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/qr-codes/types/stats - QR tip istatistikleri
router.get('/types/stats', async (req, res) => {
  try {
    const stats = await db.query(`
      SELECT 
        type,
        COUNT(*) as count,
        SUM(download_count) as total_downloads
      FROM qr_codes 
      WHERE is_active = TRUE 
      GROUP BY type 
      ORDER BY count DESC
    `);
    
    res.json({
      success: true,
      data: stats
    });
  } catch (error) {
    console.error('❌ QR types stats error:', error);
    res.status(500).json({
      success: false,
      message: 'QR tip istatistikleri yüklenirken hata oluştu',
      error: error.message
    });
  }
});

module.exports = router;



