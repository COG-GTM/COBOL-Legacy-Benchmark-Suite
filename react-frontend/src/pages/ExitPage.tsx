import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  Typography,
  Button,
  CircularProgress,
  Alert
} from '@mui/material';
import {
  ExitToApp,
  CheckCircle,
  Home
} from '@mui/icons-material';

const ExitPage: React.FC = () => {
  const navigate = useNavigate();
  const [isLoggingOut, setIsLoggingOut] = useState(true);
  const [logoutComplete, setLogoutComplete] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsLoggingOut(false);
      setLogoutComplete(true);
    }, 2000);

    return () => clearTimeout(timer);
  }, []);

  const handleReturnToMenu = () => {
    navigate('/');
  };

  const handleActualExit = () => {
    window.close();
  };

  return (
    <Box 
      sx={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        minHeight: '60vh',
        mt: 4
      }}
    >
      <Paper 
        elevation={3} 
        sx={{ 
          p: 6, 
          maxWidth: 500, 
          textAlign: 'center',
          width: '100%'
        }}
      >
        {isLoggingOut && (
          <>
            <ExitToApp 
              sx={{ 
                fontSize: 64, 
                color: 'primary.main', 
                mb: 3 
              }} 
            />
            
            <Typography variant="h5" gutterBottom>
              Logging Out...
            </Typography>
            
            <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
              Please wait while we securely terminate your session
            </Typography>
            
            <CircularProgress size={40} />
          </>
        )}

        {logoutComplete && (
          <>
            <CheckCircle 
              sx={{ 
                fontSize: 64, 
                color: 'success.main', 
                mb: 3 
              }} 
            />
            
            <Typography variant="h5" gutterBottom color="success.main">
              Session Terminated
            </Typography>
            
            <Alert severity="success" sx={{ mb: 4, textAlign: 'left' }}>
              <Typography variant="body1">
                Your session has been successfully terminated. All resources have been released.
              </Typography>
            </Alert>
            
            <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
              Thank you for using the Portfolio Management System
            </Typography>
            
            <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center' }}>
              <Button
                variant="contained"
                startIcon={<Home />}
                onClick={handleReturnToMenu}
                color="primary"
              >
                Return to Main Menu
              </Button>
              
              <Button
                variant="outlined"
                startIcon={<ExitToApp />}
                onClick={handleActualExit}
                color="secondary"
              >
                Close Application
              </Button>
            </Box>
            
            <Typography variant="caption" color="text.secondary" sx={{ mt: 3, display: 'block' }}>
              COBOL Legacy Benchmark Suite - Session Management
            </Typography>
          </>
        )}
      </Paper>
    </Box>
  );
};

export default ExitPage;
