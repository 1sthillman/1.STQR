const express = require('express');
const { body, validationResult } = require('express-validator');
const db = require('../config/database');
const router = express.Router();

// ============================================
// 📱 SCAN HISTORY API ENDPOINTS - SQLite
// ============================================

// GET /api/scan-history - Tüm tarama geçmişini listele
router.get('/', async (req, res) => {
  try {
    const { 
      qr_type, 
      successful_only, 
      start_date, 
      end_date, 
      search,
      page = 1, 
      limit = 50 
    } = req.query;
    
    let sql = `
      SELECT sh.*, p.name as product_name, p.category as product_category
      FROM scan_history sh
      LEFT JOIN products p ON sh.product_id = p.id
      WHERE 1=1
    `;
    const params = [];
    
    // QR type filter
    if (qr_type && qr_type !== 'all') {
      sql += ' AND sh.qr_type = ?';
      params.push(qr_type);
    }
    
    // Success filter
    if (successful_only === 'true') {
      sql += ' AND sh.was_successful = 1';
    }
    
    // Date range filter
    if (start_date) {
      sql += ' AND DATE(sh.scanned_at) >= ?';
      params.push(start_date);
    }
    
    if (end_date) {
      sql += ' AND DATE(sh.scanned_at) <= ?';
      params.push(end_date);
    }
    
    // Search filter
    if (search) {
      sql += ' AND (sh.qr_content LIKE ? OR p.name LIKE ? OR sh.error_message LIKE ?)';
      params.push(`%${search}%`, `%${search}%`, `%${search}%`);
    }
    
    // Pagination
    const offset = (page - 1) * limit;
    sql += ' ORDER BY sh.scanned_at DESC LIMIT ? OFFSET ?';
    params.push(parseInt(limit), parseInt(offset));
    
    const scans = db.query(sql, params);
    
    // Process scan_result JSON
    const processedScans = scans.map(scan => ({
      ...scan,
      scan_result: scan.scan_result ? JSON.parse(scan.scan_result) : null
    }));
    
    // Total count
    let countSql = 'SELECT COUNT(*) as total FROM scan_history sh LEFT JOIN products p ON sh.product_id = p.id WHERE 1=1';
    const countParams = [];
    
    if (qr_type && qr_type !== 'all') {
      countSql += ' AND sh.qr_type = ?';
      countParams.push(qr_type);
    }
    
    if (successful_only === 'true') {
      countSql += ' AND sh.was_successful = 1';
    }
    
    if (start_date) {
      countSql += ' AND DATE(sh.scanned_at) >= ?';
      countParams.push(start_date);
    }
    
    if (end_date) {
      countSql += ' AND DATE(sh.scanned_at) <= ?';
      countParams.push(end_date);
    }
    
    if (search) {
      countSql += ' AND (sh.qr_content LIKE ? OR p.name LIKE ? OR sh.error_message LIKE ?)';
      countParams.push(`%${search}%`, `%${search}%`, `%${search}%`);
    }
    
    const countResult = db.query(countSql, countParams);
    const total = countResult[0]?.total || 0;
    
    res.json({
      success: true,
      data: processedScans,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        total,
        pages: Math.ceil(total / limit)
      }
    });
  } catch (error) {
    console.error('❌ Scan history fetch error:', error);
    res.status(500).json({
      success: false,
      message: 'Tarama geçmişi yüklenirken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/scan-history/:id - Tek tarama detayı
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const scan = db.queryOne(`
      SELECT sh.*, p.name as product_name, p.category as product_category, 
             p.price as product_price, p.barcode as product_barcode
      FROM scan_history sh
      LEFT JOIN products p ON sh.product_id = p.id
      WHERE sh.id = ?
    `, [id]);
    
    if (!scan) {
      return res.status(404).json({
        success: false,
        message: 'Tarama bulunamadı'
      });
    }
    
    // Parse scan_result JSON
    scan.scan_result = scan.scan_result ? JSON.parse(scan.scan_result) : null;
    
    res.json({
      success: true,
      data: scan
    });
  } catch (error) {
    console.error('❌ Scan fetch error:', error);
    res.status(500).json({
      success: false,
      message: 'Tarama yüklenirken hata oluştu',
      error: error.message
    });
  }
});

// POST /api/scan-history - Yeni tarama kaydı oluştur
router.post('/', [
  body('qr_content').trim().isLength({ min: 1 }).withMessage('QR içeriği gerekli'),
  body('was_successful').isBoolean().withMessage('Başarı durumu boolean olmalı')
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
    
    const { 
      qr_content, 
      qr_type, 
      product_id, 
      scan_result, 
      was_successful, 
      error_message 
    } = req.body;
    
    // Convert scan_result object to JSON string
    const scanResultJson = scan_result ? JSON.stringify(scan_result) : null;
    
    const result = db.query(`
      INSERT INTO scan_history (qr_content, qr_type, product_id, scan_result, was_successful, error_message)
      VALUES (?, ?, ?, ?, ?, ?)
    `, [qr_content, qr_type, product_id, scanResultJson, was_successful ? 1 : 0, error_message]);
    
    const newScan = db.queryOne(`
      SELECT sh.*, p.name as product_name, p.category as product_category
      FROM scan_history sh
      LEFT JOIN products p ON sh.product_id = p.id
      WHERE sh.id = ?
    `, [result.insertId]);
    
    // Parse scan_result back
    if (newScan.scan_result) {
      newScan.scan_result = JSON.parse(newScan.scan_result);
    }
    
    res.status(201).json({
      success: true,
      message: 'Tarama kaydı başarıyla oluşturuldu',
      data: newScan
    });
  } catch (error) {
    console.error('❌ Scan history create error:', error);
    res.status(500).json({
      success: false,
      message: 'Tarama kaydı oluştururken hata oluştu',
      error: error.message
    });
  }
});

// DELETE /api/scan-history/:id - Tarama kaydı sil
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    
    const existingScan = db.queryOne('SELECT * FROM scan_history WHERE id = ?', [id]);
    if (!existingScan) {
      return res.status(404).json({
        success: false,
        message: 'Tarama kaydı bulunamadı'
      });
    }
    
    db.query('DELETE FROM scan_history WHERE id = ?', [id]);
    
    res.json({
      success: true,
      message: 'Tarama kaydı başarıyla silindi'
    });
  } catch (error) {
    console.error('❌ Scan history delete error:', error);
    res.status(500).json({
      success: false,
      message: 'Tarama kaydı silinirken hata oluştu',
      error: error.message
    });
  }
});

// DELETE /api/scan-history - Tüm tarama geçmişini temizle
router.delete('/', async (req, res) => {
  try {
    const { confirm } = req.body;
    
    if (confirm !== 'DELETE_ALL_SCAN_HISTORY') {
      return res.status(400).json({
        success: false,
        message: 'Onay kodu gerekli: DELETE_ALL_SCAN_HISTORY'
      });
    }
    
    const result = db.query('DELETE FROM scan_history');
    
    res.json({
      success: true,
      message: `${result.affectedRows || result.changes} tarama kaydı başarıyla silindi`
    });
  } catch (error) {
    console.error('❌ Scan history clear error:', error);
    res.status(500).json({
      success: false,
      message: 'Tarama geçmişi temizlenirken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/scan-history/stats/summary - Tarama istatistikleri
router.get('/stats/summary', async (req, res) => {
  try {
    const summaryResult = db.query(`
      SELECT 
        COUNT(*) as total_scans,
        SUM(CASE WHEN was_successful = 1 THEN 1 ELSE 0 END) as successful_scans,
        SUM(CASE WHEN was_successful = 0 THEN 1 ELSE 0 END) as failed_scans,
        ROUND((SUM(CASE WHEN was_successful = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)), 2) as success_rate,
        SUM(CASE WHEN DATE(scanned_at) = DATE('now') THEN 1 ELSE 0 END) as today_scans,
        SUM(CASE WHEN DATE(scanned_at) >= DATE('now', '-7 days') THEN 1 ELSE 0 END) as week_scans,
        SUM(CASE WHEN DATE(scanned_at) >= DATE('now', '-30 days') THEN 1 ELSE 0 END) as month_scans,
        COUNT(DISTINCT qr_type) as unique_qr_types,
        COUNT(DISTINCT product_id) as scanned_products
      FROM scan_history
    `);
    
    const summary = summaryResult[0];
    
    // QR Type breakdown
    const qrTypeStats = db.query(`
      SELECT 
        qr_type,
        COUNT(*) as count,
        SUM(CASE WHEN was_successful = 1 THEN 1 ELSE 0 END) as successful,
        ROUND((SUM(CASE WHEN was_successful = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)), 2) as success_rate
      FROM scan_history
      WHERE qr_type IS NOT NULL
      GROUP BY qr_type
      ORDER BY count DESC
    `);
    
    // Recent activity (last 24 hours by hour)
    const hourlyActivity = db.query(`
      SELECT 
        CAST(strftime('%H', scanned_at) AS INTEGER) as hour,
        COUNT(*) as scan_count,
        SUM(CASE WHEN was_successful = 1 THEN 1 ELSE 0 END) as successful_count
      FROM scan_history
      WHERE scanned_at >= datetime('now', '-24 hours')
      GROUP BY CAST(strftime('%H', scanned_at) AS INTEGER)
      ORDER BY hour
    `);
    
    res.json({
      success: true,
      data: {
        summary,
        qr_type_stats: qrTypeStats,
        hourly_activity: hourlyActivity
      }
    });
  } catch (error) {
    console.error('❌ Scan stats error:', error);
    res.status(500).json({
      success: false,
      message: 'Tarama istatistikleri yüklenirken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/scan-history/export - Tarama geçmişini export et
router.get('/export', async (req, res) => {
  try {
    const { format = 'json', start_date, end_date } = req.query;
    
    let sql = `
      SELECT 
        sh.id,
        sh.qr_content,
        sh.qr_type,
        p.name as product_name,
        p.barcode as product_barcode,
        sh.scanned_at,
        sh.was_successful,
        sh.error_message
      FROM scan_history sh
      LEFT JOIN products p ON sh.product_id = p.id
      WHERE 1=1
    `;
    const params = [];
    
    if (start_date) {
      sql += ' AND DATE(sh.scanned_at) >= ?';
      params.push(start_date);
    }
    
    if (end_date) {
      sql += ' AND DATE(sh.scanned_at) <= ?';
      params.push(end_date);
    }
    
    sql += ' ORDER BY sh.scanned_at DESC';
    
    const scans = db.query(sql, params);
    
    if (format === 'csv') {
      // CSV format
      let csv = 'ID,QR İçeriği,QR Tipi,Ürün Adı,Barkod,Tarama Zamanı,Başarılı,Hata Mesajı\n';
      
      scans.forEach(scan => {
        csv += `${scan.id},"${(scan.qr_content || '').replace(/"/g, '""')}","${scan.qr_type || ''}","${(scan.product_name || '').replace(/"/g, '""')}","${scan.product_barcode || ''}","${scan.scanned_at}","${scan.was_successful ? 'Evet' : 'Hayır'}","${(scan.error_message || '').replace(/"/g, '""')}"\n`;
      });
      
      res.setHeader('Content-Type', 'text/csv; charset=utf-8');
      res.setHeader('Content-Disposition', `attachment; filename="tarama-gecmisi-${new Date().toISOString().split('T')[0]}.csv"`);
      res.send(csv);
    } else {
      // JSON format
      res.setHeader('Content-Type', 'application/json');
      res.setHeader('Content-Disposition', `attachment; filename="tarama-gecmisi-${new Date().toISOString().split('T')[0]}.json"`);
      res.json({
        export_date: new Date().toISOString(),
        total_records: scans.length,
        data: scans
      });
    }
  } catch (error) {
    console.error('❌ Scan history export error:', error);
    res.status(500).json({
      success: false,
      message: 'Tarama geçmişi export edilirken hata oluştu',
      error: error.message
    });
  }
});

module.exports = router;
