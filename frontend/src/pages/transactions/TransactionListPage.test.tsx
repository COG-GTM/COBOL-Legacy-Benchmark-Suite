import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { TransactionListPage } from './TransactionListPage';
import { renderWithRouter } from '@/test/helpers';

describe('TransactionListPage', () => {
  it('renders the page title', () => {
    renderWithRouter(<TransactionListPage />);
    expect(screen.getByText('Transaction History')).toBeInTheDocument();
  });

  it('renders the description', () => {
    renderWithRouter(<TransactionListPage />);
    expect(screen.getByText('View and filter all transactions across accounts')).toBeInTheDocument();
  });

  it('renders search and date filter controls', () => {
    renderWithRouter(<TransactionListPage />);
    expect(screen.getByPlaceholderText('Search by account number...')).toBeInTheDocument();
    expect(screen.getByText('From')).toBeInTheDocument();
    expect(screen.getByText('To')).toBeInTheDocument();
  });

  it('renders data table with columns', () => {
    renderWithRouter(<TransactionListPage />);
    expect(screen.getByText('Date')).toBeInTheDocument();
    expect(screen.getByText('Account')).toBeInTheDocument();
    expect(screen.getByText('Fund ID')).toBeInTheDocument();
    expect(screen.getByText('Units')).toBeInTheDocument();
  });

  it('renders transaction data rows', () => {
    renderWithRouter(<TransactionListPage />);
    const rows = screen.getAllByRole('row');
    expect(rows.length).toBeGreaterThan(1);
  });

  it('filters by account search', async () => {
    const user = userEvent.setup();
    renderWithRouter(<TransactionListPage />);
    const searchInput = screen.getByPlaceholderText('Search by account number...');
    await user.type(searchInput, '100000001');
    const rows = screen.getAllByRole('row');
    expect(rows.length).toBeGreaterThanOrEqual(2);
  });

  it('renders pagination', () => {
    renderWithRouter(<TransactionListPage />);
    expect(screen.getByText(/Page \d+ of \d+/)).toBeInTheDocument();
  });

  it('shows clear filters button when filters are active', async () => {
    const user = userEvent.setup();
    renderWithRouter(<TransactionListPage />);
    const searchInput = screen.getByPlaceholderText('Search by account number...');
    await user.type(searchInput, '999');
    expect(screen.getByText('Clear filters')).toBeInTheDocument();
  });

  it('renders showing transactions count text', () => {
    renderWithRouter(<TransactionListPage />);
    expect(screen.getByText(/Showing \d+/)).toBeInTheDocument();
  });
});
