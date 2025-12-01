const express = require('express');
const { body, validationResult } = require('express-validator');
const db = require('../config/database');
const router = express.Router();

// ============================================
// 🛒 SALES API ENDPOINTS
// ============================================

// GET /api/sales - Tüm satışları listele
router.get('/', async (req, res) => {
  try {
    const { 
      payment_method, 
      start_date, 
      end_date, 
      page = 1, 
      limit = 20 
    } = req.query;
    
    let sql = `
      SELECT s.*, 
             COUNT(si.id) as item_count,
             GROUP_CONCAT(si.product_name || ' (' || si.quantity || 'x)', ', ') as items_summary
      FROM sales s 
      LEFT JOIN sale_items si ON s.id = si.sale_id
      WHERE 1=1
    `;
    const params = [];
    
    // Payment method filter
    if (payment_method && payment_method !== 'all') {
      sql += ' AND s.payment_method = ?';
      params.push(payment_method);
    }
    
    // Date range filter
    if (start_date) {
      sql += ' AND DATE(s.sale_date) >= ?';
      params.push(start_date);
    }
    
    if (end_date) {
      sql += ' AND DATE(s.sale_date) <= ?';
      params.push(end_date);
    }
    
    // Group by and pagination
    sql += ' GROUP BY s.id ORDER BY s.sale_date DESC';
    
    const offset = (page - 1) * limit;
    sql += ' LIMIT ? OFFSET ?';
    params.push(parseInt(limit), parseInt(offset));
    
    const sales = db.query(sql, params);
    
    // Process customer_info JSON
    const processedSales = sales.map(sale => ({
      ...sale,
      customer_info: sale.customer_info ? JSON.parse(sale.customer_info) : null
    }));
    
    // Total count
    let countSql = 'SELECT COUNT(*) as total FROM sales WHERE 1=1';
    const countParams = [];
    
    if (payment_method && payment_method !== 'all') {
      countSql += ' AND payment_method = ?';
      countParams.push(payment_method);
    }
    
    if (start_date) {
      countSql += ' AND DATE(sale_date) >= ?';
      countParams.push(start_date);
    }
    
    if (end_date) {
      countSql += ' AND DATE(sale_date) <= ?';
      countParams.push(end_date);
    }
    
    const countResult = db.query(countSql, countParams);
    const total = countResult[0]?.total || 0;
    
    res.json({
      success: true,
      data: processedSales,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        total,
        pages: Math.ceil(total / limit)
      }
    });
  } catch (error) {
    console.error('❌ Sales fetch error:', error);
    res.status(500).json({
      success: false,
      message: 'Satışlar yüklenirken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/sales/:id - Tek satış detayı
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    
    // Sale details
    const sale = db.queryOne('SELECT * FROM sales WHERE id = ?', [id]);
    
    if (!sale) {
      return res.status(404).json({
        success: false,
        message: 'Satış bulunamadı'
      });
    }
    
    // Sale items
    const saleItems = db.query(`
      SELECT si.*, p.name as current_product_name, p.category
      FROM sale_items si
      LEFT JOIN products p ON si.product_id = p.id
      WHERE si.sale_id = ?
      ORDER BY si.id
    `, [id]);
    
    // Parse customer_info
    sale.customer_info = sale.customer_info ? JSON.parse(sale.customer_info) : null;
    sale.items = saleItems;
    
    res.json({
      success: true,
      data: sale
    });
  } catch (error) {
    console.error('❌ Sale fetch error:', error);
    res.status(500).json({
      success: false,
      message: 'Satış yüklenirken hata oluştu',
      error: error.message
    });
  }
});

// POST /api/sales - Yeni satış oluştur
router.post('/', [
  body('total_amount').isFloat({ min: 0 }).withMessage('Geçerli toplam tutar girin'),
  body('payment_method').isIn(['cash', 'credit_card', 'debit_card']).withMessage('Geçerli ödeme yöntemi seçin'),
  body('items').isArray({ min: 1 }).withMessage('En az bir ürün gerekli'),
  body('items.*.product_name').trim().isLength({ min: 1 }).withMessage('Ürün adı gerekli'),
  body('items.*.quantity').isInt({ min: 1 }).withMessage('Geçerli miktar girin'),
  body('items.*.unit_price').isFloat({ min: 0 }).withMessage('Geçerli birim fiyat girin')
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
    
    const { total_amount, payment_method, customer_info, notes, items } = req.body;
    
    // Validate total amount matches items
    const calculatedTotal = items.reduce((sum, item) => {
      return sum + (item.quantity * item.unit_price);
    }, 0);
    
    if (Math.abs(calculatedTotal - total_amount) > 0.01) {
      return res.status(400).json({
        success: false,
        message: 'Toplam tutar ile ürün tutarları uyuşmuyor'
      });
    }
    
    // SQLite transaction - senkron
    const createSale = db.transaction((saleData) => {
      // Create sale
      const customerInfoJson = customer_info ? JSON.stringify(customer_info) : null;
      const saleResult = db.query(`
        INSERT INTO sales (total_amount, payment_method, customer_info, notes)
        VALUES (?, ?, ?, ?)
      `, [total_amount, payment_method, customerInfoJson, notes]);
      
      const saleId = saleResult.insertId;
      
      // Create sale items and update product stock
      for (const item of items) {
        const { product_id, product_name, quantity, unit_price } = item;
        const total_price = quantity * unit_price;
        
        // Insert sale item
        db.query(`
          INSERT INTO sale_items (sale_id, product_id, product_name, quantity, unit_price, total_price)
          VALUES (?, ?, ?, ?, ?, ?)
        `, [saleId, product_id, product_name, quantity, unit_price, total_price]);
        
        // Update product stock if product_id exists
        if (product_id) {
          const updateResult = db.query(`
            UPDATE products 
            SET stock_quantity = MAX(0, stock_quantity - ?) 
            WHERE id = ?
          `, [quantity, product_id]);
          
          if (updateResult.affectedRows === 0) {
            console.warn(`⚠️ Product ${product_id} not found for stock update`);
          }
        }
      }
      
      return saleId;
    });
    
    // Run transaction
    const saleId = createSale({ total_amount, payment_method, customer_info, notes, items });
    
    // Fetch created sale with items
    const newSale = db.queryOne('SELECT * FROM sales WHERE id = ?', [saleId]);
    const saleItems = db.query('SELECT * FROM sale_items WHERE sale_id = ?', [saleId]);
    
    newSale.customer_info = newSale.customer_info ? JSON.parse(newSale.customer_info) : null;
    newSale.items = saleItems;
    
    res.status(201).json({
      success: true,
      message: 'Satış başarıyla oluşturuldu',
      data: newSale
    });
  } catch (error) {
    console.error('❌ Sale create error:', error);
    res.status(500).json({
      success: false,
      message: 'Satış oluştururken hata oluştu',
      error: error.message
    });
  }
});

// DELETE /api/sales/:id - Satış sil (hard delete - dikkatli kullanın)
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    
    const existingSale = db.queryOne('SELECT * FROM sales WHERE id = ?', [id]);
    if (!existingSale) {
      return res.status(404).json({
        success: false,
        message: 'Satış bulunamadı'
      });
    }
    
    // SQLite transaction
    const deleteSale = db.transaction((saleId) => {
      // Get sale items to restore stock
      const saleItems = db.query('SELECT * FROM sale_items WHERE sale_id = ?', [saleId]);
      
      // Restore product stock
      for (const item of saleItems) {
        if (item.product_id) {
          db.query(`
            UPDATE products 
            SET stock_quantity = stock_quantity + ? 
            WHERE id = ?
          `, [item.quantity, item.product_id]);
        }
      }
      
      // Delete sale items first (foreign key constraint)
      db.query('DELETE FROM sale_items WHERE sale_id = ?', [saleId]);
      
      // Delete sale
      db.query('DELETE FROM sales WHERE id = ?', [saleId]);
    });
    
    // Run transaction
    deleteSale(id);
    
    res.json({
      success: true,
      message: 'Satış başarıyla silindi ve stok geri yüklendi'
    });
  } catch (error) {
    console.error('❌ Sale delete error:', error);
    res.status(500).json({
      success: false,
      message: 'Satış silinirken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/sales/stats/daily - Günlük satış istatistikleri
router.get('/stats/daily', async (req, res) => {
  try {
    const { days = 30 } = req.query;
    
    const stats = db.query(`
      SELECT 
        DATE(sale_date) as date,
        COUNT(*) as sale_count,
        SUM(total_amount) as total_revenue,
        AVG(total_amount) as avg_sale_amount,
        payment_method,
        SUM(CASE WHEN payment_method = 'cash' THEN 1 ELSE 0 END) as cash_count,
        SUM(CASE WHEN payment_method = 'credit_card' THEN 1 ELSE 0 END) as credit_count,
        SUM(CASE WHEN payment_method = 'debit_card' THEN 1 ELSE 0 END) as debit_count
      FROM sales 
      WHERE sale_date >= datetime('now', '-${parseInt(days)} days')
      GROUP BY DATE(sale_date), payment_method
      ORDER BY date DESC
    `, []);
    
    res.json({
      success: true,
      data: stats
    });
  } catch (error) {
    console.error('❌ Daily sales stats error:', error);
    res.status(500).json({
      success: false,
      message: 'Günlük satış istatistikleri yüklenirken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/sales/stats/summary - Satış özet istatistikleri
router.get('/stats/summary', async (req, res) => {
  try {
    const summaryResult = db.query(`
      SELECT 
        COUNT(*) as total_sales,
        SUM(total_amount) as total_revenue,
        AVG(total_amount) as avg_sale_amount,
        MAX(total_amount) as highest_sale,
        MIN(total_amount) as lowest_sale,
        SUM(CASE WHEN DATE(sale_date) = DATE('now') THEN 1 ELSE 0 END) as today_sales,
        SUM(CASE WHEN DATE(sale_date) = DATE('now') THEN total_amount ELSE 0 END) as today_revenue,
        SUM(CASE WHEN DATE(sale_date) >= DATE('now', '-7 days') THEN 1 ELSE 0 END) as week_sales,
        SUM(CASE WHEN DATE(sale_date) >= DATE('now', '-7 days') THEN total_amount ELSE 0 END) as week_revenue,
        SUM(CASE WHEN DATE(sale_date) >= DATE('now', '-30 days') THEN 1 ELSE 0 END) as month_sales,
        SUM(CASE WHEN DATE(sale_date) >= DATE('now', '-30 days') THEN total_amount ELSE 0 END) as month_revenue
      FROM sales
    `);
    
    const summary = summaryResult[0];
    
    res.json({
      success: true,
      data: summary
    });
  } catch (error) {
    console.error('❌ Sales summary error:', error);
    res.status(500).json({
      success: false,
      message: 'Satış özeti yüklenirken hata oluştu',
      error: error.message
    });
  }
});

module.exports = router;
