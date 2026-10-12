import { useEffect, useRef } from 'react';

export interface ShortcutHandlers {
  /** PF3: Esc or Alt+3 */
  onBack?: () => void;
  /** PF7: PageUp */
  onPrevious?: () => void;
  /** PF8: PageDown */
  onNext?: () => void;
}

/** Optional keyboard equivalents of the legacy PF keys (section 3.2). Buttons remain the primary control. */
export function useShortcuts(handlers: ShortcutHandlers) {
  const ref = useRef(handlers);
  ref.current = handlers;

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.defaultPrevented || event.ctrlKey || event.metaKey) return;
      const h = ref.current;
      if (h.onBack && (event.key === 'Escape' || (event.altKey && (event.key === '3' || event.code === 'Digit3')))) {
        event.preventDefault();
        h.onBack();
      } else if (h.onPrevious && event.key === 'PageUp' && !event.altKey) {
        event.preventDefault();
        h.onPrevious();
      } else if (h.onNext && event.key === 'PageDown' && !event.altKey) {
        event.preventDefault();
        h.onNext();
      }
    }
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, []);
}
