import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Navbar from './Navbar';
import styles from './MainLayout.module.css';

const MainLayout: React.FC = () => {
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  return (
    <div className={styles.layout}>
      <Sidebar 
        isCollapsed={isSidebarCollapsed} 
        onToggle={() => setIsSidebarCollapsed(!isSidebarCollapsed)} 
        isMobileOpen={isMobileMenuOpen}
        onCloseMobile={() => setIsMobileMenuOpen(false)}
      />
      <div className={styles.mainWrapper}>
        <main className={styles.content}>
          <Navbar onMenuToggle={() => setIsMobileMenuOpen(!isMobileMenuOpen)} />
          <Outlet />
        </main>
      </div>
      
      {/* Overlay for mobile sidebar */}
      {isMobileMenuOpen && (
        <div 
          className={styles.overlay} 
          onClick={() => setIsMobileMenuOpen(false)}
        />
      )}
    </div>
  );
};

export default MainLayout;
