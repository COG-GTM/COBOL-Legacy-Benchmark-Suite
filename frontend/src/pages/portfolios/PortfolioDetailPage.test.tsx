import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { PortfolioDetailPage } from './PortfolioDetailPage';
import { ErrorProvider } from '@/context/ErrorContext';
import { AuthProvider } from '@/context/AuthContext';
import { PortfolioProvider } from '@/context/PortfolioContext';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

function renderDetailPage(id: string) {
  return render(
    <ErrorProvider>
      <AuthProvider>
        <PortfolioProvider>
          <MemoryRouter initialEntries={[`/portfolios/${id}`]}>
            <Routes>
              <Route path="/portfolios/:id" element={<PortfolioDetailPage />} />
              <Route path="/portfolios" element={<div>Portfolio List</div>} />
              <Route path="/portfolios/:id/edit" element={<div>Edit Page</div>} />
            </Routes>
          </MemoryRouter>
        </PortfolioProvider>
      </AuthProvider>
    </ErrorProvider>,
  );
}

describe('PortfolioDetailPage', () => {
  it('renders portfolio details for valid ID', () => {
    renderDetailPage('PORT0001');
    expect(screen.getAllByText('Growth Equity Fund').length).toBeGreaterThanOrEqual(1);
    // PORT0001 appears in both the description and the detail field
    expect(screen.getAllByText('PORT0001').length).toBeGreaterThanOrEqual(1);
  });

  it('renders edit and delete buttons', () => {
    renderDetailPage('PORT0001');
    expect(screen.getByText('Edit')).toBeInTheDocument();
    // "Delete" button in the header
    expect(screen.getAllByText('Delete').length).toBeGreaterThanOrEqual(1);
  });

  it('shows not found for invalid ID', () => {
    renderDetailPage('INVALID');
    expect(screen.getByText('Portfolio not found')).toBeInTheDocument();
    expect(screen.getByText('Back to Portfolios')).toBeInTheDocument();
  });

  it('shows delete confirmation dialog', async () => {
    const user = userEvent.setup();
    renderDetailPage('PORT0001');
    // Click the Delete button in the page header
    const deleteButtons = screen.getAllByText('Delete');
    await user.click(deleteButtons[0]);
    expect(screen.getByText(/Are you sure you want to delete/)).toBeInTheDocument();
  });

  it('deletes portfolio and navigates to list', async () => {
    const user = userEvent.setup();
    renderDetailPage('PORT0001');
    // Click the Delete button to open the dialog
    const deleteButtons = screen.getAllByText('Delete');
    await user.click(deleteButtons[0]);
    // Click the Delete button in the confirm dialog
    const allDeleteButtons = screen.getAllByRole('button', { name: 'Delete' });
    await user.click(allDeleteButtons[allDeleteButtons.length - 1]);
    expect(screen.getByText('Portfolio List')).toBeInTheDocument();
  });

  it('renders portfolio detail fields', () => {
    renderDetailPage('PORT0001');
    expect(screen.getByText('Portfolio ID')).toBeInTheDocument();
    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Status')).toBeInTheDocument();
    expect(screen.getByText('Total Value')).toBeInTheDocument();
    expect(screen.getByText('Create Date')).toBeInTheDocument();
  });
});
