/**
 * @deprecated Bu servis artık kullanılmıyor.
 * Bunun yerine `useSales` hook'unu kullanın (src/hooks/useDatabase.ts)
 * 
 * Tüm veri işlemleri artık SQLite üzerinden yapılmaktadır.
 * LocalStorage kullanımı kaldırılmıştır.
 */

import { Sale, CartItem, PaymentMethod } from '../types';
import { storage } from '../utils/storage';
import { STORAGE_KEYS } from '../constants';

class SalesService {
  /**
   * @deprecated useSales hook kullanın
   */
  getAll(): Sale[] {
    console.warn('SalesService.getAll() deprecated! useSales() hook kullanın.');
    return storage.getItem<Sale[]>(STORAGE_KEYS.SALES_HISTORY, []);
  }

  /**
   * Get sale by ID
   */
  getById(id: number): Sale | undefined {
    const sales = this.getAll();
    return sales.find((s) => s.id === id);
  }

  /**
   * Create sale
   */
  create(items: CartItem[], paymentMethod: PaymentMethod): boolean {
    const total = items.reduce((sum, item) => sum + item.price * item.quantity, 0);
    
    const sale: Sale = {
      id: Date.now(),
      items,
      total,
      paymentMethod,
      date: new Date().toISOString(),
      status: 'completed',
    };

    const sales = this.getAll();
    sales.unshift(sale);
    // Keep only last 500 sales
    return storage.setItem(STORAGE_KEYS.SALES_HISTORY, sales.slice(0, 500));
  }

  /**
   * Get sales by date range
   */
  getByDateRange(startDate: Date, endDate: Date): Sale[] {
    const sales = this.getAll();
    return sales.filter((s) => {
      const saleDate = new Date(s.date);
      return saleDate >= startDate && saleDate <= endDate;
    });
  }

  /**
   * Get today's sales
   */
  getToday(): Sale[] {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);
    
    return this.getByDateRange(today, tomorrow);
  }

  /**
   * Get total revenue
   */
  getTotalRevenue(): number {
    const sales = this.getAll();
    return sales.reduce((total, s) => total + s.total, 0);
  }

  /**
   * Get today's revenue
   */
  getTodayRevenue(): number {
    const todaySales = this.getToday();
    return todaySales.reduce((total, s) => total + s.total, 0);
  }

  /**
   * Get sales count
   */
  getCount(): number {
    return this.getAll().length;
  }

  /**
   * Get today's count
   */
  getTodayCount(): number {
    return this.getToday().length;
  }

  /**
   * Refund sale
   */
  refund(id: number): boolean {
    const sales = this.getAll();
    const index = sales.findIndex((s) => s.id === id);
    
    if (index === -1) return false;
    
    sales[index].status = 'refunded';
    return storage.setItem(STORAGE_KEYS.SALES_HISTORY, sales);
  }

  /**
   * Clear all sales
   */
  clearAll(): boolean {
    return storage.setItem(STORAGE_KEYS.SALES_HISTORY, []);
  }

  /**
   * Export sales
   */
  export(): Sale[] {
    return this.getAll();
  }
}

export const salesService = new SalesService();
export default salesService;






