import { render } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { ErrorProvider } from '@/context/ErrorContext';
import { AuthProvider } from '@/context/AuthContext';
import { PortfolioProvider } from '@/context/PortfolioContext';

export function renderWithProviders(ui: ReactNode, { route = '/' } = {}) {
  return render(
    <ErrorProvider>
      <AuthProvider>
        <PortfolioProvider>
          <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
        </PortfolioProvider>
      </AuthProvider>
    </ErrorProvider>,
  );
}

export function renderWithRouter(ui: ReactNode, { route = '/' } = {}) {
  return render(<MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>);
}
