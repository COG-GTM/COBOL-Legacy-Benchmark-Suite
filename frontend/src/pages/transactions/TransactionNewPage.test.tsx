import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { TransactionNewPage } from './TransactionNewPage';
import { renderWithProviders } from '@/test/helpers';

describe('TransactionNewPage', () => {
  it('renders the page title and step indicator', () => {
    renderWithProviders(<TransactionNewPage />);
    expect(screen.getByText('New Transaction')).toBeInTheDocument();
    expect(screen.getByText('Enter Details')).toBeInTheDocument();
    expect(screen.getByText('Review & Confirm')).toBeInTheDocument();
  });

  it('renders form fields for Buy transaction type', () => {
    renderWithProviders(<TransactionNewPage />);
    expect(screen.getByText('Transaction Type')).toBeInTheDocument();
    expect(screen.getByText('Account Number')).toBeInTheDocument();
    expect(screen.getByText('Fund ID')).toBeInTheDocument();
    expect(screen.getByText('Units')).toBeInTheDocument();
    expect(screen.getByText('Price per Unit')).toBeInTheDocument();
  });

  it('shows validation errors for empty required fields', async () => {
    const user = userEvent.setup();
    renderWithProviders(<TransactionNewPage />);
    await user.click(screen.getByRole('button', { name: /next/i }));
    expect(screen.getByText('Account number must be exactly 9 digits')).toBeInTheDocument();
    expect(screen.getByText('Fund ID must be exactly 6 characters')).toBeInTheDocument();
  });

  it('validates account number must be 9 digits', async () => {
    const user = userEvent.setup();
    renderWithProviders(<TransactionNewPage />);
    await user.type(screen.getByPlaceholderText('123456789'), '123');
    await user.click(screen.getByRole('button', { name: /next/i }));
    expect(screen.getByText('Account number must be exactly 9 digits')).toBeInTheDocument();
  });

  it('validates fund ID must be 6 characters', async () => {
    const user = userEvent.setup();
    renderWithProviders(<TransactionNewPage />);
    await user.type(screen.getByPlaceholderText('GRWEQF'), 'AB');
    await user.click(screen.getByRole('button', { name: /next/i }));
    expect(screen.getByText('Fund ID must be exactly 6 characters')).toBeInTheDocument();
  });

  it('validates units must be positive', async () => {
    const user = userEvent.setup();
    renderWithProviders(<TransactionNewPage />);
    await user.type(screen.getByPlaceholderText('123456789'), '100000001');
    await user.type(screen.getByPlaceholderText('GRWEQF'), 'GRWEQF');
    await user.type(screen.getByPlaceholderText('0.000'), '-1');
    await user.type(screen.getByPlaceholderText('0.00'), '10');
    await user.click(screen.getByRole('button', { name: /next/i }));
    expect(screen.getByText('Units must be a positive number')).toBeInTheDocument();
  });

  it('advances to review step with valid data', async () => {
    const user = userEvent.setup();
    renderWithProviders(<TransactionNewPage />);
    await user.type(screen.getByPlaceholderText('123456789'), '100000001');
    await user.type(screen.getByPlaceholderText('GRWEQF'), 'GRWEQF');
    await user.type(screen.getByPlaceholderText('0.000'), '100');
    await user.type(screen.getByPlaceholderText('0.00'), '50');
    await user.click(screen.getByRole('button', { name: /next/i }));
    expect(screen.getByText('Review & Confirm')).toBeInTheDocument();
    expect(screen.getByText('Confirm Transaction')).toBeInTheDocument();
    expect(screen.getByText('100000001')).toBeInTheDocument();
  });

  it('can go back from review to entry step', async () => {
    const user = userEvent.setup();
    renderWithProviders(<TransactionNewPage />);
    await user.type(screen.getByPlaceholderText('123456789'), '100000001');
    await user.type(screen.getByPlaceholderText('GRWEQF'), 'GRWEQF');
    await user.type(screen.getByPlaceholderText('0.000'), '100');
    await user.type(screen.getByPlaceholderText('0.00'), '50');
    await user.click(screen.getByRole('button', { name: /next/i }));
    await user.click(screen.getByRole('button', { name: /back to edit/i }));
    expect(screen.getByPlaceholderText('123456789')).toBeInTheDocument();
  });

  it('shows success after confirming transaction', async () => {
    const user = userEvent.setup();
    renderWithProviders(<TransactionNewPage />);
    await user.type(screen.getByPlaceholderText('123456789'), '100000001');
    await user.type(screen.getByPlaceholderText('GRWEQF'), 'GRWEQF');
    await user.type(screen.getByPlaceholderText('0.000'), '100');
    await user.type(screen.getByPlaceholderText('0.00'), '50');
    await user.click(screen.getByRole('button', { name: /next/i }));
    await user.click(screen.getByRole('button', { name: /confirm transaction/i }));
    expect(screen.getByText('Transaction Submitted')).toBeInTheDocument();
    expect(screen.getByText(/Transaction ID:/)).toBeInTheDocument();
  });

  it('allows starting a new transaction after submission', async () => {
    const user = userEvent.setup();
    renderWithProviders(<TransactionNewPage />);
    await user.type(screen.getByPlaceholderText('123456789'), '100000001');
    await user.type(screen.getByPlaceholderText('GRWEQF'), 'GRWEQF');
    await user.type(screen.getByPlaceholderText('0.000'), '100');
    await user.type(screen.getByPlaceholderText('0.00'), '50');
    await user.click(screen.getByRole('button', { name: /next/i }));
    await user.click(screen.getByRole('button', { name: /confirm transaction/i }));
    await user.click(screen.getByRole('button', { name: /new transaction/i }));
    expect(screen.getByText('Enter Details')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('123456789')).toHaveValue('');
  });

  it('disables units and price fields when Fee is selected', async () => {
    const user = userEvent.setup();
    renderWithProviders(<TransactionNewPage />);
    const select = screen.getByDisplayValue('Buy');
    await user.selectOptions(select, 'FE');
    const unitsInput = screen.getByPlaceholderText('0.000');
    expect(unitsInput).toBeDisabled();
  });

  it('shows amount input for Fee type', async () => {
    const user = userEvent.setup();
    renderWithProviders(<TransactionNewPage />);
    const select = screen.getByDisplayValue('Buy');
    await user.selectOptions(select, 'FE');
    // The amount field becomes editable for fees
    expect(screen.getByText('Amount')).toBeInTheDocument();
  });

  it('validates amount for Fee type', async () => {
    const user = userEvent.setup();
    renderWithProviders(<TransactionNewPage />);
    const select = screen.getByDisplayValue('Buy');
    await user.selectOptions(select, 'FE');
    await user.type(screen.getByPlaceholderText('123456789'), '100000001');
    await user.type(screen.getByPlaceholderText('GRWEQF'), 'GRWEQF');
    await user.click(screen.getByRole('button', { name: /next/i }));
    expect(screen.getByText('Amount must be a positive number')).toBeInTheDocument();
  });
});
