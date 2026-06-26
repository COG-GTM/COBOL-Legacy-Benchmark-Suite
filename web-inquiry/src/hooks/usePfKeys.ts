import { useEffect } from 'react';

/**
 * Maps legacy 3270 PF (Program Function) keys to handlers.
 *
 * Legacy navigation hints from INQSET.bms:
 *   PF3 = Exit / Back to menu
 *   PF7 = Previous page
 *   PF8 = Next page
 *
 * Browsers map PF keys to F3/F7/F8. We also accept Enter for the menu/confirm
 * action (the legacy "Press ENTER to continue" on the error screen).
 */
export interface PfKeyHandlers {
  onPf3?: () => void;
  onPf7?: () => void;
  onPf8?: () => void;
  onEnter?: () => void;
}

export function usePfKeys(handlers: PfKeyHandlers): void {
  useEffect(() => {
    const handler = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      const isTyping =
        target?.tagName === 'INPUT' || target?.tagName === 'TEXTAREA';

      switch (event.key) {
        case 'F3':
          if (handlers.onPf3) {
            event.preventDefault();
            handlers.onPf3();
          }
          break;
        case 'F7':
          if (handlers.onPf7) {
            event.preventDefault();
            handlers.onPf7();
          }
          break;
        case 'F8':
          if (handlers.onPf8) {
            event.preventDefault();
            handlers.onPf8();
          }
          break;
        case 'Enter':
          // Don't hijack Enter while the user is typing in a field/form.
          if (handlers.onEnter && !isTyping) {
            event.preventDefault();
            handlers.onEnter();
          }
          break;
        default:
          break;
      }
    };

    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [handlers]);
}
