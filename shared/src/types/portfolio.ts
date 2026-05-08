/**
 * Portfolio types derived from COBOL copybook PORTFLIO.cpy (lines 11-34)
 * and DB2 table PORTFOLIO_MASTER
 */

export type ClientType = 'INDIVIDUAL' | 'CORPORATE' | 'TRUST';

export type PortfolioStatus = 'ACTIVE' | 'CLOSED' | 'SUSPENDED';

export interface Portfolio {
  id: string;              // PORT-ID (8 chars)
  accountNo: string;       // PORT-ACCOUNT-NO (10 chars)
  clientName: string;      // PORT-CLIENT-NAME (30 chars)
  clientType: ClientType;  // PORT-CLIENT-TYPE I/C/T
  createDate: string;      // PORT-CREATE-DATE (ISO 8601)
  status: PortfolioStatus; // PORT-STATUS A/C/S
  totalValue: number;      // PORT-TOTAL-VALUE (COMP-3 S9(13)V99)
  cashBalance: number;     // PORT-CASH-BALANCE (COMP-3 S9(13)V99)
  lastUser: string;        // PORT-LAST-USER (8 chars)
  lastTransaction: string; // PORT-LAST-TRANS (ISO 8601)
}
