const Database = require('better-sqlite3');
const path = require('path');
const fs = require('fs');

// ============================================
// 🗄️ SQLITE DATABASE CONNECTION
// ============================================

// Database dosya yolu
const DB_PATH = process.env.DB_PATH || path.join(__dirname, '..', 'data', 'qrmaster.db');

// Data klasörünü oluştur
const dataDir = path.dirname(DB_PATH);
if (!fs.existsSync(dataDir)) {
  fs.mkdirSync(dataDir, { recursive: true });
}

// SQLite veritabanı bağlantısı oluştur
const db = new Database(DB_PATH, {
  verbose: process.env.NODE_ENV === 'development' ? console.log : null,
  fileMustExist: false
});

// WAL mode'u etkinleştir (daha iyi performans)
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

// ============================================
// 🔧 HELPER FUNCTIONS
// ============================================

/**
 * Query executor - MySQL benzeri kullanım için
 * @param {string} sql - SQL sorgusu
 * @param {array} params - Parametreler
 * @returns {array} Sonuçlar
 */
function query(sql, params = []) {
  try {
    // SELECT sorgularında all() kullan
    if (sql.trim().toUpperCase().startsWith('SELECT')) {
      return db.prepare(sql).all(...params);
    }
    // INSERT/UPDATE/DELETE sorgularında run() kullan
    else {
      const result = db.prepare(sql).run(...params);
      return {
        insertId: result.lastInsertRowid,
        affectedRows: result.changes,
        changes: result.changes
      };
    }
  } catch (error) {
    console.error('❌ SQL Query Error:', error.message);
    console.error('SQL:', sql);
    console.error('Params:', params);
    throw error;
  }
}

/**
 * Tek satır döndüren query
 * @param {string} sql - SQL sorgusu
 * @param {array} params - Parametreler
 * @returns {object|null} Tek satır veya null
 */
function queryOne(sql, params = []) {
  try {
    return db.prepare(sql).get(...params) || null;
  } catch (error) {
    console.error('❌ SQL QueryOne Error:', error.message);
    console.error('SQL:', sql);
    console.error('Params:', params);
    throw error;
  }
}

/**
 * Transaction başlat
 */
function beginTransaction() {
  db.prepare('BEGIN TRANSACTION').run();
}

/**
 * Transaction commit
 */
function commit() {
  db.prepare('COMMIT').run();
}

/**
 * Transaction rollback
 */
function rollback() {
  db.prepare('ROLLBACK').run();
}

/**
 * Transaction wrapper
 * @param {function} callback - Transaction içinde çalışacak fonksiyon
 */
function transaction(callback) {
  const trx = db.transaction(callback);
  return trx;
}

// ============================================
// 📊 DATABASE INFO
// ============================================
console.log(`
✅ SQLite Database Connected
📁 Database File: ${DB_PATH}
📊 Database Size: ${(fs.statSync(DB_PATH).size / 1024).toFixed(2)} KB
🔧 WAL Mode: Enabled
🔐 Foreign Keys: Enabled
`);

// ============================================
// 🎯 EXPORT
// ============================================
module.exports = {
  db,
  query,
  queryOne,
  beginTransaction,
  commit,
  rollback,
  transaction,
  
  // MySQL uyumluluğu için
  getConnection: (callback) => {
    // SQLite connection pooling yok, direkt callback çağır
    callback(null, { release: () => {} });
  }
};
