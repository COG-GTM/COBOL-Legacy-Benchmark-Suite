import { PortfolioPosition, TransactionHistory, Transaction } from '../types';

const mockPositions: Record<string, PortfolioPosition[]> = {
  ACC001: [
    {
      accountId: 'ACC001',
      fundId: 'FND001',
      fundName: 'Growth Fund A',
      units: 1500.5,
      costBasis: 45000.0,
      marketValue: 52500.75,
    },
    {
      accountId: 'ACC001',
      fundId: 'FND002',
      fundName: 'Income Fund B',
      units: 2000.0,
      costBasis: 30000.0,
      marketValue: 32000.0,
    },
  ],
  ACC002: [
    {
      accountId: 'ACC002',
      fundId: 'FND003',
      fundName: 'Balanced Fund C',
      units: 500.25,
      costBasis: 15000.0,
      marketValue: 16250.5,
    },
  ],
};

const mockTransactions: Record<string, Transaction[]> = {
  ACC001: [
    {
      date: '2024-01-15',
      type: 'BUY',
      units: 100.0,
      price: 30.0,
      amount: 3000.0,
    },
    {
      date: '2024-02-20',
      type: 'BUY',
      units: 150.0,
      price: 32.5,
      amount: 4875.0,
    },
    {
      date: '2024-03-10',
      type: 'SELL',
      units: 50.0,
      price: 35.0,
      amount: 1750.0,
    },
    {
      date: '2024-04-05',
      type: 'DIV',
      units: 0.0,
      price: 0.0,
      amount: 250.0,
    },
    {
      date: '2024-05-15',
      type: 'BUY',
      units: 200.0,
      price: 33.0,
      amount: 6600.0,
    },
  ],
  ACC002: [
    {
      date: '2024-01-10',
      type: 'BUY',
      units: 500.25,
      price: 30.0,
      amount: 15007.5,
    },
    {
      date: '2024-03-15',
      type: 'DIV',
      units: 0.0,
      price: 0.0,
      amount: 125.0,
    },
  ],
};

class MockDataService {
  async getPortfolioPositions(accountId: string): Promise<PortfolioPosition[]> {
    await this.simulateDelay();
    const positions = mockPositions[accountId.toUpperCase()];
    if (!positions) {
      throw new Error(`Account ${accountId} not found`);
    }
    return positions;
  }

  async getTransactionHistory(accountId: string): Promise<TransactionHistory> {
    await this.simulateDelay();
    const transactions = mockTransactions[accountId.toUpperCase()];
    if (!transactions) {
      throw new Error(`Account ${accountId} not found`);
    }
    return {
      accountId: accountId.toUpperCase(),
      transactions,
    };
  }

  private simulateDelay(): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, 500));
  }
}

export const mockDataService = new MockDataService();
export default MockDataService;
