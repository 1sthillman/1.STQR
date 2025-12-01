import React, { useRef } from 'react';

interface PremiumFileInputProps {
  label: string;
  accept?: string;
  onChange: (file: File | null) => void;
  icon?: string;
  variant?: 'primary' | 'secondary' | 'success' | 'warning';
  hasFile?: boolean;
  onClear?: () => void;
  gradientColors?: string;
}

export default function PremiumFileInput({
  label,
  accept = 'image/*',
  onChange,
  icon = '📁',
  variant = 'primary',
  hasFile = false,
  onClear,
  gradientColors
}: PremiumFileInputProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] || null;
    if (file) {
      onChange(file);
    }
  };

  const handleClear = () => {
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
    onChange(null);
    onClear?.();
  };

  const variantStyles = {
    primary: gradientColors || 'from-blue-500 to-purple-500',
    secondary: 'from-gray-500 to-gray-600',
    success: 'from-green-500 to-emerald-500',
    warning: 'from-yellow-500 to-orange-500',
  };

  return (
    <div className="relative">
      <input
        ref={fileInputRef}
        type="file"
        accept={accept}
        onChange={handleChange}
        className="hidden"
        id={`file-input-${label}`}
      />
      <label
        htmlFor={`file-input-${label}`}
        className={`
          relative cursor-pointer inline-flex items-center justify-center gap-2 px-6 py-3 
          bg-gradient-to-r ${variantStyles[variant]} text-white rounded-xl
          font-medium shadow-lg hover:shadow-xl transform hover:scale-105 
          transition-all duration-300 overflow-hidden group
          ${hasFile ? 'ring-2 ring-white ring-offset-2 ring-offset-gray-900' : ''}
        `}
      >
        <span className="text-xl">{icon}</span>
        <span className="text-sm">{label}</span>
        {hasFile && (
          <button
            type="button"
            onClick={(e) => {
              e.preventDefault();
              handleClear();
            }}
            className="ml-2 text-white/80 hover:text-white transition-colors"
          >
            ✕
          </button>
        )}
        
        {/* Shine Effect */}
        <div className="absolute inset-0 -translate-x-full group-hover:translate-x-full transition-transform duration-1000 bg-gradient-to-r from-transparent via-white/20 to-transparent" />
      </label>
    </div>
  );
}