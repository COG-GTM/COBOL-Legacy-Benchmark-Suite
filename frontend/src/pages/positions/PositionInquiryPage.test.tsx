import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { PositionInquiryPage } from './PositionInquiryPage';
import { renderWithRouter } from '@/test/helpers';

describe('PositionInquiryPage', () => {
  it('renders the page title', () => {
    renderWithRouter(<PositionInquiryPage />);
    expect(screen.getByText('Position Inquiry')).toBeInTheDocument();
  });

  it('renders the search input', () => {
    renderWithRouter(<PositionInquiryPage />);
    expect(screen.getByPlaceholderText('Enter account number (e.g. 100000001)')).toBeInTheDocument();
  });

  it('renders search button', () => {
    renderWithRouter(<PositionInquiryPage />);
    expect(screen.getByRole('button', { name: 'Search' })).toBeInTheDocument();
  });

  it('shows empty state before search', () => {
    renderWithRouter(<PositionInquiryPage />);
    expect(screen.getByText('No account selected')).toBeInTheDocument();
    expect(screen.getByText('Enter an account number above to view positions.')).toBeInTheDocument();
  });

  it('shows positions when valid account is searched', async () => {
    const user = userEvent.setup();
    renderWithRouter(<PositionInquiryPage />);
    const input = screen.getByPlaceholderText('Enter account number (e.g. 100000001)');
    await user.type(input, '100000001');
    await user.click(screen.getByRole('button', { name: 'Search' }));
    expect(screen.getByText(/Showing \d+ position/)).toBeInTheDocument();
  });

  it('shows no results for unknown account', async () => {
    const user = userEvent.setup();
    renderWithRouter(<PositionInquiryPage />);
    const input = screen.getByPlaceholderText('Enter account number (e.g. 100000001)');
    await user.type(input, '999999999');
    await user.click(screen.getByRole('button', { name: 'Search' }));
    expect(screen.getByText('Account not found: 999999999')).toBeInTheDocument();
  });

  it('searches on Enter key', async () => {
    const user = userEvent.setup();
    renderWithRouter(<PositionInquiryPage />);
    const input = screen.getByPlaceholderText('Enter account number (e.g. 100000001)');
    await user.type(input, '100000001{Enter}');
    expect(screen.getByText(/Showing \d+ position/)).toBeInTheDocument();
  });
});
