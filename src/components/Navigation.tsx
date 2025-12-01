import { Link, useLocation } from 'react-router-dom';
import { memo } from 'react';

const Navigation = memo(() => {
  const location = useLocation();

  const navItems = [
    { 
      path: '/', 
      label: 'Ana Sayfa',
      icon: (
        <svg viewBox="0 0 24 24" className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" strokeLinecap="round" strokeLinejoin="round"/>
          <polyline points="9 22 9 12 15 12 15 22" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      ),
      gradient: 'from-blue-500 to-cyan-500'
    },
    { 
      path: '/qr-olustur', 
      label: 'Oluştur',
      icon: (
        <svg viewBox="0 0 24 24" className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth="2">
          <rect x="3" y="3" width="7" height="7" strokeLinecap="round" strokeLinejoin="round"/>
          <rect x="14" y="3" width="7" height="7" strokeLinecap="round" strokeLinejoin="round"/>
          <rect x="14" y="14" width="7" height="7" strokeLinecap="round" strokeLinejoin="round"/>
          <rect x="3" y="14" width="7" height="7" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      ),
      gradient: 'from-purple-500 to-pink-500'
    },
    { 
      path: '/qr-tara', 
      label: 'Tara',
      icon: (
        <svg viewBox="0 0 24 24" className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2m0 10v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2" strokeLinecap="round" strokeLinejoin="round"/>
          <line x1="12" y1="8" x2="12" y2="16" strokeLinecap="round"/>
        </svg>
      ),
      gradient: 'from-green-500 to-emerald-500'
    },
    { 
      path: '/urun-yonetimi', 
      label: 'Ürünler',
      icon: (
        <svg viewBox="0 0 24 24" className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" strokeLinecap="round" strokeLinejoin="round"/>
          <polyline points="3.27 6.96 12 12.01 20.73 6.96" strokeLinecap="round" strokeLinejoin="round"/>
          <line x1="12" y1="22.08" x2="12" y2="12" strokeLinecap="round"/>
        </svg>
      ),
      gradient: 'from-orange-500 to-amber-500'
    },
    { 
      path: '/akilli-sepet', 
      label: 'Sepet',
      icon: (
        <svg viewBox="0 0 24 24" className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="9" cy="21" r="1" strokeLinecap="round" strokeLinejoin="round"/>
          <circle cx="20" cy="21" r="1" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      ),
      gradient: 'from-red-500 to-rose-500'
    },
    { 
      path: '/sosyal-harita', 
      label: 'Harita',
      icon: (
        <svg viewBox="0 0 24 24" className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" strokeLinecap="round" strokeLinejoin="round"/>
          <circle cx="12" cy="10" r="3" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M12 2a8 8 0 0 0-8 8c0 1.5.5 3 1 4" strokeLinecap="round" strokeLinejoin="round" opacity="0.5"/>
          <path d="M20 10c0 1.5-.5 3-1 4" strokeLinecap="round" strokeLinejoin="round" opacity="0.5"/>
        </svg>
      ),
      gradient: 'from-teal-500 to-cyan-500'
    },
  ];

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 will-change-transform safe-area-bottom">
      {/* Glass morphism background */}
      <div className="absolute inset-0 bg-white/90 backdrop-blur-xl border-t border-gray-200/50 shadow-2xl"></div>
      
      {/* Navigation items */}
      <div className="relative max-w-6xl mx-auto px-1 sm:px-4 py-1.5 sm:py-2">
        <div className="flex items-center justify-around gap-1">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path;
            
            return (
              <Link
                key={item.path}
                to={item.path}
                className="relative flex flex-col items-center py-1.5 px-1 sm:px-2 group will-change-transform flex-1 max-w-[80px] sm:max-w-none"
              >
                {/* Active indicator */}
                {isActive && (
                  <div className={`absolute -top-1 left-1/2 -translate-x-1/2 w-8 sm:w-12 h-0.5 sm:h-1 bg-gradient-to-r ${item.gradient} rounded-full`}></div>
                )}
                
                {/* Icon container */}
                <div className={`relative mb-0.5 sm:mb-1 transition-all duration-200 will-change-transform ${
                  isActive 
                    ? 'scale-100 sm:scale-110' 
                    : 'scale-90 sm:scale-100 group-active:scale-95'
                }`}>
                  {/* Gradient background for active state */}
                  {isActive && (
                    <div className={`absolute inset-0 bg-gradient-to-br ${item.gradient} opacity-20 rounded-lg blur-sm`}></div>
                  )}
                  
                  <div className={`relative p-1.5 sm:p-2 rounded-lg sm:rounded-xl transition-all duration-200 will-change-transform ${
                    isActive 
                      ? `bg-gradient-to-br ${item.gradient} text-white shadow-lg` 
                      : 'text-gray-500 group-active:text-gray-700'
                  }`}>
                    <div className="w-5 h-5 sm:w-6 sm:h-6">
                      {item.icon}
                    </div>
                  </div>
                </div>
                
                {/* Label */}
                <span className={`text-[10px] sm:text-xs font-medium transition-all duration-200 truncate max-w-full text-center ${
                  isActive 
                    ? 'text-gray-900 font-bold' 
                    : 'text-gray-600'
                }`}>
                  {item.label}
                </span>
                
                {/* Tap effect */}
                <div className={`absolute inset-0 bg-gradient-to-br ${item.gradient} opacity-0 group-active:opacity-10 rounded-xl transition-opacity duration-150`}></div>
              </Link>
            );
          })}
        </div>
      </div>
      
      {/* Safe area padding for iOS */}
      <div className="h-[env(safe-area-inset-bottom)]"></div>
    </div>
  );
});
Navigation.displayName = 'Navigation';

export default Navigation;








