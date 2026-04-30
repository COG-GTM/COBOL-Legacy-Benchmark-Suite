export interface MenuOption {
  id: string;
  label: string;
  shortcut: string;
  description: string;
  route?: string;
  action?: () => void;
}

export interface MenuState {
  selectedOption: string | null;
  isKeyboardNavigation: boolean;
}

export type MenuOptionId = 'portfolio' | 'history' | 'dashboard';

export const MENU_OPTIONS: MenuOption[] = [
  {
    id: 'portfolio',
    label: 'Portfolio Inquiry',
    shortcut: '1',
    description: 'Look up and analyze investment portfolio holdings, positions, and performance metrics',
    route: '/portfolio-inquiry',
  },
  {
    id: 'history',
    label: 'Transaction History',
    shortcut: '2',
    description: 'Review investment transaction activity including buys, sells, transfers, and fees',
    route: '/transaction-history',
  },
  {
    id: 'dashboard',
    label: 'Portfolio Dashboard',
    shortcut: '3',
    description: 'Visual overview of portfolio allocation, performance trends, and key financial metrics',
    route: '/dashboard',
  },
];
