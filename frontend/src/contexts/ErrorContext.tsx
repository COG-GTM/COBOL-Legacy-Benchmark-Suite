import { useCallback, useState, type ReactNode } from 'react';
import { ErrorContext } from './errorContextDef';

interface InlineErrors {
  [field: string]: string;
}

interface ModalState {
  open: boolean;
  code: string;
  details: string;
}

const INITIAL_MODAL: ModalState = { open: false, code: '', details: '' };

export function ErrorProvider({ children }: { children: ReactNode }) {
  const [inlineErrors, setInlineErrors] = useState<InlineErrors>({});
  const [modal, setModal] = useState<ModalState>(INITIAL_MODAL);
  const [offline, setOfflineState] = useState(false);

  const setInlineError = useCallback((field: string, message: string) => {
    setInlineErrors((prev) => ({ ...prev, [field]: message }));
  }, []);

  const clearInlineError = useCallback((field: string) => {
    setInlineErrors((prev) => {
      const next = { ...prev };
      delete next[field];
      return next;
    });
  }, []);

  const showErrorModal = useCallback((code: string, details: string) => {
    setModal({ open: true, code, details });
  }, []);

  const dismissErrorModal = useCallback(() => {
    setModal(INITIAL_MODAL);
  }, []);

  const setOffline = useCallback((value: boolean) => {
    setOfflineState(value);
  }, []);

  return (
    <ErrorContext.Provider
      value={{
        inlineErrors,
        setInlineError,
        clearInlineError,
        modal,
        showErrorModal,
        dismissErrorModal,
        offline,
        setOffline,
      }}
    >
      {children}
    </ErrorContext.Provider>
  );
}
