import { createContext } from 'react';

interface InlineErrors {
  [field: string]: string;
}

interface ModalState {
  open: boolean;
  code: string;
  details: string;
}

export interface ErrorContextValue {
  inlineErrors: InlineErrors;
  setInlineError: (field: string, message: string) => void;
  clearInlineError: (field: string) => void;
  modal: ModalState;
  showErrorModal: (code: string, details: string) => void;
  dismissErrorModal: () => void;
  offline: boolean;
  setOffline: (value: boolean) => void;
}

export const ErrorContext = createContext<ErrorContextValue | null>(null);
