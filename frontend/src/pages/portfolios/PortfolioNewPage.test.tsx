import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { PortfolioNewPage } from './PortfolioNewPage';
import { ErrorProvider } from '@/context/ErrorContext';
import { AuthProvider } from '@/context/AuthContext';
import { PortfolioProvider } from '@/context/PortfolioContext';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

function renderNewPage() {
  return render(
    <ErrorProvider>
      <AuthProvider>
        <PortfolioProvider>
          <MemoryRouter initialEntries={['/portfolios/new']}>
            <Routes>
              <Route path="/portfolios/new" element={<PortfolioNewPage />} />
              <Route path="/portfolios" element={<div>Portfolio List</div>} />
            </Routes>
          </MemoryRouter>
        </PortfolioProvider>
      </AuthProvider>
    </ErrorProvider>,
  );
}

describe('PortfolioNewPage', () => {
  it('renders the page title', () => {
    renderNewPage();
    expect(screen.getByRole('heading', { name: 'Create Portfolio' })).toBeInTheDocument();
    expect(screen.getByText('Add a new investment portfolio')).toBeInTheDocument();
  });

  it('renders form fields', () => {
    renderNewPage();
    expect(screen.getByLabelText('Portfolio ID')).toBeInTheDocument();
    expect(screen.getByLabelText('Name')).toBeInTheDocument();
    expect(screen.getByLabelText('Status')).toBeInTheDocument();
    expect(screen.getByLabelText('Total Value')).toBeInTheDocument();
  });

  it('shows validation errors for empty form', async () => {
    const user = userEvent.setup();
    renderNewPage();
    await user.click(screen.getByRole('button', { name: /create/i }));
    expect(screen.getByText(/Portfolio ID must start with "PORT"/)).toBeInTheDocument();
    expect(screen.getByText('Name is required')).toBeInTheDocument();
  });

  it('validates portfolio ID format', async () => {
    const user = userEvent.setup();
    renderNewPage();
    await user.type(screen.getByLabelText('Portfolio ID'), 'BAD');
    await user.click(screen.getByRole('button', { name: /create/i }));
    expect(screen.getByText(/Portfolio ID must start with "PORT"/)).toBeInTheDocument();
  });

  it('validates invalid total value', async () => {
    const user = userEvent.setup();
    renderNewPage();
    await user.type(screen.getByLabelText('Portfolio ID'), 'PORT12345');
    await user.type(screen.getByLabelText('Name'), 'Test Fund');
    await user.type(screen.getByLabelText('Total Value'), 'abc');
    await user.click(screen.getByRole('button', { name: /create/i }));
    expect(screen.getByText('Total value must be a valid positive number')).toBeInTheDocument();
  });

  it('creates portfolio with valid data and navigates', async () => {
    const user = userEvent.setup();
    renderNewPage();
    await user.type(screen.getByLabelText('Portfolio ID'), 'PORT99999');
    await user.type(screen.getByLabelText('Name'), 'New Test Fund');
    await user.type(screen.getByLabelText('Total Value'), '50000');
    await user.click(screen.getByRole('button', { name: /create/i }));
    expect(screen.getByText('Portfolio List')).toBeInTheDocument();
  });
});
