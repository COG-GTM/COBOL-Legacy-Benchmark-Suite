import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Layout from './components/Layout';
import MainMenu from './pages/MainMenu';
import PortfolioInquiry from './pages/PortfolioInquiry';
import TransactionHistory from './pages/TransactionHistory';
import ExitPage from './pages/ExitPage';
import NotFound from './pages/NotFound';
import ErrorBoundary from './components/ErrorBoundary';

const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
    background: {
      default: '#f5f5f5',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
    h4: {
      fontWeight: 600,
    },
  },
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <ErrorBoundary>
        <Router>
          <Layout>
            <Routes>
              <Route path="/" element={<MainMenu />} />
              <Route path="/menu" element={<MainMenu />} />
              <Route path="/portfolio-inquiry" element={<PortfolioInquiry />} />
              <Route path="/transaction-history" element={<TransactionHistory />} />
              <Route path="/exit" element={<ExitPage />} />
              <Route path="*" element={<NotFound />} />
            </Routes>
          </Layout>
        </Router>
      </ErrorBoundary>
    </ThemeProvider>
  );
}

export default App;
