import { useState } from 'react';
import AccountInput from '../components/AccountInput';
import ErrorPanel from '../components/ErrorPanel';
import { fetchHistory, HistoryRecord } from '../api/client';

const typeLabels: Record<string, string> = { PT: 'Portfolio', PS: 'Position', TR: 'Transaction' };

const formatDate = (d: string) => `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6, 8)}`;

/** HISMAP (ROW1..ROW10 replaced by a data table with PF7/PF8-equivalent paging). */
export default function HistoryPage() {
  const [account, setAccount] = useState('');
  const [rows, setRows] = useState<HistoryRecord[]>([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [error, setError] = useState('');

  const load = async (targetPage: number) => {
    const res = await fetchHistory(account.trim(), targetPage);
    if (!res.rows.length && res.commarea.inqcomResponseCode !== 0) {
      setRows([]);
      setTotalPages(0);
      setError(res.commarea.inqcomErrorMsg.trim());
      return;
    }
    setError('');
    setRows(res.rows);
    setPage(res.page);
    setTotalPages(res.totalPages);
  };

  return (
    <section className="panel">
      <h1>Transaction History Inquiry</h1>
      <AccountInput value={account} onChange={setAccount} onSubmit={() => load(1)} />
      {error && <ErrorPanel code="INQH12" details={error} />}
      {rows.length > 0 && (
        <>
          <table className="history-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Type</th>
                <th>Units</th>
                <th>Price</th>
                <th>Amount</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={`${r.histDate}-${r.histSeqNo}`}>
                  <td className="data-value">{formatDate(r.histDate)}</td>
                  <td className="data-value">{typeLabels[r.histRecordType] ?? r.histRecordType}</td>
                  <td className="data-value num">{r.histUnits.toLocaleString()}</td>
                  <td className="data-value num">{r.histPrice.toFixed(4)}</td>
                  <td className="data-value num">{r.histAmount.toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="pager">
            <button type="button" disabled={page <= 1} onClick={() => load(page - 1)}>
              PF7 Previous
            </button>
            <span>
              Page {page} of {totalPages}
            </span>
            <button type="button" disabled={page >= totalPages} onClick={() => load(page + 1)}>
              PF8 Next
            </button>
          </div>
        </>
      )}
    </section>
  );
}
