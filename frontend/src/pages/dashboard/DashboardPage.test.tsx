import { screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { DashboardPage } from './DashboardPage';
import { renderWithProviders } from '@/test/helpers';

describe('DashboardPage', () => {
  it('renders the page title', () => {
    renderWithProviders(<DashboardPage />);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });

  it('renders the description', () => {
    renderWithProviders(<DashboardPage />);
    expect(screen.getByText('Overview of your investment portfolio management system')).toBeInTheDocument();
  });

  it('renders summary cards', () => {
    renderWithProviders(<DashboardPage />);
    expect(screen.getByText('Total Portfolios')).toBeInTheDocument();
    expect(screen.getByText('Total Market Value')).toBeInTheDocument();
    expect(screen.getByText('Active Positions')).toBeInTheDocument();
    expect(screen.getByText('Pending Transactions')).toBeInTheDocument();
  });

  it('renders recent transactions section', () => {
    renderWithProviders(<DashboardPage />);
    expect(screen.getByText('Recent Transactions')).toBeInTheDocument();
    expect(screen.getByText('Trans ID')).toBeInTheDocument();
    expect(screen.getByText('Account')).toBeInTheDocument();
  });

  it('renders quick navigation section', () => {
    renderWithProviders(<DashboardPage />);
    expect(screen.getByText('Quick Navigation')).toBeInTheDocument();
    expect(screen.getByText('Manage investment portfolios')).toBeInTheDocument();
    expect(screen.getByText('View transaction history')).toBeInTheDocument();
    expect(screen.getByText('Generate financial reports')).toBeInTheDocument();
    expect(screen.getByText('Monitor batch job status')).toBeInTheDocument();
    expect(screen.getByText('Review system errors')).toBeInTheDocument();
  });

  it('renders quick links that navigate correctly', () => {
    renderWithProviders(<DashboardPage />);
    const portfolioLink = screen.getByText('Manage investment portfolios').closest('a');
    expect(portfolioLink).toHaveAttribute('href', '/portfolios');
    const txnLink = screen.getByText('View transaction history').closest('a');
    expect(txnLink).toHaveAttribute('href', '/transactions');
  });
});
