import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme, CssBaseline, Box, Typography } from '@mui/material';
import Layout from './components/common/Layout';
import MainMenu from './components/screens/MainMenu';
import PortfolioInquiry from './components/screens/PortfolioInquiry';
import TransactionHistory from './components/screens/TransactionHistory';
import ErrorDisplay from './components/screens/ErrorDisplay';

const theme = createTheme({
  palette: {
    mode: 'light',
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
  },
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter>
        <Layout>
          <Routes>
            <Route path="/" element={<Navigate to="/menu" replace />} />
            <Route path="/menu" element={<MainMenu />} />
            <Route path="/portfolio" element={<PortfolioInquiry />} />
            <Route path="/history" element={<TransactionHistory />} />
            <Route path="/exit" element={<ExitScreen />} />
            <Route path="*" element={<ErrorDisplay />} />
          </Routes>
        </Layout>
      </BrowserRouter>
    </ThemeProvider>
  );
}

function ExitScreen() {
  return (
    <Box sx={{ textAlign: 'center', p: 4 }}>
      <Typography variant="h4" gutterBottom>
        Session Ended
      </Typography>
      <Typography variant="body1" paragraph>
        Thank you for using the Portfolio Management System.
      </Typography>
      <Typography variant="body1">You have been logged out successfully.</Typography>
    </Box>
  );
}

export default App;
