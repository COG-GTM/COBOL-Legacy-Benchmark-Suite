import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  Typography,
  Button
} from '@mui/material';
import {
  Error,
  Home,
  ArrowBack
} from '@mui/icons-material';

const NotFound: React.FC = () => {
  const navigate = useNavigate();

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
        <Error 
          sx={{ 
            fontSize: 80, 
            color: 'error.main', 
            mb: 3 
          }} 
        />
        
        <Typography variant="h4" gutterBottom color="error">
          404 - Page Not Found
        </Typography>
        
        <Typography variant="h6" color="text.secondary" sx={{ mb: 2 }}>
          The requested page could not be found
        </Typography>
        
        <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
          The page you are looking for might have been removed, had its name changed, 
          or is temporarily unavailable.
        </Typography>
        
        <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center' }}>
          <Button
            variant="contained"
            startIcon={<Home />}
            onClick={() => navigate('/')}
            color="primary"
          >
            Go to Main Menu
          </Button>
          
          <Button
            variant="outlined"
            startIcon={<ArrowBack />}
            onClick={() => navigate(-1)}
            color="secondary"
          >
            Go Back
          </Button>
        </Box>
        
        <Typography variant="caption" color="text.secondary" sx={{ mt: 4, display: 'block' }}>
          Portfolio Management System - Error Handler
        </Typography>
      </Paper>
    </Box>
  );
};

export default NotFound;
