import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  define: {
    __BASE_PATH__: JSON.stringify('/')
  },
  base: '/',
  server: {
    port: 3000,
    host: '0.0.0.0',
    proxy: {
      '/api/nominatim': {
        target: 'https://nominatim.openstreetmap.org',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/nominatim/, ''),
        headers: {
          'User-Agent': 'QRMaster-App/1.0'
        }
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    minify: 'esbuild', // ✅ esbuild - daha hızlı ve sorunsuz
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          qr: ['qrcode', 'jsqr', 'html5-qrcode'],
          db: ['@capacitor-community/sqlite']
        }
      }
    }
  },
  esbuild: {
    drop: ['console', 'debugger'], // ✅ Console.log'lar silinir - esbuild ile
  },
  optimizeDeps: {
    include: ['react', 'react-dom', 'react-router-dom']
  }
})


