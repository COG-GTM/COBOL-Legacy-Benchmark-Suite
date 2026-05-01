import axios from 'axios';
import type { ApiResponse, LoginResponse, Portfolio, InvestmentPosition, Transaction, BatchJob, SystemHealth, StatisticsReport, AuditLog } from '../types';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

// Auth
export const login = (username: string, password: string) =>
  api.post<ApiResponse<LoginResponse>>('/auth/login', { username, password }).then(r => r.data);

export const register = (username: string, password: string) =>
  api.post<ApiResponse<LoginResponse>>('/auth/register', { username, password }).then(r => r.data);

// Portfolios
export const listPortfolios = (params?: Record<string, string | number>) =>
  api.get<ApiResponse<Portfolio[]>>('/portfolios', { params }).then(r => r.data);

export const getPortfolio = (id: string) =>
  api.get<ApiResponse<Portfolio>>(`/portfolios/${id}`).then(r => r.data);

export const createPortfolio = (data: Record<string, string>) =>
  api.post<ApiResponse<Portfolio>>('/portfolios', data).then(r => r.data);

export const updatePortfolio = (id: string, data: Record<string, unknown>) =>
  api.put<ApiResponse<Portfolio>>(`/portfolios/${id}`, data).then(r => r.data);

export const deletePortfolio = (id: string, reason?: string) =>
  api.delete<ApiResponse<Portfolio>>(`/portfolios/${id}`, { params: { reason } }).then(r => r.data);

export const validatePortfolio = (id: string) =>
  api.post<ApiResponse<{ valid: boolean; issues: string[] }>>(`/portfolios/${id}/validate`).then(r => r.data);

// Positions
export const getPositions = (portfolioId: string) =>
  api.get<ApiResponse<InvestmentPosition[]>>(`/portfolios/${portfolioId}/positions`).then(r => r.data);

export const updatePositions = (portfolioId: string, positions: Array<{ investmentId: string; marketValue: number }>) =>
  api.put<ApiResponse<InvestmentPosition[]>>(`/portfolios/${portfolioId}/positions`, { positions }).then(r => r.data);

// Transactions
export const createTransaction = (data: Record<string, unknown>) =>
  api.post<ApiResponse<Transaction>>('/transactions', data).then(r => r.data);

export const getTransactionHistory = (portfolioId: string, params?: Record<string, string | number>) =>
  api.get<ApiResponse<Transaction[]>>(`/transactions/portfolio/${portfolioId}`, { params }).then(r => r.data);

export const getTransaction = (id: string) =>
  api.get<ApiResponse<Transaction>>(`/transactions/${id}`).then(r => r.data);

// Batch
export const runBatch = (data?: { jobName?: string; processDate?: string }) =>
  api.post<ApiResponse<{ jobId: number; status: string; recordsRead: number; recordsWritten: number; errorCount: number }>>('/batch/run', data).then(r => r.data);

export const getBatchStatus = () =>
  api.get<ApiResponse<BatchJob[]>>('/batch/status').then(r => r.data);

// Reports
export const getPositionReport = (params?: Record<string, string>) =>
  api.get<ApiResponse<{ positions: InvestmentPosition[]; summary: Record<string, unknown> }>>('/reports/positions', { params }).then(r => r.data);

export const getAuditReport = (params?: Record<string, string>) =>
  api.get<ApiResponse<{ auditLogs: AuditLog[]; summary: Record<string, unknown> }>>('/reports/audit', { params }).then(r => r.data);

export const getStatistics = () =>
  api.get<ApiResponse<StatisticsReport>>('/reports/statistics').then(r => r.data);

// System
export const getSystemHealth = () =>
  api.get<ApiResponse<SystemHealth>>('/system/health').then(r => r.data);

export const validateSystem = () =>
  api.post<ApiResponse<{ status: string; issueCount: number; issues: Array<{ type: string; message: string; severity: string }> }>>('/system/validate').then(r => r.data);

export const runMaintenance = (operation: string) =>
  api.post<ApiResponse<Record<string, unknown>>>('/system/maintenance', { operation }).then(r => r.data);

export default api;
