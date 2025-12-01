/**
 * Production-safe logger
 * Sadece development'da log basar
 */

const isDevelopment = import.meta.env.DEV;

export const logger = {
  log: (...args: any[]) => {
    if (isDevelopment) {
      console.log(...args);
    }
  },
  error: (...args: any[]) => {
    if (isDevelopment) {
      console.error(...args);
    }
  },
  warn: (...args: any[]) => {
    if (isDevelopment) {
      console.warn(...args);
    }
  }
};

// Production'da tüm logları devre dışı bırak
if (!isDevelopment) {
  console.log = () => {};
  console.warn = () => {};
  // console.error'ı bırakıyoruz - kritik hatalar için
}

