import { ReactNode } from 'react';
import { AppBar, Toolbar, Typography, Container, Box } from '@mui/material';
import Navigation from './Navigation';
import ErrorBoundary from './ErrorBoundary';

interface LayoutProps {
  children: ReactNode;
}

function Layout({ children }: LayoutProps) {
  return (
    <ErrorBoundary>
      <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
        <AppBar position="static">
          <Toolbar>
            <Typography variant="h6" component="h1" sx={{ flexGrow: 1 }}>
              Portfolio Management System
            </Typography>
          </Toolbar>
        </AppBar>
        <Navigation />
        <Container component="main" sx={{ flexGrow: 1, py: 3 }}>
          {children}
        </Container>
        <Box
          component="footer"
          sx={{
            py: 2,
            px: 2,
            mt: 'auto',
            backgroundColor: (theme) => theme.palette.grey[200],
            textAlign: 'center',
          }}
        >
          <Typography variant="body2" color="text.secondary">
            Portfolio Management System - Modernized from COBOL Legacy
          </Typography>
        </Box>
      </Box>
    </ErrorBoundary>
  );
}

export default Layout;
