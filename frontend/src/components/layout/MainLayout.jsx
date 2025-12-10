import React from 'react';
import { Outlet } from 'react-router-dom';
import Header from '../common/Header';
import MainMenu from '../menu/MainMenu';

/**
 * MainLayout Component
 * 
 * Provides the basic app layout with navigation for the Investment Portfolio
 * Management System. This layout mirrors the screen structure from the CICS
 * BMS mapset (INQSET.bms) with header, menu, and content areas.
 * 
 * Layout Structure:
 * - Header: App branding and user info (similar to terminal header line)
 * - MainMenu: Navigation sidebar (similar to CICS menu options)
 * - Content: Main content area where pages are rendered
 * - Footer: System information and status
 * 
 * CRITICAL LIMITATIONS:
 * - Backend API endpoints need to be created before this frontend is functional
 * - Authentication integration with existing COBOL SECMGR needs architectural planning
 * - Session management requires backend implementation
 * 
 * Security Placeholder:
 * The existing system has comprehensive security through SECMGR.cbl.
 * Authentication state is passed as props but actual authentication logic
 * needs to be implemented once the backend API is available.
 * 
 * @param {Object} props - Component props
 * @param {Object} props.user - User authentication state (placeholder)
 * @param {boolean} props.isAuthenticated - Authentication status (placeholder)
 */
const MainLayout = ({ user = null, isAuthenticated = false }) => {
  return (
    <div style={styles.container}>
      <Header user={user} isAuthenticated={isAuthenticated} />
      
      <div style={styles.mainContent}>
        <aside style={styles.sidebar}>
          <MainMenu />
        </aside>
        
        <main style={styles.content}>
          <Outlet />
        </main>
      </div>
      
      <footer style={styles.footer}>
        <div style={styles.footerContent}>
          <span>Investment Portfolio Management System v1.0</span>
          <span style={styles.footerDivider}>|</span>
          <span>COBOL Legacy Benchmark Suite</span>
          <span style={styles.footerDivider}>|</span>
          <span style={styles.systemStatus}>
            System Status: API Not Connected
          </span>
        </div>
        <div style={styles.footerNote}>
          Note: Backend API endpoints are required for full functionality
        </div>
      </footer>
    </div>
  );
};

const styles = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    minHeight: '100vh',
    backgroundColor: '#f5f5f5',
  },
  mainContent: {
    display: 'flex',
    flex: 1,
  },
  sidebar: {
    width: '250px',
    backgroundColor: '#2d3748',
    padding: '1rem',
    boxShadow: '2px 0 4px rgba(0,0,0,0.1)',
  },
  content: {
    flex: 1,
    padding: '2rem',
    backgroundColor: '#ffffff',
    margin: '1rem',
    borderRadius: '8px',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
  },
  footer: {
    backgroundColor: '#1a365d',
    color: 'white',
    padding: '1rem 2rem',
    textAlign: 'center',
  },
  footerContent: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    gap: '0.5rem',
    fontSize: '0.875rem',
  },
  footerDivider: {
    opacity: 0.5,
  },
  systemStatus: {
    color: '#ffc107',
  },
  footerNote: {
    marginTop: '0.5rem',
    fontSize: '0.75rem',
    opacity: 0.7,
  },
};

export default MainLayout;
