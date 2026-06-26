import { FunctionCode } from './functionCodes'

/**
 * Client-side model of the online inquiry COMMAREA, ported field-for-field
 * from `src/copybook/online/INQCOM.cpy`:
 *
 *   01 INQCOM-AREA.
 *     05 INQCOM-FUNCTION      PIC X(4).
 *     05 INQCOM-ACCOUNT-NO    PIC X(10).
 *     05 INQCOM-RESPONSE-CODE PIC S9(8) COMP.
 *     05 INQCOM-ERROR-MSG     PIC X(80).
 */
export interface InqCommArea {
  /** INQCOM-FUNCTION: current 4-char function code. */
  func: FunctionCode
  /** INQCOM-ACCOUNT-NO: 10-char account number context. */
  accountNo: string
  /** INQCOM-RESPONSE-CODE: numeric status (0 = normal). */
  responseCode: number
  /** INQCOM-ERROR-MSG: 80-char error/status message. */
  errorMsg: string
}

/**
 * Response codes modeled on the CICS `RESP` values used throughout
 * `INQONLN.cbl`. `NORMAL` (0) means success; `INVREQ` flags the invalid
 * selection handled by the `WHEN OTHER` error routine.
 */
export const ResponseCode = {
  NORMAL: 0,
  INVREQ: 16,
} as const

export const EMPTY_COMMAREA: InqCommArea = {
  func: FunctionCode.MENU,
  accountNo: '',
  responseCode: ResponseCode.NORMAL,
  errorMsg: '',
}
