import { useMemo, type ReactNode } from 'react';
import { MockHistoryService } from './mockHistoryService';
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
      history: services?.history ?? new MockHistoryService(),
    }),
    [services],
  );
  return (
    <ServicesContext.Provider value={value}>
      {children}
    </ServicesContext.Provider>
  );
}
