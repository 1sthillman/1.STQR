import React from 'react';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  hover?: boolean;
  padding?: 'none' | 'sm' | 'md' | 'lg';
  shadow?: 'none' | 'sm' | 'md' | 'lg';
}

export const Card: React.FC<CardProps> = ({
  children,
  className = '',
  hover = false,
  padding = 'md',
  shadow = 'md',
}) => {
  const paddingStyles = {
    none: '',
    sm: 'p-4',
    md: 'p-6',
    lg: 'p-8',
  };

  const shadowStyles = {
    none: '',
    sm: 'shadow-sm',
    md: 'shadow-lg',
    lg: 'shadow-2xl',
  };

  return (
    <div
      className={`
        bg-white rounded-2xl border border-gray-100
        ${paddingStyles[padding]}
        ${shadowStyles[shadow]}
        ${hover ? 'hover:shadow-xl transition-shadow duration-200' : ''}
        ${className}
      `}
    >
      {children}
    </div>
  );
};









































