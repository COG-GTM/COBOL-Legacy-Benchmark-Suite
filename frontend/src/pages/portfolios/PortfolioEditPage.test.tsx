import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { PortfolioEditPage } from './PortfolioEditPage';
import { ErrorProvider } from '@/context/ErrorContext';
import { AuthProvider } from '@/context/AuthContext';
import { PortfolioProvider } from '@/context/PortfolioContext';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

function renderEditPage(id: string) {
  return render(
    <ErrorProvider>
      <AuthProvider>
        <PortfolioProvider>
          <MemoryRouter initialEntries={[`/portfolios/${id}/edit`]}>
            <Routes>
              <Route path="/portfolios/:id/edit" element={<PortfolioEditPage />} />
              <Route path="/portfolios/:id" element={<div>Detail Page</div>} />
              <Route path="/portfolios" element={<div>Portfolio List</div>} />
            </Routes>
          </MemoryRouter>
        </PortfolioProvider>
      </AuthProvider>
    </ErrorProvider>,
  );
}

describe('PortfolioEditPage', () => {
  it('renders the page title', () => {
    renderEditPage('PORT0001');
    expect(screen.getByText('Edit Portfolio')).toBeInTheDocument();
    expect(screen.getByText('Editing PORT0001')).toBeInTheDocument();
  });

  it('pre-fills form with portfolio data', () => {
    renderEditPage('PORT0001');
    expect(screen.getByDisplayValue('Growth Equity Fund')).toBeInTheDocument();
  });

  it('disables portfolio ID field', () => {
    renderEditPage('PORT0001');
    expect(screen.getByDisplayValue('PORT0001')).toBeDisabled();
  });

  it('shows not found for invalid ID', () => {
    renderEditPage('INVALID');
    expect(screen.getByText('Portfolio not found')).toBeInTheDocument();
    expect(screen.getByText('Back to Portfolios')).toBeInTheDocument();
  });

  it('validates empty name', async () => {
    const user = userEvent.setup();
    renderEditPage('PORT0001');
    const nameInput = screen.getByDisplayValue('Growth Equity Fund');
    await user.clear(nameInput);
    await user.click(screen.getByRole('button', { name: /save/i }));
    expect(screen.getByText('Name is required')).toBeInTheDocument();
  });

  it('saves changes and navigates to detail page', async () => {
    const user = userEvent.setup();
    renderEditPage('PORT0001');
    const nameInput = screen.getByDisplayValue('Growth Equity Fund');
    await user.clear(nameInput);
    await user.type(nameInput, 'Updated Fund');
    await user.click(screen.getByRole('button', { name: /save/i }));
    expect(screen.getByText('Detail Page')).toBeInTheDocument();
  });
});
