export interface HistoryEntry {
  transDate: string;   // 10 chars, maps to WS-TRANS-DATE
  transType: string;   // 4 chars, maps to WS-TRANS-TYPE
  transUnits: number;  // decimal, maps to WS-TRANS-UNITS
  transPrice: number;  // decimal, maps to WS-TRANS-PRICE
  transAmount: number; // decimal, maps to WS-TRANS-AMOUNT
}

const mockHistory: HistoryEntry[] = [
  { transDate: "2025-12-15", transType: "BUY ", transUnits: 150.000, transPrice: 98.45, transAmount: 14767.50 },
  { transDate: "2025-12-10", transType: "SELL", transUnits: 75.500, transPrice: 97.80, transAmount: 7383.90 },
  { transDate: "2025-11-28", transType: "BUY ", transUnits: 200.000, transPrice: 96.12, transAmount: 19224.00 },
  { transDate: "2025-11-15", transType: "DIV ", transUnits: 12.345, transPrice: 95.50, transAmount: 1178.95 },
  { transDate: "2025-11-01", transType: "BUY ", transUnits: 100.000, transPrice: 94.75, transAmount: 9475.00 },
  { transDate: "2025-10-20", transType: "SELL", transUnits: 50.000, transPrice: 93.20, transAmount: 4660.00 },
  { transDate: "2025-10-15", transType: "BUY ", transUnits: 300.000, transPrice: 92.80, transAmount: 27840.00 },
  { transDate: "2025-10-01", transType: "DIV ", transUnits: 8.765, transPrice: 91.50, transAmount: 802.00 },
  { transDate: "2025-09-18", transType: "BUY ", transUnits: 125.000, transPrice: 90.25, transAmount: 11281.25 },
  { transDate: "2025-09-10", transType: "SELL", transUnits: 60.000, transPrice: 89.90, transAmount: 5394.00 },
  { transDate: "2025-08-28", transType: "BUY ", transUnits: 175.000, transPrice: 88.45, transAmount: 15478.75 },
  { transDate: "2025-08-15", transType: "DIV ", transUnits: 10.234, transPrice: 87.60, transAmount: 896.50 },
  { transDate: "2025-08-01", transType: "BUY ", transUnits: 250.000, transPrice: 86.30, transAmount: 21575.00 },
  { transDate: "2025-07-20", transType: "SELL", transUnits: 80.000, transPrice: 85.75, transAmount: 6860.00 },
  { transDate: "2025-07-10", transType: "BUY ", transUnits: 110.000, transPrice: 84.90, transAmount: 9339.00 },
  { transDate: "2025-06-28", transType: "BUY ", transUnits: 90.000, transPrice: 83.55, transAmount: 7519.50 },
  { transDate: "2025-06-15", transType: "DIV ", transUnits: 15.678, transPrice: 82.40, transAmount: 1291.87 },
  { transDate: "2025-06-01", transType: "SELL", transUnits: 40.000, transPrice: 81.20, transAmount: 3248.00 },
  { transDate: "2025-05-20", transType: "BUY ", transUnits: 180.000, transPrice: 80.10, transAmount: 14418.00 },
  { transDate: "2025-05-10", transType: "BUY ", transUnits: 220.000, transPrice: 79.45, transAmount: 17479.00 },
  { transDate: "2025-04-28", transType: "SELL", transUnits: 95.000, transPrice: 78.80, transAmount: 7486.00 },
  { transDate: "2025-04-15", transType: "DIV ", transUnits: 11.456, transPrice: 77.90, transAmount: 892.43 },
  { transDate: "2025-04-01", transType: "BUY ", transUnits: 160.000, transPrice: 76.55, transAmount: 12248.00 },
  { transDate: "2025-03-18", transType: "BUY ", transUnits: 140.000, transPrice: 75.30, transAmount: 10542.00 },
  { transDate: "2025-03-05", transType: "SELL", transUnits: 70.000, transPrice: 74.80, transAmount: 5236.00 },
];

export interface HistoryPage {
  entries: HistoryEntry[];
  totalEntries: number;
  currentPage: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  message: string;
}

const PAGE_SIZE = 10; // matches BMS map ROW1–ROW10

export async function fetchHistory(page: number = 1): Promise<HistoryPage> {
  // Simulate network delay
  await new Promise((resolve) => setTimeout(resolve, 300));

  const totalEntries = mockHistory.length;
  const totalPages = Math.ceil(totalEntries / PAGE_SIZE);
  const safePage = Math.max(1, Math.min(page, totalPages));
  const startIdx = (safePage - 1) * PAGE_SIZE;
  const entries = mockHistory.slice(startIdx, startIdx + PAGE_SIZE);

  return {
    entries,
    totalEntries,
    currentPage: safePage,
    totalPages,
    hasNext: safePage < totalPages,
    hasPrevious: safePage > 1,
    message: `Displaying page ${safePage} of ${totalPages} (${totalEntries} total records)`,
  };
}
