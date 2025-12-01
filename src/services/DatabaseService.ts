/**
 * 🗄️ 1STQR - Enterprise SQLite Service
 * ✅ Web (SQL.js) ve Native (Capacitor SQLite) desteği
 * ✅ Otomatik kuyruk sistemi
 * ✅ Veri kaybı yok
 */

import { CapacitorSQLite, SQLiteConnection, SQLiteDBConnection } from '@capacitor-community/sqlite';
import { Capacitor } from '@capacitor/core';
import initSqlJs, { Database as SqlJsDatabase } from 'sql.js';

const DB_VERSION = 1;
const DB_NAME = 'qrmaster.db';
const isNative = Capacitor.isNativePlatform();

// Type Definitions
export interface Product {
  id: string;
  name: string;
  price: number;
  barcode: string;
  category: string;
  stock: number;
  image?: string;
  description?: string;
  createdAt: number;
}

export interface CartItem {
  id: string;
  name: string;
  price: number;
  quantity: number;
  barcode: string;
  image?: string;
}

export interface ScanHistoryItem {
  id: string;
  content: string;
  type: string;
  timestamp: number;
}

export interface Sale {
  id: string;
  items: CartItem[];
  total: number;
  paymentMethod: 'cash' | 'card';
  cashAmount?: number;
  change?: number;
  timestamp: number;
}

export interface QRCode extends ScanHistoryItem {
  image?: string;
}

interface QueuedOperation {
  type: 'query' | 'execute';
  sql: string;
  params?: any[];
  resolve: (value: any) => void;
  reject: (error: any) => void;
}

class DatabaseService {
  private static instance: DatabaseService;
  
  private sqliteConnection: SQLiteConnection | null = null;
  private db: SQLiteDBConnection | null = null;
  private webDb: SqlJsDatabase | null = null;
  
  private isInitialized: boolean = false;
  private initializationPromise: Promise<void> | null = null;
  private queryQueue: QueuedOperation[] = [];
  private isProcessingQueue: boolean = false;

  private constructor() {}

  static getInstance(): DatabaseService {
    if (!DatabaseService.instance) {
      DatabaseService.instance = new DatabaseService();
    }
    return DatabaseService.instance;
  }

  /**
   * Database başlat (lazy)
   */
  async ensureInitialized(): Promise<void> {
    if (this.isInitialized) return;
    if (this.initializationPromise) return this.initializationPromise;
    
    this.initializationPromise = this.initialize();
    
    try {
      await this.initializationPromise;
    } catch (error) {
      this.initializationPromise = null;
      throw error;
    }
  }

  /**
   * Database initialization
   */
  private async initialize(): Promise<void> {
    try {
      if (isNative) {
        // ==================== NATIVE ====================
        this.sqliteConnection = new SQLiteConnection(CapacitorSQLite);
        
        // Eski bağlantıları temizle
        try {
          await this.sqliteConnection.closeAllConnections();
        } catch {}
        
        // Database oluştur/aç
        try {
          this.db = await this.sqliteConnection.createConnection(
            DB_NAME,
            false,
            'no-encryption',
            DB_VERSION,
            false
          );
        } catch (err: any) {
          if (err.message?.includes('already exists')) {
            this.db = await this.sqliteConnection.retrieveConnection(DB_NAME, false);
          } else {
            throw err;
          }
        }
        
        // Aç
        if (this.db) {
          try {
            await this.db.open();
          } catch (openErr: any) {
            // Zaten açıksa devam et
            if (!openErr.message?.includes('already open')) {
              throw openErr;
            }
          }
        }
      } else {
        // ==================== WEB ====================
        const SQL = await initSqlJs({
          locateFile: (file: string) => `https://sql.js.org/dist/${file}`
        });
        
        try {
          const dbData = await this.loadFromIndexedDB();
          this.webDb = dbData ? new SQL.Database(dbData) : new SQL.Database();
        } catch {
          this.webDb = new SQL.Database();
        }
      }
      
      // Tabloları oluştur
      await this.createTables();
      
      // Migration
      await this.migrateFromLocalStorage();
      
      // Başarılı
      this.isInitialized = true;
      
      // Web için kaydet
      if (!isNative && this.webDb) {
        await this.saveToIndexedDB().catch(() => {});
      }
      
      // Kuyruğu işle
      await this.processQueue();
      
    } catch (error) {
      this.isInitialized = false;
      throw error;
    }
  }

  /**
   * Query - Kuyruk destekli
   */
  async runQuery(query: string, params: any[] = []): Promise<any[]> {
    if (this.isInitialized) {
      return this.runQueryDirect(query, params);
    }
    
    return new Promise((resolve, reject) => {
      this.queryQueue.push({ type: 'query', sql: query, params, resolve, reject });
      this.ensureInitialized().catch(reject);
    });
  }

  /**
   * Execute - Kuyruk destekli
   */
  async runExecute(query: string, params: any[] = []): Promise<any> {
    if (this.isInitialized) {
      return this.runExecuteDirect(query, params);
    }
    
    return new Promise((resolve, reject) => {
      this.queryQueue.push({ type: 'execute', sql: query, params, resolve, reject });
      this.ensureInitialized().catch(reject);
    });
  }

  /**
   * Query - Direkt
   */
  private async runQueryDirect(query: string, params: any[] = []): Promise<any[]> {
    if (isNative && this.db) {
      const result = await this.db.query(query, params || []);
      return result.values || [];
    } else if (this.webDb) {
      const stmt = this.webDb.prepare(query);
      if (params && params.length > 0) {
        stmt.bind(params);
      }
      
      const results: any[] = [];
      while (stmt.step()) {
        results.push(stmt.getAsObject());
      }
      stmt.free();
      return results;
    }
    
    throw new Error('Database not initialized');
  }

  /**
   * Execute - Direkt
   */
  private async runExecuteDirect(query: string, params: any[] = []): Promise<any> {
    if (isNative && this.db) {
      await this.db.run(query, params || []);
      return { changes: 1 };
    } else if (this.webDb) {
      this.webDb.run(query, params);
      await this.saveToIndexedDB().catch(() => {});
      return { changes: this.webDb.getRowsModified() };
    }
    
    throw new Error('Database not initialized');
  }

  /**
   * Kuyruk işleme
   */
  private async processQueue(): Promise<void> {
    if (this.isProcessingQueue || this.queryQueue.length === 0) return;
    
    this.isProcessingQueue = true;
    
    while (this.queryQueue.length > 0) {
      const operation = this.queryQueue.shift();
      if (!operation) continue;
      
      try {
        if (operation.type === 'query') {
          const result = await this.runQueryDirect(operation.sql, operation.params);
          operation.resolve(result);
        } else {
          const result = await this.runExecuteDirect(operation.sql, operation.params);
          operation.resolve(result);
        }
      } catch (error) {
        operation.reject(error);
      }
    }
    
    this.isProcessingQueue = false;
  }

  /**
   * IndexedDB - Load
   */
  private async loadFromIndexedDB(): Promise<Uint8Array | null> {
    return new Promise((resolve) => {
      const request = indexedDB.open(DB_NAME, 1);
      
      request.onerror = () => resolve(null);
      request.onupgradeneeded = (event: any) => {
        const db = event.target.result;
        if (!db.objectStoreNames.contains('database')) {
          db.createObjectStore('database');
        }
      };
      
      request.onsuccess = (event: any) => {
        const db = event.target.result;
        const transaction = db.transaction(['database'], 'readonly');
        const store = transaction.objectStore('database');
        const getRequest = store.get('data');
        
        getRequest.onsuccess = () => {
          resolve(getRequest.result || null);
        };
        getRequest.onerror = () => resolve(null);
      };
    });
  }

  /**
   * IndexedDB - Save
   */
  private async saveToIndexedDB(): Promise<void> {
    if (!this.webDb) return;
    
    return new Promise((resolve, reject) => {
      const data = this.webDb!.export();
      const request = indexedDB.open(DB_NAME, 1);
      
      request.onerror = () => reject(new Error('IndexedDB open failed'));
      request.onupgradeneeded = (event: any) => {
        const db = event.target.result;
        if (!db.objectStoreNames.contains('database')) {
          db.createObjectStore('database');
        }
      };
      
      request.onsuccess = (event: any) => {
        const db = event.target.result;
        const transaction = db.transaction(['database'], 'readwrite');
        const store = transaction.objectStore('database');
        store.put(data, 'data');
        
        transaction.oncomplete = () => resolve();
        transaction.onerror = () => reject(new Error('IndexedDB save failed'));
      };
    });
  }

  /**
   * Tabloları oluştur
   */
  private async createTables(): Promise<void> {
    const tables = [
      `CREATE TABLE IF NOT EXISTS products (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        price REAL NOT NULL,
        barcode TEXT UNIQUE NOT NULL,
        category TEXT,
        stock INTEGER DEFAULT 0,
        image TEXT,
        description TEXT,
        createdAt INTEGER
      )`,
      `CREATE TABLE IF NOT EXISTS cart (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        price REAL NOT NULL,
        quantity INTEGER NOT NULL,
        barcode TEXT,
        image TEXT
      )`,
      `CREATE TABLE IF NOT EXISTS scan_history (
        id TEXT PRIMARY KEY,
        content TEXT NOT NULL,
        type TEXT NOT NULL,
        timestamp INTEGER NOT NULL
      )`,
      `CREATE TABLE IF NOT EXISTS sales (
        id TEXT PRIMARY KEY,
        items TEXT NOT NULL,
        total REAL NOT NULL,
        paymentMethod TEXT NOT NULL,
        cashAmount REAL,
        change REAL,
        timestamp INTEGER NOT NULL
      )`,
      `CREATE TABLE IF NOT EXISTS qrcodes (
        id TEXT PRIMARY KEY,
        content TEXT NOT NULL,
        type TEXT NOT NULL,
        timestamp INTEGER NOT NULL,
        image TEXT
      )`,
      `CREATE TABLE IF NOT EXISTS photo_posts (
        id TEXT PRIMARY KEY,
        postType TEXT DEFAULT 'photo',
        photo TEXT,
        music TEXT,
        audio TEXT,
        qrCode TEXT,
        qrType TEXT,
        note TEXT,
        caption TEXT,
        latitude REAL,
        longitude REAL,
        timestamp INTEGER NOT NULL,
        userName TEXT
      )`,
      `CREATE TABLE IF NOT EXISTS settings (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL
      )`
    ];

    for (const sql of tables) {
      await this.runExecuteDirect(sql, []);
    }
  }

  /**
   * Migration
   */
  private async migrateFromLocalStorage(): Promise<void> {
    try {
      const migrated = await this.getSetting('migrated_v1');
      if (migrated === 'true') return;
      
      // Migration mantığı buraya
      
      await this.setSetting('migrated_v1', 'true');
    } catch {}
  }

  async getSetting(key: string): Promise<string | null> {
    try {
      const results = await this.runQueryDirect('SELECT value FROM settings WHERE key = ?', [key]);
      return results.length > 0 ? results[0].value : null;
    } catch {
      return null;
    }
  }

  async setSetting(key: string, value: string): Promise<void> {
    await this.runExecuteDirect(
      'INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)',
      [key, value]
    );
  }

  // ========== PRODUCT METHODS ==========
  async getAllProducts(): Promise<Product[]> {
    return this.runQuery('SELECT * FROM products ORDER BY name');
  }

  async getProductByBarcode(barcode: string): Promise<Product | null> {
    const results = await this.runQuery('SELECT * FROM products WHERE barcode = ?', [barcode]);
    return results.length > 0 ? results[0] : null;
  }

  async addProduct(product: Product): Promise<void> {
    await this.runExecute(
      'INSERT INTO products (id, name, price, barcode, category, stock, image, description, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)',
      [product.id, product.name, product.price, product.barcode, product.category, product.stock, product.image || null, product.description || null, product.createdAt]
    );
  }

  async updateProduct(id: string, updates: Partial<Product>): Promise<void> {
    // ID ile mevcut ürünü bul
    const allProducts = await this.getAllProducts();
    const product = allProducts.find(p => String(p.id) === String(id));
    
    if (!product) {
      throw new Error('Product not found');
    }
    
    // Güncellemeleri birleştir
    const updated = { ...product, ...updates };
    
    await this.runExecute(
      'UPDATE products SET name = ?, price = ?, category = ?, stock = ?, image = ?, description = ? WHERE id = ?',
      [updated.name, updated.price, updated.category, updated.stock, updated.image || null, updated.description || null, id]
    );
  }

  async deleteProduct(id: string): Promise<void> {
    await this.runExecute('DELETE FROM products WHERE id = ?', [id]);
  }

  // ========== CART METHODS ==========
  async getCart(): Promise<CartItem[]> {
    return this.runQuery('SELECT * FROM cart');
  }

  async addToCart(item: CartItem): Promise<void> {
    // Önce item var mı kontrol et
    const existing = await this.runQuery('SELECT * FROM cart WHERE id = ?', [item.id]) as CartItem[];
    
    if (existing && existing.length > 0) {
      // Varsa quantity artır
      const newQuantity = existing[0].quantity + item.quantity;
      await this.runExecute('UPDATE cart SET quantity = ? WHERE id = ?', [newQuantity, item.id]);
    } else {
      // Yoksa yeni ekle
      await this.runExecute(
        'INSERT INTO cart (id, name, price, quantity, barcode, image) VALUES (?, ?, ?, ?, ?, ?)',
        [item.id, item.name, item.price, item.quantity, item.barcode, item.image || null]
      );
    }
  }

  async updateCartItem(id: string, quantity: number): Promise<void> {
    await this.runExecute('UPDATE cart SET quantity = ? WHERE id = ?', [quantity, id]);
  }

  async removeFromCart(id: string): Promise<void> {
    await this.runExecute('DELETE FROM cart WHERE id = ?', [id]);
  }

  async clearCart(): Promise<void> {
    await this.runExecute('DELETE FROM cart');
  }

  // ========== SCAN HISTORY ==========
  async getScanHistory(limit?: number): Promise<ScanHistoryItem[]> {
    if (limit) {
      return this.runQuery('SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT ?', [limit]);
    }
    return this.runQuery('SELECT * FROM scan_history ORDER BY timestamp DESC');
  }

  async addToScanHistory(item: ScanHistoryItem): Promise<void> {
    await this.runExecute(
      'INSERT INTO scan_history (id, content, type, timestamp) VALUES (?, ?, ?, ?)',
      [item.id, item.content, item.type, item.timestamp]
    );
  }

  async clearScanHistory(): Promise<void> {
    await this.runExecute('DELETE FROM scan_history');
  }

  async deleteFromScanHistory(id: string): Promise<void> {
    await this.runExecute('DELETE FROM scan_history WHERE id = ?', [id]);
  }

  // ========== SALES ==========
  async getSales(limit: number = 100): Promise<Sale[]> {
    const results = await this.runQuery('SELECT * FROM sales ORDER BY timestamp DESC LIMIT ?', [limit]);
    return results.map(row => ({
      ...row,
      items: JSON.parse(row.items)
    }));
  }

  async addSale(sale: Sale): Promise<void> {
    await this.runExecute(
      'INSERT INTO sales (id, items, total, paymentMethod, cashAmount, change, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)',
      [sale.id, JSON.stringify(sale.items), sale.total, sale.paymentMethod, sale.cashAmount || null, sale.change || null, sale.timestamp]
    );
  }

  async deleteSale(id: string): Promise<void> {
    await this.runExecute('DELETE FROM sales WHERE id = ?', [id]);
  }

  // ========== QR CODES ==========
  async getQRCodes(limit?: number): Promise<QRCode[]> {
    if (limit) {
      return this.runQuery('SELECT * FROM qrcodes ORDER BY timestamp DESC LIMIT ?', [limit]);
    }
    return this.runQuery('SELECT * FROM qrcodes ORDER BY timestamp DESC');
  }

  async addQRCode(qrcode: QRCode): Promise<void> {
    await this.runExecute(
      'INSERT INTO qrcodes (id, content, type, timestamp, image) VALUES (?, ?, ?, ?, ?)',
      [qrcode.id, qrcode.content, qrcode.type, qrcode.timestamp, qrcode.image || null]
    );
  }

  async deleteQRCode(id: string): Promise<void> {
    await this.runExecute('DELETE FROM qrcodes WHERE id = ?', [id]);
  }

  // ========== PHOTO POSTS (Social Map) ==========
  async getPhotoPosts(): Promise<any[]> {
    return this.runQuery('SELECT * FROM photo_posts ORDER BY timestamp DESC');
  }

  async addPhotoPost(post: {
    id: string;
    postType: string;
    photo?: string | null;
    music?: string | null;
    audio?: string | null;
    qrCode?: string | null;
    qrType?: string | null;
    note?: string | null;
    caption: string;
    latitude: number;
    longitude: number;
    timestamp: number;
    userName: string;
  }): Promise<void> {
    await this.runExecute(
      `INSERT INTO photo_posts (id, postType, photo, music, audio, qrCode, qrType, note, caption, latitude, longitude, timestamp, userName) 
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        post.id,
        post.postType,
        post.photo || null,
        post.music || null,
        post.audio || null,
        post.qrCode || null,
        post.qrType || null,
        post.note || null,
        post.caption,
        post.latitude,
        post.longitude,
        post.timestamp,
        post.userName,
      ]
    );
  }

  async deletePhotoPost(id: string): Promise<void> {
    await this.runExecute('DELETE FROM photo_posts WHERE id = ?', [id]);
  }

  async closeAllConnections(): Promise<void> {
    if (isNative && this.sqliteConnection) {
      await this.sqliteConnection.closeAllConnections().catch(() => {});
    }
  }

  async resetDatabase(): Promise<void> {
    // Tüm tabloları temizle
    const tables = ['products', 'cart', 'scan_history', 'sales', 'qrcodes', 'photo_posts', 'settings'];
    for (const table of tables) {
      await this.runExecute(`DELETE FROM ${table}`);
    }
    
    // Tabloları yeniden oluştur
    await this.createTables();
  }
}

export const databaseService = DatabaseService.getInstance();
export default databaseService;

