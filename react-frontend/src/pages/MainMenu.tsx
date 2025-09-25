import React from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Box, 
  Card, 
  CardContent, 
  Typography, 
  Button, 
  List, 
  ListItem, 
  ListItemButton, 
  ListItemIcon, 
  ListItemText,
  Paper
} from '@mui/material';
import { 
  AccountBalance, 
  History, 
  ExitToApp,
  ArrowForward
} from '@mui/icons-material';

const MainMenu: React.FC = () => {
  const navigate = useNavigate();

  const menuOptions = [
    {
      id: 1,
      title: 'Portfolio Position Inquiry',
      description: 'View current portfolio positions and holdings',
      icon: <AccountBalance />,
      path: '/portfolio-inquiry',
      color: 'primary'
    },
    {
      id: 2,
      title: 'Transaction History',
      description: 'Browse historical transactions and activity',
      icon: <History />,
      path: '/transaction-history',
      color: 'secondary'
    },
    {
      id: 3,
      title: 'Exit',
      description: 'Terminate session and logout',
      icon: <ExitToApp />,
      path: '/exit',
      color: 'error'
    }
  ];

  const handleOptionSelect = (path: string) => {
    navigate(path);
  };

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto', mt: 4 }}>
      <Paper elevation={3} sx={{ p: 4, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom align="center">
          Portfolio Management System
        </Typography>
        <Typography variant="h6" color="text.secondary" align="center" sx={{ mb: 4 }}>
          Select an option to continue:
        </Typography>

        <List sx={{ width: '100%' }}>
          {menuOptions.map((option) => (
            <ListItem key={option.id} disablePadding sx={{ mb: 2 }}>
              <Card 
                sx={{ 
                  width: '100%',
                  transition: 'all 0.2s ease-in-out',
                  '&:hover': {
                    transform: 'translateY(-2px)',
                    boxShadow: 4
                  }
                }}
              >
                <ListItemButton
                  onClick={() => handleOptionSelect(option.path)}
                  sx={{ p: 3 }}
                >
                  <ListItemIcon sx={{ minWidth: 56 }}>
                    <Box 
                      sx={{ 
                        color: `${option.color}.main`,
                        fontSize: 32
                      }}
                    >
                      {option.icon}
                    </Box>
                  </ListItemIcon>
                  
                  <ListItemText
                    primary={
                      <Typography variant="h6" component="div">
                        {option.id}. {option.title}
                      </Typography>
                    }
                    secondary={
                      <Typography variant="body2" color="text.secondary">
                        {option.description}
                      </Typography>
                    }
                  />
                  
                  <ArrowForward sx={{ color: 'action.active' }} />
                </ListItemButton>
              </Card>
            </ListItem>
          ))}
        </List>

        <Box sx={{ mt: 4, textAlign: 'center' }}>
          <Typography variant="body2" color="text.secondary">
            COBOL Legacy Benchmark Suite - Investment Portfolio Management System
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Modernized React Interface
          </Typography>
        </Box>
      </Paper>
    </Box>
  );
};

export default MainMenu;
