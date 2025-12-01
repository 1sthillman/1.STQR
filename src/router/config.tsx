import { RouteObject } from 'react-router-dom';
import { lazy, Suspense } from 'react';

// Loading component
const PageLoader = () => (
  <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 flex items-center justify-center">
    <div className="text-center">
      <div className="animate-spin w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full mx-auto mb-4"></div>
      <p className="text-gray-600 font-medium">Sayfa yükleniyor...</p>
    </div>
  </div>
);

// Lazy load components
const Home = lazy(() => import('../pages/home/page'));
const QRCreate = lazy(() => import('../pages/qr-create/page'));
const QRScan = lazy(() => import('../pages/qr-scan/page'));
const ProductManagement = lazy(() => import('../pages/product-management/page'));
const SmartCart = lazy(() => import('../pages/smart-cart/page'));
const LocationQR = lazy(() => import('../pages/location-qr/page'));
const SocialMap = lazy(() => import('../pages/social-map/page'));
const FloatingQR = lazy(() => import('../pages/floating-qr/page'));
const FloatingOCR = lazy(() => import('../pages/floating-ocr/page'));
const AutoClicker = lazy(() => import('../pages/auto-clicker/page'));
const NotFound = lazy(() => import('../pages/NotFound'));

// Route configuration
const routes: RouteObject[] = [
  {
    path: '/',
    element: (
      <Suspense fallback={<PageLoader />}>
        <Home />
      </Suspense>
    ),
  },
  {
    path: '/qr-olustur',
    element: (
      <Suspense fallback={<PageLoader />}>
        <QRCreate />
      </Suspense>
    ),
  },
  {
    path: '/qr-tara',
    element: (
      <Suspense fallback={<PageLoader />}>
        <QRScan />
      </Suspense>
    ),
  },
  {
    path: '/urun-yonetimi',
    element: (
      <Suspense fallback={<PageLoader />}>
        <ProductManagement />
      </Suspense>
    ),
  },
  {
    path: '/urunler',
    element: (
      <Suspense fallback={<PageLoader />}>
        <ProductManagement />
      </Suspense>
    ),
  },
  {
    path: '/akilli-sepet',
    element: (
      <Suspense fallback={<PageLoader />}>
        <SmartCart />
      </Suspense>
    ),
  },
  {
    path: '/konum-qr',
    element: (
      <Suspense fallback={<PageLoader />}>
        <LocationQR />
      </Suspense>
    ),
  },
  {
    path: '/sosyal-harita',
    element: (
      <Suspense fallback={<PageLoader />}>
        <SocialMap />
      </Suspense>
    ),
  },
  {
    path: '/floating-qr',
    element: (
      <Suspense fallback={<PageLoader />}>
        <FloatingQR />
      </Suspense>
    ),
  },
  {
    path: '/floating-ocr',
    element: (
      <Suspense fallback={<PageLoader />}>
        <FloatingOCR />
      </Suspense>
    ),
  },
  {
    path: '/auto-clicker',
    element: (
      <Suspense fallback={<PageLoader />}>
        <AutoClicker />
      </Suspense>
    ),
  },
  {
    path: '*',
    element: (
      <Suspense fallback={<PageLoader />}>
        <NotFound />
      </Suspense>
    ),
  },
];

export default routes;




