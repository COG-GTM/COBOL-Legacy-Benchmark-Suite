import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { AccountForm } from '../components/AccountForm';
import { Message } from '../components/Message';
import { ApiError, getTransactions } from '../api/client';
import {
  TRANSACTION_TYPE_LABELS,
  type TransactionsResponse,
} from '../types/portfolio';
import { formatCurrency, formatUnits } from '../lib/format';

/**
 * Transaction History — replaces BMS HISMAP / program INQHIST.
 * Enter an account number; displays a paginated table of date, type, units,
 * price and amount. Page size is 10 to mirror the COBOL fetch of 10 rows
 * (WS-HISTORY-ENTRY OCCURS 10 TIMES); PF7/PF8 scrolling becomes Prev/Next.
 */
export function TransactionHistoryView() {
  const [searchParams, setSearchParams] = useSearchParams();
  const account = searchParams.get('account') ?? '';
  const page = Math.max(1, Number.parseInt(searchParams.get('page') ?? '1', 10) || 1);

  const [data, setData] = useState<TransactionsResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!account) {
      setData(null);
      setError(null);
      return;
    }

    const controller = new AbortController();
    setLoading(true);
    setError(null);

    getTransactions(account, page, controller.signal)
      .then(setData)
      .catch((err: unknown) => {
        if (err instanceof DOMException && err.name === 'AbortError') return;
        setData(null);
        setError(err instanceof ApiError ? err.message : 'Unexpected error');
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [account, page]);

  const goToPage = (nextPage: number) => {
    setSearchParams({ account, page: String(nextPage) });
  };

  return (
    <section className="card">
      <h1 className="view-title">Transaction History Inquiry</h1>
      <AccountForm
        label="Account:"
        initialValue={account}
        onSubmit={(value) => setSearchParams({ account: value, page: '1' })}
      />

      {loading && <Message variant="info">Reading history…</Message>}
      {error && <Message variant="error">{error}</Message>}

      {data && !error && (
        <>
          {data.total === 0 ? (
            <Message variant="info">No transaction history for this account.</Message>
          ) : (
            <>
              <div className="table-wrap">
                <table className="data-table">
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
                    {data.transactions.map((txn, index) => (
                      <tr key={`${txn.transDate}-${index}`}>
                        <td className="mono">{txn.transDate}</td>
                        <td>{TRANSACTION_TYPE_LABELS[txn.transType]}</td>
                        <td className="num">{formatUnits(txn.transUnits, 2)}</td>
                        <td className="num">{formatCurrency(txn.transPrice)}</td>
                        <td className="num">{formatCurrency(txn.transAmount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="pager">
                <button
                  type="button"
                  className="btn btn--ghost"
                  disabled={data.page <= 1}
                  onClick={() => goToPage(data.page - 1)}
                >
                  ◄ PF7 Previous
                </button>
                <span className="pager__status">
                  Page {data.page} of {data.totalPages} · {data.total} rows
                </span>
                <button
                  type="button"
                  className="btn btn--ghost"
                  disabled={data.page >= data.totalPages}
                  onClick={() => goToPage(data.page + 1)}
                >
                  PF8 Next ►
                </button>
              </div>
            </>
          )}
        </>
      )}
    </section>
  );
}
