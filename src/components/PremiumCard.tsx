import { ReactNode } from 'react';

interface PremiumCardProps {
  children: ReactNode;
  active?: boolean;
  onClick?: () => void;
  variant?: 'default' | 'primary' | 'secondary' | 'success' | 'warning' | 'danger';
  icon?: ReactNode;
  label?: string;
  className?: string;
}

const PremiumCard = ({
  children,
  active = false,
  onClick,
  variant = 'default',
  icon,
  label,
  className = '',
}: PremiumCardProps) => {
  const variants = {
    default: active 
      ? 'border-blue-500 bg-gradient-to-br from-blue-50 to-purple-50 text-blue-700 shadow-lg shadow-blue-500/30'
      : 'border-gray-200 bg-white text-gray-700 hover:border-blue-300 hover:shadow-md',
    primary: active
      ? 'border-blue-500 bg-gradient-to-br from-blue-500 to-purple-600 text-white shadow-lg shadow-blue-500/50'
      : 'border-blue-200 bg-gradient-to-br from-blue-50 to-purple-50 text-blue-700 hover:border-blue-400',
    secondary: active
      ? 'border-purple-500 bg-gradient-to-br from-purple-500 to-pink-600 text-white shadow-lg shadow-purple-500/50'
      : 'border-purple-200 bg-gradient-to-br from-purple-50 to-pink-50 text-purple-700 hover:border-purple-400',
    success: active
      ? 'border-green-500 bg-gradient-to-br from-green-500 to-emerald-600 text-white shadow-lg shadow-green-500/50'
      : 'border-green-200 bg-gradient-to-br from-green-50 to-emerald-50 text-green-700 hover:border-green-400',
    warning: active
      ? 'border-orange-500 bg-gradient-to-br from-orange-500 to-amber-600 text-white shadow-lg shadow-orange-500/50'
      : 'border-orange-200 bg-gradient-to-br from-orange-50 to-amber-50 text-orange-700 hover:border-orange-400',
    danger: active
      ? 'border-red-500 bg-gradient-to-br from-red-500 to-rose-600 text-white shadow-lg shadow-red-500/50'
      : 'border-red-200 bg-gradient-to-br from-red-50 to-rose-50 text-red-700 hover:border-red-400',
  };

  return (
    <button
      onClick={onClick}
      className={`
        relative overflow-hidden
        border-2 rounded-xl p-4
        ${variants[variant]}
        transition-all duration-300
        hover:scale-105 active:scale-95
        ${active ? 'scale-105' : ''}
        ${className}
      `}
    >
      {/* Shine effect */}
      <span className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent -translate-x-full hover:translate-x-full transition-transform duration-700"></span>
      
      {/* Content */}
      <div className="relative flex flex-col items-center space-y-2">
        {icon && (
          <div className={`text-3xl ${active ? 'animate-bounce-soft' : ''}`}>
            {icon}
          </div>
        )}
        {label && (
          <div className={`text-sm font-semibold ${active ? 'font-bold' : ''}`}>
            {label}
          </div>
        )}
        {children}
      </div>
    </button>
  );
};

export default PremiumCard;








































