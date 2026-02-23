// Types migrated from COBOL copybook definitions and BMS map fields

// From INQCOM copybook - Communication area between programs
export interface CommArea {
  function: 'MENU' | 'INQP' | 'INQH' | 'EXIT';
  accountNo: string;
  errorMsg: string;
  responseCode: number;
}

// From POSREC copybook - Position record (VSAM)
export interface PortfolioPosition {
  fundId: string;      // FUNDOUT - 6 chars
  fundName: string;    // NAMEOUT - 30 chars
  units: string;       // UNITOUT - 15 chars
  costBasis: string;   // COSTOUT - 15 chars
  marketValue: string; // VALOUT - 15 chars
}

// From INQHIST WS-HISTORY-ENTRY - Transaction history row
export interface TransactionHistoryEntry {
  date: string;        // WS-TRANS-DATE - 10 chars
  type: string;        // WS-TRANS-TYPE - 4 chars (BUY/SELL/DIV/etc.)
  units: string;       // WS-TRANS-UNITS - numeric
  price: string;       // WS-TRANS-PRICE - numeric
  amount: string;      // WS-TRANS-AMOUNT - numeric
}

// From SECMGR SECURITY-REQUEST-AREA
export interface SecurityRequest {
  requestType: 'V' | 'A' | 'L'; // Validate, Authorize, Log
  userId: string;
  resourceName: string;
  accessType: string;
}

export interface SecurityResponse {
  responseCode: number;
  errorInfo: string;
}

// From ERRHNDL WS-ERROR-AREA
export interface ErrorInfo {
  errorCode: string;   // ERRCOUT - 8 chars
  errorDetails: string; // ERRDOUT - 65 chars
  program: string;
  paragraph: string;
  severity: 'I' | 'W' | 'F'; // Info, Warning, Fatal
  traceId: string;
}

// From ERRMAP - Error display
export interface SystemError {
  code: string;
  details: string;
}

// API response wrappers
export interface ApiResponse<T> {
  data: T | null;
  error: string | null;
  success: boolean;
}

// Auth state (replaces SECMGR session management)
export interface AuthState {
  isAuthenticated: boolean;
  userId: string;
  sessionActive: boolean;
}

// Navigation state (replaces CURSMGR cursor/screen management)
export interface NavigationState {
  currentScreen: 'MENU' | 'PORTFOLIO' | 'HISTORY' | 'ERROR';
  previousScreen: 'MENU' | 'PORTFOLIO' | 'HISTORY' | 'ERROR' | null;
}

// Pagination (replaces PF7/PF8 navigation)
export interface PaginationState {
  currentPage: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}
