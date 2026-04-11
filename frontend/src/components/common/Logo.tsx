import React from 'react';
import { cn } from '@/lib/utils';

interface LogoProps {
  className?: string;
  size?: 'sm' | 'md' | 'lg';
}

/**
 * Vercel 风格极简 Logo
 */
export const Logo: React.FC<LogoProps> = ({ className, size = 'md' }) => {
  const sizeClasses = {
    sm: 'w-6 h-6',
    md: 'w-10 h-10',
    lg: 'w-14 h-14',
  };

  return (
    <div className={cn('relative flex items-center justify-center', sizeClasses[size], className)}>
      {/* Vercel 风格黑色三角 */}
      <svg
        viewBox="0 0 76 65"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="w-full h-full fill-foreground"
      >
        <path d="M37.5 0L75 65H0L37.5 0Z" />
      </svg>
    </div>
  );
};
