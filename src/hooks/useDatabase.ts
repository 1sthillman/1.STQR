/**
 * 🔌 Universal Database Hook - PERFORMANCE OPTIMIZED
 * ✅ useCallback ile tüm fonksiyonlar optimize edildi
 * ✅ Console.log'lar kaldırıldı - production-ready
 * ✅ Gereksiz re-render'lar engellendi
 */

import { useState, useEffect, useCallback } from 'react';
import { databaseService } from '../services';
import type { Product, CartItem, ScanHistoryItem, Sale } from '../services';

// ==================== PRODUCTS ====================

export const useProducts = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  const loadProducts = useCallback(async () => {
    try {
      const data = await databaseService.getAllProducts();
      setProducts(data);
    } catch (error) {
      // Silent - production
    } finally {
      setLoading(false);
    }
  }, []);

  const addProduct = useCallback(async (product: Product) => {
    await databaseService.addProduct(product);
    await loadProducts();
  }, [loadProducts]);

  const updateProduct = useCallback(async (id: string, updates: Partial<Product>) => {
    await databaseService.updateProduct(id, updates);
    await loadProducts();
  }, [loadProducts]);

  const deleteProduct = useCallback(async (id: string) => {
    await databaseService.deleteProduct(id);
    await loadProducts();
  }, [loadProducts]);

  const getProductByBarcode = useCallback(async (barcode: string): Promise<Product | null> => {
    try {
      return await databaseService.getProductByBarcode(barcode);
    } catch (error) {
      return null;
    }
  }, []);

  const getProductById = useCallback(async (id: string): Promise<Product | null> => {
    try {
      const allProducts = await databaseService.getAllProducts();
      const product = allProducts.find(p => 
        String(p.id) === String(id) || Number(p.id) === Number(id)
      );
      return product || null;
    } catch (error) {
      return null;
    }
  }, []);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  return {
    products,
    loading,
    addProduct,
    updateProduct,
    deleteProduct,
    getProductByBarcode,
    getProductById,
    reload: loadProducts
  };
};

// ==================== CART ====================

export const useCart = () => {
  const [cart, setCart] = useState<CartItem[]>([]);
  const [loading, setLoading] = useState(true);

  const loadCart = useCallback(async () => {
    try {
      const data = await databaseService.getCart();
      setCart(data);
    } catch (error) {
      // Silent
    } finally {
      setLoading(false);
    }
  }, []);

  const addToCart = useCallback(async (item: CartItem) => {
    await databaseService.addToCart(item);
    await loadCart();
  }, [loadCart]);

  const updateCartItem = useCallback(async (id: string, quantity: number) => {
    await databaseService.updateCartItem(id, quantity);
    await loadCart();
  }, [loadCart]);

  const removeFromCart = useCallback(async (id: string) => {
    await databaseService.removeFromCart(id);
    await loadCart();
  }, [loadCart]);

  const clearCart = useCallback(async () => {
    await databaseService.clearCart();
    await loadCart();
  }, [loadCart]);

  useEffect(() => {
    loadCart();
  }, [loadCart]);

  return {
    cart,
    loading,
    addToCart,
    updateCartItem,
    removeFromCart,
    clearCart,
    reload: loadCart
  };
};

// ==================== SCAN HISTORY ====================

export const useScanHistory = () => {
  const [history, setHistory] = useState<ScanHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);

  const loadHistory = useCallback(async () => {
    try {
      const data = await databaseService.getScanHistory(50);
      setHistory(data);
    } catch (error) {
      // Silent
    } finally {
      setLoading(false);
    }
  }, []);

  const addToHistory = useCallback(async (item: ScanHistoryItem) => {
    await databaseService.addToScanHistory(item);
    await loadHistory();
  }, [loadHistory]);

  const clearHistory = useCallback(async () => {
    await databaseService.clearScanHistory();
    await loadHistory();
  }, [loadHistory]);

  const deleteFromHistory = useCallback(async (id: string) => {
    await databaseService.deleteFromScanHistory(id);
    await loadHistory();
  }, [loadHistory]);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  return {
    history,
    loading,
    addToHistory,
    clearHistory,
    deleteFromHistory,
    reload: loadHistory
  };
};

// ==================== SALES ====================

export const useSales = () => {
  const [sales, setSales] = useState<Sale[]>([]);
  const [loading, setLoading] = useState(true);

  const loadSales = useCallback(async () => {
    try {
      const data = await databaseService.getSales(100);
      setSales(data);
    } catch (error) {
      // Silent
    } finally {
      setLoading(false);
    }
  }, []);

  const addSale = useCallback(async (sale: Sale) => {
    await databaseService.addSale(sale);
    await loadSales();
  }, [loadSales]);

  const deleteSale = useCallback(async (id: string) => {
    await databaseService.deleteSale(id);
    await loadSales();
  }, [loadSales]);

  // 📊 GÜNLÜK CİRO HESAPLA - useCallback ile optimize
  const getDailyRevenue = useCallback((): number => {
    const today = new Date().toLocaleDateString();
    return sales
      .filter(sale => new Date(sale.timestamp).toLocaleDateString() === today)
      .reduce((sum, sale) => sum + (sale.total || 0), 0);
  }, [sales]);

  useEffect(() => {
    loadSales();
  }, [loadSales]);

  return {
    sales,
    loading,
    addSale,
    deleteSale,
    getDailyRevenue,
    reload: loadSales
  };
};

export default {
  useProducts,
  useCart,
  useScanHistory,
  useSales
};
