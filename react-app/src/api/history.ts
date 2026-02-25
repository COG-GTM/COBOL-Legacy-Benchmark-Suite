/**
 * History API — mock data layer
 * Replaces DB2 POSHIST table queries from INQHIST COBOL program.
 * Data structure matches WS-HISTORY-ENTRY (INQHIST.cbl lines 20-27):
 *   WS-TRANS-DATE    PIC X(10)
 *   WS-TRANS-TYPE    PIC X(4)
 *   WS-TRANS-UNITS   PIC S9(9)V99
 *   WS-TRANS-PRICE   PIC S9(9)V99
 *   WS-TRANS-AMOUNT  PIC S9(9)V99
 */

export interface HistoryEntry {
  transDate: string;   // 10 chars
  transType: string;   // 4 chars — BUY, SELL, DIV, XFER
  transUnits: number;  // decimal
  transPrice: number;  // decimal
  transAmount: number; // decimal
}

export interface HistoryPage {
  entries: HistoryEntry[];
  totalRows: number;
  currentPage: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

const MOCK_HISTORY: HistoryEntry[] = [
  { transDate: "2025-12-15", transType: "BUY",  transUnits: 25.000, transPrice: 347.12, transAmount: 8678.00 },
  { transDate: "2025-12-01", transType: "DIV",  transUnits: 0.843,  transPrice: 347.12, transAmount: 292.62 },
  { transDate: "2025-11-15", transType: "BUY",  transUnits: 10.000, transPrice: 342.50, transAmount: 3425.00 },
  { transDate: "2025-11-01", transType: "DIV",  transUnits: 0.812,  transPrice: 342.50, transAmount: 278.11 },
  { transDate: "2025-10-20", transType: "SELL", transUnits: 5.000,  transPrice: 339.80, transAmount: 1699.00 },
  { transDate: "2025-10-15", transType: "BUY",  transUnits: 15.000, transPrice: 338.25, transAmount: 5073.75 },
  { transDate: "2025-10-01", transType: "DIV",  transUnits: 0.790,  transPrice: 338.25, transAmount: 267.22 },
  { transDate: "2025-09-15", transType: "BUY",  transUnits: 20.000, transPrice: 335.40, transAmount: 6708.00 },
  { transDate: "2025-09-01", transType: "DIV",  transUnits: 0.775,  transPrice: 335.40, transAmount: 259.94 },
  { transDate: "2025-08-20", transType: "XFER", transUnits: 50.000, transPrice: 330.10, transAmount: 16505.00 },
  { transDate: "2025-08-15", transType: "BUY",  transUnits: 12.000, transPrice: 330.10, transAmount: 3961.20 },
  { transDate: "2025-08-01", transType: "DIV",  transUnits: 0.750,  transPrice: 330.10, transAmount: 247.58 },
  { transDate: "2025-07-15", transType: "BUY",  transUnits: 18.000, transPrice: 325.75, transAmount: 5863.50 },
  { transDate: "2025-07-01", transType: "DIV",  transUnits: 0.720,  transPrice: 325.75, transAmount: 234.54 },
  { transDate: "2025-06-20", transType: "SELL", transUnits: 8.000,  transPrice: 320.90, transAmount: 2567.20 },
  { transDate: "2025-06-15", transType: "BUY",  transUnits: 22.000, transPrice: 320.90, transAmount: 7059.80 },
  { transDate: "2025-06-01", transType: "DIV",  transUnits: 0.695,  transPrice: 320.90, transAmount: 223.03 },
  { transDate: "2025-05-15", transType: "BUY",  transUnits: 14.000, transPrice: 318.60, transAmount: 4460.40 },
  { transDate: "2025-05-01", transType: "DIV",  transUnits: 0.680,  transPrice: 318.60, transAmount: 216.65 },
  { transDate: "2025-04-15", transType: "BUY",  transUnits: 30.000, transPrice: 315.20, transAmount: 9456.00 },
  { transDate: "2025-04-01", transType: "DIV",  transUnits: 0.660,  transPrice: 315.20, transAmount: 208.03 },
  { transDate: "2025-03-15", transType: "SELL", transUnits: 10.000, transPrice: 310.50, transAmount: 3105.00 },
  { transDate: "2025-03-01", transType: "DIV",  transUnits: 0.640,  transPrice: 310.50, transAmount: 198.72 },
  { transDate: "2025-02-15", transType: "BUY",  transUnits: 16.000, transPrice: 308.30, transAmount: 4932.80 },
  { transDate: "2025-02-01", transType: "DIV",  transUnits: 0.620,  transPrice: 308.30, transAmount: 191.15 },
];

const PAGE_SIZE = 10;

export function fetchTransactionHistory(page: number = 1): Promise<HistoryPage> {
  return new Promise((resolve) => {
    setTimeout(() => {
      const totalRows = MOCK_HISTORY.length;
      const totalPages = Math.ceil(totalRows / PAGE_SIZE);
      const safePage = Math.max(1, Math.min(page, totalPages));
      const start = (safePage - 1) * PAGE_SIZE;
      const entries = MOCK_HISTORY.slice(start, start + PAGE_SIZE);

      resolve({
        entries,
        totalRows,
        currentPage: safePage,
        totalPages,
        hasNext: safePage < totalPages,
        hasPrevious: safePage > 1,
      });
    }, 300);
  });
}
