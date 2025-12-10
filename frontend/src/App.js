import React from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import MainLayout from './components/layout/MainLayout';
import PortfolioInquiry from './pages/PortfolioInquiry';
import TransactionHistory from './pages/TransactionHistory';
import ErrorPage from './pages/ErrorPage';
import './App.css';

/**
 * Investment Portfolio Management System - React Frontend
 * 
 * This application provides a modern web interface for the COBOL Legacy
 * Benchmark Suite's portfolio inquiry functionality.
 * 
 * CRITICAL LIMITATIONS:
 * - Backend API endpoints need to be created before this frontend is functional
 * - Authentication integration with existing COBOL SECMGR needs architectural planning
 * - Data format conversions between mainframe and web formats need middleware implementation
 * 
 * Route Structure:
 * - / : Home page (redirects to Portfolio Inquiry)
 * - /portfolio : Portfolio position inquiry (maps to INQPORT.cbl)
 * - /transactions : Transaction history inquiry (maps to INQHIST.cbl)
 * - * : Error page for unhandled routes
 * 
 * Security Placeholder:
 * The existing system has comprehensive security through SECMGR.cbl.
 * User authentication state is passed through the layout but actual
 * authentication logic needs to be implemented once the backend API is available.
 */

/**
 * Home Page Component
 * 
 * Displays a welcome message and navigation options.
 * This mirrors the main menu screen from the CICS online controller (INQONLN.cbl).
 */
const HomePage = () => {
  return (
    <div style={styles.homePage}>
      <div style={styles.welcomeSection}>
        <h1 style={styles.welcomeTitle}>Welcome to the Investment Portfolio Management System</h1>
        <p style={styles.welcomeText}>
          This web interface provides access to portfolio inquiry functions from the
          COBOL Legacy Benchmark Suite mainframe system.
        </p>
      </div>

      <div style={styles.apiNotice}>
        <h2 style={styles.noticeTitle}>Important Notice</h2>
        <p style={styles.noticeText}>
          This frontend application is a scaffolding implementation. The backend API
          endpoints required for full functionality have not yet been implemented.
        </p>
        <p style={styles.noticeText}>
          The following components need to be developed before this interface is operational:
        </p>
        <ul style={styles.noticeList}>
          <li>REST API endpoints for portfolio and transaction inquiries</li>
          <li>Middleware for mainframe data format conversion</li>
          <li>Authentication integration with COBOL SECMGR</li>
          <li>Error handling and logging integration</li>
        </ul>
      </div>

      <div style={styles.functionsSection}>
        <h2 style={styles.sectionTitle}>Available Functions</h2>
        <div style={styles.functionCards}>
          <div style={styles.functionCard}>
            <h3 style={styles.cardTitle}>Portfolio Inquiry</h3>
            <p style={styles.cardDescription}>
              View portfolio positions and balances for an account.
              Maps to INQPORT.cbl functionality.
            </p>
            <a href="/portfolio" style={styles.cardLink}>Go to Portfolio Inquiry</a>
          </div>
          <div style={styles.functionCard}>
            <h3 style={styles.cardTitle}>Transaction History</h3>
            <p style={styles.cardDescription}>
              View transaction records by account and date range.
              Maps to INQHIST.cbl functionality.
            </p>
            <a href="/transactions" style={styles.cardLink}>Go to Transaction History</a>
          </div>
        </div>
      </div>
    </div>
  );
};

const styles = {
  homePage: {
    maxWidth: '900px',
    margin: '0 auto',
  },
  welcomeSection: {
    marginBottom: '2rem',
  },
  welcomeTitle: {
    fontSize: '1.75rem',
    color: '#1a365d',
    marginBottom: '0.5rem',
  },
  welcomeText: {
    color: '#666',
    fontSize: '1rem',
    lineHeight: 1.6,
  },
  apiNotice: {
    backgroundColor: '#fff3cd',
    border: '1px solid #ffc107',
    borderRadius: '8px',
    padding: '1.5rem',
    marginBottom: '2rem',
  },
  noticeTitle: {
    color: '#856404',
    fontSize: '1.25rem',
    marginTop: 0,
    marginBottom: '0.75rem',
  },
  noticeText: {
    color: '#856404',
    fontSize: '0.9375rem',
    margin: '0 0 0.75rem',
    lineHeight: 1.5,
  },
  noticeList: {
    color: '#856404',
    fontSize: '0.875rem',
    margin: 0,
    paddingLeft: '1.5rem',
  },
  functionsSection: {
    marginTop: '2rem',
  },
  sectionTitle: {
    fontSize: '1.25rem',
    color: '#333',
    marginBottom: '1rem',
    paddingBottom: '0.5rem',
    borderBottom: '2px solid #1a365d',
  },
  functionCards: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
    gap: '1.5rem',
  },
  functionCard: {
    backgroundColor: '#f8f9fa',
    borderRadius: '8px',
    padding: '1.5rem',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
  },
  cardTitle: {
    fontSize: '1.125rem',
    color: '#1a365d',
    marginTop: 0,
    marginBottom: '0.5rem',
  },
  cardDescription: {
    color: '#666',
    fontSize: '0.9375rem',
    marginBottom: '1rem',
    lineHeight: 1.5,
  },
  cardLink: {
    display: 'inline-block',
    backgroundColor: '#1a365d',
    color: 'white',
    padding: '0.5rem 1rem',
    borderRadius: '4px',
    textDecoration: 'none',
    fontSize: '0.875rem',
    fontWeight: '500',
  },
};

/**
 * Router Configuration
 * 
 * Defines the application routes and their corresponding components.
 * Uses React Router v6 createBrowserRouter for declarative routing.
 */
const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    errorElement: <ErrorPage />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: 'portfolio',
        element: <PortfolioInquiry />,
      },
      {
        path: 'transactions',
        element: <TransactionHistory />,
      },
    ],
  },
  {
    path: '*',
    element: <ErrorPage />,
  },
]);

/**
 * App Component
 * 
 * Root component that provides the router context to the application.
 */
function App() {
  return <RouterProvider router={router} />;
}

export default App;
