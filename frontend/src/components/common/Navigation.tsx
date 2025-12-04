import { Link as RouterLink, useLocation } from 'react-router-dom';
import { Box, Tabs, Tab } from '@mui/material';
import HomeIcon from '@mui/icons-material/Home';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import HistoryIcon from '@mui/icons-material/History';
import ExitToAppIcon from '@mui/icons-material/ExitToApp';

function Navigation() {
  const location = useLocation();

  const navItems = [
    { path: '/menu', label: 'Main Menu', icon: <HomeIcon /> },
    { path: '/portfolio', label: 'Portfolio Inquiry', icon: <AccountBalanceIcon /> },
    { path: '/history', label: 'Transaction History', icon: <HistoryIcon /> },
    { path: '/exit', label: 'Exit', icon: <ExitToAppIcon /> },
  ];

  const currentTabIndex = navItems.findIndex((item) => item.path === location.pathname);

  return (
    <Box sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: 'background.paper' }}>
      <Tabs
        value={currentTabIndex >= 0 ? currentTabIndex : 0}
        aria-label="navigation tabs"
        centered
      >
        {navItems.map((item) => (
          <Tab
            key={item.path}
            icon={item.icon}
            label={item.label}
            component={RouterLink}
            to={item.path}
            iconPosition="start"
          />
        ))}
      </Tabs>
    </Box>
  );
}

export default Navigation;
