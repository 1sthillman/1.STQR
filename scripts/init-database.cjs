const Database = require('better-sqlite3');
const path = require('path');
const fs = require('fs');
require('dotenv').config();

// ============================================
// 🏗️ SQLITE DATABASE INITIALIZATION SCRIPT
// ============================================

const DB_PATH = process.env.DB_PATH || path.join(__dirname, '..', 'data', 'qrmaster.db');
const dataDir = path.dirname(DB_PATH);

// Data klasörünü oluştur
if (!fs.existsSync(dataDir)) {
  fs.mkdirSync(dataDir, { recursive: true });
  console.log('📁 Data klasörü oluşturuldu:', dataDir);
}

// Eğer database varsa yedekle
if (fs.existsSync(DB_PATH)) {
  const backupPath = `${DB_PATH}.backup.${Date.now()}`;
  fs.copyFileSync(DB_PATH, backupPath);
  console.log('💾 Mevcut veritabanı yedeklendi:', backupPath);
}

// SQLite veritabanı oluştur
const db = new Database(DB_PATH);

// WAL mode ve foreign keys
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

console.log('🏗️ SQLite veritabanı başlatılıyor...');

// ============================================
// 📦 PRODUCTS TABLE
// ============================================
db.exec(`
CREATE TABLE IF NOT EXISTS products (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  description TEXT,
  category TEXT,
  price REAL DEFAULT 0.00,
  stock_quantity INTEGER DEFAULT 0,
  barcode TEXT UNIQUE,
  qr_code TEXT,
  image_url TEXT,
  created_at TEXT DEFAULT (datetime('now')),
  updated_at TEXT DEFAULT (datetime('now')),
  is_active INTEGER DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_barcode ON products(barcode);
CREATE INDEX IF NOT EXISTS idx_products_active ON products(is_active);
`);

console.log('✅ Products tablosu oluşturuldu');

// ============================================
// 📱 QR CODES TABLE
// ============================================
db.exec(`
CREATE TABLE IF NOT EXISTS qr_codes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  type TEXT NOT NULL,
  content TEXT NOT NULL,
  qr_image_url TEXT,
  customization TEXT,
  title TEXT,
  description TEXT,
  created_at TEXT DEFAULT (datetime('now')),
  updated_at TEXT DEFAULT (datetime('now')),
  is_active INTEGER DEFAULT 1,
  download_count INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_qr_codes_type ON qr_codes(type);
CREATE INDEX IF NOT EXISTS idx_qr_codes_active ON qr_codes(is_active);
CREATE INDEX IF NOT EXISTS idx_qr_codes_created_at ON qr_codes(created_at);
`);

console.log('✅ QR Codes tablosu oluşturuldu');

// ============================================
// 🛒 SALES TABLE
// ============================================
db.exec(`
CREATE TABLE IF NOT EXISTS sales (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sale_date TEXT DEFAULT (datetime('now')),
  total_amount REAL NOT NULL,
  payment_method TEXT NOT NULL CHECK(payment_method IN ('cash', 'credit_card', 'debit_card')),
  customer_info TEXT,
  notes TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_sales_sale_date ON sales(sale_date);
CREATE INDEX IF NOT EXISTS idx_sales_payment_method ON sales(payment_method);
`);

console.log('✅ Sales tablosu oluşturuldu');

// ============================================
// 🛒 SALE ITEMS TABLE
// ============================================
db.exec(`
CREATE TABLE IF NOT EXISTS sale_items (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sale_id INTEGER NOT NULL,
  product_id INTEGER,
  product_name TEXT NOT NULL,
  quantity INTEGER NOT NULL DEFAULT 1,
  unit_price REAL NOT NULL,
  total_price REAL NOT NULL,
  created_at TEXT DEFAULT (datetime('now')),
  FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_sale_items_sale_id ON sale_items(sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_product_id ON sale_items(product_id);
`);

console.log('✅ Sale Items tablosu oluşturuldu');

// ============================================
// 📱 SCAN HISTORY TABLE
// ============================================
db.exec(`
CREATE TABLE IF NOT EXISTS scan_history (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  qr_content TEXT NOT NULL,
  qr_type TEXT,
  product_id INTEGER,
  scan_result TEXT,
  scanned_at TEXT DEFAULT (datetime('now')),
  was_successful INTEGER DEFAULT 1,
  error_message TEXT,
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_scan_history_scanned_at ON scan_history(scanned_at);
CREATE INDEX IF NOT EXISTS idx_scan_history_product_id ON scan_history(product_id);
CREATE INDEX IF NOT EXISTS idx_scan_history_successful ON scan_history(was_successful);
`);

console.log('✅ Scan History tablosu oluşturuldu');

// ============================================
// 📊 CATEGORIES TABLE
// ============================================
db.exec(`
CREATE TABLE IF NOT EXISTS categories (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE,
  description TEXT,
  icon TEXT,
  color TEXT DEFAULT '#3B82F6',
  created_at TEXT DEFAULT (datetime('now')),
  is_active INTEGER DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_categories_name ON categories(name);
CREATE INDEX IF NOT EXISTS idx_categories_active ON categories(is_active);
`);

console.log('✅ Categories tablosu oluşturuldu');

// ============================================
// 🌱 SEED DEFAULT CATEGORIES
// ============================================
const insertCategory = db.prepare(`
  INSERT OR IGNORE INTO categories (name, description, icon, color) 
  VALUES (?, ?, ?, ?)
`);

const categories = [
  ['Elektronik', 'Elektronik ürünler', '📱', '#3B82F6'],
  ['Giyim', 'Giyim ve aksesuar', '👕', '#10B981'],
  ['Yiyecek', 'Gıda ürünleri', '🍎', '#F59E0B'],
  ['İçecek', 'İçecek ürünleri', '🥤', '#06B6D4'],
  ['Kitap', 'Kitap ve dergi', '📚', '#8B5CF6'],
  ['Oyuncak', 'Oyuncak ve hobi', '🧸', '#F97316'],
  ['Ev & Yaşam', 'Ev eşyaları', '🏠', '#EC4899'],
  ['Spor', 'Spor malzemeleri', '⚽', '#EF4444'],
  ['Sağlık', 'Sağlık ürünleri', '💊', '#22C55E'],
  ['Diğer', 'Diğer ürünler', '📦', '#6B7280']
];

const insertCategories = db.transaction((cats) => {
  for (const cat of cats) {
    insertCategory.run(...cat);
  }
});

insertCategories(categories);
console.log('✅ 10 kategori eklendi');

// ============================================
// 🌱 SEED SAMPLE PRODUCTS
// ============================================
const insertProduct = db.prepare(`
  INSERT OR IGNORE INTO products (name, description, category, price, stock_quantity, barcode) 
  VALUES (?, ?, ?, ?, ?, ?)
`);

const products = [
  ['iPhone 15 Pro', 'Apple iPhone 15 Pro 256GB', 'Elektronik', 45999.99, 10, '8901030896934'],
  ['Samsung Galaxy S24', 'Samsung Galaxy S24 Ultra', 'Elektronik', 42999.99, 8, '8901030896935'],
  ['Nike Air Max', 'Nike Air Max 270 Spor Ayakkabı', 'Spor', 899.99, 25, '8901030896936'],
  ['Adidas Tişört', 'Adidas Originals Tişört', 'Giyim', 299.99, 50, '8901030896937'],
  ['Coca Cola', 'Coca Cola 330ml', 'İçecek', 15.99, 100, '8901030896938']
];

const insertProducts = db.transaction((prods) => {
  for (const prod of prods) {
    insertProduct.run(...prod);
  }
});

insertProducts(products);
console.log('✅ 5 örnek ürün eklendi');

// ============================================
// 🎯 TRIGGERS FOR updated_at
// ============================================
db.exec(`
-- Products trigger
CREATE TRIGGER IF NOT EXISTS update_products_timestamp 
AFTER UPDATE ON products
BEGIN
  UPDATE products SET updated_at = datetime('now') WHERE id = NEW.id;
END;

-- QR Codes trigger
CREATE TRIGGER IF NOT EXISTS update_qr_codes_timestamp 
AFTER UPDATE ON qr_codes
BEGIN
  UPDATE qr_codes SET updated_at = datetime('now') WHERE id = NEW.id;
END;
`);

console.log('✅ Update triggers oluşturuldu');

// ============================================
// 📊 FINAL STATS
// ============================================
const stats = {
  products: db.prepare('SELECT COUNT(*) as count FROM products').get().count,
  categories: db.prepare('SELECT COUNT(*) as count FROM categories').get().count,
  qr_codes: db.prepare('SELECT COUNT(*) as count FROM qr_codes').get().count,
  sales: db.prepare('SELECT COUNT(*) as count FROM sales').get().count
};

console.log(`
✅ QRMaster SQLite veritabanı başarıyla oluşturuldu!

📊 Oluşturulan tablolar:
 ✓ products (ürünler) - ${stats.products} kayıt
 ✓ qr_codes (QR kodları) - ${stats.qr_codes} kayıt
 ✓ sales (satışlar) - ${stats.sales} kayıt
 ✓ sale_items (satış kalemleri)
 ✓ scan_history (tarama geçmişi)
 ✓ categories (kategoriler) - ${stats.categories} kayıt

🌱 Örnek veriler eklendi:
 ✓ 10 kategori
 ✓ 5 örnek ürün

📁 Database: ${DB_PATH}
📊 Size: ${(fs.statSync(DB_PATH).size / 1024).toFixed(2)} KB
🎉 Hazır!
`);

db.close();
