import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ScreenFrame } from '../components/ScreenFrame';
import { useSession } from '../session/sessionContextValue';
import { usePfKeys } from '../hooks/usePfKeys';
import type { InqFunction } from '../types';

/**
 * Main menu (BMS MENMAP). Three options matching the legacy screen:
 *   1. Portfolio Position Inquiry  -> INQP
 *   2. Transaction History         -> INQH
 *   3. Exit                        -> EXIT
 *
 * Selecting an option sets the INQCOM function code and routes accordingly,
 * mirroring the EVALUATE in INQONLN P100-PROCESS-REQUEST.
 */
const OPTIONS: { num: number; label: string; fn: InqFunction; to: string }[] = [
  { num: 1, label: 'Portfolio Position Inquiry', fn: 'INQP', to: '/portfolio' },
  { num: 2, label: 'Transaction History', fn: 'INQH', to: '/history' },
  { num: 3, label: 'Exit', fn: 'EXIT', to: '/exit' },
];

export function MenuScreen() {
  const navigate = useNavigate();
  const { comm, setFunction, clearError } = useSession();

  useEffect(() => {
    setFunction('MENU');
  }, [setFunction]);

  const select = (fn: InqFunction, to: string) => {
    clearError();
    setFunction(fn);
    navigate(to);
  };

  // PF3 on the menu exits the session (SET SESSION-TERMINATED).
  usePfKeys({ onPf3: () => navigate('/exit') });

  return (
    <ScreenFrame
      title="Portfolio Management System"
      mapId="MENMAP"
      errorMsg={comm.errorMsg || undefined}
      functionKeys={[{ pf: 'PF3', label: 'Exit', onClick: () => navigate('/exit') }]}
    >
      <p className="menu-prompt">Select Option:</p>
      <div className="menu-list">
        {OPTIONS.map((opt) => (
          <button
            key={opt.num}
            type="button"
            className="menu-option"
            onClick={() => select(opt.fn, opt.to)}
          >
            <span className="num">{opt.num}</span>
            <span>{opt.label}</span>
            <span className="chev" aria-hidden>
              ›
            </span>
          </button>
        ))}
      </div>
    </ScreenFrame>
  );
}
