import { useMemo, type ReactNode } from 'react';
import { MockPortfolioService } from './mockPortfolioService';
import { MockPositionService } from './mockPositionService';
import { MockTransactionService } from './mockTransactionService';
import { ServicesContext, type Services } from './servicesContext';

/**
 * Provides the application's service layer. By default it wires up the
 * in-memory mock services; tests (or a future production build) can inject
 * alternative implementations via the `services` prop.
 */
export function ServicesProvider({
  children,
  services,
}: {
  children: ReactNode;
  services?: Partial<Services>;
}) {
  const value = useMemo<Services>(
    () => ({
      portfolios: services?.portfolios ?? new MockPortfolioService(),
      positions: services?.positions ?? new MockPositionService(),
      transactions: services?.transactions ?? new MockTransactionService(),
    }),
    [services],
  );
  return (
    <ServicesContext.Provider value={value}>
      {children}
    </ServicesContext.Provider>
  );
}
