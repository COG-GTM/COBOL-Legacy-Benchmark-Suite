import type { PortfolioSummary, Transaction } from '../types';

const API_BASE_URL = '/api';

export class ApiError extends Error {
  constructor(
    message: string,
    public status?: number,
    public statusText?: string
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export async function fetchPortfolio(accountNumber: string): Promise<PortfolioSummary> {
  try {
    const response = await fetch(`${API_BASE_URL}/portfolio/${accountNumber}`);

    if (!response.ok) {
      if (response.status === 400) {
        const errorData = await response.json();
        throw new ApiError(errorData.detail || 'Invalid account number', response.status, response.statusText);
      }
      throw new ApiError(`HTTP ${response.status}: ${response.statusText}`, response.status, response.statusText);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    if (error instanceof TypeError && error.message.includes('fetch')) {
      throw new ApiError('Unable to connect to the server. Please ensure the backend is running.');
    }
    throw new ApiError('An unexpected error occurred while fetching portfolio data.');
  }
}

export async function fetchTransactions(accountNumber: string): Promise<{
  accountNumber: string;
  transactions: Transaction[];
  message: string;
}> {
  try {
    const response = await fetch(`${API_BASE_URL}/transactions/${accountNumber}`);

    if (!response.ok) {
      if (response.status === 400) {
        const errorData = await response.json();
        throw new ApiError(errorData.detail || 'Invalid account number', response.status, response.statusText);
      }
      throw new ApiError(`HTTP ${response.status}: ${response.statusText}`, response.status, response.statusText);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    if (error instanceof TypeError && error.message.includes('fetch')) {
      throw new ApiError('Unable to connect to the server. Please ensure the backend is running.');
    }
    throw new ApiError('An unexpected error occurred while fetching transaction data.');
  }
}

export async function fetchDashboard(accountNumber: string): Promise<{
  accountNumber: string;
  portfolio: PortfolioSummary;
  transactions: Transaction[];
  allocationData: { name: string; value: number; color: string }[];
  performanceData: { month: string; value: number }[];
}> {
  try {
    const response = await fetch(`${API_BASE_URL}/dashboard/${accountNumber}`);

    if (!response.ok) {
      if (response.status === 400) {
        const errorData = await response.json();
        throw new ApiError(errorData.detail || 'Invalid account number', response.status, response.statusText);
      }
      throw new ApiError(`HTTP ${response.status}: ${response.statusText}`, response.status, response.statusText);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    if (error instanceof TypeError && error.message.includes('fetch')) {
      throw new ApiError('Unable to connect to the server. Please ensure the backend is running.');
    }
    throw new ApiError('An unexpected error occurred while fetching dashboard data.');
  }
}
