import React from 'react';
import { NavLink } from 'react-router-dom';

/**
 * MainMenu Component
 * 
 * Provides navigation to Portfolio Inquiry and Transaction History pages.
 * This component mirrors the menu options available in the CICS online
 * controller (INQONLN.cbl) which handles screen navigation.
 * 
 * Menu Options (matching COBOL system):
 * 1. Portfolio Inquiry - Maps to INQPORT.cbl functionality
 * 2. Transaction History - Maps to INQHIST.cbl functionality
 * 
 * CRITICAL LIMITATIONS:
 * - Backend API endpoints need to be created before this frontend is functional
 * - Menu access control should integrate with SECMGR.cbl authorization levels
 * - User permissions need to be validated against mainframe security
 * 
 * Security Placeholder:
 * The existing system validates user access through SECMGR.cbl before
 * allowing access to inquiry functions. This menu should eventually
 * filter options based on user authorization level.
 */
const MainMenu = () => {
  const menuItems = [
    {
      path: '/portfolio',
      label: 'Portfolio Inquiry',
      description: 'View portfolio positions and balances',
      icon: '📊',
    },
    {
      path: '/transactions',
      label: 'Transaction History',
      description: 'View transaction records by date range',
      icon: '📋',
    },
  ];

  return (
    <nav style={styles.nav}>
      <div style={styles.menuHeader}>
        <h3 style={styles.menuTitle}>Main Menu</h3>
        <span style={styles.menuSubtitle}>Inquiry Functions</span>
      </div>
      
      <ul style={styles.menuList}>
        {menuItems.map((item) => (
          <li key={item.path} style={styles.menuItem}>
            <NavLink
              to={item.path}
              style={({ isActive }) => ({
                ...styles.menuLink,
                ...(isActive ? styles.menuLinkActive : {}),
              })}
            >
              <span style={styles.menuIcon}>{item.icon}</span>
              <div style={styles.menuContent}>
                <span style={styles.menuLabel}>{item.label}</span>
                <span style={styles.menuDescription}>{item.description}</span>
              </div>
            </NavLink>
          </li>
        ))}
      </ul>
      
      <div style={styles.menuFooter}>
        <div style={styles.helpSection}>
          <span style={styles.helpTitle}>Help</span>
          <p style={styles.helpText}>
            Select an inquiry function from the menu above.
            Enter account information to retrieve data.
          </p>
        </div>
        <div style={styles.systemNote}>
          <strong>Note:</strong> API connection required for data retrieval.
        </div>
      </div>
    </nav>
  );
};

const styles = {
  nav: {
    display: 'flex',
    flexDirection: 'column',
    height: '100%',
  },
  menuHeader: {
    marginBottom: '1.5rem',
    paddingBottom: '1rem',
    borderBottom: '1px solid rgba(255,255,255,0.1)',
  },
  menuTitle: {
    margin: 0,
    color: 'white',
    fontSize: '1.25rem',
    fontWeight: 'bold',
  },
  menuSubtitle: {
    color: 'rgba(255,255,255,0.6)',
    fontSize: '0.75rem',
    marginTop: '0.25rem',
    display: 'block',
  },
  menuList: {
    listStyle: 'none',
    padding: 0,
    margin: 0,
    flex: 1,
  },
  menuItem: {
    marginBottom: '0.5rem',
  },
  menuLink: {
    display: 'flex',
    alignItems: 'flex-start',
    padding: '0.75rem 1rem',
    borderRadius: '6px',
    textDecoration: 'none',
    color: 'rgba(255,255,255,0.8)',
    transition: 'all 0.2s',
    backgroundColor: 'transparent',
  },
  menuLinkActive: {
    backgroundColor: 'rgba(255,255,255,0.15)',
    color: 'white',
  },
  menuIcon: {
    fontSize: '1.25rem',
    marginRight: '0.75rem',
    marginTop: '0.125rem',
  },
  menuContent: {
    display: 'flex',
    flexDirection: 'column',
  },
  menuLabel: {
    fontWeight: '500',
    fontSize: '0.9375rem',
  },
  menuDescription: {
    fontSize: '0.75rem',
    opacity: 0.7,
    marginTop: '0.25rem',
  },
  menuFooter: {
    marginTop: 'auto',
    paddingTop: '1rem',
    borderTop: '1px solid rgba(255,255,255,0.1)',
  },
  helpSection: {
    marginBottom: '1rem',
  },
  helpTitle: {
    color: 'rgba(255,255,255,0.8)',
    fontSize: '0.875rem',
    fontWeight: '500',
    display: 'block',
    marginBottom: '0.5rem',
  },
  helpText: {
    color: 'rgba(255,255,255,0.5)',
    fontSize: '0.75rem',
    margin: 0,
    lineHeight: 1.4,
  },
  systemNote: {
    backgroundColor: 'rgba(255,193,7,0.1)',
    color: '#ffc107',
    padding: '0.5rem',
    borderRadius: '4px',
    fontSize: '0.6875rem',
  },
};

export default MainMenu;
