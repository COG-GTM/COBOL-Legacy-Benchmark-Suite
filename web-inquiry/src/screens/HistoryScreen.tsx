import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ScreenFrame } from '../components/ScreenFrame';
import { useSession } from '../session/sessionContextValue';
import { usePfKeys } from '../hooks/usePfKeys';
import { getHistory, validateAccount, ACCOUNT_LENGTH } from '../services/inquiryService';
import { formatMoney, formatUnits } from '../utils/format';
import type { HistoryPage } from '../types';

function typeClass(type: string): string {
  const t = type.trim().toUpperCase();
  if (t === 'BUY') return 'buy';
  if (t === 'SELL') return 'sell';
  if (t === 'DIV') return 'div';
  return '';
}

/**
 * Transaction History Inquiry (BMS HISMAP). Mirrors INQHIST:
 *  - account input -> DB2 cursor over POSHIST (ORDER BY TRANS_DATE DESC)
 *  - up to 10 rows per page; Date / Type / Units / Price / Amount columns
 *  - PF7 = Previous page, PF8 = Next page
 *  - ERROR -> route to the System Error screen (ERRMAP)
 */
export function HistoryScreen() {
  const navigate = useNavigate();
  const { comm, setFunction, setAccountNo, setError, clearError } = useSession();
  const [account, setAccount] = useState(comm.accountNo);
  const [page, setPage] = useState<HistoryPage | null>(null);
  const [localError, setLocalError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setFunction('INQH');
  }, [setFunction]);

  const load = useCallback(
    async (acct: string, pageNo: number) => {
      setBusy(true);
      const result = await getHistory(acct, pageNo);
      setBusy(false);
      if (result.status === 'OK') {
        setPage(result.page);
      } else {
        setError(result.errorMsg, result.responseCode);
        navigate('/error');
      }
    },
    [navigate, setError],
  );

  const submit = (event?: React.FormEvent) => {
    event?.preventDefault();
    const validationError = validateAccount(account);
    if (validationError) {
      setPage(null);
      setLocalError(validationError);
      return;
    }
    setLocalError('');
    setAccountNo(account.trim());
    void load(account, 1);
  };

  const goPrev = () => {
    if (page?.hasPrevious) void load(account, page.page - 1);
  };
  const goNext = () => {
    if (page?.hasNext) void load(account, page.page + 1);
  };
  const backToMenu = () => {
    clearError();
    navigate('/menu');
  };

  usePfKeys({ onPf3: backToMenu, onPf7: goPrev, onPf8: goNext });

  return (
    <ScreenFrame
      title="Transaction History Inquiry"
      mapId="HISMAP"
      errorMsg={localError || undefined}
      functionKeys={[
        { pf: 'PF3', label: 'Exit', onClick: backToMenu },
        { pf: 'PF7', label: 'Previous', onClick: goPrev, disabled: !page?.hasPrevious },
        { pf: 'PF8', label: 'Next', onClick: goNext, disabled: !page?.hasNext },
      ]}
    >
      <form className="inquiry-form" onSubmit={submit}>
        <div className="field">
          <label htmlFor="account">Account</label>
          <input
            id="account"
            value={account}
            maxLength={ACCOUNT_LENGTH}
            autoFocus
            placeholder="0000001001"
            onChange={(e) => setAccount(e.target.value)}
          />
        </div>
        <button type="submit" className="btn primary" disabled={busy}>
          {busy ? 'Fetching…' : 'Inquire'}
        </button>
      </form>

      {page && (
        <>
          <div className="history-meta">
            <span>Account {comm.accountNo}</span>
            <span>{page.totalRows} transaction(s), most recent first</span>
          </div>
          <table className="history">
            <thead>
              <tr>
                <th>Date</th>
                <th>Type</th>
                <th className="num">Units</th>
                <th className="num">Price</th>
                <th className="num">Amount</th>
              </tr>
            </thead>
            <tbody>
              {page.rows.length === 0 ? (
                <tr>
                  <td className="empty" colSpan={5}>
                    No transaction history for this account.
                  </td>
                </tr>
              ) : (
                page.rows.map((row, idx) => (
                  <tr key={`${row.date}-${idx}`}>
                    <td>{row.date}</td>
                    <td>
                      <span className={`type-tag ${typeClass(row.type)}`}>
                        {row.type}
                      </span>
                    </td>
                    <td className="num">{formatUnits(row.units)}</td>
                    <td className="num">{formatMoney(row.price)}</td>
                    <td className="num">{formatMoney(row.amount)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>

          <div className="pager">
            <button
              type="button"
              className="btn"
              onClick={goPrev}
              disabled={!page.hasPrevious}
            >
              ‹ PF7 Previous
            </button>
            <span className="page-status">
              Page {page.page} of {page.totalPages}
            </span>
            <button
              type="button"
              className="btn"
              onClick={goNext}
              disabled={!page.hasNext}
            >
              PF8 Next ›
            </button>
          </div>
        </>
      )}
    </ScreenFrame>
  );
}
