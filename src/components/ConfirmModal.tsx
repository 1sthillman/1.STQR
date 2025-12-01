import React from 'react';

interface ConfirmModalProps {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm: () => void;
  onCancel: () => void;
  type?: 'info' | 'warning' | 'danger' | 'success';
}

export default function ConfirmModal({
  title,
  message,
  confirmText = 'Evet',
  cancelText = 'Hayır',
  onConfirm,
  onCancel,
  type = 'info'
}: ConfirmModalProps) {
  
  const typeStyles = {
    info: {
      icon: 'ri-information-line',
      iconColor: 'text-blue-500',
      iconBg: 'bg-blue-100',
      confirmBg: 'bg-blue-600 hover:bg-blue-700'
    },
    warning: {
      icon: 'ri-error-warning-line',
      iconColor: 'text-yellow-500',
      iconBg: 'bg-yellow-100',
      confirmBg: 'bg-yellow-600 hover:bg-yellow-700'
    },
    danger: {
      icon: 'ri-alert-line',
      iconColor: 'text-red-500',
      iconBg: 'bg-red-100',
      confirmBg: 'bg-red-600 hover:bg-red-700'
    },
    success: {
      icon: 'ri-checkbox-circle-line',
      iconColor: 'text-green-500',
      iconBg: 'bg-green-100',
      confirmBg: 'bg-green-600 hover:bg-green-700'
    }
  };
  
  const style = typeStyles[type];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-fade-in">
      {/* Modal */}
      <div className="bg-white rounded-3xl shadow-2xl max-w-md w-full transform animate-slide-up">
        {/* Icon */}
        <div className="flex justify-center pt-8 pb-4">
          <div className={`${style.iconBg} w-20 h-20 rounded-full flex items-center justify-center`}>
            <i className={`${style.icon} ${style.iconColor} text-4xl`}></i>
          </div>
        </div>
        
        {/* Content */}
        <div className="px-8 pb-6 text-center">
          {title && (
            <h3 className="text-2xl font-bold text-gray-900 mb-3">
              {title}
            </h3>
          )}
          <p className="text-gray-600 text-lg leading-relaxed">
            {message}
          </p>
        </div>
        
        {/* Actions */}
        <div className="flex gap-3 px-6 pb-6">
          <button
            onClick={onCancel}
            className="flex-1 px-6 py-4 bg-gray-200 text-gray-800 rounded-2xl font-bold text-lg hover:bg-gray-300 transition-all"
          >
            {cancelText}
          </button>
          <button
            onClick={onConfirm}
            className={`flex-1 px-6 py-4 ${style.confirmBg} text-white rounded-2xl font-bold text-lg transition-all shadow-lg`}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}



































