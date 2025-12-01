const express = require('express');
const db = require('../config/database');
const router = express.Router();

// ============================================
// 📊 STATISTICS API ENDPOINTS - SQLite
// ============================================

// GET /api/stats - Ana sayfa istatistikleri
router.get('/', async (req, res) => {
  try {
    // Ana istatistikler
    const mainStatsResult = db.query(`
      SELECT 
        (SELECT COUNT(*) FROM qr_codes WHERE is_active = 1) as total_qr_codes,
        (SELECT SUM(download_count) FROM qr_codes WHERE is_active = 1) as total_qr_downloads,
        (SELECT COUNT(*) FROM products WHERE is_active = 1) as total_products,
        (SELECT COUNT(*) FROM scan_history) as total_scans,
        (SELECT COUNT(*) FROM sales) as total_sales,
        (SELECT COALESCE(SUM(total_amount), 0) FROM sales) as total_revenue,
        (SELECT COUNT(*) FROM scan_history WHERE DATE(scanned_at) = DATE('now')) as today_scans,
        (SELECT COUNT(*) FROM sales WHERE DATE(sale_date) = DATE('now')) as today_sales,
        (SELECT COALESCE(SUM(total_amount), 0) FROM sales WHERE DATE(sale_date) = DATE('now')) as today_revenue
    `);
    
    const mainStats = mainStatsResult[0];

    // Son 7 günün günlük istatistikleri  
    const dailyStats = db.query(`
      WITH RECURSIVE dates(date, n) AS (
        SELECT DATE('now'), 0
        UNION ALL
        SELECT DATE('now', '-' || (n+1) || ' days'), n+1
        FROM dates
        WHERE n < 6
      )
      SELECT 
        d.date,
        COALESCE(s.scan_count, 0) as scans,
        COALESCE(sales.sale_count, 0) as sales,
        COALESCE(sales.revenue, 0) as revenue,
        COALESCE(qr.qr_count, 0) as qr_created
      FROM dates d
      LEFT JOIN (
        SELECT DATE(scanned_at) as date, COUNT(*) as scan_count
        FROM scan_history 
        WHERE scanned_at >= DATE('now', '-7 days')
        GROUP BY DATE(scanned_at)
      ) s ON d.date = s.date
      LEFT JOIN (
        SELECT DATE(sale_date) as date, COUNT(*) as sale_count, SUM(total_amount) as revenue
        FROM sales 
        WHERE sale_date >= DATE('now', '-7 days')
        GROUP BY DATE(sale_date)
      ) sales ON d.date = sales.date
      LEFT JOIN (
        SELECT DATE(created_at) as date, COUNT(*) as qr_count
        FROM qr_codes 
        WHERE created_at >= DATE('now', '-7 days')
        GROUP BY DATE(created_at)
      ) qr ON d.date = qr.date
      ORDER BY d.date DESC
    `);

    // En popüler QR tipleri
    const popularQrTypes = db.query(`
      SELECT 
        type,
        COUNT(*) as count,
        SUM(download_count) as total_downloads
      FROM qr_codes 
      WHERE is_active = 1
      GROUP BY type 
      ORDER BY count DESC 
      LIMIT 5
    `);

    // En çok satılan ürünler
    const topProducts = db.query(`
      SELECT 
        si.product_name,
        p.id as product_id,
        p.category,
        SUM(si.quantity) as total_sold,
        SUM(si.total_price) as total_revenue,
        COUNT(DISTINCT si.sale_id) as sale_count
      FROM sale_items si
      LEFT JOIN products p ON si.product_id = p.id
      GROUP BY si.product_name, p.id, p.category
      ORDER BY total_sold DESC
      LIMIT 10
    `);

    // Son aktiviteler
    const recentActivities = db.query(`
      SELECT 'scan' as type, 'QR Tarama' as title, qr_content as description, scanned_at as created_at
      FROM scan_history 
      WHERE scanned_at >= datetime('now', '-24 hours')
      
      UNION ALL
      
      SELECT 'sale' as type, 'Satış' as title, 
             ('₺' || ROUND(total_amount, 2) || ' - ' || payment_method) as description, 
             sale_date as created_at
      FROM sales 
      WHERE sale_date >= datetime('now', '-24 hours')
      
      UNION ALL
      
      SELECT 'qr_create' as type, 'QR Oluşturma' as title, 
             (type || ': ' || SUBSTR(content, 1, 50)) as description,
             created_at
      FROM qr_codes 
      WHERE created_at >= datetime('now', '-24 hours')
      
      ORDER BY created_at DESC
      LIMIT 20
    `);

    // Ödeme yöntemi dağılımı
    const paymentMethodStats = db.query(`
      SELECT 
        payment_method,
        COUNT(*) as count,
        SUM(total_amount) as total_amount,
        ROUND((COUNT(*) * 100.0 / (SELECT COUNT(*) FROM sales)), 2) as percentage
      FROM sales
      GROUP BY payment_method
      ORDER BY count DESC
    `);

    // Kategori bazlı stok durumu
    const stockByCategory = db.query(`
      SELECT 
        c.name as category,
        c.color,
        c.icon,
        COUNT(p.id) as product_count,
        SUM(p.stock_quantity) as total_stock,
        AVG(p.price) as avg_price,
        SUM(CASE WHEN p.stock_quantity <= 5 THEN 1 ELSE 0 END) as low_stock_count
      FROM categories c
      LEFT JOIN products p ON c.name = p.category AND p.is_active = 1
      WHERE c.is_active = 1
      GROUP BY c.id, c.name, c.color, c.icon
      ORDER BY product_count DESC
    `);

    res.json({
      success: true,
      data: {
        main_stats: mainStats,
        daily_stats: dailyStats,
        popular_qr_types: popularQrTypes,
        top_products: topProducts,
        recent_activities: recentActivities,
        payment_methods: paymentMethodStats,
        stock_by_category: stockByCategory
      }
    });
  } catch (error) {
    console.error('❌ Stats fetch error:', error);
    res.status(500).json({
      success: false,
      message: 'İstatistikler yüklenirken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/stats/dashboard - Dashboard özet istatistikleri
router.get('/dashboard', async (req, res) => {
  try {
    const dashboardResult = db.query(`
      SELECT 
        -- QR İstatistikleri
        (SELECT COUNT(*) FROM qr_codes WHERE is_active = 1) as qr_count,
        (SELECT COUNT(*) FROM qr_codes WHERE DATE(created_at) = DATE('now')) as qr_today,
        (SELECT SUM(download_count) FROM qr_codes WHERE is_active = 1) as total_downloads,
        
        -- Tarama İstatistikleri
        (SELECT COUNT(*) FROM scan_history) as scan_count,
        (SELECT COUNT(*) FROM scan_history WHERE DATE(scanned_at) = DATE('now')) as scan_today,
        (SELECT ROUND((SUM(CASE WHEN was_successful = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)), 2) FROM scan_history) as scan_success_rate,
        
        -- Ürün İstatistikleri
        (SELECT COUNT(*) FROM products WHERE is_active = 1) as product_count,
        (SELECT COUNT(*) FROM products WHERE is_active = 1 AND stock_quantity <= 5) as low_stock_count,
        (SELECT AVG(price) FROM products WHERE is_active = 1) as avg_product_price,
        
        -- Satış İstatistikleri
        (SELECT COUNT(*) FROM sales) as sales_count,
        (SELECT COUNT(*) FROM sales WHERE DATE(sale_date) = DATE('now')) as sales_today,
        (SELECT COALESCE(SUM(total_amount), 0) FROM sales) as total_revenue,
        (SELECT COALESCE(SUM(total_amount), 0) FROM sales WHERE DATE(sale_date) = DATE('now')) as revenue_today,
        (SELECT COALESCE(AVG(total_amount), 0) FROM sales) as avg_sale_amount
    `);
    
    const dashboard = dashboardResult[0];

    res.json({
      success: true,
      data: dashboard
    });
  } catch (error) {
    console.error('❌ Dashboard stats error:', error);
    res.status(500).json({
      success: false,
      message: 'Dashboard istatistikleri yüklenirken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/stats/revenue - Gelir analizi
router.get('/revenue', async (req, res) => {
  try {
    const { period = 'month' } = req.query;
    
    let dateFormat, intervalDays;
    switch (period) {
      case 'week':
        dateFormat = '%Y-%m-%d';
        intervalDays = 7;
        break;
      case 'year':
        dateFormat = '%Y-%m';
        intervalDays = 365;
        break;
      default: // month
        dateFormat = '%Y-%m-%d';
        intervalDays = 30;
    }

    const revenueStats = db.query(`
      SELECT 
        strftime('${dateFormat}', sale_date) as period,
        COUNT(*) as sale_count,
        SUM(total_amount) as revenue,
        AVG(total_amount) as avg_sale,
        MIN(total_amount) as min_sale,
        MAX(total_amount) as max_sale,
        SUM(CASE WHEN payment_method = 'cash' THEN 1 ELSE 0 END) as cash_sales,
        SUM(CASE WHEN payment_method = 'credit_card' THEN 1 ELSE 0 END) as credit_sales,
        SUM(CASE WHEN payment_method = 'debit_card' THEN 1 ELSE 0 END) as debit_sales
      FROM sales 
      WHERE sale_date >= DATE('now', '-${intervalDays} days')
      GROUP BY strftime('${dateFormat}', sale_date)
      ORDER BY period DESC
    `, []);

    res.json({
      success: true,
      data: {
        period,
        stats: revenueStats
      }
    });
  } catch (error) {
    console.error('❌ Revenue stats error:', error);
    res.status(500).json({
      success: false,
      message: 'Gelir istatistikleri yüklenirken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/stats/products/performance - Ürün performans analizi
router.get('/products/performance', async (req, res) => {
  try {
    const { category, days = 30 } = req.query;

    let sql = `
      SELECT 
        p.id,
        p.name,
        p.category,
        p.price,
        p.stock_quantity,
        COALESCE(sales.total_sold, 0) as total_sold,
        COALESCE(sales.total_revenue, 0) as total_revenue,
        COALESCE(sales.sale_count, 0) as sale_count,
        COALESCE(scans.scan_count, 0) as scan_count,
        CASE 
          WHEN p.stock_quantity = 0 THEN 'Tükendi'
          WHEN p.stock_quantity <= 5 THEN 'Düşük Stok'
          WHEN p.stock_quantity <= 20 THEN 'Orta Stok'
          ELSE 'Yeterli Stok'
        END as stock_status,
        CASE 
          WHEN COALESCE(sales.total_sold, 0) = 0 THEN 'Satılmadı'
          WHEN COALESCE(sales.total_sold, 0) <= 5 THEN 'Düşük Satış'
          WHEN COALESCE(sales.total_sold, 0) <= 20 THEN 'Orta Satış'
          ELSE 'Yüksek Satış'
        END as sales_performance
      FROM products p
      LEFT JOIN (
        SELECT 
          si.product_id,
          SUM(si.quantity) as total_sold,
          SUM(si.total_price) as total_revenue,
          COUNT(DISTINCT si.sale_id) as sale_count
        FROM sale_items si
        JOIN sales s ON si.sale_id = s.id
        WHERE s.sale_date >= DATE('now', '-' || ? || ' days')
        GROUP BY si.product_id
      ) sales ON p.id = sales.product_id
      LEFT JOIN (
        SELECT 
          product_id,
          COUNT(*) as scan_count
        FROM scan_history
        WHERE product_id IS NOT NULL 
          AND scanned_at >= DATE('now', '-' || ? || ' days')
        GROUP BY product_id
      ) scans ON p.id = scans.product_id
      WHERE p.is_active = 1
    `;

    const params = [parseInt(days), parseInt(days)];

    if (category && category !== 'all') {
      sql += ' AND p.category = ?';
      params.push(category);
    }

    sql += ' ORDER BY total_sold DESC, scan_count DESC';

    const products = db.query(sql, params);

    res.json({
      success: true,
      data: products
    });
  } catch (error) {
    console.error('❌ Product performance stats error:', error);
    res.status(500).json({
      success: false,
      message: 'Ürün performans istatistikleri yüklenirken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/stats/categories - Kategori istatistikleri
router.get('/categories', async (req, res) => {
  try {
    const categoryStats = db.query(`
      SELECT 
        c.id,
        c.name,
        c.icon,
        c.color,
        COUNT(p.id) as product_count,
        SUM(CASE WHEN p.is_active = 1 THEN 1 ELSE 0 END) as active_products,
        COALESCE(SUM(p.stock_quantity), 0) as total_stock,
        COALESCE(AVG(p.price), 0) as avg_price,
        COALESCE(sales.total_sold, 0) as total_sold,
        COALESCE(sales.total_revenue, 0) as total_revenue,
        COALESCE(scans.scan_count, 0) as scan_count
      FROM categories c
      LEFT JOIN products p ON c.name = p.category
      LEFT JOIN (
        SELECT 
          p.category,
          SUM(si.quantity) as total_sold,
          SUM(si.total_price) as total_revenue
        FROM sale_items si
        JOIN products p ON si.product_id = p.id
        GROUP BY p.category
      ) sales ON c.name = sales.category
      LEFT JOIN (
        SELECT 
          p.category,
          COUNT(*) as scan_count
        FROM scan_history sh
        JOIN products p ON sh.product_id = p.id
        GROUP BY p.category
      ) scans ON c.name = scans.category
      WHERE c.is_active = 1
      GROUP BY c.id, c.name, c.icon, c.color
      ORDER BY product_count DESC
    `);

    res.json({
      success: true,
      data: categoryStats
    });
  } catch (error) {
    console.error('❌ Category stats error:', error);
    res.status(500).json({
      success: false,
      message: 'Kategori istatistikleri yüklenirken hata oluştu',
      error: error.message
    });
  }
});

// GET /api/stats/hourly - 24 Saatlik Ciro Raporu
router.get('/hourly', async (req, res) => {
  try {
    // Son 24 saat saatlik breakdown
    const hourlyStats = db.query(`
      WITH RECURSIVE hours(hour_num, n) AS (
        SELECT strftime('%H', 'now'), 0
        UNION ALL
        SELECT strftime('%H', datetime('now', '-' || (n+1) || ' hours')), n+1
        FROM hours
        WHERE n < 23
      )
      SELECT 
        h.hour_num as hour,
        COALESCE(s.sale_count, 0) as sales_count,
        COALESCE(s.total_revenue, 0) as revenue,
        COALESCE(s.items_sold, 0) as items_sold
      FROM hours h
      LEFT JOIN (
        SELECT 
          strftime('%H', sale_date) as hour,
          COUNT(*) as sale_count,
          SUM(total_amount) as total_revenue,
          SUM(json_array_length(items)) as items_sold
        FROM sales
        WHERE sale_date >= datetime('now', '-24 hours')
        GROUP BY strftime('%H', sale_date)
      ) s ON h.hour_num = s.hour
      ORDER BY h.hour_num DESC
    `);
    
    // Bugünün toplam cirosu
    const todayTotal = db.queryOne(`
      SELECT 
        COALESCE(SUM(total_amount), 0) as total_revenue,
        COUNT(*) as total_sales,
        COALESCE(AVG(total_amount), 0) as avg_sale
      FROM sales
      WHERE DATE(sale_date) = DATE('now')
    `);
    
    // Son 24 saatin toplam cirosu
    const last24Hours = db.queryOne(`
      SELECT 
        COALESCE(SUM(total_amount), 0) as total_revenue,
        COUNT(*) as total_sales,
        COALESCE(AVG(total_amount), 0) as avg_sale
      FROM sales
      WHERE sale_date >= datetime('now', '-24 hours')
    `);

    res.json({
      success: true,
      data: {
        hourly: hourlyStats,
        today: todayTotal,
        last24Hours: last24Hours
      }
    });
    
  } catch (error) {
    console.error('❌ Hourly stats error:', error);
    res.status(500).json({
      success: false,
      message: '24 saatlik istatistikler yüklenirken hata oluştu',
      error: error.message
    });
  }
});

module.exports = router;
