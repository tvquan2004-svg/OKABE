import React, { useState } from 'react';
import { useUpdateBoardBackgroundMutation, useUpdateBoardMutation } from '../../services/boardApi';
import { FiUpload } from 'react-icons/fi';
import styles from './BackgroundPicker.module.css';

interface BackgroundPickerProps {
  boardId: number;
  currentBackground: string | null;
}

const PRESET_COLORS = [
  '#1e293b', '#334155', '#475569', // Slates
  '#1e3a8a', '#1d4ed8', '#2563eb', // Blues
  '#064e3b', '#047857', '#059669', // Greens
  '#7c2d12', '#9a3412', '#c2410c', // Oranges/Browns
  '#701a75', '#86198f', '#a21caf', // Purples
  '#4c0519', '#881337', '#be123c', // Reds
];

const PRESET_IMAGES = [
  { url: '/backgrounds/mau-background-dep-1.jpg',  label: 'Núi rừng 1' },
  { url: '/backgrounds/mau-background-dep-2.jpg',  label: 'Núi rừng 2' },
  { url: '/backgrounds/mau-background-dep-4.jpg',  label: 'Thiên nhiên 1' },
  { url: '/backgrounds/mau-background-dep-5.jpg',  label: 'Thiên nhiên 2' },
  { url: '/backgrounds/mau-background-dep-6.jpg',  label: 'Phong cảnh 1' },
  { url: '/backgrounds/mau-background-dep-7.jpg',  label: 'Phong cảnh 2' },
  { url: '/backgrounds/mau-background-dep-8.jpg',  label: 'Phong cảnh 3' },
  { url: '/backgrounds/mau-background-dep-9.jpg',  label: 'Phong cảnh 4' },
  { url: '/backgrounds/mau-background-dep-10.jpg', label: 'Phong cảnh 5' },
  { url: '/backgrounds/mau-background-dep-11.jpg', label: 'Phong cảnh 6' },
];

const BackgroundPicker: React.FC<BackgroundPickerProps> = ({ boardId, currentBackground }) => {
  const [activeTab, setActiveTab] = useState<'COLORS' | 'PHOTOS'>('COLORS');
  const [customHex, setCustomHex] = useState('');
  const [updateBackground, { isLoading: isUploading }] = useUpdateBoardBackgroundMutation();
  const [updateBoard, { isLoading: isSettingPreset }] = useUpdateBoardMutation();
  const isLoading = isUploading || isSettingPreset;

  const handleColorSelect = async (hex: string) => {
    try {
      await updateBackground({ id: boardId, type: 'COLOR', value: hex }).unwrap();
    } catch (err) {
      console.error('Failed to update background color', err);
    }
  };

  const handleImageSelect = async (url: string) => {
    try {
      // Use the standard board update endpoint (no type validation)
      // This works on both local and production backends
      await updateBoard({ id: boardId, body: { background: url } }).unwrap();
    } catch (err) {
      console.error('Failed to update background image', err);
    }
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      await updateBackground({ id: boardId, type: 'IMAGE', file }).unwrap();
    } catch (err: unknown) {
      console.error('Failed to upload background image', err);
      const error = err as { data?: { message?: string } };
      const message = error.data?.message || 'Failed to upload image. Please check your internet or file size.';
      alert(`Error: ${message}`);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.tabs}>
        <button 
          className={`${styles.tab} ${activeTab === 'COLORS' ? styles.activeTab : ''}`}
          onClick={() => setActiveTab('COLORS')}
        >
          Colors
        </button>
        <button 
          className={`${styles.tab} ${activeTab === 'PHOTOS' ? styles.activeTab : ''}`}
          onClick={() => setActiveTab('PHOTOS')}
        >
          Photos
        </button>
      </div>

      {activeTab === 'COLORS' ? (
        <>
          <div className={styles.grid}>
            {PRESET_COLORS.map(color => (
              <div 
                key={color}
                className={`${styles.colorItem} ${currentBackground === color ? styles.activeItem : ''}`}
                style={{ backgroundColor: color }}
                onClick={() => handleColorSelect(color)}
              />
            ))}
          </div>
          <div className={styles.customHex}>
            <input 
              type="text" 
              placeholder="#hexcolor" 
              value={customHex}
              onChange={(e) => setCustomHex(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && customHex.startsWith('#')) {
                  handleColorSelect(customHex);
                }
              }}
            />
          </div>
        </>
      ) : (
        <>
          <div className={styles.grid}>
            {PRESET_IMAGES.map(img => (
              <div 
                key={img.url}
                className={`${styles.imageItem} ${currentBackground === img.url ? styles.activeItem : ''}`}
                style={{ backgroundImage: `url(${img.url})` }}
                title={img.label}
                onClick={() => handleImageSelect(img.url)}
              />
            ))}
          </div>
          <label className={styles.uploadZone}>
            <FiUpload size={20} />
            <span>{isLoading ? 'Uploading...' : 'Upload from your computer'}</span>
            <input 
              type="file" 
              accept="image/*" 
              className={styles.hiddenInput}
              onChange={handleFileUpload}
              disabled={isLoading}
            />
          </label>
        </>
      )}
    </div>
  );
};

export default BackgroundPicker;
