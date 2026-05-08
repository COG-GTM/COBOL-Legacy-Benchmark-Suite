/**
 * History record types derived from COBOL copybook HISTREC.cpy (lines 6-27)
 */

export type RecordType = 'PORT' | 'POS' | 'TRN';

export type ActionCode = 'ADD' | 'CHANGE' | 'DELETE';

export interface HistoryRecord {
  portfolioId: string;     // HIST-PORTFOLIO-ID (8 chars)
  date: string;            // HIST-DATE (ISO 8601)
  time: string;            // HIST-TIME (HH:MM:SS)
  seqNo: string;           // HIST-SEQ-NO (4 chars)
  recordType: RecordType;  // HIST-REC-TYPE PORT/POS/TRN
  actionCode: ActionCode;  // HIST-ACTION-CODE ADD/CHANGE/DELETE
  beforeImage: string;     // HIST-BEFORE-IMAGE (400 chars)
  afterImage: string;      // HIST-AFTER-IMAGE (400 chars)
  reasonCode: string;      // HIST-REASON-CODE (4 chars)
  processDate: string;     // HIST-PROCESS-DATE (ISO 8601)
  processUser: string;     // HIST-PROCESS-USER (8 chars)
}
