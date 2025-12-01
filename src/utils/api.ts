// ============================================
// 🌐 API CLIENT UTILITIES
// ============================================

import { 
  API_BASE_URL, 
  API_CONFIG, 
  ApiResponse, 
  PaginatedResponse, 
  ApiError, 
  NetworkError, 
  ValidationError 
} from '../constants/api';

// ============================================
// 🔧 BASE API CLIENT
// ============================================

class ApiClient {
  private baseURL: string;
  private config: typeof API_CONFIG;

  constructor(baseURL: string = API_BASE_URL, config = API_CONFIG) {
    this.baseURL = baseURL;
    this.config = config;
  }

  /**
   * Make HTTP request
   */
  private async request<T = any>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseURL}${endpoint}`;
    
    const config: RequestInit = {
      ...options,
      headers: {
        ...this.config.headers,
        ...options.headers
      }
    };

    try {
      console.log(`🌐 API Request: ${config.method || 'GET'} ${url}`);
      
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), this.config.timeout);
      
      const response = await fetch(url, {
        ...config,
        signal: controller.signal
      });
      
      clearTimeout(timeoutId);
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        
        if (response.status === 400 && errorData.errors) {
          throw new ValidationError(errorData.message || 'Validasyon hatası', errorData.errors);
        }
        
        throw new ApiError(
          errorData.message || `HTTP ${response.status}: ${response.statusText}`,
          response.status,
          errorData
        );
      }

      const data = await response.json();
      console.log(`✅ API Success: ${endpoint}`, data);
      
      return data;
    } catch (error) {
      console.error(`❌ API Error: ${endpoint}`, error);
      
      if (error instanceof ApiError || error instanceof ValidationError) {
        throw error;
      }
      
      if (error instanceof TypeError || (error as any)?.name === 'AbortError') {
        throw new NetworkError('İnternet bağlantısını kontrol edin');
      }
      
      throw new ApiError('Beklenmeyen bir hata oluştu');
    }
  }

  /**
   * GET request
   */
  async get<T = any>(endpoint: string, params?: Record<string, any>): Promise<T> {
    const searchParams = params ? new URLSearchParams(
      Object.entries(params)
        .filter(([, value]) => value != null)
        .map(([key, value]) => [key, String(value)])
    ).toString() : '';
    
    const url = searchParams ? `${endpoint}?${searchParams}` : endpoint;
    
    return this.request<T>(url, {
      method: 'GET'
    });
  }

  /**
   * POST request
   */
  async post<T = any>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined
    });
  }

  /**
   * PUT request
   */
  async put<T = any>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined
    });
  }

  /**
   * DELETE request
   */
  async delete<T = any>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'DELETE'
    });
  }

  /**
   * Upload file
   */
  async upload<T = any>(endpoint: string, file: File, fieldName: string = 'file'): Promise<T> {
    const formData = new FormData();
    formData.append(fieldName, file);

    return this.request<T>(endpoint, {
      method: 'POST',
      headers: {
        // Don't set Content-Type for FormData - browser will set it with boundary
      },
      body: formData
    });
  }
}

// ============================================
// 🚀 SINGLETON API CLIENT
// ============================================

export const apiClient = new ApiClient();

// ============================================
// 🛠️ UTILITY FUNCTIONS
// ============================================

/**
 * Handle API response with error checking
 */
export function handleApiResponse<T>(response: ApiResponse<T>): T {
  if (!response.success) {
    throw new ApiError(response.message || 'API hatası', 500, response);
  }
  
  return response.data as T;
}

/**
 * Handle paginated API response
 */
export function handlePaginatedResponse<T>(response: PaginatedResponse<T>) {
  if (!response.success) {
    throw new ApiError(response.message || 'API hatası', 500, response);
  }
  
  return {
    data: response.data as T,
    pagination: response.pagination
  };
}

/**
 * Create query parameters from object
 */
export function createQueryParams(params: Record<string, any>): string {
  return new URLSearchParams(
    Object.entries(params)
      .filter(([, value]) => value != null && value !== '')
      .map(([key, value]) => [key, String(value)])
  ).toString();
}

/**
 * Check if backend is available
 */
export async function checkBackendHealth(): Promise<boolean> {
  try {
    const response = await apiClient.get('/health');
    return response.success === true;
  } catch (error) {
    console.error('❌ Backend health check failed:', error);
    return false;
  }
}

/**
 * Retry function with exponential backoff
 */
export async function retryWithBackoff<T>(
  fn: () => Promise<T>,
  maxRetries: number = 3,
  baseDelay: number = 1000
): Promise<T> {
  let lastError: Error;
  
  for (let i = 0; i <= maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error as Error;
      
      if (i === maxRetries) {
        throw lastError;
      }
      
      // Exponential backoff: 1s, 2s, 4s, 8s...
      const delay = baseDelay * Math.pow(2, i);
      console.log(`⏳ Retry ${i + 1}/${maxRetries} in ${delay}ms...`);
      
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }
  
  throw lastError!;
}

/**
 * Format error message for UI
 */
export function formatApiError(error: unknown): string {
  if (error instanceof ValidationError) {
    return error.errors?.map(e => e.msg).join(', ') || error.message;
  }
  
  if (error instanceof ApiError) {
    return error.message;
  }
  
  if (error instanceof NetworkError) {
    return 'İnternet bağlantınızı kontrol edin';
  }
  
  if (error instanceof Error) {
    return error.message;
  }
  
  return 'Bilinmeyen bir hata oluştu';
}

// ============================================
// 🔄 CACHE UTILITIES
// ============================================

interface CacheEntry<T> {
  data: T;
  timestamp: number;
  ttl: number;
}

class ApiCache {
  private cache = new Map<string, CacheEntry<any>>();

  set<T>(key: string, data: T, ttlMs: number = 300000): void { // 5 minutes default
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      ttl: ttlMs
    });
  }

  get<T>(key: string): T | null {
    const entry = this.cache.get(key);
    
    if (!entry) {
      return null;
    }
    
    if (Date.now() - entry.timestamp > entry.ttl) {
      this.cache.delete(key);
      return null;
    }
    
    return entry.data;
  }

  clear(): void {
    this.cache.clear();
  }

  delete(key: string): void {
    this.cache.delete(key);
  }
}

export const apiCache = new ApiCache();

/**
 * Cached API request
 */
export async function cachedApiRequest<T>(
  key: string,
  requestFn: () => Promise<T>,
  ttlMs: number = 300000
): Promise<T> {
  // Try to get from cache first
  const cached = apiCache.get<T>(key);
  if (cached) {
    console.log(`🚀 Cache hit: ${key}`);
    return cached;
  }
  
  // Make request and cache result
  console.log(`📡 Cache miss: ${key}`);
  const data = await requestFn();
  apiCache.set(key, data, ttlMs);
  
  return data;
}

export default apiClient;
