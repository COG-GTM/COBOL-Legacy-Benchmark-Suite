import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { NAV_ITEMS } from '../nav/navigation';

/** Window (ms) after the `g` leader key in which a target key is accepted. */
const LEADER_TIMEOUT_MS = 1200;

function isEditableTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) {
    return false;
  }
  const tag = target.tagName;
  return (
    tag === 'INPUT' ||
    tag === 'TEXTAREA' ||
    tag === 'SELECT' ||
    target.isContentEditable
  );
}

/**
 * Optional keyboard navigation: press `g` then a section key (d/p/t/h/r) to
 * jump between sections — a modern stand-in for the legacy PF-key flow.
 * Shortcuts are ignored while typing in a form field.
 */
export function useKeyboardShortcuts(): void {
  const navigate = useNavigate();

  useEffect(() => {
    let leaderActive = false;
    let leaderTimer: ReturnType<typeof setTimeout> | undefined;

    const clearLeader = () => {
      leaderActive = false;
      if (leaderTimer) {
        clearTimeout(leaderTimer);
        leaderTimer = undefined;
      }
    };

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.metaKey || event.ctrlKey || event.altKey) {
        return;
      }
      if (isEditableTarget(event.target)) {
        return;
      }

      if (!leaderActive) {
        if (event.key === 'g') {
          leaderActive = true;
          leaderTimer = setTimeout(clearLeader, LEADER_TIMEOUT_MS);
        }
        return;
      }

      const match = NAV_ITEMS.find((item) => item.shortcut === event.key);
      clearLeader();
      if (match) {
        event.preventDefault();
        navigate(match.path);
      }
    };

    window.addEventListener('keydown', onKeyDown);
    return () => {
      window.removeEventListener('keydown', onKeyDown);
      clearLeader();
    };
  }, [navigate]);
}
