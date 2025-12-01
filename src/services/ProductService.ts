/**
 * @deprecated Bu servis artık kullanılmıyor. 
 * Bunun yerine `useProducts` hook'unu kullanın (src/hooks/useDatabase.ts)
 * 
 * Tüm veri işlemleri artık SQLite üzerinden yapılmaktadır.
 * LocalStorage kullanımı kaldırılmıştır.
 */

import { Product } from '../types';
import { storage } from '../utils/storage';
import { STORAGE_KEYS } from '../constants';

class ProductService {
  /**
   * @deprecated useProducts hook kullanın
   */
  getAll(): Product[] {
    console.warn('ProductService.getAll() deprecated! useProducts() hook kullanın.');
    return storage.getItem<Product[]>(STORAGE_KEYS.PRODUCTS, []);
  }

  /**
   * Get product by ID
   */
  getById(id: number): Product | undefined {
    const products = this.getAll();
    return products.find((p) => p.id === id);
  }

  /**
   * Get product by barcode
   */
  getByBarcode(barcode: string): Product | undefined {
    const products = this.getAll();
    return products.find((p) => p.barcode === barcode);
  }

  /**
   * Create product
   */
  create(product: Product): boolean {
    const products = this.getAll();
    products.push(product);
    return storage.setItem(STORAGE_KEYS.PRODUCTS, products);
  }

  /**
   * Update product
   */
  update(id: number, updates: Partial<Product>): boolean {
    const products = this.getAll();
    const index = products.findIndex((p) => p.id === id);
    
    if (index === -1) return false;
    
    products[index] = { ...products[index], ...updates, updatedAt: new Date().toISOString() };
    return storage.setItem(STORAGE_KEYS.PRODUCTS, products);
  }

  /**
   * Delete product
   */
  delete(id: number): boolean {
    const products = this.getAll();
    const filtered = products.filter((p) => p.id !== id);
    return storage.setItem(STORAGE_KEYS.PRODUCTS, filtered);
  }

  /**
   * Search products
   */
  search(query: string): Product[] {
    const products = this.getAll();
    const lowerQuery = query.toLowerCase();
    
    return products.filter(
      (p) =>
        p.name.toLowerCase().includes(lowerQuery) ||
        p.barcode.includes(query) ||
        p.description.toLowerCase().includes(lowerQuery)
    );
  }

  /**
   * Filter by category
   */
  filterByCategory(category: string): Product[] {
    const products = this.getAll();
    return products.filter((p) => p.category === category);
  }

  /**
   * Get low stock products
   */
  getLowStock(threshold: number = 10): Product[] {
    const products = this.getAll();
    return products.filter((p) => p.stock <= threshold);
  }

  /**
   * Update stock
   */
  updateStock(id: number, quantity: number): boolean {
    const product = this.getById(id);
    if (!product) return false;
    
    return this.update(id, { stock: product.stock + quantity });
  }

  /**
   * Get total value
   */
  getTotalValue(): number {
    const products = this.getAll();
    return products.reduce((total, p) => total + p.price * p.stock, 0);
  }
}

export const productService = new ProductService();
export default productService;






