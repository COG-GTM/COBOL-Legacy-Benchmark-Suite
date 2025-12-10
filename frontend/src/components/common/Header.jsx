import React from 'react';
import { Link } from 'react-router-dom';

/**
 * Header Component
 * 
 * Provides app title/branding and basic navigation elements for the
 * Investment Portfolio Management System frontend.
 * 
 * This component mirrors the header functionality that would be displayed
 * in the CICS terminal screens (INQSET.bms mapset).
 * 
 * CRITICAL LIMITATIONS:
 * - Authentication integration with existing COBOL SECMGR needs architectural planning
 * - User session management requires backend API implementation
 * - Security context from mainframe RACF/ACF2 needs middleware translation
 * 
 * @param {Object} props - Component props
 * @param {Object} props.user - User authentication state (placeholder for future implementation)
 * @param {boolean} props.isAuthenticated - Authentication status (placeholder)
 */
const Header = ({ user = null, isAuthenticated = false }) => {
  return (
    <header style={styles.header}>
      <div style={styles.brandContainer}>
        <h1 style={styles.title}>Investment Portfolio Management System</h1>
        <span style={styles.subtitle}>COBOL Legacy Benchmark Suite - Web Interface</span>
      </div>
      
      <nav style={styles.nav}>
        <Link to="/" style={styles.navLink}>Home</Link>
        <Link to="/portfolio" style={styles.navLink}>Portfolio Inquiry</Link>
        <Link to="/transactions" style={styles.navLink}>Transaction History</Link>
      </nav>
      
      <div style={styles.userSection}>
        {isAuthenticated && user ? (
          <span style={styles.userInfo}>
            User: {user.userId || 'Unknown'}
          </span>
        ) : (
          <span style={styles.userInfo}>
            Not Authenticated
          </span>
        )}
      </div>
    </header>
  );
};

const styles = {
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '1rem 2rem',
    backgroundColor: '#1a365d',
    color: 'white',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
  },
  brandContainer: {
    display: 'flex',
    flexDirection: 'column',
  },
  title: {
    margin: 0,
    fontSize: '1.5rem',
    fontWeight: 'bold',
  },
  subtitle: {
    fontSize: '0.75rem',
    opacity: 0.8,
    marginTop: '0.25rem',
  },
  nav: {
    display: 'flex',
    gap: '1.5rem',
  },
  navLink: {
    color: 'white',
    textDecoration: 'none',
    fontSize: '1rem',
    padding: '0.5rem 1rem',
    borderRadius: '4px',
    transition: 'background-color 0.2s',
  },
  userSection: {
    display: 'flex',
    alignItems: 'center',
  },
  userInfo: {
    fontSize: '0.875rem',
    padding: '0.5rem 1rem',
    backgroundColor: 'rgba(255,255,255,0.1)',
    borderRadius: '4px',
  },
};

export default Header;
