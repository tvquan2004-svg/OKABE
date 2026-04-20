import React, { useState } from 'react';
import { useUpdateBoardBackgroundMutation } from '../../services/boardApi';
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
  'https://images.unsplash.com/photo-1477346611705-65d1883cee1e?auto=format&fit=crop&w=400&q=80',
  'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=400&q=80',
  'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=400&q=80',
  'https://images.unsplash.com/photo-1434725039720-abb26e22ebe8?auto=format&fit=crop&w=400&q=80',
  'https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=400&q=80',
  'https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=400&q=80',
];

const BackgroundPicker: React.FC<BackgroundPickerProps> = ({ boardId, currentBackground }) => {
  const [activeTab, setActiveTab] = useState<'COLORS' | 'PHOTOS'>('COLORS');
  const [customHex, setCustomHex] = useState('');
  const [updateBackground, { isLoading }] = useUpdateBoardBackgroundMutation();

  const handleColorSelect = async (hex: string) => {
    try {
      await updateBackground({ id: boardId, type: 'COLOR', value: hex }).unwrap();
    } catch (err) {
      console.error('Failed to update background color', err);
    }
  };

  const handleImageSelect = async (url: string) => {
    try {
      // Use COLOR type for preset URLs as they are just strings
      await updateBackground({ id: boardId, type: 'COLOR', value: url }).unwrap();
    } catch (err) {
      console.error('Failed to update background image', err);
    }
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      await updateBackground({ id: boardId, type: 'IMAGE', file }).unwrap();
    } catch (err: any) {
      console.error('Failed to upload background image', err);
      const message = err?.data?.message || 'Failed to upload image. Please check your internet or file size.';
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
            {PRESET_IMAGES.map(url => (
              <div 
                key={url}
                className={`${styles.imageItem} ${currentBackground === url ? styles.activeItem : ''}`}
                style={{ backgroundImage: `url(${url})` }}
                onClick={() => handleImageSelect(url)}
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
