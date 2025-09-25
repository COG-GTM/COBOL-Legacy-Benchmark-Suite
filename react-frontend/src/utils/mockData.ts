export interface PortfolioPosition {
  fundId: string;
  fundName: string;
  units: number;
  costBasis: number;
  marketValue: number;
  gainLoss: number;
  gainLossPercent: number;
}

export interface Transaction {
  date: string;
  type: 'BUY' | 'SELL' | 'DIV' | 'FEE';
  units: number;
  price: number;
  amount: number;
  description: string;
}

export const mockPortfolioData: Record<string, PortfolioPosition> = {
  'ACC001': {
    fundId: 'FUND01',
    fundName: 'Growth Equity Fund',
    units: 1250.50,
    costBasis: 125000.00,
    marketValue: 142750.00,
    gainLoss: 17750.00,
    gainLossPercent: 14.2
  },
  'ACC002': {
    fundId: 'FUND02',
    fundName: 'Conservative Bond Fund',
    units: 2000.00,
    costBasis: 200000.00,
    marketValue: 205600.00,
    gainLoss: 5600.00,
    gainLossPercent: 2.8
  }
};

export const mockTransactionData: Record<string, Transaction[]> = {
  'ACC001': [
    {
      date: '2024-09-20',
      type: 'BUY',
      units: 500.00,
      price: 100.00,
      amount: -50000.00,
      description: 'Purchase Growth Equity Fund'
    },
    {
      date: '2024-09-15',
      type: 'DIV',
      units: 0,
      price: 0,
      amount: 1250.00,
      description: 'Dividend Payment'
    },
    {
      date: '2024-09-10',
      type: 'BUY',
      units: 750.50,
      price: 100.00,
      amount: -75050.00,
      description: 'Additional Purchase'
    },
    {
      date: '2024-09-05',
      type: 'SELL',
      units: -200.00,
      price: 105.00,
      amount: 21000.00,
      description: 'Partial Sale'
    },
    {
      date: '2024-09-01',
      type: 'FEE',
      units: 0,
      price: 0,
      amount: -25.00,
      description: 'Management Fee'
    }
  ],
  'ACC002': [
    {
      date: '2024-09-18',
      type: 'BUY',
      units: 1000.00,
      price: 100.00,
      amount: -100000.00,
      description: 'Purchase Conservative Bond Fund'
    },
    {
      date: '2024-09-12',
      type: 'DIV',
      units: 0,
      price: 0,
      amount: 2800.00,
      description: 'Quarterly Dividend'
    },
    {
      date: '2024-09-08',
      type: 'BUY',
      units: 1000.00,
      price: 100.00,
      amount: -100000.00,
      description: 'Initial Purchase'
    }
  ]
};
