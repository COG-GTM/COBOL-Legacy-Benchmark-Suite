import {
  HISTORY_ACTION_LABELS,
  HISTORY_RECORD_TYPE_LABELS,
  type HistoryRecord,
} from '../../types/history';
import { TRANSACTION_TYPE_LABELS } from '../../types/transaction';
import { formatCobolDate, formatCobolTime } from '../../utils/date';
import { toCsv } from '../../utils/csv';

const HEADERS = [
  'Date',
  'Time',
  'Portfolio',
  'Sequence',
  'Record Type',
  'Action',
  'Transaction Type',
  'Fund ID',
  'Units',
  'Price',
  'Amount',
  'Currency',
  'Reason Code',
  'Process User',
] as const;

/**
 * Exported figures stay as raw decimal strings (no grouping or currency
 * symbol) so the file stays machine-readable in Excel and downstream tools.
 */
function row(record: HistoryRecord): string[] {
  return [
    formatCobolDate(record.date),
    formatCobolTime(record.time),
    record.portfolioId,
    record.seqNo,
    HISTORY_RECORD_TYPE_LABELS[record.recordType],
    HISTORY_ACTION_LABELS[record.actionCode],
    record.transactionType
      ? TRANSACTION_TYPE_LABELS[record.transactionType]
      : '',
    record.investmentId ?? '',
    record.units ?? '',
    record.price ?? '',
    record.amount ?? '',
    record.currency ?? '',
    record.reasonCode,
    record.processUser,
  ];
}

/** Serializes the current (already filtered) history result set as CSV. */
export function historyToCsv(records: readonly HistoryRecord[]): string {
  return toCsv(HEADERS, records.map(row));
}

/** `history-ACCT100001-20240101-20240430.csv`, bounds omitted when unset. */
export function historyCsvFilename(
  accountNo: string,
  startDate: string,
  endDate: string,
): string {
  const parts = ['history', accountNo, startDate, endDate].filter(Boolean);
  return `${parts.join('-')}.csv`;
}
