import React, { useState } from 'react';
import { getFullFileUrl } from '../../utils/urlHelper';

interface UserAvatarProps {
  avatarUrl?: string | null;
  username: string;
  size?: number;
  className?: string;
}

const COLORS = [
  '#6366f1', '#8b5cf6', '#ec4899', '#f43f5e',
  '#f97316', '#eab308', '#22c55e', '#14b8a6',
  '#06b6d4', '#3b82f6',
];

function getColor(name: string): string {
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return COLORS[Math.abs(hash) % COLORS.length] || '#6366f1';
}

export const UserAvatar: React.FC<UserAvatarProps> = ({
  avatarUrl,
  username,
  size = 32,
  className,
}) => {
  const [imgError, setImgError] = useState(false);
  const src = getFullFileUrl(avatarUrl ?? undefined);
  const showImg = src && !imgError;

  return (
    <div
      className={className}
      style={{
        width: size,
        height: size,
        borderRadius: '50%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: size * 0.42,
        fontWeight: 600,
        color: '#fff',
        background: showImg ? undefined : getColor(username),
        overflow: 'hidden',
        flexShrink: 0,
        lineHeight: 1,
      }}
      title={username}
    >
      {showImg ? (
        <img
          src={src}
          alt={username}
          onError={() => setImgError(true)}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
          }}
        />
      ) : (
        username?.charAt(0).toUpperCase() || '?'
      )}
    </div>
  );
};
