const express = require('express');
const { body, validationResult } = require('express-validator');
const db = require('../config/database');
const router = express.Router();

// ============================================
// 📦 PRODUCTS API ENDPOINTS
// ============================================

// GET /api/products - Tüm ürünleri listele
router.get('/', async (req, res) => {
  try {
    const { category, search, page = 1, limit = 50 } = req.query;
    let sql = `
      SELECT p.*, c.name as category_name, c.icon as category_icon, c.color as category_color
      FROM products p 
      LEFT JOIN categories c ON p.category = c.name
      WHERE p.is_active = TRUE
    `;
    const params = [];
    
    // Category filter
    if (category && category !== 'all') {
      sql += ' AND p.category = ?';
      params.push(category);
    }
    
    // Search filter
    if (search) {
      sql += ' AND (p.name LIKE ? OR p.description LIKE ? OR p.barcode LIKE ?)';
      params.push(`%${search}%`, `%${search}%`, `%${search}%`);
    }
    
    // Pagination
    const offset = (page - 1) * limit;
    sql += ' ORDER BY p.created_at DESC LIMIT ? OFFSET ?';
    params.push(parseInt(limit), parseInt(offset));
    
    const products = await db.query(sql, params);
    
    // Total count
    let countSql = 'SELECT COUNT(*) as total FROM products WHERE is_active = TRUE';
    const countParams = [];
    
    if (category && category !== 'all') {
      countSql += ' AND category = ?';
      countParams.push(category);
    }
    
    if (search) {
      countSql += ' AND (name LIKE ? OR description LIKE ? OR barcode LIKE ?)';
      countParams.push(`%${search}%`, `%${search}%`, `%${search}%`);
    }
    
    const [{ total }] = await db.query(countSql, countParams);
    
    res.json({
      success: true,
      data: products,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        total,
        pages: Math.ceil(total / limit)
      }
    });
  } catch (error) {
    console.error('❌ Products fetch error:', error);
    res.status(500).json({
      success: false,
      message: 'Ürünler yüklenirken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/products/:id - Tek ürün detayı
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const product = await db.queryOne(`
      SELECT p.*, c.name as category_name, c.icon as category_icon, c.color as category_color
      FROM products p 
      LEFT JOIN categories c ON p.category = c.name
      WHERE p.id = ? AND p.is_active = TRUE
    `, [id]);
    
    if (!product) {
      return res.status(404).json({
        success: false,
        message: 'Ürün bulunamadı'
      });
    }
    
    res.json({
      success: true,
      data: product
    });
  } catch (error) {
    console.error('❌ Product fetch error:', error);
    res.status(500).json({
      success: false,
      message: 'Ürün yüklenirken hata oluştu',
      error: error.message
    });
  }
});

// POST /api/products - Yeni ürün oluştur
router.post('/', [
  body('name').trim().isLength({ min: 1 }).withMessage('Ürün adı gerekli'),
  body('price').isFloat({ min: 0 }).withMessage('Geçerli fiyat girin'),
  body('stock_quantity').isInt({ min: 0 }).withMessage('Geçerli stok miktarı girin')
], async (req, res) => {
  try {
    // Validation check
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({
        success: false,
        message: 'Validasyon hatası',
        errors: errors.array()
      });
    }
    
    const { name, description, category, price, stock_quantity, barcode, qr_code, image_url } = req.body;
    
    // Check if barcode exists
    if (barcode) {
      const existingProduct = await db.queryOne('SELECT id FROM products WHERE barcode = ?', [barcode]);
      if (existingProduct) {
        return res.status(400).json({
          success: false,
          message: 'Bu barkod zaten mevcut'
        });
      }
    }
    
    const result = await db.query(`
      INSERT INTO products (name, description, category, price, stock_quantity, barcode, qr_code, image_url)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `, [name, description, category, price, stock_quantity, barcode, qr_code, image_url]);
    
    const newProduct = await db.queryOne('SELECT * FROM products WHERE id = ?', [result.insertId]);
    
    res.status(201).json({
      success: true,
      message: 'Ürün başarıyla oluşturuldu',
      data: newProduct
    });
  } catch (error) {
    console.error('❌ Product create error:', error);
    res.status(500).json({
      success: false,
      message: 'Ürün oluştururken hata oluştu',
      error: error.message
    });
  }
});

// PUT /api/products/:id - Ürün güncelle
router.put('/:id', [
  body('name').trim().isLength({ min: 1 }).withMessage('Ürün adı gerekli'),
  body('price').isFloat({ min: 0 }).withMessage('Geçerli fiyat girin'),
  body('stock_quantity').isInt({ min: 0 }).withMessage('Geçerli stok miktarı girin')
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
    const { name, description, category, price, stock_quantity, barcode, qr_code, image_url } = req.body;
    
    // Check if product exists
    const existingProduct = await db.queryOne('SELECT * FROM products WHERE id = ? AND is_active = TRUE', [id]);
    if (!existingProduct) {
      return res.status(404).json({
        success: false,
        message: 'Ürün bulunamadı'
      });
    }
    
    // Check barcode uniqueness (excluding current product)
    if (barcode && barcode !== existingProduct.barcode) {
      const duplicateBarcode = await db.queryOne('SELECT id FROM products WHERE barcode = ? AND id != ?', [barcode, id]);
      if (duplicateBarcode) {
        return res.status(400).json({
          success: false,
          message: 'Bu barkod zaten başka bir üründe kullanılıyor'
        });
      }
    }
    
    await db.query(`
      UPDATE products 
      SET name = ?, description = ?, category = ?, price = ?, stock_quantity = ?, 
          barcode = ?, qr_code = ?, image_url = ?, updated_at = datetime('now')
      WHERE id = ?
    `, [name, description, category, price, stock_quantity, barcode, qr_code, image_url, id]);
    
    const updatedProduct = await db.queryOne('SELECT * FROM products WHERE id = ?', [id]);
    
    res.json({
      success: true,
      message: 'Ürün başarıyla güncellendi',
      data: updatedProduct
    });
  } catch (error) {
    console.error('❌ Product update error:', error);
    res.status(500).json({
      success: false,
      message: 'Ürün güncellenirken hata oluştu',
      error: error.message
    });
  }
});

// DELETE /api/products/:id - Ürün sil (soft delete)
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    
    const existingProduct = await db.queryOne('SELECT * FROM products WHERE id = ? AND is_active = TRUE', [id]);
    if (!existingProduct) {
      return res.status(404).json({
        success: false,
        message: 'Ürün bulunamadı'
      });
    }
    
    await db.query('UPDATE products SET is_active = FALSE WHERE id = ?', [id]);
    
    res.json({
      success: true,
      message: 'Ürün başarıyla silindi'
    });
  } catch (error) {
    console.error('❌ Product delete error:', error);
    res.status(500).json({
      success: false,
      message: 'Ürün silinirken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/products/barcode/:barcode - Barkod ile ürün ara
router.get('/barcode/:barcode', async (req, res) => {
  try {
    const { barcode } = req.params;
    const product = await db.queryOne(`
      SELECT p.*, c.name as category_name, c.icon as category_icon, c.color as category_color
      FROM products p 
      LEFT JOIN categories c ON p.category = c.name
      WHERE p.barcode = ? AND p.is_active = TRUE
    `, [barcode]);
    
    if (!product) {
      return res.status(404).json({
        success: false,
        message: 'Bu barkod ile ürün bulunamadı'
      });
    }
    
    res.json({
      success: true,
      data: product
    });
  } catch (error) {
    console.error('❌ Barcode search error:', error);
    res.status(500).json({
      success: false,
      message: 'Barkod aranırken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/products/categories/list - Kategori listesi
router.get('/categories/list', async (req, res) => {
  try {
    const categories = await db.query('SELECT * FROM categories WHERE is_active = TRUE ORDER BY name');
    
    res.json({
      success: true,
      data: categories
    });
  } catch (error) {
    console.error('❌ Categories fetch error:', error);
    res.status(500).json({
      success: false,
      message: 'Kategoriler yüklenirken hata oluştu',
      error: error.message
    });
  }
});

module.exports = router;



