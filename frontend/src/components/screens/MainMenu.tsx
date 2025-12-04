import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Paper,
} from '@mui/material';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import HistoryIcon from '@mui/icons-material/History';
import ExitToAppIcon from '@mui/icons-material/ExitToApp';
import { MenuItem } from '../../types';

interface MenuItemWithIcon extends MenuItem {
  icon: React.ReactNode;
}

function MainMenu() {
  const navigate = useNavigate();

  const menuItems: MenuItemWithIcon[] = [
    {
      id: 1,
      label: 'Portfolio Position Inquiry',
      path: '/portfolio',
      icon: <AccountBalanceIcon />,
    },
    { id: 2, label: 'Transaction History', path: '/history', icon: <HistoryIcon /> },
    { id: 3, label: 'Exit', path: '/exit', icon: <ExitToAppIcon /> },
  ];

  const handleMenuSelect = (path: string) => {
    navigate(path);
  };

  return (
    <Box sx={{ maxWidth: 600, mx: 'auto', mt: 4 }}>
      <Paper elevation={3} sx={{ p: 3 }}>
        <Typography variant="h5" gutterBottom>
          Select Option:
        </Typography>
        <List>
          {menuItems.map((item) => (
            <ListItem key={item.id} disablePadding>
              <ListItemButton onClick={() => handleMenuSelect(item.path)}>
                <ListItemIcon>{item.icon}</ListItemIcon>
                <ListItemText primary={`${item.id}. ${item.label}`} />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
      </Paper>
    </Box>
  );
}

export default MainMenu;
