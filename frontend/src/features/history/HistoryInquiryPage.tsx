import { useCallback, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Pagination } from '../../components/Pagination';
import { useHistoryService } from '../../services/servicesContext';
import {
  HISTORY_ACTION_LABELS,
  HISTORY_RECORD_TYPE_LABELS,
  HISTORY_RECORD_TYPES,
  historyKey,
  type HistoryRecord,
  type HistoryRecordType,
} from '../../types/history';
import { TRANSACTION_TYPE_LABELS } from '../../types/transaction';
import { downloadCsv } from '../../utils/csv';
import { formatCobolDate, toCobolDate } from '../../utils/date';
import { formatCurrency, formatQuantity } from '../../utils/decimal';
import { historyCsvFilename, historyToCsv } from './historyCsv';

/** Matches the ten history rows the legacy HISMAP screen showed at a time. */
const PAGE_SIZE = 10;

interface Search {
  accountNo: string;
  startDate: string;
  endDate: string;
  recordType: HistoryRecordType | '';
}

export function HistoryInquiryPage() {
  const service = useHistoryService();
  const navigate = useNavigate();

  const [accountInput, setAccountInput] = useState('');
  const [startInput, setStartInput] = useState('');
  const [endInput, setEndInput] = useState('');
  const [recordTypeInput, setRecordTypeInput] = useState<
    HistoryRecordType | ''
  >('');

  const [records, setRecords] = useState<HistoryRecord[]>([]);
  const [search, setSearch] = useState<Search | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  // Guards against out-of-order responses: only the latest search may commit.
  const requestId = useRef(0);

  const load = useCallback(
    async (params: Search) => {
      const id = ++requestId.current;
      setLoading(true);
      setError(null);
      setPage(1);
      try {
        const results = await service.listByAccount(params.accountNo, {
          startDate: params.startDate,
          endDate: params.endDate,
          recordType: params.recordType,
        });
        if (id !== requestId.current) return;
        setRecords(results);
        setSearch(params);
      } catch {
        if (id !== requestId.current) return;
        setError('Unable to load history. Please try again.');
      } finally {
        if (id === requestId.current) setLoading(false);
      }
    },
    [service],
  );

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const accountNo = accountInput.trim();
    if (!accountNo) {
      setError('Enter an account number to search.');
      return;
    }
    const startDate = toCobolDate(startInput);
    const endDate = toCobolDate(endInput);
    if (startDate && endDate && startDate > endDate) {
      setError('The start date must not be after the end date.');
      return;
    }
    void load({ accountNo, startDate, endDate, recordType: recordTypeInput });
  };

  const onReset = () => {
    requestId.current += 1;
    setAccountInput('');
    setStartInput('');
    setEndInput('');
    setRecordTypeInput('');
    setRecords([]);
    setSearch(null);
    setError(null);
    setLoading(false);
    setPage(1);
  };

  const onExport = () => {
    if (!search) return;
    downloadCsv(
      historyCsvFilename(search.accountNo, search.startDate, search.endDate),
      historyToCsv(records),
    );
  };

  const pageCount = Math.max(1, Math.ceil(records.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount);
  const pageRecords = records.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );
  const hasResults = Boolean(search) && !loading && records.length > 0;

  return (
    <section>
      <div className="page-header">
        <div>
          <h1 className="page-header__title">Transaction History Inquiry</h1>
          <p className="page-header__subtitle">
            Search account history (INQHIST / HISTREC)
          </p>
        </div>
        <button
          type="button"
          className="btn btn--ghost"
          onClick={onExport}
          disabled={!hasResults}
        >
          Export CSV
        </button>
      </div>

      <form className="filters" onSubmit={onSubmit} aria-label="Search history">
        <div className="field">
          <label htmlFor="history-account">Account number</label>
          <input
            id="history-account"
            type="text"
            value={accountInput}
            onChange={(e) => setAccountInput(e.target.value)}
            placeholder="e.g. ACCT100001"
          />
        </div>
        <div className="field">
          <label htmlFor="history-start-date">Start date</label>
          <input
            id="history-start-date"
            type="date"
            value={startInput}
            onChange={(e) => setStartInput(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="history-end-date">End date</label>
          <input
            id="history-end-date"
            type="date"
            value={endInput}
            onChange={(e) => setEndInput(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="history-record-type">Record type</label>
          <select
            id="history-record-type"
            value={recordTypeInput}
            onChange={(e) =>
              setRecordTypeInput(e.target.value as HistoryRecordType | '')
            }
          >
            <option value="">All</option>
            {HISTORY_RECORD_TYPES.map((type) => (
              <option key={type} value={type}>
                {HISTORY_RECORD_TYPE_LABELS[type]}
              </option>
            ))}
          </select>
        </div>
        <div className="filters__actions">
          <button type="submit" className="btn btn--primary">
            Search
          </button>
          <button type="button" className="btn btn--ghost" onClick={onReset}>
            Reset
          </button>
        </div>
      </form>

      {error && (
        <div className="alert alert--error" role="alert">
          {error}
        </div>
      )}

      <div className="card">
        {!search ? (
          <p className="state-msg">
            Enter an account number and search to view its history.
          </p>
        ) : loading ? (
          <p className="state-msg">Loading history…</p>
        ) : records.length === 0 ? (
          <p className="state-msg" data-testid="empty-state">
            No history found for account {search.accountNo}.
          </p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Type</th>
                <th>Action</th>
                <th className="num">Units</th>
                <th className="num">Price</th>
                <th className="num">Amount</th>
              </tr>
            </thead>
            <tbody>
              {pageRecords.map((record) => {
                const key = historyKey(record);
                return (
                  <tr
                    key={key}
                    className="table__row--clickable"
                    onClick={() => navigate(`/history/${key}`)}
                  >
                    <td>
                      <Link
                        to={`/history/${key}`}
                        onClick={(e) => e.stopPropagation()}
                      >
                        {formatCobolDate(record.date)}
                      </Link>
                    </td>
                    <td>{typeLabel(record)}</td>
                    <td>{HISTORY_ACTION_LABELS[record.actionCode]}</td>
                    <td className="num">
                      {record.units ? formatQuantity(record.units) : '—'}
                    </td>
                    <td className="num">
                      {record.price
                        ? formatCurrency(record.price, record.currency ?? 'USD', 4)
                        : '—'}
                    </td>
                    <td className="num">
                      {record.amount
                        ? formatCurrency(record.amount, record.currency ?? 'USD')
                        : '—'}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {hasResults && (
        <>
          <Pagination
            page={currentPage}
            pageCount={pageCount}
            onPageChange={setPage}
          />
          <p className="result-count">
            {records.length} history record{records.length === 1 ? '' : 's'}
          </p>
        </>
      )}
    </section>
  );
}

/**
 * The legacy HISMAP "Type" column carried the transaction type; rows that are
 * not transactions fall back to their HIST-RECORD-TYPE.
 */
function typeLabel(record: HistoryRecord): string {
  return record.transactionType
    ? TRANSACTION_TYPE_LABELS[record.transactionType]
    : HISTORY_RECORD_TYPE_LABELS[record.recordType];
}
