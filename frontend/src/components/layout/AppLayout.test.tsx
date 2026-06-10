import { render, screen } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { AppLayout } from './AppLayout';
import { AuthProvider } from '@/context/AuthContext';
import { PortfolioProvider } from '@/context/PortfolioContext';
import { ErrorProvider } from '@/context/ErrorContext';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

function renderAppLayout({ authenticated = false, route = '/' } = {}) {
  if (authenticated) {
    sessionStorage.setItem('auth', JSON.stringify({ isAuthenticated: true, userId: 'ADMIN' }));
  }
  return render(
    <ErrorProvider>
      <AuthProvider>
        <PortfolioProvider>
          <MemoryRouter initialEntries={[route]}>
            <Routes>
              <Route path="/login" element={<div>Login Page</div>} />
              <Route element={<AppLayout />}>
                <Route path="/" element={<div>Dashboard Content</div>} />
              </Route>
            </Routes>
          </MemoryRouter>
        </PortfolioProvider>
      </AuthProvider>
    </ErrorProvider>,
  );
}

describe('AppLayout', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('redirects to login when not authenticated', () => {
    renderAppLayout({ authenticated: false });
    expect(screen.getByText('Login Page')).toBeInTheDocument();
    expect(screen.queryByText('Dashboard Content')).not.toBeInTheDocument();
  });

  it('renders layout with outlet when authenticated', () => {
    renderAppLayout({ authenticated: true });
    expect(screen.getByText('Dashboard Content')).toBeInTheDocument();
    expect(screen.getByText('Investment Portfolio Manager')).toBeInTheDocument();
  });

  it('renders sidebar when authenticated', () => {
    renderAppLayout({ authenticated: true });
    expect(screen.getByLabelText('Collapse sidebar')).toBeInTheDocument();
  });
});
