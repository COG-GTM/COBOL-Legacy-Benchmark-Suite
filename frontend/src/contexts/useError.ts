import { useContext } from 'react';
import { ErrorContext } from './errorContextDef';
import type { ErrorContextValue } from './errorContextDef';

export function useError(): ErrorContextValue {
  const ctx = useContext(ErrorContext);
  if (!ctx) {
    throw new Error('useError must be used within an <ErrorProvider>');
  }
  return ctx;
}
