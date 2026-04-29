import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Button, Typography, theme } from 'antd';
import {
  DashboardOutlined,
  FolderOutlined,
  SearchOutlined,
  SwapOutlined,
  BarChartOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
} from '@ant-design/icons';
import type { MenuProps } from 'antd';
import ProtectedRoute from '../components/ProtectedRoute';
import { useAuth } from '../contexts/AuthContext';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

type MenuItem = Required<MenuProps>['items'][number];

const menuItems: MenuItem[] = [
  {
    key: '/',
    icon: <DashboardOutlined />,
    label: 'Dashboard',
  },
  {
    key: 'portfolios-sub',
    icon: <FolderOutlined />,
    label: 'Portfolio Management',
    children: [
      { key: '/portfolios', label: 'Portfolio List' },
      { key: '/portfolios/new', label: 'Create Portfolio' },
    ],
  },
  {
    key: 'inquiries-sub',
    icon: <SearchOutlined />,
    label: 'Inquiries',
    children: [
      { key: '/positions', label: 'Position Inquiry' },
      { key: '/history', label: 'Transaction History' },
    ],
  },
  {
    key: 'transactions-sub',
    icon: <SwapOutlined />,
    label: 'Transactions',
    children: [
      { key: '/transactions/new', label: 'New Transaction' },
    ],
  },
  {
    key: 'reports-sub',
    icon: <BarChartOutlined />,
    label: 'Reports',
    children: [
      { key: '/reports/valuation', label: 'Valuation Report' },
      { key: '/reports/audit', label: 'Audit Trail' },
      { key: '/reports/statistics', label: 'System Statistics' },
    ],
  },
];

function getSelectedKeys(pathname: string): string[] {
  return [pathname];
}

function getOpenKeys(pathname: string): string[] {
  if (pathname.startsWith('/portfolios')) return ['portfolios-sub'];
  if (pathname.startsWith('/positions') || pathname.startsWith('/history'))
    return ['inquiries-sub'];
  if (pathname.startsWith('/transactions')) return ['transactions-sub'];
  if (pathname.startsWith('/reports')) return ['reports-sub'];
  return [];
}

export function Component() {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();
  const {
    token: { colorBgContainer },
  } = theme.useToken();

  const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
    navigate(key);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <ProtectedRoute>
      <Layout style={{ minHeight: '100vh' }}>
        <Sider
          collapsible
          collapsed={collapsed}
          onCollapse={setCollapsed}
          trigger={null}
          breakpoint="lg"
          style={{ overflow: 'auto', height: '100vh', position: 'sticky', top: 0, left: 0 }}
        >
          <div
            style={{
              height: 48,
              margin: 12,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Text
              strong
              style={{
                color: '#fff',
                fontSize: collapsed ? 14 : 16,
                whiteSpace: 'nowrap',
                overflow: 'hidden',
              }}
            >
              {collapsed ? 'PMS' : 'Portfolio Mgmt'}
            </Text>
          </div>
          <Menu
            theme="dark"
            mode="inline"
            selectedKeys={getSelectedKeys(location.pathname)}
            defaultOpenKeys={getOpenKeys(location.pathname)}
            items={menuItems}
            onClick={handleMenuClick}
          />
        </Sider>
        <Layout>
          <Header
            style={{
              padding: '0 24px',
              background: colorBgContainer,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
              <Button
                type="text"
                icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                onClick={() => setCollapsed(!collapsed)}
              />
              <Text strong style={{ fontSize: 18 }}>
                Portfolio Management System
              </Text>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
              <UserOutlined />
              <Text>{user?.username}</Text>
              <Button
                type="text"
                icon={<LogoutOutlined />}
                onClick={handleLogout}
              >
                Logout
              </Button>
            </div>
          </Header>
          <Content style={{ margin: 24 }}>
            <Outlet />
          </Content>
        </Layout>
      </Layout>
    </ProtectedRoute>
  );
}
