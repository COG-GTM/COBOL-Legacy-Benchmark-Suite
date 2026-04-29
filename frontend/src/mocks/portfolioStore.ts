import type { Portfolio } from '../types/portfolio';
import initialData from './portfolios.json';

let portfolios: Portfolio[] = [...(initialData as Portfolio[])];

export function getPortfolios(): Portfolio[] {
  return portfolios;
}

export function getPortfolioById(id: string): Portfolio | undefined {
  return portfolios.find((p) => p.id === id);
}

export function addPortfolio(portfolio: Portfolio): void {
  portfolios = [...portfolios, portfolio];
}

export function updatePortfolio(id: string, updates: Partial<Portfolio>): void {
  portfolios = portfolios.map((p) => (p.id === id ? { ...p, ...updates } : p));
}

export function deletePortfolio(id: string): void {
  portfolios = portfolios.filter((p) => p.id !== id);
}

export function getNextPortfolioId(): string {
  const maxNum = portfolios.reduce((max, p) => {
    const num = parseInt(p.id.substring(4), 10);
    return num > max ? num : max;
  }, 0);
  return `PORT${String(maxNum + 1).padStart(4, '0')}`;
}
