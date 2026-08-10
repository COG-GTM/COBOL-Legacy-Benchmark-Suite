import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useHistoryService } from '../../services/servicesContext';
import {
  HISTORY_ACTION_LABELS,
  HISTORY_RECORD_TYPE_LABELS,
  type HistoryRecord,
} from '../../types/history';
import { TRANSACTION_TYPE_LABELS } from '../../types/transaction';
import { formatCobolDate, formatCobolTime } from '../../utils/date';
import { formatCurrency, formatQuantity } from '../../utils/decimal';

/** Drill-down view of one HISTORY-RECORD, reached by clicking a history row. */
export function HistoryDetailPage() {
  const { recordKey } = useParams<{ recordKey: string }>();
  const service = useHistoryService();

  const [record, setRecord] = useState<HistoryRecord | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!recordKey) return;
    setLoading(true);
    setError(null);
    try {
      const result = await service.get(recordKey);
      setRecord(result ?? null);
    } catch {
      setError('Unable to load the history record.');
    } finally {
      setLoading(false);
    }
  }, [recordKey, service]);

  useEffect(() => {
    void load();
  }, [load]);

  if (loading) {
    return <p className="state-msg">Loading history record…</p>;
  }

  if (error || !record) {
    return (
      <section>
        <div className="alert alert--error" role="alert">
          {error ?? `History record “${recordKey}” was not found.`}
        </div>
        <Link to="/history" className="btn btn--ghost">
          Back to history
        </Link>
      </section>
    );
  }

  const currency = record.currency ?? 'USD';

  return (
    <section>
      <div className="page-header">
        <div>
          <h1 className="page-header__title">History Record Detail</h1>
          <p className="page-header__subtitle">
            {formatCobolDate(record.date)} {formatCobolTime(record.time)} ·{' '}
            {record.portfolioId} · seq {record.seqNo}
          </p>
        </div>
        <Link to="/history" className="btn btn--ghost">
          Back to history
        </Link>
      </div>

      <div className="detail-grid">
        <div className="card detail-card">
          <h2 className="detail-card__title">Change</h2>
          <dl className="detail-list">
            <DetailRow
              label="Record type"
              value={HISTORY_RECORD_TYPE_LABELS[record.recordType]}
            />
            <DetailRow
              label="Action"
              value={HISTORY_ACTION_LABELS[record.actionCode]}
            />
            <DetailRow label="Reason code" value={record.reasonCode} />
            <DetailRow label="Portfolio ID" value={record.portfolioId} />
          </dl>
        </div>

        <div className="card detail-card">
          <h2 className="detail-card__title">Transaction</h2>
          <dl className="detail-list">
            <DetailRow
              label="Transaction type"
              value={
                record.transactionType
                  ? TRANSACTION_TYPE_LABELS[record.transactionType]
                  : '—'
              }
            />
            <DetailRow label="Fund ID" value={record.investmentId ?? '—'} />
            <DetailRow
              label="Units"
              value={record.units ? formatQuantity(record.units) : '—'}
            />
            <DetailRow
              label="Price"
              value={
                record.price ? formatCurrency(record.price, currency, 4) : '—'
              }
            />
            <DetailRow
              label="Amount"
              value={
                record.amount ? formatCurrency(record.amount, currency) : '—'
              }
            />
          </dl>
        </div>

        <div className="card detail-card">
          <h2 className="detail-card__title">Record images</h2>
          <dl className="detail-list">
            <DetailRow
              label="Before"
              value={record.beforeImage || '—'}
              mono
            />
            <DetailRow label="After" value={record.afterImage || '—'} mono />
          </dl>
        </div>

        <div className="card detail-card">
          <h2 className="detail-card__title">Audit</h2>
          <dl className="detail-list">
            <DetailRow label="Processed at" value={record.processDate} />
            <DetailRow label="Processed by" value={record.processUser} />
          </dl>
        </div>
      </div>
    </section>
  );
}

function DetailRow({
  label,
  value,
  mono,
}: {
  label: string;
  value: string;
  mono?: boolean;
}) {
  return (
    <div className="detail-list__row">
      <dt>{label}</dt>
      <dd className={mono ? 'detail-list__value--mono' : undefined}>{value}</dd>
    </div>
  );
}
