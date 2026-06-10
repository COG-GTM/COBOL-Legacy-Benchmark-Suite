import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { PortfolioListPage } from './PortfolioListPage';
import { renderWithProviders } from '@/test/helpers';

describe('PortfolioListPage', () => {
  it('renders the page title', () => {
    renderWithProviders(<PortfolioListPage />);
    expect(screen.getByText('Portfolios')).toBeInTheDocument();
  });

  it('renders the description', () => {
    renderWithProviders(<PortfolioListPage />);
    expect(screen.getByText('Manage your investment portfolios')).toBeInTheDocument();
  });

  it('renders new portfolio button', () => {
    renderWithProviders(<PortfolioListPage />);
    expect(screen.getByText('New Portfolio')).toBeInTheDocument();
  });

  it('renders search input', () => {
    renderWithProviders(<PortfolioListPage />);
    expect(screen.getByPlaceholderText('Search by name or ID...')).toBeInTheDocument();
  });

  it('renders status filter', () => {
    renderWithProviders(<PortfolioListPage />);
    expect(screen.getByDisplayValue('All Statuses')).toBeInTheDocument();
  });

  it('renders portfolio data', () => {
    renderWithProviders(<PortfolioListPage />);
    expect(screen.getByText('Growth Equity Fund')).toBeInTheDocument();
  });

  it('filters portfolios by search text', async () => {
    const user = userEvent.setup();
    renderWithProviders(<PortfolioListPage />);
    await user.type(screen.getByPlaceholderText('Search by name or ID...'), 'Growth');
    expect(screen.getByText('Growth Equity Fund')).toBeInTheDocument();
  });

  it('filters by status', async () => {
    const user = userEvent.setup();
    renderWithProviders(<PortfolioListPage />);
    const statusFilter = screen.getByDisplayValue('All Statuses');
    await user.selectOptions(statusFilter, 'A');
    expect(screen.getByText('Growth Equity Fund')).toBeInTheDocument();
  });
});
