import React from 'react';
import { AppBar, Toolbar, Typography, Container, Box } from '@mui/material';
import { useLocation } from 'react-router-dom';

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const location = useLocation();
  
  const getPageTitle = () => {
    switch (location.pathname) {
      case '/':
      case '/menu':
        return 'Portfolio Management System';
      case '/portfolio-inquiry':
        return 'Portfolio Position Inquiry';
      case '/transaction-history':
        return 'Transaction History';
      case '/exit':
        return 'System Exit';
      default:
        return 'Portfolio Management System';
    }
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <AppBar position="static" elevation={2}>
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            {getPageTitle()}
          </Typography>
        </Toolbar>
      </AppBar>
      
      <Container 
        component="main" 
        maxWidth="lg" 
        sx={{ 
          flexGrow: 1, 
          py: 3,
          display: 'flex',
          flexDirection: 'column'
        }}
      >
        {children}
      </Container>
      
      <Box 
        component="footer" 
        sx={{ 
          py: 2, 
          px: 3, 
          backgroundColor: 'grey.100',
          borderTop: '1px solid',
          borderColor: 'grey.300'
        }}
      >
        <Typography variant="body2" color="text.secondary" align="center">
          COBOL Legacy Benchmark Suite - Investment Portfolio Management System
        </Typography>
      </Box>
    </Box>
  );
};

export default Layout;
