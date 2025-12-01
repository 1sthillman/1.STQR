import { useEffect, memo } from 'react';

interface ToastProps {
  message: string;
  type?: 'success' | 'error' | 'warning' | 'info';
  duration?: number;
  onClose: () => void;
}

const Toast = memo(({ message, type = 'info', duration = 3000, onClose }: ToastProps) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose();
    }, duration);

    return () => clearTimeout(timer);
  }, [duration, onClose]);

  const icons = {
    success: 'ri-checkbox-circle-line',
    error: 'ri-close-circle-line',
    warning: 'ri-error-warning-line',
    info: 'ri-information-line',
  };

  const colors = {
    success: 'from-green-500 to-emerald-600 text-white',
    error: 'from-red-500 to-rose-600 text-white',
    warning: 'from-orange-500 to-amber-600 text-white',
    info: 'from-blue-500 to-cyan-600 text-white',
  };

  return (
    <div className="fixed top-20 left-1/2 transform -translate-x-1/2 z-[100] animate-slide-down pointer-events-auto">
      <div className={`bg-gradient-to-r ${colors[type]} px-6 py-4 rounded-2xl shadow-2xl min-w-[300px] max-w-[90vw] backdrop-blur-xl border-2 border-white/20`}>
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 bg-white/20 rounded-xl flex items-center justify-center flex-shrink-0">
            <i className={`${icons[type]} text-2xl`}></i>
          </div>
          <div className="flex-1 min-w-0">
            <p className="font-semibold text-base leading-tight break-words">{message}</p>
          </div>
          <button 
            onClick={onClose}
            className="w-8 h-8 bg-white/20 hover:bg-white/30 rounded-lg flex items-center justify-center transition-all flex-shrink-0"
          >
            <i className="ri-close-line text-lg"></i>
          </button>
        </div>
      </div>
    </div>
  );
});

Toast.displayName = 'Toast';

export default Toast;



























