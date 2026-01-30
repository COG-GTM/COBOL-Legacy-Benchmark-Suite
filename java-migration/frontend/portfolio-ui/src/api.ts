import axios from 'axios';
import type { Portfolio, Transaction, TransactionRequest, Position, AuditLog, BatchProcessingResult, PortfolioUpdateRequest } from './types';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const portfolioApi = {
  getAll: async (): Promise<Portfolio[]> => {
    const response = await api.get('/api/portfolios');
    return response.data;
  },

  getById: async (portfolioId: string): Promise<Portfolio> => {
    const response = await api.get(`/api/portfolios/${portfolioId}`);
    return response.data;
  },

  getByAccountNo: async (accountNo: string): Promise<Portfolio> => {
    const response = await api.get(`/api/portfolios/account/${accountNo}`);
    return response.data;
  },

  getByStatus: async (status: string): Promise<Portfolio[]> => {
    const response = await api.get(`/api/portfolios/status/${status}`);
    return response.data;
  },

  search: async (clientName: string): Promise<Portfolio[]> => {
    const response = await api.get(`/api/portfolios/search?clientName=${encodeURIComponent(clientName)}`);
    return response.data;
  },

  create: async (data: { portfolioId: string; accountNo: string; clientName: string; clientType: string; userId: string }): Promise<Portfolio> => {
    const response = await api.post('/api/portfolios', data);
    return response.data;
  },

  update: async (portfolioId: string, data: PortfolioUpdateRequest): Promise<Portfolio> => {
    const response = await api.put(`/api/portfolios/${portfolioId}`, data);
    return response.data;
  },

  updateStatus: async (portfolioId: string, status: string, userId: string): Promise<Portfolio> => {
    const response = await api.put(`/api/portfolios/${portfolioId}/status`, { status, userId });
    return response.data;
  },

  updateName: async (portfolioId: string, clientName: string, userId: string): Promise<Portfolio> => {
    const response = await api.put(`/api/portfolios/${portfolioId}/name`, { clientName, userId });
    return response.data;
  },

  updateValue: async (portfolioId: string, totalValue: number, userId: string): Promise<Portfolio> => {
    const response = await api.put(`/api/portfolios/${portfolioId}/value`, { totalValue: totalValue.toString(), userId });
    return response.data;
  },
};

export const transactionApi = {
  process: async (request: TransactionRequest): Promise<Transaction> => {
    const response = await api.post('/api/transactions/process', request);
    return response.data;
  },

  processBatch: async (requests: TransactionRequest[]): Promise<BatchProcessingResult> => {
    const response = await api.post('/api/transactions/batch', requests);
    return response.data;
  },

  getByPortfolio: async (portfolioId: string): Promise<Transaction[]> => {
    const response = await api.get(`/api/transactions/portfolio/${portfolioId}`);
    return response.data;
  },

  getByDateRange: async (startDate: string, endDate: string): Promise<Transaction[]> => {
    const response = await api.get(`/api/transactions/date-range?startDate=${startDate}&endDate=${endDate}`);
    return response.data;
  },
};

export const positionApi = {
  getByAccount: async (accountNo: string, userId?: string): Promise<Position> => {
    const response = await api.get(`/api/positions/account/${accountNo}`, {
      headers: userId ? { 'X-User-Id': userId } : {},
    });
    return response.data;
  },

  getByPortfolio: async (portfolioId: string, userId?: string): Promise<Position[]> => {
    const response = await api.get(`/api/positions/portfolio/${portfolioId}`, {
      headers: userId ? { 'X-User-Id': userId } : {},
    });
    return response.data;
  },

  getAll: async (): Promise<Position[]> => {
    const response = await api.get('/api/positions');
    return response.data;
  },
};

export const auditApi = {
  getByPortfolio: async (portfolioId: string): Promise<AuditLog[]> => {
    const response = await api.get(`/api/audit/portfolio/${portfolioId}`);
    return response.data;
  },

  getByAccount: async (accountNo: string): Promise<AuditLog[]> => {
    const response = await api.get(`/api/audit/account/${accountNo}`);
    return response.data;
  },

  getByDateRange: async (startTime: string, endTime: string): Promise<AuditLog[]> => {
    const response = await api.get(`/api/audit/date-range?startTime=${startTime}&endTime=${endTime}`);
    return response.data;
  },
};

export default api;
