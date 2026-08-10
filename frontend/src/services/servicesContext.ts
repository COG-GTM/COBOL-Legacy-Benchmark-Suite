import { createContext, useContext } from 'react';
import type { HistoryService } from './historyService';

export interface Services {
  history: HistoryService;
}

export const ServicesContext = createContext<Services | null>(null);

function useServices(): Services {
  const ctx = useContext(ServicesContext);
  if (!ctx) {
    throw new Error('Service hooks must be used within a ServicesProvider');
  }
  return ctx;
}

export function useHistoryService(): HistoryService {
  return useServices().history;
}
